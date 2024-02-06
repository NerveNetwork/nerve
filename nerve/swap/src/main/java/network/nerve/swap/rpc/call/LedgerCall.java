package network.nerve.swap.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.NonceBalance;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.utils.LoggerUtil;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LedgerCall extends BaseCall {

    /**
     * Query account balance
     */
    public static NonceBalance getBalanceNonce(int chainId, int assetChainId, int assetId, String address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("address", address);
            params.put("isConfirmed", true);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "getBalanceNonce", params);
            String strNonce = (String) result.get("nonce");
            byte[] nonce = null != strNonce ? RPCUtil.decode(strNonce) : SwapConstant.DEFAULT_NONCE;
            BigInteger available = BigIntegerUtils.stringToBigInteger(result.get("available").toString());
            BigInteger freeze = BigIntegerUtils.stringToBigInteger(result.get("freeze").toString());
            return new NonceBalance(nonce, available, freeze);
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "getBalanceNonce");
            LoggerUtil.LOG.error(msg, e);
            throw new NulsException(CommonCodeConstanst.RPC_REQUEST_FAILD);
        }
    }

    /**
     * Query account balance
     */
    public static BigInteger getBalance(int chainId, int assetChainId, int assetId, String address) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("isConfirmed", true);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "getBalance", params);
            Object available = result.get("available");
            if (null == available) {
                LoggerUtil.LOG.error("call getBalance response available is null, error:{}",
                        CommonCodeConstanst.RPC_REQUEST_FAILD.getCode());
                return new BigInteger("0");
            }
            return BigIntegerUtils.stringToBigInteger(String.valueOf(available));
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "getBalance");
            LoggerUtil.LOG.error(msg, e);
            return new BigInteger("0");
        }
    }

    /**
     * registerLPasset
     */
    public static Integer lpAssetReg(int chainId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("assetName", assetName);
            params.put("initNumber", 0);
            params.put("decimalPlace", decimalPlace);
            params.put("assetSymbol", assetSymbol);
            params.put("address", assetAddress);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_chain_asset_swap_liquidity_pool_reg", params);
            Object value = result.get("assetId");
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_reg");
            LoggerUtil.LOG.error(msg, e);
            throw e;
        }
    }

    /**
     * removeLPasset
     */
    public static Boolean lpAssetDelete(int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_chain_asset_swap_liquidity_pool_rollback", params);
            Object value = result.get("value");
            return (Boolean) value;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_rollback");
            LoggerUtil.LOG.error(msg, e);
            throw e;
        }
    }

    public static boolean existNerveAsset(int chainId, int assetChainId, int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_get_asset", params);
            if (result == null || result.get("assetSymbol") == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_get_asset");
            LoggerUtil.LOG.error(msg, e);
            return false;
        }
    }

    public static LedgerAssetDTO getNerveAsset(int chainId, int assetChainId, int assetId) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_get_asset", params);
            if (result == null || result.get("assetSymbol") == null) {
                return null;
            }
            return new LedgerAssetDTO(assetChainId, result);
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_get_asset");
            LoggerUtil.LOG.error(msg, e);
            return null;
        }
    }

}
