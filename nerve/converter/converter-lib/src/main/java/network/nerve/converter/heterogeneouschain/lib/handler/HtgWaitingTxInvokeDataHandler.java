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
package network.nerve.converter.heterogeneouschain.lib.handler;

import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgResendHelper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxInvokeInfoStorageService;
import network.nerve.converter.utils.LoggerUtil;

import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.RESEND_TIME;


/**
 * Waiting for the transaction queue, the current node saves the call parameters for the transaction（The transaction is sent out by a certain administrator, and the administrator who ranks first in the order of the administrator sends out the transaction. If the sending fails or is not sent out, the transaction is sent out in the next order, and so on）
 *
 * @author: Mimi
 * @date: 2020-08-26
 */
public class HtgWaitingTxInvokeDataHandler implements Runnable, BeanInitial {

    private HtgTxInvokeInfoStorageService htTxInvokeInfoStorageService;
    private HtgWalletApi htgWalletApi;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgParseTxHelper htgParseTxHelper;
    private HtgResendHelper htgResendHelper;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void run() {
        IConverterCoreApi converterCoreApi = htgContext.getConverterCoreApi();
        String symbol = htgContext.getConfig().getSymbol();
        LinkedBlockingDeque<HtgWaitingTxPo> queue = htgContext.WAITING_TX_QUEUE();
        HtgWaitingTxPo po = null;
        try {
            if (!converterCoreApi.isRunning()) {
                LoggerUtil.LOG.debug("[{}]Ignoring synchronous block mode", symbol);
                return;
            }
            if (!htgContext.getConverterCoreApi().checkNetworkRunning(htgContext.HTG_CHAIN_ID())) {
                htgContext.logger().info("Test network[{}]Run Pause, chainId: {}", htgContext.getConfig().getSymbol(), htgContext.HTG_CHAIN_ID());
                return;
            }
            if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                LoggerUtil.LOG.debug("[{}]Non virtual bank member, skip this task", symbol);
                return;
            }
            if (!htgContext.isAvailableRPC()) {
                htgContext.logger().error("[{}]networkRPCUnavailable, pause this task", symbol);
                return;
            }
            LoggerUtil.LOG.debug("[{}Transaction call data waiting task] - every other{}Execute once per second.", symbol, htgContext.getConfig().getWaitingTxQueuePeriod());
            htgWalletApi.checkApi(converterCoreApi.getVirtualBankOrder());
            // Persistence tasks loaded while waiting for application restart
            htgContext.INIT_WAITING_TX_QUEUE_LATCH().await();
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                po = queue.poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("[{}]removeemptyobject", symbol);
                    }
                    continue;
                }
                String nerveTxHash = po.getNerveTxHash();
                // querynerveIs the transaction an issue left by the board of directors? Transaction
                if (converterCoreApi.skippedTransaction(nerveTxHash)) {
                    logger().info("[{}][Transaction call data waiting queue] Historical legacy problem data, Remove transaction, hash:{}", symbol, nerveTxHash);
                    this.clearDB(nerveTxHash);
                    continue;
                }
                // querynerveCorresponding to the transactionethWhether the transaction was successful
                if (htgInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("[{}]Nerve tx stay NERVE Network confirmed, Successfully removed queue, nerveHash: {}", symbol, nerveTxHash);
                    this.clearDB(nerveTxHash);
                    continue;
                }
                // each30Check each block once and check the contract,nervetransactionkeyIs it completed
                Long validateHeight = po.getValidateHeight();
                if (validateHeight == null) {
                    validateHeight = getCurrentBlockHeightOnNerve() + 30;
                }
                if (getCurrentBlockHeightOnNerve() >= validateHeight) {
                    validateHeight = getCurrentBlockHeightOnNerve() + 30;
                    if (htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash)) {
                        logger().info("Nerve tx stay {} Network confirmed, Successfully removed queue, nerveHash: {}", symbol, nerveTxHash);
                        this.clearDB(nerveTxHash);
                        continue;
                    }
                }
                po.setValidateHeight(validateHeight);

                if (!htgResendHelper.canResend(nerveTxHash)) {
                    logger().warn("[{}]Nervetransaction[{}]Resend over{}Second, discard transaction", symbol, nerveTxHash, RESEND_TIME);
                    htgResendHelper.clear(nerveTxHash);
                    this.clearDB(nerveTxHash);
                    continue;
                }
                long now = System.currentTimeMillis();
                long waitingEndTime = po.getWaitingEndTime();
                long maxWaitingEndTime = po.getMaxWaitingEndTime();
                int currentNodeSendOrder = po.getCurrentNodeSendOrder();
                logger().info("[{}]hash: {}, now: {}, waiting: {}, maxWaiting: {}, order: {}, isSend: {}", symbol, nerveTxHash, now, waitingEndTime, maxWaitingEndTime, currentNodeSendOrder, po.isInvokeResend());
                // If all administrators fail to send transactions, return to continue sending transactions from the first order
                if (now >= maxWaitingEndTime) {
                    logger().info("[{}]The maximum waiting time has ended, resend transactions starting from the first order, nerveHash: {}", symbol, nerveTxHash);
                    htgInvokeTxHelper.clearRecordOfCurrentNodeSentEthTx(nerveTxHash, po);
                    if (currentNodeSendOrder == 1) {
                        logger().info("[{}]First priority resend transaction, nerveHash: {}", symbol, nerveTxHash);
                        // Initiate transaction
                        htgResendHelper.reSend(po);
                        // Flag for recording called resend function
                        po.setInvokeResend(true);
                    }
                    // Incomplete, put back in queue
                    queue.offer(po);
                    continue;
                }
                // nerveTransaction not completed,[Non primary node] Check if the waiting time has ended. After that, check if the transaction has been initiated. Otherwise, initiate the transaction
                if (now >= waitingEndTime && currentNodeSendOrder != 1 && !po.isInvokeResend()) {
                    logger().info("[{}]Waiting time has ended, resend transaction, nerveHash: {}", symbol, nerveTxHash);
                    // Initiate transaction
                    htgResendHelper.reSend(po);
                    // Flag for recording called resend function
                    po.setInvokeResend(true);
                    // Incomplete, put back in queue
                    queue.offer(po);
                    continue;
                }
                // Incomplete, put back in queue
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
        htTxInvokeInfoStorageService.deleteByTxHash(nerveTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return htgContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
