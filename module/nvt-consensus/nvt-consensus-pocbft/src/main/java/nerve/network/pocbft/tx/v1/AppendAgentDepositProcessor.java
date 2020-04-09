package nerve.network.pocbft.tx.v1;

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
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.bo.tx.txdata.ChangeAgentDepositData;
import nerve.network.pocbft.model.bo.tx.txdata.RedPunishData;
import nerve.network.pocbft.model.bo.tx.txdata.StopAgent;
import nerve.network.pocbft.model.po.ChangeAgentDepositPo;
import nerve.network.pocbft.utils.LoggerUtil;
import nerve.network.pocbft.utils.manager.AgentDepositManager;
import nerve.network.pocbft.utils.manager.AgentDepositNonceManager;
import nerve.network.pocbft.utils.manager.AgentManager;
import nerve.network.pocbft.utils.manager.ChainManager;
import nerve.network.pocbft.utils.validator.AppendAgentDepositValidator;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * CoinBase交易处理器
 * @author: Jason
 * @date 2019/10/22
 */
@Component("AppendAgentDepositProcessorV1")
public class AppendAgentDepositProcessor implements TransactionProcessor {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private AppendAgentDepositValidator validator;
    @Autowired
    private AgentDepositManager agentDepositManager;

    @Override
    public int getType() {
        return TxType.APPEND_AGENT_DEPOSIT;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            result.put("txList", txs);
            result.put("errorCode", ConsensusErrorCode.CHAIN_NOT_EXIST.getCode());
            return result;
        }
        List<Transaction> invalidTxList = new ArrayList<>();
        String errorCode = null;

        Set<String> invalidAgentSet = new HashSet<>();
        List<Transaction> redPunishTxList = txMap.get(TxType.RED_PUNISH);
        if(redPunishTxList != null && redPunishTxList.size() >0){
            for (Transaction redPunishTx:redPunishTxList) {
                RedPunishData redPunishData = new RedPunishData();
                try {
                    redPunishData.parse(redPunishTx.getTxData(), 0);
                    String redPunishAddress = AddressTool.getStringAddressByBytes(redPunishData.getAddress());
                    invalidAgentSet.add(redPunishAddress);
                }catch (NulsException e){
                    chain.getLogger().error(e);
                }
            }
        }
        List<Transaction> stopAgentTxList = txMap.get(TxType.STOP_AGENT);
        if(stopAgentTxList != null &&  !stopAgentTxList.isEmpty()){
            for (Transaction stopAgentTx : stopAgentTxList) {
                StopAgent stopAgent = new StopAgent();
                try {
                    stopAgent.parse(stopAgentTx.getTxData(),0);
                    String stopAgentAddress = AddressTool.getStringAddressByBytes(stopAgent.getAddress());
                    invalidAgentSet.add(stopAgentAddress);
                }catch (NulsException e){
                    chain.getLogger().error(e);
                }
            }
        }

