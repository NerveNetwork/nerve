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
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgPendingTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgStorageHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class FchAnalysisTxHelper implements BeanInitial {

    private HtgUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    private FchWalletApi htgWalletApi;
    private HtgListener htgListener;
    private HtgStorageHelper htgStorageHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private FchContext htgContext;
    private FchParseTxHelper fchParseTxHelper;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void analysisTx(TxInfo txInfo, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        String htgTxHash = txInfo.getId();
        BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) fchParseTxHelper.parseDepositTransaction(txInfo, true);
        if (po == null) {
            return;
        }
        isDepositTx = true;
        htgContext.logger().info("Listening to {} Network basedMainAssetRecharge transaction[{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                htgContext.getConfig().getSymbol(), htgTxHash,
                po.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());

        // add by pierre at 2022/6/29 Add recharge pause mechanism
        if (htgContext.getConverterCoreApi().isPauseInHeterogeneousAsset(htgContext.HTG_CHAIN_ID(), po.getAssetId())) {
            htgContext.logger().warn("[Recharge pause] transaction[{}]", htgTxHash);
            return;
        }
        // Check if it has been affectedNerveNetwork confirmation, the cause is the current node parsingethThe transaction is slower than other nodes, and the current node only resolves this transaction after other nodes confirm it
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(htgTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{}transaction[{}]Has been[Nervenetwork]Confirm, no further processing", htgContext.getConfig().getSymbol(), htgTxHash);
                return;
            }
        }
        // Confirmation required for deposit of recharge transactionsnIn the pending confirmation transaction queue of blocks
        // Save analyzed recharge transactions
        htgStorageHelper.saveTxInfo(po);
        htUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
    }



}
