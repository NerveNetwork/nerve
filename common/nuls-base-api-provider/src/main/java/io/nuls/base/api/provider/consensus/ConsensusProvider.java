package io.nuls.base.api.provider.consensus;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.consensus.facade.*;
import io.nuls.base.api.provider.ledger.facade.AssetInfo;
import io.nuls.base.api.provider.transaction.facade.MultiSignTransferRes;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:43
 * @Description:
 * consensus provider
 */
public interface ConsensusProvider {

    /**
     * create consensus node
     * @param req
     * @return
     */
    Result<String> createAgent(CreateAgentReq req);

    /**
     * create consensus node
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> createAgentForMultiSignAccount(CreateMultiSignAgentReq req);

    /**
     * stop consensus node
     * @param req
     * @return
     */
    Result<String> stopAgent(StopAgentReq req);

    /**
     * stop consensus node
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> stopAgentForMultiSignAccount(StopMultiSignAgentReq req);

    /**
     * 改变节点押金
     *
     * @param req
     * @return
     */
    Result<String> changeAgentDeposit(AgentDepositChangeReq req);

    /**
     * 改变节点押金
     *
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> changeMultiAgentDeposit(MultiAgentDepositChangeReq req);


    /**
     * 委托共识
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> multiSignJoinStacking(MultiSignJoinStackingReq req);


    /**
     * staking 参与委托
     * @param req
     * @return
     */
    Result<String> deposit(DepositReq req);

    /**
     * staking 退出委托
     * @param req
     * @return
     */
    Result<String> withdraw(WithdrawReq req);

    Result<String> batchWithdraw(WithdrawReq req);


    /**
     * 退出委托
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> withdrawForMultiSignAccount(MultiSignAccountWithdrawReq req);


    /**
     * 查询节点信息
     * @param req
     * @return
     */
    Result<AgentInfo> getAgentInfo(GetAgentInfoReq req);


    /**
     * 查询节点列表
     * @param req
     * @return
     */
    Result<AgentInfo> getAgentList(GetAgentListReq req);

    /**
     * 查询委托列表
     * @param req
     * @return
     */
    Result<DepositInfo> getDepositList(GetDepositListReq req);

    /**
     * 根据symbol查询参与stacking资产的id
     * @param req
     * @return
     */
    Result<AssetInfo> getStatcingAssetBySymbol(GetStackingAssetBySymbolReq req);

    /**
     * 查询退出保证金/退出节点对应的交易列表
     * @param req
     * @return
     */
    Result<ReduceNonceInfo> getReduceNonceList(GetReduceNonceReq req);


    /**
     * 获取可参与stacking的资产列表
     * @param req
     * @return
     */
    Result<AssetInfo> getCanStackingAssetList(GetCanStackingAssetListReq req);

    /**
     * 获取指定区块高度的区块总奖励数
     * @param req
     * @return
     */
    Result<BigDecimal> getTotalRewardForBlockHeight(GetTotalRewardForBlockHeightReq req);

    Result<String> batchStakingMerge(BatchStakingMergeReq batchStakingMergeReq);
}
