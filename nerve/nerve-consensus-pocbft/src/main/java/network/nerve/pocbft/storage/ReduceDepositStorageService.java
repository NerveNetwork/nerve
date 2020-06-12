package network.nerve.pocbft.storage;

import network.nerve.pocbft.model.po.ChangeAgentDepositPo;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.util.List;

/**
 * 减少保证金数据库管理类
 * Reduce margin database management
 *
 * @author  tag
 * 2019/10/23
 * */
public interface ReduceDepositStorageService {
    /**
     * 保存退出保证金数据
     * Save reduce margin data
     *
     * @param  po        追加保证金数据
     * @param chainID    链ID/chain id
     * @return boolean
     * */
    boolean save(ChangeAgentDepositPo po, int chainID);

    /**
     * 根据交易Hash查询退出保证金交易详细数据
     * Query exit margin transaction details according to transaction hash
     *
     * @param  txHash    交易Hash
     * @param chainID    链ID/chain id
     * @return           指定交易对应的详细信息
     * */
    ChangeAgentDepositPo get(NulsHash txHash, int chainID);

    /**
     * 删除指定退出保证金交易详细信息
     * Delete specified exit margin transaction details
     *
     * @param txHash     交易Hash
     * @param chainID    链ID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash txHash,int chainID);


    /**
     * 获取所有退出保证金信息
     * Get all exit margin call information
     *
     * @param  chainID               链ID/chain id
     * @exception NulsException      Data serialization exception
     * @return                       追加保证金列表
     * */
    List<ChangeAgentDepositPo> getList(int chainID) throws NulsException;
}
