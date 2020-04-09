package nerve.network.pocbft.storage;

import nerve.network.pocbft.model.po.AwardSettleRecordPo;

import java.util.Map;

public interface AwardSettleRecordService {
    /**
     * 保存指定链的共识奖励结算记录
     * Save configuration information for the specified chain
     *
     * @param recordPo          共识奖励结算记录类/Consensus award settlement records
     * @param chainID           链ID/chain id
     * @return                  保存是否成功/Is preservation successful?
     * @exception Exception     保存中途异常
     * */
    boolean save(AwardSettleRecordPo recordPo, int chainID)throws Exception;

    /**
     * 查询某条链的配置信息
     * Query the configuration information of a chain
     *
     * @param chainID 链ID/chain id
     * @return 配置信息类/config bean
     * */
    AwardSettleRecordPo get(int chainID);

    /**
     * 删除某条链的配置信息
     * Delete configuration information for a chain
     *
     * @param chainID 链ID/chain id
     * @return 删除是否成功/Delete success
     * */
    boolean delete(int chainID);

    /**
     * 获取当前节点所有的链信息
     * Get all the chain information of the current node
     *
     * @return 节点信息列表/Node information list
     * */
    Map<Integer, AwardSettleRecordPo> getList();
}
