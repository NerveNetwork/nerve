package io.nuls.consensus.utils.manager;

import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.po.ChangeAgentDepositPo;
import io.nuls.consensus.utils.compare.ChangeDepositComparator;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.storage.AppendDepositStorageService;
import io.nuls.consensus.storage.ReduceDepositStorageService;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node Guarantee Change Management
 * Node assurance change management
 *
 * @author tag
 * 2019/10/23
 */
@Component
public class AgentDepositManager {
    @Autowired
    private AppendDepositStorageService appendDepositStorageService;

    @Autowired
    private ReduceDepositStorageService reduceDepositStorageService;

    /**
     * Load node additional margin information
     * Add margin information to load node
     *
     * @param chain Chain information/chain info
     */
    public void loadAppendDeposits(Chain chain) throws NulsException {
        List<ChangeAgentDepositPo> appendDepositList = appendDepositStorageService.getList(chain.getChainId());
        //sort
        appendDepositList.sort(new ChangeDepositComparator());
        chain.setAppendDepositList(appendDepositList);
    }

    /**
     * Save additional margin deposit records
     * @param chain Chain information
     * @param po    Additional margin trading information
     * @return      Save Results
     * */
    public boolean saveAppendDeposit(Chain chain, ChangeAgentDepositPo po){
        if(!appendDepositStorageService.save(po, chain.getChainId())){
            chain.getLogger().error("Append agent deposit record save error!");
            return false;
        }
        chain.getAppendDepositList().add(po);
        return true;
    }


    /**
     * Delete additional margin deposit records
     * @param chain      Chain information
     * @param txHash     Additional margin tradingHASH
     * @return           Whether the deletion was successful
     * */
    public boolean removeAppendDeposit(Chain chain, NulsHash txHash){
        if(!appendDepositStorageService.delete(txHash, chain.getChainId())){
            chain.getLogger().error("Append agent deposit record rollback error!");
            return false;
        }
        chain.getAppendDepositList().removeIf(s -> s.getTxHash().equals(txHash));
        return true;
    }

    /**
     * Obtain the additional deposit amount after obtaining the specified height
     * @param chain      Chain information
     * @param height     block height
     * @return           The amount of margin added by each node after the specified height
     * */
    public Map<NulsHash, BigInteger> getAppendDepositAfterHeight(Chain chain, long height){
        Map<NulsHash, BigInteger> appendDepositMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        int appendDepositSize = chain.getAppendDepositList().size();
        ChangeAgentDepositPo po;
        for (int i = appendDepositSize - 1; i >= 0 ; i--){
            po = chain.getAppendDepositList().get(i);
            if(po.getBlockHeight() <= height){
                break;
            }
            BigInteger totalAppend = appendDepositMap.get(po.getAgentHash());
            if(totalAppend == null){
                totalAppend = po.getAmount();
            }else{
                totalAppend = totalAppend.add(po.getAmount());
            }
            appendDepositMap.put(po.getAgentHash(), totalAppend);
        }
        return appendDepositMap;
    }

    /**
     * Load node exit margin information
     * Add margin information to load node
     *
     * @param chain Chain information/chain info
     */
    public void loadReduceDeposits(Chain chain)throws NulsException{
        List<ChangeAgentDepositPo> reduceDepositList = reduceDepositStorageService.getList(chain.getChainId());
        //sort
        reduceDepositList.sort(new ChangeDepositComparator());
        chain.setReduceDepositList(reduceDepositList);
    }

    /**
     * Save exit margin record
     * @param chain Chain information
     * @param po    Additional margin trading information
     * @return      Save Results
     * */
    public boolean saveReduceDeposit(Chain chain, ChangeAgentDepositPo po){
        if(!reduceDepositStorageService.save(po, chain.getChainId())){
            chain.getLogger().error("Append agent deposit record save error!");
            return false;
        }
        chain.getReduceDepositList().add(po);
        return true;
    }

    /**
     * Delete exit margin record
     * @param chain      Chain information
     * @param txHash     Additional margin tradingHASH
     * @return           Whether the deletion was successful
     * */
    public boolean removeReduceDeposit(Chain chain, NulsHash txHash){
        if(!reduceDepositStorageService.delete(txHash, chain.getChainId())){
            chain.getLogger().error("Append agent deposit record rollback error!");
            return false;
        }
        chain.getAppendDepositList().removeIf(s -> s.getTxHash().equals(txHash));
        return true;
    }

    /**
     * Obtain the withdrawal deposit amount after obtaining the specified height
     * @param chain      Chain information
     * @param height     block height
     * @return           The amount of margin added by each node after the specified height
     * */
    public Map<NulsHash, BigInteger> getReduceDepositAfterHeight(Chain chain, long height){
        Map<NulsHash, BigInteger> reduceDepositMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
        int reduceDepositSize = chain.getReduceDepositList().size();
        ChangeAgentDepositPo po;
        for (int i = reduceDepositSize - 1; i >= 0 ; i--){
            po = chain.getReduceDepositList().get(i);
            if(po.getBlockHeight() <= height){
                break;
            }
            BigInteger totalReduce = reduceDepositMap.get(po.getAgentHash());
            if(totalReduce == null){
                totalReduce = po.getAmount();
            }else{
                totalReduce = totalReduce.add(po.getAmount());
            }
            reduceDepositMap.put(po.getAgentHash(), totalReduce);
        }
        return reduceDepositMap;
    }
}
