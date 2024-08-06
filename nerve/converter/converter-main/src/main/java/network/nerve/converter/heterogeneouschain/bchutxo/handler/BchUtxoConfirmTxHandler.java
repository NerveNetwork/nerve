/**
 * MIT License
 * <p>
 * Copyrightg (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rightgs
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyrightg notice and this permission notice shall be included in all
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
package network.nerve.converter.heterogeneouschain.bchutxo.handler;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class BchUtxoConfirmTxHandler extends BitCoinLibConfirmTxHandler {

    private BitCoinLibWalletApi htgWalletApi;
    private HtgContext htgContext;

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return htgWalletApi;
    }

    @Override
    protected HtgContext context() {
        return htgContext;
    }
    /**
     * Verify recharge transactions
     */
    protected boolean validateDepositTxConfirmedInBtcNet(String htgTxHash, boolean ifContractAsset) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean validateTx = true;
        do {
            RawTransaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
            if (htgTx.getConfirmations() == null || htgTx.getConfirmations().intValue() == 0) {
                validateTx = false;
                logger().error("[{}] Verify transaction [{}] Failed, tx not confirmed yet", symbol, htgTxHash);
                break;
            }
        } while (false);
        return validateTx;
    }

    protected BroadcastTxValidateStatus validateBroadcastTxConfirmedInBtcNet(HtgUnconfirmedTxPo po) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        BroadcastTxValidateStatus status;
        String htgTxHash = po.getTxHash();
        do {
            RawTransaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
            if (htgTx.getConfirmations() == null || htgTx.getConfirmations().intValue() == 0) {
                po.setSkipTimes(3);
                status = BroadcastTxValidateStatus.RE_VALIDATE;
                logger().error("[{}] Verify transaction [{}] Failed, tx not confirmed yet", symbol, htgTxHash);
                break;
            }
            status = BroadcastTxValidateStatus.SUCCESS;
        } while (false);
        return status;
    }
}
