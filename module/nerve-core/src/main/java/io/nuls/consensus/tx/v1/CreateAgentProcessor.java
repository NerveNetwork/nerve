package io.nuls.consensus.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.bo.tx.txdata.RedPunishData;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.AgentDepositNonceManager;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.consensus.utils.validator.CreateAgentValidator;

import java.util.*;

/**
 * Create Node Processor
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("CreateAgentProcessorV1")
public class CreateAgentProcessor implements TransactionProcessor {
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private CreateAgentValidator validator;

    @Override
    public int getType() {
        return TxType.REGISTER_AGENT;
    }

    @Override
    public int getPriority() {
        return 8;
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
        Set<String> createAgentAddressSet = new HashSet<>();
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
        for (Transaction createAgentTx : txs) {
            try {
                rs = validator.validate(chain, createAgentTx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(createAgentTx);
                    chain.getLogger().info("Failure to create node transaction validation");
                    errorCode = rs.getErrorCode().getCode();
                    continue;
                }
                Agent agent = new Agent();
                agent.parse(createAgentTx.getTxData(), 0);
                String agentAddressHex = HexUtil.encode(agent.getAgentAddress());
                String packAddressHex = HexUtil.encode(agent.getPackingAddress());
                /*
                 * An address that has obtained a red card transaction cannot create a node
                 * */
                if (!redPunishAddressSet.isEmpty()) {
                    if (redPunishAddressSet.contains(agentAddressHex) || redPunishAddressSet.contains(packAddressHex)) {
                        invalidTxList.add(createAgentTx);
                        chain.getLogger().info("Creating Node Trading and Red Card Trading Conflict");
                        errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                        continue;
                    }
                }
                /*
                 * Repeatedly creating nodes
                 * */
                if (!createAgentAddressSet.add(agentAddressHex) || !createAgentAddressSet.add(packAddressHex)) {
                    invalidTxList.add(createAgentTx);
                    chain.getLogger().info("Repeated transactions");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }
            } catch (NulsException e) {
                invalidTxList.add(createAgentTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();

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
            if (createAgentCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //Roll back transactions that have been successfully submitted
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                createAgentRollBack(rollbackTx, chain, blockHeader);
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
            if (createAgentRollBack(tx, chain, blockHeader)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }

        }
        //Save successfully rolled back transactions
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                createAgentCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }


    private boolean createAgentCommit(Transaction transaction, BlockHeader blockHeader, Chain chain) {
        Agent agent = new Agent();
        try {
            agent.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        agent.setTxHash(transaction.getHash());
        agent.setBlockHeight(blockHeader.getHeight());
        agent.setTime(transaction.getTime());
        if (!agentManager.addAgent(chain, agent)) {
            chain.getLogger().error("Agent record save fail");
            return false;
        }
        if (!AgentDepositNonceManager.init(agent, chain, transaction.getHash())) {
            agentManager.removeAgent(chain, transaction.getHash());
            chain.getLogger().error("Agent deposit nonce record init error");
            return false;
        }
        return true;
    }

    private boolean createAgentRollBack(Transaction transaction, Chain chain, BlockHeader blockHeader) {
        if (!AgentDepositNonceManager.delete(chain, transaction.getHash())) {
            chain.getLogger().error("Agent deposit init nonce rollback error");
            return false;
        }
        if (!agentManager.removeAgent(chain, transaction.getHash())) {
            Agent agent = new Agent();
            try {
                agent.parse(transaction.getTxData(), 0);
            } catch (NulsException e) {
                chain.getLogger().error(e);
                return false;
            }
            agent.setTxHash(transaction.getHash());
            agent.setBlockHeight(blockHeader.getHeight());
            agent.setTime(transaction.getTime());
            AgentDepositNonceManager.init(agent, chain, transaction.getHash());
            chain.getLogger().error("Create agent tx rollback error");
            return false;
        }
        return true;
    }
}
