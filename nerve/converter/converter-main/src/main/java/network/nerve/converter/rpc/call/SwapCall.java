package network.nerve.converter.rpc.call;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.WithdrawalUTXO;
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

    public static boolean isLegalCoinForRemoveStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_is_legal_coin_for_remove_stable", params);
            if (result == null || result.get("value") == null) {
                return false;
            }
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_is_legal_coin_for_remove_stable");
            LoggerUtil.LOG.error(msg, e);
            return false;
        }
    }

    public static boolean isLegalCoinForStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_is_legal_coin_for_stable", params);
            if (result == null || result.get("value") == null) {
                return false;
            }
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_is_legal_coin_for_stable");
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

    public static boolean isLegalSwapFeeRate(int chainId, String swapPairAddress, Integer feeRate) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("swapPairAddress", swapPairAddress);
            params.put("feeRate", feeRate);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_is_legal_swap_fee_rate", params);
            if (result == null || result.get("value") == null) {
                return false;
            }
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_is_legal_swap_fee_rate");
            LoggerUtil.LOG.error(msg, e);
            return false;
        }
    }


    public static void addCoinForStable(int chainId, String stablePairAddress, int assetChainId, int assetId) throws NulsException {
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
                LoggerUtil.LOG.error("[Proposal to add currency] Currency addition failed. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_add_coin_for_stable");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static void removeCoinForStable(int chainId, String stablePairAddress, int assetChainId, int assetId, String status) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("status", status);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_remove_coin_for_stable", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[Proposal to remove currency] Currency removal failed. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_remove_coin_for_stable");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static void pauseCoinForStable(int chainId, String stablePairAddress, int assetChainId, int assetId, String status) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("stablePairAddress", stablePairAddress);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("status", status);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_pause_coin_for_stable", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[Suspend multi chain routing pool currency transactions] Currency pause failed. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_pause_coin_for_stable");
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
                LoggerUtil.LOG.error("[Proposal management for stablecoin trading-Used forSwaptransaction] Add failed. stablePairAddress: {}", stablePairAddress);
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
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_remove_stable_for_swap_trade", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[Proposal rollback] Removing stablecoin trading pair failed. stablePairAddress: {}", stablePairAddress);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_remove_stable_for_swap_trade");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static void updateSwapPairFeeRate(int chainId, String swapPairAddress, Integer feeRate) throws NulsException {
        // swapCustomization of handling fees
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("swapPairAddress", swapPairAddress);
            params.put("feeRate", feeRate);
            boolean success;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_update_swap_pair_fee_rate", params);
            if (result == null || result.get("value") == null) {
                success = false;
            } else {
                success = (boolean) result.get("value");
            }
            if (!success) {
                LoggerUtil.LOG.error("[SWAPCustomization of handling fees] Update failed. swapPairAddress: {}, feeRate:{}", swapPairAddress, feeRate);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_update_swap_pair_fee_rate");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    public static String signFchWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signer, String to,
                                         long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signer", signer);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_signFchWithdraw", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_signFchWithdraw");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static boolean verifyFchWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signData);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_verifyFchWithdraw", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            return (Boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_verifyFchWithdraw");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static int verifyFchWithdrawCount(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String toAddress, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signatureData);
            params.put("to", toAddress);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_verifyFchWithdrawCount", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            return Integer.parseInt(result.get("value").toString());
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_verifyFchWithdrawCount");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static String createFchMultiSignWithdrawTx(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signatureData);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_createFchMultiSignWithdrawTx", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_createFchMultiSignWithdrawTx");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }


    public static String signBchWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signer, String to,
                                         long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signer", signer);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            params.put("mainnet", mainnet);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_signBchWithdraw", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_signBchWithdraw");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static boolean verifyBchWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signData);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            params.put("mainnet", mainnet);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_verifyBchWithdraw", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            return (Boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_verifyBchWithdraw");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static int verifyBchWithdrawCount(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String toAddress, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signatureData);
            params.put("to", toAddress);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            params.put("mainnet", mainnet);
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_verifyBchWithdrawCount", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            return Integer.parseInt(result.get("value").toString());
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_verifyBchWithdrawCount");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static String createBchMultiSignWithdrawTx(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(new WithdrawalUTXOTxData(withdrawalUTXO).serialize()));
            params.put("signData", signatureData);
            params.put("to", to);
            params.put("amount", amount);
            params.put("feeRate", feeRate);
            params.put("opReturn", opReturn);
            params.put("m", m);
            params.put("n", n);
            params.put("useAllUTXO", useAllUTXO);
            params.put("splitGranularity", splitGranularity);
            params.put("mainnet", mainnet);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.SW.abbr, "sw_createBchMultiSignWithdrawTx", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call swap module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.SW.abbr, "sw_createBchMultiSignWithdrawTx");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }
}
