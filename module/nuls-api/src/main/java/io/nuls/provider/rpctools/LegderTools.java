package io.nuls.provider.rpctools;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.provider.rpctools.vo.AccountBalance;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @Author: zhoulijun
 * @Time: 2019-06-12 17:31
 * @Description: 账本模块工具类
 */
@Component
public class LegderTools implements CallRpc {

    /**
     * 获取可用余额和nonce
     * Get the available balance and nonce
     */
    public Result<AccountBalance> getBalanceAndNonce(int chainId, int assetChainId, int assetId, String address) {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("assetChainId", assetChainId);
        params.put("address", address);
        params.put("assetId", assetId);
        try {
            return callRpc(ModuleE.LG.abbr, "getBalanceNonce", params, (Function<Map<String, Object>, Result<AccountBalance>>) map -> {
                if (map == null) {
                    return null;
                }
                AccountBalance balanceInfo = new AccountBalance();
                balanceInfo.setBalance(map.get("available").toString());
                balanceInfo.setTimeLock(map.get("timeHeightLocked").toString());
                balanceInfo.setConsensusLock(map.get("permanentLocked").toString());
                balanceInfo.setFreeze(map.get("freeze").toString());
                balanceInfo.setNonce((String) map.get("nonce"));
                balanceInfo.setTotalBalance(new BigInteger(balanceInfo.getBalance())
                        .add(new BigInteger(balanceInfo.getConsensusLock()))
                        .add(new BigInteger(balanceInfo.getTimeLock())).toString());
                balanceInfo.setNonceType((Integer) map.get("nonceType"));
                return new Result<>(balanceInfo);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    public Result<List> getAllAsset(int chainId) {
        Map<String, Object> params = new HashMap(2);
        params.put(Constants.CHAIN_ID, chainId);
        try {
            return callRpc(ModuleE.LG.abbr, "lg_get_all_asset", params, (Function<Map<String, Object>, Result<List>>) map -> {
                if (map == null) {
                    return null;
                }
                List assets = (List) map.get("assets");
                return new Result<>(assets);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询异构跨链资产在nerve链对应的资产信息
     * 当contractAddress=null时，默认查询异构链主资产
     */
    public Result<Map> getHeterogeneousChainAssetInfo(int heterogeneousChainId, String contractAddress) {
        Map<String, Object> params = new HashMap(2);

        if (StringUtils.isNotBlank(contractAddress)) {
            params.put("heterogeneousChainId", heterogeneousChainId);
            params.put("contractAddress", contractAddress);
            try {
                return callRpc(ModuleE.CV.abbr, "cv_get_heterogeneous_chain_asset_info_by_address", params, (Function<Map<String, Object>, Result<Map>>) map -> {
                    return new Result<>(map);
                });
            } catch (NulsRuntimeException e) {
                return Result.fail(e.getCode(), e.getMessage());
            }
        } else {
            params.put("heterogeneousChainId", heterogeneousChainId);
            params.put("heterogeneousAssetId", 1);
            try {
                return callRpc(ModuleE.CV.abbr, "cv_get_heterogeneous_chain_asset_info_by_id", params, (Function<Map<String, Object>, Result<Map>>) map -> {
                    return new Result<>(map);
                });
            } catch (NulsRuntimeException e) {
                return Result.fail(e.getCode(), e.getMessage());
            }
        }
    }

    public Result<Map> getAssetInfo(int chainId, int assetChainId, int assetId) {
        Map<String, Object> params = new HashMap(2);
        params.put("chainId",chainId);
        params.put("assetChainId",assetChainId);
        params.put("assetId",assetId);

        return callRpc(ModuleE.LG.abbr,"lg_get_asset",params,(Function<Map<String, Object>, Result<Map>>) map -> {
            return new Result<>(map);
        });
    }
}
