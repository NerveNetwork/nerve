package io.nuls.api.manager;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.model.dto.AssetBaseInfo;
import io.nuls.api.model.po.AssetInfo;
import io.nuls.core.basic.Result;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 11:43
 * @Description: asset基础信息管理
 */
public class AssetManager {

    private static final Map<String,AssetBaseInfo> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取资产基础信息
     * @param assetChainId
     * @param assetId
     * @return
     */
    public static AssetBaseInfo getAssetBaseInfo(int assetChainId,int assetId){
        String key = getKey(assetChainId,assetId);
        AssetBaseInfo info = CACHE.get(getKey(assetChainId,assetId));
        if(info != null){
            return info;
        }
        if(assetChainId == ApiContext.defaultChainId && assetId == ApiContext.defaultAssetId){
            info = new AssetBaseInfo();
            info.setChainId(assetChainId);
            info.setAssetId(assetId);
            info.setSymbol(ApiContext.defaultSymbol);
            info.setDecimals(ApiContext.defaultDecimals);
            info.setOfficial(true);
            CACHE.put(getKey(assetChainId,assetId),info);
            return info;
        }
        if(CacheManager.getAssetInfoMap().containsKey(key)){
            AssetInfo assetInfo = CacheManager.getRegisteredAsset(key);
            AssetBaseInfo baseInfo = new AssetBaseInfo();
            baseInfo.setOfficial(false);
            baseInfo.setDecimals(assetInfo.getDecimals());
            baseInfo.setAssetId(assetId);
            baseInfo.setChainId(assetChainId);
            baseInfo.setSymbol(assetInfo.getSymbol());
            baseInfo.setAddress(assetInfo.getAddress());
            CACHE.put(key,baseInfo);
            return baseInfo;
        }
        Result<Map> res = WalletRpcHandler.queryAssetInfo(assetChainId,assetId);
        if(res.isFailed()){
            return null;
        }
        return null;
    }

    private static String getKey(int assetChainId,int assetId){
        return assetChainId + ApiConstant.SPACE + assetId;
    }

}
