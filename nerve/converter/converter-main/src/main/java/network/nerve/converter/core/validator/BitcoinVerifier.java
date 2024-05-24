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

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.utils.ConverterDBUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.List;

/**
 * @author: Loki
 * @date: 2020/5/21
 */
@Component
public class BitcoinVerifier {

    @Autowired
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    public void validateWithdrawlUTXO(Chain chain, Transaction tx) throws NulsException {
        WithdrawalUTXOTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalUTXOTxData.class);
        String nerveTxHash = txData.getNerveTxHash();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHtgChainId());
        List<UTXOData> utxoDataList = txData.getUtxoDataList();
        if (null != asyncProcessedTxStorageService.getComponentCall(chain, nerveTxHash)) {
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }

        boolean rebuild = false;
        WithdrawalUTXOTxData oldUtxoTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
        if (oldUtxoTxData != null) {
            rebuild = oldUtxoTxData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD;
        }
        if (HtgUtil.isEmptyList(utxoDataList)) {
            return;
        }
        for (UTXOData utxo : utxoDataList) {
            String nerveHash = docking.getBitCoinApi().getNerveHashByLockedUTXO(utxo.getTxid(), utxo.getVout());
            boolean lockedUTXO = nerveHash != null;
            if (lockedUTXO) {
                if (!rebuild || !nerveHash.equals(nerveTxHash)) {
                    throw new NulsException(ConverterErrorCode.WITHDRAWAL_UTXO_LOCKED);
                }
            }
        }
    }

    public void validateWithdrawFee(Chain chain, Transaction tx) throws NulsException {
        WithdrawalFeeLog txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalFeeLog.class);
        int htgChainId = txData.getHtgChainId();
        String htgTxHash = txData.getHtgTxHash();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        boolean hasRecordFeePayment = docking.getBitCoinApi().hasRecordFeePayment(htgTxHash);
        if (hasRecordFeePayment) {
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
    }
}
