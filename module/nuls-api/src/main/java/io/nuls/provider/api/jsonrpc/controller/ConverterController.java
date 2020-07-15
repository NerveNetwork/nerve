package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.provider.api.config.Context;
import io.nuls.provider.model.dto.VirtualBankDirectorDTO;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.rpctools.ConverterTools;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.provider.utils.VerifyUtils;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class ConverterController {

    @Autowired
    ConverterTools converterTools;

    @RpcMethod("getEthAddress")
    @ApiOperation(description = "根据共识节点打包地址查询相应的以太坊地址", order = 601)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "共识节点打包地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousAddress", description = "异构链地址"),
    })
    )
    public RpcResult getEthAddress(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String packingAddress;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            packingAddress = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[packingAddress] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(packingAddress) || !AddressTool.validAddress(chainId, packingAddress)) {
            return RpcResult.paramError("[packingAddress] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousAddress(chainId, 101, packingAddress);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getVirtualBank")
    @ApiOperation(description = "获取虚拟银行成员信息", order = 602)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个List对象", responseType = @TypeDescriptor(value = List.class,
            collectionElement = VirtualBankDirectorDTO.class)
    )
    public RpcResult getVirtualBankInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<List<VirtualBankDirectorDTO>> result = converterTools.getVirtualBankInfo(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }
}
