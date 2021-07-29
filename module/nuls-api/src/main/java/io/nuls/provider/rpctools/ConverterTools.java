package io.nuls.provider.rpctools;

import io.nuls.base.api.provider.Result;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.provider.model.dto.VirtualBankDirectorDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Component
public class ConverterTools implements CallRpc {


    /**
     * 查询异构链地址
     */
    public Result getHeterogeneousAddress(int chainId, int heterogeneousChainId, String packingAddress) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chainId", chainId);
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("packingAddress", packingAddress);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_get_heterogeneous_address", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询异构链主资产信息
     */
    public Result getHeterogeneousMainAsset(int heterogeneousChainId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousAssetId", 1);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_get_heterogeneous_chain_asset_info_by_id", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 重新将异构链提现交易放入task, 重发消息
     */
    public Result retryWithdrawalMsg(int chainId, String hash) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chainId", chainId);
        params.put("hash", hash);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_retry_withdrawal", params, (Function<Map<String, Object>, Result>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 虚拟银行成员信息
     * @param chainId
     * @return
     */
    public Result<List<VirtualBankDirectorDTO>> getVirtualBankInfo(int chainId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chainId", chainId);
        // 默认不查余额 容易超时
//        params.put("balance", true);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_virtualBankInfo", params, (Function<Map<String, Object>, Result<List<VirtualBankDirectorDTO>>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res.get("list"));
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 获取已撤销虚拟银行资格节点地址列表
     * @param chainId
     * @return
     */
    public Result<String> getDisqualification(int chainId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chainId", chainId);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_disqualification", params, (Function<Map<String, Object>, Result<String>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res.get("list"));
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 申请提案
     * @param chainId
     * @return
     */
    public Result<String> proposal(Map params) {
        try {
            return callRpc(ModuleE.CV.abbr, "cv_proposal", params, (Function<Map<String, Object>, Result<String>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 追加手续费
     * @return
     */
    public Result<String> withdrawalAdditionalFee(Map params) {
        try {
            return callRpc(ModuleE.CV.abbr, "cv_withdrawal_additional_fee", params, (Function<Map<String, Object>, Result<String>>) res -> {
                if(res == null){
                    return new Result();
                }
                return new Result(res);
            });
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询提案信息（序列化字符串）
     */
    public Result<String> getProposalInfo(int chainId, String proposalTxHash) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chainId", chainId);
        params.put("proposalTxHash", proposalTxHash);
        try {
            return callRpc(ModuleE.CV.abbr, "cv_getProposalInfo", params, (Function<String, Result<String>>) res -> new Result(res));
        } catch (NulsRuntimeException e) {
            return Result.fail(e.getCode(), e.getMessage());
        }
    }
}
