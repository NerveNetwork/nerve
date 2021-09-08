package io.nuls.base.api.provider.farm;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.farm.facade.*;
import io.nuls.core.rpc.model.ModuleE;

import java.util.function.Function;

/**
 * @author Niels
 */
@Provider(Provider.ProviderType.RPC)
public class FarmProviderImpl extends BaseRpcService implements FarmProvider {

    @Override
    public Result<String> createFarm(CreateFarmReq req) {
        return call("sw_createFarm", req, null);
    }

    @Override
    public Result<String> stake(FarmStakeReq req) {
        return call("sw_farmstake", req, null);
    }

    @Override
    public Result<String> withdraw(FarmWithdrawReq req) {
        return call("sw_farmwithdraw", req, null);
    }

    @Override
    public Result<String> farmInfo(String farmHash) {
        return call("sw_getfarm", new FarmInfoReq(farmHash), null);
    }

    @Override
    public Result<String> farmUserInfo(String farmHash, String userAddress) {
        return call("sw_getstakeinfo", new FarmUserInfoReq(farmHash, userAddress), null);
    }

    @Override
    public Result<String> getFarmList() {
        return call("sw_getfarmlist", null, null);
    }

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        if (callback == null) {
            callback = res -> {
                return success(res);
            };
        }
        return callRpc(ModuleE.SW.abbr, method, req, callback);
    }
}
