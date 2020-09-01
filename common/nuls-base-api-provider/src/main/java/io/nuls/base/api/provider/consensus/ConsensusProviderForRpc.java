package io.nuls.base.api.provider.consensus;

import io.nuls.base.api.provider.BaseRpcService;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.consensus.facade.*;
import io.nuls.base.api.provider.ledger.facade.AssetInfo;
import io.nuls.base.api.provider.transaction.facade.MultiSignTransferRes;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.log.Log;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:59
 * @Description: 共识
 */
@Provider(Provider.ProviderType.RPC)
public class ConsensusProviderForRpc extends BaseRpcService implements ConsensusProvider {

    @Override
    protected <T, R> Result<T> call(String method, Object req, Function<R, Result> callback) {
        return callRpc(ModuleE.CS.abbr, method, req, callback);
    }

    @Override
    public Result<String> createAgent(CreateAgentReq req) {
        return callReturnString("cs_createAgent", req, "txHash");
    }

    @Override
    public Result<MultiSignTransferRes> createAgentForMultiSignAccount(CreateMultiSignAgentReq req) {
        return callRpc(ModuleE.CS.abbr, "cs_createMultiAgent", req, (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
    }

    @Override
    public Result<String> stopAgent(StopAgentReq req) {
        return callReturnString("cs_stopAgent", req, "txHash");
    }

    @Override
    public Result<MultiSignTransferRes> stopAgentForMultiSignAccount(StopMultiSignAgentReq req) {
        return callRpc(ModuleE.CS.abbr, "cs_stopMultiAgent", req, (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
    }

    @Override
    public Result<String> changeAgentDeposit(AgentDepositChangeReq req) {
        if (req.getAmount().equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("deposit can't be zero");
        }
        if (req.getAmount().compareTo(BigInteger.ZERO) > 0) {
            return callReturnString("cs_appendAgentDeposit", req, "txHash");
        } else {
            req.setAmount(req.getAmount().abs());
            return callReturnString("cs_reduceAgentDeposit", req, "txHash");
        }
    }

    @Override
    public Result<MultiSignTransferRes> changeMultiAgentDeposit(MultiAgentDepositChangeReq req) {
        if (req.getAmount().equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("deposit can't be zero");
        }
        if (req.getAmount().compareTo(BigInteger.ZERO) > 0) {
            return callRpc(ModuleE.CS.abbr, "cs_appendMultiAgentDeposit", req,  (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
        } else {
            req.setAmount(req.getAmount().abs());
            return callRpc(ModuleE.CS.abbr, "cs_reduceMultiAgentDeposit", req,  (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
        }
    }

    @Override
    public Result<MultiSignTransferRes> multiSignJoinStacking(MultiSignJoinStackingReq req) {
        return callRpc(ModuleE.CS.abbr, "cs_multiDeposit", req, (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
    }

    @Override
    public Result<String> deposit(DepositReq req) {
        return callReturnString("cs_depositToStacking", req, "txHash");
    }

    @Override
    public Result<String> withdraw(WithdrawReq req) {
        return callReturnString("cs_withdraw", req, "txHash");
    }
    @Override
    public Result<String> batchWithdraw(WithdrawReq req) {
        return callReturnString("cs_batch_withdraw", req, "txHash");
    }

    @Override
    public Result<String> batchStakingMerge(BatchStakingMergeReq req) {
        return callReturnString("cs_batch_staking_merge", req, "txHash");
    }

    @Override
    public Result<MultiSignTransferRes> withdrawForMultiSignAccount(MultiSignAccountWithdrawReq req) {
        return callRpc(ModuleE.CS.abbr, "cs_multiWithdraw", req, (Function<Map, Result>) (data -> success(MapUtils.mapToBean(data, new MultiSignTransferRes()))));
    }

    @Override
    public Result<AgentInfo> getAgentInfo(GetAgentInfoReq req) {
        return call("cs_getAgentInfo", req, (Function<Map, Result>) res -> {
            if (res == null) {
                return fail(RPC_ERROR_CODE, "agent not found");
            }
            AgentInfo agentInfo = MapUtils.mapToBean(res, new AgentInfo());

            return success(agentInfo);
        });
    }

    @Override
    public Result<AgentInfo> getAgentList(GetAgentListReq req) {
        return call("cs_getAgentList", req, (Function<Map, Result>) res -> {
            try {
                List<AgentInfo> list = MapUtils.mapsToObjects((List<Map<String, Object>>) res.get("list"), AgentInfo.class);
                return success(list);
            } catch (Exception e) {
                Log.error("cs_getAgentList fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<DepositInfo> getDepositList(GetDepositListReq req) {
        return call("cs_getDepositList", req, (Function<Map, Result>) res -> {
            try {
                List<DepositInfo> list = MapUtils.mapsToObjects((List<Map<String, Object>>) res.get("list"), DepositInfo.class);
                return success(list);
            } catch (Exception e) {
                Log.error("cs_getDepositList fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<AssetInfo> getStatcingAssetBySymbol(GetStackingAssetBySymbolReq req) {
        return call("cs_getAssetBySymbol", req, (Function<Map, Result>) res -> {
            AssetInfo info = MapUtils.mapToBean(res, new AssetInfo());
            return success(info);
        });
    }

    @Override
    public Result<ReduceNonceInfo> getReduceNonceList(GetReduceNonceReq req) {
        return call("cs_getReduceDepositList", req, (Function<Map, Result>) res -> {
            try {
                List<ReduceNonceInfo> list = MapUtils.mapsToObjects((List<Map<String, Object>>) res.get("list"), ReduceNonceInfo.class);
                return success(list);
            } catch (Exception e) {
                Log.error("cs_getReduceDepositList fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<AssetInfo> getCanStackingAssetList(GetCanStackingAssetListReq req) {
        return call("cs_getCanStackingAssetList", req, (Function<Map, Result>) res -> {
            try {
                List<AssetInfo> list = ((List<Map<String, Object>>) res.get("list")).stream().map(data->{
                    AssetInfo assetInfo = new AssetInfo();
                    assetInfo.setAssetChainId((Integer)data.get("chainId"));
                    assetInfo.setAssetId((Integer) data.get("assetId"));
                    assetInfo.setSymbol((String) data.get("simple"));
                    return assetInfo;
                }).collect(Collectors.toList());
                return success(list);
            } catch (Exception e) {
                Log.error("cs_getCanStackingAssetList fail", e);
                return fail(CommonCodeConstanst.FAILED);
            }
        });
    }

    @Override
    public Result<BigDecimal> getTotalRewardForBlockHeight(GetTotalRewardForBlockHeightReq req) {
        return call("cs_getRewardUnit", req, (Function<Map, Result>) res -> {
            BigDecimal reward = new BigDecimal(res.get("value").toString());
            return success(reward);
        });
    }
}
