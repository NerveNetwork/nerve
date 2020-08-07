package io.nuls.api.utils;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.db.AgentService;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.CoinContextInfo;
import io.nuls.api.model.rpc.BalanceInfo;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.ioc.SpringLiteContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AssetTool {

    /**
     * 查询名义锁定地址的总额
     * @return
     */
    public static BigInteger getLockedTotal(){
        return Arrays.stream(ApiContext.LOCKED_ADDRESS).map(d->{
            if(AddressTool.getChainIdByAddress(d) == ApiContext.defaultChainId){
                return getBalanceByNerve(d);
            }else{
                return getBalanceByNuls(d);
            }
        }).reduce(BigInteger::add).orElse(BigInteger.ZERO);
    }

    private static BigInteger getBalanceByNuls(String d) {
        return SpringLiteContext.getBean(NulsApi.class).getBalanceByNulsNetwork(d,ApiContext.defaultChainId,ApiContext.defaultAssetId);
    }

    private static BigInteger getBalanceByNerve(String d) {
        BalanceInfo balanceInfo = WalletRpcHandler.getAccountBalance(ApiContext.defaultChainId,d,ApiContext.defaultChainId,ApiContext.defaultAssetId);
        if(balanceInfo == null){
            return BigInteger.ZERO;
        }
        return balanceInfo.getTotalBalance();
    }

    public static Map getNulsAssets() {
        ApiCache apiCache = CacheManager.getCache(ApiContext.defaultChainId);
        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
        Map<String, Object> map = new HashMap<>();
        map.put("trades", coinContextInfo.getTxCount());
        map.put("totalAssets", AssetTool.toDouble(coinContextInfo.getTotal()));
        BigInteger lockedTotal = getLockedTotal();
        map.put("circulation", AssetTool.toDouble(coinContextInfo.getCirculation().subtract(lockedTotal)));
        map.put("lockedTotal",AssetTool.toDouble(lockedTotal));
        map.put("deposit", AssetTool.toDouble(coinContextInfo.getConsensusTotal()));
        map.put("business", AssetTool.toDouble(coinContextInfo.getBusiness()));
        map.put("team", AssetTool.toDouble(coinContextInfo.getTeam()));
        map.put("community", AssetTool.toDouble(coinContextInfo.getCommunity()));
        map.put("unmapped", AssetTool.toDouble(coinContextInfo.getUnmapped()));
        map.put("dailyReward", AssetTool.toDouble(coinContextInfo.getDailyReward()));
        map.put("destroy", AssetTool.toDouble(coinContextInfo.getDestroy()));
        int consensusCount = apiCache.getCurrentRound().getMemberCount() - apiCache.getChainInfo().getSeeds().size();
        if (consensusCount < 0) {
            consensusCount = 0;
        }
        map.put("consensusNodes", consensusCount);
        long count = 0;
        if (apiCache.getBestHeader() != null) {
            AgentService agentService = SpringLiteContext.getBean(AgentService.class);
            if (agentService != null) {
                count = agentService.agentsCount(ApiContext.defaultChainId, apiCache.getBestHeader().getHeight());
            }
        }
        map.put("totalNodes", count);
        return map;
    }

    public static double toDouble(BigInteger value) {
        return new BigDecimal(value).movePointLeft(ApiContext.defaultDecimals).setScale(ApiContext.defaultDecimals, RoundingMode.HALF_DOWN).doubleValue();
    }

    public static String toCoinString(BigInteger value) {
        BigDecimal decimal = new BigDecimal(value).movePointLeft(ApiContext.defaultDecimals).setScale(ApiContext.defaultDecimals, RoundingMode.HALF_DOWN);
        DecimalFormat format = new DecimalFormat("0.########");
        return format.format(decimal);
    }


}
