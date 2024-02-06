package network.nerve.dex.rpc.call;

import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.RpcCall;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.model.bean.AssetInfo;

import java.util.*;
import java.util.stream.Collectors;

public class CrossChainCall {

    /**
     * Query registered asset information
     *
     * @return
     */
    public static Result<Set<AssetInfo>> getAssetInfoList(int chainId) {
        try {
            Map params = new HashMap();
            params.put("chainId",chainId);
            Map data = (Map) RpcCall.request(ModuleE.LG.abbr, "lg_get_all_asset", params);
            List<Map<String,Object>> list = (List<Map<String, Object>>) data.get("assets");
            return Result.getSuccess(list.stream().map(d->{
                AssetInfo r = new AssetInfo();
                r.setAssetChainId((Integer) d.get("assetChainId"));
                r.setAssetId((Integer) d.get("assetId"));
                r.setSymbol(d.get("assetSymbol").toString());
                r.setDecimal((Integer) d.get("decimalPlace"));
                r.setStatus(DexConstant.ASSET_ENABLE);
                return r;
            }).collect(Collectors.toSet()));
//            Map<String, Object> map = (Map) RpcCall.request(ModuleE.CC.abbr, DexRpcConstant.GET_REGISTERED_CHAIN, new HashMap());
//            List<Map<String, Object>> resultList = (List<Map<String, Object>>) map.get("list");
//
//            List<AssetInfo> assetInfoList = new ArrayList<>();
//
//            for (Map<String, Object> resultMap : resultList) {
//                List<Map<String, Object>> assetList = (List<Map<String, Object>>) resultMap.get("assetInfoList");
//                if (assetList != null) {
//                    for (Map<String, Object> assetMap : assetList) {
//                        AssetInfo assetInfo = new AssetInfo();
//                        assetInfo.setAssetChainId((Integer) resultMap.get("chainId"));
//                        assetInfo.setAssetId((Integer) assetMap.get("assetId"));
//                        assetInfo.setSymbol((String) assetMap.get("symbol"));
//                        assetInfo.setDecimal((Integer) assetMap.get("decimalPlaces"));
//                        boolean usable = (boolean) assetMap.get("usable");
//                        if (usable) {
//                            assetInfo.setStatus(DexConstant.ASSET_ENABLE);
//                        } else {
//                            assetInfo.setStatus(DexConstant.ASSET_DISABLE);
//                        }
//                        assetInfoList.add(assetInfo);
//                    }
//                }
//            }
//            return Result.getSuccess(null).setData(assetInfoList);
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

}
