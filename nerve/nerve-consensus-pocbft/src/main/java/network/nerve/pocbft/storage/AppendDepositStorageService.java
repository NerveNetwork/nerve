package network.nerve.pocbft.storage;

import network.nerve.pocbft.model.po.ChangeAgentDepositPo;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.util.List;

/**
 * 追加保证金数据库管理类
 * Additional margin database management
 *
 * @author  tag
 * 2019/10/23
 * */
public interface AppendDepositStorageService {
    /**
     * 保存追加保证金数据
     * Save additional margin data
     *
     * @param  po        追加保证金数据
     * @param chainID    链ID/chain id
     * @return boolean
     * */
    boolean save(ChangeAgentDepositPo po, int chainID);

    /**
     * 根据交易Hash查询追加保证金交易详细数据
     * Query the detailed data of margin call transaction according to the transaction hash
     *
     * @param  txHash    交易Hash
     * @param chainID    链ID/chain id
     * */
    ChangeAgentDepositPo get(NulsHash txHash, int chainID);

    /**
     * 删除指定追加保证金交易详细信息
     * Delete specified margin call transaction details
     *
     * @param txHash     交易Hash
     * @param chainID    链ID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash txHash,int chainID);


    /**
     * 获取所有追加保证金信息
     * Get all margin call information
     *
     * @param  chainID    链ID/chain id
     * @return 追加保证金列表
     * @exception Exception
     * @return    所有追加记录
     * */
    List<ChangeAgentDepositPo> getList(int chainID) throws NulsException;
}
