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
package network.nerve.converter.heterogeneouschain.fch.helper;

import apipClass.TxInfo;
import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import fchClass.CashMark;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.btc.utils.BtcUtil;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgPendingTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgStorageHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;

import java.math.BigInteger;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class FchAnalysisTxHelper implements BeanInitial {

    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private FchWalletApi htgWalletApi;
    private HtgListener htgListener;
    private HtgStorageHelper htgStorageHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private FchContext htgContext;
    private FchParseTxHelper fchParseTxHelper;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void analysisTx(TxInfo txInfo, long txTime, long blockHeight, String blockHash) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String htgTxHash = txInfo.getId();
        if (txInfo.getBlockTime() <= 0) {
            txInfo.setBlockTime(txTime);
        }
        if (txInfo.getBlockId() == null) {
            txInfo.setBlockId(blockHash);
        }
        if (txInfo == null) {
            htgContext.logger().warn("Transaction does not exist");
            return;
        }
        if (HtgUtil.isEmptyList(txInfo.getIssuedCashes())) {
            return;
        }
        if (HtgUtil.isEmptyList(txInfo.getSpentCashes())) {
            return;
        }
        List<CashMark> outputList = txInfo.getIssuedCashes();
        List<CashMark> inputList = txInfo.getSpentCashes();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (CashMark input : inputList) {
                String inputAddress = input.getOwner();
                if (htgListener.isListeningAddress(inputAddress)) {
                    txType = HeterogeneousChainTxType.WITHDRAW;
                    break OUT;
                }
            }
            for (CashMark output : outputList) {
                String outputAddress = output.getOwner();
                if (htgListener.isListeningAddress(outputAddress)) {
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    break OUT;
                }
            }
        } while (false);
        if (txType == null) {
            return;
        }

        BtcUnconfirmedTxPo po = null;
        if (txType == HeterogeneousChainTxType.DEPOSIT) {
            po = (BtcUnconfirmedTxPo) fchParseTxHelper.parseDepositTransaction(txInfo, true);
            if (po != null && po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                this.recordRechargeOfWithdrawalFee(po.getValue(), htgTxHash, txTime, blockHeight, blockHash);
            }
            isDepositTx = true;
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            this.recordUsedWithdrawalFee(txInfo, txTime, blockHeight, blockHash);

            po = (BtcUnconfirmedTxPo) fchParseTxHelper.parseWithdrawalTransaction(txInfo, true);
            isBroadcastTx = true;
        }
        // the transaction is illegal
        if (po == null) {
            return;
        }

        if (isDepositTx) {
            htgContext.logger().info("Listening to {} Network based MainAsset Recharge transaction [{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), htgTxHash,
                    po.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
            // add by pierre at 2022/6/29 Add recharge pause mechanism
            if (htgContext.getConverterCoreApi().isPauseInHeterogeneousAsset(htgContext.HTG_CHAIN_ID(), po.getAssetId())) {
                htgContext.logger().warn("[Recharge pause] transaction [{}]", htgTxHash);
                return;
            }
        }
        if (isBroadcastTx) {
            htgContext.logger().info("Listening to {} Network based [{}] transaction [{}], nerveTxHash: {}", htgContext.getConfig().getSymbol(), txType, htgTxHash, po.getNerveTxHash());
        }

        // Check if it has been affectedNerveNetwork confirmation, the cause is the current node parsingethThe transaction is slower than other nodes, and the current node only resolves this transaction after other nodes confirm it
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{} transaction [{}] Has been [Nervenetwork] Confirm, no further processing", htgContext.getConfig().getSymbol(), htgTxHash);
                return;
            }
        }
        boolean isLocalSent = true;
        if (txPoFromDB == null) {
            isLocalSent = false;
        }
        if (isBroadcastTx) {
            po.setStatus(MultiSignatureStatus.COMPLETED);
            // Remove listening
            htgListener.removeListeningTx(htgTxHash);
        }
        // Confirmation required for deposit of recharge transactionsnIn the pending confirmation transaction queue of blocks
        // Save analyzed recharge transactions
        htgStorageHelper.saveTxInfo(po);
        htgUnconfirmedTxStorageService.save(po);
        if (!isLocalSent) {
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        }
    }

    private void recordRechargeOfWithdrawalFee(BigInteger value, String txHash, long txTime, long blockHeight, String blockHash) throws Exception {
        this.recordWithdrawalFeeCompleted(value, txHash, txTime, blockHeight, blockHash, HeterogeneousChainTxType.WITHDRAW_FEE_RECHARGE);
    }

    private void recordUsedWithdrawalFee(TxInfo txInfo, long txTime, long blockHeight, String blockHash) throws Exception {
        long fee = txInfo.getFee();
        this.recordWithdrawalFeeCompleted(BigInteger.valueOf(fee), txInfo.getId(), txTime, blockHeight, blockHash, HeterogeneousChainTxType.WITHDRAW_FEE_USED);
    }

    private void recordWithdrawalFeeCompleted(BigInteger value, String txHash, long txTime, long blockHeight, String blockHash, HeterogeneousChainTxType txType) throws Exception {
        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        po.setValue(value);
        po.setTxTime(txTime);
        po.setBlockHash(blockHash);
        po.setBlockHeight(blockHeight);
        po.setTxHash(ConverterConstant.BTC_WITHDRAW_FEE_TX_HASH_PREFIX + txHash);//When the db is saved, the key is used by other businesses, the PO data is overwritten, and the key needs to be redefined.
        po.setTxType(txType);
        po.setStatus(MultiSignatureStatus.COMPLETED);
        htgStorageHelper.saveTxInfo(po);
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
    }

}
