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
            @Key(name = "heterogeneousAddress", description = "异构链地址")
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


    @RpcMethod("getDisqualification")
    @ApiOperation(description = "获取已撤销虚拟银行资格节点地址列表", order = 603)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个List对象", responseType = @TypeDescriptor(value = List.class,
            collectionElement = String.class)
    )
    public RpcResult getDisqualification(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<String> result = converterTools.getDisqualification(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("retryWithdrawalMsg")
    @ApiOperation(description = "重新将异构链提现交易放入task, 重发消息", order = 604)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = int.class), parameterDes = "链内提现交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public RpcResult retryWithdrawalMsg(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String hash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            hash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[packingAddress] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[packingAddress] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.retryWithdrawalMsg(chainId, hash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousMainAsset")
    @ApiOperation(description = "返回异构链主资产在NERVE网络的资产信息", order = 605)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "异构链ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "资产链ID"),
            @Key(name = "assetId", valueType = int.class, description = "资产ID"),
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数")
    })
    )
    public RpcResult getHeterogeneousMainAsset(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousMainAsset(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getProposalInfo")
    @ApiOperation(description = "查询提案信息（序列化字符串）", order = 606)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "提案交易hash")
    })
    @ResponseData(name = "返回值", description = "返回 network.nerve.converter.model.po.ProposalPO 对象的序列化字符串")
    public RpcResult getProposalInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String proposalTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            proposalTxHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[proposalTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(proposalTxHash)) {
            return RpcResult.paramError("[proposalTxHash] is incorrect");
        }
        Result<String> result = converterTools.getProposalInfo(chainId, proposalTxHash);
        return ResultUtil.getJsonRpcResult(result);
    }


}
