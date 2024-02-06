package network.nerve.dex.task;

import io.nuls.core.basic.Result;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.bean.AssetInfo;
import network.nerve.dex.rpc.call.LedgerCall;
import network.nerve.dex.util.LoggerUtil;

import java.util.List;

/**
 * Every minute, refresh the asset list regularly
 * 1.Refresh the asset list of this chain
 * 2.Refresh registered cross chain asset list
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
        //Query registered asset information on this chain
        try {
            Result<List<AssetInfo>> result = LedgerCall.getAllAssets(chainId);
            if (result.isSuccess()) {
                List<AssetInfo> assetInfoList = result.getData();
                for (AssetInfo assetInfo : assetInfoList) {
                    dexManager.addAssetInfo(assetInfo);
                }
            }
//            //Query registered cross chain asset information
//            Result<Set<AssetInfo>> result = CrossChainCall.getAssetInfoList(chainId);
//            if (result.isSuccess()) {
//                Set<AssetInfo> assetInfoList = result.getData();
//                //Only when registered cross chain assets are found can this module be launched normally
//                if (assetInfoList.size() == 0) {
//                    LoggerUtil.dexLog.info("---------Unable to obtain registered asset information----------");
//                    return;
//                }
//                for (AssetInfo assetInfo : assetInfoList) {
//                    //Filter out the asset information of this chain
//                    dexManager.addAssetInfo(assetInfo);
//                }
//            }
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
        }
    }
}
