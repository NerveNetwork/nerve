package io.nuls.consensus.utils.manager;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.model.po.nonce.AgentDepositNoncePo;
import io.nuls.consensus.model.po.nonce.NonceDataPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.utils.compare.NonceDataComparator;
import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.storage.AgentDepositNonceService;

import java.math.BigInteger;
import java.util.*;

@Component
public class AgentDepositNonceManager {
    @Autowired
    private static AgentDepositNonceService agentDepositNonceService;

    /**
     * Create node initialization nodeNONCEdata
     * @param agent   Node information
     * @param chain   Chain information
     * @param agentHash  transactionHASH
     * */
    public static boolean init(Agent agent, Chain chain, NulsHash agentHash){
        //Initialize account node marginNONCEdata
        NonceDataPo dataPo = new NonceDataPo(agent.getDeposit(), CallMethodUtils.getNonce(agentHash.getBytes()));
        if(!agentDepositNonceService.save(agentHash, new AgentDepositNoncePo(dataPo), chain.getChainId())){
            chain.getLogger().error("Agent deposit nonce data save error");
            return false;
        }
        return true;
    }

    /**
     * Create node rollback delete nodeNONCEdata
     * @param chain      Chain information
     * @param agentHash  transactionHASH
     * */
    public static boolean delete(Chain chain, NulsHash agentHash){
        if(!agentDepositNonceService.delete(agentHash, chain.getChainId())){
            chain.getLogger().error("Agent deposit nonce data remove error");
            return false;
        }
        return true;
    }

    /**
     * Additional marginNONCESubmit
     * @param chain       Chain information
     * @param agentHash   nodeHASH
     * @param txHash      transactionHash
     * @param deposit     Deposit amount
     * */
    public static boolean addNonceCommit(Chain chain, NulsHash agentHash, NulsHash txHash, BigInteger deposit){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
        NonceDataPo dataPo = new NonceDataPo(deposit, CallMethodUtils.getNonce(txHash.getBytes()));
        noncePo.getValidNonceList().add(dataPo);
        return agentDepositNonceService.save(agentHash, noncePo, chain.getChainId());
    }

    /**
     * Additional marginNONCERollBACK
     * @param chain       Chain information
     * @param agentHash   nodeHASH
     * @param txHash      transactionHash
     * */
    public static boolean addNonceRollBack(Chain chain, NulsHash agentHash, NulsHash txHash){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
        byte[] nonce = CallMethodUtils.getNonce(txHash.getBytes());
        Iterator<NonceDataPo> iterator = noncePo.getValidNonceList().iterator();
        while (iterator.hasNext()){
            NonceDataPo po = iterator.next();
            if(Arrays.equals(nonce, po.getNonce())){
                iterator.remove();
                break;
            }
        }
        return agentDepositNonceService.save(agentHash, noncePo, chain.getChainId());
    }

    /**
     * Obtain exit deposit/Stop node/Assembled for red card tradingNONCEData List
     * @param chain       Chain information
     * @param agentHash   nodeHASH
     * @param deposit     Deposit amount
     * @param quitAll     Do you want to exit all
     * */
    public static List<NonceDataPo> getNonceDataList(Chain chain, BigInteger deposit, NulsHash agentHash, boolean quitAll){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
        if(noncePo == null || noncePo.getValidNonceList().isEmpty()){
            return new ArrayList<>();
        }
        noncePo.getValidNonceList().sort(new NonceDataComparator());
        if(quitAll){
            return noncePo.getValidNonceList();
        }
        List<NonceDataPo> dataPoList = new ArrayList<>();
        BigInteger totalDeposit = BigInteger.ZERO;
        for(NonceDataPo  dataPo : noncePo.getValidNonceList()){
            dataPoList.add(dataPo);
            totalDeposit = totalDeposit.add(dataPo.getDeposit());
            if(totalDeposit.compareTo(deposit) >= 0){
                break;
            }
        }
        return dataPoList;
    }

