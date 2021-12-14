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
package network.nerve.converter.core.heterogeneous.callback;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
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
import org.web3j.utils.Numeric;

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
     * 异构链chainId
     */
    private int hChainId;
    private AssembleTxService assembleTxService;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    private ProposalStorageService proposalStorageService;
    private MergeComponentStorageService mergeComponentStorageService;
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    private ConverterCoreApi converterCoreApi;
    // 重复收集计时器
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
     * 管理员变更: 收集完所有异构链组件的管理员变更确认交易，再组装发送交易
     * 提现: 直接组装发送交易
     *
     * @param txType           交易类型 - WITHDRAW/CHANGE 提现/管理员变更
     * @param nerveTxHash      本链交易hash
     * @param txHash           异构链交易hash
     * @param blockHeight      异构链交易确认高度
     * @param txTime           异构链交易时间
     * @param multiSignAddress 当前多签地址
     * @param signers          交易签名地址列表
     */
    @Override
    public void txConfirmed(HeterogeneousChainTxType txType, String nerveTxHash, String txHash, Long blockHeight, Long txTime, String multiSignAddress, List<HeterogeneousAddress> signers) throws Exception {
        NulsHash nerveHash = NulsHash.fromHex(nerveTxHash);
        // 检查是否由提案发起的确认交易
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
                    logger().info("提案执行确认[{}], proposalHash: {}, ETH txHash: {}", proposalType, nerveProposalHash.toHex(), txHash);
                    this.createCommonConfirmProposalTx(proposalType, nerveProposalHash, null, txHash, proposalPO.getAddress(), signers, txTime);
                    break;
                }
                // 正常提现的确认交易
                ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
                txData.setHeterogeneousChainId(hChainId);
                txData.setHeterogeneousHeight(blockHeight);
                txData.setHeterogeneousTxHash(txHash);
                txData.setWithdrawalTxHash(nerveHash);
                txData.setListDistributionFee(signers);
                assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, txTime);
                break;
            case CHANGE:
                // 查询合并数据库，检查变更交易是否合并
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
                    // 为空时，表示没有add和remove的直接确认交易，则确认交易的时间使用原始变更交易的时间
                    if (txTime == null) {
                        Transaction realNerveTx = TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(realNerveTxHash));
                        realTxTime = realNerveTx.getTime();
                    }
                    HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank(realNerveTxHash, hChainId, multiSignAddress, txHash, realTxTime, signers);
                    this.checkProposal(realNerveTxHash, txHash, realTxTime, signers);
                    this.confirmChangeTx(realNerveTxHash, bank, mergeCount);
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
                    logger().info("提案执行确认[多签合约升级], proposalHash: {}, ETH txHash: {}", nerveProposalHash.toHex(), txHash);
                    this.createUpgradeConfirmProposalTx(nerveProposalHash, txHash, proposalPO.getAddress(), multiSignAddress, signers, txTime);
                }
                break;
        }
    }

    @Override
    public void pendingTxOfWithdraw(String nerveTxHash, String heterogeneousTxHash) throws Exception {
        if (!converterCoreApi.isSupportNewMechanismOfWithdrawalFee()) {
            return;
        }
        // 提现待确认交易组装并发出
        WithdrawalHeterogeneousSendTxData txData = new WithdrawalHeterogeneousSendTxData();
        txData.setNerveTxHash(nerveTxHash);
        txData.setHeterogeneousTxHash(heterogeneousTxHash);
        txData.setHeterogeneousChainId(hChainId);
        Transaction nerveTx = converterCoreApi.getNerveTx(nerveTxHash);
        assembleTxService.withdrawalHeterogeneousSendTx(nerveChain, txData, nerveTx.getTime());
    }

    private void checkProposal(String nerveTxHash, String txHash, Long txTime, List<HeterogeneousAddress> signers) throws NulsException {
        NulsHash nerveHash = NulsHash.fromHex(nerveTxHash);
        // 检查是否由提案发起的确认交易
        NulsHash nerveProposalHash = proposalStorageService.getExeBusiness(nerveChain, nerveTxHash);
        ProposalPO proposalPO = null;
        if (nerveProposalHash != null) {
            proposalPO = proposalStorageService.find(nerveChain, nerveProposalHash);
        }
        if (proposalPO != null) {
            logger().info("提案执行确认[撤销银行资格], proposalHash: {}, ETH txHash: {}", nerveProposalHash.toHex(), txHash);
            this.createCommonConfirmProposalTx(ProposalTypeEnum.EXPELLED, nerveProposalHash, nerveHash, txHash, proposalPO.getAddress(), signers, txTime);
        }
    }

    private int confirmChangeTx(String nerveTxHash, HeterogeneousConfirmedVirtualBank bank, int mergeCount) throws Exception {
        AtomicInteger count = countHash.get(nerveTxHash);
        if (count == null) {
            count = new AtomicInteger(0);
            countHash.put(nerveTxHash, count);
        }
        logger().info("收集异构链变更确认, NerveTxHash: {}", nerveTxHash);
        HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
        if (vbPo == null) {
            vbPo = new HeterogeneousConfirmedChangeVBPo();
            vbPo.setNerveTxHash(nerveTxHash);
            vbPo.setHgCollection(new HashSet<>());
        }
        Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
        int hChainSize = heterogeneousDockingManager.getAllHeterogeneousDocking().size();
        // 检查重复添加
        if (!vbSet.add(bank)) {
            // 检查是否已经确认
            ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(nerveChain, nerveTxHash);
            if (po != null) {
                Transaction confirmedTx = TransactionCall.getConfirmedTx(nerveChain, po.getConfirmedChangeVirtualBank());
                ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(confirmedTx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
                List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();
                for (HeterogeneousConfirmedVirtualBank virtualBank : listConfirmed) {
                    heterogeneousDockingManager.getHeterogeneousDocking(virtualBank.getHeterogeneousChainId()).txConfirmedCompleted(virtualBank.getHeterogeneousTxHash(), nerveChain.getLatestBasicBlock().getHeight(), nerveTxHash);
                }
                // 清理计数器
                countHash.remove(nerveTxHash);
                return 0;
            }
            logger().info("重复收集异构链变更确认, NerveTxHash: {}", nerveTxHash);
            // 每重复收集5次，再检查是否收集完成
            if (count.incrementAndGet() % 5 != 0) {
                logger().info("当前 count: {}", count.get());
                return 2;
            }
        }
        // 收集完成，组装广播交易
        if (vbSet.size() == hChainSize) {
            logger().info("完成收集异构链变更确认, 创建确认交易, NerveTxHash: {}, 变更合并交易数量: {}", nerveTxHash, mergeCount);
            List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
            VirtualBankUtil.sortListByChainId(hList);
            assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
            heterogeneousConfirmedChangeVBStorageService.save(vbPo);
            // 清理计数器
            countHash.remove(nerveTxHash);
            return 1;
        } else {
            logger().info("当前已收集的异构链变更确认交易数量: {}, 所有异构链数量: {}, 变更合并交易数量: {}", vbSet.size(), hChainSize, mergeCount);
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
        // 发布提案确认交易
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
    }

    private void createUpgradeConfirmProposalTx(NulsHash nerveProposalHash, String heterogeneousTxHash, byte[] address, String multiSignAddress, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ConfirmUpgradeTxData businessData = new ConfirmUpgradeTxData();
        businessData.setNerveTxHash(nerveProposalHash);
        businessData.setHeterogeneousChainId(hChainId);
        businessData.setHeterogeneousTxHash(heterogeneousTxHash);
        businessData.setAddress(address);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        // 兼容非以太系地址 update by pierre at 2021/11/16
        businessData.setOldAddress(docking.getAddressBytes(multiSignAddress));
        businessData.setListDistributionFee(signers);
        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(ProposalTypeEnum.UPGRADE.value());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        // 发布提案确认交易
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
    }
}
