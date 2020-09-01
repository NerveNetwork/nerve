package io.nuls.api.task;

import com.mongodb.Function;
import com.mongodb.client.model.Filters;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.AccountLedgerService;
import io.nuls.api.db.DepositService;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.Asset;
import io.nuls.api.model.dto.AssetAndDepositType;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.DepositInfo;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-03 14:38
 * @Description: 功能描述
 */
public class ReportTask implements Runnable {

    static final int STACK_ASSET_COUNT = 10;


    @Component
    public static class ReportProvider {

        /**
         * 缓存各个资产总量
         * 每小时统计一次
         */
        private Map<Asset, BigInteger> assetTotal = new HashMap<>();

        /**
         * 质押总量，按质押类型
         */
        private Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByType = new HashMap<>();

        /**
         * 质押总量，按资产分组
         * 没小时统计一次
         */
        private Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByAssetAndType = new HashMap<>();

        /**
         * 质押总量，按资产分组
         * 没小时统计一次
         */
        private Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByAsset = new HashMap<>();

        /**
         * 参与质押的地址总数
         */
        private long depositAddressCount = 0L;

        public Map<Asset, BigInteger> getAssetTotal() {
            return assetTotal;
        }

        public void setAssetTotal(Map<Asset, BigInteger> assetTotal) {
            this.assetTotal = assetTotal;
        }

        public long getDepositAddressCount() {
            return depositAddressCount;
        }

        public void setDepositAddressCount(long depositAddressCount) {
            this.depositAddressCount = depositAddressCount;
        }

        public Map<AssetAndDepositType, SymbolUsdPercentDTO> getDepositGroupByType() {
            return depositGroupByType;
        }

        public void setDepositGroupByType(Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByType) {
            this.depositGroupByType = depositGroupByType;
        }

        public Map<AssetAndDepositType, SymbolUsdPercentDTO> getDepositGroupByAssetAndType() {
            return depositGroupByAssetAndType;
        }

        public void setDepositGroupByAssetAndType(Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByAssetAndType) {
            this.depositGroupByAssetAndType = depositGroupByAssetAndType;
        }

        public Map<AssetAndDepositType, SymbolUsdPercentDTO> getDepositGroupByAsset() {
            return depositGroupByAsset;
        }

        public void setDepositGroupByAsset(Map<AssetAndDepositType, SymbolUsdPercentDTO> depositGroupByAsset) {
            this.depositGroupByAsset = depositGroupByAsset;
        }
    }

    AccountLedgerService accountLedgerService;
    ReportProvider reportProvider;
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;
    SymbolQuotationPriceService symbolPriceService;
    SymbolRegService symbolRegService;

    public ReportTask() {
        this.accountLedgerService = SpringLiteContext.getBean(AccountLedgerService.class);
        this.reportProvider = SpringLiteContext.getBean(ReportProvider.class);
        this.symbolUsdtPriceProviderService = SpringLiteContext.getBean(SymbolUsdtPriceProviderService.class);
        this.symbolPriceService = SpringLiteContext.getBean(SymbolQuotationPriceService.class);
        this.symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
    }

    @Override
    public void run() {

        try{
            List<DepositInfo> depositInfoList = getDepositList();
            calcDepositGroupByAssetAndType(depositInfoList);
            calcDepositGroupByAsset(depositInfoList);
            calcDepositGroupByType(depositInfoList);
            calcAssetTotal();
            calcDepositAddressCount(depositInfoList);
        }catch (Throwable e){
            Log.error("ReportTask统计质押数据发生异常：",e);
        }
    }

    private void calcDepositGroupByAssetAndType(List<DepositInfo> depositInfoList){
        Map<String,List<DepositInfo>> groupByAsset = new HashMap<>();
        depositInfoList.forEach(d->{
            groupByAsset.putIfAbsent(d.getSymbol(),new ArrayList<>());
            groupByAsset.get(d.getSymbol()).add(d);
        });
        Map<AssetAndDepositType, SymbolUsdPercentDTO> groupByAssetAndType = new HashMap<>();
        groupByAsset.values().forEach(list->{
            groupByAssetAndType.putAll(group(list,d->d.getSymbol() + "-" + getType(d)));
        });
        reportProvider.setDepositGroupByAssetAndType(groupByAssetAndType);
    }

