/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.ConfirmProposalHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ConfirmUpgradeTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.util.*;

/**
 * 确认提案交易处理器
 */
@Component("ConfirmProposalV1")
public class ConfirmProposalProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.CONFIRM_PROPOSAL;
    }

    @Autowired
    private ConfirmProposalHelper confirmProposalHelper;
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
    private ProposalStorageService proposalStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;

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
            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                ConfirmProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmProposalTxData.class);
                String proposalTxHash;
                if(ProposalTypeEnum.UPGRADE.value() == txData.getType()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    proposalTxHash = upgradeTxData.getNerveTxHash().toHex();
                } else {
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    proposalTxHash = businessData.getProposalTxHash().toHex();
                }
                if (!setDuplicate.add(proposalTxHash)) {
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The proposalTxHash in the block is repeated (Repeat business)");
                    continue;
                }

                if(null != tx.getCoinData() && tx.getCoinData().length > 0){
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.COINDATA_CANNOT_EXIST.getCode();
                    log.error(ConverterErrorCode.COINDATA_CANNOT_EXIST.getMsg());
                    continue;
                }
                try {
                    // 签名验证
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
                setDuplicate.add(proposalTxHash);
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
                ConfirmProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmProposalTxData.class);
                int heterogeneousChainId = -1;
                String heterogeneousTxHash = null;
                IHeterogeneousChainDocking docking = null;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    docking.updateMultySignAddress(Numeric.toHexString(upgradeTxData.getAddress()));
                }else{
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    heterogeneousChainId = businessData.getHeterogeneousChainId();
                    heterogeneousTxHash = businessData.getHeterogeneousTxHash();
                    ProposalPO po = this.proposalStorageService.find(chain, businessData.getProposalTxHash());
                    if(ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED){
                        // 重置执行撤银行节点提案标志
                        heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, false);
                    }
                }
                if (syncStatus == SyncStatusEnum.RUNNING.value() && isCurrentDirector) {
                    if (txData.getType() == ProposalTypeEnum.UPGRADE.value() ||
                            txData.getType() == ProposalTypeEnum.EXPELLED.value() ||
                            txData.getType() == ProposalTypeEnum.REFUND.value() ||
                            txData.getType() == ProposalTypeEnum.WITHDRAW.value()) {
                        if (null == docking) {
                            docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                        }
                        docking.txConfirmedCompleted(heterogeneousTxHash, blockHeader.getHeight());

                        // 补贴手续费
                        if (txData.getType() == ProposalTypeEnum.UPGRADE.value() ||
                                txData.getType() == ProposalTypeEnum.REFUND.value() ) {
                            //放入后续处理队列, 可能发起手续费补贴交易
                            TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                            pendingPO.setTx(tx);
                            pendingPO.setBlockHeader(blockHeader);
                            pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                            txSubsequentProcessStorageService.save(chain, pendingPO);
                            chain.getPendingTxQueue().offer(pendingPO);
                        }
                    }
                }
                chain.getLogger().info("[commit] 确认提案执行交易 hash:{} type:{}",
                        tx.getHash().toHex(), ProposalTypeEnum.getEnum(txData.getType()));
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
                ConfirmProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmProposalTxData.class);
                IHeterogeneousChainDocking docking = null;
                int heterogeneousChainId = -1;
                String heterogeneousTxHash = null;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    docking.updateMultySignAddress(Numeric.toHexString(upgradeTxData.getOldAddress()));
                }else {
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    ProposalPO po = this.proposalStorageService.find(chain, businessData.getProposalTxHash());
                    if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED) {
                        // 重置执行撤银行节点提案标志
                        heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, true);
                    }
                }
                if (isCurrentDirector) {
                    if (txData.getType() != ProposalTypeEnum.UPGRADE.value()) {
                        ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                        heterogeneousChainId = businessData.getHeterogeneousChainId();
                        heterogeneousTxHash = businessData.getHeterogeneousTxHash();
                    }
                    if (null == docking) {
                        docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    }
                    docking.txConfirmedRollback(heterogeneousTxHash);
                }
                chain.getLogger().info("[rollback] 确认提案交易 hash:{}", tx.getHash().toHex());
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
