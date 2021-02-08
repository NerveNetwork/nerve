/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.bnb.schedules;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbParseTxHelper;
import network.nerve.converter.heterogeneouschain.bnb.helper.BnbResendHelper;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbWaitingTxPo;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbTxInvokeInfoStorageService;
import network.nerve.converter.utils.LoggerUtil;

import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant.RESEND_TIME;
import static network.nerve.converter.heterogeneouschain.bnb.context.BnbContext.logger;


/**
 * 等待交易队列，当前节点保存交易的调用参数（交易由某一个管理员发出，按管理员顺序，排在首位的管理员发出交易，若发送失败或者未发出，则由下一顺位发出交易，以此类推）
 *
 * @author: Mimi
 * @date: 2020-08-26
 */
@Component("bnbWaitingTxInvokeDataScheduled")
public class BnbWaitingTxInvokeDataScheduled implements Runnable {

    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private BnbTxInvokeInfoStorageService bnbTxInvokeInfoStorageService;
    @Autowired
    private BnbInvokeTxHelper bnbInvokeTxHelper;
    @Autowired
    private BnbParseTxHelper bnbParseTxHelper;
    @Autowired
    private BnbResendHelper bnbResendHelper;

    public void run() {
        if (!BnbContext.getConverterCoreApi().isRunning()) {
            LoggerUtil.LOG.debug("忽略同步区块模式");
            return;
        }
        if (!BnbContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            return;
        }
        LoggerUtil.LOG.debug("[BNB交易调用数据等待任务] - 每隔10秒执行一次。");
        LinkedBlockingDeque<BnbWaitingTxPo> queue = BnbContext.WAITING_TX_QUEUE;
        BnbWaitingTxPo po = null;
        try {
            bnbWalletApi.checkApi(BnbContext.getConverterCoreApi().getVirtualBankOrder());
            // 等待重启应用时，加载的持久化任务
            BnbContext.INIT_WAITING_TX_QUEUE_LATCH.await();
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                po = queue.poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("移除empty对象");
                    }
                    continue;
                }
                String nerveTxHash = po.getNerveTxHash();
                // 查询nerve交易对应的eth交易是否成功
                if (bnbInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("Nerve tx 在NERVE网络已确认, 成功移除队列, nerveHash: {}", nerveTxHash);
                    this.clearDB(nerveTxHash);
                    continue;
                }
                // 每30个区块检查一次，查询合约中，nerve交易key是否是已完成
                Long validateHeight = po.getValidateHeight();
                if (validateHeight == null) {
                    validateHeight = getCurrentBlockHeightOnNerve() + 30;
                }
                if (getCurrentBlockHeightOnNerve() >= validateHeight) {
                    validateHeight = getCurrentBlockHeightOnNerve() + 30;
                    if (bnbParseTxHelper.isCompletedTransactionByLatest(nerveTxHash)) {
                        logger().info("Nerve tx 在BNB网络已确认, 成功移除队列, nerveHash: {}", nerveTxHash);
                        this.clearDB(nerveTxHash);
                        continue;
                    }
                }
                po.setValidateHeight(validateHeight);

                if (!bnbResendHelper.canResend(nerveTxHash)) {
                    logger().warn("Nerve交易[{}]重发超过{}次，丢弃交易", nerveTxHash, RESEND_TIME);
                    bnbResendHelper.clear(nerveTxHash);
                    this.clearDB(nerveTxHash);
                    continue;
                }
                // nerve交易未完成，[非首位节点] 检查等待时间是否结束，结束后，检查是否已发起交易，否则发起交易
                long waitingEndTime = po.getWaitingEndTime();
                if (System.currentTimeMillis() >= waitingEndTime && po.getCurrentNodeSendOrder() != 1 && !bnbInvokeTxHelper.currentNodeSentEthTx(nerveTxHash)) {
                    logger().info("等待时间已结束，重发交易, nerveHash: {}", nerveTxHash);
                    // 发起交易
                    bnbResendHelper.reSend(po);
                    // 未完成，放回队列
                    queue.offer(po);
                    continue;
                }
                // 检查若所有管理员均发送交易失败，则返回从第一顺位继续发交易
                if (System.currentTimeMillis() >= po.getMaxWaitingEndTime()) {
                    logger().info("最大等待时间已结束，从第一顺位开始重发交易, nerveHash: {}", nerveTxHash);
                    bnbInvokeTxHelper.clearRecordOfCurrentNodeSentEthTx(nerveTxHash, po);
                    if (po.getCurrentNodeSendOrder() == 1) {
                        logger().info("第一顺位重发交易, nerveHash: {}", nerveTxHash);
                        // 发起交易
                        bnbResendHelper.reSend(po);
                        this.clearDB(nerveTxHash);
                        continue;
                    }
                }
                // 未完成，放回队列
                queue.offer(po);
            }
        } catch (Exception e) {
            logger().error("waiting tx error", e);
            if (po != null) {
                queue.offer(po);
            }
        }
    }

    private void clearDB(String nerveTxHash) throws Exception {
        if(StringUtils.isBlank(nerveTxHash)) {
            return;
        }
        bnbTxInvokeInfoStorageService.deleteByTxHash(nerveTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return BnbContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
