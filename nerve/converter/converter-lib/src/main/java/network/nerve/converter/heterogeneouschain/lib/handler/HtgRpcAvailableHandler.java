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

import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
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
    private HtgContext htgContext;

    private long lastRecordHeight = 0;
    private long lastRecordTime = 0;

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
            if (LoggerUtil.LOG.isDebugEnabled()) {
                LoggerUtil.LOG.debug("[{} network RPC Availability check task] - every other {} Execute once per second.", htgContext.getConfig().getSymbol(), htgContext.getConfig().getBlockQueuePeriod());
            }
            boolean availableRPC = true;
            do {
                // Latest local blocks
                HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
                if (localMax == null) {
                    // When starting a node, the local block is empty with no basis for checking, skipping this check
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
                        htgContext.logger().error("{}Network block synchronization exception, local block height: {}, Existing{}Block not synchronized in seconds, please check the networkRPCservice",
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
            htgContext.logger().error(String.format("{} network RPC Availability check task failed", htgContext.getConfig().getSymbol()), e);
        }
    }

}
