package io.nuls.base.api.provider.dex;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.dex.facode.CoinTradingReq;
import io.nuls.base.api.provider.dex.facode.DexQueryReq;
import io.nuls.base.api.provider.dex.facode.EditTradingReq;
import io.nuls.core.rpc.model.ModuleE;

import java.util.Map;
import java.util.function.Function;

@Provider(Provider.ProviderType.RPC)
public class DexProviderForRpc extends BaseRpcService implements DexProvider {
    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        return callRpc(ModuleE.DX.abbr, method, req, callback);
    }

    @Override
    public Result<String> createTrading(CoinTradingReq req) {
        return callReturnString("dx_createCoinTradingTx", req, "txHash");
    }

    @Override
    public Result<String> editTrading(EditTradingReq req) {
        return callReturnString("dx_editCoinTradingTx", req, "txHash");
    }

    @Override
    public Result<Map> getTrading(DexQueryReq req) {
        return callResutlMap("dx_getCoinTrading", req);
    }
}
