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
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ComponentCalledPO;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component("ConfirmWithdrawalV1")
public class ConfirmWithdrawalProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.CONFIRM_WITHDRAWAL;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService;

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
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                String originalHash = txData.getWithdrawalTxHash().toHex();
                if (setDuplicate.contains(originalHash)) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
                if (null != po) {
                    // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out,This transaction is a duplicate confirmed withdrawal transaction
                    failsList.add(tx);
                    // NerveWithdrawal transaction does not exist
                    errorCode = ConverterErrorCode.CFM_IS_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.CFM_IS_DUPLICATION.getMsg());
                    continue;
                }
                if(null != tx.getCoinData() && tx.getCoinData().length > 0){
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.COINDATA_CANNOT_EXIST.getCode();
                    log.error(ConverterErrorCode.COINDATA_CANNOT_EXIST.getMsg());
                    continue;
                }
                // Signature verification
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }

                setDuplicate.add(originalHash);
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
            for (Transaction tx : txs) {
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                ConfirmWithdrawalPO po = new ConfirmWithdrawalPO(txData, tx.getHash());
                boolean rs = confirmWithdrawalStorageService.save(chain, po);
                if (!rs) {
                    chain.getLogger().error("[commit] Save confirm withdrawal tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                ComponentCalledPO callPO = new ComponentCalledPO(
                        txData.getWithdrawalTxHash().toHex(),
                        -1,
                        true);
                asyncProcessedTxStorageService.saveComponentCall(chain, callPO, false);

                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(txData.getHeterogeneousChainId());
                if (docking != null) {
                    docking.txConfirmedCheck(txData.getHeterogeneousTxHash(), blockHeader.getHeight(), txData.getWithdrawalTxHash().toHex(), tx.getRemark());
                }

                // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-12
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    docking.txConfirmedCompleted(txData.getHeterogeneousTxHash(), blockHeader.getHeight(), txData.getWithdrawalTxHash().toHex(), tx.getRemark());
                    //Put into the subsequent processing queue, Possible initiation of handling fee subsidy transactions
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    txSubsequentProcessStorageService.deleteBackup(chain, txData.getWithdrawalTxHash().toHex());
                    chain.getPendingTxQueue().offer(pendingPO);

                }
                chain.getLogger().info("[commit] Confirm withdrawal transactions hash:{}", tx.getHash().toHex());
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
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for (Transaction tx : txs) {
                ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
                boolean rs = confirmWithdrawalStorageService.deleteByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
                if (!rs) {
                    chain.getLogger().error("[rollback] remove confirm withdrawal tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                if (isCurrentDirector) {
                    // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-13
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                    docking.txConfirmedRollback(txData.getHeterogeneousTxHash());
                }
                chain.getLogger().info("[rollback] Confirm withdrawal transactions hash:{}, Original withdrawal transactionhash:{}", tx.getHash().toHex(), txData.getWithdrawalTxHash().toHex());
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
