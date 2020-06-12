package io.nuls.api.task;


import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.AssetInfo;
import io.nuls.api.model.po.ChainInfo;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.utils.DBUtil;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.Result;
import io.nuls.core.core.ioc.SpringLiteContext;

import java.util.*;

import static io.nuls.api.constant.ApiConstant.ENABLE;

public class QueryChainInfoTask implements Runnable {

    private int chainId;


    public QueryChainInfoTask(int chainId) {
        this.chainId = chainId;
    }

    @Override
    public void run() {
        Map<Integer, ChainInfo> chainInfoMap;
        Map<String, AssetInfo> assetInfoMap;
        try {
            chainInfoMap = new HashMap<>();
            assetInfoMap = new HashMap<>();
            ApiCache apiCache = CacheManager.getCache(chainId);
            ChainInfo chainInfo = apiCache.getChainInfo();
            chainInfoMap.put(chainInfo.getChainId(), chainInfo);
            assetInfoMap.put(chainInfo.getDefaultAsset().getKey(), chainInfo.getDefaultAsset());
            SymbolRegService symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
            //去账本同步资产信息
            symbolRegService.updateSymbolRegList();
//            if (ApiContext.isRunCrossChain) {
//                //获取生态内跨链资产列表
//                Result<Map<String, Object>> result = WalletRpcHandler.getRegisteredChainInfoList();
//                Map<String, Object> map = result.getData();
//                chainInfoMap.putAll((Map<Integer, ChainInfo>) map.get("chainInfoMap"));
//                assetInfoMap.putAll((Map<String, AssetInfo>) map.get("assetInfoMap"));
//            }
//

//            //将生态内跨链的资产信息刷新到资产注册信息表中
//            assetInfoMap.entrySet().forEach(entry -> {
//                AssetInfo ai = entry.getValue();
//                SymbolRegInfo symbolRegInfo = symbolRegService.get(entry.getValue().getChainId(), entry.getValue().getAssetId());
//                if (symbolRegInfo == null) {
//                    symbolRegInfo = new SymbolRegInfo();
//                    symbolRegInfo.setDecimals(ai.getDecimals());
//                    symbolRegInfo.setSymbol(ai.getSymbol());
//                    symbolRegInfo.setLevel(ApiConstant.SYMBOL_REG_SOURCE_CC);
//                    symbolRegInfo.setSource(symbolRegInfo.getLevel());
//                    symbolRegInfo.setAssetId(ai.getAssetId());
//                    symbolRegInfo.setChainId(ai.getChainId());
//                    symbolRegInfo.setFullName(ai.getSymbol());
//                    symbolRegInfo.setStackWeight(0);
//                    symbolRegService.save(symbolRegInfo);
//                } else {
//                    if (symbolRegInfo.getSource().equals(ApiConstant.SYMBOL_REG_SOURCE_CC)) {
//                        symbolRegInfo.setDecimals(ai.getDecimals());
//                        symbolRegInfo.setSymbol(ai.getSymbol());
//                        symbolRegInfo.setFullName(ai.getSymbol());
//                        symbolRegService.save(symbolRegInfo);
//                    }
//                }
//            });
            //获取所有资产注册信息放入内存中
            symbolRegService.getAll().stream().sorted(Comparator.comparing(d -> d.getAssetId())).forEach(d -> {
                if (d.getAssetId() == 1) {
                    if (!chainInfoMap.containsKey(d.getChainId())) {
                        ChainInfo newChainInfo = new ChainInfo();
                        newChainInfo.setAssets(new HashSet<>());
                        newChainInfo.setChainId(d.getChainId());
                        newChainInfo.setChainName(d.getSymbol());
                        newChainInfo.setSeeds(new ArrayList<>());
                        newChainInfo.setStatus(ENABLE);
                        chainInfoMap.put(d.getChainId(), newChainInfo);
                    }
                }
                AssetInfo assetInfo = new AssetInfo(d.getChainId(), d.getAssetId(), d.getSymbol(), d.getDecimals());
                String key = DBUtil.getAssetKey(d.getChainId(), d.getAssetId());
                assetInfoMap.put(key, assetInfo);
                if (d.getChainId() > 0) {
                    chainInfoMap.get(d.getChainId()).getAssets().add(assetInfo);
                }
            });
            CacheManager.setChainInfoMap(chainInfoMap);
            CacheManager.setAssetInfoMap(assetInfoMap);
            ApiContext.isReady = true;
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
    }
}
