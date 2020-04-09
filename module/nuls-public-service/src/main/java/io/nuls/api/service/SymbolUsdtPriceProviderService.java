package io.nuls.api.service;

import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:26
 * @Description: 通过外部交易所或其他相关接口获取指定币种兑USDT的汇率。
 * 支持的币种将持久化在monggo数据库，但汇率不进行持久化。只能获取最新指数价格，而不能获取历史价格。
 * 对于没有数据源的币种，返回汇率为0.
 */
public interface SymbolUsdtPriceProviderService {

    /**
     * 注册一种需要提供价格的币种
     * @param symbolRegInfo
     */
    void regSymbol(SymbolRegInfo symbolRegInfo);

    /**
     * 获取一种币种的USDT汇率
     * @param symbol
     * @return
     */
    SymbolPrice getSymbolPriceForUsdt(String symbol);

}
