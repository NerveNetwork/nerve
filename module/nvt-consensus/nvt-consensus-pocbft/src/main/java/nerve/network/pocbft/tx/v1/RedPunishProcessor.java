package nerve.network.pocbft.tx.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.bo.tx.txdata.RedPunishData;
import nerve.network.pocbft.model.po.PunishLogPo;
import nerve.network.pocbft.storage.PunishStorageService;
import nerve.network.pocbft.utils.LoggerUtil;
import nerve.network.pocbft.utils.enumeration.PunishType;
import nerve.network.pocbft.utils.manager.AgentDepositNonceManager;
import nerve.network.pocbft.utils.manager.AgentManager;
import nerve.network.pocbft.utils.manager.ChainManager;
import nerve.network.pocbft.utils.manager.PunishManager;

import java.util.*;

/**
 * 红牌交易处理器
 *
 * @author: Jason
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
                 * 重复的红牌交易不打包
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
        //回滚已提交成功的交易
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
        //保存已回滚成功的交易
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
        找到被惩罚的节点
        Find the punished node
         */
        Agent agent = agentManager.getAgentByAddress(chain, punishLogPo.getAddress());

        if (null == agent || agent.getDelHeight() > 0) {
            chain.getLogger().error("Agent does not exist or has been unregistered");
            return false;
        }

        //保存红牌惩罚信息
        punishStorageService.save(punishLogPo, chainId);

        //修改惩罚节点信息
        agent.setDelHeight(blockHeight);
        if(!agentManager.updateAgent(chain, agent)){
            punishStorageService.delete(punishLogPo.getKey(), chainId);
            return false;
        }

        //修改NONCE信息
        if(!AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true)){
            agent.setDelHeight(-1);
            agentManager.updateAgent(chain, agent);
            punishStorageService.delete(punishLogPo.getKey(), chainId);
            chain.getLogger().error("Red punish update agent deposit nonce error");
            return false;
        }

        //更新缓存
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
        找到被惩罚的节点
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

        //修改惩罚节点信息
        agent.setDelHeight(-1L);
        if(!agentManager.updateAgent(chain, agent)){
            AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true);
            chain.getLogger().error("Red punish tx agent data rollback error");
            return false;
        }

        //删除红牌惩罚信息
        byte[] key = ByteUtils.concatenate(punishData.getAddress(), new byte[]{PunishType.RED.getCode()}, SerializeUtils.uint64ToByteArray(blockHeight), new byte[]{0});
        if (!punishStorageService.delete(key, chainId)) {
            AgentDepositNonceManager.unLockTxCommit(chain, agent.getTxHash(), tx, true);
            agent.setDelHeight(blockHeight);
            agentManager.updateAgent(chain, agent);
            chain.getLogger().error("Data save error!");
            return false;
        }

        /*
         * 修改缓存
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
