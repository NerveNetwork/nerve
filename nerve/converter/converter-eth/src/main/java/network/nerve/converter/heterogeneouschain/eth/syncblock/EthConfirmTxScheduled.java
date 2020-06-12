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

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthResendHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthStorageHelper;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.register.EthRegister;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus.SUCCESS;

/**
 * @author: Mimi
 * @date: 2020-03-02
 */
@Component("ethConfirmTxScheduled")
public class EthConfirmTxScheduled implements Runnable {

    private ErrorCode TX_ALREADY_EXISTS_0 = ErrorCode.init(ModuleE.TX.getPrefix() + "_0013");
    private ErrorCode TX_ALREADY_EXISTS_1 = ErrorCode.init(ModuleE.CV.getPrefix() + "_0040");
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthRegister ethRegister;
    @Autowired
    private EthStorageHelper ethStorageHelper;
    @Autowired
    private EthResendHelper ethResendHelper;
    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;

    public void run() {
        if (!EthContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            EthContext.logger().info("非虚拟银行成员，跳过此任务");
            return;
        }
        EthContext.logger().info("[ETH交易确认任务] - 每隔20秒执行一次。");
        LinkedBlockingDeque<EthUnconfirmedTxPo> queue = EthContext.UNCONFIRMED_TX_QUEUE;
        EthUnconfirmedTxPo po = null;
        try {
            ethWalletApi.checkApi(EthContext.getConverterCoreApi().getVirtualBankOrder());
            EthContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.await();
            long ethNewestHeight = ethWalletApi.getBlockHeight();
            int size = EthContext.UNCONFIRMED_TX_QUEUE.size();
            for (int i = 0; i < size; i++) {
                po = EthContext.UNCONFIRMED_TX_QUEUE.poll();
                if (po == null || po.getTxType() == null) {
                    this.clearDB(po.getTxHash());
                    if(EthContext.logger().isDebugEnabled()) {
                        EthContext.logger().debug("空值PO，详情: {}", po.toString());
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
                        EthContext.logger().info("区块高度为空，此交易已处理过，移除此交易，详情: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    if(EthContext.logger().isDebugEnabled()) {
                        EthContext.logger().debug("区块高度为空，放回队列等待下次处理，详情: {}", po.toString());
                    }
                    queue.offer(po);
                    continue;
                }

                // 交易触发重新验证后，等待`skipTimes`的轮次，再做验证
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    if(EthContext.logger().isDebugEnabled()) {
                        EthContext.logger().debug("交易触发重新验证，剩余等待再次验证的轮次数量: {}", po.getSkipTimes());
                    }
                    continue;
                }
                // 未达到确认高度，放回队列中，下次继续检查
                if (ethNewestHeight - po.getBlockHeight() < EthContext.getConfig().getTxBlockConfirmations()) {
                    if(EthContext.logger().isDebugEnabled()) {
                        EthContext.logger().debug("交易[{}]确认高度等待: {}", po.getTxHash(), EthContext.getConfig().getTxBlockConfirmations() - (ethNewestHeight - po.getBlockHeight()));
                    }
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            if(EthContext.logger().isDebugEnabled()) {
                                EthContext.logger().debug("充值交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                    case UPGRADE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            if(EthContext.logger().isDebugEnabled()) {
                                EthContext.logger().debug("广播交易重新放回队列, 详情: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    default:
                        EthContext.logger().error("unkown tx: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        break;
                }
            }
        } catch (Exception e) {
            EthContext.logger().error("confirming error", e);
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
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，从队列中移除
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                EthContext.logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
            if (e instanceof NulsException && TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())) {
                EthContext.logger().info("Nerve交易已存在，从队列中移除待确认的ETH交易[{}]", ethTxHash);
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
        if(txPo == null) {
            txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        }
        // 当状态为移除，不再回调Nerve核心，放回队列中，等待达到移除高度后，从DB中删除，不放回队列
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                ethResendHelper.clear(txPo.getNerveTxHash());
                EthContext.logger().info("[{}]交易[{}]已确认超过{}个高度, 移除队列, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
                // 失败或者正在多签的交易不用处理，从队列和DB中移除
                EthContext.logger().info("{} 正在多签的或者失败的交易不用处理，从队列和DB中移除[{}]", txPo.getStatus(), ethTxHash);
                isReOfferQueue = false;
                this.clearDB(ethTxHash);
                break;
            case RESEND:
                if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                    EthContext.logger().warn("重发超过三十次，丢弃交易, 详情: {}", po.toString());
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
                        case FAILED:
                            // 验证失败，从DB和队列中移除当前交易
                            this.clearDB(ethTxHash);
                            return !isReOfferQueue;
                        case RE_VALIDATE:
                            // 放回队列，再次验证
                            return isReOfferQueue;
                        case RE_SEND:
                            if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                                EthContext.logger().warn("重发超过三十次，丢弃交易, 详情: {}", po.toString());
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
                    EthContext.logger().info("[{}]签名完成的ETH交易[{}]调用Nerve确认[{}]", po.getTxType(), ethTxHash, txPo.getNerveTxHash());
                    // 签名完成的交易将触发回调Nerve Core
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            txPo.getNerveTxHash(),
                            ethTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            EthContext.MULTY_SIGN_ADDRESS,
                            txPo.getSigners());
                } catch (NulsException e) {
                    // 交易已存在，等待确认移除
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        EthContext.logger().info("Nerve交易[{}]已存在，从队列中移除待确认的ETH交易[{}]", txPo.getNerveTxHash(), ethTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    /**
     * 验证充值交易
     */
    private boolean validateDepositTxConfirmedInEthNet(String ethTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            TransactionReceipt receipt = ethWalletApi.getTxReceipt(ethTxHash);
            if (receipt == null) {
                EthContext.logger().error("再次验证交易[{}]失败，获取不到receipt", ethTxHash);
                break;
            }
            if (!receipt.isStatusOK()) {
                EthContext.logger().error("再次验证交易[{}]失败，receipt状态不正确", ethTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogs() == null || receipt.getLogs().size() == 0)) {
                EthContext.logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", ethTxHash);
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
            EthContext.logger().info("[{}]交易[{}]重发, 详情: {}", po.getTxType(), po.getTxHash(), po.toString());
            switch (po.getTxType()) {
                case WITHDRAW:
                    String ethWithdrawHash = ethRegister.getDockingImpl().createOrSignWithdrawTx(po.getNerveTxHash(), po.getTo(), po.getValue(), po.getAssetId());
                    if(StringUtils.isBlank(ethWithdrawHash)) {
                        EthContext.logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                case CHANGE:
                    String ethChangesHash = ethRegister.getDockingImpl().createOrSignManagerChangesTx(po.getNerveTxHash(), po.getAddAddresses(), po.getRemoveAddresses(), po.getCurrentAddresses());
                    if(StringUtils.isBlank(ethChangesHash)) {
                        EthContext.logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                case UPGRADE:
                    String ethUpgradeHash = ethRegister.getDockingImpl().createOrSignUpgradeTx(po.getNerveTxHash());
                    if(StringUtils.isBlank(ethUpgradeHash)) {
                        EthContext.logger().info("Nerve交易[{}]已完成，无需重发", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                default:
                    break;
            }
            success = true;
        } catch (Exception e) {
            EthContext.logger().error("交易重发失败，等待再次重发交易", e);
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
                EthContext.logger().error("再次验证交易[{}]失败，获取不到receipt", ethTxHash);
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
                EthContext.logger().error("再次验证交易[{}]失败，receipt状态不正确", ethTxHash);
                break;
            } else if (receipt.getLogs() == null || receipt.getLogs().size() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                EthContext.logger().error("再次验证交易[{}]失败，receipt.Log状态不正确", ethTxHash);
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
