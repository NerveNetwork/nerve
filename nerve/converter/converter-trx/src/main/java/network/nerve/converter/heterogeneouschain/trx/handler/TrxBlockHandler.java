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
package network.nerve.converter.heterogeneouschain.trx.handler;

import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxBlockAnalysisHelper;
import network.nerve.converter.utils.LoggerUtil;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Numeric;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class TrxBlockHandler implements Runnable, BeanInitial {

    private TrxWalletApi trxWalletApi;
    private TrxBlockAnalysisHelper trxBlockAnalysisHelper;
    private TrxAnalysisTxHelper trxAnalysisTxHelper;
    private HtgCallBackManager htgCallBackManager;
    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgCommonHelper htgCommonHelper;
    private ConverterConfig converterConfig;

    private boolean firstSync = true;
    private boolean clearDB = false;
    private boolean managerChangeSync = false;

    private HtgContext htgContext;

    public void run() {
        try {
            if (!htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) return;
            if (!htgContext.getConverterCoreApi().isRunning()) {
                if (LoggerUtil.LOG.isDebugEnabled()) {
                    LoggerUtil.LOG.debug("[{}]Ignoring synchronous block mode", htgContext.getConfig().getSymbol());
                }
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
            trxWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            if (trxWalletApi.isReSyncBlock()) {
                htgContext.logger().info("[{}]Delete all local blocks from the network and wait for the next round of execution", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                trxWalletApi.setReSyncBlock(false);
                return;
            }
            // + Due to the production issue of block parsing failure, a virtual bank change occurred during this period, and the main network must synchronize the wave field height 34899400
            if (!managerChangeSync) {
                long managerChangeHeight = 34899400L;
                managerChangeSync = htgLocalBlockHelper.isSynced(managerChangeHeight);
                if (htgContext.NERVE_CHAINID() == 9 && !managerChangeSync) {
                    Response.BlockExtention block = trxWalletApi.getBlockByHeight(managerChangeHeight);
                    if(block == null) {
                        htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                        return;
                    }
                    htgLocalBlockHelper.deleteAllLocalBlockHeader();
                    trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                    htgLocalBlockHelper.saveSynced(managerChangeHeight);
                    managerChangeSync = true;
                    return;
                }
            }
            // -

            // currentHTGThe latest blocks in the network
            long blockHeightFromEth = trxWalletApi.getBlockHeight();
            // Latest local blocks
            HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // When starting a node, the local block is empty and will be removed from theHTGStarting synchronization at the latest height of the network
                Response.BlockExtention block = trxWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            long difference = blockHeightFromEth - localBlockHeight;
            // When starting a node, the latest local altitude matchesHTGWhen the height of a network block differs by two or more blocks, it will be removed from theHTGNetwork height begins to synchronize
            if (firstSync && Math.abs(difference) >= 2) {
                Response.BlockExtention block = trxWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
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

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * Synchronize and parse data
                 */
                try {
                    Response.BlockExtention block = trxWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("Unable to obtain{}Block, waiting for the next round of execution", htgContext.getConfig().getSymbol());
                        break;
                    }
                    trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                } catch (Exception e) {
                    htgContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            htgContext.logger().error(String.format("synchronization%sBlock failure", htgContext.getConfig().getSymbol()), e);
        }
    }

    private int checkNewestBlock(HtgSimpleBlockHeader max) throws Exception {
        Response.BlockExtention block = trxWalletApi.getBlockByHeight(max.getHeight().longValue());
        String blockHash = Numeric.toHexString(block.getBlockid().toByteArray());
        if (block == null) {
            return 0;
        } else if (blockHash.equals(max.getHash())) {
            return 2;
        } else {
            return 1;
        }
    }
}
