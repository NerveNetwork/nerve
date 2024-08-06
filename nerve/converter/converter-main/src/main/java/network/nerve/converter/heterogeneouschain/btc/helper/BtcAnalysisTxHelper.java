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
package network.nerve.converter.heterogeneouschain.btc.helper;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.AnalysisTxInfo;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;

import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class BtcAnalysisTxHelper extends BitCoinLibAnalysisTxHelper {

    @Override
    protected AnalysisTxInfo analysisTxTypeInfo(Object _txInfo, long txTime, String blockHash) {
        RawTransaction txInfo = (RawTransaction) _txInfo;
        if (txInfo == null) {
            htgContext.logger().warn("Transaction does not exist");
            return null;
        }
        if (txInfo.getTime() == null || txInfo.getBlockTime() == null) {
            txInfo.setTime(txTime);
            txInfo.setBlockTime(txTime);
        }
        if (txInfo.getBlockHash() == null) {
            txInfo.setBlockHash(blockHash);
        }
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        if (HtgUtil.isEmptyList(txInfo.getVIn())) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        List<RawInput> inputList = txInfo.getVIn();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (RawInput input : inputList) {
                String inputAddress = BitCoinLibUtil.takeMultiSignAddressWithP2WSH(input, htgContext.getConverterCoreApi().isNerveMainnet());
                if (htgListener.isListeningAddress(inputAddress)) {
                    txType = HeterogeneousChainTxType.WITHDRAW;
                    break OUT;
                }
            }
            for (RawOutput output : outputList) {
                String outputAddress = output.getScriptPubKey().getAddress();
                if (htgListener.isListeningAddress(outputAddress)) {
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    break OUT;
                }
            }
        } while (false);
        return new AnalysisTxInfo(txType, _txInfo);
    }

    @Override
    protected long calcTxFee(Object _txInfo, BitCoinLibWalletApi htgWalletApi) {
        return BitCoinLibUtil.calcTxFee((RawTransaction) _txInfo, htgWalletApi);
    }

    @Override
    protected String fetchTxHash(Object _txInfo) {
        RawTransaction txInfo = (RawTransaction) _txInfo;
        return txInfo.getTxId();
    }
}
