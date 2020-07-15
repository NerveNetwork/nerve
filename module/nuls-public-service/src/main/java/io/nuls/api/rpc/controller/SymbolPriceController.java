package io.nuls.api.rpc.controller;

import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.config.ApiConfig;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.model.po.PageInfo;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolQuotationRecordInfo;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.VerifyUtils;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import org.bson.types.Symbol;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-17 10:33
 * @Description: 喂价及币种价格相关接口
 */
@Controller
public class SymbolPriceController {

    @Autowired
    SymbolQuotationPriceService symbolPriceService;

    @Autowired
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;
    @Autowired
    ApiConfig apiConfig;

    /**
     * 获取报价币种的最新价格
     *
     * @param params
     * @return
     */
    @RpcMethod("getQuotationCoinPrice")
    public RpcResult getQuotationCoinPrice(List<Object> params) {
//                "symbol": "",
//                "price": 1,             StackSymbolPriceInfo
//                "weight": 1,            共识模块RPC查询
//                "assetChainId": 1,      StackSymbolPriceInfo
//                "assetId": ""           StackSymbolPriceInfo
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(ApiConstant.USD);
        return RpcResult.success(symbolPriceService.getAllFreshPrice().stream().filter(d->d.getAssetChainId() != null && d.getAssetId() != null).map(d -> {
            Log.info("喂价信息：{}",d);
            double weight = 1;
            if(apiConfig.getChainId() == d.getAssetChainId() && d.getAssetId() == apiConfig.getAssetId()){
                weight = ApiContext.localAssertBase;
            } else if (d.getAssetChainId() == apiConfig.getMainChainId() && d.getAssetId() == apiConfig.getMainAssetId()){
                weight = ApiContext.mainAssertBase;
            }
            return Map.of(
                    "symbol", d.getSymbol(),
//                    "price", d.getPrice().movePointRight(ApiConstant.USDT_DECIMAL),
                    "price",  usd.getPrice().multiply(d.getPrice(), MathContext.DECIMAL64).setScale(ApiConstant.USD_DECIMAL, RoundingMode.HALF_DOWN),
                    "weight", weight,
                    "assetChainId", d.getAssetChainId(),
                    "assetId", d.getAssetId()
            );
        }).collect(Collectors.toList()));
    }




    /**
     * 获取报价记录
     *
     * @param params
     * @return
     */
    @RpcMethod("getLastQuotationRecord")
    public RpcResult getLastQuotationRecord(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String symbol;
        try {
            symbol = params.get(1).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[symbol] is inValid");
        }
        List<SymbolQuotationRecordInfo> data = symbolPriceService.queryLastQuotationList(symbol);
        return RpcResult.success(data.stream().map(this::buildRes).collect(Collectors.toList()));
    }


    /**
     * 获取报价记录
     *
     * @param params
     * @return
     */
    @RpcMethod("getQuotationRecord")
    public RpcResult getQuotationRecord(List<Object> params) {
        //从报价流水中获取，目前还未存储此交易类型
//                "nodeName", "",
//                "nodeHash", "",
//                "price", 1,
//                "symbol", "",
//                "createdTime", 1
        VerifyUtils.verifyParams(params, 2);
        int pageNumber, pageSize;
        long start,end;
        String symbol;
        try {
            symbol = params.get(1).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[symbol] is inValid");
        }
        try {
            pageNumber = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            if(params.size() > 4){
                start = (long) params.get(4);
            }else{
                start = 0L;
            }
        } catch (Exception e) {
            return RpcResult.paramError("[startTime] is inValid");
        }
        try {
            if(params.size() > 5){
                end = (long) params.get(5);
            }else{
                end = System.currentTimeMillis();
            }
        } catch (Exception e) {
            return RpcResult.paramError("[endTime] is inValid");
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<SymbolQuotationRecordInfo> data = symbolPriceService.queryQuotationList(symbol, pageNumber,pageSize,start,end);
        return RpcResult.success(new PageInfo<Map>(data.getPageNumber(),data.getPageSize(),data.getTotalCount(),
                data.getList().stream().map(this::buildRes).collect(Collectors.toList())));
    }

    private Map<String,Object> buildRes(SymbolQuotationRecordInfo d){
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(ApiConstant.USD);
        return Map.of(
                "nodeName" , StringUtils.isNotBlank(d.getAlias()) ? d.getAlias() : d.getAddress().substring(d.getAddress().length()-8).toUpperCase(),
                "price" , usd.getPrice().multiply(d.getPrice(), MathContext.DECIMAL64).setScale(ApiConstant.USD_DECIMAL, RoundingMode.HALF_DOWN),
                "symbol" , d.getSymbol(),
                "createdTime", d.getCreateTime()
        );
    }

}
