package io.nuls.base.api.provider.ledger;

import io.nuls.base.api.provider.BaseReq;
import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.account.facade.AccountInfo;
import io.nuls.base.api.provider.ledger.facade.*;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.log.Log;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 13:44
 * @Description: Function Description
 */
@Provider(Provider.ProviderType.RPC)
public class LedgerProviderForRpc extends BaseRpcService implements LedgerProvider {

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        return callRpc(ModuleE.LG.abbr, method, req, callback);
    }

    @Override
    public Result<AccountBalanceInfo> getBalance(GetBalanceReq req) {
        Function<Map, Result> callback = res -> {
            BigInteger total = new BigInteger(String.valueOf(res.get("total")));
            BigInteger freeze = new BigInteger(String.valueOf(res.get("freeze")));
            BigInteger available = new BigInteger(String.valueOf(res.get("available")));
            AccountBalanceInfo info = new AccountBalanceInfo();
            info.setAvailable(available);
            info.setFreeze(freeze);
            info.setTotal(total);
            return success(info);
        };
        return call("getBalance", req, callback);
    }

    @Override
    public Result<Map> getLocalAsset(GetAssetReq req) {
        return callResutlMap("getAssetRegInfoByHash", req);
    }

    @Override
    public Result<Map> getContractAsset(ContractAsset req) {
        return callResutlMap("getAssetContract", req);
    }

    @Override
    public Result<Map> regLocalAsset(RegLocalAssetReq req) {
        return callResutlMap("chainAssetTxReg", req);
    }

    @Override
    public Result<AssetInfo> getAssetList(GetAssetListReq req) {
        return _call("lg_get_all_asset", new BaseReq(), res -> {
            try {
                List<AssetInfo> list = ((List<Map<String, Object>>) res.get("assets")).stream().map(data->{
                    AssetInfo assetInfo = new AssetInfo();
                    assetInfo.setAssetChainId((Integer)data.get("assetChainId"));
                    assetInfo.setAssetId((Integer) data.get("assetId"));
                    assetInfo.setSymbol((String) data.get("assetSymbol"));
                    assetInfo.setAssetType((Integer) data.get("assetType"));
                    assetInfo.setDecimals((Integer) data.get("decimalPlace"));
                    return assetInfo;
                }).collect(Collectors.toList());
                if(req.getAssetType() > 0){
                    list = list.stream().filter(d->d.getAssetType() == req.getAssetType()).collect(Collectors.toList());
                }
                return success(list);
            } catch (Exception e) {
                Log.error("lg_get_all_asset fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    private <T> Result<T> _call(String method, Object req, Function<Map, Result> callback) {
        return call(method, req, callback);
    }

}
