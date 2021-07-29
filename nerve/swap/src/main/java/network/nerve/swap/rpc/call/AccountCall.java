package network.nerve.swap.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.swap.constant.SwapErrorCode;

import java.util.HashMap;
import java.util.Map;

public class AccountCall extends BaseCall {

    private static final String CALL_AC_GET_PRIKEY_BY_ADDRESS = "ac_getPriKeyByAddress";


    public static String getAccountPrikey(int chainId, String address, String password) throws NulsException {

        Map<String, Object> callParams = new HashMap<>(4);
        callParams.put("chainId", chainId);
        callParams.put("address", address);
        callParams.put("password", password);
        Response cmdResp = null;
        try {
            cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_PRIKEY_BY_ADDRESS, callParams);
        } catch (Exception e) {
            Log.error(e);
            throw new NulsException(SwapErrorCode.MOUDLE_COMMUNICATION_ERROR);
        }
        if (!cmdResp.isSuccess()) {
            throw new NulsException(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }
        HashMap callResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_AC_GET_PRIKEY_BY_ADDRESS);
        if (callResult == null || callResult.size() == 0) {
            throw new NulsException(SwapErrorCode.ACCOUNT_VALID_ERROR);
        }
        return (String) callResult.get("priKey");
    }
}
