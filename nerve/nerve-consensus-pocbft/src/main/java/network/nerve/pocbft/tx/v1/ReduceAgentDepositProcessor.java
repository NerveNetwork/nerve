package network.nerve.pocbft.tx.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.bo.tx.txdata.ChangeAgentDepositData;
import network.nerve.pocbft.model.bo.tx.txdata.RedPunishData;
import network.nerve.pocbft.model.bo.tx.txdata.StopAgent;
import network.nerve.pocbft.model.po.ChangeAgentDepositPo;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.AgentDepositManager;
import network.nerve.pocbft.utils.manager.AgentDepositNonceManager;
import network.nerve.pocbft.utils.manager.AgentManager;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.validator.ReduceAgentDepositValidator;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * CoinBase交易处理器
 *
 * @author tag
 * @date 2019/10/22
 */
@Component("ReduceAgentDepositProcessorV1")
public class ReduceAgentDepositProcessor implements TransactionProcessor {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ReduceAgentDepositValidator validator;
    @Autowired
    private AgentDepositManager agentDepositManager;
    @Autowired
    private AgentManager agentManager;

    @Override
    public int getType() {
        return TxType.REDUCE_AGENT_DEPOSIT;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            result.put("txList", txs);
            result.put("errorCode", ConsensusErrorCode.CHAIN_NOT_EXIST.getCode());
            return result;
        }
        List<Transaction> invalidTxList = new ArrayList<>();
        String errorCode = null;

        Set<String> invalidAgentSet = new HashSet<>();
        List<Transaction> redPunishTxList = txMap.get(TxType.RED_PUNISH);
        if (redPunishTxList != null && redPunishTxList.size() > 0) {
            for (Transaction redPunishTx : redPunishTxList) {
                RedPunishData redPunishData = new RedPunishData();
                try {
                    redPunishData.parse(redPunishTx.getTxData(), 0);
                    String redPunishAddress = AddressTool.getStringAddressByBytes(redPunishData.getAddress());
                    invalidAgentSet.add(redPunishAddress);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                }
            }
        }
        List<Transaction> stopAgentTxList = txMap.get(TxType.STOP_AGENT);
        if (stopAgentTxList != null && !stopAgentTxList.isEmpty()) {
            for (Transaction stopAgentTx : stopAgentTxList) {
                StopAgent stopAgent = new StopAgent();
                try {
                    stopAgent.parse(stopAgentTx.getTxData(), 0);
                    String stopAgentAddress = AddressTool.getStringAddressByBytes(stopAgent.getAddress());
                    invalidAgentSet.add(stopAgentAddress);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                }
            }
        }

