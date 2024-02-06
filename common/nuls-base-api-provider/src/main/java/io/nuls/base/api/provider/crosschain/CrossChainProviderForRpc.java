package io.nuls.base.api.provider.crosschain;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.crosschain.facade.*;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @Author: zhoulijun
 * @Time: 2019-05-06 17:14
 * @Description: Function Description
 */
@Provider(Provider.ProviderType.RPC)
public class CrossChainProviderForRpc extends BaseRpcService implements CrossChainProvider {


    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        return callRpc(ModuleE.CC.abbr,method,req,callback);
    }

    @Override
    public Result<String> createCrossTx(CreateCrossTxReq req) {
        return callReturnString("createCrossTx",req,"txHash");
    }


    @Override
    public Result<Integer> getCrossTxState(GetCrossTxStateReq req) {
        return _call("getCrossTxState",req,res->{
            if(res == null){
                return fail(RPC_ERROR_CODE,"tx not found");
            }
            Integer data = (Integer) res.get("value");
            return success(data);
        });
    }

    @Override
    public Result<List> getRegisteredChainInfoList() {
        return _call("getRegisteredChainInfoList",null,res->{
            if(res == null){
                return fail(RPC_ERROR_CODE,"data not found");
            }
            List data = (List) res.get("list");
            return success(data);
        });
    }

    @Override
    public Result<String> rehandleCtx(RehandleCtxReq req) {
        return  callReturnString("ctxRehandle",req,"msg");
    }

    private <T> Result<T> _call(String method, Object req, Function<Map, Result> callback){
        return call(method,req,callback);
    }

}
