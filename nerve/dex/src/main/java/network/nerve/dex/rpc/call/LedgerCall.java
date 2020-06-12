package network.nerve.dex.rpc.call;

import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.RpcCall;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.context.DexRpcConstant;
import network.nerve.dex.model.bean.AssetInfo;

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
    public static Result<List<AssetInfo>> getAllAssets(int chainId) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);

        List<AssetInfo> assetInfos = new ArrayList<>();
        try {
            Map map = (Map) RpcCall.request(ModuleE.LG.abbr, DexRpcConstant.CMD_GET_ALL_ASSET, params);
            List<Map<String, Object>> assets = (List<Map<String, Object>>) map.get("assets");
            for (Map<String, Object> obj : assets) {
                AssetInfo assetInfo = new AssetInfo();
                assetInfo.setAssetChainId((Integer) obj.get("assetChainId"));
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
