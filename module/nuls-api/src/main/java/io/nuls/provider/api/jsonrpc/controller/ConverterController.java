package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.constant.CommonCodeConstanst;
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

import java.util.HashMap;
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
            return RpcResult.paramError("[hash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[hash] is incorrect");
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

    @RpcMethod("getRechargeNerveHash")
    @ApiOperation(description = "根据异构链跨链转入的交易hash查询NERVE的交易hash", order = 607)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链跨链转入的交易hash")
    })
    @ResponseData(name = "返回值", description = "NERVE交易hash")
    public RpcResult getRechargeNerveHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String heterogeneousTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            heterogeneousTxHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(heterogeneousTxHash)) {
            return RpcResult.paramError("[heterogeneousTxHash] is incorrect");
        }
        Result<String> result = converterTools.getRechargeNerveHash(chainId, heterogeneousTxHash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("findByWithdrawalTxHash")
    @ApiOperation(description = "根据提现交易hash获取确认信息", order = 608)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "提现交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", description = "异构链ID"),
            @Key(name = "heterogeneousHeight", description = "异构链交易区块高度"),
            @Key(name = "heterogeneousTxHash", description = "异构链交易hash"),
            @Key(name = "confirmWithdrawalTxHash", description = "NERVE确认交易hash")
    }))
    public RpcResult findByWithdrawalTxHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(txHash)) {
            return RpcResult.paramError("[txHash] is incorrect");
        }
        Result<Map<String, Object>> result = converterTools.findByWithdrawalTxHash(chainId, txHash);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousRegisterNetwork")
    @ApiOperation(description = "查询资产的异构链注册网络", order = 609)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "异构链ID"),
            @Key(name = "contractAddress", valueType = String.class, description = "资产对应合约地址(若有)")
    })
    )
    public RpcResult getHeterogeneousRegisterNetwork(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousRegisterNetwork(chainId, assetId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getHeterogeneousAssetInfo")
    @ApiOperation(description = "查询资产的异构链资产信息", order = 610)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = Integer.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class)
    )
    public RpcResult getHeterogeneousAssetInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        Result<Map<String, Object>> result = converterTools.getHeterogeneousAssetInfo(chainId, assetId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("retryVirtualBank")
    @ApiOperation(description = "重新执行变更", order = 611)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "变更交易hash"),
            @Parameter(parameterName = "height", requestType = @TypeDescriptor(value = long.class), parameterDes = "变更交易所在高度"),
            @Parameter(parameterName = "prepare", requestType = @TypeDescriptor(value = int.class), parameterDes = "1 - 准备阶段，2 - 非准备，执行阶段")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public RpcResult retryVirtualBank(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, prepare;
        String hash;
        long height;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            hash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }
        try {
            height = Long.parseLong(params.get(2).toString());
        } catch (Exception e) {
            return RpcResult.paramError("[height] is inValid");
        }
        try {
            prepare = Integer.parseInt(params.get(3).toString());
        } catch (Exception e) {
            return RpcResult.paramError("[prepare] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(hash)) {
            return RpcResult.paramError("[hash] is incorrect");
        }
        Map<String, Object> params1 = new HashMap<>(8);
        params1.put("chainId", chainId);
        params1.put("hash", hash);
        params1.put("height", height);
        params1.put("prepare", prepare);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_retryVirtualBank", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("checkRetryHtgTx")
    @ApiOperation(description = "重新解析异构链交易", order = 612)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链id"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public RpcResult checkRetryHtgTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId,heterogeneousChainId;
        String heterogeneousTxHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            heterogeneousChainId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousChainId] is inValid");
        }
        try {
            heterogeneousTxHash = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[heterogeneousTxHash] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(heterogeneousTxHash)) {
            return RpcResult.paramError("[heterogeneousTxHash] is incorrect");
        }
        Map<String, Object> params1 = new HashMap<>(8);
        params1.put("chainId", chainId);
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("heterogeneousTxHash", heterogeneousTxHash);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_checkRetryHtgTx", params1);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("registerheterogeneousasset")
    @ApiOperation(description = "注册异构链资产", order = 613)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产小数位数"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产符号"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注", canNull = true)
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public RpcResult registerheterogeneousasset(List<Object> params) {
        VerifyUtils.verifyParams(params, 8);
        int i = 0;
        int chainId = (int) params.get(i++);
        int heterogeneousChainId = (int) params.get(i++);
        int decimals = (int) params.get(i++);
        String symbol = (String) params.get(i++);
        String contractAddress = (String) params.get(i++);
        String address = (String) params.get(i++);
        String password = (String) params.get(i++);
        String remark = (String) params.get(i++);

        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("heterogeneousChainId", heterogeneousChainId);
        params1.put("decimals", decimals);
        params1.put("symbol", symbol);
        params1.put("contractAddress", contractAddress);
        Result<Map<String, Object>> result = converterTools.commonRequest("cv_validate_heterogeneous_contract_asset_reg_pending_tx", params1);
        if (result.isFailed()) {
            return ResultUtil.getJsonRpcResult(result);
        }
        Map<String, Object> dataMap = result.getData();
        Boolean data = Boolean.parseBoolean(dataMap.get("value").toString());
        if (!data.booleanValue()) {
            return RpcResult.failed(CommonCodeConstanst.DATA_ERROR, "validate error");
        }

        Map<String, Object> params2 = new HashMap<>();
        params2.put("chainId", chainId);
        params2.put("heterogeneousChainId", heterogeneousChainId);
        params2.put("decimals", decimals);
        params2.put("symbol", symbol);
        params2.put("contractAddress", contractAddress);
        params2.put("address", address);
        params2.put("password", password);
        params2.put("remark", remark);
        Result<Map<String, Object>> result1 = converterTools.commonRequest("cv_create_heterogeneous_contract_asset_reg_pending_tx", params2);

        return ResultUtil.getJsonRpcResult(result1);
    }
}
