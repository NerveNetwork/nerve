package io.nuls.provider.rpctools;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;

import java.util.Map;
import java.util.function.Function;


@Component
public class CommonTools implements CallRpc {

    public Result commonRequest(String module, String cmd, Map params) {
        try {
            return callRpc(module, cmd, params, (Function<Object, Result<Object>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
