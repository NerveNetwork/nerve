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

package network.nerve.converter.core.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcTxInfo;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.model.txdata.RechargeTxData;
import network.nerve.converter.model.txdata.WithdrawalAddFeeByCrossChainTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Recharge transaction business validator
 * (After creating the transaction)
 *
 * @author: Loki
 * @date: 2020/4/15
 */
@Component
public class RechargeVerifier {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        if (converterCoreApi.isProtocol16()) {
            this.validateProtocol16(chain, tx);
        } else {
            this._validate(chain, tx);
        }
    }

    public void _validate(Chain chain, Transaction tx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() > 1) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        CoinTo coinTo = listCoinTo.get(0);
        RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
        if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        // Through in chain assetsid Obtain heterogeneous chain information
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());

        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                heterogeneousAssetInfo.getChainId(),
                txData.getOriginalTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null != info) {
            heterogeneousRechargeValid(info, coinTo, heterogeneousAssetInfo);
        } else {
            // Check proposals (OriginalTxHash Not a heterogeneous chain transactionhash, Or it could be a proposalhash)
            ProposalPO proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
            if (null == proposalPO) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            proposalRechargeValid(chain, proposalPO, coinTo, heterogeneousAssetInfo);
        }

    }

    public void validateProtocol16(Chain chain, Transaction tx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() > 2) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
        if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                txData.getHeterogeneousChainId(),
                txData.getOriginalTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);

        if (null != info) {
            if (info.isDepositIIMainAndToken()) {
                // Verify simultaneous rechargetokenandmain
                CoinTo tokenAmountTo, mainAmountTo;
                HeterogeneousAssetInfo tokenInfo, mainInfo;
                CoinTo coin0 = listCoinTo.get(0);
                HeterogeneousAssetInfo info0 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coin0.getAssetsChainId(), coin0.getAssetsId());
                CoinTo coin1 = listCoinTo.get(1);
                HeterogeneousAssetInfo info1 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coin1.getAssetsChainId(), coin1.getAssetsId());
                // Both assets cannot be primary assets, assetId:1 Main assets
                if (info0.getAssetId() == 1 && info1.getAssetId() == 1) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                if (info0.getAssetId() == 1) {
                    mainAmountTo = coin0;
                    mainInfo = info0;
                    tokenAmountTo = coin1;
                    tokenInfo = info1;
                } else if (info1.getAssetId() == 1){
                    mainAmountTo = coin1;
                    mainInfo = info1;
                    tokenAmountTo = coin0;
                    tokenInfo = info0;
                } else {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                this.heterogeneousRechargeValidForCrossOutII(info, tokenAmountTo, tokenInfo, mainAmountTo, mainInfo);
            } else {
                // Verify only rechargetoken perhaps Recharge only main
                if (listCoinTo.size() > 1) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
                }
                // Through in chain assetsid Obtain heterogeneous chain information
                CoinTo coinTo = listCoinTo.get(0);
                HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                heterogeneousRechargeValid(info, coinTo, heterogeneousAssetInfo);
            }
        } else {
            // Check proposals (OriginalTxHash Not a heterogeneous chain transactionhash, Or it could be a proposalhash)
            ProposalPO proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
            if (null == proposalPO) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            proposalRechargeValidProtocol16(chain, proposalPO, listCoinTo);
        }

    }

    public void validateTxOfBtcSys(Chain chain, Transaction tx) throws NulsException {
        // Recharge verification
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() > 3) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
        if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                txData.getHeterogeneousChainId(),
                txData.getOriginalTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);

        if (null != info) {
            BtcTxInfo txInfo = (BtcTxInfo) info;
            if (StringUtils.isNotBlank(txInfo.getNerveFeeTo())) {
                if (listCoinTo.size() != 2) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
                }
                CoinTo coin0 = listCoinTo.get(0);
                CoinTo coin1 = listCoinTo.get(1);
                if (txInfo.getValue().add(txInfo.getFee()).compareTo(coin0.getAmount().add(coin1.getAmount())) != 0) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
                }
            } else {
                if (listCoinTo.size() > 1) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
                }
                CoinTo coinTo = listCoinTo.get(0);
                if (txInfo.getValue().add(txInfo.getFee()).compareTo(coinTo.getAmount()) != 0) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
                }
            }
        } else {
            // Check proposals (OriginalTxHash Not a heterogeneous chain transactionhash, Or it could be a proposalhash)
            ProposalPO proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
            if (null == proposalPO) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            proposalRechargeValidOfBtcSys(chain, proposalPO, listCoinTo);
        }

    }

    /**
     * Verify heterogeneous chain recharge transactions
     *
     * @param info
     * @param coinTo
     * @throws NulsException
     */
    private void heterogeneousRechargeValid(HeterogeneousTransactionInfo info, CoinTo coinTo, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // Recharge asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger infoValue = info.getValue();
        if (converterCoreApi.isProtocol22()) {
            infoValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(heterogeneousAssetInfo, info.getValue());
        }
        if (infoValue.compareTo(coinTo.getAmount()) != 0) {
            // Recharge amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * Verify heterogeneous chain recharge transactions - CrossOutII
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValidForCrossOutII(HeterogeneousTransactionInfo info, CoinTo tokenAmountTo, HeterogeneousAssetInfo tokenInfo, CoinTo mainAmountTo, HeterogeneousAssetInfo mainInfo) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // RechargetokenAsset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(tokenAmountTo.getAmount()) != 0) {
            // RechargetokenAmount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // Recharge main asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger depositIIMainAssetValue = info.getDepositIIMainAssetValue();
        if (converterCoreApi.isProtocol22()) {
            depositIIMainAssetValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(mainInfo, depositIIMainAssetValue);
        }
        if (depositIIMainAssetValue.compareTo(mainAmountTo.getAmount()) != 0) {
            // Recharge main amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(tokenAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(mainAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    private void heterogeneousRechargeValidForOneClickCrossChain(Chain chain, HeterogeneousTransactionInfo info, CoinTo tokenAmountTo, HeterogeneousAssetInfo tokenInfo, CoinTo mainAmountTo, HeterogeneousAssetInfo mainInfo, CoinTo coinTipping) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // RechargetokenAsset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger tipping = BigInteger.ZERO;
        if (coinTipping != null) {
            tipping = coinTipping.getAmount();
        }
        if (info.getValue().compareTo(tokenAmountTo.getAmount().add(tipping)) != 0) {
            // RechargetokenAmount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }

        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // Recharge main asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger depositIIMainAssetValue = info.getDepositIIMainAssetValue();
        if (converterCoreApi.isProtocol22()) {
            depositIIMainAssetValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(mainInfo, depositIIMainAssetValue);
        }
        if (depositIIMainAssetValue.compareTo(mainAmountTo.getAmount()) != 0) {
            // Recharge main amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(tokenAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * Verify proposal transfer to another address
     *
     * @param chain
     * @param proposalPO
     * @param coinTo
     * @throws NulsException
     */
    private void proposalRechargeValid(Chain chain, ProposalPO proposalPO, CoinTo coinTo, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (ProposalTypeEnum.TRANSFER.value() != proposalPO.getType()) {
            // Proposal type error
            chain.getLogger().error("Proposal type is not transfer");
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (proposalPO.getFavorNumber() < byzantineCount) {
            // The proposal was not voted through
            chain.getLogger().error("Proposal type was rejected");
            throw new NulsException(ConverterErrorCode.PROPOSAL_REJECTED);
        }

        // Obtain heterogeneous chain recharge transactions in the proposal
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                proposalPO.getHeterogeneousChainId(),
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }

        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // Recharge asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
            // Recharge amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!Arrays.equals(proposalPO.getAddress(), coinTo.getAddress())) {
            // The payment address for the recharge transaction is not the proposed payment address
            chain.getLogger().error("The payment address for the recharge transaction is not the proposed payment address");
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    private void proposalRechargeValidOfBtcSys(Chain chain, ProposalPO proposalPO, List<CoinTo> listCoinTo) throws NulsException {
        if (ProposalTypeEnum.TRANSFER.value() != proposalPO.getType()) {
            // Proposal type error
            chain.getLogger().error("Proposal type is not transfer");
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (proposalPO.getFavorNumber() < byzantineCount) {
            // The proposal was not voted through
            chain.getLogger().error("Proposal type was rejected");
            throw new NulsException(ConverterErrorCode.PROPOSAL_REJECTED);
        }

        // Obtain heterogeneous chain recharge transactions in the proposal
        int htgChainId = proposalPO.getHeterogeneousChainId();
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        HeterogeneousTransactionInfo info;
        try {
            info = docking.getUnverifiedDepositTransaction(proposalPO.getHeterogeneousTxHash());
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
        }
        if (null == info) {
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        BtcTxInfo txInfo = (BtcTxInfo) info;
        if (listCoinTo.size() > 1) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        CoinTo coinTo = listCoinTo.get(0);
        if (txInfo.getValue().add(txInfo.getFee()).compareTo(coinTo.getAmount()) != 0) {
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
    }

    private void proposalRechargeValidProtocol16(Chain chain, ProposalPO proposalPO, List<CoinTo> listCoinTo) throws NulsException {
        if (ProposalTypeEnum.TRANSFER.value() != proposalPO.getType()) {
            // Proposal type error
            chain.getLogger().error("Proposal type is not transfer");
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (proposalPO.getFavorNumber() < byzantineCount) {
            // The proposal was not voted through
            chain.getLogger().error("Proposal type was rejected");
            throw new NulsException(ConverterErrorCode.PROPOSAL_REJECTED);
        }

        // Obtain heterogeneous chain recharge transactions in the proposal
        int htgChainId = proposalPO.getHeterogeneousChainId();
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        if (info.isDepositIIMainAndToken()) {
            if (listCoinTo.size() != 2) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            // Verify simultaneous rechargetokenandmain
            CoinTo tokenAmountTo, mainAmountTo;
            HeterogeneousAssetInfo tokenInfo, mainInfo;
            CoinTo coin0 = listCoinTo.get(0);
            HeterogeneousAssetInfo info0 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coin0.getAssetsChainId(), coin0.getAssetsId());
            CoinTo coin1 = listCoinTo.get(1);
            HeterogeneousAssetInfo info1 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coin1.getAssetsChainId(), coin1.getAssetsId());
            // Must betokenandmainasset
            if (info0.getAssetId() == 1 && info1.getAssetId() == 1) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info0.getAssetId() == 1) {
                mainAmountTo = coin0;
                mainInfo = info0;
                tokenAmountTo = coin1;
                tokenInfo = info1;
            } else if (info1.getAssetId() == 1){
                mainAmountTo = coin1;
                mainInfo = info1;
                tokenAmountTo = coin0;
                tokenInfo = info0;
            } else {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }

            if (info.getAssetId() != tokenInfo.getAssetId()) {
                // RechargetokenAsset error
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getValue().compareTo(tokenAmountTo.getAmount()) != 0) {
                // RechargetokenAmount error
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
                // Recharge main asset error
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getDepositIIMainAssetValue().compareTo(mainAmountTo.getAmount()) != 0) {
                // Recharge main amount error
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), tokenAmountTo.getAddress())) {
                chain.getLogger().error("The payment address for the recharge transaction is not the proposed payment address");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), mainAmountTo.getAddress())) {
                chain.getLogger().error("The payment address for the recharge transaction is not the proposed payment address");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        } else {
            CoinTo coinTo = listCoinTo.get(0);
            HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
            if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
                // Recharge asset error
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
                // Recharge amount error
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), coinTo.getAddress())) {
                // The payment address for the recharge transaction is not the proposed payment address
                chain.getLogger().error("The payment address for the recharge transaction is not the proposed payment address");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        }
    }

    public void validateOneClickCrossChain(Chain chain, Transaction tx) throws NulsException {
        // Verify one click cross chain transactions, Verify withdrawal amount, Handling fee amount, tippingdata, Target chain information
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || (listCoinTo.size() != 2 && listCoinTo.size() != 3)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
        HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
        String originalTxHash = heterogeneousHash.getHeterogeneousHash();
        int htgChainId = heterogeneousHash.getHeterogeneousChainId();

        if(null != rechargeStorageService.find(chain, originalTxHash)){
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), originalTxHash);
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                originalTxHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (info == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String extend = info.getDepositIIExtend();
        HeterogeneousOneClickCrossChainData data = docking.parseOneClickCrossChainData(extend);
        if (data == null) {
            chain.getLogger().error("[{}][One click cross chain]Transaction verification failed, originalTxHash: {}, extend: {}", htgChainId, extend);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        BigInteger tipping = data.getTipping();
        boolean hasTipping = tipping.compareTo(BigInteger.ZERO) > 0;
        if (hasTipping) {
            if (listCoinTo.size() != 3) {
                throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
            }
        } else {
            if (listCoinTo.size() != 2) {
                throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
            }
        }
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        CoinTo coinWithdrawal = null, coinFee = null, coinTipping = null;
        for (CoinTo to : listCoinTo) {
            if (Arrays.equals(withdrawalBlackhole, to.getAddress())) {
                coinWithdrawal = to;
            } else if (Arrays.equals(withdrawalFeeAddress, to.getAddress())) {
                coinFee = to;
            } else {
                coinTipping = to;
            }
        }
        if (coinWithdrawal == null || coinFee == null || (hasTipping && coinTipping == null)) {
            chain.getLogger().error("[{}][One click cross chain]Transaction verification failed, CoinDatadeletion, originalTxHash: {}", htgChainId, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        String desToAddress = data.getDesToAddress();
        desToAddress = ConverterUtil.addressToLowerCase(desToAddress);
        int desChainId = txData.getDesChainId();
        if (txData.getDesChainId() != data.getDesChainId() ||
                !txData.getDesToAddress().equals(desToAddress) ||
                coinFee.getAmount().compareTo(data.getFeeAmount()) != 0) {
            chain.getLogger().error("[{}][One click cross chain]Transaction verification failed, Target chain information error, feeAmount: {}, desId: {}, desAddress: {}, originalTxHash: {}", htgChainId, coinFee.getAmount(), txData.getDesChainId(), txData.getDesToAddress(), originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        BigInteger tippingFromCoin = BigInteger.ZERO;
        if (coinTipping != null) {
            tippingFromCoin = coinTipping.getAmount();
            if (coinWithdrawal.getAssetsChainId() != coinTipping.getAssetsChainId() || coinWithdrawal.getAssetsId() != coinTipping.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
        }
        // NerveOne click cross chain transaction requires one asset to be the main asset, which serves as the transaction fee for crossing to the target chain
        boolean hasToken = info.isIfContractAsset();
        if (hasToken) {
            // The source chain transaction must have been rechargedtoken
            HeterogeneousAssetInfo infoWithdrawal = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinWithdrawal.getAssetsChainId(), coinWithdrawal.getAssetsId());
            HeterogeneousAssetInfo infoFee = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinFee.getAssetsChainId(), coinFee.getAssetsId());
            // Cross chain assets must betokenasset, The handling fee asset must be the main asset, assetId:1 Main assets
            if (infoWithdrawal.getAssetId() == 1 || infoFee.getAssetId() != 1) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.isDepositIIMainAndToken()) {
                // Verify simultaneous rechargetokenandmain
                this.heterogeneousRechargeValidForOneClickCrossChain(chain, info, coinWithdrawal, infoWithdrawal, coinFee, infoFee, coinTipping);
            } else {
                // Only rechargedtoken
                if (info.getAssetId() != infoWithdrawal.getAssetId()) {
                    // Recharge asset error
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                if (info.getValue().compareTo(coinWithdrawal.getAmount().add(tippingFromCoin)) != 0) {
                    // Recharge amount error
                    throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
                }
                if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinWithdrawal.getAddress()))) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
                }
                if (coinFee.getAmount().compareTo(BigInteger.ZERO) != 0) {
                    // Incorrect handling fee amount
                    throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_FEE_ERROR);
                }
            }
        } else {
            // The source chain only recharged the main asset
            if (coinWithdrawal.getAssetsChainId() != coinFee.getAssetsChainId() || coinWithdrawal.getAssetsId() != coinFee.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            HeterogeneousAssetInfo infoMain = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinFee.getAssetsChainId(), coinFee.getAssetsId());
            if (info.getAssetId() != infoMain.getAssetId()) {
                // Recharge asset error
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            BigInteger infoValue = info.getValue();
            if (converterCoreApi.isProtocol22()) {
                infoValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(infoMain, infoValue);
            }
            if (infoValue.compareTo(coinWithdrawal.getAmount().add(coinFee.getAmount()).add(tippingFromCoin)) != 0) {
                // Recharge amount error
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinWithdrawal.getAddress()))) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        }
        // Check target chain data
        IHeterogeneousChainDocking desDocking = heterogeneousDockingManager.getHeterogeneousDocking(desChainId);
        // Check if the recharged assets can cross the chain to the target chain
        HeterogeneousAssetInfo desChainAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(desChainId, coinWithdrawal.getAssetsChainId(), coinWithdrawal.getAssetsId());
        if (desChainAssetInfo == null) {
            chain.getLogger().error("[{}]OneClickCrossChain des error, desChainId:{}, desToAddress:{}, originalTxHash: {}", htgChainId, desChainId, desToAddress, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
        // Check if the target chain address is legal
        boolean validateAddress = desDocking.validateAddress(desToAddress);
        if (!validateAddress) {
            chain.getLogger().error("[{}]OneClickCrossChain desToAddress error, desChainId:{}, desToAddress:{}, originalTxHash: {}", htgChainId, desChainId, desToAddress, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
    }

    public void validateAddFeeCrossChain(Chain chain, Transaction tx) throws NulsException {
        // Business verification of cross chain additional transaction fees
        String hash = tx.getHash().toHex();
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() != 1) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        WithdrawalAddFeeByCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAddFeeByCrossChainTxData.class);
        HeterogeneousHash heterogeneousHash = txData.getHtgTxHash();
        String originalTxHash = heterogeneousHash.getHeterogeneousHash();
        int htgChainId = heterogeneousHash.getHeterogeneousChainId();

        if(null != rechargeStorageService.find(chain, originalTxHash)){
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    hash, originalTxHash);
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                originalTxHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (info == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String extend = info.getDepositIIExtend();
        HeterogeneousAddFeeCrossChainData data = docking.parseAddFeeCrossChainData(extend);
        if (data == null) {
            chain.getLogger().error("[{}][Cross chain additional handling fees]Transaction verification failed, originalTxHash: {}, extend: {}", htgChainId, extend);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        CoinTo coinTo = listCoinTo.get(0);
        if (!Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
            chain.getLogger().error("[{}][Cross chain additional handling fees]Transaction verification failed, Collection address for handling fees and additional transactionstoAddress mismatch, toAddress:{}, withdrawalFeeAddress:{} ",
                    htgChainId,
                    AddressTool.getStringAddressByBytes(coinTo.getAddress()),
                    AddressTool.getStringAddressByBytes(withdrawalFeeAddress));
            throw new NulsException(ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH);
        }
        if (info.isIfContractAsset()) {
            throw new NulsException(ConverterErrorCode.ADD_FEE_CROSS_CHAIN_COIN_ERROR);
        }
        HeterogeneousAssetInfo infoMain = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
        if (info.getAssetId() != infoMain.getAssetId()) {
            // Recharge asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
            // Recharge amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
        // Verify withdrawal transactions
        String basicTxHash = txData.getNerveTxHash();
        if (StringUtils.isBlank(basicTxHash)) {
            chain.getLogger().error("Original withdrawal transaction with additional transaction feeshashNot present! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
            // Withdrawal transactionshash
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
        if (null == basicTx) {
            chain.getLogger().error("The original transaction does not exist , hash:{}", basicTxHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN) {
            // Not a withdrawal transaction
            chain.getLogger().error("txdataThe corresponding transaction is not a withdrawal transaction/One click cross chain , hash:{}", basicTxHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out, No additional handling fees allowed
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // Handling fee assets for withdrawal transactionsID
            CoinData basicCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            Coin feeCoin = null;
            for (CoinTo basicCoinTo : basicCoinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, basicCoinTo.getAddress())) {
                    feeCoin = basicCoinTo;
                    break;
                }
            }
            feeAssetChainId = feeCoin.getAssetsChainId();
            feeAssetId = feeCoin.getAssetsId();
        }

        // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
        if (coinTo.getAssetsChainId() != feeAssetChainId || coinTo.getAssetsId() != feeAssetId) {
            chain.getLogger().error("Additional transaction assets must be consistent with the transaction fee assets for withdrawal transactions, AssetsChainId:{}, AssetsId:{}",
                    coinTo.getAssetsChainId(), coinTo.getAssetsId());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
        }
    }
}
