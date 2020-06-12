package network.nerve.converter.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.NonceBalance;
import network.nerve.converter.utils.LoggerUtil;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LedgerCall extends BaseCall {

    /**
     * 查询账户余额
     */
    public static NonceBalance getBalanceNonce(Chain chain, int assetChainId, int assetId, String address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("address", address);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "getBalanceNonce", params);
            Object availableObj = result.get("available");
            String strNonce = (String) result.get("nonce");
            byte[] nonce = null != strNonce ? RPCUtil.decode(strNonce) : ConverterConstant.DEFAULT_NONCE;
            BigInteger available = null != availableObj ?
                    BigIntegerUtils.stringToBigInteger(String.valueOf(availableObj)) : BigInteger.ZERO;
            return new NonceBalance(nonce, available);
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "getBalanceNonce");
            chain.getLogger().error(msg, e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }

    /**
     * 查询账户余额
     */
    public static BigInteger getBalance(Chain chain, int assetChainId, int assetId, String address) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("address", address);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "getBalance", params);
            Object available = result.get("available");
            if (null == available) {
                chain.getLogger().error("call getBalance response available is null, error:{}",
                        ConverterErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND.getCode());
                return new BigInteger("0");
            }
            return BigIntegerUtils.stringToBigInteger(String.valueOf(available));
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "getBalance");
            chain.getLogger().error(msg, e);
            return new BigInteger("0");
        }
    }

    /**
     * 登记异构跨链资产
     */
    public static Integer crossChainAssetReg(int chainId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("assetName", assetName);
            params.put("initNumber", 0);
            params.put("decimalPlace", decimalPlace);
            params.put("assetSymbol", assetSymbol);
            params.put("address", assetAddress);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_reg", params);
            Object value = result.get("assetId");
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_reg");
            LoggerUtil.LOG.error(msg, e);
            throw e;
        }
    }


    /**
     * 移除异构跨链资产
     */
    public static Boolean crossChainAssetDelete(int assetId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("assetId", assetId);
            Map result = (Map) requestAndResponse(ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_rollback", params);
            Object value = result.get("value");
            return (Boolean) value;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.LG.abbr, "lg_chain_asset_heterogeneous_rollback");
            LoggerUtil.LOG.error(msg, e);
            throw e;
        }
    }

}
