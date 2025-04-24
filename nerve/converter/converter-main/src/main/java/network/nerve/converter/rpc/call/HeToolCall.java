package network.nerve.converter.rpc.call;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.btc.txdata.FtData;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.ConverterContext;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeToolCall extends BaseCall {

    public static Map buildTbcWithdrawTbc(int nerveChainId, WithdrawalUTXOTxData withdrawalUTXO, String multiSigAddress, String address_to, String amount_tbc, String opReturn) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(withdrawalUTXO.serialize()));
            params.put("from", multiSigAddress);
            params.put("to", address_to);
            params.put("amount", amount_tbc);
            params.put("opReturn", opReturn);
            Map resultStr;
            Map result = (Map) requestAndResponse(ModuleE.HT.abbr, "ht_buildTbcWithdrawTBC", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call hetool module error, get empty data.");
            }
            resultStr = (Map) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.HT.abbr, "ht_buildTbcWithdrawTBC");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static String finishTbcWithdrawTbc(int nerveChainId, String txraw, List sigs, List pubKeys) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("txraw", txraw);
            params.put("sigs", sigs);
            params.put("pubKeys", pubKeys);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.HT.abbr, "ht_finishTbcWithdrawTBC", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call hetool module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.HT.abbr, "ht_finishTbcWithdrawTBC");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static Map buildTbcWithdrawFt(int nerveChainId, WithdrawalUTXOTxData withdrawalUTXO, String multiSigAddress, String multiSigAddressHash, String address_to, FtData tokenInfo, String transferTokenAmount,
                                         List<String> preTXs, List<String> prepreTxDatas, String contractTx, String opReturn) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("withdrawalUTXO", HexUtil.encode(withdrawalUTXO.serialize()));
            params.put("from", multiSigAddress);
            params.put("fromHash", multiSigAddressHash);
            params.put("to", address_to);
            params.put("tokenInfo", tokenInfo);
            params.put("amount", transferTokenAmount);
            params.put("preTXs", preTXs);
            params.put("prepreTxDatas", prepreTxDatas);
            params.put("contractTx", contractTx);
            params.put("opReturn", opReturn);
            Map resultStr;
            Map result = (Map) requestAndResponse(ModuleE.HT.abbr, "ht_buildTbcWithdrawFT", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call hetool module error, get empty data.");
            }
            resultStr = (Map) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.HT.abbr, "ht_buildTbcWithdrawFT");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static String finishTbcWithdrawFt(int nerveChainId, String txraw, List sigs, List pubKeys) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, nerveChainId);

            params.put("txraw", txraw);
            params.put("sigs", sigs);
            params.put("pubKeys", pubKeys);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.HT.abbr, "ht_finishTbcWithdrawFT", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call hetool module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.HT.abbr, "ht_finishTbcWithdrawFT");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }

    public static String fetchFtPrePreTxData(String preTXHex, int preTxVout, Map prePreTxs) throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");

            params.put("preTXHex", preTXHex);
            params.put("preTxVout", preTxVout);
            params.put("prePreTxs", prePreTxs);
            String resultStr;
            Map result = (Map) requestAndResponse(ModuleE.HT.abbr, "ht_fetchFtPrePreTxData", params);
            if (result == null || result.get("value") == null) {
                throw new Exception("Call hetool module error, get empty data.");
            }
            resultStr = (String) result.get("value");
            return resultStr;
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.HT.abbr, "ht_fetchFtPrePreTxData");
            ConverterContext.MAIN_CHAIN_LOGGER.error(msg, e);
            throw new Exception(e);
        }
    }
}
