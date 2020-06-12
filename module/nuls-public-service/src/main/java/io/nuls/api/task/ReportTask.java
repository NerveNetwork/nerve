package io.nuls.api.task;

import io.nuls.api.ApiContext;
import io.nuls.api.db.AccountLedgerService;
import io.nuls.api.db.DepositService;
import io.nuls.api.model.dto.Asset;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import org.bson.Document;

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


    @Component
    public static class ReportProvider {
        /**
         * 缓存各个资产总量
         * 每小时统计一次
         */
        private Map<Asset, BigInteger> assetTotal = new HashMap<>();

        /**
         * 质押总量，按资产分组
         * 没小时统计一次
         */
        private Map<Asset, BigInteger> depositTotal = new HashMap<>();

        public Map<Asset, BigInteger> getAssetTotal() {
            return assetTotal;
        }

        public void setAssetTotal(Map<Asset, BigInteger> assetTotal) {
            this.assetTotal = assetTotal;
        }

        public Map<Asset, BigInteger> getDepositTotal() {
            return depositTotal;
        }

        public void setDepositTotal(Map<Asset, BigInteger> depositTotal) {
            this.depositTotal = depositTotal;
        }
    }

    @Override
    public void run() {
        AccountLedgerService accountLedgerService = SpringLiteContext.getBean(AccountLedgerService.class);
        DepositService depositService = SpringLiteContext.getBean(DepositService.class);
        ReportProvider reportProvider = SpringLiteContext.getBean(ReportProvider.class);
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
        reportProvider.setDepositTotal(depositService.getDepositSumList(ApiContext.defaultChainId).stream().map(d -> {
            Asset asset = new Asset();
            asset.setChainId(d.getAssetChainId());
            asset.setAssetId(d.getAssetId());
            return Map.of(asset, d.getAmount());
        }).reduce(new HashMap<>(), this::merge));
    }

    private Map<Asset, BigInteger> merge(Map<Asset, BigInteger> od, Map<Asset, BigInteger> nd) {
        nd.entrySet().forEach(e -> {
            od.merge(e.getKey(), e.getValue(), (ov, nv) -> ov.add(nv));
        });
        return od;
    }

}
