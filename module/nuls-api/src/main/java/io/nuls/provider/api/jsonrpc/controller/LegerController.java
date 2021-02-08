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

@Controller
@Api(type = ApiType.JSONRPC)
public class LegerController {


    @Autowired
    private LegderTools legderTools;

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
        return RpcResult.success(result.getData());
    }
}
