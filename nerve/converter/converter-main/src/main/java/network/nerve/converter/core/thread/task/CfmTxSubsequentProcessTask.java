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
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
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
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.*;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.BlockCall;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService = SpringLiteContext.getBean(HeterogeneousChainInfoStorageService.class);
    private MergeComponentStorageService mergeComponentStorageService = SpringLiteContext.getBean(MergeComponentStorageService.class);
    private AssembleTxService assembleTxService = SpringLiteContext.getBean(AssembleTxService.class);
    private VirtualBankStorageService virtualBankStorageService = SpringLiteContext.getBean(VirtualBankStorageService.class);
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService = SpringLiteContext.getBean(VirtualBankAllHistoryStorageService.class);
    private VirtualBankService virtualBankService = SpringLiteContext.getBean(VirtualBankService.class);
    private HeterogeneousService heterogeneousService = SpringLiteContext.getBean(HeterogeneousService.class);
    private HeterogeneousAssetHelper heterogeneousAssetHelper = SpringLiteContext.getBean(HeterogeneousAssetHelper.class);
    private ComponentSignStorageService componentSignStorageService = SpringLiteContext.getBean(ComponentSignStorageService.class);
    private ConverterCoreApi converterCoreApi = SpringLiteContext.getBean(ConverterCoreApi.class);
    private CfmChangeBankStorageService cfmChangeBankStorageService = SpringLiteContext.getBean(CfmChangeBankStorageService.class);

    private String p35ChangeHash = "6ef994439924c868bfd98f15f154c8280e7b25905510d80ae763c562d615e991";
    private boolean checkedFillInP35ChangeTxOnStartup = false;
    private boolean checkedFillInP35v1ChangeTxOnStartup = false;

    @Override
    public void run() {

        try {
            fillInP35ChangeTx();
            fillInP35v1ChangeTx();
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            out:
            while (!pendingTxQueue.isEmpty()) {
                // Only remove,Do not remove head elements
                TxSubsequentProcessPO pendingPO = pendingTxQueue.peekFirst();
                Transaction tx = pendingPO.getTx();
                String nerveTxHash = tx.getHash().toHex();
                if (converterCoreApi.skippedTransaction(nerveTxHash)) {
                    // Determine if there is a problem with the transaction, Remove from queue, And remove it from the persistent library
                    chain.getLogger().info("[Heterogeneous chain pending queue] Historical legacy problem data, Remove transaction, hash:{}", nerveTxHash);
                    // And remove it from the persistence library
                    txSubsequentProcessStorageService.delete(chain, nerveTxHash);
                    componentSignStorageService.delete(chain, nerveTxHash);
                    // Successfully removed queue header elements
                    pendingTxQueue.remove();
                    continue;
                }

                if (this.isP35ChangeTx(nerveTxHash)) {
                    pendingPO = this.checkUpdateChangeTxForP35(pendingPO);
                    if (!converterCoreApi.isProtocol35()) {
                        continue;
                    }
                }

                if (!pendingPO.getRetry() && null != asyncProcessedTxStorageService.getComponentCall(chain, nerveTxHash)) {
                    // Judging that it has been executed, Remove from queue, And remove it from the persistent library
                    chain.getLogger().info("[Heterogeneous chain pending queue] Executed,Remove transaction, hash:{}", nerveTxHash);
                    // And remove it from the persistence library
                    txSubsequentProcessStorageService.delete(chain, nerveTxHash);
                    // Successfully removed queue header elements
                    pendingTxQueue.remove();
                    continue;
                }
                // Determine if it has been confirmed
                if (null == TransactionCall.getConfirmedTx(chain, tx.getHash())) {
                    if (pendingPO.getIsConfirmedVerifyCount() > ConverterConstant.CONFIRMED_VERIFY_COUNT) {
                        chain.getLogger().error("[Heterogeneous chain pending queue] Transaction unconfirmed(Remove processing), hash:{}", nerveTxHash);
                        // And remove it from the persistence library
                        txSubsequentProcessStorageService.delete(chain, nerveTxHash);
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
                            // check btcSys'chain handle
                            if (TxType.WITHDRAWAL == tx.getType()) {
                                WithdrawalTxData txData1 = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
                                int htgChainId = txData1.getHeterogeneousChainId();
                                if (htgChainId > 200) {
                                    int feeChangeVersion = chain.getWithdrawFeeChangeVersion(nerveTxHash);
                                    if (pendingPO.isWithdrawExceedErrorTime(feeChangeVersion, 10)) {
                                        chain.getLogger().warn("[withdraw] Insufficient withdrawal fees, retry count exceeded limit, temporarily suspending processing of current advance tasks, feeChangeVersion: {}, txHash: {}", feeChangeVersion, nerveTxHash);
                                        throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                                    }
                                    if (!withdrawalByzantineForBTC(pendingPO)) {
                                        TxSubsequentProcessPO po = chain.getPendingTxQueue().poll();
                                        chain.getPendingTxQueue().addLast(po);
                                        continue;
                                    }
                                    break;
                                }
                            }
                            int feeChangeVersion = chain.getWithdrawFeeChangeVersion(nerveTxHash);
                            if (pendingPO.isWithdrawExceedErrorTime(feeChangeVersion, 10)) {
                                chain.getLogger().warn("[withdraw] Insufficient withdrawal fees, retry count exceeded limit, temporarily suspending processing of current advance tasks, feeChangeVersion: {}, txHash: {}", feeChangeVersion, nerveTxHash);
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
                            nerveTxHash,
                            pendingPO.getBlockHeader().getHeight(),
                            false);
                    asyncProcessedTxStorageService.saveComponentCall(chain, callPO, pendingPO.getCurrentQuit());
                }

                chain.getLogger().info("[Heterogeneous chain pending queue] Successfully executed to remove transaction, hash:{}", nerveTxHash);
                // And remove it from the persistence library
                txSubsequentProcessStorageService.delete(chain, nerveTxHash);
                // Successfully removed queue header elements
                pendingTxQueue.remove();
            }
        } catch (Throwable e) {

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

        boolean waitingBitSysChain = false;
        // Check whether the multi-signature address needs to be changed on bitSys'chains
        for (IHeterogeneousChainDocking docking : hInterfaces) {
            Integer htgChainId = docking.getChainId();
            if (htgChainId > 200) {
                boolean enoughAvailablePubs = docking.getBitCoinApi().checkEnoughAvailablePubs(docking.getCurrentMultySignAddress());
                if (converterCoreApi.checkChangeP35(txHash)) {
                    enoughAvailablePubs = true;
                }
                if (!enoughAvailablePubs) {
                    // change multi-sign addr, transfer to new addr from current addr
                    // check if withdrawl tx has made UTXOs'tx
                    if (!checkManagerChangeUTXOTx(pendingPO, htgChainId, docking.getCurrentMultySignAddress())) {
                        waitingBitSysChain = true;
                    }
                }
            }
        }
        if (waitingBitSysChain) {
            throw new NulsException(ConverterErrorCode.CHANGE_WAITING);
        }

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
                if (converterCoreApi.checkChangeP35(txHash)) {
                    inAddress = converterCoreApi.inChangeP35();
                    outAddress = converterCoreApi.outChangeP35();
                }
                // call message signature
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
            List<Map<Integer, HeterogeneousSign>> list = compSignPO.getListMsg().stream().map(m -> m.getListSign().stream().collect(Collectors.toMap(hSign -> hSign.getHeterogeneousAddress().getChainId(), Function.identity()))).collect(Collectors.toList());
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
                            String signed = callParm.getSigned();
                            if (docking.getChainId() > 200) {
                                StringBuilder signatureDataBuilder = new StringBuilder();
                                for (Map<Integer, HeterogeneousSign> nodeSignMap : list) {
                                    signatureDataBuilder.append(HexUtil.encode(nodeSignMap.get(docking.getChainId()).getSignature())).append(",");
                                }
                                signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
                                signed = signatureDataBuilder.toString();
                            } else if (converterCoreApi.checkChangeP35(callParm.getTxHash())) {
                                // In order to align the managers, take the signatures of all nodes
                                StringBuilder signatureDataBuilder = new StringBuilder();
                                for (Map<Integer, HeterogeneousSign> nodeSignMap : list) {
                                    signatureDataBuilder.append(HexUtil.encode(nodeSignMap.get(docking.getChainId()).getSignature()));
                                }
                                signed = signatureDataBuilder.toString();
                            }
                            boolean vaildPass = docking.validateManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    signed);
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
                            String signed = callParm.getSigned();
                            if (docking.getChainId() > 200) {
                                StringBuilder signatureDataBuilder = new StringBuilder();
                                for (Map<Integer, HeterogeneousSign> nodeSignMap : list) {
                                    signatureDataBuilder.append(HexUtil.encode(nodeSignMap.get(docking.getChainId()).getSignature())).append(",");
                                }
                                signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
                                signed = signatureDataBuilder.toString();
                            }
                            String ethTxHash = docking.createOrSignManagerChangesTxII(
                                    callParm.getTxHash(),
                                    callParm.getInAddress(),
                                    callParm.getOutAddress(),
                                    callParm.getOrginTxCount(),
                                    signed);
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
            // If it is currently exiting, Then from pending PO Obtaining heterogeneous chain information from the dropout
            if (pendingPO.getCurrentQuit() && agentAddress.equals(pendingPO.getCurrentQuitDirector().getAgentAddress())) {
                if (heterogeneousChainId > 200) {
                    // fill in the pubkey on the bitSys'chain
                    hAddress = pendingPO.getCurrentQuitDirector().getSignAddrPubKey();
                } else {
                    HeterogeneousAddress heterogeneousAddress =
                            pendingPO.getCurrentQuitDirector().getHeterogeneousAddrMap().get(heterogeneousChainId);
                    hAddress = heterogeneousAddress.getAddress();
                }
            } else {
                String signAddress = virtualBankAllHistoryStorageService.findSignAddressByAgentAddress(chain, agentAddress);
                if (StringUtils.isNotBlank(signAddress)) {
                    VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, signAddress);
                    if (heterogeneousChainId > 200) {
                        // fill in the pubkey on the bitSys'chain
                        hAddress = director.getSignAddrPubKey();
                    } else {
                        HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
                        hAddress = heterogeneousAddress.getAddress();
                    }
                }
            }
            if (StringUtils.isBlank(hAddress)) {
                chain.getLogger().error("Heterogeneous chain address signature message[changeVirtualBank] No heterogeneous chain address obtained, agentAddress:{}", agentAddress);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
            }
            address[i] = hAddress;
        }
    }

    private boolean withdrawalByzantineForBTC(TxSubsequentProcessPO pendingPO) throws Exception {
        SyncStatusEnum syncStatus = chain.getLatestBasicBlock().getSyncStatusEnum();
        if (null == syncStatus || !syncStatus.equals(SyncStatusEnum.RUNNING)) {
            throw new NulsException(ConverterErrorCode.NODE_NOT_IN_RUNNING);
        }
        if (!chain.getCurrentIsDirector().get()) {
            return true;
        }
        Transaction tx = pendingPO.getTx();
        NulsHash nerveTxHashObj = tx.getHash();
        String nerveTxHash = nerveTxHashObj.toString();

        WithdrawalTxData txData1 = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        int htgChainId = txData1.getHeterogeneousChainId();
        String toAddress = txData1.getHeterogeneousAddress();

        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        // check if withdrawl tx has made UTXOs'tx
        WithdrawalUTXOTxData withdrawalUTXOTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
        // if not
        if (withdrawalUTXOTxData == null) {
            makeWithdrawalUTXOTx(pendingPO, htgChainId, docking.getCurrentMultySignAddress(), chain.getMapVirtualBank());
            return false;
        } else if (withdrawalUTXOTxData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD) {
            // add Fee to rebuild WithdrawalUTXOTxData, refer to WithdrawalAdditionalFeeProcessor#commitV35
            rebuildWithdrawalUTXOTx(pendingPO, htgChainId);
            return false;
        } else {
            // if AA,BB or CC UTXOs'tx confirmed, take signature from docking
            // all node can make withdrawl'tx from UTXOs'tx
            // Determine if you have received the message, And signed it
            ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, nerveTxHash);
            boolean needSign = false;
            if (null != compSignPO) {
                if (!compSignPO.getCurrentSigned()) {
                    needSign = true;
                }
            } else {
                needSign = true;
            }
            if (needSign) {
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
                    chain.getLogger().error("[Heterogeneous chain address signature message-withdraw] no withdrawCoinTo. hash:{}", nerveTxHash);
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                int heterogeneousChainId = assetInfo.getChainId();
                BigInteger amount = withdrawCoinTo.getAmount();
                // If the current node has not yet signed, trigger the current node's signature,storage And broadcast
                // BTC multi-signature address partition signature and collection
                String signStrData = docking.getBitCoinApi().signWithdraw(nerveTxHash, toAddress, amount, assetInfo.getAssetId());

                String currentHaddress = docking.getCurrentSignAddress();
                if (StringUtils.isBlank(currentHaddress)) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
                }
                HeterogeneousAddress heterogeneousAddress = new HeterogeneousAddress(heterogeneousChainId, currentHaddress);
                HeterogeneousSign currentSign = new HeterogeneousSign(heterogeneousAddress, HexUtil.decode(signStrData));
                List<HeterogeneousSign> listSign = new ArrayList<>();
                listSign.add(currentSign);
                ComponentSignMessage currentMessage = new ComponentSignMessage(pendingPO.getCurrenVirtualBankTotal(),
                        nerveTxHashObj, listSign);
                // Initialize the object for storing signatures
                if (null == compSignPO) {
                    compSignPO = new ComponentSignByzantinePO(nerveTxHashObj, new ArrayList<>(), false, false);
                } else if (null == compSignPO.getListMsg()) {
                    compSignPO.setListMsg(new ArrayList<>());
                }
                compSignPO.getListMsg().add(currentMessage);
                compSignPO.setCurrentSigned(true);
                // Broadcast current node signature message
                NetWorkCall.broadcast(chain, currentMessage, ConverterCmdConstant.COMPONENT_SIGN);
                chain.getLogger().info("[withdraw] Calling heterogeneous chain components to execute signatures, Send signed message. hash:{}", nerveTxHash);
            }

            boolean rs = false;
            if (compSignPO.getByzantinePass()) {
                // collect more signatures
                StringBuilder signatureDataBuilder = new StringBuilder();
                for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                    signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature())).append(",");
                }
                signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
                chain.getLogger().info("[withdraw] Collected {} signatures for {}", compSignPO.getListMsg().size(), nerveTxHash);

                if (pendingPO.getRetry() || !compSignPO.getCompleted()) {
                    // Execute calls to heterogeneous chains
                    List<ComponentCallParm> callParmsList = compSignPO.getCallParms();
                    if (null == callParmsList) {
                        chain.getLogger().info("[withdraw] Call heterogeneous chain parameter is empty");
                        return false;
                    }
                    ComponentCallParm callParm = callParmsList.get(0);
                    if (!converterCoreApi.checkNetworkRunning(docking.getChainId())) {
                        chain.getLogger().info("[withdraw] Test network [{}] Run Pause, chainId: {}", docking.getChainSymbol(), docking.getChainId());
                        throw new NulsException(ConverterErrorCode.WITHDRAWAL_PAUSE);
                    }

                    // check fee
                    WithdrawalTotalFeeInfo totalFeeInfo = assembleTxService.calculateWithdrawalTotalFee(chain, tx);
                    // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
                    BigDecimal feeAmount = new BigDecimal(converterCoreApi.checkDecimalsSubtractedToNerveForWithdrawal(totalFeeInfo.getHtgMainAssetName().chainId(), 1, totalFeeInfo.getFee()));
                    // Verify heterogeneous chain master assets as transaction fees
                    // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal to ETH, Payment BNB As a handling fee
                    boolean enoughFeeOfWithdraw = docking.getBitCoinApi().isEnoughFeeOfWithdraw(nerveTxHash, totalFeeInfo.getHtgMainAssetName(), feeAmount);
                    if (!enoughFeeOfWithdraw) {
                        // Abnormal Count
                        pendingPO.increaseWithdrawErrorTime();
                        chain.getLogger().error("[withdraw] Withdrawal fee calculation, The handling fee is insufficient to cover the withdrawal fee. hash: {}, amount: {}",
                                nerveTxHash, callParm.getValue());
                        throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                    }

                    String htgTxHash;
                    if (pendingPO.getRetry() && docking instanceof HtgDocking) {
                        // When the mechanism for resending withdrawals is executed, the withdrawal is triggered without checking the execution order, and the current node immediately executes it
                        HtgDocking htgDocking = (HtgDocking) docking;
                        htgTxHash = htgDocking.getBitCoinApi().createOrSignWithdrawTx(
                                callParm.getTxHash(),
                                callParm.getToAddress(),
                                callParm.getValue(),
                                callParm.getAssetId(),
                                signatureDataBuilder.toString(), false);
                    } else {
                        htgTxHash = docking.getBitCoinApi().createOrSignWithdrawTx(
                                callParm.getTxHash(),
                                callParm.getToAddress(),
                                callParm.getValue(),
                                callParm.getAssetId(),
                                signatureDataBuilder.toString(), true);
                    }

                    compSignPO.setCompleted(true);
                    // The withdrawal transaction was successfully sent out, and the status of clearing additional fees has been cleared
                    chain.clearWithdrawFeeChange(nerveTxHash);
                    chain.getLogger().info("[Heterogeneous chain address signature message-Byzantine passage-withdraw] Calling heterogeneous chain components to execute withdrawals. nerveTxHash:{}, htgTxHash:{}", nerveTxHash, htgTxHash);
                }
                rs = true;
            }
            // Store updated compSignPO
            componentSignStorageService.save(chain, compSignPO);
            return rs;
        }
    }

    private boolean seedPackerOrderCall(TxSubsequentProcessPO pendingPO, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank, SeedPackerOrder seedPackerOrder) throws Exception {
        Transaction tx = pendingPO.getTx();
        // check if AA,BB or CC
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (signAccountDTO == null) {
            chain.getLogger().error("empty getPackerInfo");
            return false;
        }
        Integer order = converterCoreApi.getSeedPackerOrder(signAccountDTO.getAddress());
        if (order == null) {
            return false;
        }
        // if true
        if (order == 0) {
            // AA call
            seedPackerOrder.call(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
            return false;
        } else {
            // BB + 5min to call
            // CC + 10min to call
            if (pendingPO.getTimeForMakeUTXO() == 0) {
                pendingPO.setTimeForMakeUTXO(System.currentTimeMillis() + (long) order * 5 * 60 * 1000);
                return false;
            } else if (pendingPO.getTimeForMakeUTXO() <= System.currentTimeMillis()) {
                // BB or CC call
                seedPackerOrder.call(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
                return false;
            }
        }
        return false;
    }

    private boolean checkManagerChangeUTXOTx(TxSubsequentProcessPO pendingPO, int htgChainId, String currentMultiSignAddress) throws Exception {
        Transaction tx = pendingPO.getTx();
        NulsHash nerveTxHashObj = tx.getHash();
        String nerveTxHash = nerveTxHashObj.toString();

        WithdrawalUTXOTxData withdrawalUTXOTxData = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId).getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
        if (withdrawalUTXOTxData != null) {
            if (withdrawalUTXOTxData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD) {
                return this.rebuildManagerChangeUTXOTx(pendingPO, htgChainId, currentMultiSignAddress);
            }
            return true;
        }
        return this.makeManagerChangeUTXOTx(pendingPO, htgChainId, currentMultiSignAddress);
    }


    private boolean makeManagerChangeUTXOTx(TxSubsequentProcessPO pendingPO, int htgChainId, String currentMultiSignAddress) throws Exception {
        return this.seedPackerOrderCall(pendingPO, htgChainId, currentMultiSignAddress, chain.getMapVirtualBank(), new SeedPackerOrder() {
            @Override
            public void call(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
                WithdrawalUTXOTxData txData = heterogeneousAssetHelper.makeManagerChangeUTXOTxData(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
                assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
            }
        });
    }

    private boolean rebuildManagerChangeUTXOTx(TxSubsequentProcessPO pendingPO, int htgChainId, String currentMultiSignAddress) throws Exception {
        return this.seedPackerOrderCall(pendingPO, htgChainId, currentMultiSignAddress, chain.getMapVirtualBank(), new SeedPackerOrder() {
            @Override
            public void call(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
                WithdrawalUTXOTxData txData = heterogeneousAssetHelper.rebuildManagerChangeUTXOTxData(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
                assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
            }
        });
    }

    private boolean makeWithdrawalUTXOTx(TxSubsequentProcessPO pendingPO, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
        // When checking that an administrator has changed the transaction,
        // if the UTXO transaction has not been locked, the withdrawal transaction will be suspended.
        // If it has been locked, the execution will continue.
        Transaction tx = pendingPO.getTx();
        NulsHash nerveTxHashObj = tx.getHash();
        String nerveTxHash = nerveTxHashObj.toString();
        if (chain.getHeterogeneousChangeBankExecuting().get()) {
            chain.getLogger().info("withdrawal tx: {}, waitting chang transaction", nerveTxHash);
            return false;
        }

        return this.seedPackerOrderCall(pendingPO, htgChainId, currentMultiSignAddress, chain.getMapVirtualBank(), new SeedPackerOrder() {
            @Override
            public void call(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
                WithdrawalUTXOTxData txData = heterogeneousAssetHelper.makeWithdrawalUTXOsTxData(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
                assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
            }
        });

        //// check if AA,BB or CC
        //SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        //if (signAccountDTO == null) {
        //    chain.getLogger().error("empty getPackerInfo");
        //    return false;
        //}
        //Integer order = converterCoreApi.getSeedPackerOrder(signAccountDTO.getAddress());
        //if (order == null) {
        //    return false;
        //}
        //// if true
        //if (order == 0) {
        //    // AA get UTXOs and make UTXOs'tx
        //    WithdrawalUTXOTxData txData = heterogeneousAssetHelper.makeWithdrawalUTXOsTxData(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
        //    assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
        //    return false;
        //} else {
        //    // BB + 5min to get UTXOs and make UTXOs'tx
        //    // CC + 10min to get UTXOs and make UTXOs'tx
        //    if (pendingPO.getTimeForMakeUTXO() == 0) {
        //        pendingPO.setTimeForMakeUTXO(System.currentTimeMillis() + (long) order * 5 * 60 * 1000);
        //        return false;
        //    } else if (pendingPO.getTimeForMakeUTXO() <= System.currentTimeMillis()) {
        //        // BB or CC to get UTXOs and make UTXOs'tx
        //        WithdrawalUTXOTxData txData = heterogeneousAssetHelper.makeWithdrawalUTXOsTxData(chain, tx, htgChainId, currentMultiSignAddress, mapVirtualBank);
        //        assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
        //        return false;
        //    }
        //}
        //return false;
    }

    private boolean rebuildWithdrawalUTXOTx(TxSubsequentProcessPO pendingPO, int htgChainId) throws Exception {
        // When checking that an administrator has changed the transaction,
        // if the UTXO transaction has not been locked, the withdrawal transaction will be suspended.
        // If it has been locked, the execution will continue.
        Transaction tx = pendingPO.getTx();
        NulsHash nerveTxHashObj = tx.getHash();
        String nerveTxHash = nerveTxHashObj.toString();
        if (chain.getHeterogeneousChangeBankExecuting().get()) {
            chain.getLogger().info("withdrawal tx: {}, waitting chang transaction", nerveTxHash);
            return false;
        }

        return this.seedPackerOrderCall(pendingPO, htgChainId, null, null, new SeedPackerOrder() {
            @Override
            public void call(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
                WithdrawalUTXOTxData txData = heterogeneousAssetHelper.rebuildWithdrawalUTXOsTxData(chain, tx, htgChainId);
                assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
            }
        });

        //// check if AA,BB or CC
        //SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        //if (signAccountDTO == null) {
        //    chain.getLogger().error("empty getPackerInfo");
        //    return false;
        //}
        //Integer order = converterCoreApi.getSeedPackerOrder(signAccountDTO.getAddress());
        //if (order == null) {
        //    return false;
        //}
        //// if true
        //if (order == 0) {
        //    // AA get UTXOs and rebuild UTXOs'tx
        //    WithdrawalUTXOTxData txData = heterogeneousAssetHelper.rebuildWithdrawalUTXOsTxData(chain, tx, htgChainId);
        //    assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
        //    return false;
        //} else {
        //    // BB + 5min to get UTXOs and rebuild UTXOs'tx
        //    // CC + 10min to get UTXOs and rebuild UTXOs'tx
        //    if (pendingPO.getTimeForMakeUTXO() == 0) {
        //        pendingPO.setTimeForMakeUTXO(System.currentTimeMillis() + (long) order * 5 * 60 * 1000);
        //        return false;
        //    } else if (pendingPO.getTimeForMakeUTXO() <= System.currentTimeMillis()) {
        //        // BB or CC to get UTXOs and rebuild UTXOs'tx
        //        WithdrawalUTXOTxData txData = heterogeneousAssetHelper.rebuildWithdrawalUTXOsTxData(chain, tx, htgChainId);
        //        assembleTxService.createWithdrawUTXOTx(chain, txData, tx.getTime());
        //        return false;
        //    }
        //}
        //return false;
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
            // collect more signatures
            StringBuilder signatureDataBuilder = new StringBuilder();
            for (ComponentSignMessage msg : compSignPO.getListMsg()) {
                signatureDataBuilder.append(HexUtil.encode(msg.getListSign().get(0).getSignature())).append(",");
            }
            signatureDataBuilder.deleteCharAt(signatureDataBuilder.length() - 1);
            chain.getLogger().info("[withdraw] Collected {} signatures for {}, signatures: {}", compSignPO.getListMsg().size(), txHash, signatureDataBuilder.toString());

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
                        enoughFeeOfWithdraw = docking.isEnoughNvtFeeOfWithdraw(new BigDecimal(totalFeeInfo.getFee()), callParm.getAssetId());
                    } else {
                        BigDecimal feeAmount = new BigDecimal(converterCoreApi.checkDecimalsSubtractedToNerveForWithdrawal(totalFeeInfo.getHtgMainAssetName().chainId(), 1, totalFeeInfo.getFee()));
                        // Verify heterogeneous chain master assets as transaction fees
                        // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
                        enoughFeeOfWithdraw = docking.isEnoughFeeOfWithdrawByMainAssetProtocol15(totalFeeInfo.getHtgMainAssetName(), feeAmount, callParm.getAssetId());
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

    TxSubsequentProcessPO makeProcessPOForP35Change() throws Exception {
        try {
            TxSubsequentProcessPO p35Change = new TxSubsequentProcessPO();
            Transaction tx = TransactionCall.getConfirmedTx(chain, "6ef994439924c868bfd98f15f154c8280e7b25905510d80ae763c562d615e991");
            BlockHeader blockHeader = BlockCall.getBlockHeader(chain, 56006500);
            List<VirtualBankDirector> listInDirector = new ArrayList<>();
            VirtualBankDirector in = new VirtualBankDirector();
            in.setAgentHash("ab1c0c6d8b94c923d7ead1413126731c5363c3e52a086d69e0d07d83a52d36a6");
            in.setSignAddress("NERVEepb66oGcmJnrjX5AGzFjXJrLiErHMo1cn");
            in.setSignAddrPubKey("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
            in.setAgentAddress("NERVEepb6BLLGVzrcbt9Ga9YtL7SFwmDkTD4LV");
            in.setRewardAddress("NERVEepb6CZEJ8RFBLvgE1KpLEBABgTvu5YyJn");
            in.setSeedNode(false);
            in.setOrder(15);
            Map<String, HeterogeneousAddress> map = JSONUtils.json2map("{\"101\":{\"chainId\":101,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"102\":{\"chainId\":102,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"103\":{\"chainId\":103,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"104\":{\"chainId\":104,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"105\":{\"chainId\":105,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"106\":{\"chainId\":106,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"107\":{\"chainId\":107,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"108\":{\"chainId\":108,\"address\":\"TRFoJDw5r385RoYTFJNiqHnKaNANRiMXFL\"},\"109\":{\"chainId\":109,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"110\":{\"chainId\":110,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"111\":{\"chainId\":111,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"112\":{\"chainId\":112,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"113\":{\"chainId\":113,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"114\":{\"chainId\":114,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"115\":{\"chainId\":115,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"116\":{\"chainId\":116,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"117\":{\"chainId\":117,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"119\":{\"chainId\":119,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"120\":{\"chainId\":120,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"121\":{\"chainId\":121,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"122\":{\"chainId\":122,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"123\":{\"chainId\":123,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"124\":{\"chainId\":124,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"125\":{\"chainId\":125,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"126\":{\"chainId\":126,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"127\":{\"chainId\":127,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"128\":{\"chainId\":128,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"129\":{\"chainId\":129,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"130\":{\"chainId\":130,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"131\":{\"chainId\":131,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"133\":{\"chainId\":133,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"134\":{\"chainId\":134,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"135\":{\"chainId\":135,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"138\":{\"chainId\":138,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"139\":{\"chainId\":139,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"140\":{\"chainId\":140,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"141\":{\"chainId\":141,\"address\":\"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951\"},\"201\":{\"chainId\":201,\"address\":\"19n5qwmBPrYr5rvjeMSGi2DwrsfBgEw2cf\"},\"202\":{\"chainId\":202,\"address\":\"FEcCJkCGFAmWi2omW36RgQkUtXgCW5SFhy\"}}", HeterogeneousAddress.class);
            Map<Integer, HeterogeneousAddress> addressMap = map.entrySet().stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
            in.setHeterogeneousAddrMap(addressMap);
            listInDirector.add(in);
            List<VirtualBankDirector> listOutDirector = new ArrayList<>();
            VirtualBankDirector out = new VirtualBankDirector();
            out.setAgentHash("0eec2c4716c48059bf81b57f969b65d9b2ebaf457c7cef05d75a8470f20e9472");
            out.setSignAddress("NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64");
            out.setSignAddrPubKey("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
            out.setAgentAddress("NERVEepb6Doc1HeS13ntknsSDsJusDktdTBagN");
            out.setRewardAddress("NERVEepb6Doc1HeS13ntknsSDsJusDktdTBagN");
            out.setSeedNode(false);
            out.setOrder(12);
            Map<String, HeterogeneousAddress> outMap = JSONUtils.json2map("{\"101\":{\"chainId\":101,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"102\":{\"chainId\":102,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"103\":{\"chainId\":103,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"104\":{\"chainId\":104,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"105\":{\"chainId\":105,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"106\":{\"chainId\":106,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"107\":{\"chainId\":107,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"108\":{\"chainId\":108,\"address\":\"TMJRCZuVWU6xcxRpG5zrBJb4kKGBMhy6dg\"},\"109\":{\"chainId\":109,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"110\":{\"chainId\":110,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"111\":{\"chainId\":111,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"112\":{\"chainId\":112,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"113\":{\"chainId\":113,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"114\":{\"chainId\":114,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"115\":{\"chainId\":115,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"116\":{\"chainId\":116,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"117\":{\"chainId\":117,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"119\":{\"chainId\":119,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"120\":{\"chainId\":120,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"121\":{\"chainId\":121,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"122\":{\"chainId\":122,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"123\":{\"chainId\":123,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"124\":{\"chainId\":124,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"125\":{\"chainId\":125,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"126\":{\"chainId\":126,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"127\":{\"chainId\":127,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"128\":{\"chainId\":128,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"129\":{\"chainId\":129,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"130\":{\"chainId\":130,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"131\":{\"chainId\":131,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"133\":{\"chainId\":133,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"134\":{\"chainId\":134,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"135\":{\"chainId\":135,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"138\":{\"chainId\":138,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"139\":{\"chainId\":139,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"140\":{\"chainId\":140,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"141\":{\"chainId\":141,\"address\":\"0x7c4b783a0101359590e6153df3b58c7fe24ea468\"},\"201\":{\"chainId\":201,\"address\":\"18zsADmbY6149Sd5yKQ4XYLJCwhbWGDRDf\"},\"202\":{\"chainId\":202,\"address\":\"FDpyd2CgPQDimcW7q14DVvrqEbicPXJCao\"}}", HeterogeneousAddress.class);
            Map<Integer, HeterogeneousAddress> outAddressMap = outMap.entrySet().stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
            out.setHeterogeneousAddrMap(outAddressMap);
            listOutDirector.add(out);

            p35Change.setTx(tx);
            p35Change.setListInDirector(listInDirector);
            p35Change.setListOutDirector(listOutDirector);
            p35Change.setBlockHeader(blockHeader);
            p35Change.setSyncStatusEnum(SyncStatusEnum.RUNNING);
            p35Change.setCurrentJoin(false);
            p35Change.setCurrentQuit(false);
            p35Change.setCurrentQuitDirector(null);
            p35Change.setCurrentDirector(true);
            p35Change.setCurrenVirtualBankTotal(15);
            return p35Change;
        } catch (Exception e) {
            chain.getLogger().warn("TxSubsequentProcessPO p35Change make error", e);
            return null;
        }
    }

    boolean isP35ChangeTx(String nerveTxHash) {
        if (chain.getChainId() != 9) {
            return false;
        }
        return this.p35ChangeHash.equals(nerveTxHash);
    }

    TxSubsequentProcessPO checkUpdateChangeTxForP35(TxSubsequentProcessPO pendingPO) {
        try {
            int size = pendingPO.getListInDirector().get(0).getHeterogeneousAddrMap().size();
            if (size < 39) {
                Transaction tx = pendingPO.getTx();
                String nerveTxHash = tx.getHash().toHex();
                // upate data due to some new cross-chain
                componentSignStorageService.delete(chain, nerveTxHash);
                asyncProcessedTxStorageService.removeComponentCall(chain, nerveTxHash);
                txSubsequentProcessStorageService.delete(chain, nerveTxHash);
                pendingPO = this.makeProcessPOForP35Change();
                txSubsequentProcessStorageService.save(chain, pendingPO);
                txSubsequentProcessStorageService.saveBackup(chain, pendingPO);
                return pendingPO;
            }
            return pendingPO;
        } catch (Exception e) {
            chain.getLogger().warn("checkUpdateChangeTxForP35 error", e);
            return null;
        }
    }

    void fillInP35ChangeTx() {
        try {
            if (chain.getChainId() != 9) {
                return;
            }
            if (!converterCoreApi.isProtocol35()) {
                return;
            }
            if (checkedFillInP35ChangeTxOnStartup) {
                return;
            }

            ConfirmedChangeVirtualBankPO confirmedPo = cfmChangeBankStorageService.find(chain, p35ChangeHash);
            if (confirmedPo != null) {
                checkedFillInP35ChangeTxOnStartup = true;
                return;
            }

            boolean exist = false;
            LinkedBlockingDeque<TxSubsequentProcessPO> pendingTxQueue = chain.getPendingTxQueue();
            for (TxSubsequentProcessPO po : pendingTxQueue) {
                Transaction tx = po.getTx();
                String nerveTxHash = tx.getHash().toHex();
                if (this.isP35ChangeTx(nerveTxHash)) {
                    exist = true;
                    TxSubsequentProcessPO pendingPO = this.checkUpdateChangeTxForP35(po);
                    po.setListInDirector(pendingPO.getListInDirector());
                    po.setListOutDirector(pendingPO.getListOutDirector());
                }
            }
            if (!exist) {
                TxSubsequentProcessPO _po = txSubsequentProcessStorageService.get(chain, this.p35ChangeHash);
                if (_po == null) {
                    _po = this.makeProcessPOForP35Change();
                    txSubsequentProcessStorageService.save(chain, _po);
                    txSubsequentProcessStorageService.saveBackup(chain, _po);
                } else {
                    _po = this.checkUpdateChangeTxForP35(_po);
                }
                chain.getPendingTxQueue().offer(_po);
            }
            checkedFillInP35ChangeTxOnStartup = true;
        } catch (Exception e) {
            chain.getLogger().warn("fillInP35ChangeTx error", e);
        }

    }

    void fillInP35v1ChangeTx() {
        try {
            if (chain.getChainId() != 9) {
                return;
            }
            if (!converterCoreApi.isProtocol35()) {
                return;
            }
            if (checkedFillInP35v1ChangeTxOnStartup) {
                return;
            }
            List<String> addrList = new ArrayList<>();
            addrList.add("NERVEepb66oGcmJnrjX5AGzFjXJrLiErHMo1cn");
            addrList.add("NERVEepb69pdDv3gZEZtJEmahzsHiQE6CK4xRi");
            addrList.add("NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64");
            addrList.add("NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB");

            for (String addr : addrList) {
                VirtualBankDirector director = virtualBankAllHistoryStorageService.findBySignAddress(chain, addr);
                if (director != null && director.getHeterogeneousAddrMap() != null) {
                    Map<Integer, HeterogeneousAddress> map = director.getHeterogeneousAddrMap();
                    HeterogeneousAddress heterogeneousAddress = map.get(101);
                    String address = heterogeneousAddress.getAddress();
                    map.put(134, new HeterogeneousAddress(134, address));
                    map.put(138, new HeterogeneousAddress(138, address));
                    map.put(139, new HeterogeneousAddress(139, address));
                    map.put(140, new HeterogeneousAddress(140, address));
                    map.put(141, new HeterogeneousAddress(141, address));
                    virtualBankAllHistoryStorageService.save(chain, director);
                }
            }
            checkedFillInP35v1ChangeTxOnStartup = true;
        } catch (Exception e) {
            chain.getLogger().warn("fillInP35v1ChangeTx error", e);
        }
    }
}
