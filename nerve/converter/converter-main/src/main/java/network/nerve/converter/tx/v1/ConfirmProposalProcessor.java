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

import io.nuls.base.basic.AddressTool;
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
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.context.HeterogeneousChainManager;
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
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.ProposalExeStorageService;
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
    private HeterogeneousChainManager heterogeneousChainManager;
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
    @Autowired
    private ProposalExeStorageService proposalExeStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

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
        if (converterCoreApi.isProtocol16()) {
            return commitProtocol16(chainId, txs, blockHeader, syncStatus, true);
        } else {
            return _commit(chainId, txs, blockHeader, syncStatus, true);
        }

    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        if (converterCoreApi.isProtocol16()) {
            return rollbackProtocol16(chainId, txs, blockHeader, true);
        } else {
            return _rollback(chainId, txs, blockHeader, true);
        }

    }

    private boolean _commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
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
                NulsHash proposalHash = null;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    proposalHash = upgradeTxData.getNerveTxHash();
                    String newMultySignAddress = Numeric.toHexString(upgradeTxData.getAddress());
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    // 通知异构链更新多签合约
                    docking.updateMultySignAddress(newMultySignAddress);
                    // 持久化更新多签合约
                    heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, newMultySignAddress);
                }else{
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    heterogeneousChainId = businessData.getHeterogeneousChainId();
                    heterogeneousTxHash = businessData.getHeterogeneousTxHash();
                    proposalHash = businessData.getProposalTxHash();
                    ProposalPO po = this.proposalStorageService.find(chain, businessData.getProposalTxHash());
                    if(ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED){
                        // 重置执行撤银行节点提案标志
                        heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, false);
                    } else if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.ADDCOIN) {
                        // 执行币种添加到稳定币兑换交易对里
                        String[] split = po.getContent().split("-");
                        int assetChainId = Integer.parseInt(split[0].trim());
                        int assetId = Integer.parseInt(split[1].trim());
                        String stablePairAddress = AddressTool.getStringAddressByBytes(po.getAddress());
                        SwapCall.addCoinForAddStable(chainId, stablePairAddress, assetChainId, assetId);
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
                        docking.txConfirmedCompleted(heterogeneousTxHash, blockHeader.getHeight(), proposalHash.toHex());

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
                boolean rs = proposalExeStorageService.save(chain, proposalHash.toHex(), tx.getHash().toHex());
                if (!rs) {
                    chain.getLogger().error("[commit] 确认提案执行交易 保存失败 hash:{}, proposalType:{}", tx.getHash().toHex(), txData.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[commit] 确认提案执行交易 hash:{} proposalType:{}",
                        tx.getHash().toHex(), ProposalTypeEnum.getEnum(txData.getType()));
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                _rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    private boolean _rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
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
                NulsHash proposalHash = null;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    proposalHash = upgradeTxData.getNerveTxHash();
                    String oldMultySignAddress = Numeric.toHexString(upgradeTxData.getOldAddress());
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    docking.updateMultySignAddress(oldMultySignAddress);
                    heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, oldMultySignAddress);
                }else {
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    proposalHash = businessData.getProposalTxHash();
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
                boolean rs = proposalExeStorageService.delete(chain, proposalHash.toHex());
                if (!rs) {
                    chain.getLogger().error("[commit] 确认提案执行交易 保存失败 hash:{}, proposalType:{}", tx.getHash().toHex(), txData.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[rollback] 确认提案交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                _commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }


    private boolean commitProtocol16(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for (Transaction tx : txs) {
                ConfirmProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmProposalTxData.class);
                int heterogeneousChainId;
                String heterogeneousTxHash;
                IHeterogeneousChainDocking docking = null;
                NulsHash proposalHash;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    proposalHash = upgradeTxData.getNerveTxHash();
                    // 更新合约版本号
                    ProposalPO po = this.proposalStorageService.find(chain, proposalHash);
                    String[] split = po.getContent().split("-");
                    byte newVersion = Integer.valueOf(split[1].trim()).byteValue();
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    // 兼容非以太系地址 update by pierre at 2021/11/16
                    String newMultySignAddress = docking.getAddressString(upgradeTxData.getAddress());
                    // 通知异构链更新多签合约
                    docking.updateMultySignAddressProtocol16(newMultySignAddress, newVersion);
                    // 持久化更新多签合约
                    heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, newMultySignAddress);
                }else{
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    heterogeneousChainId = businessData.getHeterogeneousChainId();
                    heterogeneousTxHash = businessData.getHeterogeneousTxHash();
                    proposalHash = businessData.getProposalTxHash();
                    ProposalPO po = this.proposalStorageService.find(chain, proposalHash);
                    if(ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED){
                        // 重置执行撤银行节点提案标志
                        heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, false);
                    } else if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.ADDCOIN) {
                        // 执行币种添加到稳定币兑换交易对里
                        String[] split = po.getContent().split("-");
                        int assetChainId = Integer.parseInt(split[0].trim());
                        int assetId = Integer.parseInt(split[1].trim());
                        String stablePairAddress = AddressTool.getStringAddressByBytes(po.getAddress());
                        SwapCall.addCoinForAddStable(chainId, stablePairAddress, assetChainId, assetId);
                    } else if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.MANAGE_STABLE_PAIR_FOR_SWAP_TRADE) {
                        // 执行管理稳定币交易对-用于Swap交易
                        String stablePairAddress = AddressTool.getStringAddressByBytes(po.getAddress());
                        if ("REMOVE".equals(po.getContent())) {
                            SwapCall.removeStablePairForSwapTrade(chainId, stablePairAddress);
                        } else {
                            SwapCall.addStablePairForSwapTrade(chainId, stablePairAddress);
                        }
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
                        docking.txConfirmedCompleted(heterogeneousTxHash, blockHeader.getHeight(), proposalHash.toHex());

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
                boolean rs = proposalExeStorageService.save(chain, proposalHash.toHex(), tx.getHash().toHex());
                if (!rs) {
                    chain.getLogger().error("[commit] 确认提案执行交易 保存失败 hash:{}, proposalType:{}", tx.getHash().toHex(), txData.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[commit] 确认提案执行交易 hash:{} proposalType:{}",
                        tx.getHash().toHex(), ProposalTypeEnum.getEnum(txData.getType()));
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollbackProtocol16(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    private boolean rollbackProtocol16(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
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
                NulsHash proposalHash;
                if (txData.getType() == ProposalTypeEnum.UPGRADE.value()) {
                    ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
                    heterogeneousChainId = upgradeTxData.getHeterogeneousChainId();
                    heterogeneousTxHash = upgradeTxData.getHeterogeneousTxHash();
                    proposalHash = upgradeTxData.getNerveTxHash();
                    // 更新合约版本号
                    ProposalPO po = this.proposalStorageService.find(chain, proposalHash);
                    String[] split = po.getContent().split("-");
                    byte oldVersion = Integer.valueOf(split[0].trim()).byteValue();
                    docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
                    // 兼容非以太系地址 update by pierre at 2021/11/16
                    String oldMultySignAddress = docking.getAddressString(upgradeTxData.getOldAddress());
                    docking.updateMultySignAddressProtocol16(oldMultySignAddress, oldVersion);
                    heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, oldMultySignAddress);
                }else {
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    proposalHash = businessData.getProposalTxHash();
                    ProposalPO po = this.proposalStorageService.find(chain, businessData.getProposalTxHash());
                    if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED) {
                        // 重置执行撤银行节点提案标志
                        heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, true);
                    } else if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.MANAGE_STABLE_PAIR_FOR_SWAP_TRADE) {
                        // 执行回滚 移除稳定币交易对-用于Swap交易
                        String stablePairAddress = AddressTool.getStringAddressByBytes(po.getAddress());
                        if ("REMOVE".equals(po.getContent())) {
                            SwapCall.addStablePairForSwapTrade(chainId, stablePairAddress);
                        } else {
                            SwapCall.removeStablePairForSwapTrade(chainId, stablePairAddress);
                        }
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
                boolean rs = proposalExeStorageService.delete(chain, proposalHash.toHex());
                if (!rs) {
                    chain.getLogger().error("[commit] 确认提案执行交易 保存失败 hash:{}, proposalType:{}", tx.getHash().toHex(), txData.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[rollback] 确认提案交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commitProtocol16(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}
