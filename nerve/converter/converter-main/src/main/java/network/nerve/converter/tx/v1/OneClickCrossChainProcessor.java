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

package network.nerve.converter.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2022/3/24
 */
@Component("OneClickCrossChainV1")
public class OneClickCrossChainProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.ONE_CLICK_CROSS_CHAIN;
    }
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            //Check for duplicate transactions within the block business
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
                HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
                String originalTxHash = heterogeneousHash.getHeterogeneousHash();
                if(!setDuplicate.add(originalTxHash.toLowerCase())){
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash in the block is repeated (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), txData.getOriginalTxHash());
                    continue;
                }
                if(null != rechargeStorageService.find(chain, originalTxHash)){
                    // The original transaction has already been recharged
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), txData.getOriginalTxHash());
                }

                // Signature Byzantine Verification
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            chain.getLogger().error(e);
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    private boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for(Transaction tx : txs) {
                OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
                HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
                String originalTxHash = heterogeneousHash.getHeterogeneousHash();
                int htgChainId = heterogeneousHash.getHeterogeneousChainId();
                NulsHash hash = tx.getHash();
                boolean rs = rechargeStorageService.save(chain, originalTxHash, hash);
                if (!rs) {
                    chain.getLogger().error("[commit] Save recharge failed. hash:{}, type:{}", hash.toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }

                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(htgChainId);
                if (docking != null) {
                    docking.txConfirmedCheck(originalTxHash, blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                }

                // When the block is in normal operation state（Non block synchronization mode）Only then will it be executed
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    docking.txConfirmedCompleted(originalTxHash, blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                    // Placing a queue like processing mechanism Prepare to notify heterogeneous chain components to execute withdrawals
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setCurrentDirector(true);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    pendingPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                }
                chain.getLogger().info("[commit]OneClickCrossChain One click cross chain transaction hash:{}", hash.toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    private boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            // Rolling back the transaction status of heterogeneous chain components // add by Mimi at 2020-03-13
            for(Transaction tx : txs) {
                OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
                HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
                String originalTxHash = heterogeneousHash.getHeterogeneousHash();
                int htgChainId = heterogeneousHash.getHeterogeneousChainId();
                // Perform heterogeneous chain transaction confirmation rollback only for recharge transactions that are not proposed for execution
                if (htgChainId > 0) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
                    docking.txConfirmedRollback(originalTxHash);
                }
                boolean rs = this.rechargeStorageService.delete(chain, originalTxHash);
                if (!rs) {
                    chain.getLogger().error("[rollback] remove recharge failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }

                chain.getLogger().info("[rollback]Recharge Recharge transaction hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}
