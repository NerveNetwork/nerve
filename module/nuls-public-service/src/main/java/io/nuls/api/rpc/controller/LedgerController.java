package io.nuls.api.rpc.controller;

import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.Asset;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.task.ReportTask;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-03 14:02
 * @Description: 功能描述
 */
@Controller
public class LedgerController {

    @Autowired
    ReportTask.ReportProvider reportProvider;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;

    /**
     * 跨链资产总量分组汇总
     * @return
     */
    @RpcMethod("totalCrossAsset")
    public RpcResult totalCrossAsset(List<Object> param){
        return RpcResult.success(reportProvider.getAssetTotal().entrySet().stream().filter(d->d.getKey().getChainId() != ApiContext.defaultChainId).map(this::build).collect(Collectors.toList()));
    }

    /**
     * 跨链资产总量分组汇总
     * @return
     */
    @RpcMethod("totalDepositAsset")
    public RpcResult totalDepositAsset(List<Object> param){
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(ApiConstant.USD);
        return RpcResult.success(reportProvider.getDepositTotal().entrySet().stream().map(this::build).collect(Collectors.toList()));
    }

    private Map<String,Object> build(Map.Entry<Asset,BigInteger> entry){
        Asset asset = entry.getKey();
        BigInteger total = entry.getValue();
        SymbolRegInfo symbolRegInfo = symbolRegService.get(asset.getChainId(),asset.getAssetId());
        SymbolPrice symbolPrice = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(symbolRegInfo.getSymbol());
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(ApiConstant.USD);
        return Map.of("symbol",symbolRegInfo,"total",usd.transfer(symbolPrice,new BigDecimal(total).movePointLeft(symbolRegInfo.getDecimals())));
    }


}
