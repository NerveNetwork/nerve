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
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.*;
import io.nuls.transaction.model.po.TransactionNetPO;
import io.nuls.transaction.rpc.call.ContractCall;
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

import static io.nuls.transaction.constant.TxConstant.*;
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
     * 打包交易
     * 适用于不包含智能合约交易的区块链
     *
     * @param chain
     * @param endtimestamp  打包截止时间
     * @param maxTxDataSize 所有交易数据最大size
     * @return
     */
    @Override
    public TxPackage packageBasic(Chain chain, long endtimestamp, long maxTxDataSize, long blockTime, String preStateRoot) {
        chain.getPackageLock().lock();
        long startTime = NulsDateUtils.getCurrentTimeMillis();
        long packableTime = endtimestamp - startTime;
        long height = chain.getBestBlockHeight() + 1;
        List<TxPackageWrapper> packingTxList = new ArrayList<>();
        //记录账本的孤儿交易,返回给共识的时候给过滤出去,因为在因高度变化而导致重新打包的时候,需要还原到待打包队列
        Set<TxPackageWrapper> orphanTxSet = new HashSet<>();
        //组装统一验证参数数据,key为各模块统一验证器cmd
        Map<String, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        NulsLogger log = chain.getLogger();
        try {
            log.info("[Package start] 区块时间:{}, - height:{}, -打包总可用时间：{}, -可打包容量：{}B , - 当前待打包队列交易hash数:{}, - 待打包队列实际交易数:{}",
                    blockTime, height, packableTime, maxTxDataSize, packablePool.packableHashQueueSize(chain), packablePool.packableTxMapSize(chain));
            if (packableTime <= TxConstant.BASIC_PACKAGE_RESERVE_TIME) {
                //直接打空块
                return null;
            }
            long collectTime = 0L, batchModuleTime = 0L;
            long collectTimeStart = NulsDateUtils.getCurrentTimeMillis();
            Result rs = collectProcessTransactionsBasic(chain, blockTime, endtimestamp, maxTxDataSize, packingTxList, orphanTxSet, moduleVerifyMap, preStateRoot);
            if (rs.isFailed()) {
                return null;
            }
            Map<String, Object> dataMap = (Map<String, Object>) rs.getData();
            // Swap交易的执行状态 和 生成的系统交易
            String stateRoot = (String) dataMap.get("stateRoot");
            List<String> swapTxList = (List<String>) dataMap.get("swapTxList");
            if (log.isDebugEnabled()) {
                collectTime = NulsDateUtils.getCurrentTimeMillis() - collectTimeStart;
            }
            //模块验证交易
            long batchStart = NulsDateUtils.getCurrentTimeMillis();
            List<String> allNewlyList = txService.txModuleValidatorPackable(chain, moduleVerifyMap, packingTxList, orphanTxSet, height, blockTime);
            //模块统一验证使用总时间
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


            // 处理需要打包时内部生成的交易
            if (!swapTxList.isEmpty()) {
                packableTxs.addAll(swapTxList);
            }
            if (!allNewlyList.isEmpty()) {
                packableTxs.addAll(allNewlyList);
            }

            //孤儿交易加回待打包队列去
            txService.putBackPackablePool(chain, orphanTxSet);
            if (chain.getProtocolUpgrade().get()) {
                processProtocolUpgrade(chain, packingTxList);
                return null;
            }

            long current = NulsDateUtils.getCurrentTimeMillis();
            if (endtimestamp - current < TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME) {
                //超时,留给最后数据组装和RPC传输时间不足
                log.error("getPackableTxs time out, endtimestamp:{}, current:{}, endtimestamp-current:{}, reserveTime:{}",
                        endtimestamp, current, endtimestamp - current, TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME);
                throw new NulsException(TxErrorCode.PACKAGE_TIME_OUT);
            }
            long totalTime = NulsDateUtils.getCurrentTimeMillis() - startTime;
            log.debug("[打包时间统计] 总可用:{}ms, 总执行:{}, 总剩余:{}, 收集交易与账本验证:{}, 模块统一验证:{}",
                    packableTime, totalTime, packableTime - totalTime, collectTime, batchModuleTime);

            log.info("[Package end] - height:{} - 本次打包交易数:{} - 当前待打包队列交易hash数:{}, - 待打包队列实际交易数:{}" + TxUtil.nextLine(),
                    height, packableTxs.size(), packablePool.packableHashQueueSize(chain), packablePool.packableTxMapSize(chain));
            TxPackage txPackage = new TxPackage();
            txPackage.setList(packableTxs);
            txPackage.setStateRoot(stateRoot);
            return txPackage;
        } catch (Exception e) {
            log.error(e);
            //可打包交易,孤儿交易,全加回去
            txService.putBackPackablePool(chain, packingTxList, orphanTxSet);
            return null;
        } finally {
            chain.getPackageLock().unlock();
        }
    }


    /**
     * 获取打包的交易, 并验证账本
     *
     * @return false 表示直接出空块
     * @throws NulsException
     */
    private Result collectProcessTransactionsBasic(Chain chain, long blockTime, long endtimestamp, long maxTxDataSize, List<TxPackageWrapper> packingTxList,
                                                    Set<TxPackageWrapper> orphanTxSet, Map<String, List<String>> moduleVerifyMap, String preStateRoot) throws NulsException {

        //SWAP通知标识,出现的第一个SWAP交易并且调用验证器通过时,有则只第一次时通知.
        boolean swapNotify = false;
        //本次打包高度
        long blockHeight = chain.getBestBlockHeight() + 1;
        //向账本模块发送要批量验证coinData的标识
        LedgerCall.coinDataBatchNotify(chain);
        int allCorssTxCount = 0, batchCorssTxCount = 0;
        long totalSize = 0L, totalSizeTemp = 0L;
        List<String> batchProcessList = new ArrayList<>();
        Set<String> duplicatesVerify = new HashSet<>();
        List<TxPackageWrapper> currentBatchPackableTxs = new ArrayList<>();
        NulsLogger log = chain.getLogger();
        // 打包在该区块中交易的时间, 要在区块时间的一定范围内.
        long txTimeRangStart = blockTime - BLOCK_TX_TIME_RANGE_SEC;
        long txTimeRangEnd = blockTime + BLOCK_TX_TIME_RANGE_SEC;
        // SWAP模块生成的系统交易集合
        List<String> swapGenerateTxs = new ArrayList<>();
        // 系统交易相应的原始交易hash集合（SWAP模块生成的系统交易）
        List<String> swapOriginTxHashList = new ArrayList<>();
        String newStateRoot = preStateRoot;

        for (int index = 0; ; index++) {
            long currentTimeMillis = NulsDateUtils.getCurrentTimeMillis();
            long currentReserve = endtimestamp - currentTimeMillis;
            if (currentReserve <= TxConstant.BASIC_PACKAGE_RESERVE_TIME) {
                if (log.isDebugEnabled()) {
                    log.info("获取交易时间到,进入模块验证阶段: currentTimeMillis:{}, -endtimestamp:{}, -offset:{}, -remaining:{}",
                            currentTimeMillis, endtimestamp, TxConstant.BASIC_PACKAGE_RESERVE_TIME, currentReserve);
                }
                backTempPackablePool(chain, currentBatchPackableTxs);
                break;
            }
            if (currentReserve < TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME) {
                //超时,留给最后数据组装和RPC传输时间不足
                log.error("getPackableTxs time out, endtimestamp:{}, current:{}, endtimestamp-current:{}, reserveTime:{}",
                        endtimestamp, currentTimeMillis, currentReserve, TxConstant.BASIC_PACKAGE_RPC_RESERVE_TIME);
                backTempPackablePool(chain, currentBatchPackableTxs);
                throw new NulsException(TxErrorCode.PACKAGE_TIME_OUT);
            }
            if (chain.getProtocolUpgrade().get()) {
                log.info("Protocol Upgrade Package stop -chain:{} -best block height", chain.getChainId(), chain.getBestBlockHeight());
                backTempPackablePool(chain, currentBatchPackableTxs);
                //放回可打包交易和孤儿
                txService.putBackPackablePool(chain, packingTxList, orphanTxSet);
                return Result.getFailed(TxErrorCode.DATA_ERROR);
            }
            if (packingTxList.size() >= TxConstant.BASIC_PACKAGE_TX_MAX_COUNT) {
                if (log.isDebugEnabled()) {
                    log.info("获取交易已达max count,进入模块验证阶段: currentTimeMillis:{}, -endtimestamp:{}, -offset:{}, -remaining:{}",
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
                    // 6.28 重置成打包无交易,等待模式
                    Thread.sleep(10L);
                    continue;
                } else if (tx == null && batchProcessListSize > 0) {
                    //达到处理该批次的条件
                    process = true;
                } else if (tx != null) {
                    if (null != confirmedTxStorageService.getTx(chain.getChainId(), tx.getHash())) {
                        //交易已确认过
                        txService.baseClearTx(chain, tx);
                        continue;
                    }
                    if (!duplicatesVerify.add(tx.getHash().toHex())) {
                        //加入不进去表示已存在
                        continue;
                    }
                    long txSize = tx.size();
                    if ((totalSizeTemp + txSize) > maxTxDataSize) {
                        packablePool.offerFirstOnlyHash(chain, tx);
                        log.info("交易已达最大容量, 实际值: {}, totalSizeTemp:{}, 当前交易size：{} - 预定最大值maxTxDataSize:{}, txhash:{}", totalSize, totalSizeTemp, txSize, maxTxDataSize, tx.getHash().toHex());
                        maxDataSize = true;
                        if (batchProcessListSize == 0) {
                            break;
                        }
                        //达到处理该批次的条件
                        process = true;
                    } else {
                        TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
                        if (txRegister.getModuleCode().equals(ModuleE.CS.abbr)) {
                            // 如果是共识模块的交易, 先验证交易时间, 在区块时间的一定范围内
                            long txTime = tx.getTime();
                            if (txTime < txTimeRangStart || txTime > txTimeRangEnd) {
                                chain.getLogger().error("[TxPackage] The transction time does not match the block time, blockTime:{}, txTime:{}, range:±{}",
                                        blockTime, txTime, BLOCK_TX_TIME_RANGE_SEC);
                                continue;
                            }
                        }
                        //限制跨链交易数量
                        if (txRegister.getModuleCode().equals(ModuleE.CC.abbr)) {
                            if (allCorssTxCount + (++batchCorssTxCount) >= TxConstant.BASIC_PACKAGE_CROSS_TX_MAX_COUNT) {
                                //限制单个区块包含的跨链交易总数，超过跨链交易最大个数，放回去, 然后停止获取交易
                                packablePool.add(chain, tx);
                                if (batchProcessListSize == 0) {
                                    break;
                                }
                                //达到处理该批次的条件
                                process = true;
                            }
                        }
                        String txHex;
                        try {
                            txHex = RPCUtil.encode(tx.serialize());
                        } catch (Exception e) {
                            log.warn(e.getMessage(), e);
                            log.error("丢弃获取hex出错交易, txHash:{}, - type:{}, - time:{}", tx.getHash().toHex(), tx.getType(), tx.getTime());
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
                                //达到处理该批次的条件
                                process = true;
                            }
                        }
                    }
                    //总大小加上当前批次各笔交易大小
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
                            // 出现SWAP交易,且通知标识为false,则先调用通知
                            if (!swapNotify) {
                                log.info("[{}]开始Swap模块-打包, {}, {}", blockHeight, blockTime, preStateRoot);
                                SwapCall.swapBatchBegin(chain, blockHeight, blockTime, preStateRoot, 0);
                                swapNotify = true;
                            }
                            try {
                                // 调用执行SWAP交易
                                log.info("[{}]处理Swap交易-打包, {}, {}", blockHeight, blockTime, txPackageWrapper.getTxHex());
                                Map<String, Object> invokeContractRs = SwapCall.invoke(chain, txPackageWrapper.getTxHex(), blockHeight, blockTime, 0);
                                List<String> txList = (List<String>) invokeContractRs.get("txList");
                                if (txList != null && !txList.isEmpty()) {
                                    log.info("[{}]得到生成交易_Swap模块-打包, {}, {}", blockHeight, blockTime, Arrays.toString(txList.toArray()));
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
                        //计算跨链交易的数量
                        if (ModuleE.CC.abbr.equals(txRegister.getModuleCode())) {
                            allCorssTxCount++;
                        }
                        //根据模块的统一验证器名，对所有交易进行分组，准备进行各模块的统一验证
                        TxUtil.moduleGroups(moduleVerifyMap, txRegister, RPCUtil.encode(transaction.serialize()));
                    }
                    //更新到当前最新区块交易大小总值
                    totalSizeTemp = totalSize;
                    packingTxList.addAll(currentBatchPackableTxs);
                    //批次结束重置数据
                    batchProcessList.clear();
                    currentBatchPackableTxs.clear();
                    batchCorssTxCount = 0;
                    if (maxDataSize) {
                        break;
                    }
                }
            } catch (Exception e) {
                currentBatchPackableTxs.clear();
                log.error("打包交易异常, txHash:{}, - type:{}, - time:{}", tx.getHash().toHex(), tx.getType(), tx.getTime());
                log.error(e);
                continue;
            }
        }
        /** SWAP 当通知标识为true, 则表明有SWAP交易被调用执行*/
        if (swapNotify) {
            Log.info("收集交易");
            Map<String, Object> batchEnd = SwapCall.swapBatchEnd(chain, blockHeight, 0);
            newStateRoot = (String) batchEnd.get("stateRoot");
            log.info("[{}]结束Swap模块-打包, {}, {}", blockHeight, blockTime, newStateRoot);
        }
        if (log.isDebugEnabled()) {
            log.debug("收集交易 -count:{} - data size:{}",
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
        //开始处理
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
                //去除账本验证失败的交易
                for (String hash : failHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        txService.clearInvalidTx(chain, transaction);
                        it.remove();
                        continue removeAndGo;
                    }
                }
                //去除孤儿交易, 同时把孤儿交易放入孤儿池
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
     * 打包时如遇协议升级, 将交易放回未处理队列,重新处理
     */
    private void processProtocolUpgrade(Chain chain, List<TxPackageWrapper> packingTxList) throws NulsException {
        //协议升级直接打空块,取出的交易，倒序放入新交易处理队列
        int size = packingTxList.size();
        for (int i = size - 1; i >= 0; i--) {
            TxPackageWrapper txPackageWrapper = packingTxList.get(i);
            Transaction tx = txPackageWrapper.getTx();
            //执行交易基础验证
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
     * 打包时,从待打包队列获取交易阶段,会产生临时交易列表,在中断获取交易时需要把遗留的临时交易还回待打包队列
     */
    private void backTempPackablePool(Chain chain, List<TxPackageWrapper> listTx) {
        for (int i = listTx.size() - 1; i >= 0; i--) {
            packablePool.offerFirstOnlyHash(chain, listTx.get(i).getTx());
        }
    }

    /**
     * 验证区块中只允许有一个的交易不能有多个
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
        LoggerUtil.LOG.debug("[验区-区块时间]:{}, 高度:{}", blockTime, blockHeight);
        boolean isLogDebug = log.isDebugEnabled();
        if (isLogDebug) {
            log.debug("[验区块交易] 开始 -----高度:{} -----区块交易数:{}", blockHeight, txStrList.size());
        }
        //验证区块中只允许有一个的交易不能有多个
        Set<Integer> onlyOneTxTypes = new HashSet<>();
        //SWAP通知标识,出现的第一个SWAP交易并且调用验证器通过时,有则只第一次时通知.
        boolean swapNotify = false;
        List<TxVerifyWrapper> txList = new ArrayList<>();
        //组装统一验证参数数据,key为各模块统一验证器cmd
        Map<String, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_8);
        List<byte[]> keys = new ArrayList<>();
        int totalSize = 0;
        // 打包在该区块中交易的时间, 要在区块时间的一定范围内.
        long txTimeRangStart = blockTime - BLOCK_TX_TIME_RANGE_SEC;
        long txTimeRangEnd = blockTime + BLOCK_TX_TIME_RANGE_SEC;
        // SWAP模块生成的系统交易集合
        List<String> swapGenerateTxs = new ArrayList<>();
        List<String> swapGenerateTxsByVerify = new ArrayList<>();

        log.info("区块交易列表-验证: {}", Arrays.toString(txStrList.toArray()));
        for (String txStr : txStrList) {
            Transaction tx = TxUtil.getInstanceRpcStr(txStr, Transaction.class);
            int type = tx.getType();
            TxRegister txRegister = TxManager.getTxRegister(chain, type);
            boolean isUnSystemSwapTx = TxManager.isUnSystemSwap(txRegister);
            if (isUnSystemSwapTx) {
                // 出现SWAP交易,且通知标识为false,则先调用通知
                if (!swapNotify) {
                    log.info("[{}]开始Swap模块-验证, {}, {}", blockHeight, blockTime, preStateRoot);
                    SwapCall.swapBatchBegin(chain, blockHeight, blockTime, preStateRoot, 1);
                    swapNotify = true;
                }
                // 调用执行SWAP交易
                log.info("[{}]处理Swap交易-验证, {}, {}", blockHeight, blockTime, txStr);
                Map<String, Object> invokeContractRs = SwapCall.invoke(chain, txStr, blockHeight, blockTime, 1);
                List<String> swapTxList = (List<String>) invokeContractRs.get("txList");
                if (swapTxList != null && !swapTxList.isEmpty()) {
                    log.info("[{}]得到生成交易_Swap模块-验证, {}, {}", blockHeight, blockTime, Arrays.toString(swapTxList.toArray()));
                    swapGenerateTxsByVerify.addAll(swapTxList);
                }
            } else if (TxManager.isSystemSwap(txRegister)) {
                // 筛选出SWAP系统交易集合
                swapGenerateTxs.add(txStr);
            }
            if (txRegister.getModuleCode().equals(ModuleE.CS.abbr)) {
                // 如果是共识模块的交易, 先验证交易时间, 在区块时间的一定范围内
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

        //验证本地没有的交易
        List<Future<Boolean>> futures = new ArrayList<>();
        verifyNonLocalTxs(chain, futures, keys, txList, blockHeight);
        keys = null;

        //验证账本
        long coinDataV = NulsDateUtils.getCurrentTimeMillis();
        if (!LedgerCall.verifyBlockTxsCoinData(chain, txStrList, blockHeight)) {
            throw new NulsException(TxErrorCode.TX_LEDGER_VERIFY_FAIL);
        }
        if (isLogDebug) {
            log.debug("[验区块交易] coinData -距方法开始的时间:{}，-验证时间:{}",
                    NulsDateUtils.getCurrentTimeMillis() - s1, NulsDateUtils.getCurrentTimeMillis() - coinDataV);
        }
        //模块统一验证器
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

        // 本节点验证区块产生的stateRoot
        String verifyStateRoot = preStateRoot;
        /** SWAP 当通知标识为true, 则表明有SWAP交易被调用执行*/
        if (swapNotify) {
            Log.info("验证区块");
            Map<String, Object> batchEnd = SwapCall.swapBatchEnd(chain, blockHeight, 1);
            verifyStateRoot = (String) batchEnd.get("stateRoot");
            log.info("[{}]结束Swap模块-验证, {}, {}", blockHeight, blockTime, verifyStateRoot);
        }
        // 打包的stateRoot
        String stateRoot = RPCUtil.encode(blockHeader.getExtendsData().getStateRoot());
        // 当打包的stateRoot是null时，说明区块中没有swap交易
        if (stateRoot == null) {
            // 此时验证区块的stateRoot非空的话，必须是初始StateRoot
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

        // 验证打包时生成的交易
        // 分组 将所有交易分组 包含原始交易和打包时生成的交易
        Map<String, List> packProduceCallMap = packProduceProcess(chain, txList, blockHeight);
        verifyNewProduceTx(chain, packProduceCallMap, blockHeight, blockHeader.getTime());

        if (isLogDebug) {
            log.debug("[验区块交易] 模块统一验证时间:{}", NulsDateUtils.getCurrentTimeMillis() - moduleV);
            log.debug("[验区块交易] 模块统一验证 -距方法开始的时间:{}", NulsDateUtils.getCurrentTimeMillis() - s1);
        }
        //检查[验证本地没有的交易]的多线程处理结果
        checkbaseValidateResult(chain, futures);

        if (isLogDebug) {
            log.debug("[验区块交易] 合计执行时间:{}, 交易总size:{} - 高度:{} - 区块交易数:{}, " + TxUtil.nextLine(),
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
        // K: 模块code V:{k: 原始交易hash, v:原始交易字符串}
        Map<String, List> packProduceCallMap = TxManager.getGroup(chain);

        while (iterator.hasNext()) {
            TxVerifyWrapper txVerifyWrapper = iterator.next();
            Transaction tx = txVerifyWrapper.getTx();
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getPackProduce()) {
                List<TxVerifyWrapper> listCallTxs = packProduceCallMap.computeIfAbsent(txRegister.getModuleCode(), k -> new ArrayList<>());
                listCallTxs.add(txVerifyWrapper);
                LoggerUtil.LOG.debug("验证区块时模块内部处理生成, 原始交易hash:{}, 高度:{}, moduleCode:{}", tx.getHash().toHex(), height, txRegister.getModuleCode());
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
            // 没有异常表示验证通过
            TransactionCall.packProduce(chain, BaseConstant.TX_PACKPRODUCE, moduleCode, txHexList, height, blockTime, VERIFY);
        }
    }

    /**
     * 验证是否含有已确认交易
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
     * 开启多线程 对不在本地的未确认数据库中的交易, 进行基础验证
     */
    private void verifyNonLocalTxs(Chain chain, List<Future<Boolean>> futures, List<byte[]> keys, List<TxVerifyWrapper> txList, long height) {
        //获取区块的交易中, 在未确认数据库中存在的交易
        List<String> unconfirmedList = unconfirmedTxStorageService.getExistKeysStr(chain.getChainId(), keys);
        Set<String> set = new HashSet<>();
        set.addAll(unconfirmedList);
        unconfirmedList = null;
        for (TxVerifyWrapper txVerifyWrapper : txList) {
            Transaction tx = txVerifyWrapper.getTx();
            //能加入表明未确认中没有,则需要处理
            if (set.add(tx.getHash().toHex())) {
                //不在未确认中就进行基础验证
                //多线程处理单个交易
                Future<Boolean> res = verifySignExecutor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        try {
                            //只验证单个交易的基础内容(TX模块本地验证)
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
     * 检查基础验证结果
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
