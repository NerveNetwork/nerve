/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

package io.nuls.transaction.service.impl;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.*;
import io.nuls.transaction.model.po.TransactionNetPO;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.SwapCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.service.TxPackageService;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.ConfirmedTxStorageService;
import io.nuls.transaction.storage.UnconfirmedTxStorageService;
import io.nuls.transaction.utils.LoggerUtil;
import io.nuls.transaction.utils.TxUtil;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static io.nuls.transaction.constant.TxConstant.CACHED_SIZE;
import static io.nuls.transaction.constant.TxConstant.VERIFY;
import static io.nuls.transaction.constant.TxContext.BLOCK_TX_TIME_RANGE_SEC;

/**
 * @author: Charlie
 * @date: 2019/11/18
 */
@Component
public class TxPackageServiceImpl implements TxPackageService {

    @Autowired
    private PackablePool packablePool;

    @Autowired
    private TxService txService;

    @Autowired
    private ConfirmedTxStorageService confirmedTxStorageService;

    @Autowired
    private UnconfirmedTxStorageService unconfirmedTxStorageService;

    private ExecutorService verifySignExecutor = ThreadUtils.createThreadPool(Runtime.getRuntime().availableProcessors(),
            CACHED_SIZE, new NulsThreadFactory(TxConstant.BASIC_VERIFY_TX_SIGN_THREAD));
    private String INITIAL_STATE_ROOT = "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421";

    /**
     * package deal
     * Suitable for blockchain transactions without smart contracts
     *
     * @param chain
     * @param endtimestamp  Packaging deadline
     * @param maxTxDataSize Maximum transaction data for all transactionssize
     * @return
     */
    @Override
    public TxPackage packageBasic(Chain chain, long endtimestamp, long maxTxDataSize, long blockTime, String preStateRoot) {
        chain.getPackageLock().lock();
        long startTime = NulsDateUtils.getCurrentTimeMillis();
        long packableTime = endtimestamp - startTime;
        long height = chain.getBestBlockHeight() + 1;
        List<TxPackageWrapper> packingTxList = new ArrayList<>();
        //Record orphan transactions in the ledger,Filter out when returning to consensus,Because when repackaging due to height changes,Need to restore to the queue to be packaged
        Set<TxPackageWrapper> orphanTxSet = new HashSet<>();
        //Assemble unified validation parameter data,keyUnify validators for each modulecmd
        Map<String, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        NulsLogger log = chain.getLogger();
        try {
            log.info("[Package start] Block time:{}, - height:{}, -Total available time for packaging：{}, -Packable capacity：{}B , - Current queue transactions to be packagedhashnumber:{}, - Actual number of transactions in the queue to be packaged:{}",
                    blockTime, height, packableTime, maxTxDataSize, packablePool.packableHashQueueSize(chain), packablePool.packableTxMapSize(chain));
            if (packableTime <= TxConstant.BASIC_PACKAGE_RESERVE_TIME) {
                //Directly hit the empty block
                return null;
            }
            long collectTime = 0L, batchModuleTime = 0L;
            long collectTimeStart = NulsDateUtils.getCurrentTimeMillis();
            Result rs = collectProcessTransactionsBasic(chain, blockTime, endtimestamp, maxTxDataSize, packingTxList, orphanTxSet, moduleVerifyMap, preStateRoot);
            if (rs.isFailed()) {
                return null;
            }
            Map<String, Object> dataMap = (Map<String, Object>) rs.getData();
            // SwapExecution status of transactions and System transactions generated
            String stateRoot = (String) dataMap.get("stateRoot");
            List<String> swapTxList = (List<String>) dataMap.get("swapTxList");
            if (log.isDebugEnabled()) {
                collectTime = NulsDateUtils.getCurrentTimeMillis() - collectTimeStart;
            }
            //Module verification transaction
            long batchStart = NulsDateUtils.getCurrentTimeMillis();
            List<String> allNewlyList = txService.txModuleValidatorPackable(chain, moduleVerifyMap, packingTxList, orphanTxSet, height, blockTime);
            //Total time for module unified verification usage
            if (log.isDebugEnabled()) {
                batchModuleTime = NulsDateUtils.getCurrentTimeMillis() - batchStart;
            }

            List<String> packableTxs = new ArrayList<>();
            Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
            Map<NulsHash, Integer> txPackageOrphanMap = chain.getTxPackageOrphanMap();
            while (iterator.hasNext()) {
                TxPackageWrapper txPackageWrapper = iterator.next();
                Transaction tx = txPackageWrapper.getTx();
                NulsHash hash = tx.getHash();
                if (txPackageOrphanMap.containsKey(hash)) {
                    txPackageOrphanMap.remove(hash);
                }
                try {
                    String txHex = RPCUtil.encode(tx.serialize());
                    packableTxs.add(txHex);
                } catch (Exception e) {
                    txService.clearInvalidTx(chain, tx);
                    iterator.remove();
                    throw new NulsException(e);
                }
            }


            // Processing transactions generated internally when packaging is required
            if (!swapTxList.isEmpty()) {
                packableTxs.addAll(swapTxList);
            }
            if (!allNewlyList.isEmpty()) {
                packableTxs.addAll(allNewlyList);
            }

            //Add the orphan transaction back to the pending packaging queue
            txService.putBackPackablePool(chain, orphanTxSet);
            if (chain.getProtocolUpgrade().get()) {
                processProtocolUpgrade(chain, packingTxList);
                return null;
            }

            long current = NulsDateUtils.getCurrentTimeMillis();
            if (endtimestamp - current < TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME) {
                //overtime,Leave for final data assembly andRPCInsufficient transmission time
                log.error("getPackableTxs time out, endtimestamp:{}, current:{}, endtimestamp-current:{}, reserveTime:{}",
                        endtimestamp, current, endtimestamp - current, TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME);
                throw new NulsException(TxErrorCode.PACKAGE_TIME_OUT);
            }
            long totalTime = NulsDateUtils.getCurrentTimeMillis() - startTime;
            log.debug("[Packaging time statistics] Total available:{}ms, Overall execution:{}, Total remaining:{}, Collect transactions and verify ledgers:{}, Unified module verification:{}",
                    packableTime, totalTime, packableTime - totalTime, collectTime, batchModuleTime);

            log.info("[Package end] - height:{} - The number of packaged transactions this time:{} - Current queue transactions to be packagedhashnumber:{}, - Actual number of transactions in the queue to be packaged:{}" + TxUtil.nextLine(),
                    height, packableTxs.size(), packablePool.packableHashQueueSize(chain), packablePool.packableTxMapSize(chain));
            TxPackage txPackage = new TxPackage();
            txPackage.setList(packableTxs);
            txPackage.setStateRoot(stateRoot);
            return txPackage;
        } catch (Exception e) {
            log.error(e);
            //Packable transactions,Orphan Trading,Add it all back
            txService.putBackPackablePool(chain, packingTxList, orphanTxSet);
            return null;
        } finally {
            chain.getPackageLock().unlock();
        }
    }


