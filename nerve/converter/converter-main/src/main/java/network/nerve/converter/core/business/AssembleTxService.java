/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter.core.business;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;
import network.nerve.converter.model.dto.*;
import network.nerve.converter.model.txdata.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 组装交易
 *
 * @author: Loki
 * @date: 2020-02-28
 */
public interface AssembleTxService {

    /**
     * 虚拟银行节点变更交易
     *
     * @param chain        链信息
     * @param inAgentList  加入的节点信息
     * @param outAgentList 移除的节点信息
     * @param outHeight    停止节点交易确认高度
     * @param txTime       交易时间
     * @return
     * @throws NulsException
     */
    Transaction createChangeVirtualBankTx(Chain chain,
                                          List<byte[]> inAgentList,
                                          List<byte[]> outAgentList,
                                          long outHeight,
                                          long txTime) throws NulsException;

    /**
     *  只创建交易, 不发送到交易模块
     * @param chain
     * @param inAgentList
     * @param outAgentList
     * @param outHeight
     * @param txTime
     * @return
     * @throws NulsException
     */
    Transaction assembleChangeVirtualBankTx(Chain chain,
                                          List<byte[]> inAgentList,
                                          List<byte[]> outAgentList,
                                          long outHeight,
                                          long txTime) throws NulsException;

    /**
     * 确认 虚拟银行节点变更交易
     *
     * @param chain                   链信息
     * @param changeVirtualBankTxHash 虚拟银行节点变更交易hash
     * @param listConfirmed           异构链变更确认信息
     * @param txTime                  交易时间
     * @return
     * @throws NulsException
     */
    Transaction createConfirmedChangeVirtualBankTx(Chain chain,
                                                   NulsHash changeVirtualBankTxHash,
                                                   List<HeterogeneousConfirmedVirtualBank> listConfirmed,
                                                   long txTime) throws NulsException;

    Transaction createConfirmedChangeVirtualBankTxWithoutSign(Chain chain,
                                                              NulsHash changeVirtualBankTxHash,
                                                              List<HeterogeneousConfirmedVirtualBank> listConfirmed,
                                                              long txTime) throws NulsException;

    /**
     * 初始化异构链交易
     * PS:当节点区块处于正在同步模式时,不触发该笔交易的创建
     *
     * @param chain
     * @param heterogeneousChainId
     * @param txTime
     * @return
     * @throws NulsException
     */
    Transaction createInitializeHeterogeneousTx(Chain chain,
                                                int heterogeneousChainId,
            /*List<byte[]> listDirector,*/
                                                long txTime) throws NulsException;

