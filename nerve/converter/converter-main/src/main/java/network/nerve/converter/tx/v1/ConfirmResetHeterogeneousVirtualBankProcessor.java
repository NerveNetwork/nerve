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
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.txdata.ConfirmResetVirtualBankTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmResetBankStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * @author: Loki
 * @date: 2020/6/26
 */
@Component("ConfirmResetHeterogeneousVirtualBankV1")
public class ConfirmResetHeterogeneousVirtualBankProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConfirmResetBankStorageService confirmResetBankStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousService heterogeneousService;
    @Override
    public int getType() {
        return TxType.CONFIRM_HETEROGENEOUS_RESET_VIRTUAL_BANK;
    }

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
                ConfirmResetVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmResetVirtualBankTxData.class);
                NulsHash resetTxHash = txData.getResetTxHash();
                if (setDuplicate.contains(resetTxHash.toHex())) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
                NulsHash confirmHash = confirmResetBankStorageService.get(chain, resetTxHash);
                if (null != confirmHash) {
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
                //Obtain original transaction
                Transaction resetTx = TransactionCall.getConfirmedTx(chain, resetTxHash);
                if (null == resetTx) {
                    // NerveThe original reset transaction does not exist
                    throw new NulsException(ConverterErrorCode.RESET_TX_NOT_EXIST);
                }
                // Signature verification(seed)
                try {
                    ConverterSignValidUtil.validateVirtualBankSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }

                setDuplicate.add(resetTxHash.toHex());
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
                ConfirmResetVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmResetVirtualBankTxData.class);
                boolean rs = confirmResetBankStorageService.save(chain, txData.getResetTxHash(), tx.getHash());
                if (!rs) {
                    chain.getLogger().error("[commit] Save confirm ResetHeterogeneousVirtualBank tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-12
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    if(StringUtils.isNotBlank(txData.getHeterogeneousTxHash())) {
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                        docking.txConfirmedCompleted(txData.getHeterogeneousTxHash(), blockHeader.getHeight(), txData.getResetTxHash().toHex(), null);
                    }
                }
                heterogeneousService.saveResetVirtualBankStatus(chain, false);
                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, false);
                heterogeneousService.saveResetVirtualBankStatus(chain, false);
                chain.getLogger().info("[commit] Confirm resetting the virtual banking heterogeneous chain(contract) hash:{}", tx.getHash().toHex());
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
                ConfirmResetVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmResetVirtualBankTxData.class);
                boolean rs = confirmResetBankStorageService.remove(chain, txData.getResetTxHash());
                if (!rs) {
                    chain.getLogger().error("[rollback] remove confirm ResetHeterogeneousVirtualBank tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                if (isCurrentDirector) {
                    if(StringUtils.isNotBlank(txData.getHeterogeneousTxHash())) {
                        // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-13
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                        docking.txConfirmedRollback(txData.getHeterogeneousTxHash());
                    }
                }
                heterogeneousService.saveResetVirtualBankStatus(chain, true);
                chain.getLogger().info("[rollback] Confirm resetting the virtual banking heterogeneous chain(contract) hash:{}, Original withdrawal transactionhash:{}", tx.getHash().toHex(), txData.getResetTxHash().toHex());
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
