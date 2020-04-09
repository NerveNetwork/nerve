/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.syncblock;

import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import nerve.network.converter.heterogeneouschain.eth.constant.EthConstant;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus;
import nerve.network.converter.heterogeneouschain.eth.helper.EthStorageHelper;
import nerve.network.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import nerve.network.converter.heterogeneouschain.eth.register.EthRegister;
import nerve.network.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import nerve.network.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.concurrent.LinkedBlockingDeque;

import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;
import static nerve.network.converter.heterogeneouschain.eth.enums.BroadcastTxValidateStatus.SUCCESS;

/**
 * @author: Chino
 * @date: 2020-03-02
 */
@Component("ethConfirmTxScheduled")
public class EthConfirmTxScheduled implements Runnable {

    private ErrorCode TX_ALREADY_EXISTS = ErrorCode.init(ModuleE.TX.getPrefix() + "_0013");
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

    public void run() {
        if (!ethCallBackManager.getTxConfirmedProcessor().isVirtualBankByCurrentNode()) {
            logger().info("Not a virtual bank, skipping `EthConfirmTxScheduled`");
            return;
        }
        logger().info("ethConfirmTxScheduled - 每隔20秒执行一次。");
        LinkedBlockingDeque<EthUnconfirmedTxPo> queue = EthContext.UNCONFIRMED_TX_QUEUE;
        EthUnconfirmedTxPo po = null;
        try {
            EthContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.await();
            long ethNewestHeight = ethWalletApi.getBlockHeight();
            int size = EthContext.UNCONFIRMED_TX_QUEUE.size();
            for (int i = 0; i < size; i++) {
                po = EthContext.UNCONFIRMED_TX_QUEUE.poll();
                if (po == null) {
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
                    logger().info("区块高度为空，放回队列等待下次处理，详情: {}", po.toString());
                    queue.offer(po);
                    continue;
                }

                // 交易触发重新验证后，等待`skipTimes`的轮次，再做验证
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    logger().debug("剩余等待轮次: {}", po.getSkipTimes());
                    continue;
                }
                // 未达到确认高度，放回队列中，下次继续检查
                if (ethNewestHeight - po.getBlockHeight() < EthContext.getConfig().getTxBlockConfirmations()) {
                    logger().debug("交易[{}]确认高度等待: {}", po.getTxHash(), EthContext.getConfig().getTxBlockConfirmations() - (ethNewestHeight - po.getBlockHeight()));
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            queue.offer(po);
                        }
                        break;
                    default:
                        logger().error("unkown tx: {}", po.toString());
                        ethUnconfirmedTxStorageService.deleteByTxHash(po.getTxHash());
                        ethTxRelationStorageService.deleteByTxHash(po.getTxHash());
                        break;
                }
            }
        } catch (Exception e) {
            logger().error("confirming error [{}]", e);
            if (po != null) {
                queue.offer(po);
            }
        }
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
                ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                isReOfferQueue = false;
                logger().debug("[{}]交易[{}]确认完成, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            return isReOfferQueue;
        }
        boolean changeData = false;
        if (!po.isValidateTx()) {
            //再次验证交易
            boolean validateTx = validateDepositTxConfirmedInEthNet(ethTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // 验证失败，从DB和队列中移除交易
                ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                ethTxRelationStorageService.deleteByTxHash(ethTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
            txPo.setValidateTx(po.isValidateTx());
            changeData = true;
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
            if (changeData || nerveTxHashNotBlank) {
                ethUnconfirmedTxStorageService.save(txPo);
                if (nerveTxHashNotBlank) {
                    ethStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (NulsException e) {
            // 交易已存在，等待确认移除
            if (TX_ALREADY_EXISTS.equals(e.getErrorCode())) {
                po.setSkipTimes(1);
                return isReOfferQueue;
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
                ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                isReOfferQueue = false;
                logger().debug("[{}]交易[{}]确认完成, nerve高度: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            return isReOfferQueue;
        }

        switch (txPo.getStatus()) {
            case INITIAL:
                break;
            case FAILED:
            case DOING:
                // 失败或者正在多签的交易不用处理，从队列和DB中移除
                isReOfferQueue = false;
                ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                ethTxRelationStorageService.deleteByTxHash(ethTxHash);
                break;
            case COMPLETED:
                if (!txPo.isValidateTx()) {
                    //再次验证交易
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInEthNet(txPo);
                    switch (validate) {
                        case FAILED:
                            // 验证失败，从DB和队列中移除当前交易
                            ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                            ethTxRelationStorageService.deleteByTxHash(ethTxHash);
                            return !isReOfferQueue;
                        case RE_VALIDATE:
                            // 放回队列，再次验证
                            return isReOfferQueue;
                        case RE_SEND:
                            // 交易重发
                            this.reSend(txPo);
                            // 从DB和队列中移除当前交易
                            ethUnconfirmedTxStorageService.deleteByTxHash(ethTxHash);
                            ethTxRelationStorageService.deleteByTxHash(ethTxHash);
                            return !isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == SUCCESS);
                    txPo.setValidateTx(po.isValidateTx());
                    // 当未确认交易数据产生变化时，更新DB数据
                    if (txPo.isValidateTx()) {
                        ethUnconfirmedTxStorageService.save(txPo);
                    }
                }
                try {
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
                    if (TX_ALREADY_EXISTS.equals(e.getErrorCode())) {
                        po.setSkipTimes(1);
                        return isReOfferQueue;
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
    private void reSend(EthUnconfirmedTxPo po) throws NulsException {
        switch (po.getTxType()) {
            case WITHDRAW:
                ethRegister.getDockingImpl().createOrSignWithdrawTx(po.getNerveTxHash(), po.getTo(), po.getValue(), po.getAssetId());
                break;
            case CHANGE:
                ethRegister.getDockingImpl().createOrSignManagerChangesTx(po.getNerveTxHash(), po.getAddAddresses(), po.getRemoveAddresses(), po.getCurrentAddresses());
                break;
            default:
                break;
        }
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

    private long getCurrentBlockHeightOnNerve() {
        return ethCallBackManager.getTxConfirmedProcessor().getCurrentBlockHeightOnNerve();
    }
}