    /**
     * CoinData  NONCEvalidate
     * @param chain      Chain information
     * @param coinData   coin data
     * @param agentHash  nodeHASH
     * */
    public static boolean coinDataNonceVerify(Chain chain, CoinData coinData, NulsHash agentHash){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
        boolean isMatch;
        for (CoinFrom from : coinData.getFrom()){
            isMatch = false;
            for (NonceDataPo po : noncePo.getValidNonceList()){
                if(Arrays.equals(from.getNonce(), po.getNonce())){
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch){
                chain.getLogger().error("CoinData nonce matching failure");
                return false;
            }
        }
        return true;
    }

    /**
     * Withdrawal of margin/Stop node/Red card unlocking transaction submission
     * @param chain      Chain information
     * @param agentHash  nodeHASH
     * @param tx         Transaction information
     * @param quitAll    Do you want to exit all
     * */
    public static boolean unLockTxCommit(Chain chain, NulsHash agentHash, Transaction tx, boolean quitAll){
        try {
            AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
            if(noncePo.getInvalidNonceList() == null){
                noncePo.setInvalidNonceList(new ArrayList<>());
            }
            if(quitAll){
                noncePo.getInvalidNonceList().addAll(noncePo.getValidNonceList());
                noncePo.getValidNonceList().clear();
            }else{
                CoinData coinData = tx.getCoinDataInstance();
                noncePo.getValidNonceList().sort(new NonceDataComparator());
                Iterator<NonceDataPo> iterator = noncePo.getValidNonceList().iterator();
                while (iterator.hasNext()){
                    NonceDataPo po = iterator.next();
                    for(CoinFrom from : coinData.getFrom()){
                        if(Arrays.equals(po.getNonce(), from.getNonce())){
                            noncePo.getInvalidNonceList().add(po);
                            iterator.remove();
                            break;
                        }
                    }
                }
                //If there is a new locknonceIf generated, save the new onenonce
                if(coinData.getTo().size() > 1){
                    for(CoinTo to : coinData.getTo()){
                        if(to.getLockTime() == ConsensusConstant.CONSENSUS_LOCK_TIME){
                            NonceDataPo newNonceDataPo = new NonceDataPo(to.getAmount(), CallMethodUtils.getNonce(tx.getHash().getBytes()));
                            noncePo.getValidNonceList().add(newNonceDataPo);
                            break;
                        }
                    }
                }
            }
            return agentDepositNonceService.save(agentHash, noncePo, chain.getChainId());
        }catch (NulsException e){
            chain.getLogger().error(e);
            return false;
        }
    }

    /**
     * Withdrawal of margin/Stop node/Red card unlocking transaction rollback
     * @param chain      Chain information
     * @param agentHash  nodeHASH
     * @param tx         Transaction information
     * @param quitAll    Do you want to exit all
     * */
    public static boolean unLockTxRollback(Chain chain, NulsHash agentHash, Transaction tx, boolean quitAll){
        try {
            AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
            CoinData coinData = tx.getCoinDataInstance();
            //If it is a margin refund transaction and there is a new lock in the returned assemblynonceIf it is generated, roll back the newly generated one firstnonce
            if(!quitAll && coinData.getTo().size() > 1){
                byte[] newNonce = CallMethodUtils.getNonce(tx.getHash().getBytes());
                Iterator<NonceDataPo> iterator = noncePo.getValidNonceList().iterator();
                while (iterator.hasNext()){
                    NonceDataPo po = iterator.next();
                    if(Arrays.equals(newNonce, po.getNonce())){
                        iterator.remove();
                        break;
                    }
                }
            }
            //Rollback from valid to invalidnoncedata
            Iterator<NonceDataPo> iterator = noncePo.getInvalidNonceList().iterator();
            boolean isMatch;
            while (iterator.hasNext()){
                NonceDataPo po = iterator.next();
                isMatch = false;
                for(CoinFrom from : coinData.getFrom()){
                    if(Arrays.equals(po.getNonce(), from.getNonce())){
                        noncePo.getValidNonceList().add(po);
                        iterator.remove();
                        isMatch = true;
                        break;
                    }
                }
                if(!isMatch){
                    chain.getLogger().error("Corresponding nonce value not found in database");
                    return false;
                }
            }
            return agentDepositNonceService.save(agentHash, noncePo, chain.getChainId());
        }catch (NulsException e){
            chain.getLogger().error(e);
            return false;
        }
    }
}
