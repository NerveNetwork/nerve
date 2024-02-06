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
     * Change node deposit
     *
     * @param req
     * @return
     */
    Result<String> changeAgentDeposit(AgentDepositChangeReq req);

    /**
     * Change node deposit
     *
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> changeMultiAgentDeposit(MultiAgentDepositChangeReq req);


    /**
     * Commission consensus
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> multiSignJoinStacking(MultiSignJoinStackingReq req);


    /**
     * staking Participate in commission
     * @param req
     * @return
     */
    Result<String> deposit(DepositReq req);

    /**
     * staking Exit the commission
     * @param req
     * @return
     */
    Result<String> withdraw(WithdrawReq req);

    Result<String> batchWithdraw(WithdrawReq req);


    /**
     * Exit the commission
     * @param req
     * @return
     */
    Result<MultiSignTransferRes> withdrawForMultiSignAccount(MultiSignAccountWithdrawReq req);


    /**
     * Query node information
     * @param req
     * @return
     */
    Result<AgentInfo> getAgentInfo(GetAgentInfoReq req);


    /**
     * Query node list
     * @param req
     * @return
     */
    Result<AgentInfo> getAgentList(GetAgentListReq req);

    /**
     * Query delegation list
     * @param req
     * @return
     */
    Result<DepositInfo> getDepositList(GetDepositListReq req);

    /**
     * according tosymbolQuery participationstackingAssetsid
     * @param req
     * @return
     */
    Result<AssetInfo> getStatcingAssetBySymbol(GetStackingAssetBySymbolReq req);

    /**
     * Query withdrawal deposit/Exit the transaction list corresponding to the node
     * @param req
     * @return
     */
    Result<ReduceNonceInfo> getReduceNonceList(GetReduceNonceReq req);


    /**
     * Obtain participationstackingAsset List for
     * @param req
     * @return
     */
    Result<AssetInfo> getCanStackingAssetList(GetCanStackingAssetListReq req);

    /**
     * Obtain the total number of rewards for the specified block height
     * @param req
     * @return
     */
    Result<BigDecimal> getTotalRewardForBlockHeight(GetTotalRewardForBlockHeightReq req);

    Result<String> batchStakingMerge(BatchStakingMergeReq batchStakingMergeReq);
}
