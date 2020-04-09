package io.nuls.dex.rpc.call;

import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.RpcCall;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexRpcConstant;
import io.nuls.dex.model.bean.AssetInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrossChainCall {

    /**
     * 查询已注册跨链的资产信息
     *
     * @return
     */
    public static Result<List<AssetInfo>> getCrossAssetInfoList() {
        try {
            Map<String, Object> map = (Map) RpcCall.request(ModuleE.CC.abbr, DexRpcConstant.GET_REGISTERED_CHAIN, new HashMap());
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) map.get("list");

            List<AssetInfo> assetInfoList = new ArrayList<>();

            for (Map<String, Object> resultMap : resultList) {
                List<Map<String, Object>> assetList = (List<Map<String, Object>>) resultMap.get("assetInfoList");
                if (assetList != null) {
                    for (Map<String, Object> assetMap : assetList) {
                        AssetInfo assetInfo = new AssetInfo();
                        assetInfo.setAssetChainId((Integer) resultMap.get("chainId"));
                        assetInfo.setAssetId((Integer) assetMap.get("assetId"));
                        assetInfo.setSymbol((String) assetMap.get("symbol"));
                        assetInfo.setDecimal((Integer) assetMap.get("decimalPlaces"));
                        boolean usable = (boolean) assetMap.get("usable");
                        if (usable) {
                            assetInfo.setStatus(DexConstant.ASSET_ENABLE);
                        } else {
                            assetInfo.setStatus(DexConstant.ASSET_DISABLE);
                        }
                        assetInfoList.add(assetInfo);
                    }
                }
            }
            return Result.getSuccess(null).setData(assetInfoList);
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

}
