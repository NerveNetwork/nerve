package io.nuls.crosschain.rpc.call;

import io.nuls.account.constant.AccountErrorCode;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.model.bo.Chain;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
/**
 * Interaction class with transaction module
 * Interaction class with transaction module
 * @author tag
 * 2019/4/10
 */
public class TransactionCall {
    /**
     * Send the newly created transaction to the transaction management module
     * The newly created transaction is sent to the transaction management module
     *
     * @param chain chain info
     * @param tx transaction hex
     */
    @SuppressWarnings("unchecked")
    public static boolean sendTx(Chain chain, String tx) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put("tx", tx);
        try {
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            if (!cmdResp.isSuccess()) {
                String errorCode = cmdResp.getResponseErrorCode();
                chain.getLogger().error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{}",
                        "tx_newTx", errorCode, cmdResp.getResponseComment());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            return true;
        }catch (NulsException e){
            throw e;
        }catch (Exception e) {
            throw new NulsException(NulsCrossChainErrorCode.INTERFACE_CALL_FAILED);
        }
    }

    public static boolean isConfirmed(Chain chain, String txhash) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("txHash", txhash);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_isConfirmed", params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Calling remote interface failed. module:{} - interface:{} - ResponseComment:{}", ModuleE.TX.abbr, "tx_isConfirmed", cmdResp.getResponseComment());
                throw new NulsException(AccountErrorCode.FAILED);
            }
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("tx_isConfirmed");
            return (boolean) result.get("value");
        } catch (Exception e) {
            String msg = MessageFormat.format("Calling remote interface failed. module:{0} - interface:{1}", ModuleE.TX.abbr, "tx_isConfirmed");
            chain.getLogger().error(msg, e);
            return false;
        }
    }
}
