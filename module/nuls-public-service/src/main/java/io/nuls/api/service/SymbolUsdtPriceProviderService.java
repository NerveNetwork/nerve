package io.nuls.api.service;

import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:26
 * @Description: 通过外部交易所或其他相关接口获取指定币种兑USDT的汇率。
 * 支持的币种将持久化在monggo数据库，但汇率不进行持久化。只能获取最新指数价格，而不能获取历史价格。
 * 对于没有数据源的币种，返回汇率为0.
 */
public interface SymbolUsdtPriceProviderService {


    /**
     * 获取一种币种的USDT汇率
     * @param symbol
     * @return
     */
    SymbolPrice getSymbolPriceForUsdt(String symbol);

    /**
     * 转换成usd价值
     * @param symbol
     * @param amount
     * @return
     */
    BigDecimal toUsdValue(String symbol,BigDecimal amount);


    /**
     * 计算指定symbol在list中的usd价值占比
     * @param symbol
     * @param symbolList   symbol:amount
     * @return
     */
    SymbolUsdPercentDTO calcRate(String symbol, Map<String, BigInteger> symbolList);


}
