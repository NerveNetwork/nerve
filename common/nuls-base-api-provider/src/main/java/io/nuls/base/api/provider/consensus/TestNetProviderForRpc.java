package io.nuls.base.api.provider.consensus;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.consensus.facade.InitNet;
import io.nuls.base.api.provider.consensus.facade.UpdateNet;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.rpc.model.ModuleE;

import java.util.Map;
import java.util.function.Function;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:59
 * @Description: 共识
 */
@Provider(Provider.ProviderType.RPC)
public class TestNetProviderForRpc extends BaseRpcService implements TestNetProvider {

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        return callRpc(ModuleE.CS.abbr,method,req,callback);
    }


    @Override
    public Result<Boolean> initNet(InitNet req) {
        return call("cs_initNet",req, (Function<Map, Result>) res -> {
            try {
                Boolean result = Boolean.valueOf(res.get("value").toString());
                return success(result);
            } catch (Exception e) {
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<Boolean> cleanNet(UpdateNet req) {
        return call("cs_cleanNet",req, (Function<Map, Result>) res -> {
            try {
                Boolean result = Boolean.valueOf(res.get("value").toString());
                return success(result);
            } catch (Exception e) {
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<Boolean> updateNet(UpdateNet req) {
        return call("cs_updateNet",req, (Function<Map, Result>) res -> {
            try {
                Boolean result = Boolean.valueOf(res.get("value").toString());
                return success(result);
            } catch (Exception e) {
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }
}
