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
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.model.txdata.RechargeTxData;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.*;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component("RechargeV1")
public class RechargeProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.RECHARGE;
    }
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;

    /**
     * Main verification logic
     * 1.Verify original transactionhash
     * 2.Verify whether the recharge amount is consistent with the original recharge amount
     * 3.Verify if the delivery address is consistent
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
     * @return
     */
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
                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                if(!setDuplicate.add(txData.getOriginalTxHash().toLowerCase())){
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash in the block is repeated (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), txData.getOriginalTxHash());
                    continue;
                }
                if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
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
        boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
        try {
            // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-12
            for(Transaction tx : txs) {
                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                NulsHash hash = tx.getHash();
                boolean rs = rechargeStorageService.save(chain, txData.getOriginalTxHash(), hash);
                if (!rs) {
                    chain.getLogger().error("[commit] Save recharge failed. hash:{}, type:{}", hash.toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                ProposalPO proposalPO = null;
                if(!Numeric.containsHexPrefix(txData.getOriginalTxHash())) {
                    // Indicates that it is not a heterogeneous chain transactionhash May be a proposal
                    proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
                }

                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(txData.getHeterogeneousChainId());
                if (docking != null) {
                    docking.txConfirmedCheck(txData.getOriginalTxHash(), blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                }
                // Under normal operation of the node And it is not a recharge transaction executed by the proposal, only the heterogeneous chain transaction confirmation function is executed
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector && null == proposalPO) {
                    docking.txConfirmedCompleted(txData.getOriginalTxHash(), blockHeader.getHeight(), hash.toHex(), tx.getRemark());
                }
                if (null != proposalPO && syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    // If it is to execute the proposal Need to publish a proposal to confirm the transaction
                    publishProposalConfirmed(chain, hash, proposalPO, blockHeader.getTime());
                }

                chain.getLogger().info("[commit]Recharge Recharge transaction hash:{}", hash.toHex());
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

    /**
     * Publish proposalTRANSFERConfirmation transactions of types
     * @param txHash Current recharge transactionhash
     * @param proposalPO Corresponding proposal
     * @param time Current block time
     * @throws NulsException
     */
    private void publishProposalConfirmed(Chain chain, NulsHash txHash, ProposalPO proposalPO, long time) throws NulsException {
        ProposalExeBusinessData businessData = new ProposalExeBusinessData();
        businessData.setProposalTxHash(proposalPO.getHash());
        businessData.setAddress(proposalPO.getAddress());
        businessData.setProposalExeHash(txHash);
        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(proposalPO.getType());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        // Publish proposal to confirm transaction
        assembleTxService.createConfirmProposalTx(chain, txData, time);
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
                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                ProposalPO proposalPO = null;
                if(!Numeric.containsHexPrefix(txData.getOriginalTxHash())) {
                    // Indicates that it is not a heterogeneous chain transactionhash May be a proposal
                    proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
                }
                // Perform heterogeneous chain transaction confirmation rollback only for recharge transactions that are not proposed for execution
                if (null == proposalPO) {
                    if (txData.getHeterogeneousChainId() > 0) {
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
                        docking.txConfirmedRollback(txData.getOriginalTxHash());
                    }
                }
                boolean rs = this.rechargeStorageService.delete(chain, txData.getOriginalTxHash());
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
