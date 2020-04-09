package io.nuls.dex.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.dex.model.po.CoinTradingPo;

import java.util.List;

/**
 * 币对存储服务
 */
public interface CoinTradingStorageService {

    /**
     * 存储一条币对信息
     *
     * @param tradingPo
     * @return
     */
    void save(CoinTradingPo tradingPo)throws Exception;

    /**
     * 根据创建币对的交易hash查询一条交易对信息
     *
     * @param hash
     * @return
     */
    CoinTradingPo query(NulsHash hash) throws NulsException;

    /**
     * 根据币对id查询币对信息
     *
     * @param coinTradingKey
     * @return
     */
    CoinTradingPo query(String coinTradingKey);

    /**
     * 查询所有币对信息
     *
     * @return
     */
    List<CoinTradingPo> queryAll() throws NulsException;


    void delete(CoinTradingPo tradingPo)throws Exception;


}
