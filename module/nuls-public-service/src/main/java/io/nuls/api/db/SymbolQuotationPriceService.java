package io.nuls.api.db;

import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.StackSymbolPriceInfo;
import io.nuls.api.model.po.SymbolQuotationRecordInfo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 12:12
 * @Description: 币种兑USDT的汇率
 */
public interface SymbolQuotationPriceService {

    String USDT = "USDT";

    void saveFinalQuotation(List<StackSymbolPriceInfo> list);

    void saveQuotation(List<StackSymbolPriceInfo> list);

    PageInfo<SymbolQuotationRecordInfo> queryQuotationList(String symbol, int pageIndex, int pageSize,long startTime,long endTime);

    /**
     * 获取最新兑USDT报价
     * @param symbol
     * @return
     */
    StackSymbolPriceInfo getFreshUsdtPrice(String symbol);

    /**
     * 获取最新兑USDT报价
     * @param assetChainId
     * @param assetId
     * @return
     */
    StackSymbolPriceInfo getFreshUsdtPrice(int assetChainId, int assetId);

    /**
     * 获取最新报价
     * @param symbol
     * @param currency       计价资产
     * @return
     */
    StackSymbolPriceInfo getFreshPrice(String symbol, String currency);

    /**
     * 获取最新报价
     * @param assetChainId
     * @param assetId
     * @param currency      计价资产
     * @return
     */
    StackSymbolPriceInfo getFreshPrice(int assetChainId, int assetId, String currency);

    /**
     * 获取所有的币种的最新报价
     * @return
     */
    List<StackSymbolPriceInfo> getAllFreshPrice();

    /**
     * 查询最后一轮报价数据
     * @param symbol
     * @return
     */
    List<SymbolQuotationRecordInfo> queryLastQuotationList(String symbol);

    /**
     * 计算两个价格的涨跌幅百分比
     * @param oldVal
     * @param newVal
     * @return
     */
    public default BigDecimal calcChangeRate(BigDecimal oldVal,BigDecimal newVal){
        BigDecimal change;
        //如果获取最近一次报价的价格是0，则不计算与上一次的涨跌幅
        if(oldVal.equals(BigDecimal.ZERO)){
            change = BigDecimal.ZERO;
        }else{
            change = newVal.subtract(oldVal);
            change = change.divide(oldVal, MathContext.DECIMAL64).setScale(4, RoundingMode.HALF_DOWN);
        }
        return change;
    }

}
