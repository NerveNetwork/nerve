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
                    LoggerUtil.LOG.debug("[{}]忽略同步区块模式", htgContext.getConfig().getSymbol());
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
                    LoggerUtil.LOG.debug("[{}]非虚拟银行成员，跳过此任务", htgContext.getConfig().getSymbol());
                }
                return;
            }
            clearDB = false;
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("[{}区块解析任务] - 每隔{}秒执行一次。", htgContext.getConfig().getSymbol(), htgContext.getConfig().getBlockQueuePeriod());
            }
            try {
                htgCommonHelper.clearHash();
            } catch (Exception e) {
                htgContext.logger().error("清理充值交易hash再次验证的集合失败", e);
            }
            trxWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            // + 由于区块解析失败的生产问题，期间产生了一个虚拟银行变更，主网必须同步波场高度 34899400
            if (!managerChangeSync) {
                long managerChangeHeight = 34899400L;
                managerChangeSync = htgLocalBlockHelper.isSynced(managerChangeHeight);
                if (htgContext.NERVE_CHAINID() == 9 && !managerChangeSync) {
                    Response.BlockExtention block = trxWalletApi.getBlockByHeight(managerChangeHeight);
                    if(block == null) {
                        htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
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

            // 当前HTG网络最新的区块
            long blockHeightFromEth = trxWalletApi.getBlockHeight();
            // 本地最新的区块
            HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 当启动节点时，本地区块为空，将从HTG网络最新高度开始同步
                Response.BlockExtention block = trxWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                    return;
                }
                trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当启动节点时，本地最新高度与HTG网络区块高度相差两个区块及以上时，则从HTG网络高度开始同步
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                Response.BlockExtention block = trxWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                firstSync = false;
                return;
            }

            // 验证最新区块是否正确
            int resultCode = checkNewestBlock(localMax);
            if (resultCode == 0) {
                htgContext.logger().error("获取{}区块失败", htgContext.getConfig().getSymbol());
                return;
            } else if (resultCode == 1) {
                htgContext.logger().error("{}区块分叉", htgContext.getConfig().getSymbol());
                htgLocalBlockHelper.deleteByHeightAndUpdateMemory(localBlockHeight);
                return;
            }

            long size = blockHeightFromEth - localBlockHeight;
            for (int i = 1; i <= size; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                try {
                    Response.BlockExtention block = trxWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                        break;
                    }
                    trxBlockAnalysisHelper.analysisEthBlock(block, trxAnalysisTxHelper);
                } catch (Exception e) {
                    htgContext.logger().error("syncHeight error ", e);
                    break;
                }
            }
        } catch (Exception e) {
            htgContext.logger().error(String.format("同步%s区块失败", htgContext.getConfig().getSymbol()), e);
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
