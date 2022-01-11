package network.nerve.converter.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.utils.LoggerUtil;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class SwapCall extends BaseCall {

    public static boolean isLegalCoinForAddStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_is_legal_coin_for_add_stable", params);
            if (result == null || result.get("value") == null) {
                return false;
            }
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_is_legal_coin_for_add_stable");
            LoggerUtil.LOG.error(msg, e);
            return false;
        }
    }

    public static boolean isLegalStable(int chainId, String stablePairAddress) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_is_legal_stable", params);
            if (result == null || result.get("value") == null) {
                return false;
            }
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_is_legal_stable");
            LoggerUtil.LOG.error(msg, e);
            return false;
        }
    }


    public static void addCoinForAddStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_add_coin_for_stable", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[提案添加币种] 币种添加失败. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_add_coin_for_stable");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static void addStablePairForSwapTrade(int chainId, String stablePairAddress) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_add_stable_for_swap_trade", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[提案添加稳定币交易对-用于Swap交易] 添加失败. stablePairAddress: {}", stablePairAddress);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_add_stable_for_swap_trade");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static void removeStablePairForSwapTrade(int chainId, String stablePairAddress) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_remove_stable_pair_for_swap_trade", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[提案回滚] 移除稳定币交易对失败. stablePairAddress: {}", stablePairAddress);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_remove_stable_pair_for_swap_trade");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }
}
