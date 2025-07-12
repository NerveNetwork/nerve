package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.model.StringUtils;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.model.jsonrpc.RpcResultError;
import io.nuls.provider.rpctools.LegderTools;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class LegerController {


    @Autowired
    private LegderTools legderTools;

    @RpcMethod("getAssetInfo")
    public RpcResult getAssetInfo(List<Object> params) {
        if (params.size() < 3) {
            return RpcResult.paramError("parmas is inValid,chainId,assetChainId,assetId");
        }
        int chainId, assetChainId, assetId;
        try {
            chainId = (int) params.get(0);
            assetChainId = (int) params.get(1);
            assetId = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        Result<Map> result = legderTools.getAssetInfo(chainId, assetChainId, assetId);
        RpcResult rpcResult = new RpcResult();
        if (result.isFailed()) {
            return rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        if ((chainId == 9 && assetId == 160) || (chainId == 5 && assetId == 34)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "POL");
                data.put("assetName", "POL");
            }
        } else if (chainId == 9 && assetId == 769) {
            //MATIC_KIRA_LP
            //NERVE  |  ID: 9-769
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "POL_KIRA_LP");
                data.put("assetName", "POL_KIRA_LP");
            }
        } else if (chainId == 9 && assetId == 664) {
            //MATIC_XBNCH_LP
            //NERVE  |  ID: 9-664
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "POL_XBNCH_LP");
                data.put("assetName", "POL_XBNCH_LP");
            }
        } else if (chainId == 9 && assetId == 348) {
            //NULS_MATIC_LP
            //NERVE  |  ID: 9-348
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "NULS_POL_LP");
                data.put("assetName", "NULS_POL_LP");
            }
        } else if (chainId == 9 && assetId == 424) {
            //NVT_MATIC_LP
            //NERVE  |  ID: 9-424
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "NVT_POL_LP");
                data.put("assetName", "NVT_POL_LP");
            }
        } else if ((chainId == 9 && assetId == 448) || (chainId == 5 && assetId == 118)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "KAIA");
                data.put("assetName", "KAIA");
            }
        } else if (chainId == 9 && assetId == 657) {
            //FTM_KLAY_LP
            //NERVE  |  ID: 9-657
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "FTM_KAIA_LP");
                data.put("assetName", "FTM_KAIA_LP");
            }
        } else if (chainId == 9 && assetId == 502) {
            //NULS_KLAY_LP
            //NERVE  |  ID: 9-502
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "NULS_KAIA_LP");
                data.put("assetName", "NULS_KAIA_LP");
            }
        } else if (chainId == 9 && assetId == 649) {
            //NVT_KLAY_LP
            //NERVE  |  ID: 9-649
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "NVT_KAIA_LP");
                data.put("assetName", "NVT_KAIA_LP");
            }
        } else if ((chainId == 9 && assetId == 692) || (chainId == 5 && assetId == 148)) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "A");
                data.put("assetName", "A");
            }
        } else if (chainId == 9 && assetId == 695) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "NVT_A_LP");
                data.put("assetName", "NVT_A_LP");
            }
        } else if (chainId == 9 && assetId == 768) {
            Map<String, Object> data = result.getData();
            if (data != null) {
                data.put("assetSymbol", "A_KIRA_LP");
                data.put("assetName", "A_KIRA_LP");
            }
        }
        return rpcResult.setResult(result.getData());
    }

    @RpcMethod("getAllAsset")
    public RpcResult getAllAsset(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        Result<List> result = legderTools.getAllAsset(chainId);
        RpcResult rpcResult = new RpcResult();
        if (result.isFailed()) {
            return rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult.setResult(result.getList());
    }

    @RpcMethod("getHeterogeneousChainAssetInfo")
    public RpcResult getHeterogeneousChainAssetInfo(List<Object> params) {
        int heterogeneousChainId;
        String contractAddress;
        try {
            heterogeneousChainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            contractAddress = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[symbol] is inValid");
        }

        Result result = legderTools.getHeterogeneousChainAssetInfo(heterogeneousChainId, contractAddress);
        if (result.getData() == null) {
            return RpcResult.success(null);
        }
        Map data = (Map) result.getData();
        int chainId = (int) data.get("chainId");
        int assetId = (int) data.get("assetId");
        if ((chainId == 9 && assetId == 160) || (chainId == 5 && assetId == 34)) {
            data.put("symbol", "POL");
        } else if ((chainId == 9 && assetId == 448) || (chainId == 5 && assetId == 118)) {
            data.put("symbol", "KAIA");
        } else if ((chainId == 9 && assetId == 692) || (chainId == 5 && assetId == 148)) {
            data.put("symbol", "A");
        }
        return RpcResult.success(result.getData());
    }
}
