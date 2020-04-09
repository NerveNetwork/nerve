package io.nuls.dex.rpc.call;

import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.RpcCall;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.context.DexRpcConstant;
import io.nuls.dex.model.bean.AssetInfo;
import io.nuls.dex.util.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LedgerCall {

    /**
     * 查询本链所有已注册资产（非合约资产）
     *
     * @param chainId
     * @return
     */
    public static Result<List<AssetInfo>> getLocalAssets(int chainId) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("assetType", 1);

        List<AssetInfo> assetInfos = new ArrayList<>();
        try {
            Map map = (Map) RpcCall.request(ModuleE.LG.abbr, DexRpcConstant.CALL_CHAIN_ASSET_REG_INFO, params);
            List<Map<String, Object>> assets = (List<Map<String, Object>>) map.get("assets");
            for (Map<String, Object> obj : assets) {
                AssetInfo assetInfo = new AssetInfo();
                assetInfo.setAssetChainId(chainId);
                assetInfo.setAssetId((Integer) obj.get("assetId"));
                assetInfo.setDecimal((Integer) obj.get("decimalPlace"));
                assetInfo.setSymbol((String) obj.get("assetSymbol"));
                assetInfo.setStatus(DexConstant.ASSET_ENABLE);

                assetInfos.add(assetInfo);
            }
            return Result.getSuccess(null).setData(assetInfos);
        } catch (NulsException e) {
            Log.error(e);
            return Result.getFailed(DexErrorCode.DATA_PARSE_ERROR);
        }
    }
}
