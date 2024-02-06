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

package network.nerve.converter.core.thread.task;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.message.VirtualBankSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import static network.nerve.converter.config.ConverterContext.FEE_ADDITIONAL_HEIGHT;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_1;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_2;

/**
 * Transaction confirmation follow-up processing scheduled task
 * Calling heterogeneous chain components after processing transactions,Generate new transactions through association, etc
 *
 * @author: Loki
 * @date: 2020-03-10
 */
public class CfmTxSubsequentProcessTask implements Runnable {
    private Chain chain;


    public CfmTxSubsequentProcessTask(Chain chain) {
        this.chain = chain;
    }

    private HeterogeneousDockingManager heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
    private AsyncProcessedTxStorageService asyncProcessedTxStorageService = SpringLiteContext.getBean(AsyncProcessedTxStorageService.class);
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService = SpringLiteContext.getBean(TxSubsequentProcessStorageService.class);
    private MergeComponentStorageService mergeComponentStorageService = SpringLiteContext.getBean(MergeComponentStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private VirtualBankStorageService virtualBankStorageService = SpringLiteContext.getBean(VirtualBankStorageService.class);
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService = SpringLiteContext.getBean(VirtualBankAllHistoryStorageService.class);
    private VirtualBankService virtualBankService = SpringLiteContext.getBean(VirtualBankService.class);
    private HeterogeneousService heterogeneousService = SpringLiteContext.getBean(HeterogeneousService.class);
    private HeterogeneousAssetHelper heterogeneousAssetHelper = SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
    private ComponentSignStorageService componentSignStorageService = SpringLiteContext.getBean(ComponentSignStorageService.class);
    private ConverterCoreApi converterCoreApi = SpringLiteContext.getBean(ConverterCoreApi.class);

    @Override
    public void run() {
        try {
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            out:
            while (!pendingTxQueue.isEmpty()) {
                // Only remove,Do not remove head elements
                TxSubsequentProcessPO pendingPO = pendingTxQueue.peekFirst();
                Transaction tx = pendingPO.getTx();
                if (converterCoreApi.skippedTransaction(tx.getHash().toHex())) {
                    // Determine if there is a problem with the transaction, Remove from queue, And remove it from the persistent library
                    chain.getLogger().info("[Heterogeneous chain pending queue] Historical legacy problem data, Remove transaction, hash:{}", tx.getHash().toHex());
                    // And remove it from the persistence library
                    txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                    componentSignStorageService.delete(chain, tx.getHash().toHex());
                    // Successfully removed queue header elements
                    pendingTxQueue.remove();
                    continue;
                }
                if (!pendingPO.getRetry() && null != asyncProcessedTxStorageService.getComponentCall(chain, tx.getHash().toHex())) {
                    // Judging that it has been executed, Remove from queue, And remove it from the persistent library
                    chain.getLogger().info("[Heterogeneous chain pending queue] Executed,Remove transaction, hash:{}", tx.getHash().toHex());
                    // And remove it from the persistence library
                    txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                    // Successfully removed queue header elements
                    pendingTxQueue.remove();
                    continue;
                }
                // Determine if it has been confirmed
                if (null == TransactionCall.getConfirmedTx(chain, tx.getHash())) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        chain.getLogger().error("[Heterogeneous chain pending queue] Transaction unconfirmed(Remove processing), hash:{}", tx.getHash().toHex());
                        // And remove it from the persistence library
                        txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                        // remove
                        pendingTxQueue.remove();
                        continue;
                    }
                    pendingPO.setIsConfirmedVerifyCount(pendingPO.getIsConfirmedVerifyCount() + 1);
                    // Terminate this execution and wait for the next execution to check if the transaction is confirmed again
                    break;
                }
                switch (tx.getType()) {
                    case TxType.CHANGE_VIRTUAL_BANK:
                        // Handling bank changes
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            if (chain.getHeterogeneousChangeBankExecuting().get()) {
                                // Virtual bank change heterogeneous chain transaction is currently being executed, Pause new heterogeneous processing
                                chain.getLogger().info("[Task-CHANGE_VIRTUAL_BANK] pause new change Executing virtual bank change heterogeneous chain transaction, Suspend new bank change heterogeneous processing!");
                                break out;
                            }
                            changeVirtualBankProcessor();
                            // Subsequent batch processing has been completed
                            continue;
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            if (!changeVirtualBankByzantineProcessor(pendingPO)) {
                                break out;
                            }
                        }
                        break;
                    case TxType.WITHDRAWAL:
                    case TxType.ONE_CLICK_CROSS_CHAIN:
                        // Processing withdrawals
                        //if (chain.getHeterogeneousChangeBankExecuting().get()) {
                        //    // Virtual bank change heterogeneous chain transaction is currently being executed, Pause new heterogeneous processing
                        //    chain.getLogger().info("[Task-change_virtual_bank] pause withdrawal Executing virtual bank change heterogeneous chain transaction, Suspend new heterogeneous processing of withdrawals!");
                        //    break out;
                        //}
                        if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                            withdrawalProcessor(pendingPO);
                        } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                            int feeChangeVersion = chain.getWithdrawFeeChangeVersion(tx.getHash().toHex());
                            if (pendingPO.isWithdrawExceedErrorTime(feeChangeVersion, 10)) {
                                chain.getLogger().warn("[withdraw] Insufficient withdrawal fees, retry count exceeded limit, temporarily suspending processing of current advance tasks, feeChangeVersion: {}, txHash: {}", feeChangeVersion, tx.getHash().toHex());
                                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                            }
                            if (!withdrawalByzantineProcessor(pendingPO)) {
                                TxSubsequentProcessPO po = chain.getPendingTxQueue().poll();
                                chain.getPendingTxQueue().addLast(po);
                                continue;
                            }
                        }
                        break;
                    case TxType.CONFIRM_WITHDRAWAL:
                        // Confirm withdrawal transactions Processing subsidy handling fee transactions
                        confirmWithdrawalDistributionFee(pendingPO);
                        break;
                    case TxType.INITIALIZE_HETEROGENEOUS:
                        // Initialize joining a new heterogeneous chain
                        initializeHeterogeneousProcessor(pendingPO);
                        break;
                    case TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING:
                        // Process heterogeneous chain contract asset registration
                        heterogeneousContractAssetRegCompleteProcessor(pendingPO);
                        break;
                    case TxType.CONFIRM_PROPOSAL:
                        // Confirmation of subsidy handling fees for proposals
                        confirmProposalDistributionFee(pendingPO);
                        break;
                    case TxType.RESET_HETEROGENEOUS_VIRTUAL_BANK:
                        resetHeterogeneousVirtualBank(pendingPO);
                        // Subsequent batch processing has been completed
                        break;
                    default:
                }
                if (chain.getCurrentIsDirector().get()) {
                    // Store successfully executed transactionshash, Executed items will no longer be executed (When the current node is in synchronous block mode,We also need to save thishash, Indicates that it has been executed)
                    ComponentCalledPO callPO = new ComponentCalledPO(
                            tx.getHash().toHex(),
                            pendingPO.getBlockHeader().getHeight(),
                            false);
                    asyncProcessedTxStorageService.saveComponentCall(chain, callPO, pendingPO.getCurrentQuit());
                }

                chain.getLogger().info("[Heterogeneous chain pending queue] Successfully executed to remove transaction, hash:{}", tx.getHash().toHex());
                // And remove it from the persistence library
                txSubsequentProcessStorageService.delete(chain, tx.getHash().toHex());
                // Successfully removed queue header elements
                pendingTxQueue.remove();
            }
        } catch (Exception e) {

            /**
             * If the team leader trade can change positions, Then put it at the end of the team,
             * Otherwise, traverse in order,Scan an element that can change position, Place at the head of the team
             */
            try {
                TxSubsequentProcessPO pendingPO = chain.getPendingTxQueue().peekFirst();
                if (null == pendingPO) {
                    return;
                }
                if (TxType.CHANGE_VIRTUAL_BANK != pendingPO.getTx().getType()) {
                    TxSubsequentProcessPO po = chain.getPendingTxQueue().poll();
                    chain.getPendingTxQueue().addLast(po);
                } else {
                    /**
                     * Currently processing virtual bank changes(Exception thrown), Just go from behind and place a transaction that is not a virtual bank change at the beginning of the team
                     */
                    Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
                    TxSubsequentProcessPO toFirstPO = null;
                    while (it.hasNext()) {
                        TxSubsequentProcessPO po = it.next();
                        if (TxType.CHANGE_VIRTUAL_BANK != po.getTx().getType()) {
                            toFirstPO = po;
                            it.remove();
                            break;
                        }
                    }
                    if (null != toFirstPO) {
                        chain.getPendingTxQueue().addFirst(toFirstPO);
                    }
                }
            } catch (Exception ex) {
                chain.getLogger().error(ex);
            }

            if (e instanceof NulsException) {
                if (ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW.equals(((NulsException) e).getErrorCode())) {
                    return;
                }
            }
            chain.getLogger().error(e);
        }
    }

    /**
     * Processing virtual banking change business
     * Trading through heterogeneous chain addresseshashautograph, And broadcast signature
     * Determine and send heterogeneous chain transactions
     *
     * @param pendingPO
     * @return true:Need to delete elements from the queue(Heterogeneous chain called, Or no need to, No permission to execute, etc) false: Put the team at the end of the line
     * @throws Exception
     */
    private boolean changeVirtualBankByzantineProcessor(TxSubsequentProcessPO pendingPO) throws Exception {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        if (pendingPO.getCurrentJoin()) {
            chain.getLogger().info("Virtual Bank Change, Execution signature[Current node joining virtual bank(Do not sign), Close the execution of new heterogeneous chain changes(close)]");
            heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
            return true;
        }
        Transaction tx = pendingPO.getTx();
        NulsHash hash = tx.getHash();
        String txHash = hash.toHex();
        // Determine if you have received the message, And signed it
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, pendingPO.getTx().getHash().toHex());
        boolean sign = false;
        if (null != compSignPO) {
            if (!compSignPO.getCurrentSigned()) {
                sign = true;
            } else if (pendingPO.getRetry() && !pendingPO.isRetryVirtualBankInit()) {
                compSignPO.setCurrentSigned(false);
                // Clear signature list on retry
                if (compSignPO.getListMsg() != null) {
                    compSignPO.getListMsg().clear();
                }
                if (compSignPO.getCallParms() != null) {
                    compSignPO.getCallParms().clear();
                }
                compSignPO.setByzantinePass(false);
                compSignPO.setCompleted(false);
                sign = true;
                pendingPO.setRetryVirtualBankInit(true);
            }
        } else {
            sign = true;
        }
        if (sign) {
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
            int inSize = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
            int outSize = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
            List<HeterogeneousSign> currentSignList = new ArrayList<>();
            for (IHeterogeneousChainDocking docking : hInterfaces) {
                int hChainId = docking.getChainId();
                // Assembly with added parameters
                String[] inAddress = new String[inSize];
                if (null != txData.getInAgents()) {
                    getHeterogeneousAddress(chain, hChainId, inAddress, txData.getInAgents(), pendingPO);
                }
                String[] outAddress = new String[outSize];
                if (null != txData.getOutAgents()) {
                    getHeterogeneousAddress(chain, hChainId, outAddress, txData.getOutAgents(), pendingPO);
                }
                // Verify message signature
                String signStrData = docking.signManagerChangesII(txHash, inAddress, outAddress, 1);
                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                HeterogeneousSign currentSign = new HeterogeneousSign(
                        new HeterogeneousAddress(hChainId, currentHaddress),
                        HexUtil.decode(signStrData));
                currentSignList.add(currentSign);
            }
            ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                    hash, currentSignList);
            // Initialize the object for storing signatures
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);

            // Broadcast current node signature message
            if (pendingPO.getRetry()) {
                VirtualBankSignMessage retryMessage = VirtualBankSignMessage.of(currentMessage, pendingPO.getPrepare());
                NetWorkCall.broadcast(chain, retryMessage, ConverterCmdConstant.RETRY_VIRTUAL_BANK_MESSAGE);
                pendingPO.setRetry(false);
            } else {
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            }
        }
        // Change retry preparation stage
        if (pendingPO.getRetry() && pendingPO.getPrepare() == 1) {
            return true;
        }
        boolean rs = false;
        if (compSignPO.getByzantinePass() && (!chain.getHeterogeneousChangeBankExecuting().get() || pendingPO.getRetry())) {
            if (!compSignPO.getCompleted()) {
                // Execute calls to heterogeneous chains
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().error("Virtual Bank Change, Call heterogeneous chain parameter is empty");
                    return false;
                }
                if (callParmsList.size() != hInterfaces.size()) {
                    chain.getLogger().error("Virtual Bank Change, Insufficient number of calls to heterogeneous chains, Number of calls: {}, Current number of networks: {}", callParmsList.size(), hInterfaces.size());
                    return false;
                }
                for (IHeterogeneousChainDocking docking : hInterfaces) {
                    for (ComponentCallParm callParm : callParmsList) {
                        if (docking.getChainId() == callParm.getHeterogeneousId()) {
                            boolean vaildPass = docking.validateManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    callParm.getSigned());
                            if (!vaildPass) {
                                chain.getLogger().error("[Heterogeneous chain address signature message-Heterogeneous chain verification failed-changeVirtualBank] Verification failed when calling heterogeneous chain components. hash:{}, ethHash:{}", txHash);
                                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
                            }
                            break;
                        }
                    }
                }
                for (IHeterogeneousChainDocking docking : hInterfaces) {
                    for (ComponentCallParm callParm : callParmsList) {
                        if (docking.getChainId() == callParm.getHeterogeneousId()) {
                            String ethTxHash = docking.createOrSignManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    callParm.getSigned());
                            chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-changeVirtualBank] Calling heterogeneous chain components to execute virtual bank changes. hash:{}, ethHash:{}", txHash, ethTxHash);
                            break;
                        }
                    }
                }
            }
            /**
             * Successfully tuned heterogeneous chain
             * Starting processing bank changes to heterogeneous chain transaction mode
             */
            heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
            chain.getLogger().info("[Start executing virtual banking heterogeneous chain transactions, Close the execution of new heterogeneous chain changes(close)] hash:{}", txHash);
            // Preserve the storage mode of the merge mechanism, Wait until the transaction is confirmed and match the door opening
            List<String> hashList = new ArrayList<>();
            hashList.add(txHash);
            mergeComponentStorageService.saveMergeComponentCall(chain, txHash, new MergedComponentCallPO(hashList));
            compSignPO.setCompleted(true);
            rs = true;
        }
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);

        // 2020/12/24 During the Byzantine process of changing virtual banks,Move transactions from the back of the queue to the front for execution,Prevent infinite loop waiting
        if (!rs) {
            Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
            TxSubsequentProcessPO toFirstPO = null;
            while (it.hasNext()) {
                TxSubsequentProcessPO po = it.next();
                if (TxType.CHANGE_VIRTUAL_BANK != po.getTx().getType()) {
                    toFirstPO = po;
                    it.remove();
                    break;
                }
            }
            if (null != toFirstPO) {
                chain.getPendingTxQueue().addFirst(toFirstPO);
            }

            // add by pierre at 2022/3/11, Identify duplicate tasks for changes and remove duplicates
            it = chain.getPendingTxQueue().iterator();
            Map<String, List<TxSubsequentProcessPO>> map = null;
            boolean hasMany = false;
            while (it.hasNext()) {
                TxSubsequentProcessPO po = it.next();
                if (TxType.CHANGE_VIRTUAL_BANK == po.getTx().getType()) {
                    map = map == null ? new HashMap<>() : map;
                    List<TxSubsequentProcessPO> list = map.computeIfAbsent(po.getTx().getHash().toHex(), a -> new ArrayList<>());
                    list.add(po);
                    if (list.size() > 1) {
                        hasMany = true;
                    }
                }
            }
            if (hasMany) {
                Collection<List<TxSubsequentProcessPO>> values = map.values();
                for (List<TxSubsequentProcessPO> list : values) {
                    if (list.size() <= 1) {
                        continue;
                    }
                    // In repeated change tasks, only the tasks that are being retried are retained, and the rest are removed from the queue
                    for (TxSubsequentProcessPO po : list) {
                        if (po.getRetry()) {
                            list.remove(po);
                            break;
                        }
                    }
                    for (TxSubsequentProcessPO po : list) {
                        chain.getPendingTxQueue().remove(po);
                    }
                }
            }

            if (null != toFirstPO) {
                chain.getPendingTxQueue().addFirst(toFirstPO);
            }

        }
        return rs;
    }

    /**
     * Based on the node address list And heterogeneous chainsId, Obtain heterogeneous chain addresses
     * Special treatment, If it is currently exiting, Then frompendingPOObtaining heterogeneous chain information from the dropout
     *
     * @param chain
     * @param heterogeneousChainId
     * @param address
     * @param list
     * @param pendingPO
     * @throws NulsException
     */
    private void getHeterogeneousAddress(Chain chain, int heterogeneousChainId, String[] address, List<byte[]> list, TxSubsequentProcessPO pendingPO) throws NulsException {
        for (int i = 0; i < list.size(); i++) {
            byte[] bytes = list.get(i);
            String agentAddress = AddressTool.getStringAddressByBytes(bytes);
            String hAddress = null;
            // If it is currently exiting, Then frompendingPOObtaining heterogeneous chain information from the dropout
            if (pendingPO.getCurrentQuit() && agentAddress.equals(pendingPO.getCurrentQuitDirector().getAgentAddress())) {
                HeterogeneousAddress heterogeneousAddress =
                        pendingPO.getCurrentQuitDirector().getHeterogeneousAddrMap().get(heterogeneousChainId);
                hAddress = heterogeneousAddress.getAddress();
            } else {
                String signAddress = virtualBankAllHistoryStorageService.findSignAddressByAgentAddress(chain, agentAddress);
                if (StringUtils.isNotBlank(signAddress)) {
                    VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                    HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                    hAddress = heterogeneousAddress.getAddress();
                }
            }
            if (StringUtils.isBlank(hAddress)) {
                chain.getLogger().error("Heterogeneous chain address signature message[changeVirtualBank] No heterogeneous chain address obtained, agentAddress:{}", agentAddress);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            address[i] = hAddress;
        }
    }


    /**
     * Processing heterogeneous chain withdrawals
     * Trading through heterogeneous chain addresseshashautograph, And broadcast signature
     * Determine and send heterogeneous chain transactions
     *
     * @param pendingPO
     * @return true:Need to delete elements from the queue(Heterogeneous chain called, Or no need to, No permission to execute, etc) false: Put the team at the end of the line
     * @throws NulsException
     */
    private boolean withdrawalByzantineProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return true;
        }
        Transaction tx = pendingPO.getTx();
        NulsHash hash = tx.getHash();
        String txHash = hash.toHex();
        // Determine if you have received the message, And signed it
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, txHash);
        boolean needSign = false;
        if (null != compSignPO) {
            if (!compSignPO.getCurrentSigned()) {
                needSign = true;
            }
        } else {
            needSign = true;
        }
        if (needSign) {
            int htgChainId = 0;
            String toAddress = null;
            if (TxType.WITHDRAWAL == tx.getType()) {
                WithdrawalTxData txData1 = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                htgChainId = txData1.getHeterogeneousChainId();
                toAddress = txData1.getHeterogeneousAddress();
            } else if (TxType.ONE_CLICK_CROSS_CHAIN == tx.getType()) {
                OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
                htgChainId = txData.getDesChainId();
                toAddress = txData.getDesToAddress();
            }
            CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
            HeterogeneousAssetInfo assetInfo = null;
            CoinTo withdrawCoinTo = null;
            byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                    assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    if (assetInfo != null) {
                        withdrawCoinTo = coinTo;
                        break;
                    }
                }
            }
            if (null == assetInfo) {
                chain.getLogger().error("[Heterogeneous chain address signature message-withdraw] no withdrawCoinTo. hash:{}", txHash);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
            int heterogeneousChainId = assetInfo.getChainId();
            BigInteger amount = withdrawCoinTo.getAmount();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
            String signStrData = docking.signWithdrawII(txHash, toAddress, amount, assetInfo.getAssetId());
            String currentHaddress = docking.getCurrentSignAddress();
            if (StringUtils.isBlank(currentHaddress)) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            HeterogeneousAddress heterogeneousAddress = new HeterogeneousAddress(heterogeneousChainId, currentHaddress);
            HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
            List<HeterogeneousSign> listSign = new ArrayList<>();
            listSign.add(currentSign);
            ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                    hash, listSign);
            // Initialize the object for storing signatures
            if (null == compSignPO) {
                compSignPO = new ComponentSignByzantinePO(hash, new ArrayList<>(), false, false);
            } else if (null == compSignPO.getListMsg()) {
                compSignPO.setListMsg(new ArrayList<>());
            }
            compSignPO.getListMsg().add(currentMessage);
            compSignPO.setCurrentSigned(true);
            // Broadcast current node signature message
            NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
            chain.getLogger().info("[withdraw] Calling heterogeneous chain components to execute signatures, Send signed message. hash:{}", txHash);
        }

        boolean rs = false;
        if (compSignPO.getByzantinePass()) {
            if (pendingPO.getRetry() || !compSignPO.getCompleted()) {
                // Execute calls to heterogeneous chains
                List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                if (null == callParmsList) {
                    chain.getLogger().info("[withdraw] Call heterogeneous chain parameter is empty");
                    return false;
                }
                ComponentCallParm callParm = callParmsList.get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(callParm.getHeterogeneousId());
                if (!converterCoreApi.checkNetworkRunning(docking.getChainId())) {
                    chain.getLogger().info("[withdraw] Test network[{}]Run Pause, chainId: {}", docking.getChainSymbol(), docking.getChainId());
                    throw new NulsException(ConverterErrorCode.WITHDRAWAL_PAUSE);
                }
                if (chain.getLatestBasicBlock().getHeight() >= FEE_ADDITIONAL_HEIGHT) {
                    WithdrawalTotalFeeInfo totalFeeInfo = assembleTxService.calculateWithdrawalTotalFee(chain, tx);
                    boolean enoughFeeOfWithdraw;
                    // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
                    if (totalFeeInfo.isNvtAsset()) {
                        // validateNVTAs a handling fee
                        enoughFeeOfWithdraw = docking.isEnoughNvtFeeOfWithdraw(new BigDecimal(totalFeeInfo.getFee()), callParm.getAssetId(), txHash);
                    } else {
                        BigDecimal feeAmount = new BigDecimal(converterCoreApi.checkDecimalsSubtractedToNerveForWithdrawal(totalFeeInfo.getHtgMainAssetName().chainId(), 1, totalFeeInfo.getFee()));
                        // Verify heterogeneous chain master assets as transaction fees
                        // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
                        enoughFeeOfWithdraw = docking.isEnoughFeeOfWithdrawByMainAssetProtocol15(totalFeeInfo.getHtgMainAssetName(), feeAmount, callParm.getAssetId(), txHash);
                    }
                    if (!enoughFeeOfWithdraw) {
                        // Abnormal Count
                        pendingPO.increaseWithdrawErrorTime();
                        chain.getLogger().error("[withdraw] Withdrawal fee calculation, The handling fee is insufficient to cover the withdrawal fee. hash:{}, amount:{}",
                                txHash, callParm.getValue());
                        throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                    }
                }
                String ethTxHash;
                if (pendingPO.getRetry() && docking instanceof HtgDocking) {
                    // When the mechanism for resending withdrawals is executed, the withdrawal is triggered without checking the execution order, and the current node immediately executes it
                    HtgDocking htgDocking = (HtgDocking) docking;
                    ethTxHash = htgDocking.createOrSignWithdrawTxII(
                            callParm.getTxHash(),
                            callParm.getToAddress(),
                            callParm.getValue(),
                            callParm.getAssetId(),
                            callParm.getSigned(), false);
                } else {
                    ethTxHash = docking.createOrSignWithdrawTxII(
                            callParm.getTxHash(),
                            callParm.getToAddress(),
                            callParm.getValue(),
                            callParm.getAssetId(),
                            callParm.getSigned());
                }

                compSignPO.setCompleted(true);
                // The withdrawal transaction was successfully sent out, and the status of clearing additional fees has been cleared
                chain.clearWithdrawFeeChange(txHash);
                chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-withdraw] Calling heterogeneous chain components to execute withdrawals. hash:{}, ethHash:{}", txHash, ethTxHash);
            }
            rs = true;
        }
        // Store updated compSignPO
        componentSignStorageService.save(chain, compSignPO);
        return rs;
    }


    /**
     * Handle the business of changing the heterogeneous chain part of virtual banks
     * Changing heterogeneous chain multi signature addresses/Contract members
     *
     * @throws NulsException
     */
    private void changeVirtualBankProcessor() throws Exception {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        List<TxSubsequentProcessPO> mergeList = getChangeVirtualBankTxs(null);
        if (mergeList.isEmpty()) {
            chain.getLogger().error("When virtual banking changes and calls heterogeneous chains, No merged transaction data. changeVirtualBank no merged tx list");
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        if (chain.getCurrentIsDirector().get()) {
            String key = mergeList.get(0).getTx().getHash().toHex();
            /**
             * Storage Correspondence
             * key:Merge the first transaction in the listhash, value:All merger transactionshashlist
             */
            List<String> hashList = new ArrayList<>();
            mergeList.forEach(k -> hashList.add(k.getTx().getHash().toHex()));
            mergeComponentStorageService.saveMergeComponentCall(chain, key, new MergedComponentCallPO(hashList));

            try {
                Result<Integer> result = changeVirtualBankAddress(key, mergeList, hInterfaces);
                if (result.isFailed() && result.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                    // Based on the number of transactions returned Re merge and assemble
                    mergeList = getChangeVirtualBankTxs(result.getData());
                    if (mergeList.isEmpty()) {
                        chain.getLogger().error("When virtual banking changes and calls heterogeneous chains, Secondary acquisition of transaction data without any merging. changeVirtualBank no merged tx list");
                        throw new NulsException(ConverterErrorCode.DATA_ERROR);
                    }
                    String keyTwo = mergeList.get(0).getTx().getHash().toHex();
                    if (!keyTwo.equals(key)) {
                        chain.getLogger().error("[Virtual Bank Change] Secondary merger transaction keyInconsistent!");
                        throw new NulsException(ConverterErrorCode.DATA_ERROR);
                    }
                    // Delete and save again althoughkeyunanimous But the number of merger transactions may not be consistent
                    mergeComponentStorageService.removeMergedTx(chain, key);
                    List<String> hashListNew = new ArrayList<>();
                    mergeList.forEach(k -> hashListNew.add(k.getTx().getHash().toHex()));
                    mergeComponentStorageService.saveMergeComponentCall(chain, key, new MergedComponentCallPO(hashListNew));
                    Result<Integer> resultTwo = changeVirtualBankAddress(key, mergeList, hInterfaces);
                    if (resultTwo.isFailed() && resultTwo.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                        chain.getLogger().error("[Virtual Bank Change] Secondary merger transaction Sending throw failed!.");
                        throw new NulsException(resultTwo.getErrorCode());
                    }
                }
            } catch (Exception e) {
                // deletekey
                try {
                    mergeComponentStorageService.removeMergedTx(chain, key);
                } catch (Exception ex) {
                    throw ex;
                }
                throw e;
            }

        }
        // Remove elements from queue
        chain.getPendingTxQueue().removeAll(mergeList);
        for (TxSubsequentProcessPO po : mergeList) {
            String hash = po.getTx().getHash().toHex();
            if (chain.getCurrentIsDirector().get()) {
                // Store successfully executed transactionshash, Executed items will no longer be executed.
                ComponentCalledPO callPO = new ComponentCalledPO(
                        hash,
                        po.getBlockHeader().getHeight(),
                        false);
                asyncProcessedTxStorageService.saveComponentCall(chain, callPO, po.getCurrentQuit());

            }
            chain.getLogger().debug("[Heterogeneous chain pending queue] Successfully executed to remove transaction, hash:{}", hash);
            // And remove it from the persistence library
            txSubsequentProcessStorageService.delete(chain, hash);
        }
    }

    /**
     * return Extracting virtual bank change transactions from the queue for consolidation Transaction List
     *
     * @param count How many transactions can be taken from the queue at most for merging, bynullThen there is no restriction(Remove all items that meet the conditions)
     * @return
     * @throws NulsException
     */
    private List<TxSubsequentProcessPO> getChangeVirtualBankTxs(Integer count) {
        List<TxSubsequentProcessPO> mergeList = new ArrayList<>();
        Iterator<TxSubsequentProcessPO> it = chain.getPendingTxQueue().iterator();
        int current = 0;
        while (it.hasNext()) {
            TxSubsequentProcessPO po = it.next();
            Transaction tx = po.getTx();
            if (tx.getType() == TxType.CHANGE_VIRTUAL_BANK) {
                mergeList.add(po);
                if (null != count && count == ++current) {
                    break;
                }
            }
        }
        return mergeList;
    }

    /**
     * Change handling of virtual bank public address
     *
     * @param key
     * @param mergeList
     * @return
     * @throws NulsException
     */
    private Result<Integer> changeVirtualBankAddress(String key, List<TxSubsequentProcessPO> mergeList, List<IHeterogeneousChainDocking> hInterfaces) throws NulsException {
        /*
         * key: node, value: in/out frequency(in +1 ;out -1)
         * value greater than 0, Assemble to add; value less than 0, Assembly to exit; value equal to 0, Then there is no need to change the contract
         */
        Map<VirtualBankDirector, Integer> mapAllDirector = new HashMap<>();
        // Original transaction quantity before merger
        int originalMergedCount = mergeList.size();
        chain.getLogger().info("[bank-Mergerable transactions] size:{}", mergeList.size());
        for (TxSubsequentProcessPO pendingPO : mergeList) {
            chain.getLogger().info("----------------------------------------");
            chain.getLogger().info("[bank-Mergerable transactions] hash:{}", pendingPO.getTx().getHash().toHex());
            for (VirtualBankDirector director : pendingPO.getListInDirector()) {
                mapAllDirector.compute(director, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
                chain.getLogger().info("InAddress: {}", director.getAgentAddress());
            }
            for (VirtualBankDirector director : pendingPO.getListOutDirector()) {
                mapAllDirector.compute(director, (k, v) -> {
                    if (null == v) {
                        return -1;
                    } else {
                        return v - 1;
                    }
                });
                chain.getLogger().info("OutAddress: {}", director.getAgentAddress());
            }
            chain.getLogger().info("----------------------------------------");
        }
        int inCount = 0;
        int outCount = 0;
        for (Integer count : mapAllDirector.values()) {
            if (count < 0) {
                outCount++;
            } else if (count > 0) {
                inCount++;
            }
        }

        for (IHeterogeneousChainDocking hInterface : hInterfaces) {
            // Assembly with added parameters
            String[] inAddress = new String[inCount];
            int i = 0;
            for (Map.Entry<VirtualBankDirector, Integer> map : mapAllDirector.entrySet()) {
                if (map.getValue() <= 0) {
                    // Less than or equal to0Not counted, Only count added items(greater than0of)
                    continue;
                }
                VirtualBankDirector director = map.getKey();
                HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                inAddress[i] = heterogeneousAddress.getAddress();
                i++;
            }

            String[] outAddress = new String[outCount];
            i = 0;
            for (Map.Entry<VirtualBankDirector, Integer> map : mapAllDirector.entrySet()) {
                if (map.getValue() >= 0) {
                    // Greater than or equal to0Not counted, Only count exits(less than0of)
                    continue;
                }
                VirtualBankDirector director = map.getKey();
                HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(hInterface.getChainId());
                outAddress[i] = heterogeneousAddress.getAddress();
                i++;
            }

            // Create a new virtual bank heterogeneous chain with multiple signed addresses/Modify contract members
            try {
                String hash = hInterface.createOrSignManagerChangesTx(
                        key,
                        inAddress,
                        outAddress,
                        originalMergedCount);
            } catch (NulsException e) {
                if (!e.getErrorCode().equals(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY)) {
                    throw e;
                }
                try {
                    HeterogeneousChangePendingInfo Info = hInterface.getChangeVirtualBankPendingInfo(key);
                    int orginTxCount = Info.getOrginTxCount();
                    return Result.getFailed(e.getErrorCode()).setData(orginTxCount);
                } catch (Exception ex) {
                    chain.getLogger().error("[Virtual Bank Change] According to the mergerkeyException in obtaining heterogeneous chain call information");
                    ex.printStackTrace();
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
                }
            }
            //  if(StringUtils.isNotBlank(hash))Cancel the judgment because After reaching Byzantium, If a node is called normally, it will return null, Causing the following state to remain unchanged
            if (null == asyncProcessedTxStorageService.getComponentCall(chain, key)) {
                /**
                 * Successfully tuned heterogeneous chain
                 * Starting processing bank changes to heterogeneous chain transaction mode
                 */
                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                chain.getLogger().info("[Start executing virtual banking heterogeneous chain transactions, Close the execution of new heterogeneous chain changes] key:{}", key);
            }
            chain.getLogger().info("[bank-Execute changes] key:{}, inAddress:{}, outAddress:{}",
                    key, Arrays.toString(inAddress), Arrays.toString(outAddress));
        }
        return Result.getSuccess(0);
    }


    private void heterogeneousContractAssetRegCompleteProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        try {
            Transaction tx = pendingPO.getTx();
            assembleTxService.createHeterogeneousContractAssetRegCompleteTx(chain, tx);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }


    private void initializeHeterogeneousProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        Transaction tx = pendingPO.getTx();
        // Maintain virtual banking Synchronous mode also requires maintenance
        InitializeHeterogeneousTxData txData = ConverterUtil.getInstance(tx.getTxData(), InitializeHeterogeneousTxData.class);
        IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            if (!director.getSeedNode()) {
                // Create heterogeneous chain multi sign addresses for new members
                String heterogeneousAddress = heterogeneousInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                director.getHeterogeneousAddrMap().put(heterogeneousInterface.getChainId(),
                        new HeterogeneousAddress(heterogeneousInterface.getChainId(), heterogeneousAddress));
                virtualBankStorageService.update(chain, director);
                virtualBankAllHistoryStorageService.save(chain, director);
                chain.getLogger().info("[Create heterogeneous chain multi sign addresses for new members] Node address:{}, isomerismid:{}, Heterogeneous addresses:{}",
                        director.getAgentAddress(), heterogeneousInterface.getChainId(), director.getHeterogeneousAddrMap().get(heterogeneousInterface.getChainId()));
            }
        }
    }

    /**
     * Processing withdrawal transactions
     *
     * @param pendingPO
     * @throws NulsException
     */

    private void withdrawalProcessor(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        Transaction tx = pendingPO.getTx();
        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        CoinTo withdrawCoinTo = null;
        for (CoinTo coinTo : coinData.getTo()) {
            if (coinTo.getAssetsId() != chain.getConfig().getAssetId()) {
                withdrawCoinTo = coinTo;
                break;
            }
        }
        if (null == withdrawCoinTo) {
            chain.getLogger().error("[withdraw] Withdraw transaction cointo data error, no withdrawCoinTo. hash:{}", tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        int assetId = withdrawCoinTo.getAssetsId();
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), withdrawCoinTo.getAssetsChainId(), assetId);
        BigInteger amount = withdrawCoinTo.getAmount();
        String heterogeneousAddress = txData.getHeterogeneousAddress();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousAssetInfo.getChainId());
        String ethTxHash = docking.createOrSignWithdrawTx(
                tx.getHash().toHex(),
                heterogeneousAddress,
                amount,
                heterogeneousAssetInfo.getAssetId());
        chain.getLogger().info("[withdraw] Calling heterogeneous chain components to execute withdrawals. hash:{},ethHash:{}", tx.getHash().toHex(), ethTxHash);
    }


    /**
     * Confirm withdrawal transactions Subsequent handling fee subsidy business
     * If a handling fee subsidy is required,Then trigger the transaction of handling fee subsidy
     * The subsidy trading time is unified as the time when the withdrawal exchange is confirmed in the block
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void confirmWithdrawalDistributionFee(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmWithdrawalTxData.class);
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : txData.getListDistributionFee()) {
            String address = chain.getDirectorRewardAddress(addr);
            if (StringUtils.isBlank(address)) {
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            assembleTxService.createDistributionFeeTx(chain, txData.getWithdrawalTxHash(), listRewardAddress, pendingPO.getBlockHeader().getTime(), false);
        } catch (NulsException e) {
            if ("tx_0013".equals(e.getErrorCode().getCode())) {
                chain.getLogger().info("The subsidy handling fee transaction has been confirmed..(original)Withdrawal confirmation transactiontxhash:{}", pendingPO.getTx().getHash().toHex());
            }
        }
    }

    /**
     * Handling fees for subsidy implementation proposals(for example Withdrawal, Returning the original route, etc)
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void confirmProposalDistributionFee(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return;
        }
        ConfirmProposalTxData txData = ConverterUtil.getInstance(pendingPO.getTx().getTxData(), ConfirmProposalTxData.class);
        List<HeterogeneousAddress> addressList;
        if (ProposalTypeEnum.UPGRADE.value() == txData.getType()) {
            ConfirmUpgradeTxData upgradeTxData = ConverterUtil.getInstance(txData.getBusinessData(), ConfirmUpgradeTxData.class);
            addressList = upgradeTxData.getListDistributionFee();
        } else {
            ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
            addressList = businessData.getListDistributionFee();
        }
        List<byte[]> listRewardAddress = new ArrayList<>();
        for (HeterogeneousAddress addr : addressList) {
            String address = chain.getDirectorRewardAddress(addr);
            if (StringUtils.isBlank(address)) {
                String signAddress = virtualBankAllHistoryStorageService.findByHeterogeneousAddress(chain, addr.getAddress());
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                address = director.getRewardAddress();
            }
            listRewardAddress.add(AddressTool.getAddress(address));
        }
        try {
            // basisTxHash To confirm the proposed transactionhash
            assembleTxService.createDistributionFeeTx(chain, pendingPO.getTx().getHash(), listRewardAddress, pendingPO.getBlockHeader().getTime(), true);
        } catch (NulsException e) {
            if ("tx_0013".equals(e.getErrorCode().getCode())) {
                chain.getLogger().info("The subsidy handling fee transaction has been confirmed..(original)Withdrawal confirmation transactiontxhash:{}", pendingPO.getTx().getHash().toHex());
            }
            chain.getLogger().error(e);
        }
    }


    /**
     * Reset Virtual Bank Heterogeneous Chain
     *
     * @param pendingPO
     * @throws NulsException
     */
    private void resetHeterogeneousVirtualBank(TxSubsequentProcessPO pendingPO) throws NulsException {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        /**
         * 1.Remove all virtual bank change transactions (All virtual bank members are required to execute)
         * 2.Initiate reset of heterogeneous chain transactions (Only the seed node executes)
         */
        List<TxSubsequentProcessPO> changeVirtualBankTxs = getChangeVirtualBankTxs(null);
        // 1.Remove processed elements from the queue
        chain.getPendingTxQueue().removeAll(changeVirtualBankTxs);
        for (TxSubsequentProcessPO po : changeVirtualBankTxs) {
            String hash = po.getTx().getHash().toHex();
            // Store successfully executed transactionshash, Executed items will no longer be executed (When the current node is in synchronous block mode,We also need to save thishash, Indicates that it has been executed)
            ComponentCalledPO callPO = new ComponentCalledPO(
                    hash,
                    po.getBlockHeader().getHeight(),
                    false);
            asyncProcessedTxStorageService.saveComponentCall(chain, callPO, po.getCurrentQuit());
            chain.getLogger().debug("[Heterogeneous chain pending queue] Reset Virtual Bank Heterogeneous Chain - Successfully executed to remove transaction, hash:{}", hash);
            // And remove it from the persistence library
            txSubsequentProcessStorageService.delete(chain, hash);
        }

        VirtualBankDirector currentDirector = virtualBankService.getCurrentDirector(chain.getChainId());
        if (null == currentDirector || !currentDirector.getSeedNode()) {
            chain.getLogger().info("[Reset heterogeneous chain transactions-The current node does not require execution] Current director is not seed node, heterogeneous reset operation cannot be performed");
            return;
        }

        // 2.
        Transaction tx = pendingPO.getTx();
        ResetVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ResetVirtualBankTxData.class);
        int virtualBankSize = chain.getMapVirtualBank().size();
        String[] allManagers = new String[virtualBankSize];
        List<String> seedManagerList = new ArrayList<>();
        int i = 0;
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(txData.getHeterogeneousChainId());
            allManagers[i] = heterogeneousAddress.getAddress();
            if (director.getSeedNode()) {
                seedManagerList.add(heterogeneousAddress.getAddress());
            }
            i++;
        }
        String[] seedManagers = seedManagerList.toArray(new String[seedManagerList.size()]);

        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        chain.getLogger().debug("[Reset Virtual Bank Heterogeneous Chain Call] Call parameters. hash :{}, seedManagers:{}, allManagers:{}",
                tx.getHash().toHex(), Arrays.toString(seedManagers), Arrays.toString(allManagers));
        String hash = null;
        try {
            hash = docking.forceRecovery(tx.getHash().toHex(), seedManagers, allManagers);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (!e.getErrorCode().getCode().equals("cv_0048")
                    && !e.getErrorCode().getCode().equals("cv_0040")
                    && !e.getErrorCode().getCode().equals("tx_0013")) {
                throw e;
            }
        }
        chain.getLogger().debug("[Reset Virtual Bank Heterogeneous Chain Call] Successfully called heterogeneous chain component. heterogeneousChainId :{}, heterogeneousHash:{}",
                txData.getHeterogeneousChainId(), hash);
    }

}
