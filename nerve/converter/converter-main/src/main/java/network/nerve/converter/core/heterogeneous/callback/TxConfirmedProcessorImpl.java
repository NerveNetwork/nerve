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
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.callback.interfaces.ITxConfirmedProcessor;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ConfirmUpgradeTxData;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.storage.ProposalExeStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ProposalExeStorageService proposalExeStorageService;

    public TxConfirmedProcessorImpl(Chain nerveChain, int hChainId, AssembleTxService assembleTxService,
                                    HeterogeneousDockingManager heterogeneousDockingManager,
                                    HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService,
                                    ProposalStorageService proposalStorageService,
                                    ProposalExeStorageService proposalExeStorageService) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.assembleTxService = assembleTxService;
        this.heterogeneousDockingManager = heterogeneousDockingManager;
        this.heterogeneousConfirmedChangeVBStorageService = heterogeneousConfirmedChangeVBStorageService;
        this.proposalStorageService = proposalStorageService;
        this.proposalExeStorageService = proposalExeStorageService;
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
        switch (txType) {
            case WITHDRAW:
                ProposalPO proposalPO1 = proposalStorageService.find(nerveChain, nerveHash);
                boolean isProposal = proposalPO1 != null;
                if(isProposal) {
                    logger().info("提案执行确认[退回资金], proposalHash: {}, ETH txHash: {}", nerveTxHash, txHash);
                    this.createRefundOrDisqualificationConfirmProposalTx(ProposalTypeEnum.REFUND, nerveHash, null, txHash, proposalPO1.getAddress(), signers, txTime);
                    break;
                }
                ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
                txData.setHeterogeneousChainId(hChainId);
                txData.setHeterogeneousHeight(blockHeight);
                txData.setHeterogeneousTxHash(txHash);
                txData.setWithdrawalTxHash(NulsHash.fromHex(nerveTxHash));
                txData.setListDistributionFee(signers);
                assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, txTime);
                break;
            case CHANGE:
                // 成员变更的提案确认交易
                NulsHash proposalHash = proposalExeStorageService.find(nerveChain, nerveTxHash);
                if(proposalHash != null) {
                    ProposalPO proposalPO3 = proposalStorageService.find(nerveChain, proposalHash);
                    boolean isProposa3 = proposalPO3 != null;
                    if(isProposa3) {
                        logger().info("提案执行确认[撤销银行资格], proposalHash: {}, ETH txHash: {}", proposalHash.toHex(), txHash);
                        this.createRefundOrDisqualificationConfirmProposalTx(ProposalTypeEnum.EXPELLED, proposalHash, nerveHash, txHash, proposalPO3.getAddress(), signers, txTime);
                        break;
                    }
                }
                logger().info("收集异构链变更确认, NerveTxHash: {}", nerveTxHash);
                HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank();
                bank.setEffectiveTime(txTime);
                bank.setHeterogeneousChainId(hChainId);
                bank.setHeterogeneousTxHash(txHash);
                bank.setHeterogeneousAddress(multiSignAddress);
                bank.setSignedHeterogeneousAddress(signers);
                HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
                if(vbPo == null) {
                    vbPo = new HeterogeneousConfirmedChangeVBPo();
                    vbPo.setNerveTxHash(nerveTxHash);
                    vbPo.setHgCollection(new HashSet<>());
                }
                Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
                int hChainSize = heterogeneousDockingManager.getAllHeterogeneousDocking().size();
                // 检查重复添加
                if (!vbSet.add(bank)) {
                    logger().warn("重复收集异构链变更确认, NerveTxHash: {}", nerveTxHash);
                    return;
                }
                heterogeneousConfirmedChangeVBStorageService.save(vbPo);
                // 收集完成，组装广播交易
                if (vbSet.size() == hChainSize) {
                    logger().info("完成收集异构链变更确认, 创建确认交易, NerveTxHash: {}", nerveTxHash);
                    List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
                    VirtualBankUtil.sortListByChainId(hList);
                    assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
                } else {
                    logger().info("当前已收集的异构链变更确认交易数量: {}, 所有异构链数量: {}", vbSet.size(), hChainSize);
                }
                break;
            case UPGRADE:
                ProposalPO proposalPO2 = proposalStorageService.find(nerveChain, nerveHash);
                boolean isProposal2 = proposalPO2 != null;
                if(isProposal2) {
                    logger().info("提案执行确认[多签合约升级], proposalHash: {}, ETH txHash: {}", nerveHash.toHex(), txHash);
                    this.createUpgradeConfirmProposalTx(nerveHash, txHash, proposalPO2.getAddress(), multiSignAddress, signers, txTime);
                }
                break;
        }
    }

    private void createRefundOrDisqualificationConfirmProposalTx(ProposalTypeEnum proposalType, NulsHash proposalTxHash, NulsHash proposalExeHash, String heterogeneousTxHash, byte[] address, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
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

    private void createUpgradeConfirmProposalTx(NulsHash nerveHash, String heterogeneousTxHash, byte[] address, String multiSignAddress, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ConfirmUpgradeTxData businessData = new ConfirmUpgradeTxData();
        businessData.setNerveTxHash(nerveHash);
        businessData.setHeterogeneousChainId(hChainId);
        businessData.setHeterogeneousTxHash(heterogeneousTxHash);
        businessData.setAddress(address);
        businessData.setOldAddress(Numeric.hexStringToByteArray(multiSignAddress));
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
