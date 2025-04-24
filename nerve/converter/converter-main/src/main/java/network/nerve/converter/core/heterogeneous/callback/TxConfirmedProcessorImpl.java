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
package network.nerve.converter.core.heterogeneous.callback;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import network.nerve.converter.core.heterogeneous.callback.management.CallBackBeanManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.po.ConfirmedChangeVirtualBankPO;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.CfmChangeBankStorageService;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.storage.MergeComponentStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public class TxConfirmedProcessorImpl implements ITxConfirmedProcessor {

    private Chain nerveChain;
    /**
     * Heterogeneous chainchainId
     */
    private int hChainId;
    private AssembleTxService assembleTxService;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    private ProposalStorageService proposalStorageService;
    private MergeComponentStorageService mergeComponentStorageService;
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    private ConverterCoreApi converterCoreApi;
    // Repeated collection timer
    private Map<String, AtomicInteger> countHash = new HashMap<>();

    public TxConfirmedProcessorImpl(Chain nerveChain, int hChainId, CallBackBeanManager callBackBeanManager) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.assembleTxService = callBackBeanManager.getAssembleTxService();
        this.heterogeneousDockingManager = callBackBeanManager.getHeterogeneousDockingManager();
        this.heterogeneousConfirmedChangeVBStorageService = callBackBeanManager.getHeterogeneousConfirmedChangeVBStorageService();
        this.proposalStorageService = callBackBeanManager.getProposalStorageService();
        this.mergeComponentStorageService = callBackBeanManager.getMergeComponentStorageService();
        this.cfmChangeBankStorageService = callBackBeanManager.getCfmChangeBankStorageService();
        this.converterCoreApi = callBackBeanManager.getConverterCoreApi();
    }

    private NulsLogger logger() {
        return nerveChain.getLogger();
    }

    /**
     * Administrator Change: Collect all administrator change confirmation transactions for heterogeneous chain components, then assemble and send transactions
     * Withdrawal: Directly assemble and send transactions
     *
     * @param txType           Transaction type - WITHDRAW/CHANGE Withdrawal/Administrator Change
     * @param nerveTxHash      This chain transactionhash
     * @param txHash           Heterogeneous Chain Tradinghash
     * @param blockHeight      Heterogeneous chain transaction confirmation height
     * @param txTime           Heterogeneous chain transaction time
     * @param multiSignAddress Current multiple signed addresses
     * @param signers          Transaction signature address list
     */
    @Override
    public void txConfirmed(HeterogeneousChainTxType txType, String nerveTxHash, String txHash, Long blockHeight, Long txTime, String multiSignAddress, List<HeterogeneousAddress> signers, byte[] remark) throws Exception {
        NulsHash nerveHash = NulsHash.fromHex(nerveTxHash);
        // Check if the confirmation transaction was initiated by the proposal
        NulsHash nerveProposalHash = proposalStorageService.getExeBusiness(nerveChain, nerveTxHash);
        ProposalPO proposalPO = null;
        ProposalTypeEnum proposalType = null;
        if (nerveProposalHash != null) {
            proposalPO = proposalStorageService.find(nerveChain, nerveProposalHash);
            proposalType = proposalPO != null ? ProposalTypeEnum.getEnum(proposalPO.getType()) : null;
        }
        boolean isProposal = proposalPO != null;
        switch (txType) {
            case WITHDRAW:
                if (isProposal) {
                    logger().info("Proposal execution confirmation[{}], proposalHash: {}, ETH txHash: {}", proposalType, nerveProposalHash.toHex(), txHash);
                    this.createCommonConfirmProposalTx(proposalType, nerveProposalHash, null, txHash, proposalPO.getAddress(), signers, txTime);
                    break;
                }
                // Confirmed transactions for normal withdrawals
                ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
                txData.setHeterogeneousChainId(hChainId);
                txData.setHeterogeneousHeight(blockHeight);
                txData.setHeterogeneousTxHash(txHash);
                txData.setWithdrawalTxHash(nerveHash);
                txData.setListDistributionFee(signers);
                assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, txTime, remark);
                break;
            case CHANGE:
                // Query and merge databases, check if change transactions are merged
                List<String> nerveTxHashList = new ArrayList<>();
                MergedComponentCallPO mergedTx = mergeComponentStorageService.findMergedTx(nerveChain, nerveTxHash);
                if (mergedTx == null) {
                    nerveTxHashList.add(nerveTxHash);
                } else {
                    nerveTxHashList.addAll(mergedTx.getListTxHash());
                }
                int mergeCount = nerveTxHashList.size();
                Long realTxTime = txTime;
                for (String realNerveTxHash : nerveTxHashList) {
                    // When empty, it means there is noaddandremoveIf the transaction is directly confirmed, the time of confirming the transaction will be the time of the original change transaction
                    if (txTime == null) {
                        Transaction realNerveTx = TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(realNerveTxHash));
                        realTxTime = realNerveTx.getTime();
                    }
                    HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank(realNerveTxHash, hChainId, multiSignAddress, txHash, realTxTime, signers);
                    this.checkProposal(realNerveTxHash, txHash, realTxTime, signers);
                    this.confirmChangeTx(realNerveTxHash, bank, mergeCount, remark);
                }
                break;
            case RECOVERY:
                ConfirmResetVirtualBankTxData reset = new ConfirmResetVirtualBankTxData();
                reset.setHeterogeneousChainId(hChainId);
                reset.setResetTxHash(nerveHash);
                reset.setHeterogeneousTxHash(txHash);
                if (txTime == null) {
                    Transaction realNerveTx = TransactionCall.getConfirmedTx(nerveChain, nerveHash);
                    txTime = realNerveTx.getTime();
                }
                assembleTxService.createConfirmResetVirtualBankTx(nerveChain, reset, txTime);
                break;
            case UPGRADE:
                if (isProposal) {
                    logger().info("Proposal execution confirmation[Upgrade by signing multiple contracts], proposalHash: {}, ETH txHash: {}", nerveProposalHash.toHex(), txHash);
                    this.createUpgradeConfirmProposalTx(nerveProposalHash, txHash, proposalPO.getAddress(), multiSignAddress, signers, txTime);
                }
                break;
        }
    }

    @Override
    public void txRecordWithdrawFee(HeterogeneousChainTxType txType, String txHash, String blockHash, Long blockHeight, Long txTime, long fee, byte[] remark) throws Exception {
        this.txRecordWithdrawFee(txType, txHash, blockHash, blockHeight, txTime, fee, false, remark);
    }

    @Override
    public void txRecordWithdrawFee(HeterogeneousChainTxType txType, String txHash, String blockHash, Long blockHeight, Long txTime, long fee, boolean nerveInner, byte[] remark) throws Exception {
        if (hChainId < 200) {
            throw new RuntimeException("not support method");
        }
        WithdrawalFeeLog txData = new WithdrawalFeeLog();
        txData.setBlockHeight(blockHeight);
        txData.setBlockHash(blockHash);
        txData.setHtgTxHash(txHash);
        txData.setHtgChainId(hChainId);
        txData.setFee(fee);
        txData.setRecharge(txType == HeterogeneousChainTxType.WITHDRAW_FEE_RECHARGE);
        if (converterCoreApi.isProtocol36()) {
            txData.setNerveInner(nerveInner);
        }
        assembleTxService.createWithdrawlFeeLogTx(nerveChain, txData, txTime, remark);
    }

    @Override
    public void pendingTxOfWithdraw(String nerveTxHash, String heterogeneousTxHash) throws Exception {
        if (!converterCoreApi.isSupportNewMechanismOfWithdrawalFee()) {
            return;
        }
        // Withdrawal pending confirmation of transaction assembly and shipment
        WithdrawalHeterogeneousSendTxData txData = new WithdrawalHeterogeneousSendTxData();
        txData.setNerveTxHash(nerveTxHash);
        txData.setHeterogeneousTxHash(heterogeneousTxHash);
        txData.setHeterogeneousChainId(hChainId);
        Transaction nerveTx = converterCoreApi.getNerveTx(nerveTxHash);
        assembleTxService.withdrawalHeterogeneousSendTx(nerveChain, txData, nerveTx.getTime());
    }

    private void checkProposal(String nerveTxHash, String txHash, Long txTime, List<HeterogeneousAddress> signers) throws NulsException {
        NulsHash nerveHash = NulsHash.fromHex(nerveTxHash);
        // Check if the confirmation transaction was initiated by the proposal
        NulsHash nerveProposalHash = proposalStorageService.getExeBusiness(nerveChain, nerveTxHash);
        ProposalPO proposalPO = null;
        if (nerveProposalHash != null) {
            proposalPO = proposalStorageService.find(nerveChain, nerveProposalHash);
        }
        if (proposalPO != null) {
            logger().info("Proposal execution confirmation[Revocation of bank qualification], proposalHash: {}, ETH txHash: {}", nerveProposalHash.toHex(), txHash);
            this.createCommonConfirmProposalTx(ProposalTypeEnum.EXPELLED, nerveProposalHash, nerveHash, txHash, proposalPO.getAddress(), signers, txTime);
        }
    }

    private int confirmChangeTx(String nerveTxHash, HeterogeneousConfirmedVirtualBank bank, int mergeCount, byte[] remark) throws Exception {
        AtomicInteger count = countHash.get(nerveTxHash);
        if (count == null) {
            count = new AtomicInteger(0);
            countHash.put(nerveTxHash, count);
        }
        logger().info("Collect heterogeneous chain change confirmation, NerveTxHash: {}, hChainId: {}, time: {}", nerveTxHash, bank.getHeterogeneousChainId(), bank.getEffectiveTime());
        HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
        if (vbPo == null) {
            vbPo = new HeterogeneousConfirmedChangeVBPo();
            vbPo.setNerveTxHash(nerveTxHash);
            vbPo.setHgCollection(new HashSet<>());
        }
        Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
        List<HeterogeneousConfirmedVirtualBank> vbList = new ArrayList<>(vbSet);
        VirtualBankUtil.sortListByChainId(vbList);
        for (HeterogeneousConfirmedVirtualBank vb : vbList) {
            logger().info("Signed H chainId: {}, time: {}", vb.getHeterogeneousChainId(), vb.getEffectiveTime());
        }
        int hChainSize = heterogeneousDockingManager.getAllHeterogeneousDocking().size();
        // Check for duplicate additions
        if (!vbSet.add(bank)) {
            // Check if it has been confirmed
            ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(nerveChain, nerveTxHash);
            if (po != null) {
                Transaction confirmedTx = TransactionCall.getConfirmedTx(nerveChain, po.getConfirmedChangeVirtualBank());
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(confirmedTx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                for (HeterogeneousConfirmedVirtualBank virtualBank : listConfirmed) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(virtualBank.getHeterogeneousChainId());
                    if (docking == null) {
                        continue;
                    }
                    docking.txConfirmedCompleted(virtualBank.getHeterogeneousTxHash(), nerveChain.getLatestBasicBlock().getHeight(), nerveTxHash, remark);
                }
                // Cleaning Counters
                countHash.remove(nerveTxHash);
                return 0;
            }
            logger().info("Repeated collection of heterogeneous chain change confirmation, NerveTxHash: {}", nerveTxHash);
            // Collect every repeat5Check again if the collection is complete
            if (count.incrementAndGet() % 5 != 0) {
                logger().info("current count: {}", count.get());
                return 2;
            }
        }
        // Collection completed, assemble broadcast transaction
        if (vbSet.size() == hChainSize) {
            logger().info("Complete the collection of heterogeneous chain change confirmations, Create Confirmation Transaction, NerveTxHash: {}, Change the number of merged transactions: {}", nerveTxHash, mergeCount);
            List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
            VirtualBankUtil.sortListByChainId(hList);
            assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
            heterogeneousConfirmedChangeVBStorageService.save(vbPo);
            // Cleaning Counters
            countHash.remove(nerveTxHash);
            return 1;
        } else {
            logger().info("The current number of heterogeneous chain change confirmation transactions collected: {}, Number of all heterogeneous chains: {}, Change the number of merged transactions: {}", vbSet.size(), hChainSize, mergeCount);
            heterogeneousConfirmedChangeVBStorageService.save(vbPo);
        }
        return 0;
    }

    private void createCommonConfirmProposalTx(ProposalTypeEnum proposalType, NulsHash proposalTxHash, NulsHash proposalExeHash, String heterogeneousTxHash, byte[] address, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ProposalExeBusinessData businessData = new ProposalExeBusinessData();
        businessData.setProposalTxHash(proposalTxHash);
        businessData.setProposalExeHash(proposalExeHash);
        businessData.setHeterogeneousChainId(hChainId);
        businessData.setHeterogeneousTxHash(heterogeneousTxHash);
        businessData.setAddress(address);
        businessData.setListDistributionFee(signers);

        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(proposalType.value());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        // Publish proposal to confirm transaction
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
    }

    private void createUpgradeConfirmProposalTx(NulsHash nerveProposalHash, String heterogeneousTxHash, byte[] address, String multiSignAddress, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ConfirmUpgradeTxData businessData = new ConfirmUpgradeTxData();
        businessData.setNerveTxHash(nerveProposalHash);
        businessData.setHeterogeneousChainId(hChainId);
        businessData.setHeterogeneousTxHash(heterogeneousTxHash);
        businessData.setAddress(address);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        // Compatible with non Ethernet addresses update by pierre at 2021/11/16
        businessData.setOldAddress(docking.getAddressBytes(multiSignAddress));
        businessData.setListDistributionFee(signers);
        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(ProposalTypeEnum.UPGRADE.value());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        // Publish proposal to confirm transaction
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
    }
}
