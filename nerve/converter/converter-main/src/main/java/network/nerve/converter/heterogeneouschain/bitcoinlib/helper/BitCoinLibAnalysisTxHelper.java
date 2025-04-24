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
package network.nerve.converter.heterogeneouschain.bitcoinlib.helper;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.AnalysisTxInfo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgStorageHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public abstract class BitCoinLibAnalysisTxHelper implements BeanInitial {

    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected BitCoinLibWalletApi htgWalletApi;
    protected IBitCoinLibParseTxHelper parseTxHelper;
    protected HtgListener htgListener;
    protected HtgStorageHelper htgStorageHelper;
    protected HtgContext htgContext;

    protected NulsLogger logger() {
        return htgContext.logger();
    }

    protected abstract AnalysisTxInfo analysisTxTypeInfo(Object _txInfo, long txTime, String blockHash);
    protected abstract long calcTxFee(Object _txInfo, BitCoinLibWalletApi htgWalletApi);
    protected abstract String fetchTxHash(Object _txInfo);

    // In tbc network transactions, if the utxo of vin is a multi-signature address, then get the vout corresponding to its previous transaction to get its address and value
    protected List fetchVinInfoOfMultiSign(List tx) throws Exception {
        return Collections.EMPTY_LIST;
    }

    public void analysisTx(Object txInfo, long txTime, long blockHeight, String blockHash) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String htgTxHash = this.fetchTxHash(txInfo);
        AnalysisTxInfo analysisTxInfo = this.analysisTxTypeInfo(txInfo, txTime, blockHash);
        if (analysisTxInfo == null) {
            return;
        }
        HeterogeneousChainTxType txType = analysisTxInfo.getTxType();
        if (txType == null) {
            return;
        }
        BtcUnconfirmedTxPo po = null;
        if (txType == HeterogeneousChainTxType.DEPOSIT) {
            po = (BtcUnconfirmedTxPo) parseTxHelper.parseDepositTransaction(txInfo, blockHeight, true);
            if (po != null && po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                this.recordRechargeOfWithdrawalFee(po.getValue(), htgTxHash, txTime, blockHeight, blockHash);
            }
            isDepositTx = true;
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            this.recordUsedWithdrawalFee(txInfo, htgTxHash, txTime, blockHeight, blockHash);

            po = (BtcUnconfirmedTxPo) parseTxHelper.parseWithdrawalTransaction(txInfo, blockHeight, true);
            isBroadcastTx = true;
        }
        // the transaction is illegal
        if (po == null) {
            return;
        }

        if (isDepositTx) {
            // 增加tbc erc20的日志打印
            if (po.isDepositIIMainAndToken()) {
                htgContext.logger().info("Listening to {} Network based ERC20/{} Simultaneously Recharge transaction [{}], from: {}, to: {}, erc20Value: {}, nerveAddress: {}, contract: {}, decimals: {}, mainAssetValue: {}",
                        htgContext.getConfig().getSymbol(), htgContext.getConfig().getSymbol(), htgTxHash,
                        po.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals(), po.getDepositIIMainAssetValue());
            } else if (po.isIfContractAsset()) {
                htgContext.logger().info("Listening to {} Network based ERC20 Recharge transaction [{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                        htgContext.getConfig().getSymbol(), htgTxHash,
                        po.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
            } else {
                htgContext.logger().info("Listening to {} Network based MainAsset Recharge transaction [{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        htgContext.getConfig().getSymbol(), htgTxHash,
                        po.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
            }

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

    private void recordUsedWithdrawalFee(Object txInfo, String txHash, long txTime, long blockHeight, String blockHash) throws Exception {
        long fee = this.calcTxFee(txInfo, htgWalletApi);
        this.recordWithdrawalFeeCompleted(BigInteger.valueOf(fee), txHash, txTime, blockHeight, blockHash, HeterogeneousChainTxType.WITHDRAW_FEE_USED);
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
