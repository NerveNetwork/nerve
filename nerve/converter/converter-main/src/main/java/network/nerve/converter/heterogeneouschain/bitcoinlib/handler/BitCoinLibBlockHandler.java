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
package network.nerve.converter.heterogeneouschain.bitcoinlib.handler;

import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockHeader;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BitCoinLibBlockInfo;
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
public class BitCoinLibBlockHandler implements Runnable, BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private IBitCoinLibWalletApi walletApi;
    private ConverterConfig converterConfig;
    private HtgCommonHelper htgCommonHelper;
    private HtgCallBackManager htgCallBackManager;
    private BitCoinLibBlockAnalysisHelper blockAnalysisHelper;
    private BitCoinLibAnalysisTxHelper analysisTxHelper;

    private boolean firstSync = true;
    private boolean clearDB = false;

    private HtgContext htgContext;

    public void run() {
        try {
            if (!htgContext.getConverterCoreApi().isRunning()) {
                if (LoggerUtil.LOG.isDebugEnabled()) {
                    LoggerUtil.LOG.debug("[{}] Ignoring synchronous block mode", htgContext.getConfig().getSymbol());
                }
                return;
            }
            if (!htgContext.getConverterCoreApi().checkNetworkRunning(htgContext.HTG_CHAIN_ID())) {
                htgContext.logger().info("Test network [{}] Run Pause, chainId: {}", htgContext.getConfig().getSymbol(), htgContext.HTG_CHAIN_ID());
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
                    LoggerUtil.LOG.debug("[{}] Non virtual bank member, skip this task", htgContext.getConfig().getSymbol());
                }
                return;
            }
            clearDB = false;
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("[{} Block parsing task] - every other {} Execute once per second.", htgContext.getConfig().getSymbol(), htgContext.getConfig().getBlockQueuePeriod());
            }
            try {
                htgCommonHelper.clearHash();
                walletApi.priceMaintain();
            } catch (Exception e) {
                htgContext.logger().error("Clearing recharge transactionshashFailed to revalidate collection", e);
            }
            walletApi.checkApi();
            if (walletApi.isReSyncBlock()) {
                htgContext.logger().info("[{}] Delete all local blocks from the network and wait for the next round of execution", htgContext.getConfig().getSymbol());
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
                BitCoinLibBlockInfo block = walletApi.getBitCoinLibBlockByHeight(blockHeightFromNet);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain {} Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                blockAnalysisHelper.analysisEthBlock(
                        block.getTxList(),
                        block.getHeader().getBlockHeight(),
                        block.getHeader().getTxTime(),
                        block.getHeader().getBlockHash(),
                        block.getHeader().getPreBlockHash(),
                        analysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            long difference = blockHeightFromNet - localBlockHeight;
            // When starting a node, the latest local altitude matchesHTGWhen the height of a network block differs by two or more blocks, it will be removed from theHTGNetwork height begins to synchronize
            if (firstSync && Math.abs(difference) >= 20) {
                BitCoinLibBlockInfo block = walletApi.getBitCoinLibBlockByHeight(blockHeightFromNet);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain {} Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                blockAnalysisHelper.analysisEthBlock(
                        block.getTxList(),
                        block.getHeader().getBlockHeight(),
                        block.getHeader().getTxTime(),
                        block.getHeader().getBlockHash(),
                        block.getHeader().getPreBlockHash(),
                        analysisTxHelper);
                firstSync = false;
                return;
            }

            // Verify if the latest block is correct
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                htgContext.logger().error("obtain {} Block failure", htgContext.getConfig().getSymbol());
                return;
            } else if (resultCode == 1) {
                htgContext.logger().error("{} Block fork", htgContext.getConfig().getSymbol());
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
                    BitCoinLibBlockInfo block = walletApi.getBitCoinLibBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("Unable to obtain {} Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                        break;
                    }
                    blockAnalysisHelper.analysisEthBlock(
                            block.getTxList(),
                            block.getHeader().getBlockHeight(),
                            block.getHeader().getTxTime(),
                            block.getHeader().getBlockHash(),
                            block.getHeader().getPreBlockHash(),
                            analysisTxHelper);
                } catch (Exception e) {
                    htgContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Throwable e) {
            htgContext.logger().error(String.format("synchronization %s Block failure", htgContext.getConfig().getSymbol()), e);
        }
    }

    private int checkNewestBlock(HtgSimpleBlockHeader max) throws Exception {
        BitCoinLibBlockHeader header = walletApi.getBitCoinLibBlockHeaderByHeight(max.getHeight().longValue());
        if (header == null) {
            return 0;
        } else if (header.getBlockHash().equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
