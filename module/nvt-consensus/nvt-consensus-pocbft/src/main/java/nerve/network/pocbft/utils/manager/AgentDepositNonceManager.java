package nerve.network.pocbft.utils.manager;

import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
import nerve.network.pocbft.model.po.nonce.AgentDepositNoncePo;
import nerve.network.pocbft.model.po.nonce.NonceDataPo;
import nerve.network.pocbft.rpc.call.CallMethodUtils;
import nerve.network.pocbft.storage.AgentDepositNonceService;
import nerve.network.pocbft.utils.compare.NonceDataComparator;

import java.math.BigInteger;
import java.util.*;

@Component
public class AgentDepositNonceManager {
    @Autowired
    private static AgentDepositNonceService agentDepositNonceService;

    /**
     * 创建节点初始化节点NONCE数据
     * @param agent   节点信息
     * @param chain   链信息
     * @param agentHash  交易HASH
     * */
    public static boolean init(Agent agent, Chain chain, NulsHash agentHash){
        //初始化账户节点保证金NONCE数据
        NonceDataPo dataPo = new NonceDataPo(agent.getDeposit(), CallMethodUtils.getNonce(agentHash.getBytes()));
        if(!agentDepositNonceService.save(agentHash, new AgentDepositNoncePo(dataPo), chain.getChainId())){
            chain.getLogger().error("Agent deposit nonce data save error");
            return false;
        }
        return true;
    }

    /**
     * 创建节点回滚删除节点NONCE数据
     * @param chain      链信息
     * @param agentHash  交易HASH
     * */
    public static boolean delete(Chain chain, NulsHash agentHash){
        if(!agentDepositNonceService.delete(agentHash, chain.getChainId())){
            chain.getLogger().error("Agent deposit nonce data remove error");
            return false;
        }
        return true;
    }

    /**
     * 追加保证金NONCE提交
     * @param chain       链信息
     * @param agentHash   节点HASH
     * @param txHash      交易Hash
     * @param deposit     保证金金额
     * */
    public static boolean addNonceCommit(Chain chain, NulsHash agentHash, NulsHash txHash, BigInteger deposit){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
        NonceDataPo dataPo = new NonceDataPo(deposit, CallMethodUtils.getNonce(txHash.getBytes()));
        noncePo.getValidNonceList().add(dataPo);
        return agentDepositNonceService.save(agentHash, noncePo, chain.getChainId());
    }

    /**
     * 追加保证金NONCE回滚
     * @param chain       链信息
     * @param agentHash   节点HASH
     * @param txHash      交易Hash
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
     * 获取退出保证金/停止节点/红牌交易组装的NONCE数据列表
     * @param chain       链信息
     * @param agentHash   节点HASH
     * @param deposit     保证金金额
     * @param quitAll     是否退出所有
     * */
    public static List<NonceDataPo> getNonceDataList(Chain chain, BigInteger deposit, NulsHash agentHash, boolean quitAll){
        AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
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
     * CoinData  NONCE验证
     * @param chain      链信息
     * @param coinData   coin data
     * @param agentHash  节点HASH
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
     * 退出保证金/停止节点/红牌解锁交易提交
     * @param chain      链信息
     * @param agentHash  节点HASH
     * @param tx         交易信息
     * @param quitAll    是否退出所有
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
                //如果有新的锁定nonce产生，则保存新nonce
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
     * 退出保证金/停止节点/红牌解锁交易回滚
     * @param chain      链信息
     * @param agentHash  节点HASH
     * @param tx         交易信息
     * @param quitAll    是否退出所有
     * */
    public static boolean unLockTxRollback(Chain chain, NulsHash agentHash, Transaction tx, boolean quitAll){
        try {
            AgentDepositNoncePo noncePo = agentDepositNonceService.get(agentHash, chain.getChainId());
            CoinData coinData = tx.getCoinDataInstance();
            //如果为退还保证金交易且在退还组装中有新的锁定nonce产生，则先回滚新产生的nonce
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
            //回滚由有效转为失效的nonce数据
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
