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

import apipClass.BlockInfo;
import apipClass.TxInfo;
import fchClass.TxMark;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * analysisHTGBlock, listen to specified addresses and transactions, and call backNervecore
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
public class FchBlockAnalysisHelper implements BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private FchContext htgContext;
    private FchWalletApi walletApi;

    /**
     * analysisHTGblock
     */
    public void analysisEthBlock(BlockInfo block, FchAnalysisTxHelper analysisTx) throws Exception {
        ArrayList<TxMark> txList = block.getTxList();
        long blockHeight = block.getHeight();
        if (txList != null && txList.size() > 0) {
            List<TxInfo> txInfoList = walletApi.getTxInfoList(txList.stream().map(tx -> tx.getTxId()).collect(Collectors.toList()));
            long txTime = block.getTime();
            for (int i = 0, len = txInfoList.size(); i < len; i++) {
                TxInfo tx = txInfoList.get(i);
                try {
                    analysisTx.analysisTx(tx, txTime, blockHeight);
                } catch (Exception e) {
                    htgContext.logger().error(String.format("[%s] Network transaction parsing failed: %s", htgContext.getConfig().getSymbol(), tx.getId()), e);
                }
            }
        }
        // Save local blocks
        HtgSimpleBlockHeader simpleBlockHeader = new HtgSimpleBlockHeader();
        simpleBlockHeader.setHash(block.getBlockId());
        simpleBlockHeader.setPreHash(block.getPreId());
        simpleBlockHeader.setHeight(block.getHeight());
        simpleBlockHeader.setCreateTime(System.currentTimeMillis());
        htgLocalBlockHelper.saveLocalBlockHeader(simpleBlockHeader);
        // Keep only the last thirty blocks
        htgLocalBlockHelper.deleteByHeight(blockHeight - 30);
        htgContext.logger().info("synchronization {} height [{}] complete", htgContext.getConfig().getSymbol(), blockHeight);
    }



}
