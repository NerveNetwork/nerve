package network.nerve.pocbft.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.Agent;
import network.nerve.pocbft.model.bo.tx.txdata.RedPunishData;
import network.nerve.pocbft.model.bo.tx.txdata.StopAgent;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.AgentDepositNonceManager;
import network.nerve.pocbft.utils.manager.AgentManager;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.validator.StopAgentValidator;

import java.io.IOException;
import java.util.*;

/**
 * 停止节点交易处理器
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("StopAgentProcessorV1")
public class StopAgentProcessor implements TransactionProcessor {
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private StopAgentValidator validator;

    @Override
    public int getType() {
        return TxType.STOP_AGENT;
    }

    @Override
    public int getPriority() {
        return 6;
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
        Set<String> redPunishAddressSet = new HashSet<>();
        Set<NulsHash> hashSet = new HashSet<>();
        List<Transaction> redPunishTxList = txMap.get(TxType.RED_PUNISH);
        if (redPunishTxList != null && redPunishTxList.size() > 0) {
            for (Transaction redPunishTx : redPunishTxList) {
                RedPunishData redPunishData = new RedPunishData();
                try {
                    redPunishData.parse(redPunishTx.getTxData(), 0);
                    String addressHex = HexUtil.encode(redPunishData.getAddress());
                    redPunishAddressSet.add(addressHex);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                }
            }
        }
        Result rs;
        for (Transaction stopAgentTx : txs) {
            try {
                //验证停止节点交易时间正确性
                long time = NulsDateUtils.getCurrentTimeSeconds();
                if (blockHeader != null) {
                    time = blockHeader.getTime();
                }
                long txTime = stopAgentTx.getTime();
                if (txTime > time + ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT || txTime < time - ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT) {
                    invalidTxList.add(stopAgentTx);
                    chain.getLogger().error("Trading time error,txTime:{},time:{}", txTime, time);
                    errorCode = ConsensusErrorCode.ERROR_UNLOCK_TIME.getCode();
                    continue;
                }
                rs = validator.validate(chain, stopAgentTx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(stopAgentTx);
                    chain.getLogger().info("Intelligent Contract Exit Node Trading Verification Failed");
                    errorCode = rs.getErrorCode().getCode();
                    continue;
                }
                StopAgent stopAgent = new StopAgent();
                stopAgent.parse(stopAgentTx.getTxData(), 0);
                if (!hashSet.add(stopAgent.getCreateTxHash())) {
                    invalidTxList.add(stopAgentTx);
                    chain.getLogger().info("Repeated transactions");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                    continue;
                }
                if (!redPunishAddressSet.isEmpty()) {
                    if (redPunishAddressSet.contains(HexUtil.encode(stopAgent.getAddress()))) {
                        invalidTxList.add(stopAgentTx);
                        chain.getLogger().info("Intelligent contract cancellation node transaction cancellation node does not exist");
                        errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                    }
                }
            } catch (NulsException e) {
                invalidTxList.add(stopAgentTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            } catch (IOException io) {
                invalidTxList.add(stopAgentTx);
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
            if (stopAgentCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //回滚已提交成功的交易
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                stopAgentRollBack(rollbackTx, chain);
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
            if (stopAgentRollBack(tx, chain)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                stopAgentCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }


    private boolean stopAgentCommit(Transaction transaction, BlockHeader blockHeader, Chain chain) {
        //找到需要注销的节点信息
        StopAgent stopAgent = new StopAgent();
        try {
            stopAgent.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        Agent agent = agentManager.getAgentByHash(chain, stopAgent.getCreateTxHash());
        if (agent == null || agent.getDelHeight() > 0) {
            chain.getLogger().error("Agent does not exist or has been unregistered");
            return false;
        }
        agent.setDelHeight(blockHeader.getHeight());
        if (!agentManager.updateAgent(chain, agent)) {
            chain.getLogger().error("Stop agent tx commit error");
            return false;
        }
        if (!AgentDepositNonceManager.unLockTxCommit(chain, stopAgent.getCreateTxHash(), transaction, true)) {
            agent.setDelHeight(-1);
            agentManager.updateAgent(chain, agent);
            chain.getLogger().error("Stop agent tx update nonce data commit error");
            return false;
        }
        //保存数据库和缓存
        return true;
    }

    private boolean stopAgentRollBack(Transaction transaction, Chain chain) {
        StopAgent stopAgent = new StopAgent();
        try {
            stopAgent.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }

        if (!AgentDepositNonceManager.unLockTxRollback(chain, stopAgent.getCreateTxHash(), transaction, true)) {
            chain.getLogger().error("Stop agent tx failed to rollback the reduce margin nonce record");
            return false;
        }

        Agent agent = agentManager.getAgentByHash(chain, stopAgent.getCreateTxHash());
        agent.setDelHeight(-1);
        //保存数据库和缓存
        if (!agentManager.updateAgent(chain, agent)) {
            AgentDepositNonceManager.unLockTxCommit(chain, stopAgent.getCreateTxHash(), transaction, true);
            chain.getLogger().error("Stop agent tx rollback error");
            return false;
        }
        return true;
    }
}
