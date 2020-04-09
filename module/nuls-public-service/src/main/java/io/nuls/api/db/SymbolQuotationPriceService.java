package io.nuls.api.db;

import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.StackSymbolPriceInfo;
import io.nuls.api.model.po.SymbolQuotationRecordInfo;

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

    StackSymbolPriceInfo getFreshUsdtPrice(String symbol);

    StackSymbolPriceInfo getFreshUsdtPrice(int assetChainId, int assetId);

    StackSymbolPriceInfo getFreshPrice(String symbol, String currency);

    StackSymbolPriceInfo getFreshPrice(int assetChainId, int assetId, String currency);

    /**
     * 获取所有的币种的最新报价
     * @return
     */
    List<StackSymbolPriceInfo> getAllFreshPrice();

}