    private void calcDepositGroupByType(List<DepositInfo> depositInfoList){
        Map<AssetAndDepositType, SymbolUsdPercentDTO> listGroupByType = group(toNvtTotalGroupByType(depositInfoList,d->ApiContext.defaultSymbol + "-" + getType(d)),d->ApiContext.defaultSymbol + "-" + getType(d));
        listGroupByType.entrySet().forEach(d->{
            d.getValue().setUsdVal(symbolUsdtPriceProviderService.toUsdValue(d.getKey().getSymbol(),
                    new BigDecimal(d.getValue().getAmount()).movePointLeft(ApiContext.defaultDecimals)).setScale(0,RoundingMode.HALF_DOWN));
        });
        reportProvider.setDepositGroupByType(listGroupByType);
    }

    private void calcDepositGroupByAsset(List<DepositInfo> depositInfoList){
        List<DepositInfo> transferNvtTotal = toNvtTotalGroupByType(depositInfoList,d->d.getSymbol() + "-NONE");
        Map<AssetAndDepositType, SymbolUsdPercentDTO> listGroupByAsset = group(transferNvtTotal,d->d.getSymbol() + "-NONE");
        Map<String,BigInteger> groupTotalByAsset = new HashMap<>(listGroupByAsset.size());
        depositInfoList.forEach(d->{
            groupTotalByAsset.compute(d.getSymbol(),(k,old)->{
                if(old == null){
                    return d.getAmount();
                }else{
                    return d.getAmount().add(old);
                }
            });
        });
        listGroupByAsset.entrySet().forEach(d->{
            d.getValue().setUsdVal(symbolUsdtPriceProviderService.toUsdValue(d.getKey().getSymbol(),
                    new BigDecimal(d.getValue().getAmount()).movePointLeft(ApiContext.defaultDecimals)).setScale(0,RoundingMode.HALF_DOWN));
            d.getValue().setAmount(groupTotalByAsset.get(d.getKey().getSymbol()));
            SymbolRegInfo symbolRegInfo = symbolRegService.getFirst(d.getKey().getSymbol()).get();
            d.getValue().setAmountBigUnit(new BigDecimal(d.getValue().getAmount()).movePointLeft(symbolRegInfo.getDecimals()).setScale(2,RoundingMode.HALF_DOWN));
        });
        reportProvider.setDepositGroupByAsset(listGroupByAsset);
    }

    private List<DepositInfo> toNvtTotalGroupByType(List<DepositInfo> depositInfoList,Function<DepositInfo,String> getKeyFun){
        Map<String,DepositInfo> resData = new HashMap<>();
        SymbolPrice nvtUsdtPrice = symbolPriceService.getFreshUsdtPrice(ApiContext.defaultChainId,ApiContext.defaultAssetId);
        depositInfoList.forEach(d->{
            BigInteger nvtTotal;
            if(d.getAssetChainId() == ApiContext.defaultChainId && d.getAssetId() == ApiContext.defaultAssetId){
                nvtTotal = d.getAmount();
            }else{
                SymbolPrice symbolPrice = symbolPriceService.getFreshUsdtPrice(d.getAssetChainId(),d.getAssetId());
                //将当前抵押资产转换成NVT
                nvtTotal = nvtUsdtPrice.transfer(symbolPrice,new BigDecimal(d.getAmount()).movePointLeft(d.getDecimal())).movePointRight(ApiContext.defaultDecimals).toBigInteger();
            }
            resData.compute(getKeyFun.apply(d),(k,od)->{
                if(od == null){
                    DepositInfo depositInfo = new DepositInfo();
                    depositInfo.setAmount(nvtTotal);
                    depositInfo.setAssetChainId(ApiContext.defaultChainId);
                    depositInfo.setAssetId(ApiContext.defaultAssetId);
                    depositInfo.setType(d.getType());
                    depositInfo.setFixedType(d.getFixedType());
                    depositInfo.setSymbol(d.getSymbol());
                    return depositInfo;
                }else{
                    od.setAmount(od.getAmount().add(nvtTotal));
                    return od;
                }
            });
        });
        return new ArrayList<>(resData.values());
    }

    private void calcAssetTotal() {
        List<Document> totalAccountBalance = accountLedgerService.getAllBalance(ApiContext.defaultChainId);
        reportProvider.setAssetTotal(totalAccountBalance.stream().map(d -> {
            int assetChainId = (int) d.get("chainId");
            int assetId = (int) d.get("assetId");
            Asset asset = new Asset();
            asset.setAssetId(assetId);
            asset.setChainId(assetChainId);
            BigInteger balance = new BigInteger(d.get("totalBalance").toString());
            return Map.of(asset, balance);
        }).reduce(new HashMap<>(), this::merge));
    }


    /**
     * 计算参与质押的地址总数
     * @param depositInfoList
     */
    private void calcDepositAddressCount(List<DepositInfo> depositInfoList) {
        long addressCount = depositInfoList.stream().map(d->d.getAddress()).distinct().count();
        reportProvider.setDepositAddressCount(addressCount);
    }