    /**
     * 充值交易
     *
     * @param chain         链信息
     * @param rechargeTxDTO 充值数据
     * @return
     * @throws NulsException
     */
    Transaction createRechargeTx(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException;

    Transaction createRechargeTxWithoutSign(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException;

    /**
     * 异构链充值待确认交易
     * @param chain
     * @param rechargeUnconfirmedTxData
     * @return
     * @throws NulsException
     */
    Transaction rechargeUnconfirmedTx(Chain chain, RechargeUnconfirmedTxData rechargeUnconfirmedTxData, long heterogeneousTxTime) throws NulsException;
    Transaction rechargeUnconfirmedTxWithoutSign(Chain chain, RechargeUnconfirmedTxData rechargeUnconfirmedTxData, long heterogeneousTxTime) throws NulsException;

    Transaction oneClickCrossChainUnconfirmedTx(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long heterogeneousTxTime) throws NulsException;
    Transaction oneClickCrossChainUnconfirmedTxWithoutSign(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long heterogeneousTxTime) throws NulsException;

    Transaction createOneClickCrossChainTx(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long heterogeneousTxTime) throws NulsException;
    Transaction createOneClickCrossChainTxWithoutSign(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long heterogeneousTxTime) throws NulsException;

    Transaction createAddFeeCrossChainTx(Chain chain, AddFeeCrossChainTxDTO dto, long heterogeneousTxTime) throws NulsException;
    Transaction createAddFeeCrossChainTxWithoutSign(Chain chain, AddFeeCrossChainTxDTO dto, long heterogeneousTxTime) throws NulsException;

    /**
     * 提现交易
     *
     * @param chain           链信息
     * @param withdrawalTxDTO 提现数据
     * @return
     * @throws NulsException
     */
    Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException;

    /**
     * 追加提现手续费交易
     * @param chain
     * @param withdrawalAdditionalFeeTxDTO
     * @return
     * @throws NulsException
     */
    Transaction withdrawalAdditionalFeeTx(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException;

    /**
     *
     * @param chain
     * @param txData
     * @param heterogeneousTxTime
     * @return
     * @throws NulsException
     */
    Transaction withdrawalHeterogeneousSendTx(Chain chain, WithdrawalHeterogeneousSendTxData txData, long heterogeneousTxTime) throws NulsException;

    /**
     * 确认提现交易
     *
     * @param chain                   链信息
     * @param confirmWithdrawalTxData 交易信息
     * @param txTime                  交易时间
     * @return
     * @throws NulsException
     */
    Transaction createConfirmWithdrawalTx(Chain chain,
                                          ConfirmWithdrawalTxData confirmWithdrawalTxData,
                                          long txTime) throws NulsException;

    Transaction createConfirmWithdrawalTxWithoutSign(Chain chain,
                                                     ConfirmWithdrawalTxData confirmWithdrawalTxData,
                                                     long txTime) throws NulsException;


    /**
     * 外部直接发起(已组装好的)提案交易
     * @param chain
     * @param tx
     * @return
     * @throws NulsException
     */
    Transaction processProposalTx(Chain chain, Transaction tx) throws NulsException;

    /**
     * 发起提案交易
     * 默认以虚拟银行节点的身份签名, 不是虚拟银行节点将无法签名该交易
     *
     * @param chain         链信息
     * @param proposalTxDTO 提案信息
     * @return
     * @throws NulsException
     */
    Transaction createProposalTx(Chain chain, ProposalTxDTO proposalTxDTO) throws NulsException;

    /**
     * 提案投票交易
     * 用传入账户密码信息签名
     *
     * @param chain          链信息
     * @param proposalTxHash 提案交易hash
     * @param choice         表决
     * @param remark         备注
     * @param signAccount    签名账户信息
     * @return
     * @throws NulsException
     */
    Transaction createVoteProposalTx(Chain chain,
                                     NulsHash proposalTxHash,
                                     byte choice,
                                     String remark,
                                     SignAccountDTO signAccount) throws NulsException;

    /**
     * 确认提案执行成功交易
     * @param chain
     * @param confirmProposalTxData
     * @return
     * @throws NulsException
     */
    Transaction createConfirmProposalTx(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException;

    Transaction createConfirmProposalTxWithoutSign(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException;

    /**
     * 组装并发布 补贴手续费交易
     *
     * @param chain
     * @param basisTxHash 如果是提案 补贴手续费, 那该交易为[确认提案交易]
     * @param listRewardAddress
     * @param txTime
     * @return
     * @throws NulsException
     */
    Transaction createDistributionFeeTx(Chain chain,
                                        NulsHash basisTxHash,
                                        List<byte[]> listRewardAddress,
                                        long txTime,
                                        boolean isProposal) throws NulsException;

    /**
     * 组装并发布等待注册异构链合约资产交易
     *
     * @param chain
     * @param from
     * @param password
     * @param heterogeneousChainId
     * @param decimals
     * @param symbol
     * @param contractAddress
     * @param remark
     * @return
     * @throws NulsException
     */
    Transaction createHeterogeneousContractAssetRegPendingTx(Chain chain,
                                                             String from,
                                                             String password,
                                                             int heterogeneousChainId,
                                                             int decimals,
                                                             String symbol,
                                                             String contractAddress, String remark) throws NulsException;
    /**
     * 组装并发布注册异构链主资产交易
     *
     * @param chain
     * @param from
     * @param password
     * @param heterogeneousChainId
     * @param remark
     * @return
     * @throws NulsException
     */
    Transaction createHeterogeneousMainAssetRegTx(Chain chain,
                                                             String from,
                                                             String password,
                                                             int heterogeneousChainId, String remark) throws NulsException;

    /**
     * 组装并发布注册异构链主资产绑定NERVE资产交易
     *
     * @param chain
     * @param from
     * @param password
     * @param heterogeneousChainId
     * @param nerveAssetChainId
     * @param nerveAssetId
     * @param remark
     * @return
     * @throws NulsException
     */
    Transaction createHeterogeneousMainAssetBindTx(Chain chain,
                                                             String from,
                                                             String password,
                                                             int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId, String remark) throws NulsException;

    /**
     * 组装并发布完成注册异构链合约资产交易
     *
     * @param chain
     * @return
     * @throws NulsException
     * @Transaction pendingTx
     */
    Transaction createHeterogeneousContractAssetRegCompleteTx(Chain chain, Transaction pendingTx) throws NulsException;

    /**
     * 根据奖励地址计算出组装cointo数据
     * 由于奖励地址列表通过异构签名地址得到,如果出现多个虚拟银行奖励地址相同,
     * 那么奖励地址列表就会有重复的奖励地址, 因此要合并该地址得到的多笔奖励金额.
     * @param listRewardAddress
     * @param amount
     */
    Map<String, BigInteger> calculateDistributionFeeCoinToAmount(List<byte[]> listRewardAddress, BigInteger amount);


    /**
     * 创建重置虚拟银行异构链(合约)交易
     * @param chain
     * @param heterogeneousChainId
     * @param signAccount
     * @return
     * @throws NulsException
     */
    Transaction createResetVirtualBankTx(Chain chain, int heterogeneousChainId, SignAccountDTO signAccount) throws NulsException;

    /**
     * 创建确认重置虚拟银行异构链(合约)交易
     * @param chain
     * @param txData
     * @return
     * @throws NulsException
     */
    Transaction createConfirmResetVirtualBankTx(Chain chain, ConfirmResetVirtualBankTxData txData, long txTime) throws NulsException;

    /**
     * 计算应补贴的手续费的总额
     * @param chain
     * @param height
     * @param basisTxHash 原始交易hash
     * @param isProposal
     * @return
     */
    WithdrawalTotalFeeInfo calculateFee(Chain chain, Long height, Transaction basisTxHash, boolean isProposal) throws NulsException;

    WithdrawalTotalFeeInfo calculateFee(Chain chain, Transaction basisTxHash, boolean isProposal) throws NulsException;

    /**
     * 获取原路退回提案交易支付的异构链手续费总额(不包含链内交易打包手续费)
     * 提案交易中的固定手续费 + 对该交易追加的手续费(如果有)
     * @param chain
     * @param hash
     * @return
     */
     BigInteger calculateRefundTotalFee(Chain chain, String hash);

    /**
     * 获取提现交易支付的异构链手续费总额(不包含链内交易打包手续费)
     * 提现交易中的手续费 + 对该交易追加的手续费(如果有)
     * @param chain
     * @param withdrawalTx
     * @return
     * @throws NulsException
     */
    WithdrawalTotalFeeInfo calculateWithdrawalTotalFee(Chain chain, Transaction withdrawalTx) throws NulsException;
}
