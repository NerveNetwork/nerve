package io.nuls.api.task;

import com.mongodb.Function;
import com.mongodb.client.model.Filters;
import io.nuls.api.ApiContext;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.AccountLedgerService;
import io.nuls.api.db.DepositService;
import io.nuls.api.model.dto.Asset;
import io.nuls.api.model.dto.AssetAndDepositType;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.DepositInfo;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.math.BigInteger;
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

    public ReportTask() {
        this.accountLedgerService = SpringLiteContext.getBean(AccountLedgerService.class);
        this.reportProvider = SpringLiteContext.getBean(ReportProvider.class);
        this.symbolUsdtPriceProviderService = SpringLiteContext.getBean(SymbolUsdtPriceProviderService.class);
    }

    @Override
    public void run() {
        List<DepositInfo> depositInfoList = getDepositList();
        reportProvider.setDepositGroupByAssetAndType(group(depositInfoList,d->d.getSymbol() + "-" + getType(d)));
        reportProvider.setDepositGroupByAsset(group(depositInfoList,d->d.getSymbol() + "-"));
        reportProvider.setDepositGroupByType(group(depositInfoList,d->"-" + getType(d)));
        calcAssetTotal();
        calcDepositAddressCount(depositInfoList);
    }

    private void calcAssetTotal() {
        List<Document> totalAccountBalance = accountLedgerService.getAllBalance(ApiContext.defaultChainId);
        reportProvider.setAssetTotal(totalAccountBalance.stream().map(d -> {
            int assetChainId = (int) d.get("chainId");
            int assetId = (int) d.get("assetId");
            Asset asset = new Asset();
            asset.setAssetId(assetId);
            asset.setChainId(assetChainId);
            BigInteger balance = new BigInteger(d.get("balance").toString());
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
            SymbolUsdPercentDTO dto = symbolUsdtPriceProviderService.calcRate(vi,tempData);
            AssetAndDepositType assetAndTimeType = new AssetAndDepositType();
            String[] keyAry = vi.split("-");
            assetAndTimeType.setSymbol(keyAry[0]);
            assetAndTimeType.setType(keyAry[1]);
            resData.put(assetAndTimeType,dto);
            if(maxPer == null){
                maxPer = dto;
            }else if(maxPer.getPer().compareTo(dto.getPer()) < 0){
                maxPer = dto;
            }
            totalPer = totalPer.add(dto.getPer());
        }
        if(totalPer.compareTo(BigDecimal.ONE) < 0){
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



}
