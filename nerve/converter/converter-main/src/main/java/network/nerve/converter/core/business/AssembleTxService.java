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
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;
import network.nerve.converter.model.dto.*;
import network.nerve.converter.model.txdata.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Assembly transaction
 *
 * @author: Loki
 * @date: 2020-02-28
 */
public interface AssembleTxService {

    /**
     * Virtual Bank Node Change Transaction
     *
     * @param chain        Chain information
     * @param inAgentList  Joined node information
     * @param outAgentList Removed node information
     * @param outHeight    Stop node transaction confirmation height
     * @param txTime       Transaction time
     * @return
     * @throws NulsException
     */
    Transaction createChangeVirtualBankTx(Chain chain,
                                          List<byte[]> inAgentList,
                                          List<byte[]> outAgentList,
                                          long outHeight,
                                          long txTime) throws NulsException;

    /**
     *  Create transactions only, Do not send to the transaction module
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
     * confirm Virtual Bank Node Change Transaction
     *
     * @param chain                   Chain information
     * @param changeVirtualBankTxHash Virtual Bank Node Change Transactionhash
     * @param listConfirmed           Heterogeneous chain change confirmation information
     * @param txTime                  Transaction time
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
     * Initialize heterogeneous chain transactions
     * PS:When the node block is in synchronization mode,Do not trigger the creation of this transaction
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
     * Recharge transaction
     *
     * @param chain         Chain information
     * @param rechargeTxDTO Recharge data
     * @return
     * @throws NulsException
     */
    Transaction createRechargeTx(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException;

    Transaction createRechargeTxWithoutSign(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException;

    /**
     * Heterogeneous chain recharge pending confirmation transaction
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
     * Withdrawal transactions
     *
     * @param chain           Chain information
     * @param withdrawalTxDTO Withdrawal data
     * @return
     * @throws NulsException
     */
    Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException;

    /**
     * Additional withdrawal fee transaction
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
     * Confirm withdrawal transactions
     *
     * @param chain                   Chain information
     * @param confirmWithdrawalTxData Transaction information
     * @param txTime                  Transaction time
     * @return
     * @throws NulsException
     */
    Transaction createConfirmWithdrawalTx(Chain chain,
                                          ConfirmWithdrawalTxData confirmWithdrawalTxData,
                                          long txTime, byte[] remark) throws NulsException;

    Transaction createConfirmWithdrawalTxWithoutSign(Chain chain,
                                                     ConfirmWithdrawalTxData confirmWithdrawalTxData,
                                                     long txTime, byte[] remark) throws NulsException;


    /**
     * Directly initiated externally(Assembled)Proposal transaction
     * @param chain
     * @param tx
     * @return
     * @throws NulsException
     */
    Transaction processProposalTx(Chain chain, Transaction tx) throws NulsException;

    /**
     * Initiate proposal transactions
     * Sign as a virtual bank node by default, Not a virtual bank node will not be able to sign the transaction
     *
     * @param chain         Chain information
     * @param proposalTxDTO Proposal Information
     * @return
     * @throws NulsException
     */
    Transaction createProposalTx(Chain chain, ProposalTxDTO proposalTxDTO) throws NulsException;

    /**
     * Proposal voting transaction
     * Sign with incoming account password information
     *
     * @param chain          Chain information
     * @param proposalTxHash Proposal transactionhash
     * @param choice         vote
     * @param remark         Remarks
     * @param signAccount    Signature account information
     * @return
     * @throws NulsException
     */
    Transaction createVoteProposalTx(Chain chain,
                                     NulsHash proposalTxHash,
                                     byte choice,
                                     String remark,
                                     SignAccountDTO signAccount) throws NulsException;

    /**
     * Confirm successful transaction of proposal execution
     * @param chain
     * @param confirmProposalTxData
     * @return
     * @throws NulsException
     */
    Transaction createConfirmProposalTx(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException;

    Transaction createConfirmProposalTxWithoutSign(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException;

    /**
     * Assemble and publish Subsidy handling fee transaction
     *
     * @param chain
     * @param basisTxHash If it is a proposal Subsidy handling fee, So the transaction is[Confirm proposal transaction]
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
     * Assemble and publish waiting for registration of heterogeneous chain contract asset transactions
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
     * Assemble and publish registration of heterogeneous chain master asset transactions
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
     * Assemble and publish registration of heterogeneous chain master asset bindingNERVEAsset trading
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
     * Assemble and publish completed registration of heterogeneous chain contract asset transactions
     *
     * @param chain
     * @return
     * @throws NulsException
     * @Transaction pendingTx
     */
    Transaction createHeterogeneousContractAssetRegCompleteTx(Chain chain, Transaction pendingTx) throws NulsException;

    /**
     * Calculate assembly based on reward addresscointodata
     * Due to the fact that the reward address list is obtained through heterogeneous signature addresses,If multiple virtual banks have the same reward address,
     * So the reward address list will have duplicate reward addresses, Therefore, we need to merge the multiple reward amounts obtained from this address.
     * @param listRewardAddress
     * @param amount
     */
    Map<String, BigInteger> calculateDistributionFeeCoinToAmount(List<byte[]> listRewardAddress, BigInteger amount);


    /**
     * Create and reset virtual banking heterogeneous chains(contract)transaction
     * @param chain
     * @param heterogeneousChainId
     * @param signAccount
     * @return
     * @throws NulsException
     */
    Transaction createResetVirtualBankTx(Chain chain, int heterogeneousChainId, SignAccountDTO signAccount) throws NulsException;

    /**
     * Create Confirmation Reset Virtual Bank Heterogeneous Chain(contract)transaction
     * @param chain
     * @param txData
     * @return
     * @throws NulsException
     */
    Transaction createConfirmResetVirtualBankTx(Chain chain, ConfirmResetVirtualBankTxData txData, long txTime) throws NulsException;

    /**
     * Calculate the total amount of handling fees that should be subsidized
     * @param chain
     * @param height
     * @param basisTxHash Original transactionhash
     * @param isProposal
     * @return
     */
    WithdrawalTotalFeeInfo calculateFee(Chain chain, Long height, Transaction basisTxHash, boolean isProposal) throws NulsException;

    WithdrawalTotalFeeInfo calculateFee(Chain chain, Transaction basisTxHash, boolean isProposal) throws NulsException;

    /**
     * Obtain the total amount of heterogeneous chain transaction fees for the original return proposal transaction payment(Excluding in chain transaction packaging fees)
     * Fixed transaction fees in proposal transactions + Additional handling fees for this transaction(If there is any)
     * @param chain
     * @param hash
     * @return
     */
     BigInteger calculateRefundTotalFee(Chain chain, String hash);

    /**
     * Obtain the total amount of heterogeneous chain transaction fees for withdrawal transaction payments(Excluding in chain transaction packaging fees)
     * Handling fees in withdrawal transactions + Additional handling fees for this transaction(If there is any)
     * @param chain
     * @param withdrawalTx
     * @return
     * @throws NulsException
     */
    WithdrawalTotalFeeInfo calculateWithdrawalTotalFee(Chain chain, Transaction withdrawalTx) throws NulsException;

    Transaction createRechargeTxOfBtcSys(Chain nerveChain, RechargeTxOfBtcSysDTO dto) throws NulsException;
    Transaction createRechargeTxOfBtcSysWithoutSign(Chain nerveChain, RechargeTxOfBtcSysDTO dto) throws NulsException;

    Transaction createWithdrawUTXOTx(Chain chain, WithdrawalUTXOTxData txData, long txTime) throws NulsException;
    Transaction createWithdrawUTXOTxWithoutSign(Chain chain, WithdrawalUTXOTxData txData, long txTime) throws NulsException;

    Transaction createWithdrawlFeeLogTx(Chain chain, WithdrawalFeeLog txData, long txTime, byte[] remark) throws NulsException;
    Transaction createWithdrawlFeeLogTxWithoutSign(Chain chain, WithdrawalFeeLog txData, long txTime, byte[] remark) throws NulsException;

    Transaction createUnlockUTXOTx(Chain chain,
                                 String from,
                                 String password,
                                 String nerveTxHash,
                                 int forceUnlock, Integer htgChainId) throws NulsException;
}
