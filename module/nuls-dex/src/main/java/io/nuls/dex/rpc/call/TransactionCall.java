package io.nuls.dex.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.dex.context.DexErrorCode;

import java.util.HashMap;
import java.util.Map;

public class TransactionCall {

    public static void sendTx(int chainId, String tx) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", tx);
        try {
            /*boolean ledgerValidResult = commitUnconfirmedTx(chain,tx);
            if(!ledgerValidResult){
                throw new NulsException(ConsensusErrorCode.FAILED);
            }*/
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            if (!cmdResp.isSuccess()) {
                throw new NulsException(DexErrorCode.FAILED, cmdResp.getResponseErrorCode());
            }
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            Log.error(e);
            throw new NulsException(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }
}