    private List<DepositInfo> getDepositList(){
        DepositService depositService = SpringLiteContext.getBean(DepositService.class);
        Bson typeFilter = Filters.or(
                Filters.eq("type", DepositInfoType.STACKING),
                Filters.eq("type",DepositInfoType.APPEND_AGENT_DEPOSIT),
                Filters.eq("type",DepositInfoType.CREATE_AGENT)
        );
        List<DepositInfo> depositList = depositService.getDepositList(ApiContext.defaultChainId,
                Filters.eq("deleteHeight", -1),
                typeFilter
        );
        return depositList;
    }

    private Map<AssetAndDepositType, SymbolUsdPercentDTO> group(List<DepositInfo> list, Function<DepositInfo,String> getKeyFun) {
        Map<String, BigInteger> tempData = new HashMap<>(STACK_ASSET_COUNT);
        list.forEach(d->{
            tempData.compute(getKeyFun.apply(d),(k,od)->{
                if(od == null){
                    return d.getAmount();
                }else{
                    return od.add(d.getAmount());
                }
            });
        });
        SymbolUsdPercentDTO maxPer = null;
        BigDecimal totalPer = BigDecimal.ZERO;
        Map<AssetAndDepositType, SymbolUsdPercentDTO> resData = new HashMap<>(tempData.size());
        for (String vi : tempData.keySet()){

            AssetAndDepositType assetAndTimeType = new AssetAndDepositType();
            String[] keyAry = vi.split("-");
            assetAndTimeType.setSymbol(keyAry[0]);
            assetAndTimeType.setType(keyAry[1]);
            SymbolRegInfo symbolRegInfo = symbolRegService.getFirst(assetAndTimeType.getSymbol()).get();
            SymbolUsdPercentDTO dto = calcRate(vi,tempData,symbolRegInfo);
            resData.put(assetAndTimeType,dto);
            if(maxPer == null){
                maxPer = dto;
            }else if(maxPer.getPer().compareTo(dto.getPer()) < 0){
                maxPer = dto;
            }
            totalPer = totalPer.add(dto.getPer());
        }

        if(totalPer.compareTo(BigDecimal.ZERO) > 0 && totalPer.compareTo(BigDecimal.ONE) < 0){
            maxPer.setPer(maxPer.getPer().add(BigDecimal.ONE.subtract(totalPer)));
        }
        return resData;
    }

    private String getType(DepositInfo depositInfo){
        switch (depositInfo.getType()){
            case DepositInfoType.APPEND_AGENT_DEPOSIT:
            case DepositInfoType.CREATE_AGENT:
            case DepositInfoType.REDUCE_AGENT_DEPOSIT:
            case DepositInfoType.STOP_AGENT:
                return "AGENT";
            default:
                return depositInfo.getFixedType();
        }
    }

    private Map<Asset, BigInteger> merge(Map<Asset, BigInteger> od, Map<Asset, BigInteger> nd) {
        nd.entrySet().forEach(e -> {
            od.merge(e.getKey(), e.getValue(), (ov, nv) -> ov.add(nv));
        });
        return od;
    }

    public SymbolUsdPercentDTO calcRate(String key, Map<String, BigInteger> list, SymbolRegInfo symbolRegInfo){
        Map<String,BigInteger> symbolUsdTxTotalMap = new HashMap<>(list.size());
        BigInteger allSymbolTxTotalUsdValue =list.entrySet().stream().map(d->{
            symbolUsdTxTotalMap.put(d.getKey(),d.getValue());
            return d.getValue();
        }).reduce(BigInteger::add).orElse(BigInteger.ZERO);
        BigDecimal rate;
        BigInteger usdValue = symbolUsdTxTotalMap.getOrDefault(key,BigInteger.ZERO);
        if(allSymbolTxTotalUsdValue.compareTo(BigInteger.ZERO) == 0){
            rate = BigDecimal.ONE;
        }else if (usdValue.compareTo(BigInteger.ZERO) == 0){
            rate = BigDecimal.ZERO;
        }else {
            rate = new BigDecimal(symbolUsdTxTotalMap.get(key)).divide(new BigDecimal(allSymbolTxTotalUsdValue),ApiConstant.RATE_DECIMAL, RoundingMode.HALF_DOWN);
        }
        SymbolUsdPercentDTO res = new SymbolUsdPercentDTO();
        res.setPer(rate);
        res.setAmount(list.getOrDefault(key,BigInteger.ZERO));
        res.setAmountBigUnit(new BigDecimal(res.getAmount()).movePointLeft(symbolRegInfo.getDecimals()).setScale(2,RoundingMode.HALF_DOWN));
        return res;
    }


}
