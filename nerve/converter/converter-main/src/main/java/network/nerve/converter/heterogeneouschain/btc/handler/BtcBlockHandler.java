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
package network.nerve.converter.heterogeneouschain.btc.handler;

import com.neemre.btcdcli4j.core.domain.BlockHeader;
import com.neemre.btcdcli4j.core.domain.BlockInfo;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.btc.core.BtcWalletApi;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.utils.LoggerUtil;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class BtcBlockHandler implements Runnable, BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private BtcWalletApi walletApi;
    private ConverterConfig converterConfig;
    private BtcBlockAnalysisHelper htgBlockAnalysisHelper;
    private HtgCommonHelper htgCommonHelper;
    private BtcAnalysisTxHelper btcAnalysisTxHelper;
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
            walletApi.checkApi();
            if (walletApi.isReSyncBlock()) {
                htgContext.logger().info("[{}]Delete all local blocks from the network and wait for the next round of execution", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                walletApi.setReSyncBlock(false);
                return;
            }
            // currentHTGThe latest blocks in the network
            //long bestBlockHeight = walletApi.getBestBlockHeight();
            //if(bestBlock == null) {
            //    htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
            //    return;
            //}
            long blockHeightFromNet = walletApi.getBestBlockHeight();
            // Latest local blocks
            HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // When starting a node, the local block is empty and will be removed from theHTGStarting synchronization at the latest height of the network
                BlockInfo block = walletApi.getBlockByHeight(blockHeightFromNet);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgBlockAnalysisHelper.analysisEthBlock(block, btcAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // When starting a node, the latest local altitude matchesHTGWhen the height of a network block differs by two or more blocks, it will be removed from theHTGNetwork height begins to synchronize
            if (firstSync && blockHeightFromNet - localBlockHeight >= 2) {
                BlockInfo block = walletApi.getBlockByHeight(blockHeightFromNet);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                htgBlockAnalysisHelper.analysisEthBlock(block, btcAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // Verify if the latest block is correct
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                htgContext.logger().error("obtain{}Block failure", htgContext.getConfig().getSymbol());
                return;
            } else if (resultCode == 1) {
                htgContext.logger().error("{}Block fork", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromNet - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * Synchronize and parse data
                 */
                try {
                    BlockInfo block = walletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                        break;
                    }
                    htgBlockAnalysisHelper.analysisEthBlock(block, btcAnalysisTxHelper);
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
        BlockHeader header = walletApi.getBlockHeaderByHeight(max.getHeight().longValue());
        if (header == null) {
            return 0;
        } else if (header.getHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
