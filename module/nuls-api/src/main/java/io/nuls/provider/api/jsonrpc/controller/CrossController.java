package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.crosschain.CrossChainProvider;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;

@Controller
@Api(type = ApiType.JSONRPC)
public class CrossController {

    CrossChainProvider crossChainProvider = ServiceManager.get(CrossChainProvider.class);
    @RpcMethod("getRegisteredChainInfoList")
    public RpcResult getRegisteredChainInfoList(List<Object> params) {
        Result<List> result = crossChainProvider.getRegisteredChainInfoList();
        return RpcResult.success(result.getList());
    }
}
