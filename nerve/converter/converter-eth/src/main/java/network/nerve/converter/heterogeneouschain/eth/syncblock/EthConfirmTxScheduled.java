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
            LoggerUtil.LOG.debug("Ignoring synchronous block mode");
            return;
        }
        if (!EthContext.getConverterCoreApi().isVirtualBankByCurrentNode()) {
            LoggerUtil.LOG.debug("Non virtual bank member, skip this task");
            return;
        }
        LoggerUtil.LOG.debug("[ETHTransaction confirmation task] - every other20Execute once per second.");
        LinkedBlockingDeque<EthUnconfirmedTxPo> queue = EthContext.UNCONFIRMED_TX_QUEUE;
        EthUnconfirmedTxPo po = null;
        try {
            ethWalletApi.checkApi(EthContext.getConverterCoreApi().getVirtualBankOrder());
            // Persistent unconfirmed transactions loaded while waiting for application restart
            EthContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.await();
            long ethNewestHeight = ethWalletApi.getBlockHeight();
            int size = EthContext.UNCONFIRMED_TX_QUEUE.size();
            for (int i = 0; i < size; i++) {
                po = EthContext.UNCONFIRMED_TX_QUEUE.poll();
                if (po == null) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("Remove null valuesPO");
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
                    // Block height is empty, check10Next time, queryethThe exchange is at a height, if the nodes are synchronizedethIf the height is greater than the exchange's height, it indicates that the transaction has already been processed by other nodes and should be removed
                    boolean needRemovePo = checkBlockHeightTimes(po);
                    if(needRemovePo) {
                        logger().info("Block height is empty, this transaction has been processed. Remove this transaction, details: {}", po.toString());
                        this.clearDB(po.getTxHash());
                        continue;
                    }
                    // [Accelerated resend trading mechanism] If there is no block height, it indicates that it has been in an unresolved state, indicating that it has not been resolvedETHpack（Check if it has been packaged）Check if it is a transaction sent locally and accelerate the resend of transactions
                    boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > EthConstant.MINUTES_5;
                    if (timeOut) {
                        String ethTxHash = po.getTxHash();
                        boolean currentNodeSent = ethResendHelper.currentNodeSent(ethTxHash);
                        if (currentNodeSent) {
                            // Check if the transaction sent by the current node has been packaged
                            boolean packed = checkPacked(ethTxHash);
                            if (!packed) {
                                // Transaction not packaged, increase gasPrice Accelerate resend transactions
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
                if (ethNewestHeight - po.getBlockHeight() < EthContext.getConfig().getTxBlockConfirmations()) {
                    if(logger().isDebugEnabled()) {
                        logger().debug("transaction[{}]Confirm altitude waiting: {}", po.getTxHash(), EthContext.getConfig().getTxBlockConfirmations() - (ethNewestHeight - po.getBlockHeight()));
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
                    case RECOVERY:
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
        // Latest local blocks
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
            logger().warn("[Recharge task exception] DBNot obtained inPOIn the queuePO: {}", po.toString());
            return !isReOfferQueue;
        }
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBRemove from queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                logger().info("[{}]transaction[{}]Confirmed exceeding{}Height, Remove queue, nerveheight: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
            }
            // supplementpoMemory data,poPrint logs for easy viewing of data
            po.setDelete(txPo.isDelete());
            po.setDeletedHeight(txPo.getDeletedHeight());
            return isReOfferQueue;
        }
        if (!po.isValidateTx()) {
            //Verify transaction again
            boolean validateTx = validateDepositTxConfirmedInEthNet(ethTxHash, po.isIfContractAsset());
            if (!validateTx) {
                // Verification failed, fromDBRemove transactions from the queue
                this.clearDB(ethTxHash);
                return !isReOfferQueue;
            }
            po.setValidateTx(validateTx);
        }
        try {
            // Callback recharge transaction
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
                    po.getNerveAddress(), null);
            po.setNerveTxHash(nerveTxHash);
            txPo.setNerveTxHash(nerveTxHash);
            // Update when there is a change in unconfirmed transaction dataDBdata
            boolean nerveTxHashNotBlank = StringUtils.isNotBlank(nerveTxHash);
            if (nerveTxHashNotBlank) {
                ethUnconfirmedTxStorageService.update(txPo, update -> update.setNerveTxHash(nerveTxHash));
                if (nerveTxHashNotBlank) {
                    ethStorageHelper.saveTxInfo(txPo);
                }
            }
        } catch (Exception e) {
            // Transaction already exists, remove queue
            if (e instanceof NulsException &&
                    (TX_ALREADY_EXISTS_0.equals(((NulsException) e).getErrorCode())
                            || TX_ALREADY_EXISTS_2.equals(((NulsException) e).getErrorCode()))) {
                logger().info("NerveTransaction already exists, remove pending confirmation from queueETHtransaction[{}]", ethTxHash);
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
            logger().warn("[Withdrawal task exception] DBNot obtained inPOIn the queuePO: {}", po.toString());
            return !isReOfferQueue;
        }
        String nerveTxHash = po.getNerveTxHash();
        // When the status is removed, no more callbacks will be madeNerveCore, put it back in the queue, wait until the removal height is reached, and then remove it from the queueDBDelete in, do not put back in queue
        if (txPo.isDelete()) {
            long currentBlockHeightOnNerve = this.getCurrentBlockHeightOnNerve();
            if (currentBlockHeightOnNerve >= txPo.getDeletedHeight()) {
                this.clearDB(ethTxHash);
                isReOfferQueue = false;
                ethResendHelper.clear(nerveTxHash);
                logger().info("[{}]transaction[{}]Confirmed exceeding{}Height, Remove queue, nerveheight: {}, nerver hash: {}", po.getTxType(), po.getTxHash(), EthConstant.ROLLBACK_NUMER, currentBlockHeightOnNerve, po.getNerveTxHash());
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
            case DOING:
                // Check if this has been initiated locallynerveWithdrawal transaction, if not available, initiate. This situation may occur with newly added virtual banks, but if you have missed the previous virtual bank initiativesdoingThe transaction is invalid, then this plan is invalid
                checkCurrentNodeSendWithdraw(po.getTxType(), nerveTxHash);
                // Transactions that have failed or are in the process of being multi signed do not need to be processed, from the queue andDBRemove from middle
                logger().info("{} Transactions that are being oversigned or have failed do not need to be processed, from the queue andDBRemove from middle[{}]", txPo.getStatus(), ethTxHash);
                isReOfferQueue = false;
                this.clearDB(ethTxHash);
                break;
            case RESEND:
                if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                    logger().warn("Resend more than thirty times, discard transaction, details: {}", po.toString());
                    isReOfferQueue = false;
                    this.clearDB(ethTxHash);
                    break;
                }
                // Transaction resend
                boolean success0 = this.reSend(txPo);
                if(success0) {
                    isReOfferQueue = false;
                    this.clearDB(ethTxHash);
                } else {
                    // Resend failed, wait for one round to resend again
                    po.setSkipTimes(1);
                    po.setResendTimes(po.getResendTimes() + 1);
                }
                break;
            case COMPLETED:
                if (!po.isValidateTx()) {
                    //Verify transaction again
                    BroadcastTxValidateStatus validate = validateBroadcastTxConfirmedInEthNet(po);
                    switch (validate) {
                        case RE_VALIDATE:
                            // Put it back in the queue and verify again
                            return isReOfferQueue;
                        case RE_SEND:
                            if(po.getResendTimes() > EthConstant.RESEND_TIME) {
                                logger().warn("Resend more than thirty times, discard transaction, details: {}", po.toString());
                                isReOfferQueue = false;
                                this.clearDB(ethTxHash);
                                break;
                            }
                            // Transaction resend
                            boolean success1 = this.reSend(txPo);
                            if(success1) {
                                // fromDBRemove the current transaction from the queue
                                isReOfferQueue = false;
                                this.clearDB(ethTxHash);
                            } else {
                                // Resend failed, wait for one round to resend again
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
                    // Recovery mechanism
                    if (txPo.getTxType() == HeterogeneousChainTxType.RECOVERY) {
                        realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
                        if (txPo.getNerveTxHash().startsWith(EthConstant.ETH_RECOVERY_I)) {
                            // The first step of restoring is to set it as completed
                            if (!EthContext.getConverterCoreApi().isSeedVirtualBankByCurrentNode()) {
                                ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash, null);
                            }
                            // After the first step of recovery execution is completed, the seed virtual bank executes the second step
                            if (EthContext.getConverterCoreApi().isSeedVirtualBankByCurrentNode()) {
                                logger().info("[{}]The first step of recovery has been completedETHtransaction[{}]Call the second step of recovery", po.getTxType(), ethTxHash);
                                EthRecoveryDto recoveryDto = ethTxStorageService.findRecoveryByNerveTxKey(nerveTxHash);
                                if (recoveryDto == null) {
                                    logger().info("The second step of resuming transactions has been sent out in advance");
                                    ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash, null);
                                    break;
                                }
                                String secondHash = ethRegister.getDockingImpl().forceRecovery(EthConstant.ETH_RECOVERY_II + realNerveTxHash, recoveryDto.getSeedManagers(), recoveryDto.getAllManagers());
                                if (StringUtils.isNotBlank(secondHash)) {
                                    logger().info("The second step of restoring the transaction has been sent out,hash: {}", secondHash);
                                    ethRegister.getDockingImpl().txConfirmedCompleted(ethTxHash, getCurrentBlockHeightOnNerve(), nerveTxHash, null);
                                }
                            }
                            break;
                        }
                    }
                    logger().info("[{}]SignedETHtransaction[{}]callNerveconfirm[{}]", po.getTxType(), ethTxHash, realNerveTxHash);
                    // The signed transaction will trigger a callbackNerve Core
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            po.getTxType(),
                            realNerveTxHash,
                            ethTxHash,
                            txPo.getBlockHeight(),
                            txPo.getTxTime(),
                            EthContext.MULTY_SIGN_ADDRESS,
                            txPo.getSigners(),
                            null);
                } catch (NulsException e) {
                    // Transaction already exists, waiting for confirmation to remove
                    if (TX_ALREADY_EXISTS_0.equals(e.getErrorCode()) || TX_ALREADY_EXISTS_1.equals(e.getErrorCode())) {
                        logger().info("Nervetransaction[{}]Exists, remove pending confirmation from queueETHtransaction[{}]", txPo.getNerveTxHash(), ethTxHash);
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
        logger().info("Detected the need to accelerate resend transactions, type: {}, ethHash: {}, nerveTxHash: {}", txType, unconfirmedTxPo.getTxHash(), nerveTxHash);
        // towardsETHNetwork request verification
        boolean isCompleted = ethParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
            // Send a transfer to oneself to cover this transactionnonce
            String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
            if (StringUtils.isNotBlank(overrideHash)) {
                logger().info("Transfer coverage transaction: {}, covered transactions: {}", overrideHash, txInfo.getTxHash());
            } else {
                logger().info("Unsuccessful issuance of overlay transaction");
            }
            return true;
        }
        if (logger().isDebugEnabled()) {
            logger().debug("Before acceleration: {}", txInfo.toString());
        }
        // Check if the address from which the transaction was sent matches the current virtual bank address. Otherwise, retrieve it againnonceSend transaction
        String from = txInfo.getFrom();
        String currentFrom = EthContext.ADMIN_ADDRESS;
        BigInteger nonce = txInfo.getNonce();
        if (!currentFrom.equals(from)) {
            nonce = ethWalletApi.getNonce(currentFrom);
        }
        txInfo.setNonce(nonce);
        // Improve transaction efficiencyprice, obtain the currentETHnetworkprice, and oldpriceTake the larger value, and then+2, i.e price = price + 2, maximum currentpriceof1.1times
        BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
        BigInteger maxCurrentGasPrice = new BigDecimal(currentGasPrice).multiply(EthConstant.NUMBER_1_DOT_1).toBigInteger();
        BigInteger oldGasPrice = txInfo.getGasPrice();
        if (maxCurrentGasPrice.compareTo(oldGasPrice) <= 0) {
            logger().info("Current transactiongasPriceReached maximum acceleration value, no further acceleration, waitingETHNetwork packaged transactions");
            return false;
        }
        BigInteger newGasPrice = oldGasPrice.compareTo(currentGasPrice) > 0 ? oldGasPrice : currentGasPrice;
        newGasPrice = newGasPrice.add(EthConstant.GWEI_2);
        newGasPrice = newGasPrice.compareTo(maxCurrentGasPrice) > 0 ? maxCurrentGasPrice : newGasPrice;

        txInfo.setGasPrice(newGasPrice);
        // Obtain account information
        HeterogeneousAccount account = ethRegister.getDockingImpl().getAccount(currentFrom);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // Verify business data
        String contractAddress = txInfo.getTo();
        String encodedFunction = txInfo.getData();
        EthCall ethCall = ethWalletApi.validateContractCall(currentFrom, contractAddress, encodedFunction);
        if (ethCall.isReverted()) {
            if (ConverterUtil.isCompletedTransaction(ethCall.getRevertReason())) {
                logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
                // Send a transfer to oneself to cover this transactionnonce
                String overrideHash = sendOverrideTransferTx(txInfo.getFrom(), txInfo.getGasPrice(), txInfo.getNonce());
                if (StringUtils.isNotBlank(overrideHash)) {
                    logger().info("Transfer coverage transaction: {}, covered transactions: {}", overrideHash, txInfo.getTxHash());
                } else {
                    logger().info("Unsuccessful issuance of overlay transaction");
                }
                return true;
            }
            logger().warn("[{}]Accelerated resend transaction verification failed, reason: {}", txType, ethCall.getRevertReason());
            return false;
        }
        EthSendTransactionPo newTxPo = ethWalletApi.callContractRaw(priKey, txInfo);
        String ethTxHash = newTxPo.getTxHash();
        // dockinglaunchethRecord transaction relationships during transactionsdbIn, and save the currently usednonceIn the relationship table, if there is any reasonpriceIf there is a need to resend the transaction without packaging it too low, the current one used will be taken outnonceResend transaction
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash, newTxPo);

        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        BeanUtils.copyProperties(unconfirmedTxPo, po);
        // Save unconfirmed transactions
        po.setTxHash(ethTxHash);
        po.setFrom(currentFrom);
        po.setTxType(txType);
        po.setCreateDate(System.currentTimeMillis());
        ethUnconfirmedTxStorageService.save(po);
        EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // Monitor the packaging status of this transaction
        ethListener.addListeningTx(ethTxHash);
        logger().info("Accelerated retransmissionETHOnline transaction successful, type: {}, details: {}", txType, po.superString());
        if (logger().isDebugEnabled()) {
            logger().debug("After acceleration: {}", newTxPo.toString());
        }
        return true;
    }

    private String sendOverrideTransferTx(String from, BigInteger gasPrice, BigInteger nonce) {
        try {
            // Check if the address from which the transaction was sent matches the current virtual bank address, otherwise ignore it
            String currentFrom = EthContext.ADMIN_ADDRESS;
            if (!currentFrom.equals(from)) {
                logger().info("The address where the transfer was sent to cover the transaction does not match the current virtual bank address, ignore");
                return null;
            }
            // Obtain account information
            HeterogeneousAccount account = ethRegister.getDockingImpl().getAccount(currentFrom);
            account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Get Networkgasprice
            BigInteger currentGasPrice = ethWalletApi.getCurrentGasPrice();
            BigInteger newGasPrice = gasPrice.compareTo(currentGasPrice) > 0 ? gasPrice : currentGasPrice;
            newGasPrice = newGasPrice.add(EthConstant.GWEI_3);
            if (logger().isDebugEnabled()) {
                logger().debug("Assembly covered transaction data, from: {}, to: {}, value: {}, gasLimit: {}, gasPrice: {}, nonce: {}",
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
            logger().warn("Transfer overlay transaction exception occurred, ignored", e);
            return null;
        }

    }

    /**
     * inspectethHas the transaction been packaged
     */
    private boolean checkPacked(String ethTxHash) throws Exception {
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        return txReceipt != null;
    }

    /**
     * Check if this has been initiated locallynerveWithdrawal transaction, if not available, initiate. This situation may occur with newly added virtual banks, but if you have missed the previous virtual bank initiativesdoingThe transaction is invalid, then this plan is invalid
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
                    logger().info("Detected that the current virtual bank node has not sent this message[Withdrawal]Transaction will be initiated after verification of accuracy[Withdrawal]transaction, Withdrawal data: {}", withdrawTxInfo.toString());
                    EthDocking docking = (EthDocking) ethRegister.getDockingImpl();
                    docking.createOrSignWithdrawTx(
                            nerveTxHash,
                            withdrawTxInfo.getToAddress(),
                            withdrawTxInfo.getValue(),
                            withdrawTxInfo.getAssetId(), false);
                }
            }
        } catch (Exception e) {
            logger().warn(String.format("The first transaction failed for the current virtual bank node, type: %s, nerveTxHash: %s", txType, nerveTxHash), e);
        }
    }

    /**
     * Verify recharge transactions
     */
    private boolean validateDepositTxConfirmedInEthNet(String ethTxHash, boolean ifContractAsset) throws Exception {
        boolean validateTx = false;
        do {
            TransactionReceipt receipt = ethWalletApi.getTxReceipt(ethTxHash);
            if (receipt == null) {
                logger().error("Verify transaction again[{}]Failed, unable to obtainreceipt", ethTxHash);
                break;
            }
            if (!receipt.isStatusOK()) {
                logger().error("Verify transaction again[{}]Failed,receiptIncorrect status", ethTxHash);
                break;
            } else if (ifContractAsset && (receipt.getLogs() == null || receipt.getLogs().size() == 0)) {
                logger().error("Verify transaction again[{}]Failed,receipt.LogIncorrect status", ethTxHash);
                break;
            }
            validateTx = true;
        } while (false);
        return validateTx;
    }

    /**
     * Transaction resend
     */
    private boolean reSend(EthUnconfirmedTxPo po) throws NulsException {
        boolean success;
        try {
            logger().info("[{}]transaction[{}]retransmission, details: {}", po.getTxType(), po.getTxHash(), po.toString());
            EthDocking docking = (EthDocking) ethRegister.getDockingImpl();
            switch (po.getTxType()) {
                case WITHDRAW:
                    String ethWithdrawHash = docking.createOrSignWithdrawTx(po.getNerveTxHash(), po.getTo(), po.getValue(), po.getAssetId(), false);
                    if(StringUtils.isBlank(ethWithdrawHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                //case RECOVERY:
                case CHANGE:
                    String ethChangesHash = docking.createOrSignManagerChangesTx(po.getNerveTxHash(), po.getAddAddresses(), po.getRemoveAddresses(), po.getOrginTxCount());
                    if(StringUtils.isBlank(ethChangesHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                case UPGRADE:
                    String ethUpgradeHash = docking.createOrSignUpgradeTx(po.getNerveTxHash());
                    if(StringUtils.isBlank(ethUpgradeHash)) {
                        logger().info("Nervetransaction[{}]Completed, no need to resend", po.getNerveTxHash());
                        ethResendHelper.clear(po.getNerveTxHash());
                    }
                    break;
                default:
                    break;
            }
            success = true;
        } catch (Exception e) {
            logger().error("Transaction resend failed, waiting for resend transaction", e);
            success = false;
        }
        return success;
    }

    /**
     * Verification sent toETHIs the online transaction confirmed? If there are any abnormal situations, resend the transaction according to the conditions
     */
    private BroadcastTxValidateStatus validateBroadcastTxConfirmedInEthNet(EthUnconfirmedTxPo po) throws Exception {

        BroadcastTxValidateStatus status;
        String ethTxHash = po.getTxHash();
        do {
            TransactionReceipt receipt = ethWalletApi.getTxReceipt(ethTxHash);
            if (receipt == null) {
                boolean timeOut = System.currentTimeMillis() - po.getCreateDate() > EthConstant.MINUTES_20;
                /*
                There will be no repeated withdrawals, as this is a call to the contract withdrawal function, the contract business guarantees that it will only be withdrawn once
                if(po.getTxType() == WITHDRAW) {
                    // If the timeout has expired, then every15Verify once per round, otherwise, every3One round of verification
                    int skipTimes = timeOut ? 15 : 3;
                    po.setSkipTimes(skipTimes);
                    status = BroadcastTxValidateStatus.RE_VALIDATE;
                    break;
                }
                */
                logger().error("Verify transaction again[{}]Failed, unable to obtainreceipt", ethTxHash);
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
                logger().error("Verify transaction again[{}]Failed,receiptIncorrect status", ethTxHash);
                break;
            } else if (receipt.getLogs() == null || receipt.getLogs().size() == 0) {
                status = BroadcastTxValidateStatus.RE_SEND;
                logger().error("Verify transaction again[{}]Failed,receipt.LogIncorrect status", ethTxHash);
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
