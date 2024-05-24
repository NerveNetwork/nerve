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
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.ComponentCalledPO;
import network.nerve.converter.model.po.ConfirmedChangeVirtualBankPO;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * Confirm virtual bank change transaction
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
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;

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
            //Check for duplicate transactions within the block business
            Set<String> setDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                String originalHash = txData.getChangeVirtualBankTxHash().toHex();
                if (setDuplicate.contains(originalHash)) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, originalHash);
                if (null != po) {
                    // Explanation: Confirmation of duplicate transaction business,The original transaction already has a confirmed transaction Confirmed
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
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        try {
            boolean isCurrentDirector = VirtualBankUtil.isCurrentDirector(chain);
            for (Transaction tx : txs) {
                NulsHash hash = tx.getHash();
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                boolean rs = cfmChangeBankStorageService.save(chain,
                        new ConfirmedChangeVirtualBankPO(txData.getChangeVirtualBankTxHash(), hash));
                if (!rs) {
                    chain.getLogger().error("[commit] Save confirmed change virtual bank tx failed. hash:{}, type:{}", hash.toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                /**
                 * Based on the merger transactionkey(First transactionhash), Call heterogeneous chain state recovery
                 * Only virtual bank nodes will have this record
                 */
                NulsHash changeVirtualBankTxHash = txData.getChangeVirtualBankTxHash();
                /**
                 * Record transactions that have been called on heterogeneous chains(preventtask Inconsistent merger and change transaction data)
                 * Be sure to place it in the settings HeterogeneousChangeBankExecuting Save before status,
                 * isomerismtaskThe execution of threads can have a fatal impact on this state.
                 *
                 * Due to other nodes potentially executing the transaction faster,Causing the current node(Virtual Bank Members),
                 * Receive confirmation of the transaction first, However, the transaction still exists in the queue that is ready to call the heterogeneous chain, Not yet executed,
                 * Need to store first to prevent the queue from executing the transaction repeatedly.
                 */
                ComponentCalledPO callPO = new ComponentCalledPO(
                        changeVirtualBankTxHash.toHex(),
                        -1,
                        true);
                asyncProcessedTxStorageService.saveComponentCall(chain, callPO, false);

                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(bank.getHeterogeneousChainId());
                    if (docking != null) {
                        docking.txConfirmedCheck(bank.getHeterogeneousTxHash(), blockHeader.getHeight(), changeVirtualBankTxHash.toHex(), tx.getRemark());
                    }
                }

                if (isCurrentDirector && syncStatus == SyncStatusEnum.RUNNING.value()) {
                    // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-12
                    for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                        heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedCompleted(bank.getHeterogeneousTxHash(), blockHeader.getHeight(), changeVirtualBankTxHash.toHex(), tx.getRemark());
                    }
                    // Remove the collected heterogeneous chain confirmation information // add by Mimi at 2020-03-12
                    heterogeneousConfirmedChangeVBStorageService.deleteByTxHash(hash.toHex());
                    txSubsequentProcessStorageService.deleteBackup(chain, txData.getChangeVirtualBankTxHash().toHex());
                }

                MergedComponentCallPO mergedTxPO = mergeComponentStorageService.findMergedTx(chain, changeVirtualBankTxHash.toHex());
                if (null != mergedTxPO) {
                    /**
                     * Because there may be multiple change transactions merging and executing heterogeneous chains, But there's only onekey,
                     * So in multiple confirmed transactions(Confirm that the transaction is independent Will not merge) To find thatkeyTo reset the status.
                      */
                    heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, false);
                    chain.getLogger().info("[ChangeVirtualBank] Virtual banking changes complete,Reset heterogeneous chain call status(Open the door):{}", chain.getHeterogeneousChangeBankExecuting().get());
                } else if (null != signAccountDTO) {
                    // If the current node In the original change transactionin(join)in, Then it is necessary to restore the state
                    Transaction originalTx = TransactionCall.getConfirmedTx(chain, changeVirtualBankTxHash);
                    ChangeVirtualBankTxData originalTxData = ConverterUtil.getInstance(originalTx.getTxData(), ChangeVirtualBankTxData.class);
                    List<byte[]> inAgents = originalTxData.getInAgents();
                    if (null != inAgents) {
                        for (byte[] agentAddress : inAgents) {
                            VirtualBankDirector director = chain.getDirectorByAgent(AddressTool.getStringAddressByBytes(agentAddress));
                            if (null != director && director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, false);
                                chain.getLogger().info("[ChangeVirtualBank] Current node joining virtual bank change transaction confirmed, Restore heterogeneous chain call status(Open the door):{}", chain.getHeterogeneousChangeBankExecuting().get());
                                break;
                            }
                        }
                    }
                }

                chain.getLogger().info("[commit] Confirm virtual bank change transaction hash:{}, Original change transactionhash:{}", hash.toHex(), txData.getChangeVirtualBankTxHash().toHex());
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
                    // Update the transaction status of heterogeneous chain components // add by Mimi at 2020-03-13
                    List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                    for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
                        heterogeneousDockingManager.getHeterogeneousDocking(bank.getHeterogeneousChainId()).txConfirmedRollback(bank.getHeterogeneousTxHash());
                    }
                }

                /**
                 * Based on the merger transactionkey(First transactionhash), Call heterogeneous chain state recovery
                 * Only virtual bank nodes will have this record
                 */
                NulsHash changeVirtualBankTxHash = txData.getChangeVirtualBankTxHash();
                MergedComponentCallPO mergedTxPO = mergeComponentStorageService.findMergedTx(chain, changeVirtualBankTxHash.toHex());
                if (null != mergedTxPO) {
                    heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                } else if (null != signAccountDTO) {
                    // If the current node In the original change transactionin(join)in, Then it is necessary to restore the state
                    Transaction originalTx = TransactionCall.getConfirmedTx(chain, changeVirtualBankTxHash);
                    ChangeVirtualBankTxData originalTxData = ConverterUtil.getInstance(originalTx.getTxData(), ChangeVirtualBankTxData.class);
                    List<byte[]> inAgents = originalTxData.getInAgents();
                    if (null != inAgents) {
                        for (byte[] agentAddress : inAgents) {
                            VirtualBankDirector director = chain.getDirectorByAgent(AddressTool.getStringAddressByBytes(agentAddress));
                            if (null == director || director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                                chain.getLogger().info("[ChangeVirtualBank] Current node joining virtual bank change confirmation transaction has been rolled back, Heterogeneous chain call status remains unchanged:{}", chain.getHeterogeneousChangeBankExecuting().get());
                                break;
                            }
                        }
                    }
                }
                /**
                 * Record transactions that have been called on heterogeneous chains(preventtask Inconsistent merger and change transaction data)
                 */
                asyncProcessedTxStorageService.removeComponentCall(chain, changeVirtualBankTxHash.toHex());
                chain.getLogger().debug("[rollback] Confirm virtual bank change transaction hash:{}", tx.getHash().toHex());
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
