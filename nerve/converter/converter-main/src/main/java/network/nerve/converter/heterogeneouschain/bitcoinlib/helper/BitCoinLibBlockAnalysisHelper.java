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

import com.neemre.btcdcli4j.core.domain.BlockInfo;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;

import java.util.List;

/**
 * analysisHTGBlock, listen to specified addresses and transactions, and call backNervecore
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
public class BitCoinLibBlockAnalysisHelper implements BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgContext htgContext;

    public void analysisEthBlock(List txList, long blockHeight, long txTime, String blockHash, String preBlockHash, BitCoinLibAnalysisTxHelper analysisTx) throws Exception {
        if (txList != null && !txList.isEmpty()) {
            // In tbc network transactions, if the utxo of vin is a multi-signature address, then get the vout corresponding to its previous transaction to get its address and value
            txList = analysisTx.fetchVinInfoOfMultiSign(txList);
            for (int i = 0, len = txList.size(); i < len; i++) {
                Object tx = txList.get(i);
                String txHash = analysisTx.fetchTxHash(tx);
                try {
                    analysisTx.analysisTx(tx, txTime, blockHeight, blockHash);
                } catch (Exception e) {
                    htgContext.logger().error(String.format("[%s] Network transaction parsing failed: %s", htgContext.getConfig().getSymbol(), txHash), e);
                }
            }
        }
        // Save local blocks
        HtgSimpleBlockHeader simpleBlockHeader = new HtgSimpleBlockHeader();
        simpleBlockHeader.setHash(blockHash);
        simpleBlockHeader.setPreHash(preBlockHash);
        simpleBlockHeader.setHeight(blockHeight);
        simpleBlockHeader.setCreateTime(System.currentTimeMillis());
        htgLocalBlockHelper.saveLocalBlockHeader(simpleBlockHeader);
        // Keep only the last thirty blocks
        htgLocalBlockHelper.deleteByHeight(blockHeight - 30);
        htgContext.logger().info("synchronization {} height [{}] complete", htgContext.getConfig().getSymbol(), blockHeight);
    }



}
