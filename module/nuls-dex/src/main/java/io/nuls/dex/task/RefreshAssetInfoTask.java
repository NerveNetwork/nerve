package io.nuls.dex.task;

import io.nuls.core.basic.Result;
import io.nuls.core.log.Log;
import io.nuls.dex.context.DexContext;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.model.bean.AssetInfo;
import io.nuls.dex.rpc.call.CrossChainCall;
import io.nuls.dex.rpc.call.LedgerCall;
import io.nuls.dex.util.LoggerUtil;

import java.util.List;

/**
 * 每分钟一次，定时刷新资产列表
 * 1.刷新本链资产列表
 * 2.刷新已注册跨链资产列表
 */
public class RefreshAssetInfoTask implements Runnable {

    private int chainId;

    private DexManager dexManager;

    public RefreshAssetInfoTask(int chainId, DexManager dexManager) {
        this.chainId = chainId;
        this.dexManager = dexManager;
    }

    @Override
    public void run() {
        //查询本链已注册资产信息
        try {
            Result<List<AssetInfo>> result = LedgerCall.getLocalAssets(chainId);
            if (result.isSuccess()) {
                List<AssetInfo> assetInfoList = result.getData();
                for (AssetInfo assetInfo : assetInfoList) {
                    dexManager.addAssetInfo(assetInfo);
                }
            }
            //查询已注册跨链资产信息
            result = CrossChainCall.getCrossAssetInfoList();
            if (result.isSuccess()) {
                List<AssetInfo> assetInfoList = result.getData();
                //只有查询到已注册跨链资产，才能正常启动本模块
                if (assetInfoList.size() == 0) {
                    LoggerUtil.dexLog.info("---------无法获取跨链模块已注册资产信息----------");
                    return;
                }
                for (AssetInfo assetInfo : assetInfoList) {
                    //过滤掉本链的资产信息
                    if (assetInfo.getAssetChainId() != chainId) {
                        dexManager.addAssetInfo(assetInfo);
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
        }
    }
}
