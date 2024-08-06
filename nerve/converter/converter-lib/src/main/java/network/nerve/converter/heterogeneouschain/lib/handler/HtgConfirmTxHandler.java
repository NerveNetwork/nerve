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

import io.nuls.base.basic.AddressTool;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.enums.BroadcastTxValidateStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.model.bo.HeterogeneousAddFeeCrossChainData;
import network.nerve.converter.model.bo.HeterogeneousOneClickCrossChainData;
import network.nerve.converter.model.bo.WithdrawalTotalFeeInfo;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.LoggerUtil;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.*;


/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class HtgConfirmTxHandler implements Runnable, BeanInitial {

    private HtgWalletApi htgWalletApi;
    private ConverterConfig converterConfig;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgTxRelationStorageService htgTxRelationStorageService;
    private HtgTxStorageService htgTxStorageService;
    private HtgCallBackManager htgCallBackManager;
    private HtgStorageHelper htgStorageHelper;
    private HtgLocalBlockHelper htgLocalBlockHelper;
    private HtgListener htgListener;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgParseTxHelper htgParseTxHelper;
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
            String symbol = htgContext.getConfig().getSymbol();
            if (!htgContext.getConverterCoreApi().isRunning()) {
                LoggerUtil.LOG.debug("[{}] Ignoring synchronous block mode", symbol);
                return;
            }
            if (!htgContext.getConverterCoreApi().checkNetworkRunning(htgContext.HTG_CHAIN_ID())) {
                htgContext.logger().info("Test network [{}] Run Pause, chainId: {}", htgContext.getConfig().getSymbol(), htgContext.HTG_CHAIN_ID());
                return;
            }
            if (!htgContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
                LoggerUtil.LOG.debug("[{}]Non virtual bank member, skip this task", symbol);
                return;
            }
            if (!htgContext.isAvailableRPC()) {
                htgContext.logger().error("[{}] network RPC Unavailable, pause this task", symbol);
                return;
            }
            try {
                htgWalletApi.checkApi(htgContext.getConverterCoreApi().getVirtualBankOrder());
                BigInteger currentGasPrice = htgWalletApi.getCurrentGasPrice();
                if (currentGasPrice != null) {
                    htgContext.logger().debug("current {} Network based Price: {} Gwei.", symbol, new BigDecimal(currentGasPrice).divide(BigDecimal.TEN.pow(9)).toPlainString());
                    htgContext.setEthGasPrice(currentGasPrice);
                }
            } catch (Exception e) {
                htgContext.logger().error(String.format("synchronization %s current Price fail", symbol), e);
            }
            LoggerUtil.LOG.debug("[{} Transaction confirmation task] - every other {} Execute once per second.", symbol, htgContext.getConfig().getConfirmTxQueuePeriod());
            // Persistent unconfirmed transactions loaded while waiting for application restart
            htgContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH().await();
            long ethNewestHeight = htgWalletApi.getBlockHeight();
            int size = htgContext.UNCONFIRMED_TX_QUEUE().size();
            for (int i = 0; i < size; i++) {
                po = htgContext.UNCONFIRMED_TX_QUEUE().poll();
                if (po == null) {
                    logger().info("Remove null values PO");
                    continue;
                }
                // Clean up useless change tasks
                if (po.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                    clearUnusedChange();
                    break;
                }
                // When the recharge confirmation task is abnormal and exceeds the number of retries, discard the task
                if (po.isDepositExceedErrorTime(RESEND_TIME)) {
                    logger().error("[{}]The recharge confirmation task has exceeded the number of retries. Please remove this transaction. Details: {}", symbol, po.toString());
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
                    // Block height is empty, check 10 Next time, query eth The exchange is at a height, if the nodes are synchronized eth If the height is greater than the exchange's height, it indicates that the transaction has already been processed by other nodes and should be removed
                    boolean needRemovePo = checkBlockHeightTimes(po);
                    if(needRemovePo) {
                        logger().info("[{}]Block height is empty, this transaction has been processed. Remove this transaction, details: {}", symbol, po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    // [Accelerated resend trading mechanism] If there is no block height, it indicates that it has been in an unresolved state, indicating that it has not been resolvedHTpack（Check if it has been packaged）Check if it is a transaction sent locally and accelerate the resend of transactions
                    boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > HtgConstant.MINUTES_3;
                    if (timeOut) {
                        String htgTxHash = po.getTxHash();
                        boolean currentNodeSent = htgResendHelper.currentNodeSent(htgTxHash);
                        if (currentNodeSent) {
                            // Check if the transaction sent by the current node has been packaged
                            boolean packed = checkPacked(htgTxHash);
                            if (!packed) {
                                // Transaction not packaged, increase gasPrice Accelerate resend transactions
                                HtgSendTransactionPo txInfo = htgResendHelper.getSentTransactionInfo(htgTxHash);
                                boolean speedSent = speedUpResendTransaction(po.getTxType(), po.getNerveTxHash(), poFromDB, txInfo);
                                if (speedSent) {
                                    this.clearDB(htgTxHash);
                                    continue;
                                }
                            }
                        }
                    }
                    if(logger().isDebugEnabled()) {
                        logger().debug("[{}] Block height is empty, put back in queue for next processing, details: {}", symbol, po.toString());
                    }
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
                int confirmation = htgContext.getConfig().getTxBlockConfirmations();
                if (po.getTxType() == HeterogeneousChainTxType.WITHDRAW) {
                    confirmation = htgContext.getConfig().getTxBlockConfirmationsOfWithdraw();
                }
                if (ethNewestHeight - po.getBlockHeight() < confirmation) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("[{}] transaction [{}] Confirm altitude waiting: {}", symbol, po.getTxHash(), confirmation - (ethNewestHeight - po.getBlockHeight()));
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
        Transaction tx = htgWalletApi.getTransactionByHash(txHash);
        if(tx == null) {
            return false;
        }
        BigInteger blockNumber = htgParseTxHelper.getTxHeight(logger(), tx);
        // Latest local blocks
        HtgSimpleBlockHeader localMax = htgLocalBlockHelper.getLatestLocalBlockHeader();
        if(localMax == null) {
            return false;
        }
        if(localMax.getHeight().longValue() > blockNumber.longValue()) {
            return true;
        }
        return false;
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
            this.clearDB(htgTxHash);
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
            boolean validateTx = validateDepositTxConfirmedInEthNet(htgTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // Verification failed, fromDBRemove transactions from the queue
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        try {
            Transaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
            Long height = htgParseTxHelper.getTxHeight(logger(), htgTx).longValue();
            EthBlock.Block header = htgWalletApi.getBlockHeaderByHeight(height);
            long txTime = header.getTimestamp().longValue();
            // Callback recharge transaction
            String nerveTxHash;
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
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            }
            po.increaseDepositErrorTime();
            throw e;
        }
        return isReOfferQueue;
    }

    private String submitNerveAddFeeCrossChainTx(HtgUnconfirmedTxPo po, Long height, long txTime, byte[] feeAddress) throws Exception {
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
        String symbol = htgContext.getConfig().getSymbol();
        boolean isReOfferQueue = true;
        String htgTxHash = po.getTxHash();
        HtgUnconfirmedTxPo txPo = poFromDB;
        if (txPo == null) {
            txPo = htgUnconfirmedTxStorageService.findByTxHash(htgTxHash);
        }
        if (txPo == null) {
            logger().warn("[{}][{}Task exception] DBNot obtained inPOIn the queuePO: {}", symbol, po.getTxType(), po.toString());
            this.clearDB(htgTxHash);
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
                logger().info("[{}] [{}] transaction [{}] Confirmed exceeding {} Height, Remove queue, nerveheight: {}, nerver hash: {}", symbol, po.getTxType(), po.getTxHash(), HtgConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
                    logger().warn("[{}] Nerve transaction [{}] Resend over {} Second, discard transaction", symbol, nerveTxHash, RESEND_TIME);
                    htgResendHelper.clear(htgTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                logger().info("Failed {} transaction [{}] Check if the current node can send transactions", symbol, htgTxHash);
                // Check if your order is eligible for trading
                HtgWaitingTxPo waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                // Check if there are any three roundswaitingTxPoOtherwise, remove thisFAILEDtask
                if (waitingTxPo == null && !po.checkFailedTimeOut()) {
                    return isReOfferQueue;
                }
                // querynerveCorresponding to the transactionethWhether the transaction was successful
                if (htgInvokeTxHelper.isSuccessfulNerve(nerveTxHash)) {
                    logger().info("[{}]Nerve tx stay NERVE Network confirmed, Successfully removed queue, nerveHash: {}", symbol, nerveTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (waitingTxPo == null) {
                    logger().info("[{}]Check three rounds without any issues waitingTxPo Remove this FAILED task, htgTxHash: {}", symbol, htgTxHash);
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                if (this.checkIfSendByOwn(waitingTxPo, txPo.getFrom())) {
                    this.clearDB(htgTxHash);
                    return !isReOfferQueue;
                }
                // Failed transactions, not handled by the current node, from the queue andDBRemove from middle
                logger().info("Failed {} transaction [{}] The current node is not in the next order and will not be processed by the current node. Remove the queue", symbol, htgTxHash);
                this.clearDB(htgTxHash);
                return !isReOfferQueue;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //Verify transaction again
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInEthNet(po);
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
                    logger().info("[{}] Signed {} transaction [{}] call Nerve confirm [{}]", po.getTxType(), symbol, htgTxHash, realNerveTxHash);
                    Transaction htgTx = htgWalletApi.getTransactionByHash(htgTxHash);
                    Long height = htgParseTxHelper.getTxHeight(logger(), htgTx).longValue();
                    EthBlock.Block header = htgWalletApi.getBlockHeaderByHeight(height);
                    // The signed transaction will trigger a callbackNerve Core
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            htgTxHash,
                            height,
                            header.getTimestamp().longValue(),
                            htgContext.MULTY_SIGN_ADDRESS(),
                            txPo.getSigners(),
                            null);
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

    private boolean speedUpResendTransaction(HeterogeneousChainTxType txType, String nerveTxHash, HtgUnconfirmedTxPo unconfirmedTxPo, HtgSendTransactionPo txInfo) throws Exception {
        if (htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) {
            //protocol15: Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
            return speedUpResendTransactionProtocol15(txType, nerveTxHash, unconfirmedTxPo, txInfo);
        } else {
            return _speedUpResendTransaction(txType, nerveTxHash, unconfirmedTxPo, txInfo);
        }
    }

    private boolean _speedUpResendTransaction(HeterogeneousChainTxType txType, String nerveTxHash, HtgUnconfirmedTxPo unconfirmedTxPo, HtgSendTransactionPo txInfo) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        if (txInfo == null) {
            return false;
        }
        logger().info("[{}]Detected the need to accelerate resend transactions, type: {}, ethHash: {}, nerveTxHash: {}, gasPrice: {}", symbol, txType, unconfirmedTxPo.getTxHash(), nerveTxHash, new BigDecimal(htgContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString());
        // towardsHTNetwork request verification
        boolean isCompleted = htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}][{}]transaction[{}]Completed", symbol, txType, nerveTxHash);
            // Send a transfer to oneself to cover this transactionnonce
            String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
            if (StringUtils.isNotBlank(overrideHash)) {
                logger().info("[{}]Transfer coverage transaction: {}, covered transactions: {}", symbol, overrideHash, txInfo.getTxHash());
            } else {
                logger().info("[{}]Unsuccessful issuance of overlay transaction", symbol);
            }
            return true;
        }
        if (logger().isDebugEnabled()) {
            logger().debug("[{}]Before acceleration: {}", symbol, txInfo.toString());
        }
        // Get the latestnonceSend transaction
        String currentFrom = htgContext.ADMIN_ADDRESS();
        BigInteger nonce = htgWalletApi.getLatestNonce(currentFrom);
        txInfo.setNonce(nonce);

        BigInteger oldGasPrice = txInfo.getGasPrice();
        BigInteger newGasPrice;
        boolean isWithdrawTx = HeterogeneousChainTxType.WITHDRAW == txType;
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        if (isWithdrawTx && coreApi.isSupportNewMechanismOfWithdrawalFee()) {
            // calculateGasPrice
            BigDecimal gasPrice = HtgUtil.calcGasPriceOfWithdraw(AssetName.NVT, coreApi.getUsdtPriceByAsset(AssetName.NVT), new BigDecimal(coreApi.getFeeOfWithdrawTransaction(nerveTxHash).getFee()), coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME()), unconfirmedTxPo.getAssetId(), htgContext.GAS_LIMIT_OF_WITHDRAW());
            if (gasPrice == null || gasPrice.toBigInteger().compareTo(oldGasPrice) < 0) {
                logger().error("[{}][Withdrawal]transaction[{}]Insufficient handling fees, latest providedGasPrice: {}, Currently issued transaction[{}]ofGasPrice: {}", symbol, nerveTxHash, gasPrice == null ? null : gasPrice.toPlainString(), txInfo.getTxHash(), oldGasPrice);
                return false;
            }
            gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), gasPrice);
            newGasPrice = gasPrice.toBigInteger();
            if (newGasPrice.compareTo(htgContext.getEthGasPrice()) < 0) {
                logger().error("[{}][Withdrawal]transaction[{}]Insufficient handling fees, latest providedGasPrice: {}, current{}Network basedGasPrice: {}", symbol, nerveTxHash, newGasPrice, htgContext.getConfig().getSymbol(), htgContext.getEthGasPrice());
                return false;
            }
        } else {
            newGasPrice = calSpeedUpGasPriceByOrdinaryWay(oldGasPrice);
        }
        if (newGasPrice == null) {
            return false;
        }
        txInfo.setGasPrice(newGasPrice);
        // Obtain account information
        HeterogeneousAccount account = htgContext.DOCKING().getAccount(currentFrom);
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // Verify business data
        String contractAddress = txInfo.getTo();
        String encodedFunction = txInfo.getData();
        EthCall ethCall = htgWalletApi.validateContractCall(currentFrom, contractAddress, encodedFunction);
        if (ethCall.isReverted()) {
            if (ConverterUtil.isCompletedTransaction(ethCall.getRevertReason())) {
                logger().info("[{}][{}]transaction[{}]Completed", symbol, txType, nerveTxHash);
                // Send a transfer to oneself to cover this transactionnonce
                String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
                if (StringUtils.isNotBlank(overrideHash)) {
                    logger().info("[{}]Transfer coverage transaction: {}, covered transactions: {}", symbol, overrideHash, txInfo.getTxHash());
                } else {
                    logger().info("[{}]Unsuccessful issuance of overlay transaction", symbol);
                }
                return true;
            }
            logger().warn("[{}][{}]Accelerated resend transaction verification failed, reason: {}", symbol, txType, ethCall.getRevertReason());
            return false;
        }
        HtgSendTransactionPo newTxPo = htgWalletApi.callContractRaw(priKey, txInfo);
        String htgTxHash = newTxPo.getTxHash();
        // dockinglaunchethRecord transaction relationships during transactionsdbIn, and save the currently usednonceIn the relationship table, if there is any reasonpriceIf there is a need to resend the transaction without packaging it too low, the current one used will be taken outnonceResend transaction
        htgTxRelationStorageService.save(htgTxHash, nerveTxHash, newTxPo);

        HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
        BeanUtils.copyProperties(unconfirmedTxPo, po);
        // Save unconfirmed transactions
        po.setTxHash(htgTxHash);
        po.setFrom(currentFrom);
        po.setTxType(txType);
        po.setCreateDate(System.currentTimeMillis());
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        // Monitor the packaging status of this transaction
        htgListener.addListeningTx(htgTxHash);
        if (isWithdrawTx && StringUtils.isNotBlank(htgTxHash)) {
            // Record withdrawal transactions that have been transferred toHTNetwork transmission
            htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htgTxHash);
        }
        logger().info("Accelerated retransmission{}Online transaction successful, type: {}, details: {}", symbol, txType, po.superString());
        if (logger().isDebugEnabled()) {
            logger().debug("[{}]After acceleration: {}", symbol, newTxPo.toString());
        }
        return true;
    }

    private boolean speedUpResendTransactionProtocol15(HeterogeneousChainTxType txType, String nerveTxHash, HtgUnconfirmedTxPo unconfirmedTxPo, HtgSendTransactionPo txInfo) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        if (txInfo == null) {
            return false;
        }
        logger().info("[{}]Detected the need to accelerate resend transactions, type: {}, ethHash: {}, nerveTxHash: {}", symbol, txType, unconfirmedTxPo.getTxHash(), nerveTxHash);
        // towardsHTNetwork request verification
        boolean isCompleted = htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}][{}]transaction[{}]Completed", symbol, txType, nerveTxHash);
            // Send a transfer to oneself to cover this transactionnonce
            String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
            if (StringUtils.isNotBlank(overrideHash)) {
                logger().info("[{}]Transfer coverage transaction: {}, covered transactions: {}", symbol, overrideHash, txInfo.getTxHash());
            } else {
                logger().info("[{}]Unsuccessful issuance of overlay transaction", symbol);
            }
            return true;
        }
        if (logger().isDebugEnabled()) {
            logger().debug("[{}]Before acceleration: {}", symbol, txInfo.toString());
        }
        // Get the latestnonceSend transaction
        String currentFrom = htgContext.ADMIN_ADDRESS();
        BigInteger nonce = htgWalletApi.getLatestNonce(currentFrom);
        txInfo.setNonce(nonce);

        BigInteger oldGasPrice = txInfo.getGasPrice();
        BigInteger newGasPrice;
        boolean isWithdrawTx = HeterogeneousChainTxType.WITHDRAW == txType;
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        if (isWithdrawTx) {
            // calculateGasPrice
            // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            BigDecimal feeAmount = new BigDecimal(feeInfo.getFee());
            BigDecimal gasPrice;
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            // When using other main assets of non withdrawal networks as transaction fees
            feeAmount = new BigDecimal(coreApi.checkDecimalsSubtractedToNerveForWithdrawal(feeInfo.getHtgMainAssetName().chainId(), 1, feeAmount.toBigInteger()));
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                gasPrice = HtgUtil.calcGasPriceOfWithdraw(feeInfo.getHtgMainAssetName(), coreApi.getUsdtPriceByAsset(feeInfo.getHtgMainAssetName()), feeAmount, coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME()), unconfirmedTxPo.getAssetId(), htgContext.GAS_LIMIT_OF_WITHDRAW());
            } else {
                gasPrice = HtgUtil.calcGasPriceOfWithdrawByMainAssetProtocol15(feeAmount, unconfirmedTxPo.getAssetId(), htgContext.GAS_LIMIT_OF_WITHDRAW());
            }
            if (gasPrice == null || gasPrice.toBigInteger().compareTo(oldGasPrice) < 0) {
                logger().error("[{}][Withdrawal]transaction[{}]Insufficient handling fees, latest providedGasPrice: {}, Currently issued transaction[{}]ofGasPrice: {}", symbol, nerveTxHash, gasPrice == null ? null : gasPrice.toPlainString(), txInfo.getTxHash(), oldGasPrice);
                return false;
            }
            gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), gasPrice);
            newGasPrice = gasPrice.toBigInteger();
            if (newGasPrice.compareTo(htgContext.getEthGasPrice()) < 0) {
                logger().error("[Withdrawal]transaction[{}]Insufficient handling fees, latest providedGasPrice: {}, current{}Network basedGasPrice: {}", nerveTxHash, newGasPrice, symbol, htgContext.getEthGasPrice());
                return false;
            }
        } else {
            newGasPrice = calSpeedUpGasPriceByOrdinaryWay(oldGasPrice);
        }
        if (newGasPrice == null) {
            return false;
        }
        txInfo.setGasPrice(newGasPrice);
        // Obtain account information
        HeterogeneousAccount account = htgContext.DOCKING().getAccount(currentFrom);
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // Verify business data
        String contractAddress = txInfo.getTo();
        String encodedFunction = txInfo.getData();
        EthCall ethCall = htgWalletApi.validateContractCall(currentFrom, contractAddress, encodedFunction);
        if (ethCall.isReverted()) {
            if (ConverterUtil.isCompletedTransaction(ethCall.getRevertReason())) {
                logger().info("[{}][{}]transaction[{}]Completed", symbol, txType, nerveTxHash);
                // Send a transfer to oneself to cover this transactionnonce
                String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
                if (StringUtils.isNotBlank(overrideHash)) {
                    logger().info("[{}]Transfer coverage transaction: {}, covered transactions: {}", symbol, overrideHash, txInfo.getTxHash());
                } else {
                    logger().info("[{}]Unsuccessful issuance of overlay transaction", symbol);
                }
                return true;
            }
            logger().warn("[{}][{}]Accelerated resend transaction verification failed, reason: {}", symbol, txType, ethCall.getRevertReason());
            return false;
        }
        HtgSendTransactionPo newTxPo = htgWalletApi.callContractRaw(priKey, txInfo);
        String htgTxHash = newTxPo.getTxHash();
        // dockinglaunchethRecord transaction relationships during transactionsdbIn, and save the currently usednonceIn the relationship table, if there is any reasonpriceIf there is a need to resend the transaction without packaging it too low, the current one used will be taken outnonceResend transaction
        htgTxRelationStorageService.save(htgTxHash, nerveTxHash, newTxPo);

        HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
        BeanUtils.copyProperties(unconfirmedTxPo, po);
        // Save unconfirmed transactions
        po.setTxHash(htgTxHash);
        po.setFrom(currentFrom);
        po.setTxType(txType);
        po.setCreateDate(System.currentTimeMillis());
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        // Monitor the packaging status of this transaction
        htgListener.addListeningTx(htgTxHash);
        if (isWithdrawTx && StringUtils.isNotBlank(htgTxHash)) {
            // Record withdrawal transactions that have been transferred toHTNetwork transmission
            htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htgTxHash);
        }
        logger().info("Accelerated retransmission{}Online transaction successful, type: {}, details: {}", symbol, txType, po.superString());
        if (logger().isDebugEnabled()) {
            logger().debug("[{}]After acceleration: {}", symbol, newTxPo.toString());
        }
        return true;
    }

    private BigInteger calSpeedUpGasPriceByOrdinaryWay(BigInteger oldGasPrice) throws Exception {
        // Improve transaction efficiencyprice, obtain the currentHTnetworkprice, and oldpriceTake the larger value, and then+2, i.e price = price + 2, maximum currentpriceof1.1times
        BigInteger currentGasPrice = htgWalletApi.getCurrentGasPrice();
        BigInteger maxCurrentGasPrice = new BigDecimal(currentGasPrice).multiply(HtgConstant.NUMBER_1_DOT_1).toBigInteger();
        if (maxCurrentGasPrice.compareTo(oldGasPrice) <= 0) {
            logger().info("Current transactiongasPriceReached maximum acceleration value, no further acceleration, waiting{}Network packaged transactions", htgContext.getConfig().getSymbol());
            return null;
        }
        BigInteger newGasPrice = oldGasPrice.compareTo(currentGasPrice) > 0 ? oldGasPrice : currentGasPrice;
        newGasPrice = newGasPrice.add(HtgConstant.GWEI_2);
        newGasPrice = newGasPrice.compareTo(maxCurrentGasPrice) > 0 ? maxCurrentGasPrice : newGasPrice;
        return newGasPrice;
    }

    private String sendOverrideTransferTx(String from, BigInteger gasPrice, BigInteger nonce) {
        try {
            String symbol = htgContext.getConfig().getSymbol();
            // Check if the address from which the transaction was sent matches the current virtual bank address, otherwise ignore it
            String currentFrom = htgContext.ADMIN_ADDRESS();
            if (!currentFrom.equals(from)) {
                logger().info("[{}]The address where the transfer was sent to cover the transaction does not match the current virtual bank address, ignore", symbol);
                return null;
            }
            // Obtain account information
            HeterogeneousAccount account = htgContext.DOCKING().getAccount(currentFrom);
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Get Networkgasprice
            BigInteger currentGasPrice = htgWalletApi.getCurrentGasPrice();
            BigInteger newGasPrice = gasPrice.compareTo(currentGasPrice) > 0 ? gasPrice : currentGasPrice;
            newGasPrice = newGasPrice.add(HtgConstant.GWEI_3);
            if (logger().isDebugEnabled()) {
                logger().debug("[{}]Assembly covered transaction data, from: {}, to: {}, value: {}, gasLimit: {}, gasPrice: {}, nonce: {}",
                        symbol,
                        currentFrom,
                        currentFrom,
                        BigInteger.ZERO,
                        htgContext.GAS_LIMIT_OF_MAIN_ASSET(),
                        newGasPrice,
                        nonce);
            }
            String hash = htgWalletApi.sendMainAssetWithNonce(currentFrom, priKey, currentFrom, BigDecimal.ZERO, htgContext.GAS_LIMIT_OF_MAIN_ASSET(), newGasPrice, nonce);
            return hash;
        } catch (Exception e) {
            logger().warn("Transfer overlay transaction exception occurred, ignored", e);
            return null;
        }

    }

    /**
     * inspectethHas the transaction been packaged
     */
    private boolean checkPacked(String htgTxHash) throws Exception {
        TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(htgTxHash);
        return txReceipt != null;
    }

    /**
     * Verify recharge transactions
     */
    private boolean validateDepositTxConfirmedInEthNet(String htgTxHash, boolean ifContractAsset) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        boolean validateTx = false;
        do {
            TransactionReceipt receipt = htgWalletApi.getTxReceipt(htgTxHash);
            if (receipt == null) {
                logger().error("[{}]Verify transaction again[{}]Failed, unable to obtainreceipt", symbol, htgTxHash);
                break;
            }
            if (!receipt.isStatusOK()) {
                logger().error("[{}]Verify transaction again[{}]Failed,receiptIncorrect status", symbol, htgTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogs() == null || receipt.getLogs().size() == 0)) {
                logger().error("[{}]Verify transaction again[{}]Failed,receipt.LogIncorrect status", symbol, htgTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * Verification sent toHTIs the online transaction confirmed? If there are any abnormal situations, resend the transaction according to the conditions
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInEthNet(HtgUnconfirmedTxPo po) throws Exception {
        String symbol = htgContext.getConfig().getSymbol();
        BroadcastTxValidateStatus status;
        String htgTxHash = po.getTxHash();
        do {
            TransactionReceipt receipt = htgWalletApi.getTxReceipt(htgTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > HtgConstant.MINUTES_20;
                logger().error("[{}]Verify transaction again[{}]Failed, unable to obtainreceipt", symbol, htgTxHash);
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
            if (!receipt.isStatusOK()) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("[{}]Verify transaction again[{}]Failed,receiptIncorrect status", symbol, htgTxHash);
                break;
            } else if (receipt.getLogs() == null || receipt.getLogs().size() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("[{}]Verify transaction again[{}]Failed,receipt.LogIncorrect status", symbol, htgTxHash);
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