    /**
     * Get packaged transactions, And verify the ledger
     *
     * @return false Indicates direct output of empty blocks
     * @throws NulsException
     */
    private Result collectProcessTransactionsBasic(Chain chain, long blockTime, long endtimestamp, long maxTxDataSize, List<TxPackageWrapper> packingTxList,
                                                    Set<TxPackageWrapper> orphanTxSet, Map<String, List<String>> moduleVerifyMap, String preStateRoot) throws NulsException {

        //SWAPNotification identifier,The first one that appearsSWAPWhen the transaction passes and the validator is called,If there is, only notify on the first attempt.
        boolean swapNotify = false;
        //The height of this packaging
        long blockHeight = chain.getBestBlockHeight() + 1;
        //Send batch verification to the ledger modulecoinDataIdentification of
        LedgerCall.coinDataBatchNotify(chain);
        int allCorssTxCount = 0, batchCorssTxCount = 0;
        long totalSize = 0L, totalSizeTemp = 0L;
        List<String> batchProcessList = new ArrayList<>();
        Set<String> duplicatesVerify = new HashSet<>();
        List<TxPackageWrapper> currentBatchPackableTxs = new ArrayList<>();
        NulsLogger log = chain.getLogger();
        // The time to package transactions in this block, Within a certain range of block time.
        long txTimeRangStart = blockTime - BLOCK_TX_TIME_RANGE_SEC;
        long txTimeRangEnd = blockTime + BLOCK_TX_TIME_RANGE_SEC;
        // SWAPThe system transaction set generated by the module
        List<String> swapGenerateTxs = new ArrayList<>();
        // The original transaction corresponding to the system transactionhashaggregate（SWAPSystem transactions generated by modules）
        List<String> swapOriginTxHashList = new ArrayList<>();
        String newStateRoot = preStateRoot;

        for (int index = 0; ; index++) {
            long currentTimeMillis = NulsDateUtils.getCurrentTimeMillis();
            long currentReserve = endtimestamp - currentTimeMillis;
            if (currentReserve <= TxConstant.BASIC_PACKAGE_RESERVE_TIME) {
                if (log.isDebugEnabled()) {
                    log.info("Get transaction time up to,Entering the module validation phase: currentTimeMillis:{}, -endtimestamp:{}, -offset:{}, -remaining:{}",
                            currentTimeMillis, endtimestamp, TxConstant.BASIC_PACKAGE_RESERVE_TIME, currentReserve);
                }
                backTempPackablePool(chain, currentBatchPackableTxs);
                break;
            }
            if (currentReserve < TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME) {
                //overtime,Leave for final data assembly andRPCInsufficient transmission time
                log.error("getPackableTxs time out, endtimestamp:{}, current:{}, endtimestamp-current:{}, reserveTime:{}",
                        endtimestamp, currentTimeMillis, currentReserve, TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME);
                backTempPackablePool(chain, currentBatchPackableTxs);
                throw new NulsException(TxErrorCode.PACKAGE_TIME_OUT);
            }
            if (chain.getProtocolUpgrade().get()) {
                log.info("Protocol Upgrade Package stop -chain:{} -best block height", chain.getChainId(), chain.getBestBlockHeight());
                backTempPackablePool(chain, currentBatchPackableTxs);
                //Put back packable transactions and orphans
                txService.putBackPackablePool(chain, packingTxList, orphanTxSet);
                return Result.getFailed(TxErrorCode.DATA_ERROR);
            }
            if (packingTxList.size() >= TxConstant.BASIC_PACKAGE_TX_MAX_COUNT) {
                if (log.isDebugEnabled()) {
                    log.info("Obtaining transaction completedmax count,Entering the module validation phase: currentTimeMillis:{}, -endtimestamp:{}, -offset:{}, -remaining:{}",
                            currentTimeMillis, endtimestamp, TxConstant.BASIC_PACKAGE_RESERVE_TIME, endtimestamp - currentTimeMillis);
                }
                backTempPackablePool(chain, currentBatchPackableTxs);
                break;
            }
            int batchProcessListSize = batchProcessList.size();
            boolean process = false;
            Transaction tx = null;
            boolean maxDataSize = false;
            try {
                tx = packablePool.poll(chain);
                if (tx == null && batchProcessListSize == 0) {
                    // 6.28 Reset to Pack No Transactions,Waiting mode
                    Thread.sleep(10L);
                    continue;
                } else if (tx == null && batchProcessListSize > 0) {
                    //Meet the conditions for processing this batch
                    process = true;
                } else if (tx != null) {
                    if (null != confirmedTxStorageService.getTx(chain.getChainId(), tx.getHash())) {
                        //Transaction confirmed
                        txService.baseClearTx(chain, tx);
                        continue;
                    }
                    if (!duplicatesVerify.add(tx.getHash().toHex())) {
                        //If you don't join, it means it already exists
                        continue;
                    }
                    long txSize = tx.size();
                    if ((totalSizeTemp + txSize) > maxTxDataSize) {
                        packablePool.offerFirstOnlyHash(chain, tx);
                        log.info("The transaction has reached its maximum capacity, actual value: {}, totalSizeTemp:{}, Current transactionsize：{} - Reserve maximum valuemaxTxDataSize:{}, txhash:{}", totalSize, totalSizeTemp, txSize, maxTxDataSize, tx.getHash().toHex());
                        maxDataSize = true;
                        if (batchProcessListSize == 0) {
                            break;
                        }
                        //Meet the conditions for processing this batch
                        process = true;
                    } else {
                        if (ModuleE.CS.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
                            // If it is a consensus module transaction, Verify transaction time first, Within a certain range of block time
                            long txTime = tx.getTime();
                            if (txTime < txTimeRangStart || txTime > txTimeRangEnd) {
                                chain.getLogger().error("[TxPackage] The transction time does not match the block time, blockTime:{}, txTime:{}, range:±{}",
                                        blockTime, txTime, BLOCK_TX_TIME_RANGE_SEC);
                                continue;
                            }
                        }
                        //Limit the number of cross chain transactions
                        if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
                            if (allCorssTxCount + (++batchCorssTxCount) >= TxConstant.BASIC_PACKAGE_CROSS_TX_MAX_COUNT) {
                                //Limit the total number of cross chain transactions contained in a single block. If the maximum number of cross chain transactions is exceeded, put it back, Then stop obtaining transactions
                                packablePool.add(chain, tx);
                                if (batchProcessListSize == 0) {
                                    break;
                                }
                                //Meet the conditions for processing this batch
                                process = true;
                            }
                        }
                        String txHex;
                        try {
                            txHex = RPCUtil.encode(tx.serialize());
                        } catch (Exception e) {
                            log.warn(e.getMessage(), e);
                            log.error("Discard acquisitionhexWrong transaction, txHash:{}, - type:{}, - time:{}", tx.getHash().toHex(), tx.getType(), tx.getTime());
                            txService.clearInvalidTx(chain, tx);
                            continue;
                        }
                        TxPackageWrapper txPackageWrapper = new TxPackageWrapper(tx, index, txHex);
                        if (tx.getType() == TxType.FINAL_QUOTATION) {
                            packingTxList.add(txPackageWrapper);
                        } else {
                            batchProcessList.add(txHex);
                            currentBatchPackableTxs.add(txPackageWrapper);
                            if (batchProcessList.size() == TxConstant.BASIC_PACKAGE_VERIFY_COINDATA_BATCH) {
                                //Meet the conditions for processing this batch
                                process = true;
                            }
                        }
                    }
                    //Total size plus the size of each transaction in the current batch
                    totalSizeTemp += txSize;
                }
                if (process) {
                    verifyLedgerBasic(chain, batchProcessList, currentBatchPackableTxs, orphanTxSet);
                    Iterator<TxPackageWrapper> it = currentBatchPackableTxs.iterator();
                    while (it.hasNext()) {
                        TxPackageWrapper txPackageWrapper = it.next();
                        Transaction transaction = txPackageWrapper.getTx();
                        TxRegister txRegister = TxManager.getTxRegister(chain, transaction.getType());
                        String moduleCode = txRegister.getModuleCode();
                        boolean isSwapTx = moduleCode.equals(ModuleE.SW.abbr);
                        if (isSwapTx) {
                            // appearSWAPtransaction,And the notification identifier isfalse,Then call the notification first
                            if (!swapNotify) {
                                log.info("[{}]startSwapmodule-pack, {}, {}", blockHeight, blockTime, preStateRoot);
                                SwapCall.swapBatchBegin(chain, blockHeight, blockTime, preStateRoot, 0);
                                swapNotify = true;
                            }
                            try {
                                // Call ExecutionSWAPtransaction
                                log.info("[{}]handleSwaptransaction-pack, {}, {}", blockHeight, blockTime, txPackageWrapper.getTxHex());
                                Map<String, Object> invokeContractRs = SwapCall.invoke(chain, txPackageWrapper.getTxHex(), blockHeight, blockTime, 0);
                                List<String> txList = (List<String>) invokeContractRs.get("txList");
                                if (txList != null && !txList.isEmpty()) {
                                    log.info("[{}]Obtain generated transactions_Swapmodule-pack, {}, {}", blockHeight, blockTime, Arrays.toString(txList.toArray()));
                                    swapGenerateTxs.addAll(txList);
                                    String txHash = transaction.getHash().toString();
                                    for (int i = 0, size = txList.size(); i < size; i++) {
                                        swapOriginTxHashList.add(txHash);
                                    }
                                }
                            } catch (NulsException e) {
                                chain.getLogger().error(e);
                                txService.clearInvalidTx(chain, transaction);
                                continue;
                            }
                        }
                        totalSize += transaction.getSize();
                        //Calculate the number of cross chain transactions
                        if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(transaction.getType()))) {
                            allCorssTxCount++;
                        }
                        //According to the unified validator name of the module, group all transactions and prepare for unified verification of each module
                        TxUtil.moduleGroups(moduleVerifyMap, txRegister, RPCUtil.encode(transaction.serialize()));
                    }
                    //Update to the latest total block transaction size
                    totalSizeTemp = totalSize;
                    packingTxList.addAll(currentBatchPackableTxs);
                    //Batch end reset data
                    batchProcessList.clear();
                    currentBatchPackableTxs.clear();
                    batchCorssTxCount = 0;
                    if (maxDataSize) {
                        break;
                    }
                }
            } catch (Exception e) {
                currentBatchPackableTxs.clear();
                log.error("Packaging transaction exception, txHash:{}, - type:{}, - time:{}", tx.getHash().toHex(), tx.getType(), tx.getTime());
                log.error(e);
                continue;
            }
        }
        /** SWAP When the notification identifier istrue, It indicates that there isSWAPTransaction called for execution*/
        if (swapNotify) {
            Log.info("Collect transactions");
            Map<String, Object> batchEnd = SwapCall.swapBatchEnd(chain, blockHeight, 0);
            newStateRoot = (String) batchEnd.get("stateRoot");
            log.info("[{}]finishSwapmodule-pack, {}, {}", blockHeight, blockTime, newStateRoot);
        }
        if (log.isDebugEnabled()) {
            log.debug("Collect transactions -count:{} - data size:{}",
                    packingTxList.size(), totalSize);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("stateRoot", newStateRoot);
        result.put("swapTxList", swapGenerateTxs);
        return Result.getSuccess(result);
    }

    /**
     * packing verify ledger
     *
     * @param chain
     * @param batchProcessList
     * @param currentBatchPackableTxs
     * @param orphanTxSet
     * @throws NulsException
     */
    private void verifyLedgerBasic(Chain chain, List<String> batchProcessList, List<TxPackageWrapper> currentBatchPackableTxs,
                                   Set<TxPackageWrapper> orphanTxSet) throws NulsException {
        //Start processing
        Map verifyCoinDataResult = LedgerCall.verifyCoinDataBatchPackaged(chain, batchProcessList);
        List<String> failHashs = (List<String>) verifyCoinDataResult.get("fail");
        List<String> orphanHashs = (List<String>) verifyCoinDataResult.get("orphan");
        if (!failHashs.isEmpty() || !orphanHashs.isEmpty()) {
            chain.getLogger().error("Package verify Ledger fail tx count:{}", failHashs.size());
            chain.getLogger().error("Package verify Ledger orphan tx count:{}", orphanHashs.size());

            Iterator<TxPackageWrapper> it = currentBatchPackableTxs.iterator();
            removeAndGo:
            while (it.hasNext()) {
                TxPackageWrapper txPackageWrapper = it.next();
                Transaction transaction = txPackageWrapper.getTx();
                //Remove transactions with failed ledger verification
                for (String hash : failHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        txService.clearInvalidTx(chain, transaction);
                        it.remove();
                        continue removeAndGo;
                    }
                }
                //Remove orphan transactions, Simultaneously placing orphan transactions into the orphan pool
                for (String hash : orphanHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        txService.addOrphanTxSet(chain, orphanTxSet, txPackageWrapper);
                        it.remove();
                        continue removeAndGo;
                    }
                }
            }
        }
    }

    /**
     * If there is a protocol upgrade during packaging, Put the transaction back into the unprocessed queue,Reprocessing
     */
    private void processProtocolUpgrade(Chain chain, List<TxPackageWrapper> packingTxList) throws NulsException {
        //Protocol upgrade directly hits empty blocks,Retrieved transactions are placed in reverse order in the new transaction processing queue
        int size = packingTxList.size();
        for (int i = size - 1; i >= 0; i--) {
            TxPackageWrapper txPackageWrapper = packingTxList.get(i);
            Transaction tx = txPackageWrapper.getTx();
            //Perform basic transaction verification
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (null == txRegister) {
                chain.getLogger().error(new NulsException(TxErrorCode.TX_TYPE_INVALID));
                continue;
            }
            txService.baseValidateTx(chain, tx, txRegister);
            chain.getUnverifiedQueue().addLast(new TransactionNetPO(txPackageWrapper.getTx()));
        }
    }

    /**
     * When packaging,Retrieve transaction stages from the queue to be packaged,Will generate a temporary transaction list,When interrupting the acquisition of transactions, it is necessary to return the remaining temporary transactions to the queue for packaging
     */
    private void backTempPackablePool(Chain chain, List<TxPackageWrapper> listTx) {
        for (int i = listTx.size() - 1; i >= 0; i--) {
            packablePool.offerFirstOnlyHash(chain, listTx.get(i).getTx());
        }
    }

    /**
     * Only one transaction is allowed in the verification block, and there cannot be multiple transactions
     */
    public void verifySysTxCount(Set<Integer> onlyOneTxTypes, int type) throws NulsException {
        switch (type) {
            case TxType.COIN_BASE:
            case TxType.YELLOW_PUNISH:
                if (!onlyOneTxTypes.add(type)) {
                    throw new NulsException(TxErrorCode.CONTAINS_MULTIPLE_UNIQUE_TXS);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean verifyBlockTransations(Chain chain, List<String> txStrList, String blockHeaderStr, String preStateRoot) throws Exception {
        long s1 = NulsDateUtils.getCurrentTimeMillis();
        NulsLogger log = chain.getLogger();
        BlockHeader blockHeader = TxUtil.getInstanceRpcStr(blockHeaderStr, BlockHeader.class);
        long blockHeight = blockHeader.getHeight();
        long blockTime = blockHeader.getTime();
        LoggerUtil.LOG.debug("[Inspection area-Block time]:{}, height:{}", blockTime, blockHeight);
        boolean isLogDebug = log.isDebugEnabled();
        if (isLogDebug) {
            log.debug("[Verify block transactions] start -----height:{} -----Number of block transactions:{}", blockHeight, txStrList.size());
        }
        //Only one transaction is allowed in the verification block, and there cannot be multiple transactions
        Set<Integer> onlyOneTxTypes = new HashSet<>();
        //SWAPNotification identifier,The first one that appearsSWAPWhen the transaction passes and the validator is called,If there is, only notify on the first attempt.
        boolean swapNotify = false;
        List<TxVerifyWrapper> txList = new ArrayList<>();
        //Assemble unified validation parameter data,keyUnify validators for each modulecmd
        Map<String, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        List<byte[]> keys = new ArrayList<>();
        int totalSize = 0;
        // The time to package transactions in this block, Within a certain range of block time.
        long txTimeRangStart = blockTime - BLOCK_TX_TIME_RANGE_SEC;
        long txTimeRangEnd = blockTime + BLOCK_TX_TIME_RANGE_SEC;
        // SWAPThe system transaction set generated by the module
        List<String> swapGenerateTxs = new ArrayList<>();
        List<String> swapGenerateTxsByVerify = new ArrayList<>();

        log.info("Block transaction list-validate: {}", Arrays.toString(txStrList.toArray()));
        for (String txStr : txStrList) {
            Transaction tx = TxUtil.getInstanceRpcStr(txStr, Transaction.class);
            int type = tx.getType();
            TxRegister txRegister = TxManager.getTxRegister(chain, type);
            boolean isUnSystemSwapTx = TxManager.isUnSystemSwap(txRegister);
            if (isUnSystemSwapTx) {
                // appearSWAPtransaction,And the notification identifier isfalse,Then call the notification first
                if (!swapNotify) {
                    log.info("[{}]startSwapmodule-validate, {}, {}", blockHeight, blockTime, preStateRoot);
                    SwapCall.swapBatchBegin(chain, blockHeight, blockTime, preStateRoot, 1);
                    swapNotify = true;
                }
                // Call ExecutionSWAPtransaction
                log.info("[{}]handleSwaptransaction-validate, {}, {}", blockHeight, blockTime, txStr);
                Map<String, Object> invokeContractRs = SwapCall.invoke(chain, txStr, blockHeight, blockTime, 1);
                List<String> swapTxList = (List<String>) invokeContractRs.get("txList");
                if (swapTxList != null && !swapTxList.isEmpty()) {
                    log.info("[{}]Obtain generated transactions_Swapmodule-validate, {}, {}", blockHeight, blockTime, Arrays.toString(swapTxList.toArray()));
                    swapGenerateTxsByVerify.addAll(swapTxList);
                }
            } else if (TxManager.isSystemSwap(txRegister)) {
                // Filter outSWAPSystem transaction set
                swapGenerateTxs.add(txStr);
            }
            if (ModuleE.CS.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(type))) {
                // If it is a consensus module transaction, Verify transaction time first, Within a certain range of block time
                long txTime = tx.getTime();
                if (txTime < txTimeRangStart || txTime > txTimeRangEnd) {
                    chain.getLogger().error("[verifyBlockTx] The transction time does not match the block time, blockTime:{}, txTime:{}, range:±{}",
                            blockTime, txTime, BLOCK_TX_TIME_RANGE_SEC);
                    throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
                }
            }
            totalSize += tx.size();
            txList.add(new TxVerifyWrapper(tx, txStr));
            verifySysTxCount(onlyOneTxTypes, type);
            if (null == txRegister) {
                log.error("txType:{}", type);
                throw new NulsException(TxErrorCode.TX_TYPE_INVALID);
            }
            keys.add(tx.getHash().getBytes());
            TxUtil.moduleGroups(moduleVerifyMap, txRegister, txStr);
        }
        boolean isUncfm = isTxConfirmed(chain, keys);
        if (!isUncfm) {
            throw new NulsException(TxErrorCode.TX_CONFIRMED);
        }

        //Verify transactions that are not available locally
        List<Future<Boolean>> futures = new ArrayList<>();
        verifyNonLocalTxs(chain, futures, keys, txList, blockHeight);
        keys = null;

        //Verify ledger
        long coinDataV = NulsDateUtils.getCurrentTimeMillis();
        if (!LedgerCall.verifyBlockTxsCoinData(chain, txStrList, blockHeight)) {
            throw new NulsException(TxErrorCode.TX_LEDGER_VERIFY_FAIL);
        }
        if (isLogDebug) {
            log.debug("[Verify block transactions] coinData -Time from the start of the method:{},-Verification time:{}",
                    NulsDateUtils.getCurrentTimeMillis() - s1, NulsDateUtils.getCurrentTimeMillis() - coinDataV);
        }
        //Module Unified Verifier
        long moduleV = NulsDateUtils.getCurrentTimeMillis();
        Iterator<Map.Entry<String, List<String>>> it = moduleVerifyMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            List<String> txHashList = TransactionCall.txModuleValidator(chain,
                    entry.getKey(), entry.getValue(), blockHeaderStr);
            if (txHashList != null && txHashList.size() > 0) {
                log.error("batch module verify fail, module-code:{},  return count:{}", entry.getKey(), txHashList.size());
                throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
            }
        }

        // The validation block generated by this nodestateRoot
        String verifyStateRoot = preStateRoot;
        /** SWAP When the notification identifier istrue, It indicates that there isSWAPTransaction called for execution*/
        if (swapNotify) {
            Log.info("Verify Block");
            Map<String, Object> batchEnd = SwapCall.swapBatchEnd(chain, blockHeight, 1);
            verifyStateRoot = (String) batchEnd.get("stateRoot");
            log.info("[{}]finishSwapmodule-validate, {}, {}", blockHeight, blockTime, verifyStateRoot);
        }
        // packagedstateRoot
        String stateRoot = RPCUtil.encode(blockHeader.getExtendsData().getStateRoot());
        // When packagedstateRootyesnullWhen, it indicates that there is no block in the blockswaptransaction
        if (stateRoot == null) {
            // At this point, verify the block'sstateRootIf it is not empty, it must be the initialStateRoot
            if (verifyStateRoot != null && !INITIAL_STATE_ROOT.equals(verifyStateRoot)) {
                log.warn("swap stateRoot error.");
                throw new NulsException(TxErrorCode.SWAP_VERIFY_STATE_ROOT_FAIL);
            }
        } else if (!stateRoot.equals(verifyStateRoot)) {
            log.warn("swap stateRoot error. Package: {}, Verify: {}", stateRoot, verifyStateRoot);
            throw new NulsException(TxErrorCode.SWAP_VERIFY_STATE_ROOT_FAIL);
        }
        if (!this.swapGenerateTxConsistency(swapGenerateTxs, swapGenerateTxsByVerify)) {
            log.warn("swap generate txs error.");
            throw new NulsException(TxErrorCode.SWAP_VERIFY_GENERATE_TXS_FAIL);
        }

        // Verify transactions generated during packaging
        // grouping Group all transactions Include original transactions and transactions generated during packaging
        Map<String, List> packProduceCallMap = packProduceProcess(chain, txList, blockHeight);
        verifyNewProduceTx(chain, packProduceCallMap, blockHeight, blockHeader.getTime());

        if (isLogDebug) {
            log.debug("[Verify block transactions] Module Unified Verification Time:{}", NulsDateUtils.getCurrentTimeMillis() - moduleV);
            log.debug("[Verify block transactions] Unified module verification -Time from the start of the method:{}", NulsDateUtils.getCurrentTimeMillis() - s1);
        }
        //inspect[Verify transactions that are not available locally]Multithreaded processing results for
        checkbaseValidateResult(chain, futures);

        if (isLogDebug) {
            log.debug("[Verify block transactions] Total execution time:{}, Total transaction amountsize:{} - height:{} - Number of block transactions:{}, " + TxUtil.nextLine(),
                    NulsDateUtils.getCurrentTimeMillis() - s1, totalSize, blockHeight, txStrList.size());
        }
        return true;
    }

    private boolean swapGenerateTxConsistency(List<String> swapGenerateTxs, List<String> swapGenerateTxsByVerify) {
        if (swapGenerateTxs.size() != swapGenerateTxsByVerify.size()) {
            return false;
        }
        for (int i = 0, size = swapGenerateTxs.size(); i < size; i++) {
            if (!swapGenerateTxs.get(i).equals(swapGenerateTxsByVerify.get(i))) {
                return false;
            }
        }
        return true;
    }


    private Map<String, List> packProduceProcess(Chain chain, List<TxVerifyWrapper> txList, long height) {
        Iterator<TxVerifyWrapper> iterator = txList.iterator();
        // K: modulecode V:{k: Original transactionhash, v:Original transaction string}
        Map<String, List> packProduceCallMap = TxManager.getGroup(chain);

        while (iterator.hasNext()) {
            TxVerifyWrapper txVerifyWrapper = iterator.next();
            Transaction tx = txVerifyWrapper.getTx();
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getPackProduce()) {
                List<TxVerifyWrapper> listCallTxs = packProduceCallMap.computeIfAbsent(txRegister.getModuleCode(), k -> new ArrayList<>());
                listCallTxs.add(txVerifyWrapper);
                LoggerUtil.LOG.debug("Internal module processing generation during block validation, Original transactionhash:{}, height:{}, moduleCode:{}", tx.getHash().toHex(), height, txRegister.getModuleCode());
            }
        }
        return packProduceCallMap;
    }

    private void verifyNewProduceTx(Chain chain, Map<String, List> packProduceCallMap, long height, long blockTime) throws NulsException {
        Iterator<Map.Entry<String, List>> it = packProduceCallMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List> entry = it.next();
            List<TxVerifyWrapper> moduleList = entry.getValue();
            List<String> txHexList = new ArrayList<>();
            for (TxVerifyWrapper wrapper : moduleList) {
                txHexList.add(wrapper.getTxStr());
            }
            String moduleCode = entry.getKey();
            // No exceptions indicate verification passed
            TransactionCall.packProduce(chain, BaseConstant.TX_PACKPRODUCE, moduleCode, txHexList, height, blockTime, VERIFY);
        }
    }

    /**
     * Verify if there are confirmed transactions
     */
    private boolean isTxConfirmed(Chain chain, List<byte[]> keys) {
        List<byte[]> confirmedList = confirmedTxStorageService.getExistTxs(chain.getChainId(), keys);
        if (!confirmedList.isEmpty()) {
            NulsLogger log = chain.getLogger();
            log.error("There are confirmed transactions. count:{}", confirmedList.size());
            try {
                for (byte[] cfmtx : confirmedList) {
                    log.error("confirmed hash:{}", TxUtil.getTransaction(cfmtx).getHash().toHex());
                }
            } finally {
                log.error("Show confirmed transaction deserialize fail");
                return false;
            }
        }
        return true;
    }

    /**
     * Enable multithreading For transactions in unconfirmed databases that are not local, Conduct basic validation
     */
    private void verifyNonLocalTxs(Chain chain, List<Future<Boolean>> futures, List<byte[]> keys, List<TxVerifyWrapper> txList, long height) {
        //In the transaction of obtaining blocks, Transactions that exist in unconfirmed databases
        List<String> unconfirmedList = unconfirmedTxStorageService.getExistKeysStr(chain.getChainId(), keys);
        Set<String> set = new HashSet<>();
        set.addAll(unconfirmedList);
        unconfirmedList = null;
        for (TxVerifyWrapper txVerifyWrapper : txList) {
            Transaction tx = txVerifyWrapper.getTx();
            //Being able to join indicates that there is no confirmation yet,Then it needs to be handled
            if (set.add(tx.getHash().toHex())) {
                //Perform basic validation without confirmation
                //Multi threaded processing of individual transactions
                Future<Boolean> res = verifySignExecutor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        try {
                            //Verify only the basic content of a single transaction(TXModule Local Validation)
                            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
                            if (null == txRegister) {
                                throw new NulsException(TxErrorCode.TX_TYPE_INVALID);
                            }
                            txService.baseValidateTx(chain, tx, txRegister, height);
                        } catch (Exception e) {
                            chain.getLogger().error("batchVerify failed, single tx verify failed. hash:{}, -type:{}", tx.getHash().toHex(), tx.getType());
                            try {
                                chain.getLogger().error("-------tx from------");
                                for (CoinFrom from : tx.getCoinDataInstance().getFrom()) {
                                    chain.getLogger().error(from.toString());
                                }

                                chain.getLogger().error("-------tx to------");
                                for (CoinTo to : tx.getCoinDataInstance().getTo()) {
                                    chain.getLogger().error(to.toString());
                                }
                            } catch (NulsException ee) {
                                e.printStackTrace();
                            }

                            chain.getLogger().error(e);
                            return false;
                        }
                        return true;
                    }
                });
                futures.add(res);
            }
        }
    }

    /**
     * Check the basic verification results
     */
    private void checkbaseValidateResult(Chain chain, List<Future<Boolean>> futures) throws NulsException {
        NulsLogger log = chain.getLogger();
        try {
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    log.error("batchVerify failed, single tx verify failed");
                    throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
                }
            }
        } catch (InterruptedException e) {
            log.error(e);
            throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
        } catch (ExecutionException e) {
            log.error(e);
            throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
        }
    }
}
