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
public class FarmTools implements CallRpc {

    public Result<Map<String, Object>> getFarm(int chainId, String farmHash) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("farmHash", farmHash);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_farmInfo", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    public Result<List<Map<String, Object>>> getFarmList(int chainId) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_farmlist", params, (Function<List<Map<String, Object>>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
    public Result<Map<String, Object>> sw_getstakeinfo(int chainId,String farmHash,String address) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("chainId", chainId);
        params.put("farmHash", farmHash);
        params.put("userAddress", address);
        try {
            return callRpc(ModuleE.SW.abbr, "sw_userstakeinfo", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

}
