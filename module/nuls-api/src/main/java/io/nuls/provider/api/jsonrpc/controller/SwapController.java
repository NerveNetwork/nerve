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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Api(type = ApiType.JSONRPC)
public class SwapController {

    @Autowired
    SwapTools swapTools;

    @RpcMethod("getSwapPairInfo")
    @ApiOperation(description = "querySwapTransaction pair information", order = 701)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
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
    @ApiOperation(description = "Query AllSwapTransaction to address", order = 710)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
    })
    @ResponseData(description = "All transactions against addresses", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
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
    @ApiOperation(description = "Query AllStable-SwapTransaction to address", order = 711)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
    })
    @ResponseData(description = "All transactions against addresses", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
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
    @ApiOperation(description = "Address based on transaction pairs querySwapTransaction pair information", order = 702)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
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
    @ApiOperation(description = "queryStable-SwapTransaction pair information", order = 703)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction to address")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
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
    @ApiOperation(description = "queryStable-SwapTransaction pair information", order = 704)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "transactionHash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class))
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
    @ApiOperation(description = "according toLPAsset inquiry transaction address", order = 705)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "tokenLPStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetLPType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Transaction to address")
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
    @ApiOperation(description = "Finding the best trading path", order = 706)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "tokenInStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "Types of assets sold, examples：1-1"),
            @Parameter(parameterName = "tokenInAmount", requestType = @TypeDescriptor(value = String.class), parameterDes = "Number of assets sold"),
            @Parameter(parameterName = "tokenOutStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "Types of purchased assets, examples：1-1"),
            @Parameter(parameterName = "maxPairSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "Deepest trading path"),
            @Parameter(parameterName = "pairs", requestType = @TypeDescriptor(value = String[].class), parameterDes = "List of all transaction pairs in the current network"),
            @Parameter(parameterName = "resultRule", requestType = @TypeDescriptor(value = String.class), parameterDes = "`bestPrice`, `impactPrice`. according to[Optimal price]and[Price impact]Using factors to obtain results, defaults to using[Price impact]Using factors to obtain results")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "Best trading path"),
            @Key(name = "tokenAmountIn", valueType = Map.class, description = "Selling assets"),
            @Key(name = "tokenAmountOut", valueType = Map.class, description = "Buying assets"),
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
    @ApiOperation(description = "querySwapMinimum buy in for coin exchangetoken", order = 707)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "amountIn", requestType = @TypeDescriptor(value = String.class), parameterDes = "Number of assets sold"),
            @Parameter(parameterName = "tokenPath", requestType = @TypeDescriptor(value = String[].class), parameterDes = "Currency exchange asset path, the last asset in the path is the asset that the user wants to buy, such as sellingAbuyB: [A, B] or [A, C, B]")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tokenPath", valueType = List.class, description = "Best trading path"),
            @Key(name = "tokenAmountIn", valueType = Map.class, description = "Selling assets"),
            @Key(name = "tokenAmountOut", valueType = Map.class, description = "Buying assets"),
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
    @ApiOperation(description = "Query AddSwapThe minimum number of assets with liquidity", order = 708)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "amountA", requestType = @TypeDescriptor(value = String.class), parameterDes = "Added assetsAQuantity of"),
            @Parameter(parameterName = "amountB", requestType = @TypeDescriptor(value = String.class), parameterDes = "Added assetsBQuantity of"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "assetAMinimum added value"),
            @Key(name = "amountBMin", valueType = String.class, description = "assetBMinimum added value")
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
    @ApiOperation(description = "Query removalSwapThe minimum number of assets with liquidity", order = 709)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "amountLP", requestType = @TypeDescriptor(value = String.class), parameterDes = "Removed assetsLPQuantity of"),
            @Parameter(parameterName = "tokenAStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenBStr", requestType = @TypeDescriptor(value = String.class), parameterDes = "assetBType of, example：1-1")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "amountAMin", valueType = String.class, description = "assetAMinimum removal value"),
            @Key(name = "amountBMin", valueType = String.class, description = "assetBMinimum removal value")
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

    @RpcMethod("calMinAmountOnStableSwapAddLiquidity")
    @ApiOperation(description = "calMinAmountOnStableSwapAddLiquidity", order = 709)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "pairAddress"),
            @Parameter(parameterName = "tokenStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "tokenAmount", parameterType = "String", parameterDes = "tokenAmount")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public RpcResult calMinAmountOnStableSwapAddLiquidity(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int i = 0;
        int chainId;
        try {
            chainId = (int) params.get(i++);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("pairAddress", params.get(i++));
        params1.put("tokenStr", params.get(i++));
        params1.put("tokenAmount", params.get(i++));
        Result<Map<String, Object>> result = swapTools.commonRequest("sw_stable_swap_min_amount_add_liquidity", params1);

        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("calMinAmountOnStableSwapRemoveLiquidity")
    @ApiOperation(description = "calMinAmountOnStableSwapRemoveLiquidity", order = 709)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "pairAddress", parameterType = "String", parameterDes = "pairAddress"),
            @Parameter(parameterName = "tokenStr", parameterType = "String", parameterDes = "assetAType of, example：1-1"),
            @Parameter(parameterName = "liquidity", parameterType = "String", parameterDes = "Removed assetsLPQuantity of")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    }))
    public RpcResult calMinAmountOnStableSwapRemoveLiquidity(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int i = 0;
        int chainId;
        try {
            chainId = (int) params.get(i++);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Map<String, Object> params1 = new HashMap<>();
        params1.put("chainId", chainId);
        params1.put("pairAddress", params.get(i++));
        params1.put("tokenStr", params.get(i++));
        params1.put("liquidity", params.get(i++));
        Result<Map<String, Object>> result = swapTools.commonRequest("sw_stable_swap_min_amount_remove_liquidity", params1);

        return ResultUtil.getJsonRpcResult(result);
    }



    @RpcMethod("getStablePairListForSwapTrade")
    @ApiOperation(description = "Queries available forSwapStable currency trading for transactions", order = 710)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    })
    @ResponseData(name = "Return value", description = "Return a collection", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class))
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
    @ApiOperation(description = "Query all valid stablecoin transaction pairs", order = 711)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    })
    @ResponseData(name = "Return value", description = "Return a collection", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class))
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
