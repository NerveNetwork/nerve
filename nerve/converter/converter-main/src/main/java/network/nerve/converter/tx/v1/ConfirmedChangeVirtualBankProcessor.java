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
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.ConfirmedChangeVirtualBankPO;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.storage.CfmChangeBankStorageService;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.storage.MergeComponentStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * 确认虚拟银行变更交易
 *
 * @author: Loki
 * @date: 2020-02-28
 */
@Component("ConfirmedChangeVirtualBankV1")
public class ConfirmedChangeVirtualBankProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private HeterogeneousService heterogeneousService;
    @Autowired
    private MergeComponentStorageService mergeComponentStorageService;
    @Autowired
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService;

    @Override
    public int getType() {
        return TxType.CONFIRM_CHANGE_VIRTUAL_BANK;
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
            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                String originalHash = txData.getChangeVirtualBankTxHash().toHex();
                if (setDuplicate.contains(originalHash)) {
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, originalHash);
                if (null != po) {
                    // 说明确认交易业务重复,该原始交易已经有一个确认交易 已确认
                    failsList.add(tx);
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

                // 签名验证
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
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for (Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                boolean rs = cfmChangeBankStorageService.save(chain,
                        new ConfirmedChangeVirtualBankPO(txData.getChangeVirtualBankTxHash(), tx.getHash()));
                if (!rs) {
                    chain.getLogger().error("[commit] Save confirmed change virtual bank tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                /**
                 * 根据合并交易时的key(第一个交易hash), 将调用异构链状态复原
                 * 只有虚拟银行节点才会有该记录
                 */
                NulsHash changeVirtualBankTxHash = txData.getChangeVirtualBankTxHash();
                /**
                 * 记录已调用过异构链的交易(防止task 合并变更交易数据不一致)
                 * 一定放在设置 HeterogeneousChangeBankExecuting 状态之前进行保存,
                 * 异构task线程的执行会对该状态产生致命影响.
                 */
                asyncProcessedTxStorageService.saveComponentCall(chain, changeVirtualBankTxHash.toHex());

                if (isCurrentDirector && syncStatus == SyncStatusEnum.RUNNING.value()) {
                    // 更新异构链组件交易状态 // add by Mimi at 2020-03-12
                    List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                    for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                        heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedCompleted(bank.getHeterogeneousTxHash(), blockHeader.getHeight());
                    }
                    // 移除收集的异构链确认信息 // add by Mimi at 2020-03-12
                    heterogeneousConfirmedChangeVBStorageService.deleteByTxHash(tx.getHash().toHex());
                }

                MergedComponentCallPO mergedTxPO = mergeComponentStorageService.findMergedTx(chain, changeVirtualBankTxHash.toHex());
                if (null != mergedTxPO) {
                    /**
                     * 因为可能有多个变更交易合并执行异构链, 但只有一个key,
                     * 所以在多个确认交易中(确认交易是独立的 不会合并) 要找到那个key来重置状态.
                      */
                    heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, false);
                } else if (null != signAccountDTO) {
                    // 如果当前节点 在原始变更交易in(加入)里, 则需要复原状态
                    Transaction originalTx = TransactionCall.getConfirmedTx(chain, changeVirtualBankTxHash);
                    ChangeVirtualBankTxData originalTxData = ConverterUtil.getInstance(originalTx.getTxData(), ChangeVirtualBankTxData.class);
                    List<byte[]> inAgents = originalTxData.getInAgents();
                    if (null != inAgents) {
                        for (byte[] agentAddress : inAgents) {
                            VirtualBankDirector director = chain.getDirectorByAgent(AddressTool.getStringAddressByBytes(agentAddress));
                            if (null != director && director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, false);
                                chain.getLogger().info("[ChangeVirtualBank] 当前节点加入虚拟银行变更交易已确认, 复原异构链调用状态:{}", chain.getHeterogeneousChangeBankExecuting().get());
                                break;
                            }
                        }
                    }
                }

                chain.getLogger().info("[commit] 确认虚拟银行变更交易 hash:{}, 原始变更交易hash:{}", tx.getHash().toHex(), txData.getChangeVirtualBankTxHash().toHex());
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
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for (Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                boolean rs = cfmChangeBankStorageService.delete(chain, txData.getChangeVirtualBankTxHash().toHex());
                if (!rs) {
                    chain.getLogger().error("[rollback] remove confirmed change virtual bank tx failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                if (isCurrentDirector) {
                    // 更新异构链组件交易状态 // add by Mimi at 2020-03-13
                    List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                    for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                        heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedRollback(bank.getHeterogeneousTxHash());
                    }
                }

                /**
                 * 根据合并交易时的key(第一个交易hash), 将调用异构链状态复原
                 * 只有虚拟银行节点才会有该记录
                 */
                NulsHash changeVirtualBankTxHash = txData.getChangeVirtualBankTxHash();
                MergedComponentCallPO mergedTxPO = mergeComponentStorageService.findMergedTx(chain, changeVirtualBankTxHash.toHex());
                if (null != mergedTxPO) {
                    heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                } else if (null != signAccountDTO) {
                    // 如果当前节点 在原始变更交易in(加入)里, 则需要复原状态
                    Transaction originalTx = TransactionCall.getConfirmedTx(chain, changeVirtualBankTxHash);
                    ChangeVirtualBankTxData originalTxData = ConverterUtil.getInstance(originalTx.getTxData(), ChangeVirtualBankTxData.class);
                    List<byte[]> inAgents = originalTxData.getInAgents();
                    if (null != inAgents) {
                        for (byte[] agentAddress : inAgents) {
                            VirtualBankDirector director = chain.getDirectorByAgent(AddressTool.getStringAddressByBytes(agentAddress));
                            if (null == director || director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                                chain.getLogger().info("[ChangeVirtualBank] 当前节点加入虚拟银行变更确认交易已回滚, 异构链调用状态仍不变:{}", chain.getHeterogeneousChangeBankExecuting().get());
                                break;
                            }
                        }
                    }
                }
                /**
                 * 记录已调用过异构链的交易(防止task 合并变更交易数据不一致)
                 */
                asyncProcessedTxStorageService.removeComponentCall(chain, changeVirtualBankTxHash.toHex());
                chain.getLogger().debug("[rollback] 确认虚拟银行变更交易 hash:{}", tx.getHash().toHex());
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
