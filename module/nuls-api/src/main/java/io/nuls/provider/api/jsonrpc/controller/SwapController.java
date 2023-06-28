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
import io.nuls.provider.rpctools.SwapTools;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.provider.utils.VerifyUtils;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class SwapController {

    @Autowired
    SwapTools swapTools;

    @RpcMethod("getSwapPairInfo")
    @ApiOperation(description = "查询Swap交易对信息", order = 701)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getSwapPairInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId;
        String tokenAStr, tokenBStr;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            tokenAStr = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenAStr] is inValid");
        }
        try {
            tokenBStr = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenBStr] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(tokenAStr) || StringUtils.isBlank(tokenBStr)) {
            return RpcResult.paramError("[tokenAStr or tokenBStr] is incorrect");
        }
        Result<Map<String, Object>> result = swapTools.getSwapPairInfo(chainId, tokenAStr, tokenBStr);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getAllSwapPairsInfo")
    @ApiOperation(description = "查询所有Swap交易对地址", order = 710)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
    })
    @ResponseData(description = "所有交易对地址", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    public RpcResult getAllSwapPairsInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<List<String>> result = swapTools.getAllSwapPairsInfo(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getAllStableSwapPairsInfo")
    @ApiOperation(description = "查询所有Stable-Swap交易对地址", order = 711)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
    })
    @ResponseData(description = "所有交易对地址", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    public RpcResult getAllStableSwapPairsInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<List<String>> result = swapTools.getAllStableSwapPairsInfo(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getSwapPairInfoByPairAddress")
    @ApiOperation(description = "根据交易对地址 查询Swap交易对信息", order = 702)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "pairAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易对地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getSwapPairInfoByPairAddress(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String pairAddress;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pairAddress = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pairAddress] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(pairAddress) || !AddressTool.validAddress(chainId, pairAddress)) {
            return RpcResult.paramError("[pairAddress] is incorrect");
        }
        Result<Map<String, Object>> result = swapTools.getSwapPairInfoByPairAddress(chainId, pairAddress);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getStableSwapPairInfo")
    @ApiOperation(description = "查询Stable-Swap交易对信息", order = 703)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "pairAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易对地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getStableSwapPairInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String pairAddress;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pairAddress = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pairAddress] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(pairAddress) || !AddressTool.validAddress(chainId, pairAddress)) {
            return RpcResult.paramError("[pairAddress] is incorrect");
        }
        Result<Map<String, Object>> result = swapTools.getStableSwapPairInfo(chainId, pairAddress);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getSwapResultInfo")
    @ApiOperation(description = "查询Stable-Swap交易对信息", order = 704)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易Hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class))
    public RpcResult getSwapResultInfo(List<Object> params) {
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
        Result<Map<String, Object>> result = swapTools.getSwapResultInfo(chainId, txHash);
        return ResultUtil.getJsonRpcResult(result);
    }



    @RpcMethod("getPairAddressByTokenLP")
    @ApiOperation(description = "根据LP资产查询交易对地址", order = 705)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "tokenLPStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产LP的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "交易对地址")
    public RpcResult getPairAddressByTokenLP(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String tokenLPStr;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            tokenLPStr = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenLPStr] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(tokenLPStr)) {
            return RpcResult.paramError("[tokenLPStr] is incorrect");
        }
        Result<String> result = swapTools.getPairAddressByTokenLP(chainId, tokenLPStr);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getBestTradeExactIn")
    @ApiOperation(description = "寻找最佳交易路径", order = 706)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "tokenInStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "卖出资产的类型，示例：1-1"),
            @Parameter(parameterName = "tokenInAmount", requestType = @TypeDescriptor(value = String.class), parameterDes = "卖出资产数量"),
            @Parameter(parameterName = "tokenOutStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "买进资产的类型，示例：1-1"),
            @Parameter(parameterName = "maxPairSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易最深路径"),
            @Parameter(parameterName = "pairs", requestType = @TypeDescriptor(value = String[].class), parameterDes = "当前网络所有交易对列表"),
            @Parameter(parameterName = "resultRule", requestType = @TypeDescriptor(value = String.class), parameterDes = "`bestPrice`, `impactPrice`. 按[最优价格]和[价格影响]因素来取结果，默认使用[价格影响]因素来取结果")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "最佳交易路径"),
            @Key(name = "tokenAmountIn", valueType = Map.class, description = "卖出资产"),
            @Key(name = "tokenAmountOut", valueType = Map.class, description = "买进资产"),
    }))
    public RpcResult getBestTradeExactIn(List<Object> params) {
        VerifyUtils.verifyParams(params, 6);
        int chainId;
        String tokenInStr, tokenInAmount, tokenOutStr;
        int maxPairSize;
        String[] pairs;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            tokenInStr = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHash] is inValid");
        }
        try {
            tokenInAmount = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenInAmount] is inValid");
        }
        try {
            tokenOutStr = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenOutStr] is inValid");
        }
        try {
            maxPairSize = (int) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[maxPairSize] is inValid");
        }
        try {
            List<String> pairList = (List<String>) params.get(5);
            pairs = pairList.toArray(new String[pairList.size()]);
        } catch (Exception e) {
            return RpcResult.paramError("[pairs] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(tokenInStr)) {
            return RpcResult.paramError("[tokenInStr] is incorrect");
        }
        if (StringUtils.isBlank(tokenInAmount)) {
            return RpcResult.paramError("[tokenInAmount] is incorrect");
        }
        if (StringUtils.isBlank(tokenOutStr)) {
            return RpcResult.paramError("[tokenOutStr] is incorrect");
        }
        String resultRule = null;
        if (params.size() > 6) {
            resultRule = (String) params.get(6);
        }
        Result<Map<String, Object>> result = swapTools.getBestTradeExactIn(chainId, tokenInStr, tokenInAmount, tokenOutStr, maxPairSize, pairs, resultRule);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("calMinAmountOnSwapTokenTrade")
    @ApiOperation(description = "查询Swap币币交换最小买进token", order = 707)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "amountIn", requestType = @TypeDescriptor(value = String.class), parameterDes = "卖出的资产数量"),
            @Parameter(parameterName = "tokenPath", requestType = @TypeDescriptor(value = String[].class), parameterDes = "币币交换资产路径，路径中最后一个资产，是用户要买进的资产，如卖A买B: [A, B] or [A, C, B]")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "最佳交易路径"),
            @Key(name = "tokenAmountIn", valueType = Map.class, description = "卖出资产"),
            @Key(name = "tokenAmountOut", valueType = Map.class, description = "买进资产"),
    }))
    public RpcResult calMinAmountOnSwapTokenTrade(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId;
        String amountIn;
        String[] tokenPath;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            amountIn = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[amountIn] is inValid");
        }
        try {
            List<String> tokenPathList = (List<String>) params.get(2);
            tokenPath = tokenPathList.toArray(new String[tokenPathList.size()]);
        } catch (Exception e) {
            return RpcResult.paramError("[tokenPath] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (StringUtils.isBlank(amountIn)) {
            return RpcResult.paramError("[amountIn] is incorrect");
        }
        Result<Map<String, Object>> result = swapTools.calMinAmountOnSwapTokenTrade(chainId, amountIn, tokenPath);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("calMinAmountOnSwapAddLiquidity")
    @ApiOperation(description = "查询添加Swap流动性的最小资产数量", order = 708)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "amountA", requestType = @TypeDescriptor(value = String.class), parameterDes = "添加的资产A的数量"),
            @Parameter(parameterName = "amountB", requestType = @TypeDescriptor(value = String.class), parameterDes = "添加的资产B的数量"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "资产A最小添加值"),
            @Key(name = "amountBMin", valueType = String.class, description = "资产B最小添加值")
    }))
    public RpcResult calMinAmountOnSwapAddLiquidity(List<Object> params) {
        VerifyUtils.verifyParams(params, 5);
        int chainId;
        String amountA, amountB, tokenAStr, tokenBStr;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            amountA = params.get(1).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[amountA] is inValid");
        }
        try {
            amountB = params.get(2).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[amountB] is inValid");
        }
        try {
            tokenAStr = params.get(3).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[tokenAStr] is inValid");
        }
        try {
            tokenBStr = params.get(4).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[tokenBStr] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<Map<String, Object>> result = swapTools.calMinAmountOnSwapAddLiquidity(chainId, amountA, amountB, tokenAStr, tokenBStr);
        return ResultUtil.getJsonRpcResult(result);
    }


    @RpcMethod("calMinAmountOnSwapRemoveLiquidity")
    @ApiOperation(description = "查询移除Swap流动性的最小资产数量", order = 709)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "amountLP", requestType = @TypeDescriptor(value = String.class), parameterDes = "移除的资产LP的数量"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产A的类型，示例：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产B的类型，示例：1-1")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "资产A最小移除值"),
            @Key(name = "amountBMin", valueType = String.class, description = "资产B最小移除值")
    }))
    public RpcResult calMinAmountOnSwapRemoveLiquidity(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId;
        String amountLP, tokenAStr, tokenBStr;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            amountLP = params.get(1).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[amountLP] is inValid");
        }
        try {
            tokenAStr = params.get(2).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[tokenAStr] is inValid");
        }
        try {
            tokenBStr = params.get(3).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[tokenBStr] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<Map<String, Object>> result = swapTools.calMinAmountOnSwapRemoveLiquidity(chainId, amountLP, tokenAStr, tokenBStr);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getStablePairListForSwapTrade")
    @ApiOperation(description = "查询可用于Swap交易的稳定币交易对", order = 710)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个集合", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class))
    public RpcResult getStablePairListForSwapTrade(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<List<Map>> result = swapTools.getStablePairListForSwapTrade(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getAvailableStablePairList")
    @ApiOperation(description = "查询所有有效的稳定币交易对", order = 711)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个集合", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class))
    public RpcResult getAvailableStablePairList(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<List<Map>> result = swapTools.getAvailableStablePairList(chainId);
        return ResultUtil.getJsonRpcResult(result);
    }

}
