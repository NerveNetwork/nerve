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
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.callback.BnbCallBackManager;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.docking.BnbDocking;
import network.nerve.converter.heterogeneouschain.bnb.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.bnb.helper.*;
import network.nerve.converter.heterogeneouschain.bnb.listener.BnbListener;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbSendTransactionPo;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbWaitingTxPo;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbTxStorageService;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.bnb.utils.BnbUtil;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.LoggerUtil;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant.*;
import static network.nerve.converter.heterogeneouschain.bnb.context.BnbContext.logger;
import static network.nerve.converter.heterogeneouschain.bnb.enums.BroadcastTxValidateStatus.SUCCESS;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
@Component("bnbConfirmTxScheduled")
public class BnbConfirmTxScheduled implements Runnable {

    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private BnbUnconfirmedTxStorageService bnbUnconfirmedTxStorageService;
    @Autowired
    private BnbTxRelationStorageService bnbTxRelationStorageService;
    @Autowired
    private BnbTxStorageService bnbTxStorageService;
    @Autowired
    private BnbCallBackManager bnbCallBackManager;
    @Autowired
    private BnbStorageHelper bnbStorageHelper;
    @Autowired
    private BnbLocalBlockHelper bnbLocalBlockHelper;
    @Autowired
    private BnbListener bnbListener;
    @Autowired
    private BnbInvokeTxHelper bnbInvokeTxHelper;
    @Autowired
    private BnbParseTxHelper bnbParseTxHelper;
    @Autowired
    private BnbResendHelper bnbResendHelper;
    @Autowired
    private BnbPendingTxHelper bnbPendingTxHelper;

