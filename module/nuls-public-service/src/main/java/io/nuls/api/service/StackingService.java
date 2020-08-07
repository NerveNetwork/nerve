package io.nuls.api.service;

import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.db.StackSnapshootService;
import io.nuls.api.model.po.StackSnapshootInfo;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.rpc.RpcCall;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-22 15:16
 * @Description: 功能描述
 */
@Component
public class StackingService {

    public static class Item {

        private String symbol;

        private int assetChainId;

        private int assetId;

        private DepositFixedType timeType;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Item)) return false;

            Item item = (Item) o;

            if (assetChainId != item.assetChainId) return false;
            if (assetId != item.assetId) return false;
            if (!symbol.equals(item.symbol)) return false;
            return timeType.equals(item.timeType);
        }

        @Override
        public int hashCode() {
            int result = symbol.hashCode();
            result = 31 * result + assetChainId;
            result = 31 * result + assetId;
            result = 31 * result + timeType.hashCode();
            return result;
        }
    }

    private Map<Item,BigDecimal> assetInterestWeight = new HashMap<>();



    @Autowired
    StackSnapshootService stackSnapshootService;

    /**
     * 获取指定资产参与stacking的年化收益率
     * @param symbol
     * @return
     */
    public BigDecimal getAssetStackingRate(String symbol,DepositFixedType timeType){
        BigDecimal baseInterest = getBaseInterest();
        BigDecimal rate = assetInterestWeight
                .entrySet().stream()
                .filter(d->d.getKey().symbol.equals(symbol) && d.getKey().timeType.equals(timeType))
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException("not support symbol :" + symbol + ":" + timeType )).getValue();

        return new BigDecimal(Math.sqrt(rate.doubleValue())).multiply(baseInterest);
    }

    /**
     * 根据权重计算年化收益率
     * @param weight
     * @return
     */
    public BigDecimal getInterestRate(Double... weight){
        double totalWeight = Arrays.stream(weight).reduce(1D,(w1,w2)->w1 * w2);
        totalWeight = Math.sqrt(totalWeight);
        BigDecimal interest = getBaseInterest().multiply(new BigDecimal(totalWeight));
        return interest;
    }

    public BigDecimal getBaseInterest(){
        BigDecimal baseInterest;
        Optional<StackSnapshootInfo> stackSnapshootInfo = stackSnapshootService.getLastSnapshoot(ApiContext.defaultChainId);
        if(stackSnapshootInfo.isEmpty()){
            baseInterest = BigDecimal.ZERO;
        }else{
            baseInterest = stackSnapshootInfo.get().getBaseInterest();
        }
        return baseInterest;
    }

    /**
     * 获取可参与stacking的资产列表
     * @return
     */
    public List<SymbolRegInfo> getStackingAssetList(){
        return null;
    }

    /**
     * 获取指定资产参与stacking的收益权重
     * @param assetChainId
     * @param assetId
     * @param depositFixedType
     * @return
     */
    public Optional<BigDecimal> getAssetStackingTotalAddition(int assetChainId,int assetId,DepositFixedType depositFixedType){
       Optional<Map.Entry<Item,BigDecimal>> entry = assetInterestWeight
                .entrySet().stream()
                .filter(d->d.getKey().assetChainId == assetChainId && d.getKey().assetId == assetId && d.getKey().timeType.equals(depositFixedType))
                .findFirst();
       if(entry.isPresent()){
           return Optional.of(entry.get().getValue());
       }else{
           return Optional.empty();
       }
    }

    public void init() {
        Map res = null;
        try {
            res = (Map) RpcCall.request(ModuleE.CS.abbr, "cs_getRateAddition", Map.of());
            List<Map> list = (List<Map>) res.get("list");
            list.forEach(asset->{
                List<Map> detailList = (List<Map>) asset.get("detailList");
                detailList.forEach(item->{
                    Item i = new Item();
                    i.assetChainId = (int) asset.get("assetChainId");
                    i.assetId = (int) asset.get("assetId");
                    i.symbol = (String) asset.get("symbol");
                    byte timeType = ((Integer)item.get("timeType")).byteValue();
                    int depositType = (int) item.get("depositType");
                    if(depositType == 0){
                        i.timeType = DepositFixedType.NONE;
                    }else {
                        i.timeType = DepositFixedType.getValue(timeType);
                    }
                    assetInterestWeight.put(i,new BigDecimal(item.get("totalAddition").toString()));
                });
            });
        } catch (Exception e) {
            Log.error("调用rpc :cs_getRateAddition 异常",e);
            System.exit(0);
        }
    }
}
