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
package network.nerve.converter.heterogeneouschain.trx.handler;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxParseTxHelper;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousAddFeeCrossChainData;
import network.nerve.converter.model.bo.HeterogeneousOneClickCrossChainData;
import network.nerve.converter.utils.LoggerUtil;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.*;
import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.SUN_PER_ENERGY_BASE;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class TrxConfirmTxHandler implements Runnable, BeanInitial {

    private TrxWalletApi htgWalletApi;
    private TrxParseTxHelper htgParseTxHelper;
    private HtgCallBackManager htgCallBackManager;
    private ConverterConfig converterConfig;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgTxRelationStorageService htgTxRelationStorageService;
    private HtgTxStorageService htgTxStorageService;
    private HtgStorageHelper htgStorageHelper;
    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgListener htgListener;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgResendHelper htgResendHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public void run() {
        HtgUnconfirmedTxPo po = null;
        LinkedBlockingDeque<HtgUnconfirmedTxPo> queue = htgContext.UNCONFIRMED_TX_QUEUE();
        try {
            if (!htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) return;
            if (!htgContext.getConverterCoreApi().isRunning()) {
                LoggerUtil.LOG.debug("[{}]Ignoring synchronous block mode", htgContext.getConfig().getSymbol());
                return;
            }
            try {
                BigInteger energyFee = htgWalletApi.getCurrentGasPrice();
                htgContext.logger().debug("current{}Network basedEnergyFee: {}.", htgContext.getConfig().getSymbol(), energyFee);
                TrxContext trxContext = (TrxContext) htgContext;
                if (energyFee != null && trxContext.SUN_PER_ENERGY.intValue() != energyFee.intValue()) {
                    trxContext.FEE_LIMIT_OF_WITHDRAW = TrxConstant.FEE_LIMIT_OF_WITHDRAW_BASE.multiply(energyFee).divide(SUN_PER_ENERGY_BASE);
                    trxContext.FEE_LIMIT_OF_CHANGE = TrxConstant.FEE_LIMIT_OF_CHANGE_BASE.multiply(energyFee).divide(SUN_PER_ENERGY_BASE);
                    trxContext.SUN_PER_ENERGY = energyFee;
                    trxContext.gasInfo = null;
                    htgContext.logger().info("Update current{}Network basedEnergyFee: {}, FEE_LIMIT_OF_WITHDRAW: {}, FEE_LIMIT_OF_CHANGE: {}.", htgContext.getConfig().getSymbol(), energyFee, trxContext.FEE_LIMIT_OF_WITHDRAW, trxContext.FEE_LIMIT_OF_CHANGE);
                }
            } catch (Exception e) {
                htgContext.logger().error(String.format("synchronization%scurrentEnergyFeefail", htgContext.getConfig().getSymbol()), e);
            }
            if (!htgContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
                LoggerUtil.LOG.debug("[{}]Non virtual bank member, skip this task", htgContext.getConfig().getSymbol());
                return;
            }
            if (!htgContext.isAvailableRPC()) {
                htgContext.logger().error("[{}]networkRPCUnavailable, pause this task", htgContext.getConfig().getSymbol());
                return;
            }
            htgWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
            LoggerUtil.LOG.debug("[{}Transaction confirmation task] - every other{}Execute once per second.", htgContext.getConfig().getSymbol(), htgContext.getConfig().getConfirmTxQueuePeriod());
            // Persistent unconfirmed transactions loaded while waiting for application restart
            htgContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH().await();
            long ethNewestHeight = htgWalletApi.getBlockHeight();
            int size = htgContext.UNCONFIRMED_TX_QUEUE().size();
            for (int i = 0; i < size; i++) {
                po = htgContext.UNCONFIRMED_TX_QUEUE().poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("Remove null valuesPO");
                    }
                    continue;
                }
                // Clean up useless change tasks
                if (po.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                    clearUnusedChange();
                    break;
                }
                // When the recharge confirmation task is abnormal and exceeds the number of retries, discard the task
                if (po.isDepositExceedErrorTime(RESEND_TIME)) {
                    logger().error("The recharge confirmation task has exceeded the number of retries. Please remove this transaction. Details: {}", po.toString());
                    this.clearDB(po.getTxHash());
                    continue;
                }
                HtgUnconfirmedTxPo poFromDB = null;
                if (po.getBlockHeight() == null) {
                    poFromDB = htgUnconfirmedTxStorageService.findByTxHash(po.getTxHash());
                    if (poFromDB != null) {
                        po.setBlockHeight(poFromDB.getBlockHeight());
                    }
                }
                if (po.getBlockHeight() == null) {
                    // Block height is empty, check10Next time, queryethThe exchange is at a height, if the nodes are synchronizedethIf the height is greater than the exchange's height, it indicates that the transaction has already been processed by other nodes and should be removed
                    boolean needRemovePo = checkBlockHeightTimes(po);
                    if(needRemovePo) {
                        logger().info("Block height is empty, this transaction has been processed. Remove this transaction, details: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    if(logger().isDebugEnabled()) {
                        logger().debug("Block height is empty, put back in queue for next processing, details: {}", po.toString());
                    }
                    queue.offer(po);
                    continue;
                }

                // Wait for transaction triggering revalidation`skipTimes`The round will be verified again
                if (po.getSkipTimes() > 0) {
                    po.setSkipTimes(po.getSkipTimes() - 1);
                    queue.offer(po);
                    if(logger().isDebugEnabled()) {
                        logger().debug("Transaction triggered revalidation, remaining number of rounds waiting for revalidation: {}", po.getSkipTimes());
                    }
                    continue;
                }
                // Not reaching the confirmed height, put it back in the queue and continue checking next time
                int confirmation = htgContext.getConfig().getTxBlockConfirmations();
                if (po.getTxType() == HeterogeneousChainTxType.WITHDRAW) {
                    confirmation = htgContext.getConfig().getTxBlockConfirmationsOfWithdraw();
                }
                if (ethNewestHeight - po.getBlockHeight() < confirmation) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("transaction[{}]Confirm altitude waiting: {}", po.getTxHash(), confirmation - (ethNewestHeight - po.getBlockHeight()));
                    }
                    queue.offer(po);
                    continue;
                }
                switch (po.getTxType()) {
                    case DEPOSIT:
                        if (dealDeposit(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("Recharge transactions are placed back in the queue, details: {}", poFromDB != null ? poFromDB.toString() : po.toString());
                            }
                            queue.offer(po);
                        }
                        break;
                    case WITHDRAW:
                    case CHANGE:
                    case UPGRADE:
                        if (dealBroadcastTx(po, poFromDB)) {
                            if(logger().isDebugEnabled()) {
                                logger().debug("Broadcast transactions are put back into the queue, details: {}", poFromDB != null ? poFromDB.toString() : po.toString());
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

    private void clearUnusedChange() throws Exception {
        Iterator<HtgUnconfirmedTxPo> iterator = htgContext.UNCONFIRMED_TX_QUEUE().iterator();
        while(iterator.hasNext()) {
            HtgUnconfirmedTxPo po = iterator.next();
            if (po.getTxType() == HeterogeneousChainTxType.CHANGE) {
                iterator.remove();
                this.clearDB(po.getTxHash());
            }
        }
    }

    private boolean checkBlockHeightTimes(HtgUnconfirmedTxPo po) throws Exception {
        po.increaseBlockHeightTimes();
        if(po.getBlockHeightTimes() < 10) {
            return false;
        }
        po.setBlockHeightTimes(0);
        String txHash = po.getTxHash();
        if(StringUtils.isBlank(txHash)) {
            return false;
        }
        Response.TransactionInfo txReceipt = htgWalletApi.getTransactionReceipt(txHash);
        if(txReceipt == null) {
            return false;
        }
        long blockNumber = txReceipt.getBlockNumber();
        // Latest local blocks
        HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
        if(localMax == null) {
            return false;
        }
        if(localMax.getHeight().longValue() > blockNumber) {
            return true;
        }
        return false;
    }

    private boolean dealDeposit(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        if (!htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
            logger().warn("[Abnormal recharge address] transaction[{}], Remove queue, [1]Recharge address: {}", htgTxHash, po.getNerveAddress());
            this.clearDB(htgTxHash);
            return !isReOfferQueue;
        }
        HtgUnconfirmedTxPo txPo = poFromDB;
        if(txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[Recharge task exception] DBNot obtained inPOIn the queuePO: {}", po.toString());
            return !isReOfferQueue;
        }
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBRemove from queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                logger().info("[{}]transaction[{}]Confirmed exceeding{}Height, Remove queue, nerveheight: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // supplementpoMemory data,poPrint logs for easy viewing of data
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //Verify transaction again
            boolean validateTx = validateDepositTxConfirmedInHtgNet(htgTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // Verification failed, fromDBRemove transactions from the queue
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        // Callback recharge transaction
        String nerveTxHash;
        Long height = po.getBlockHeight();
        Long txTime = po.getTxTime();
        try {
            do {
                ConverterConfig converterConfig = htgContext.getConverterCoreApi().getConverterConfig();
                byte[] withdrawalBlackhole = AddressTool.getAddressByPubKeyStr(converterConfig.getBlackHolePublicKey(), converterConfig.getChainId());
                byte[] feeAddress = AddressTool.getAddressByPubKeyStr(converterConfig.getFeePubkey(), converterConfig.getChainId());
                nerveTxHash = this.submitNerveAddFeeCrossChainTx(po, height, txTime, feeAddress);
                if (StringUtils.isNotBlank(nerveTxHash)) {
                    // This transaction is a cross chain additional handling fee transaction
                    break;
                }
                nerveTxHash = this.submitNerveOneClickCrossChainTx(po, height, txTime, withdrawalBlackhole);
                if (StringUtils.isNotBlank(nerveTxHash)) {
                    // This transaction is a one click cross chain transaction
                    break;
                }
                // RechargeablenerveThe receiving address cannot be a black hole or a fee subsidy address
                if (Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole) || Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), feeAddress)) {
                    logger().error("[{}][Abnormal recharge address][Black hole or subsidy address for handling fees]Deposit Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
                    // Verification failed, fromDBRemove transactions from the queue
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (po.isDepositIIMainAndToken()) {
                    // Recharge transactions for two types of assets simultaneously
                    nerveTxHash = htgCallBackManager.getDepositTxSubmitter().depositIITxSubmit(
                            htgTxHash,
                            height,
                            po.getFrom(),
                            po.getTo(),
                            po.getValue(),
                            txTime,
                            po.getDecimals(),
                            po.getContractAddress(),
                            po.getAssetId(),
                            po.getNerveAddress(),
                            po.getDepositIIMainAssetValue(), po.getDepositIIExtend());
                } else {
                    nerveTxHash = htgCallBackManager.getDepositTxSubmitter().txSubmit(
                            htgTxHash,
                            height,
                            po.getFrom(),
                            po.getTo(),
                            po.getValue(),
                            txTime,
                            po.getDecimals(),
                            po.isIfContractAsset(),
                            po.getContractAddress(),
                            po.getAssetId(),
                            po.getNerveAddress(), po.getDepositIIExtend());
                }
            } while (false);

            po.setNerveTxHash(nerveTxHash);
            txPo.setNerveTxHash(nerveTxHash);
            // Update when there is a change in unconfirmed transaction dataDBdata
            boolean nerveTxHashNotBlank = StringUtils.isNotBlank(nerveTxHash);
            if (nerveTxHashNotBlank) {
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
                return !isReOfferQueue;
            }
            po.increaseDepositErrorTime();
            throw e;
        }
        return isReOfferQueue;
    }

    private String submitNerveAddFeeCrossChainTx(HtgUnconfirmedTxPo po, Long height, Long txTime, byte[] feeAddress) throws Exception {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return null;
        }
        HeterogeneousAddFeeCrossChainData data = htgParseTxHelper.parseAddFeeCrossChainData(po.getDepositIIExtend(), logger());
        if (data == null) {
            return null;
        }
        // Cross chain additional handling feesnerveThe receiving address can only be a service fee subsidy address
        if (!Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), feeAddress)) {
            logger().error("[{}]AddFeeCrossChain Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
            return null;
        }
        // Cannot havetokenasset
        if (po.isIfContractAsset()) {
            logger().error("[{}]AddFeeCrossChain Asset error, token address: {}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getContractAddress(), po.getTxHash());
            return null;
        }
        String nerveTxHash = htgCallBackManager.getDepositTxSubmitter().addFeeCrossChainTxSubmit(
                po.getTxHash(),
                height,
                po.getFrom(),
                po.getTo(),
                txTime,
                po.getNerveAddress(),
                po.getValue(),
                data.getNerveTxHash(),
                data.getSubExtend());
        return nerveTxHash;
    }

    // P21take effect, One click cross chain transaction
    private String submitNerveOneClickCrossChainTx(HtgUnconfirmedTxPo po, Long height, Long txTime, byte[] withdrawalBlackhole) throws Exception {
        if (!htgContext.getConverterCoreApi().isProtocol21()) {
            return null;
        }
        HeterogeneousOneClickCrossChainData data = htgParseTxHelper.parseOneClickCrossChainData(po.getDepositIIExtend(), logger());
        if (data == null) {
            return null;
        }
        // One click cross chainnerveThe receiving address can only be a black hole address
        if (!Arrays.equals(AddressTool.getAddress(po.getNerveAddress()), withdrawalBlackhole)) {
            logger().error("[{}]OneClickCrossChain Nerve address error:{}, heterogeneousHash:{}", htgContext.HTG_CHAIN_ID(), po.getNerveAddress(), po.getTxHash());
            return null;
        }
        BigInteger erc20Value, mainAssetValue;
        Integer erc20Decimals, erc20AssetId;
        if (po.isDepositIIMainAndToken()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = po.getDepositIIMainAssetValue();
        } else if (po.isIfContractAsset()) {
            erc20Value = po.getValue();
            erc20Decimals = po.getDecimals();
            erc20AssetId = po.getAssetId();
            mainAssetValue = BigInteger.ZERO;
        } else {
            erc20Value = BigInteger.ZERO;
            erc20Decimals = 0;
            erc20AssetId = 0;
            mainAssetValue = po.getValue();
        }
        String nerveTxHash = htgCallBackManager.getDepositTxSubmitter().oneClickCrossChainTxSubmit(
                po.getTxHash(),
                height,
                po.getFrom(),
                po.getTo(),
                erc20Value,
                txTime,
                erc20Decimals,
                po.getContractAddress(),
                erc20AssetId,
                po.getNerveAddress(),
                mainAssetValue,
                data.getFeeAmount(),
                data.getDesChainId(),
                data.getDesToAddress(),
                data.getTipping(),
                data.getTippingAddress(),
                data.getDesExtend());
        return nerveTxHash;
    }

    private boolean dealBroadcastTx(HtgUnconfirmedTxPo po, HtgUnconfirmedTxPo poFromDB) throws Exception {
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        HtgUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}Task exception] DBNot obtained inPOIn the queuePO: {}", po.getTxType(), po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBDelete in, do not put back in queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(htgTxHash);
                isReOfferQueue = false;
                htgResendHelper.clear(nerveTxHash);
                logger().info("[{}]transaction[{}]Confirmed exceeding{}Height, Remove queue, nerveheight: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
                if (!htgResendHelper.canResend(nerveTxHash)) {
                    logger().warn("Nervetransaction[{}]Resend over{}Second, discard transaction", nerveTxHash, RESEND_TIME);
                    htgResendHelper.clear(htgTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                logger().info("Failed{}transaction[{}]Check if the current node can send transactions", htgContext.getConfig().getSymbol(), htgTxHash);
                // Check if your order is eligible for trading
                HtgWaitingTxPo waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                // Check if there are any three roundswaitingTxPoOtherwise, remove thisFAILEDtask
                if (waitingTxPo == null && !po.checkFailedTimeOut()) {
                    return isReOfferQueue;
                }
                // querynerveCorresponding to the transactionethWhether the transaction was successful
                if (htgInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("Nerve tx stayNERVENetwork confirmed, Successfully removed queue, nerveHash: {}", nerveTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (waitingTxPo == null) {
                    logger().info("Check three rounds without any issueswaitingTxPoRemove thisFAILEDtask, htgTxHash: {}", htgTxHash);
                    return !isReOfferQueue;
                }
                if (this.checkIfSendByOwn(waitingTxPo, txPo.getFrom())) {
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                // Failed transactions, not handled by the current node, from the queue andDBRemove from middle
                logger().info("Failed{}transaction[{}]The current node is not in the next order and will not be processed by the current node. Remove the queue", htgContext.getConfig().getSymbol(), htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //Verify transaction again
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInHtgNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // Put it back in the queue and verify again
                            return isReOfferQueue;
                        case RE_SEND:
                            // If the transaction status is completed and verification fails again, resend the transaction
                            HtgWaitingTxPo _waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                            // Check if there are any three roundswaitingTxPoOtherwise, remove thisFAILEDtask
                            if (_waitingTxPo == null && po.checkFailedTimeOut()) {
                                return isReOfferQueue;
                            }
                            htgResendHelper.reSend(_waitingTxPo);
                            this.clearDB(htgTxHash);
                            return !isReOfferQueue;
                        case SUCCESS:
                        default:
                            break;
                    }
                    po.setValidateTx(validate == BroadcastTxValidateStatus.SUCCESS);
                }
                try {
                    String realNerveTxHash = nerveTxHash;
                    logger().info("[{}]Signed{}transaction[{}]callNerveconfirm[{}]", po.getTxType(), htgContext.getConfig().getSymbol(), htgTxHash, realNerveTxHash);
                    // The signed transaction will trigger a callbackNerve Core
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            htgTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            htgContext.MULTY_SIGN_ADDRESS(),
                            txPo.getSigners());
                } catch (NulsException e) {
                    // Transaction already exists, waiting for confirmation to remove
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nervetransaction[{}]Exists, remove pending confirmation from queue{}transaction[{}]", txPo.getNerveTxHash(), htgContext.getConfig().getSymbol(), htgTxHash);
                        return !isReOfferQueue;
                    }
                    throw e;
                }
                break;
        }
        return isReOfferQueue;
    }

    /**
     * ifethIf the transaction fails, check if the current node is the next sequential transaction. If it is, resend the transaction
     */
    private boolean checkIfSendByOwn(HtgWaitingTxPo waitingTxPo, String txFrom) throws Exception {
        String nerveTxHash = waitingTxPo.getNerveTxHash();
        Map<String, Integer> virtualBanks = waitingTxPo.getCurrentVirtualBanks();
        Integer totalBank = virtualBanks.size();
        Integer sendOrderCurrentNode = waitingTxPo.getCurrentNodeSendOrder();//
        int sendOrderFailure = virtualBanks.get(txFrom);
        // Check failedorderIs it the last in the administrator's ranking? If so, reset the current node to have been sentHTRecord of transactions, reset node waiting time, and resend transactions starting from the first order
        if (sendOrderFailure == totalBank) {
            htgInvokeTxHelper.clearRecordOfCurrentNodeSentEthTx(nerveTxHash, waitingTxPo);
            if (sendOrderCurrentNode == 1) {
                // Initiate transaction
                htgResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        if (sendOrderFailure + 1 == sendOrderCurrentNode) {
            // The current node is the next sequential transaction. Check if the transaction has been sent out, otherwise the transaction will be sent out
            if (!htgInvokeTxHelper.currentNodeSentEthTx(nerveTxHash)) {
                // Initiate transaction
                htgResendHelper.reSend(waitingTxPo);
                return true;
            }
        }
        return false;
    }

    /**
     * Verify recharge transactions
     */
    private boolean validateDepositTxConfirmedInHtgNet(String htgTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            Response.TransactionInfo receipt = htgWalletApi.getTransactionReceipt(htgTxHash);
            if (receipt == null) {
                logger().error("Verify transaction again[{}]Failed, unable to obtainreceipt", htgTxHash);
                break;
            }
            if (!TrxUtil.checkTransactionSuccess(receipt)) {
                logger().error("Verify transaction again[{}]Failed,receiptIncorrect status", htgTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogCount() == 0)) {
                logger().error("Verify transaction again[{}]Failed,receipt.LogIncorrect status", htgTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * Verification sent toTRXIs the online transaction confirmed? If there are any abnormal situations, resend the transaction according to the conditions
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInHtgNet(HtgUnconfirmedTxPo po) throws Exception {

        BroadcastTxValidateStatus status;
        String htgTxHash = po.getTxHash();
        do {
            Response.TransactionInfo receipt = htgWalletApi.getTransactionReceipt(htgTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > HtgConstant.MINUTES_20;
                logger().error("Verify transaction again[{}]Failed, unable to obtainreceipt", htgTxHash);
                if (timeOut) {
                    // If the transaction is not confirmed within 20 minutes, resend the transaction
                    status = BroadcastTxValidateStatus.RE_SEND;
                } else {
                    // Transaction receipt not obtained,20Within minutes, every3One round of verification
                    po.setSkipTimes(3);
                    status = BroadcastTxValidateStatus.RE_VALIDATE;
                }
                break;
            }
            if (!TrxUtil.checkTransactionSuccess(receipt)) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("Verify transaction again[{}]Failed,receiptIncorrect status", htgTxHash);
                break;
            } else if (receipt.getLogCount() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("Verify transaction again[{}]Failed,receipt.LogIncorrect status", htgTxHash);
                break;
            }
            status = BroadcastTxValidateStatus.SUCCESS;
        } while (false);
        return status;
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
