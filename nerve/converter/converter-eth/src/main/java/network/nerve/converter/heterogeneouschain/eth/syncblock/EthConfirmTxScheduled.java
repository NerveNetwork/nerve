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
package network.nerve.converter.heterogeneouschain.eth.syncblock;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.docking.EthDocking;
import network.nerve.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthResendHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthStorageHelper;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.EthRecoveryDto;
import network.nerve.converter.heterogeneouschain.eth.model.EthSendTransactionPo;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.register.EthRegister;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.model.bo.HeterogeneousWithdrawTxInfo;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.LoggerUtil;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.*;
import static network.nerve.converter.heterogeneouschain.eth.context.EthContext.logger;
import static network.nerve.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus.SUCCESS;

/**
 * @author: Mimi
 * @date: 2020-03-02
 */
@Component("ethConfirmTxScheduled")
public class EthConfirmTxScheduled implements Runnable {

    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    @Autowired
    private EthTxStorageService ethTxStorageService;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthRegister ethRegister;
    @Autowired
    private EthStorageHelper ethStorageHelper;
    @Autowired
    private EthResendHelper ethResendHelper;
    @Autowired
    private EthParseTxHelper ethParseTxHelper;
    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;
    @Autowired
    private EthListener ethListener;

    public void run() {
        if (!EthContext.getConverterCoreApi().isRunning()) {
            LoggerUtil.LOG.debug("忽略同步区块模式");
            return;
        }
        if (!EthContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            LoggerUtil.LOG.debug("非虚拟银行成员，跳过此任务");
            return;
        }
        LoggerUtil.LOG.debug("[ETH交易确认任务] - 每隔20秒执行一次。");
        LinkedBlockingDeque<EthUnconfirmedTxPo> queue = EthContext.UNCONFIRMED_TX_QUEUE;
        EthUnconfirmedTxPo po = null;
        try {
            ethWalletApi.checkApi(EthContext.getConverterCoreApi().getVirtualBankOrder());
            // 等待重启应用时，加载的持久化未确认交易
            EthContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.await();
            long ethNewestHeight = ethWalletApi.getBlockHeight();
            int size = EthContext.UNCONFIRMED_TX_QUEUE.size();
            for (int i = 0; i < size; i++) {
                po = EthContext.UNCONFIRMED_TX_QUEUE.poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("移除空值PO");
                    }
                    continue;
                }
                EthUnconfirmedTxPo poFromDB = null;
                if (po.getBlockHeight() == null) {
                    poFromDB = ethUnconfirmedTxStorageService.findByTxHash(po.getTxHash());
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
                    // [加速重发交易机制] 没有区块高度，表示一直处于未解析状态，则说明没有被ETH打包（检查是否被打包），检查是否为本地发出的交易，加速重发交易
                    boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > EthConstant.MINUTES_5;
                    if (timeOut) {
                        String ethTxHash = po.getTxHash();
                        boolean currentNodeSent = ethResendHelper.currentNodeSent(ethTxHash);
                        if (currentNodeSent) {
                            // 当前节点发出的交易，检查是否已打包
                            boolean packed = checkPacked(ethTxHash);
                            if (!packed) {
                                // 交易未打包，提高 gasPrice 加速重发交易
                                EthSendTransactionPo txInfo = ethResendHelper.getSentTransactionInfo(ethTxHash);
                                boolean speedSent = speedUpResendTransaction(po.getTxType(), po.getNerveTxHash(), poFromDB, txInfo);
                                if (speedSent) {
                                    this.clearDB(ethTxHash);
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
                if (ethNewestHeight - po.getBlockHeight() < EthContext.getConfig().getTxBlockConfirmations()) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("交易[{}]确认高度等待: {}", po.getTxHash(), EthContext.getConfig().getTxBlockConfirmations() - (ethNewestHeight - po.getBlockHeight()));
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
                    case RECOVERY:
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

    private boolean checkBlockHeightTimes(EthUnconfirmedTxPo po) throws Exception {
        po.increaseBlockHeightTimes();
        if(po.getBlockHeightTimes() < 10) {
            return false;
        }
        po.setBlockHeightTimes(0);
        String txHash = po.getTxHash();
        if(StringUtils.isBlank(txHash)) {
            return false;
        }
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        if(tx == null) {
            return false;
        }
        BigInteger blockNumber = tx.getBlockNumber();
        // 本地最新的区块
        EthSimpleBlockHeader localMax = ethLocalBlockHelper.getLatestLocalBlockHeader();
        if(localMax == null) {
            return false;
        }
        if(localMax.getHeight().longValue() > blockNumber.longValue()) {
            return true;
        }
        return false;
    }

    private boolean dealDeposit(EthUnconfirmedTxPo po, EthUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String ethTxHash = po.getTxHash();
        EthUnconfirmedTxPo txPo = poFromDB;
        if(txPo == null) {
            txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        }
        if (txPo == null) {
            logger().warn("[充值任务异常] DB中未获取到PO，队列中PO: {}", po.toString());
            return !isReOfferQueue;
        }
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，从队列中移除
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // 补充po内存数据，po打印日志，方便查看数据
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //再次验证交易
            boolean validateTx = validateDepositTxConfirmedInEthNet(ethTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // 验证失败，从DB和队列中移除交易
                this.clearDB(ethTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        try {
            // 回调充值交易
            String nerveTxHash = ethCallBackManager.getDepositTxSubmitter().txSubmit(
                    ethTxHash,
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
                ethUnconfirmedTxStorageService.update(txPo, update -> update.setNerveTxHash(nerveTxHash));
                if (nerveTxHashNotBlank) {
                    ethStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (Exception e) {
            // 交易已存在，移除队列
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("Nerve交易已存在，从队列中移除待确认的ETH交易[{}]", ethTxHash);
                return !isReOfferQueue;
            }
            throw e;
        }
        return isReOfferQueue;
    }

    private boolean dealBroadcastTx(EthUnconfirmedTxPo po, EthUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String ethTxHash = po.getTxHash();
        EthUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        }
        if (txPo == null) {
            logger().warn("[提现任务异常] DB中未获取到PO，队列中PO: {}", po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，不放回队列
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                ethResendHelper.clear(nerveTxHash);
                logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
            case DOING:
                // 检查本地是否发起过此nerve提现交易，若没有，则发起。新加入的虚拟银行会出现这种情况，不过如果已经错过之前其他虚拟银行发起的doing的交易，则此方案无效
                checkCurrentNodeSendWithdraw(po.getTxType(), nerveTxHash);
                // 失败或者正在多签的交易不用处理，从队列和DB中移除
                logger().info("{} 正在多签的或者失败的交易不用处理，从队列和DB中移除[{}]", txPo.getStatus(), ethTxHash);
                isReOfferQueue = false;
                this.clearDB(ethTxHash);
                break;
            case RESEND:
                if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                    logger().warn("重发超过三十次，丢弃交易, 详情: {}", po.toString());
                    isReOfferQueue = false;
                    this.clearDB(ethTxHash);
                    break;
                }
                // 交易重发
                boolean success0 = this.reSend(txPo);
                if(success0) {
                    isReOfferQueue = false;
                    this.clearDB(ethTxHash);
                } else {
                    // 重发失败，等待一轮再次重发
                    po.setSkipTimes(1);
                    po.setResendTimes(po.getResendTimes() + 1);
                }
                break;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //再次验证交易
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInEthNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // 放回队列，再次验证
                            return isReOfferQueue;
                        case RE_SEND:
                            if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                                logger().warn("重发超过三十次，丢弃交易, 详情: {}", po.toString());
                                isReOfferQueue = false;
                                this.clearDB(ethTxHash);
                                break;
                            }
                            // 交易重发
                            boolean success1 = this.reSend(txPo);
                            if(success1) {
                                // 从DB和队列中移除当前交易
                                isReOfferQueue = false;
                                this.clearDB(ethTxHash);
                            } else {
                                // 重发失败，等待一轮再次重发
                                po.setSkipTimes(1);
                                po.setResendTimes(po.getResendTimes() + 1);
                            }
                            return isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == SUCCESS);
                }
                try {
                    String realNerveTxHash = nerveTxHash;
                    // 恢复机制
                    if (txPo.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                        realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
                        if (txPo.getNerveTxHash().startsWith(EthConstant.ETH_RECOVERY_I)) {
                            // 第一步恢复设置为已完成
                            if (!EthContext.getConverterCoreApi().isSeedVirtualBankByCurrentNode()) {
                                ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash);
                            }
                            // 第一步恢复执行完成后，种子虚拟银行执行第二步
                            if (EthContext.getConverterCoreApi().isSeedVirtualBankByCurrentNode()) {
                                logger().info("[{}]已完成恢复第一步的ETH交易[{}]调用恢复第二步", po.getTxType(), ethTxHash);
                                EthRecoveryDto recoveryDto = ethTxStorageService.findRecoveryByNerveTxKey(nerveTxHash);
                                if (recoveryDto == null) {
                                    logger().info("第二步恢复交易已提前发出");
                                    ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash);
                                    break;
                                }
                                String secondHash = ethRegister.getDockingImpl().forceRecovery(EthConstant.ETH_RECOVERY_II + realNerveTxHash, recoveryDto.getSeedManagers(), recoveryDto.getAllManagers());
                                if (StringUtils.isNotBlank(secondHash)) {
                                    logger().info("第二步恢复交易已发出，hash: {}", secondHash);
                                    ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash);
                                }
                            }
                            break;
                        }
                    }
                    logger().info("[{}]签名完成的ETH交易[{}]调用Nerve确认[{}]", po.getTxType(), ethTxHash, realNerveTxHash);
                    // 签名完成的交易将触发回调Nerve Core
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            ethTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            EthContext.MULTY_SIGN_ADDRESS,
                            txPo.getSigners());
                } catch (NulsException e) {
                    // 交易已存在，等待确认移除
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nerve交易[{}]已存在，从队列中移除待确认的ETH交易[{}]", txPo.getNerveTxHash(), ethTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    private boolean speedUpResendTransaction(HeterogeneousChainTxType txType, String nerveTxHash, EthUnconfirmedTxPo unconfirmedTxPo, EthSendTransactionPo txInfo) throws Exception {
        if (txInfo == null) {
            return false;
        }
        logger().info("检测到需要加速重发交易，类型: {}, ethHash: {}, nerveTxHash: {}", txType, unconfirmedTxPo.getTxHash(), nerveTxHash);
        // 向ETH网络请求验证
        boolean isCompleted = ethParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
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
        String currentFrom = EthContext.ADMIN_ADDRESS;
        BigInteger nonce = txInfo.getNonce();
        if (!currentFrom.equals(from)) {
            nonce = ethWalletApi.getNonce(currentFrom);
        }
        txInfo.setNonce(nonce);
        // 提高交易的price，获取当前ETH网络price，和旧的price取较大值，再+2，即 price = price + 2，最大当前price的1.1倍
        BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
        BigInteger maxCurrentGasPrice = new BigDecimal(currentGasPrice).multiply(EthConstant.NUMBER_1_DOT_1).toBigInteger();
        BigInteger oldGasPrice = txInfo.getGasPrice();
        if (maxCurrentGasPrice.compareTo(oldGasPrice) <= 0) {
            logger().info("当前交易的gasPrice已达到加速最大值，不再继续加速，等待ETH网络打包交易");
            return false;
        }
        BigInteger newGasPrice = oldGasPrice.compareTo(currentGasPrice) > 0 ? oldGasPrice : currentGasPrice;
        newGasPrice = newGasPrice.add(EthConstant.GWEI_2);
        newGasPrice = newGasPrice.compareTo(maxCurrentGasPrice) > 0 ? maxCurrentGasPrice : newGasPrice;

        txInfo.setGasPrice(newGasPrice);
        // 获取账户信息
        HeterogeneousAccount account = ethRegister.getDockingImpl().getAccount(currentFrom);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 验证业务数据
        String contractAddress = txInfo.getTo();
        String encodedFunction = txInfo.getData();
        EthCall ethCall = ethWalletApi.validateContractCall(currentFrom, contractAddress, encodedFunction);
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
        EthSendTransactionPo newTxPo = ethWalletApi.callContractRaw(priKey, txInfo);
        String ethTxHash = newTxPo.getTxHash();
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash, newTxPo);

        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        BeanUtils.copyProperties(unconfirmedTxPo, po);
        // 保存未确认交易
        po.setTxHash(ethTxHash);
        po.setFrom(currentFrom);
        po.setTxType(txType);
        po.setCreateDate(System.currentTimeMillis());
        ethUnconfirmedTxStorageService.save(po);
        EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        ethListener.addListeningTx(ethTxHash);
        logger().info("加速重发ETH网络交易成功, 类型: {}, 详情: {}", txType, po.superString());
        if (logger().isDebugEnabled()) {
            logger().debug("加速后: {}", newTxPo.toString());
        }
        return true;
    }

    private String sendOverrideTransferTx(String from, BigInteger gasPrice, BigInteger nonce) {
        try {
            // 检查发出交易的地址和当前虚拟银行地址是否一致，否则，忽略
            String currentFrom = EthContext.ADMIN_ADDRESS;
            if (!currentFrom.equals(from)) {
                logger().info("发出转账覆盖交易的地址和当前虚拟银行地址不一致，忽略");
                return null;
            }
            // 获取账户信息
            HeterogeneousAccount account = ethRegister.getDockingImpl().getAccount(currentFrom);
            account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // 获取网络gasprice
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            BigInteger newGasPrice = gasPrice.compareTo(currentGasPrice) > 0 ? gasPrice : currentGasPrice;
            newGasPrice = newGasPrice.add(EthConstant.GWEI_3);
            if (logger().isDebugEnabled()) {
                logger().debug("组装的覆盖交易数据, from: {}, to: {}, value: {}, gasLimit: {}, gasPrice: {}, nonce: {}",
                        currentFrom,
                        currentFrom,
                        BigInteger.ZERO,
                        EthConstant.ETH_GAS_LIMIT_OF_ETH,
                        newGasPrice,
                        nonce);
            }
            String hash = ethWalletApi.sendETHWithNonce(currentFrom, priKey, currentFrom, BigDecimal.ZERO, EthConstant.ETH_GAS_LIMIT_OF_ETH, newGasPrice, nonce);
            return hash;
        } catch (Exception e) {
            logger().warn("发生转账覆盖交易异常，忽略", e);
            return null;
        }

    }

    /**
     * 检查eth交易是否被打包
     */
    private boolean checkPacked(String ethTxHash) throws Exception {
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        return txReceipt != null;
    }

    /**
     * 检查本地是否发起过此nerve提现交易，若没有，则发起。新加入的虚拟银行会出现这种情况，不过如果已经错过之前其他虚拟银行发起的doing的交易，则此方案无效
     */
    private void checkCurrentNodeSendWithdraw(HeterogeneousChainTxType txType, String nerveTxHash) {
        if (txType != HeterogeneousChainTxType.WITHDRAW) {
            return;
        }
        try {
            boolean existNerveTxHash = ethTxRelationStorageService.existNerveTxHash(nerveTxHash);
            if (!existNerveTxHash) {
                HeterogeneousWithdrawTxInfo withdrawTxInfo = EthContext.getConverterCoreApi().getWithdrawTxInfo(nerveTxHash);
                if (withdrawTxInfo != null && withdrawTxInfo.getHeterogeneousChainId() == EthContext.getConfig().getChainId()) {
                    logger().info("检测到当前虚拟银行节点未发出此[提现]交易，验证无误后将发起[提现]交易, 提现数据: {}", withdrawTxInfo.toString());
                    EthDocking docking = (EthDocking) ethRegister.getDockingImpl();
                    docking.createOrSignWithdrawTx(
                            nerveTxHash,
                            withdrawTxInfo.getToAddress(),
                            withdrawTxInfo.getValue(),
                            withdrawTxInfo.getAssetId(), false);
                }
            }
        } catch (Exception e) {
            logger().warn(String.format("当前虚拟银行节点首次发交易失败, 类型: %s, nerveTxHash: %s", txType, nerveTxHash), e);
        }
    }

    /**
     * 验证充值交易
     */
    private boolean validateDepositTxConfirmedInEthNet(String ethTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            TransactionReceipt receipt = ethWalletApi.getTxReceipt(ethTxHash);
            if (receipt == null) {
                logger().error("再次验证交易[{}]失败，获取不到receipt", ethTxHash);
                break;
            }
            if (!receipt.isStatusOK()) {
                logger().error("再次验证交易[{}]失败，receipt状态不正确", ethTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogs() == null || receipt.getLogs().size() == 0)) {
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", ethTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * 交易重发
     */
    private boolean reSend(EthUnconfirmedTxPo po) throws NulsException {
        boolean success;
        try {
            logger().info("[{}]交易[{}]重发, 详情: {}", po.getTxType(), po.getTxHash(), po.toString());
            EthDocking docking = (EthDocking) ethRegister.getDockingImpl();
            switch (po.getTxType()) {
                case WITHDRAW:
                    String ethWithdrawHash = docking.createOrSignWithdrawTx(po.getNerveTxHash(), po.getTo(), po.getValue(), po.getAssetId(), false);
                    if(StringUtils.isBlank(ethWithdrawHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                //case RECOVERY:
                case CHANGE:
                    String ethChangesHash = docking.createOrSignManagerChangesTx(po.getNerveTxHash(), po.getAddAddresses(), po.getRemoveAddresses(), po.getOrginTxCount());
                    if(StringUtils.isBlank(ethChangesHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                case UPGRADE:
                    String ethUpgradeHash = docking.createOrSignUpgradeTx(po.getNerveTxHash());
                    if(StringUtils.isBlank(ethUpgradeHash)) {
                        logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                default:
                    break;
            }
            success = true;
        } catch (Exception e) {
            logger().error("交易重发失败，等待再次重发交易", e);
            success = false;
        }
        return success;
    }

    /**
     * 验证发到ETH网络的交易是否确认，若有异常情况，则根据条件重发交易
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInEthNet(EthUnconfirmedTxPo po) throws Exception {

        BroadcastTxValidateStatus status;
        String ethTxHash = po.getTxHash();
        do {
            TransactionReceipt receipt = ethWalletApi.getTxReceipt(ethTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > EthConstant.MINUTES_20;
                /*
                不会出现重复提现的情况，由于这是调用合约提现函数，合约业务内保证只被提现一次
                if(po.getTxType() == WITHDRAW) {
                    // 若已超时，则每15轮验证一次，否则，每3轮验证一次
                    int skipTimes = timeOut ? 15 : 3;
                    po.setSkipTimes(skipTimes);
                    status = BroadcastTxValidateStatus.RE_VALIDATE;
                    break;
                }
                */
                logger().error("再次验证交易[{}]失败，获取不到receipt", ethTxHash);
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
                logger().error("再次验证交易[{}]失败，receipt状态不正确", ethTxHash);
                break;
            } else if (receipt.getLogs() == null || receipt.getLogs().size() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", ethTxHash);
                break;
            }
            status = SUCCESS;
        } while (false);
        return status;
    }

    private void clearDB(String ethTxHash) throws Exception {
        if(StringUtils.isBlank(ethTxHash)) {
            return;
        }
        ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
        ethTxRelationStorageService.deleteByTxHash(ethTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return EthContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
