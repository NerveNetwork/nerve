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
package network.nerve.converter.heterogeneouschain.lib.helper;

import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.interfaces.IHtgAnalysisTx;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.util.List;

/**
 * analysisHTGBlock, listen to specified addresses and transactions, and call backNervecore
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgBlockAnalysisHelper implements BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgContext htgContext;

    //public HtgBlockAnalysisHelper(BeanMap beanMap) {
    //    this.htgLocalBlockHelper = (HtgLocalBlockHelper) beanMap.get("htgLocalBlockHelper");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}
    /**
     * analysisHTGblock
     */
    public void analysisEthBlock(EthBlock.Block block, IHtgAnalysisTx ethAnalysisTx) throws Exception {
        List<EthBlock.TransactionResult> ethTransactionResults = block.getTransactions();
        long blockHeight = block.getNumber().longValue();
        int size;
        if (ethTransactionResults != null && (size = ethTransactionResults.size()) > 0) {
            long txTime = block.getTimestamp().longValue();
            for (int i = 0; i < size; i++) {
                org.web3j.protocol.core.methods.response.Transaction tx = (org.web3j.protocol.core.methods.response.Transaction) ethTransactionResults.get(i).get();
                try {
                    ethAnalysisTx.analysisTx(tx, txTime, blockHeight);
                } catch (Exception e) {
                    htgContext.logger().error(String.format("[%s]Network transaction parsing failed: %s", htgContext.getConfig().getSymbol(), tx.getHash()), e);
                }
            }
        }
        // Save local blocks
        HtgSimpleBlockHeader simpleBlockHeader = new HtgSimpleBlockHeader();
        simpleBlockHeader.setHash(block.getHash());
        simpleBlockHeader.setPreHash(block.getParentHash());
        simpleBlockHeader.setHeight(block.getNumber().longValue());
        simpleBlockHeader.setCreateTime(System.currentTimeMillis());
        htgLocalBlockHelper.saveLocalBlockHeader(simpleBlockHeader);
        // Keep only the last thirty blocks
        htgLocalBlockHelper.deleteByHeight(blockHeight - 30);

        if (blockHeight % 50 == 0) {
            htgContext.logger().info("synchronization {} height [{}] complete", htgContext.getConfig().getSymbol(), blockHeight);
        } else {
            htgContext.logger().debug("synchronization {} height [{}] complete", htgContext.getConfig().getSymbol(), blockHeight);
        }
    }



}
