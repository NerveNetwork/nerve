package network.nerve.swap.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.dto.AccountWhitelistDTO;

import java.util.*;
import java.util.function.Function;

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

    public static AccountWhitelistDTO getAccountWhitelistInfo(int chainId, String address) {
        try {
            Map<String, Object> param = new HashMap<>(2);
            param.put("chainId", chainId);
            param.put("address", address);
            Map result = (Map) requestAndResponse(ModuleE.AC.abbr, "ac_getAccountWhitelistInfo", param);
            if (result == null) {
                return null;
            }
            AccountWhitelistDTO dto = new AccountWhitelistDTO();
            dto.setAddress((String) result.get("address"));
            dto.setExtend((String) result.get("extend"));
            Object typesObj = result.get("types");
            if (typesObj == null) {
                dto.setTypes(Collections.EMPTY_SET);
            } else {
                Set<Integer> types = new HashSet<>();
                List<Integer> typeList = (List<Integer>) typesObj;
                for (Integer type : typeList) {
                    types.add(type);
                }
                dto.setTypes(types);
            }
            return dto;
        } catch (Exception e) {
            return null;
        }
    }
}
