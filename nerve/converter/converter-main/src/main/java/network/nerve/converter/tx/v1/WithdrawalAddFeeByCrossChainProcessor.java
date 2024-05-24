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
import io.nuls.base.data.CoinData;
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
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.po.WithdrawalAdditionalFeePO;
import network.nerve.converter.model.txdata.WithdrawalAddFeeByCrossChainTxData;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.converter.constant.ConverterConstant.INIT_CAPACITY_8;

/**
 * Cross chain additional cross chain handling fee transaction
 *
 * @author: PierreLuo
 * @date: 2022/4/13
 */
@Component("WithdrawalAddFeeByCrossChainV1")
public class WithdrawalAddFeeByCrossChainProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private TxStorageService txStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    @Override
    public int getType() {
        return TxType.ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN;
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
                WithdrawalAddFeeByCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAddFeeByCrossChainTxData.class);
                HeterogeneousHash heterogeneousHash = txData.getHtgTxHash();
                String originalTxHash = heterogeneousHash.getHeterogeneousHash();
                if(!setDuplicate.add(originalTxHash.toLowerCase())){
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash in the block is repeated (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), originalTxHash);
                    continue;
                }
                if(null != rechargeStorageService.find(chain, originalTxHash)){
                    // The original transaction has already been recharged
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), originalTxHash);
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

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
        try {
            for (Transaction tx : txs) {
                NulsHash hash = tx.getHash();
                WithdrawalAddFeeByCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAddFeeByCrossChainTxData.class);
                HeterogeneousHash htgTxHash = txData.getHtgTxHash();
                String heterogeneousHash = htgTxHash.getHeterogeneousHash();
                int heterogeneousChainId = htgTxHash.getHeterogeneousChainId();
                boolean rs = rechargeStorageService.save(chain, heterogeneousHash, hash);
                if (!rs) {
                    chain.getLogger().error("[commit] Save AddFeeCrossChain failed. hash:{}, type:{}", hash.toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                String basicTxHash = txData.getNerveTxHash();
                WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, basicTxHash);
                if (null == po) {
                    po = new WithdrawalAdditionalFeePO(basicTxHash, new HashMap<>(INIT_CAPACITY_8));
                }
                if (null == po.getMapAdditionalFee()) {
                    po.setMapAdditionalFee(new HashMap<>(INIT_CAPACITY_8));
                }
                // Changes in updated withdrawal fee additions
                chain.increaseWithdrawFeeChangeVersion(basicTxHash);
                // Save transaction
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                BigInteger fee = coinData.getTo().get(0).getAmount();
                po.getMapAdditionalFee().put(tx.getHash().toHex(), fee);
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
                if (docking != null) {
                    docking.txConfirmedCheck(heterogeneousHash, blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                }
                // Under normal operation of the node, Only execute the heterogeneous chain transaction confirmation function
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    docking.txConfirmedCompleted(heterogeneousHash, blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                }
                chain.getLogger().info("[commit] [Cross chain addition]Withdrawal transaction fee transaction hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), basicTxHash);
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

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                WithdrawalAddFeeByCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAddFeeByCrossChainTxData.class);
                HeterogeneousHash htgTxHash = txData.getHtgTxHash();
                String heterogeneousHash = htgTxHash.getHeterogeneousHash();
                int heterogeneousChainId = htgTxHash.getHeterogeneousChainId();
                String withdrawalTxHash = txData.getNerveTxHash();
                WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, withdrawalTxHash);
                if (po == null || null == po.getMapAdditionalFee() || po.getMapAdditionalFee().isEmpty()) {
                    continue;
                }
                // Changes in Rollback Withdrawal Fee Addition
                chain.decreaseWithdrawFeeChangeVersion(withdrawalTxHash);
                // Remove the current transactionkv
                po.getMapAdditionalFee().remove(tx.getHash().toHex());
                txStorageService.saveWithdrawalAdditionalFee(chain, po);
                boolean rs = this.rechargeStorageService.delete(chain, heterogeneousHash);
                if (!rs) {
                    chain.getLogger().error("[rollback] remove AddFeeCrossChain failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                docking.txConfirmedRollback(heterogeneousHash);
                chain.getLogger().info("[rollback] [Cross chain addition]Withdrawal transaction fee transaction hash:{}, withdrawalTxHash:{}", tx.getHash().toHex(), withdrawalTxHash);
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
