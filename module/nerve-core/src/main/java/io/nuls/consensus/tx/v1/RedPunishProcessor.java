package io.nuls.consensus.tx.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.bo.tx.txdata.RedPunishData;
import io.nuls.consensus.storage.PunishStorageService;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.AgentDepositNonceManager;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.manager.PunishManager;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.po.PunishLogPo;
import io.nuls.consensus.utils.enumeration.PunishType;
import io.nuls.consensus.utils.manager.AgentManager;

import java.util.*;

/**
 * Red card trading processor
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("RedPunishProcessorV1")
public class RedPunishProcessor implements TransactionProcessor {
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private PunishStorageService punishStorageService;
    @Autowired
    private AgentManager agentManager;

    @Override
    public int getType() {
        return TxType.RED_PUNISH;
    }

    @Override
    public int getPriority() {
        return 10;
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
        Set<String> addressHexSet = new HashSet<>();
        for (Transaction tx : txs) {
            try {
                RedPunishData redPunishData = new RedPunishData();
                redPunishData.parse(tx.getTxData(), 0);
                String addressHex = HexUtil.encode(redPunishData.getAddress());
                /*
                 * Repeated red card transactions are not packaged
                 * */
                if (!addressHexSet.add(addressHex)) {
                    invalidTxList.add(tx);
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }
            } catch (NulsException e) {
                invalidTxList.add(tx);
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
            if (redPunishCommit(tx, chain, blockHeader)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //Roll back transactions that have been successfully submitted
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                redPunishRollback(rollbackTx, chain, blockHeader);
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
            if (redPunishRollback(tx, chain, blockHeader)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //Save successfully rolled back transactions
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                redPunishCommit(commitTx, chain, blockHeader);
            }
        }
        return rollbackResult;
    }

    private boolean redPunishCommit(Transaction tx, Chain chain, BlockHeader blockHeader) {
        long blockHeight = blockHeader.getHeight();
        int chainId = chain.getConfig().getChainId();
        RedPunishData punishData = new RedPunishData();
        try {
            punishData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        BlockExtendsData roundData = blockHeader.getExtendsData();
        PunishLogPo punishLogPo = new PunishLogPo();
        punishLogPo.setAddress(punishData.getAddress());
        punishLogPo.setHeight(blockHeight);
        punishLogPo.setRoundIndex(roundData.getRoundIndex());
        punishLogPo.setTime(tx.getTime());
        punishLogPo.setType(PunishType.RED.getCode());
        punishLogPo.setEvidence(punishData.getEvidence());
        punishLogPo.setReasonCode(punishData.getReasonCode());

        /*
        Find the penalized node
        Find the punished node
         */
        Agent agent = agentManager.getAgentByAddress(chain, punishLogPo.getAddress());

        if (null == agent || agent.getDelHeight() > 0) {
            chain.getLogger().error("Agent does not exist or has been unregistered");
            return false;
        }

        //Save red card penalty information
        punishStorageService.save(punishLogPo, chainId);

        //Modify penalty node information
        agent.setDelHeight(blockHeight);
        if(!agentManager.updateAgent(chain, agent)){
            punishStorageService.delete(punishLogPo.getKey(), chainId);
            return false;
        }

        //modifyNONCEinformation
        if(!AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true)){
            agent.setDelHeight(-1);
            agentManager.updateAgent(chain, agent);
            punishStorageService.delete(punishLogPo.getKey(), chainId);
            chain.getLogger().error("Red punish update agent deposit nonce error");
            return false;
        }

        //Update cache
        chain.getRedPunishList().add(punishLogPo);
        return true;
    }

    private boolean redPunishRollback(Transaction tx, Chain chain, BlockHeader blockHeader) {
        long blockHeight = blockHeader.getHeight();
        int chainId = chain.getConfig().getChainId();
        RedPunishData punishData = new RedPunishData();
        try {
            punishData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        /*
        Find the penalized node
        Find the punished node
         */
        Agent agent = agentManager.getAgentByAddress(chain, punishData.getAddress());
        if (null == agent || agent.getDelHeight() <= 0) {
            chain.getLogger().error("Agent does not exist");
            return false;
        }

        if(!AgentDepositNonceManager.unLockTxRollback(chain, agent.getTxHash(), tx, true)){
            chain.getLogger().error("Red punish tx failed to rollback the reduce margin nonce record");
            return false;
        }

        //Modify penalty node information
        agent.setDelHeight(-1L);
        if(!agentManager.updateAgent(chain, agent)){
            AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true);
            chain.getLogger().error("Red punish tx agent data rollback error");
            return false;
        }

        //Delete red card penalty information
        byte[] key = ByteUtils.concatenate(punishData.getAddress(), new byte[]{PunishType.RED.getCode()}, SerializeUtils.uint64ToByteArray(blockHeight), new byte[]{0});
        if (!punishStorageService.delete(key, chainId)) {
            AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true);
            agent.setDelHeight(blockHeight);
            agentManager.updateAgent(chain, agent);
            chain.getLogger().error("Data save error!");
            return false;
        }

        /*
         * Modify cache
         * */
        BlockExtendsData roundData = blockHeader.getExtendsData();
        PunishLogPo punishLogPo = new PunishLogPo();
        punishLogPo.setAddress(punishData.getAddress());
        punishLogPo.setHeight(blockHeight);
        punishLogPo.setRoundIndex(roundData.getRoundIndex());
        punishLogPo.setTime(tx.getTime());
        punishLogPo.setType(PunishType.RED.getCode());
        punishLogPo.setEvidence(punishData.getEvidence());
        punishLogPo.setReasonCode(punishData.getReasonCode());
        chain.getRedPunishList().remove(punishLogPo);
        return true;
    }
}