        Map<NulsHash, BigInteger> appendTotalAmountMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_8);

        Result rs;
        for (Transaction tx : txs) {
            try {
                rs = validator.validate(chain, tx);
                if(rs.isFailed()){
                    invalidTxList.add(tx);
                    errorCode = rs.getErrorCode().getCode();
                    chain.getLogger().info("Failure to create node transaction validation");
                    continue;
                }

                ChangeAgentDepositData txData = new ChangeAgentDepositData();
                txData.parse(tx.getTxData(),0);
                if(invalidAgentSet.contains(AddressTool.getStringAddressByBytes(txData.getAddress()))){
                    invalidTxList.add(tx);
                    chain.getLogger().info("Error in conflict detection of additional margin");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }

                //委托金额超出节点最大委托金额
                NulsHash agentHash = txData.getAgentHash();
                if(appendTotalAmountMap.containsKey(agentHash)){
                    BigInteger totalDeposit = appendTotalAmountMap.get(agentHash).add(txData.getAmount());
                    if(totalDeposit.compareTo(chain.getConfig().getDepositMax())>0){
                        invalidTxList.add(tx);
                        chain.getLogger().info("The amount of additional margin is less than the minimum value");
                        errorCode = ConsensusErrorCode.DEPOSIT_OUT_OF_RANGE.getCode();
                    }else{
                        appendTotalAmountMap.put(agentHash, totalDeposit);
                    }
                }else{
                    Agent agent = agentManager.getAgentByHash(chain , agentHash);
                    appendTotalAmountMap.put(agentHash, agent.getDeposit().add(txData.getAmount()));
                }
            }catch (NulsException e){
                invalidTxList.add(tx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            }catch (IOException io){
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
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> commitSuccessList = new ArrayList<>();
        boolean commitResult = true;
        for (Transaction tx : txs) {
            if (appendDepositCommit(tx, blockHeader ,chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!commitResult) {
            for (Transaction commitTx : commitSuccessList) {
                appendDepositRollback(commitTx, blockHeader, chain);
            }
        }
        return commitResult;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> rollbackSuccessList = new ArrayList<>();
        boolean rollbackResult = true;
        for (Transaction tx : txs) {
            if (appendDepositRollback(tx, blockHeader ,chain)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                appendDepositCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }

    private boolean appendDepositCommit(Transaction tx, BlockHeader blockHeader, Chain chain){
        long height = blockHeader.getHeight();
        ChangeAgentDepositData data = new ChangeAgentDepositData();
        try {
            data.parse(tx.getTxData(),0);
        }catch (NulsException e){
            chain.getLogger().error(e);
            return false;
        }
        ChangeAgentDepositPo po = new ChangeAgentDepositPo(data, tx.getTime(), tx.getHash(), height);
        //保存追加记录
        if(!agentDepositManager.saveAppendDeposit(chain, po)){
            chain.getLogger().error("Failed to save the additional margin record");
            return false;
        }

        //修改节点保证金金额
        Agent agent = agentManager.getAgentByHash(chain, po.getAgentHash());
        BigInteger oldDeposit = agent.getDeposit();
        BigInteger newDeposit = oldDeposit.add(po.getAmount());
        agent.setDeposit(newDeposit);
        if(!agentManager.updateAgent(chain, agent)){
            agentDepositManager.removeAppendDeposit(chain, po.getTxHash());
            chain.getLogger().error("Agent deposit modification failed");
            return false;
        }

        //保存追加保证金交易NONCE
        if(!AgentDepositNonceManager.addNonceCommit(chain, po.getAgentHash(), tx.getHash(), po.getAmount())){
            agent.setDeposit(oldDeposit);
            agentManager.updateAgent(chain, agent);
            agentDepositManager.removeAppendDeposit(chain, po.getTxHash());
            chain.getLogger().error("Failed to save the additional margin nonce record");
            return false;
        }
        return true;
    }

    private boolean appendDepositRollback(Transaction tx, BlockHeader blockHeader, Chain chain){
        long height = blockHeader.getHeight();
        ChangeAgentDepositData data = new ChangeAgentDepositData();
        try {
            data.parse(tx.getTxData(),0);
        }catch (NulsException e){
            chain.getLogger().error(e);
            return false;
        }
        ChangeAgentDepositPo po = new ChangeAgentDepositPo(data, tx.getTime(), tx.getHash(), height);
        //修改节点保证金金额
        Agent agent = agentManager.getAgentByHash(chain, po.getAgentHash());
        BigInteger newDeposit = agent.getDeposit();
        BigInteger oldDeposit = newDeposit.subtract(po.getAmount());
        agent.setDeposit(oldDeposit);
        if(!AgentDepositNonceManager.addNonceRollBack(chain, po.getAgentHash(), tx.getHash())){
            chain.getLogger().error("Failed to rollback the additional margin nonce record");
            return false;
        }
        if(!agentManager.updateAgent(chain, agent)){
            AgentDepositNonceManager.addNonceCommit(chain, po.getAgentHash(), tx.getHash(), po.getAmount());
            chain.getLogger().error("Agent deposit modification failed");
            return false;
        }
        //删除追加记录
        if(!agentDepositManager.removeAppendDeposit(chain, po.getTxHash())){
            agent.setDeposit(newDeposit);
            agentManager.updateAgent(chain, agent);
            AgentDepositNonceManager.addNonceCommit(chain, po.getAgentHash(), tx.getHash(), po.getAmount());
            chain.getLogger().error("Failed to rollback the additional margin record");
            return false;
        }
        return true;
    }
}
