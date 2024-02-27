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
package network.nerve.converter.heterogeneouschain.lib.handler;

import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.core.methods.response.EthBlock;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgBlockHandler implements Runnable, BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgWalletApi htgWalletApi;
    private ConverterConfig converterConfig;
    private HtgBlockAnalysisHelper htgBlockAnalysisHelper;
    private HtgCommonHelper htgCommonHelper;
    private HtgAnalysisTxHelper htgAnalysisTxHelper;
    private HtgCallBackManager htgCallBackManager;

    private boolean firstSync = true;
    private boolean clearDB = false;

    private HtgContext htgContext;

    public void run() {
        try {
            if (!htgContext.getConverterCoreApi().isRunning()) {
                if (LoggerUtil.LOG.isDebugEnabled()) {
                    LoggerUtil.LOG.debug("[{}]Ignoring synchronous block mode", htgContext.getConfig().getSymbol());
                }
                return;
            }
            if (!htgContext.getConverterCoreApi().checkNetworkRunning(htgContext.HTG_CHAIN_ID())) {
                htgContext.logger().info("Test network[{}]Run Pause, chainId: {}", htgContext.getConfig().getSymbol(), htgContext.HTG_CHAIN_ID());
                return;
            }
            if (!htgContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
                try {
                    if(!clearDB) {
                        htgLocalBlockHelper.deleteAllLocalBlockHeader();
                        clearDB = true;
                    }
                } catch (Exception e) {
                    htgContext.logger().error(e);
                }
                if (LoggerUtil.LOG.isDebugEnabled()) {
                    LoggerUtil.LOG.debug("[{}]Non virtual bank member, skip this task", htgContext.getConfig().getSymbol());
                }
                return;
            }
            clearDB = false;
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("[{}Block parsing task] - every other{}Execute once per second.", htgContext.getConfig().getSymbol(), htgContext.getConfig().getBlockQueuePeriod());
            }
            try {
                htgCommonHelper.clearHash();
            } catch (Exception e) {
                htgContext.logger().error("Clearing recharge transactionshashFailed to revalidate collection", e);
            }
            htgWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            if (htgWalletApi.isReSyncBlock()) {
                htgContext.logger().info("[{}]Delete all local blocks from the network and wait for the next round of execution", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                htgWalletApi.setReSyncBlock(false);
                return;
            }
            // currentHTGThe latest blocks in the network
            long blockHeightFromEth = htgWalletApi.getBlockHeight();
            // Latest local blocks
            HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // When starting a node, the local block is empty and will be removed from theHTGStarting synchronization at the latest height of the network
                EthBlock.Block block = htgWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain {} Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            long difference = blockHeightFromEth - localBlockHeight;
            // When starting a node, the latest local altitude matchesHTGWhen the height of a network block differs by two or more blocks, it will be removed from theHTGNetwork height begins to synchronize
            if (firstSync && Math.abs(difference) >= 2) {
                EthBlock.Block block = htgWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain {} Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // Verify if the latest block is correct
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                htgContext.logger().error("obtain{}Block failure", htgContext.getConfig().getSymbol());
                return;
            } else if (resultCode == 1) {
                htgWalletApi.clearCache();
                htgContext.logger().error("{}Block fork", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * Synchronize and parse data
                 */
                try {
                    EthBlock.Block block = htgWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                        break;
                    }
                    htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
                } catch (Exception e) {
                    htgContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Throwable e) {
            htgContext.logger().error(String.format("synchronization%sBlock failure", htgContext.getConfig().getSymbol()), e);
        }
    }

    private int checkNewestBlock(HtgSimpleBlockHeader max) throws Exception {
        EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(max.getHeight().longValue());
        if (block == null) {
            return 0;
        } else if (block.getHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
