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

import java.math.BigDecimal;
import java.math.BigInteger;

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

    //public HtgBlockHandler(BeanMap beanMap) {
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}


    public void run() {
        try {
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
            htgWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            // 当前HTG网络最新的区块
            long blockHeightFromEth = htgWalletApi.getBlockHeight();
            // 本地最新的区块
            HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
            if (localMax == null) {
                // 当启动节点时，本地区块为空，将从HTG网络最新高度开始同步
                EthBlock.Block block = htgWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                    return;
                }
                htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
                firstSync = false;
                return;
            }
            Long localBlockHeight = localMax.getHeight();
            // 当启动节点时，本地最新高度与HTG网络区块高度相差两个区块及以上时，则从HTG网络高度开始同步
            if (firstSync && blockHeightFromEth - localBlockHeight >= 2) {
                EthBlock.Block block = htgWalletApi.getBlockByHeight(blockHeightFromEth);
                if(block == null) {
                    htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                    return;
                }
                htgLocalBlockHelper.deleteAllLocalBlockHeader();
                htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
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
                    EthBlock.Block block = htgWalletApi.getBlockByHeight(localBlockHeight);
                    if(block == null) {
                        htgContext.logger().info("获取不到{}区块，等待下轮执行", htgContext.getConfig().getSymbol());
                        break;
                    }
                    htgBlockAnalysisHelper.analysisEthBlock(block, htgAnalysisTxHelper);
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
