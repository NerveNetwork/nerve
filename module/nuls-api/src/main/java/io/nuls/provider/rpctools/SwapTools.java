package io.nuls.provider.rpctools;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.rpc.model.ModuleE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Component
public class SwapTools implements CallRpc {

    /**
     * 查询Swap交易对信息
     */
    public Result<Map<String, Object>> getSwapPairInfo(int chainId, String tokenAStr, String tokenBStr) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("tokenAStr", tokenAStr);
        params.put("tokenBStr", tokenBStr);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_pair_info", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询所有Swap交易对信息
     */
    public Result<List<String>> getAllSwapPairsInfo(int chainId) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_get_all_swap_pairs", params, (Function<List<String>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询所有Stable-Swap交易对信息
     */
    public Result<List<String>> getAllStableSwapPairsInfo(int chainId) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_get_all_stable_swap_pairs", params, (Function<List<String>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 根据交易对地址 查询Swap交易对信息
     */
    public Result<Map<String, Object>> getSwapPairInfoByPairAddress(int chainId, String pairAddress) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("pairAddress", pairAddress);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_pair_info_by_address", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询Stable-Swap交易对信息
     */
    public Result<Map<String, Object>> getStableSwapPairInfo(int chainId, String pairAddress) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("pairAddress", pairAddress);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_stable_swap_pair_info", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询交易执行结果
     */
    public Result<Map<String, Object>> getSwapResultInfo(int chainId, String txHash) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("txHash", txHash);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_result_info", params, (Function<Map<String, Object>, Result>) res -> new Result(res.get("value")));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 根据LP资产查询交易对地址
     */
    public Result<String> getPairAddressByTokenLP(int chainId, String tokenLPStr) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("tokenLPStr", tokenLPStr);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_pair_by_lp", params, (Function<String, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 寻找最佳交易路径
     */
    public Result<Map<String, Object>> getBestTradeExactIn(int chainId, String tokenInStr, String tokenInAmount,
                                              String tokenOutStr, int maxPairSize, String[] allPairs) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("tokenInStr", tokenInStr);
        params.put("tokenInAmount", tokenInAmount);
        params.put("tokenOutStr", tokenOutStr);
        params.put("maxPairSize", maxPairSize);
        params.put("pairs", allPairs);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_best_trade_exact_in", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询Swap币币交换最小买进token
     */
    public Result<Map<String, Object>> calMinAmountOnSwapTokenTrade(int chainId, String amountIn, String[] tokenPath) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("amountIn", amountIn);
        params.put("tokenPath", tokenPath);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_min_amount_token_trade", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询添加Swap流动性的最小资产数量
     */
    public Result<Map<String, Object>> calMinAmountOnSwapAddLiquidity(int chainId, String amountA, String amountB,
                                              String tokenAStr, String tokenBStr) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("amountA", amountA);
        params.put("amountB", amountB);
        params.put("tokenAStr", tokenAStr);
        params.put("tokenBStr", tokenBStr);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_min_amount_add_liquidity", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询移除Swap流动性的最小资产数量
     */
    public Result<Map<String, Object>> calMinAmountOnSwapRemoveLiquidity(int chainId, String amountLP,
                                              String tokenAStr, String tokenBStr) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("amountLP", amountLP);
        params.put("tokenAStr", tokenAStr);
        params.put("tokenBStr", tokenBStr);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_swap_min_amount_remove_liquidity", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询可用于Swap交易的稳定币交易对
     */
    public Result<List<Map>> getStablePairListForSwapTrade(int chainId) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_get_stable_pair_list_for_swap_trade", params, (Function<List<Map>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

}
