package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.farm.FarmProvider;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.rpc.model.*;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.rpctools.FarmTools;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class FarmController {

    @Autowired
    private FarmTools farmTools;

    @RpcMethod("getFarmInfo")
    @ApiOperation(description = "according tohashqueryfarmdetails", order = 801)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "farmHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "farmHashofhexcharacter string"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getFarmInfo(List<Object> params) {
        int chainId;
        String farmHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            farmHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[farmHash] is inValid");
        }
        return new RpcResult().setResult(farmTools.getFarm(chainId,farmHash).getData());
    }


    @RpcMethod("getUserStakeInfo")
    @ApiOperation(description = "Based on address andfarmhashQuery pledge details", order = 802)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "farmHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "farmHashofhexcharacter string"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "User address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getUserStakeInfo(List<Object> params) {
        int chainId;
        String farmHash;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            farmHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[farmHash] is inValid");
        }
        try {
            address = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        return new RpcResult().setResult(farmTools.sw_getstakeinfo(chainId,farmHash, address).getData());
    }

    @RpcMethod("getFarmList")
    @ApiOperation(description = "Get Allfarmlist", order = 803)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getFarmList(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        return new RpcResult().setResult(farmTools.getFarmList(chainId).getList());
    }
}
