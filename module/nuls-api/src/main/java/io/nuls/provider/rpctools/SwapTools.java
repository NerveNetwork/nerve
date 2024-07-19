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
     * querySwapTransaction pair information
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
     * Query AllSwapTransaction pair information
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
     * Query AllStable-SwapTransaction pair information
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
     * Address based on transaction pairs querySwapTransaction pair information
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
     * queryStable-SwapTransaction pair information
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
     * Query transaction execution results
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
     * according toLPAsset inquiry transaction address
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
     * Finding the best trading path
     */
    public Result<Map<String, Object>> getBestTradeExactIn(int chainId, String tokenInStr, String tokenInAmount,
                                              String tokenOutStr, int maxPairSize, String[] allPairs, String resultRule) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("tokenInStr", tokenInStr);
        params.put("tokenInAmount", tokenInAmount);
        params.put("tokenOutStr", tokenOutStr);
        params.put("maxPairSize", maxPairSize);
        params.put("pairs", allPairs);
        params.put("resultRule", resultRule);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_best_trade_exact_in", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * querySwapMinimum buy in for coin exchangetoken
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
     * Query AddSwapThe minimum number of assets with liquidity
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
     * Query removalSwapThe minimum number of assets with liquidity
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
     * Queries available forSwapStable currency trading for transactions
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

    /**
     * Query all valid stablecoin transaction pairs
     */
    public Result<List<Map>> getAvailableStablePairList(int chainId) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_get_available_stable_pair_list", params, (Function<List<Map>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    public Result commonRequest(String cmd, Map params) {
        try {
            return callRpc(ModuleE.SW.abbr, cmd, params, (Function<Object, Result<Object>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
