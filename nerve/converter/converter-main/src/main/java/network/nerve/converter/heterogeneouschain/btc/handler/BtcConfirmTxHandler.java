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
package network.nerve.converter.heterogeneouschain.btc.handler;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.btc.core.BtcWalletApi;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcParseTxHelper;
import network.nerve.converter.heterogeneouschain.btc.utils.BtcUtil;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.utils.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.constant.ConverterErrorCode.TX_DUPLICATION;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.*;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class BtcConfirmTxHandler implements Runnable, BeanInitial {

    private BtcWalletApi htgWalletApi;
    private ConverterConfig converterConfig;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgTxRelationStorageService htgTxRelationStorageService;
    private HtgTxStorageService htgTxStorageService;
    private HtgCallBackManager htgCallBackManager;
    private HtgStorageHelper htgStorageHelper;
    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgListener htgListener;
    private BtcParseTxHelper htgParseTxHelper;
    private HtgResendHelper htgResendHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private BtcContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void run() {
        HtgUnconfirmedTxPo po = null;
        LinkedBlockingDeque<HtgUnconfirmedTxPo> queue = htgContext.UNCONFIRMED_TX_QUEUE();
        try {
            String symbol = htgContext.getConfig().getSymbol();
            if (!htgContext.getConverterCoreApi().isRunning()) {
                LoggerUtil.LOG.debug("[{}]Ignoring synchronous block mode", symbol);
                return;
            }
            if (!htgContext.getConverterCoreApi().checkNetworkRunning(htgContext.HTG_CHAIN_ID())) {
                htgContext.logger().info("Test network [{} ]Run Pause, chainId: {}", htgContext.getConfig().getSymbol(), htgContext.HTG_CHAIN_ID());
                return;
            }
            if (!htgContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
                LoggerUtil.LOG.debug("[{}] Non virtual bank member, skip this task", symbol);
                return;
            }
            if (!htgContext.isAvailableRPC()) {
                htgContext.logger().error("[{}] network RPC Unavailable, pause this task", symbol);
                return;
            }


            try {
                htgWalletApi.checkApi();
                BigInteger currentGasPrice = BigInteger.valueOf(htgWalletApi.getFeeRate());
                if (currentGasPrice != null) {
                    htgContext.logger().info("current {} Network based feeRate: {} sat/vB.", symbol, currentGasPrice);
                    htgContext.setEthGasPrice(currentGasPrice);
                }
            } catch (Exception e) {
                htgContext.logger().error(String.format("inspect %s current API fail", symbol), e);
            }
            htgContext.logger().debug("[{} Transaction confirmation task] - every other {} Execute once per second.", symbol, htgContext.getConfig().getConfirmTxQueuePeriod());
            // Persistent unconfirmed transactions loaded while waiting for application restart
            htgContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH().await();
            int size = htgContext.UNCONFIRMED_TX_QUEUE().size();
            if (size == 0) {
                return;
            }
            long bestBlockHeight = htgWalletApi.getBestBlockHeight();
            for (int i = 0; i < size; i++) {
                po = htgContext.UNCONFIRMED_TX_QUEUE().poll();
                if (po == null) {
                    logger().info("Remove null valuesPO");
                    continue;
                }
                // When the recharge confirmation task is abnormal and exceeds the number of retries, discard the task
                if (po.isDepositExceedErrorTime(RESEND_TIME)) {
                    logger().error("[{}] Confirm that the task exception exceeds the number of retries. Remove this transaction. Details: {}", symbol, po.toString());
                    this.clearDB(po.getTxHash());
                    continue;
                }
                HtgUnconfirmedTxPo poFromDB = null;
                if (po.getBlockHeight() == null) {
                    poFromDB = htgUnconfirmedTxStorageService.findByTxHash(po.getTxHash());
                    if (poFromDB != null) {
                        po.setBlockHeight(poFromDB.getBlockHeight());
                        po.setTxTime(poFromDB.getTxTime());
                    }
                }
                if (po.getBlockHeight() == null) {
                    queue.offer(po);
                    continue;
                }

                // Wait for transaction triggering revalidation`skipTimes`The round will be verified again
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    if(logger().isDebugEnabled()) {
                        logger().debug("[{}] Transaction triggered revalidation, remaining number of rounds waiting for revalidation: {}", symbol, po.getSkipTimes());
                    }
                    continue;
                }
                // Not reaching the confirmed height, put it back in the queue and continue checking next time
                int confirmation = getConfirmation(po.getTxType());
                if (bestBlockHeight - po.getBlockHeight() < confirmation) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("[{}] transaction [{}] Confirm altitude waiting: {}", symbol, po.getTxHash(), confirmation - (bestBlockHeight - po.getBlockHeight()));
                    }
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("[{}] Recharge transactions are placed back in the queue, details: {}", symbol, poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW_FEE_RECHARGE:
                    case WITHDRAW_FEE_USED:
                        if (dealWithdrawFeeTx(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("[{}] Withdraw fee transactions are put back into the queue, details: {}", symbol, poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                    case UPGRADE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("[{}] Broadcast transactions are put back into the queue, details: {}", symbol, poFromDB != null ? poFromDB.toString() : po.toString());
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

    private int getConfirmation(HeterogeneousChainTxType txType) {
        int confirmation = htgContext.getConfig().getTxBlockConfirmations();
        if (txType == HeterogeneousChainTxType.WITHDRAW
            || txType == HeterogeneousChainTxType.WITHDRAW_FEE_RECHARGE
            || txType == HeterogeneousChainTxType.WITHDRAW_FEE_USED
        ) {
            confirmation = htgContext.getConfig().getTxBlockConfirmationsOfWithdraw();
        }
        return confirmation;
    }

    private boolean dealDeposit(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        if (!htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
            logger().warn("[{}][Abnormal recharge address] transaction[{}], Remove queue, [1]Recharge address: {}", symbol, htgTxHash, po.getNerveAddress());
            this.clearDB(htgTxHash);
            return !isReOfferQueue;
        }
        HtgUnconfirmedTxPo txPo = poFromDB;
        if(txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}][Recharge task exception] DBNot obtained inPOIn the queuePO: {}", symbol, po.toString());
            return !isReOfferQueue;
        }
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBRemove from queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                logger().info("[{}][{}]transaction[{}]Confirmed exceeding{}Height, Remove queue, nerveheight: {}, nerver hash: {}", symbol, po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // supplementpoMemory data,poPrint logs for easy viewing of data
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //Verify transaction again
            boolean validateTx = validateDepositTxConfirmedInBtcNet(htgTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // Verification failed, fromDBRemove transactions from the queue
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        try {
            Long height = po.getBlockHeight();
            long txTime = po.getTxTime();
            // Callback recharge transaction
            String nerveTxHash;
            do {
                ConverterConfig converterConfig = htgContext.getConverterCoreApi().getConverterConfig();
                byte[] withdrawalBlackhole = AddressTool.getAddressByPubKeyStr(converterConfig.getBlackHolePublicKey(), converterConfig.getChainId());
                byte[] feeAddress = AddressTool.getAddressByPubKeyStr(converterConfig.getFeePubkey(), converterConfig.getChainId());
                // RechargeablenerveThe receiving address cannot be a black hole or a fee subsidy address
                if (Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole) || Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), feeAddress)) {
                    logger().error("[{}][Abnormal recharge address][Black hole or subsidy address for handling fees]Deposit Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
                    // Verification failed, fromDBRemove transactions from the queue
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                BtcUnconfirmedTxPo fchPo = (BtcUnconfirmedTxPo) po;
                nerveTxHash = htgCallBackManager.getDepositTxSubmitter().depositTxSubmitOfBtcSys(
                        htgTxHash,
                        height,
                        po.getFrom(),
                        po.getNerveAddress(),
                        po.getValue(),
                        txTime,
                        fchPo.getFee(),
                        fchPo.getNerveFeeTo(), fchPo.getExtend0());
            } while (false);
            // Update when there is a change in unconfirmed transaction dataDBdata
            boolean nerveTxHashNotBlank = StringUtils.isNotBlank(nerveTxHash);
            if (nerveTxHashNotBlank) {
                po.setNerveTxHash(nerveTxHash);
                txPo.setNerveTxHash(nerveTxHash);
                String updateHash = nerveTxHash;
                htgUnconfirmedTxStorageService.update(txPo, update -> update.setNerveTxHash(updateHash));
                if (nerveTxHashNotBlank) {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (Exception e) {
            // Transaction already exists, remove queue
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("NerveTransaction already exists, remove pending confirmation from queue{}transaction[{}]", htgContext.getConfig().getSymbol(), htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.increaseDepositErrorTime();
            throw e;
        }
        return isReOfferQueue;
    }

    /**
     * Verify recharge transactions
     */
    private boolean validateDepositTxConfirmedInBtcNet(String htgTxHash, boolean ifContractAsset) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean validateTx = true;
        do {
            RawTransaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
            if (htgTx.getConfirmations() == null || htgTx.getConfirmations().intValue() == 0) {
                validateTx = false;
                logger().error("[{}] Verify transaction [{}] Failed, tx not confirmed yet", symbol, htgTxHash);
                break;
            }
        } while (false);
        return validateTx;
    }

    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInBtcNet(HtgUnconfirmedTxPo po) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        BroadcastTxValidateStatus status;
        String htgTxHash = po.getTxHash();
        do {
            RawTransaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
            if (htgTx.getConfirmations() == null || htgTx.getConfirmations().intValue() == 0) {
                po.setSkipTimes(3);
                status = BroadcastTxValidateStatus.RE_VALIDATE;
                logger().error("[{}] Verify transaction [{}] Failed, tx not confirmed yet", symbol, htgTxHash);
                break;
            }
            status = BroadcastTxValidateStatus.SUCCESS;
        } while (false);
        return status;
    }

    private boolean dealBroadcastTx(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        HtgUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}] [{} Task exception] DB Not obtained in PO In the queue PO: {}", symbol, po.getTxType(), po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // query nerve Corresponding to the transaction eth Whether the transaction was successful
        if (htgInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
            logger().info("[{}] Nerve tx stay NERVE Network confirmed, Successfully removed queue, nerveHash: {}", symbol, nerveTxHash);
            this.clearDB(htgTxHash);
            return !isReOfferQueue;
        }
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBDelete in, do not put back in queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                htgResendHelper.clear(nerveTxHash);
                logger().info("[{}] [{}] transaction [{}] Confirmed exceeding {} Height, Remove queue, nerve height: {}, nerver hash: {}", symbol, po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // supplementpoMemory data,poPrint logs for easy viewing of data
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }

        switch (txPo.getStatus()) {
            case INITIAL:
                break;
            case FAILED:
                // Failed transactions, not handled by the current node, from the queue and DB Remove from middle
                logger().info("Failed {} transaction [{}] The current node is not in the next order and will not be processed by the current node. Remove the queue", symbol, htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //Verify transaction again
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInBtcNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // Put it back in the queue and verify again
                            return isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == BroadcastTxValidateStatus.SUCCESS);
                }
                try {
                    BtcUnconfirmedTxPo btcPo = (BtcUnconfirmedTxPo) txPo;
                    String realNerveTxHash = nerveTxHash;
                    logger().info("[{}] Signed {} transaction [{}] call Nerve confirm [{}]", po.getTxType(), symbol, htgTxHash, realNerveTxHash);
                    // The signed transaction will trigger a callbackNerve Core
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            htgTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            htgContext.MULTY_SIGN_ADDRESS(),
                            txPo.getSigners(),
                            btcPo.getCheckWithdrawalUsedUTXOData());
                } catch (NulsException e) {
                    // Transaction already exists, waiting for confirmation to remove
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nerve transaction [{}] Exists, remove pending confirmation from queue {} transaction [{}]", txPo.getNerveTxHash(), symbol, htgTxHash);
                        this.clearDB(htgTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    private boolean dealWithdrawFeeTx(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        String realHash = BtcUtil.removeTxHashPrefix(htgTxHash);
        HtgUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}] [{} Task exception] DB Not obtained in PO In the queue PO: {}", symbol, po.getTxType(), po.toString());
            return !isReOfferQueue;
        }
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBDelete in, do not put back in queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                logger().info("[{}] [{}] transaction [{}] Confirmed exceeding {} Height, Remove queue, nerve height: {}, nerver hash: {}", symbol, po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // supplementpoMemory data,poPrint logs for easy viewing of data
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }

        switch (txPo.getStatus()) {
            case INITIAL:
                break;
            case FAILED:
                // Failed transactions, not handled by the current node, from the queue and DB Remove from middle
                logger().info("Failed {} transaction [{}] The current node is not in the next order and will not be processed by the current node. Remove the queue", symbol, htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //Verify transaction again
                    po.setTxHash(realHash);
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInBtcNet(po);
                    po.setTxHash(htgTxHash);
                    switch (validate) {
                        case RE_VALIDATE:
                            // Put it back in the queue and verify again
                            return isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == BroadcastTxValidateStatus.SUCCESS);
                }
                try {
                    BtcUnconfirmedTxPo btcPo = (BtcUnconfirmedTxPo) txPo;
                    logger().info("[{}] {}, Withdraw fee transaction [{}] call Nerve tx", po.getTxType(), symbol, htgTxHash);
                    // The signed transaction will trigger a callbackNerve Core
                    htgCallBackManager.getTxConfirmedProcessor().txRecordWithdrawFee(
                            po.getTxType(),
                            realHash,
                            btcPo.getBlockHash(),
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            txPo.getValue().longValue(),
                            String.format("Record withdraw fee [%s], chainId: %s, amount: %s, hash: %s, blockHeight: %s, blockHash: %s",
                                    po.getTxType().name().replace("WITHDRAW_FEE_", ""),
                                    htgContext.HTG_CHAIN_ID(),
                                    txPo.getValue().longValue(),
                                    realHash,
                                    txPo.getBlockHeight(),
                                    btcPo.getBlockHash()).getBytes(StandardCharsets.UTF_8));
                } catch (NulsException e) {
                    // Transaction already exists, waiting for confirmation to remove
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode())
                            || TX_DUPLICATION.equals(e.getErrorCode())
                            || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nerve transaction [{}] Exists, remove pending confirmation from queue {} transaction [{}]", txPo.getNerveTxHash(), symbol, htgTxHash);
                        this.clearDB(htgTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    private void clearDB(String htgTxHash) throws Exception {
        if(StringUtils.isBlank(htgTxHash)) {
            return;
        }
        htgUnconfirmedTxStorageService.deleteByTxHash(htgTxHash);
        htgTxRelationStorageService.deleteByTxHash(htgTxHash);
    }

    private long getCurrentBlockHeightOnNerve() {
        return htgContext.getConverterCoreApi().getCurrentBlockHeightOnNerve();
    }
}