        Map<NulsHash, BigInteger> appendTotalAmountMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_8);

        Result rs;
        for (Transaction tx : txs) {
            try {
                //验证停止节点交易时间正确性
                long time = NulsDateUtils.getCurrentTimeSeconds();
                if (blockHeader != null) {
                    time = blockHeader.getTime();
                }
                if (tx.getTime() > time + ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT || tx.getTime() < time - ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT) {
                    invalidTxList.add(tx);
                    chain.getLogger().error("Trading time error,txTime:{},time:{}", tx.getTime(), time);
                    errorCode = ConsensusErrorCode.ERROR_UNLOCK_TIME.getCode();
                    continue;
                }
                rs = validator.validate(chain, tx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(tx);
                    errorCode = rs.getErrorCode().getCode();
                    chain.getLogger().info("Failure to create node transaction validation");
                    continue;
                }
                ChangeAgentDepositData txData = new ChangeAgentDepositData();
                txData.parse(tx.getTxData(), 0);
                if (invalidAgentSet.contains(AddressTool.getStringAddressByBytes(txData.getAddress()))) {
                    invalidTxList.add(tx);
                    chain.getLogger().info("Error in conflict detection of additional margin");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }

                //委托金额超出节点最大委托金额
                NulsHash agentHash = txData.getAgentHash();
                if (appendTotalAmountMap.containsKey(agentHash)) {
                    BigInteger totalDeposit = appendTotalAmountMap.get(agentHash).subtract(txData.getAmount());
                    if (totalDeposit.compareTo(chain.getConfig().getDepositMin()) < 0) {
                        invalidTxList.add(tx);
                        chain.getLogger().info("Exit amount is greater than the current maximum amount allowed");
                        errorCode = ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE.getCode();
                    } else {
                        appendTotalAmountMap.put(agentHash, totalDeposit);
                    }
                } else {
                    Agent agent = agentManager.getAgentByHash(chain, agentHash);
                    appendTotalAmountMap.put(agentHash, agent.getDeposit().subtract(txData.getAmount()));
                }
            } catch (NulsException e) {
                invalidTxList.add(tx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            } catch (IOException io) {
                invalidTxList.add(tx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(io);
                errorCode = ConsensusErrorCode.SERIALIZE_ERROR.getCode();
            }
        }

        result.put("txList", invalidTxList);
        result.put("errorCode", errorCode);
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> commitSuccessList = new ArrayList<>();
        boolean commitResult = true;
        for (Transaction tx : txs) {
            if (reduceDepositCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!commitResult) {
            for (Transaction commitTx : commitSuccessList) {
                reduceDepositRollback(commitTx, blockHeader, chain);
            }
        }
        return commitResult;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> rollbackSuccessList = new ArrayList<>();
        boolean rollbackResult = true;
        for (Transaction tx : txs) {
            if (reduceDepositRollback(tx, blockHeader, chain)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                reduceDepositCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }

    private boolean reduceDepositCommit(Transaction tx, BlockHeader blockHeader, Chain chain) {
        long height = blockHeader.getHeight();
        ChangeAgentDepositData data = new ChangeAgentDepositData();
        try {
            data.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        ChangeAgentDepositPo po = new ChangeAgentDepositPo(data, tx.getTime(), tx.getHash(), height);
        //保存追加记录
        if (!agentDepositManager.saveReduceDeposit(chain, po)) {
            chain.getLogger().error("Failed to save the reduce agent deposit record");
            return false;
        }

        //修改节点保证金金额
        Agent agent = agentManager.getAgentByHash(chain, po.getAgentHash());
        BigInteger oldDeposit = agent.getDeposit();
        BigInteger newDeposit = oldDeposit.subtract(po.getAmount());
        agent.setDeposit(newDeposit);
        if (!agentManager.updateAgent(chain, agent)) {
            agentDepositManager.removeReduceDeposit(chain, po.getTxHash());
            chain.getLogger().error("Agent deposit modification failed");
            return false;
        }

        //节点保证金nonce信息变更
        if (!AgentDepositNonceManager.unLockTxCommit(chain, po.getAgentHash(), tx, false)) {
            agent.setDeposit(oldDeposit);
            agentManager.updateAgent(chain, agent);
            agentDepositManager.removeReduceDeposit(chain, po.getTxHash());
            chain.getLogger().error("Failed to save the reduce margin nonce record");
            return false;
        }
        return true;
    }

    private boolean reduceDepositRollback(Transaction tx, BlockHeader blockHeader, Chain chain) {
        long height = blockHeader.getHeight();
        ChangeAgentDepositData data = new ChangeAgentDepositData();
        try {
            data.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        ChangeAgentDepositPo po = new ChangeAgentDepositPo(data, tx.getTime(), tx.getHash(), height);
        //修改节点保证金金额
        Agent agent = agentManager.getAgentByHash(chain, po.getAgentHash());
        BigInteger newDeposit = agent.getDeposit();
        BigInteger oldDeposit = newDeposit.add(po.getAmount());
        agent.setDeposit(oldDeposit);
        if (!AgentDepositNonceManager.unLockTxRollback(chain, po.getAgentHash(), tx, false)) {
            chain.getLogger().error("Failed to rollback the reduce margin nonce record");
            return false;
        }
        if (!agentManager.updateAgent(chain, agent)) {
            AgentDepositNonceManager.unLockTxCommit(chain, po.getAgentHash(), tx, false);
            chain.getLogger().error("Agent deposit modification failed");
            return false;
        }
        //删除追加记录
        if (!agentDepositManager.removeReduceDeposit(chain, po.getTxHash())) {
            AgentDepositNonceManager.unLockTxCommit(chain, po.getAgentHash(), tx, false);
            agent.setDeposit(oldDeposit.subtract(po.getAmount()));
            agentManager.updateAgent(chain, agent);
            chain.getLogger().error("Failed to rollback the reduce agent deposit record");
            return false;
        }
        return true;
    }
}
