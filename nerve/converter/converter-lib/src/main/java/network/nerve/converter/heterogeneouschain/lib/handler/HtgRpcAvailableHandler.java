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

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.MINUTES_1;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgRpcAvailableHandler implements Runnable, BeanInitial {

    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgWalletApi htgWalletApi;
    private ConverterConfig converterConfig;
    private HtgBlockAnalysisHelper htgBlockAnalysisHelper;
    private HtgCommonHelper htgCommonHelper;
    private HtgAnalysisTxHelper htgAnalysisTxHelper;
    private HtgCallBackManager htgCallBackManager;
    private HtgContext htgContext;

    private long lastRecordHeight = 0;
    private long lastRecordTime = 0;

    public void run() {
        if (!htgContext.getConverterCoreApi().isRunning()) {
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("[{}]忽略同步区块模式", htgContext.getConfig().getSymbol());
            }
            return;
        }
        if (LoggerUtil.LOG.isDebugEnabled()) {
            LoggerUtil.LOG.debug("[{}网络RPC可用性检查任务] - 每隔{}秒执行一次。", htgContext.getConfig().getSymbol(), htgContext.getConfig().getBlockQueuePeriod());
        }
        try {
            boolean availableRPC = true;
            do {
                // 本地最新的区块
                HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
                if (localMax == null) {
                    // 当启动节点时，本地区块为空，没有检查依据，跳过本次检查
                    break;
                }
                Long localBlockHeight = localMax.getHeight();
                if (lastRecordHeight == 0 || localBlockHeight > lastRecordHeight) {
                    lastRecordHeight = localBlockHeight;
                    lastRecordTime = System.currentTimeMillis();
                    break;
                }
                if (localBlockHeight == lastRecordHeight) {
                    long now = System.currentTimeMillis();
                    if (lastRecordTime == 0) {
                        lastRecordTime = now;
                        break;
                    }
                    if (now - lastRecordTime > MINUTES_1) {
                        htgContext.logger().error("{}网络区块同步异常，本地区块高度: {}, 已有{}秒未同步区块，请检查网络RPC服务",
                                htgContext.getConfig().getSymbol(),
                                localBlockHeight,
                                (now - lastRecordTime) / 1000);
                        availableRPC = false;
                        break;
                    }
                }
            } while (false);
            htgContext.setAvailableRPC(availableRPC);
        } catch (Exception e) {
            htgContext.logger().error(String.format("{}网络RPC可用性检查任务失败", htgContext.getConfig().getSymbol()), e);
        }
    }

}