    public void run() {
        if (!BnbContext.getConverterCoreApi().isRunning()) {
            LoggerUtil.LOG.debug("忽略同步区块模式");
            return;
        }
        if (!BnbContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            return;
        }
        LoggerUtil.LOG.debug("[BNB交易确认任务] - 每隔10秒执行一次。");
        LinkedBlockingDeque<BnbUnconfirmedTxPo> queue = BnbContext.UNCONFIRMED_TX_QUEUE;
        BnbUnconfirmedTxPo po = null;
        try {
            bnbWalletApi.checkApi(BnbContext.getConverterCoreApi().getVirtualBankOrder());
            // 等待重启应用时，加载的持久化未确认交易
            BnbContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.await();
            long ethNewestHeight = bnbWalletApi.getBlockHeight();
            int size = BnbContext.UNCONFIRMED_TX_QUEUE.size();
            for (int i = 0; i < size; i++) {
                po = BnbContext.UNCONFIRMED_TX_QUEUE.poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("移除空值PO");
                    }
                    continue;
                }
                // 清理无用的变更任务
                if (po.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                    clearUnusedChange();
                    break;
                }
                BnbUnconfirmedTxPo poFromDB = null;
                if (po.getBlockHeight() == null) {
                    poFromDB = bnbUnconfirmedTxStorageService.findByTxHash(po.getTxHash());
                    if (poFromDB != null) {
                        po.setBlockHeight(poFromDB.getBlockHeight());
                    }
                }
                if (po.getBlockHeight() == null) {
                    // 区块高度为空，检查10次后，查询eth交易所在高度，若节点同步的eth高度大于了交易所在高度，则说明此交易已经被其他节点处理过，应移除此交易
                    boolean needRemovePo = checkBlockHeightTimes(po);
                    if(needRemovePo) {
                        logger().info("区块高度为空，此交易已处理过，移除此交易，详情: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    // [加速重发交易机制] 没有区块高度，表示一直处于未解析状态，则说明没有被BNB打包（检查是否被打包），检查是否为本地发出的交易，加速重发交易
                    boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > BnbConstant.MINUTES_3;
                    if (timeOut) {
                        String bnbTxHash = po.getTxHash();
                        boolean currentNodeSent = bnbResendHelper.currentNodeSent(bnbTxHash);
                        if (currentNodeSent) {
                            // 当前节点发出的交易，检查是否已打包
                            boolean packed = checkPacked(bnbTxHash);
                            if (!packed) {
                                // 交易未打包，提高 gasPrice 加速重发交易
                                BnbSendTransactionPo txInfo = bnbResendHelper.getSentTransactionInfo(bnbTxHash);
                                boolean speedSent = speedUpResendTransaction(po.getTxType(), po.getNerveTxHash(), poFromDB, txInfo);
                                if (speedSent) {
                                    this.clearDB(bnbTxHash);
                                    continue;
                                }
                            }
                        }
                    }
                    if(logger().isDebugEnabled()) {
                        logger().debug("区块高度为空，放回队列等待下次处理，详情: {}", po.toString());
                    }
                    queue.offer(po);
                    continue;
                }

                // 交易触发重新验证后，等待`skipTimes`的轮次，再做验证
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    if(logger().isDebugEnabled()) {
                        logger().debug("交易触发重新验证，剩余等待再次验证的轮次数量: {}", po.getSkipTimes());
                    }
                    continue;
                }
                // 未达到确认高度，放回队列中，下次继续检查
                int confirmation = BnbContext.getConfig().getTxBlockConfirmations();
                if (po.getTxType() == HeterogeneousChainTxType.WITHDRAW) {
                    confirmation = BnbContext.getConfig().getTxBlockConfirmationsOfWithdraw();
                }
                if (ethNewestHeight - po.getBlockHeight() < confirmation) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("交易[{}]确认高度等待: {}", po.getTxHash(), confirmation - (ethNewestHeight - po.getBlockHeight()));
                    }
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("充值交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                    case UPGRADE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("广播交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    default:
                        logger().error("unkown tx: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        break;
                }
            }
        } catch (Exception e) {
            logger().error("confirming error", e);
            if (po != null) {
                queue.offer(po);
            }
        }
    }

    private void clearUnusedChange() {
        Iterator<BnbUnconfirmedTxPo> iterator = BnbContext.UNCONFIRMED_TX_QUEUE.iterator();
        while(iterator.hasNext()) {
            BnbUnconfirmedTxPo po = iterator.next();
            if (po.getTxType() == HeterogeneousChainTxType.CHANGE) {
                iterator.remove();
            }
        }
    }

    private boolean checkBlockHeightTimes(BnbUnconfirmedTxPo po) throws Exception {
        po.increaseBlockHeightTimes();
        if(po.getBlockHeightTimes() < 10) {
            return false;
        }
        po.setBlockHeightTimes(0);
        String txHash = po.getTxHash();
        if(StringUtils.isBlank(txHash)) {
            return false;
        }
        Transaction tx = bnbWalletApi.getTransactionByHash(txHash);
        if(tx == null) {
            return false;
        }
        BigInteger blockNumber = tx.getBlockNumber();
        // 本地最新的区块
        BnbSimpleBlockHeader localMax = bnbLocalBlockHelper.getLatestLocalBlockHeader();
        if(localMax == null) {
            return false;
        }
        if(localMax.getHeight().longValue() > blockNumber.longValue()) {
            return true;
        }
        return false;
    }

    private boolean dealDeposit(BnbUnconfirmedTxPo po, BnbUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String bnbTxHash = po.getTxHash();
        BnbUnconfirmedTxPo txPo = poFromDB;
        if(txPo == null) {
            txPo = bnbUnconfirmedTxStorageService.findByTxHash(bnbTxHash);
        }
        if (txPo == null) {
            logger().warn("[充值任务异常] DB中未获取到PO，队列中PO: {}", po.toString());
            return !isReOfferQueue;
        }
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，从队列中移除
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(bnbTxHash);
                isReOfferQueue = false;
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), BnbConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // 补充po内存数据，po打印日志，方便查看数据
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //再次验证交易
            boolean validateTx = validateDepositTxConfirmedInEthNet(bnbTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // 验证失败，从DB和队列中移除交易
                this.clearDB(bnbTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        try {
            // 回调充值交易
            String nerveTxHash = bnbCallBackManager.getDepositTxSubmitter().txSubmit(
                    bnbTxHash,
                    po.getBlockHeight(),
                    po.getFrom(),
                    po.getTo(),
                    po.getValue(),
                    po.getTxTime(),
                    po.getDecimals(),
                    po.isIfContractAsset(),
                    po.getContractAddress(),
                    po.getAssetId(),
                    po.getNerveAddress());
            po.setNerveTxHash(nerveTxHash);
            txPo.setNerveTxHash(nerveTxHash);
            // 当未确认交易数据产生变化时，更新DB数据
            boolean nerveTxHashNotBlank = StringUtils.isNotBlank(nerveTxHash);
            if (nerveTxHashNotBlank) {
                bnbUnconfirmedTxStorageService.update(txPo, update -> update.setNerveTxHash(nerveTxHash));
                if (nerveTxHashNotBlank) {
                    bnbStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (Exception e) {
            // 交易已存在，移除队列
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("Nerve交易已存在，从队列中移除待确认的BNB交易[{}]", bnbTxHash);
                return !isReOfferQueue;
            }
            throw e;
        }
        return isReOfferQueue;
    }

    private boolean dealBroadcastTx(BnbUnconfirmedTxPo po, BnbUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String bnbTxHash = po.getTxHash();
        BnbUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = bnbUnconfirmedTxStorageService.findByTxHash(bnbTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}任务异常] DB中未获取到PO，队列中PO: {}", po.getTxType(), po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，不放回队列
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(bnbTxHash);
                isReOfferQueue = false;
                bnbResendHelper.clear(nerveTxHash);
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), BnbConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // 补充po内存数据，po打印日志，方便查看数据
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }

        switch (txPo.getStatus()) {
            case INITIAL:
                break;
            case FAILED:
                if (!bnbResendHelper.canResend(nerveTxHash)) {
                    logger().warn("Nerve交易[{}]重发超过{}次，丢弃交易", nerveTxHash, RESEND_TIME);
                    bnbResendHelper.clear(nerveTxHash);
                    return !isReOfferQueue;
                }
                logger().info("失败的BNB交易[{}]，检查当前节点是否可发交易", bnbTxHash);
                // 检查自己的顺序是否可发交易
                BnbWaitingTxPo waitingTxPo = bnbInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                // 检查三个轮次是否有waitingTxPo，否则移除此FAILED任务
                if (waitingTxPo == null && !po.checkFailedTimeOut()) {
                    return isReOfferQueue;
                }
                // 查询nerve交易对应的eth交易是否成功
                if (bnbInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("Nerve tx 在NERVE网络已确认, 成功移除队列, nerveHash: {}", nerveTxHash);
                    this.clearDB(nerveTxHash);
                    return !isReOfferQueue;
                }
                if (waitingTxPo == null) {
                    logger().info("检查三个轮次没有waitingTxPo，移除此FAILED任务, bnbTxHash: {}", bnbTxHash);
                    return !isReOfferQueue;
                }
                if (this.checkIfSendByOwn(waitingTxPo, txPo.getFrom())) {
                    this.clearDB(bnbTxHash);
                    return !isReOfferQueue;
                }
                // 失败的交易，不由当前节点处理，从队列和DB中移除
                logger().info("失败的BNB交易[{}]，当前节点不是下一顺位，不由当前节点处理，移除队列", bnbTxHash);
                this.clearDB(bnbTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //再次验证交易
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInEthNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // 放回队列，再次验证
                            return isReOfferQueue;
                        case RE_SEND:
                            // 若交易状态已完成，再次验证失败，则重发交易
                            BnbWaitingTxPo _waitingTxPo = bnbInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                            // 检查三个轮次是否有waitingTxPo，否则移除此FAILED任务
                            if (_waitingTxPo == null && po.checkFailedTimeOut()) {
                                return isReOfferQueue;
                            }
                            bnbResendHelper.reSend(_waitingTxPo);
                            this.clearDB(bnbTxHash);
                            return !isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == SUCCESS);
                }
                try {
                    String realNerveTxHash = nerveTxHash;
                    logger().info("[{}]签名完成的BNB交易[{}]调用Nerve确认[{}]", po.getTxType(), bnbTxHash, realNerveTxHash);
                    // 签名完成的交易将触发回调Nerve Core
                    bnbCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            bnbTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            BnbContext.MULTY_SIGN_ADDRESS,
                            txPo.getSigners());
                } catch (NulsException e) {
                    // 交易已存在，等待确认移除
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nerve交易[{}]已存在，从队列中移除待确认的BNB交易[{}]", txPo.getNerveTxHash(), bnbTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    /**
     * 若eth交易失败，则检查当前节点是否为下一顺位发交易，是则重发交易
     */
    private boolean checkIfSendByOwn(BnbWaitingTxPo waitingTxPo, String txFrom) throws Exception {
        String nerveTxHash = waitingTxPo.getNerveTxHash();
        Map<String, Integer> virtualBanks = waitingTxPo.getCurrentVirtualBanks();
        Integer totalBank = virtualBanks.size();
        Integer sendOrderCurrentNode = waitingTxPo.getCurrentNodeSendOrder();//
        int sendOrderFailure = virtualBanks.get(txFrom);
        // 检查失败的order是否为管理员顺位中最后一位，是则重置当前节点已发BNB交易的记录，重置节点等待时间，从第一顺位开始重发交易
        if (sendOrderFailure == totalBank) {
            bnbInvokeTxHelper.clearRecordOfCurrentNodeSentEthTx(nerveTxHash, waitingTxPo);
            if (sendOrderCurrentNode == 1) {
                // 发起交易
                bnbResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        if (sendOrderFailure + 1 == sendOrderCurrentNode) {
            // 当前节点是下一顺位发交易，检查是否已发出交易，否则发出交易
            if (!bnbInvokeTxHelper.currentNodeSentEthTx(nerveTxHash)) {
                // 发起交易
                bnbResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        return false;
    }

    private boolean speedUpResendTransaction(HeterogeneousChainTxType txType, String nerveTxHash, BnbUnconfirmedTxPo unconfirmedTxPo, BnbSendTransactionPo txInfo) throws Exception {
        if (txInfo == null) {
            return false;
        }
        logger().info("检测到需要加速重发交易，类型: {}, ethHash: {}, nerveTxHash: {}", txType, unconfirmedTxPo.getTxHash(), nerveTxHash);
        // 向BNB网络请求验证
        boolean isCompleted = bnbParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            // 发出一个转账给自己的交易覆盖此nonce
            String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
            if (StringUtils.isNotBlank(overrideHash)) {
                logger().info("转账覆盖交易: {}，被覆盖交易: {}", overrideHash, txInfo.getTxHash());
            } else {
                logger().info("未成功发出覆盖交易");
            }
            return true;
        }
        if (logger().isDebugEnabled()) {
            logger().debug("加速前: {}", txInfo.toString());
        }
        // 检查发出交易的地址和当前虚拟银行地址是否一致，否则，重新获取nonce发出交易
        String from = txInfo.getFrom();
        String currentFrom = BnbContext.ADMIN_ADDRESS;
        BigInteger nonce = txInfo.getNonce();
        if (!currentFrom.equals(from)) {
            nonce = bnbWalletApi.getNonce(currentFrom);
        }
        txInfo.setNonce(nonce);

        BigInteger oldGasPrice = txInfo.getGasPrice();
        BigInteger newGasPrice;
        boolean isWithdrawTx = HeterogeneousChainTxType.WITHDRAW == txType;
        IConverterCoreApi coreApi = BnbContext.getConverterCoreApi();
        if (isWithdrawTx && coreApi.isSupportNewMechanismOfWithdrawalFee()) {
            // 计算GasPrice
            BigDecimal gasPrice = BnbUtil.calGasPriceOfWithdraw(coreApi.getUsdtPriceByAsset(AssetName.NVT), coreApi.getFeeOfWithdrawTransaction(nerveTxHash), coreApi.getUsdtPriceByAsset(AssetName.BNB), unconfirmedTxPo.getAssetId());
            if (gasPrice == null || gasPrice.toBigInteger().compareTo(oldGasPrice) < 0) {
                logger().error("[提现]交易[{}]手续费不足，最新提供的GasPrice: {}, 当前已发出交易[{}]的GasPrice: {}", nerveTxHash, gasPrice == null ? null : gasPrice.toPlainString(), txInfo.getTxHash(), oldGasPrice);
                return false;
            }
            gasPrice = BnbUtil.calNiceGasPriceOfWithdraw(new BigDecimal(BnbContext.getEthGasPrice()), gasPrice);
            newGasPrice = gasPrice.toBigInteger();
            if (newGasPrice.compareTo(BnbContext.getEthGasPrice()) < 0) {
                logger().error("[提现]交易[{}]手续费不足，最新提供的GasPrice: {}, 当前Binance网络的GasPrice: {}", nerveTxHash, newGasPrice, BnbContext.getEthGasPrice());
                return false;
            }
        } else {
            newGasPrice = calSpeedUpGasPriceByOrdinaryWay(oldGasPrice);
        }
        if (newGasPrice == null) {
            return false;
        }
        txInfo.setGasPrice(newGasPrice);
        // 获取账户信息
        HeterogeneousAccount account = BnbDocking.getInstance().getAccount(currentFrom);
        account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 验证业务数据
        String contractAddress = txInfo.getTo();
        String encodedFunction = txInfo.getData();
        EthCall ethCall = bnbWalletApi.validateContractCall(currentFrom, contractAddress, encodedFunction);
        if (ethCall.isReverted()) {
            if (ConverterUtil.isCompletedTransaction(ethCall.getRevertReason())) {
                logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
                // 发出一个转账给自己的交易覆盖此nonce
                String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
                if (StringUtils.isNotBlank(overrideHash)) {
                    logger().info("转账覆盖交易: {}，被覆盖交易: {}", overrideHash, txInfo.getTxHash());
                } else {
                    logger().info("未成功发出覆盖交易");
                }
                return true;
            }
            logger().warn("[{}]加速重发交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            return false;
        }
        BnbSendTransactionPo newTxPo = bnbWalletApi.callContractRaw(priKey, txInfo);
        String bnbTxHash = newTxPo.getTxHash();
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        bnbTxRelationStorageService.save(bnbTxHash, nerveTxHash, newTxPo);

        BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
        BeanUtils.copyProperties(unconfirmedTxPo, po);
        // 保存未确认交易
        po.setTxHash(bnbTxHash);
        po.setFrom(currentFrom);
        po.setTxType(txType);
        po.setCreateDate(System.currentTimeMillis());
        bnbUnconfirmedTxStorageService.save(po);
        BnbContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        bnbListener.addListeningTx(bnbTxHash);
        if (isWithdrawTx && StringUtils.isNotBlank(bnbTxHash)) {
            // 记录提现交易已向BNB网络发出
            bnbPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, bnbTxHash);
        }
        logger().info("加速重发BNB网络交易成功, 类型: {}, 详情: {}", txType, po.superString());
        if (logger().isDebugEnabled()) {
            logger().debug("加速后: {}", newTxPo.toString());
        }
        return true;
    }

    private BigInteger calSpeedUpGasPriceByOrdinaryWay(BigInteger oldGasPrice) throws Exception {
        // 提高交易的price，获取当前BNB网络price，和旧的price取较大值，再+2，即 price = price + 2，最大当前price的1.1倍
        BigInteger currentGasPrice = bnbWalletApi.getCurrentGasPrice();
        BigInteger maxCurrentGasPrice = new BigDecimal(currentGasPrice).multiply(BnbConstant.NUMBER_1_DOT_1).toBigInteger();
        if (maxCurrentGasPrice.compareTo(oldGasPrice) <= 0) {
            logger().info("当前交易的gasPrice已达到加速最大值，不再继续加速，等待BNB网络打包交易");
            return null;
        }
        BigInteger newGasPrice = oldGasPrice.compareTo(currentGasPrice) > 0 ? oldGasPrice : currentGasPrice;
        newGasPrice = newGasPrice.add(BnbConstant.GWEI_2);
        newGasPrice = newGasPrice.compareTo(maxCurrentGasPrice) > 0 ? maxCurrentGasPrice : newGasPrice;
        return newGasPrice;
    }

    private String sendOverrideTransferTx(String from, BigInteger gasPrice, BigInteger nonce) {
        try {
            // 检查发出交易的地址和当前虚拟银行地址是否一致，否则，忽略
            String currentFrom = BnbContext.ADMIN_ADDRESS;
            if (!currentFrom.equals(from)) {
                logger().info("发出转账覆盖交易的地址和当前虚拟银行地址不一致，忽略");
                return null;
            }
            // 获取账户信息
            HeterogeneousAccount account = BnbDocking.getInstance().getAccount(currentFrom);
            account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // 获取网络gasprice
            BigInteger currentGasPrice = bnbWalletApi.getCurrentGasPrice();
            BigInteger newGasPrice = gasPrice.compareTo(currentGasPrice) > 0 ? gasPrice : currentGasPrice;
            newGasPrice = newGasPrice.add(BnbConstant.GWEI_3);
            if (logger().isDebugEnabled()) {
                logger().debug("组装的覆盖交易数据, from: {}, to: {}, value: {}, gasLimit: {}, gasPrice: {}, nonce: {}",
                        currentFrom,
                        currentFrom,
                        BigInteger.ZERO,
                        BnbConstant.BNB_GAS_LIMIT_OF_BNB,
                        newGasPrice,
                        nonce);
            }
            String hash = bnbWalletApi.sendBNBWithNonce(currentFrom, priKey, currentFrom, BigDecimal.ZERO, BnbConstant.BNB_GAS_LIMIT_OF_BNB, newGasPrice, nonce);
            return hash;
        } catch (Exception e) {
            logger().warn("发生转账覆盖交易异常，忽略", e);
            return null;
        }

    }

    /**
     * 检查eth交易是否被打包
     */
    private boolean checkPacked(String bnbTxHash) throws Exception {
        TransactionReceipt txReceipt = bnbWalletApi.getTxReceipt(bnbTxHash);
        return txReceipt != null;
    }

    /**
     * 验证充值交易
     */
    private boolean validateDepositTxConfirmedInEthNet(String bnbTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            TransactionReceipt receipt = bnbWalletApi.getTxReceipt(bnbTxHash);
            if (receipt == null) {
                logger().error("再次验证交易[{}]失败，获取不到receipt", bnbTxHash);
                break;
            }
            if (!receipt.isStatusOK()) {
                logger().error("再次验证交易[{}]失败，receipt状态不正确", bnbTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogs() == null || receipt.getLogs().size() == 0)) {
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", bnbTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * 验证发到BNB网络的交易是否确认，若有异常情况，则根据条件重发交易
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInEthNet(BnbUnconfirmedTxPo po) throws Exception {

        BroadcastTxValidateStatus status;
        String bnbTxHash = po.getTxHash();
        do {
            TransactionReceipt receipt = bnbWalletApi.getTxReceipt(bnbTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > BnbConstant.MINUTES_20;
                logger().error("再次验证交易[{}]失败，获取不到receipt", bnbTxHash);
                if (timeOut) {
                    // 交易二十分钟未确认，则重发交易
                    status = BroadcastTxValidateStatus.RE_SEND;
                } else {
                    // 未获取到交易收据，20分钟内，每3轮验证一次
                    po.setSkipTimes(3);
                    status = BroadcastTxValidateStatus.RE_VALIDATE;
                }
                break;
            }
            if (!receipt.isStatusOK()) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("再次验证交易[{}]失败，receipt状态不正确", bnbTxHash);
                break;
            } else if (receipt.getLogs() == null || receipt.getLogs().size() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", bnbTxHash);
                break;
            }
            status = SUCCESS;
        } while (false);
        return status;
    }

    private void clearDB(String bnbTxHash) throws Exception {
        if(StringUtils.isBlank(bnbTxHash)) {
            return;
        }
        bnbUnconfirmedTxStorageService.deleteByTxHash(bnbTxHash);
        bnbTxRelationStorageService.deleteByTxHash(bnbTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return BnbContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
