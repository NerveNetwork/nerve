package network.nerve.pocbft.utils.manager;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.po.ChangeAgentDepositPo;
import network.nerve.pocbft.utils.compare.ChangeDepositComparator;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.storage.AppendDepositStorageService;
import network.nerve.pocbft.storage.ReduceDepositStorageService;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点保证变更管理类
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
     * 加载节点追加保证金信息
     * Add margin information to load node
     *
     * @param chain 链信息/chain info
     */
    public void loadAppendDeposits(Chain chain) throws NulsException {
        List<ChangeAgentDepositPo> appendDepositList = appendDepositStorageService.getList(chain.getChainId());
        //排序
        appendDepositList.sort(new ChangeDepositComparator());
        chain.setAppendDepositList(appendDepositList);
    }

    /**
     * 保存追加保证金记录
     * @param chain 链信息
     * @param po    追加保证金交易信息
     * @return      保存结果
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
     * 删除追加保证金记录
     * @param chain      链信息
     * @param txHash     追加保证金交易HASH
     * @return           是否删除成功
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
     * 获取指定高度之后的追加保证金金额
     * @param chain      链信息
     * @param height     区块高度
     * @return           各个节点在指定高度之后追加的保证金金额
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
     * 加载节点退出保证金信息
     * Add margin information to load node
     *
     * @param chain 链信息/chain info
     */
    public void loadReduceDeposits(Chain chain)throws NulsException{
        List<ChangeAgentDepositPo> reduceDepositList = reduceDepositStorageService.getList(chain.getChainId());
        //排序
        reduceDepositList.sort(new ChangeDepositComparator());
        chain.setReduceDepositList(reduceDepositList);
    }

    /**
     * 保存退出保证金记录
     * @param chain 链信息
     * @param po    追加保证金交易信息
     * @return      保存结果
     * */
    public boolean saveReduceDeposit(Chain chain, ChangeAgentDepositPo po){
        if(!reduceDepositStorageService.save(po, chain.getChainId())){
            chain.getLogger().error("Append agent deposit record save error!");
            return false;
        }
        chain.getAppendDepositList().add(po);
        return true;
    }

    /**
     * 删除退出保证金记录
     * @param chain      链信息
     * @param txHash     追加保证金交易HASH
     * @return           是否删除成功
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
     * 获取指定高度之后的退出的保证金金额
     * @param chain      链信息
     * @param height     区块高度
     * @return           各个节点在指定高度之后追加的保证金金额
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
