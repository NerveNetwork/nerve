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
package network.nerve.converter.message.helper;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
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
 * @date: 2020-04-29
 */
@Component
public class ByzantineTransactionHelper {

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;

    public boolean genByzantineTransaction(Chain nerveChain, String byzantineTxhash, int txType, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        boolean validation = false;
        HeterogeneousHash hash;
        switch (txType) {
            case TxType.RECHARGE:
                hash = hashList.get(0);
                validation = recharge(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.CONFIRM_WITHDRAWAL:
                hash = hashList.get(0);
                validation = withdraw(nerveChain, byzantineTxhash, nerveTxHash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.CONFIRM_CHANGE_VIRTUAL_BANK:
                validation = change(nerveChain, byzantineTxhash, nerveTxHash, hashList);
                break;
            case TxType.CONFIRM_PROPOSAL:
                validation = proposal(nerveChain, byzantineTxhash, nerveTxHash, hashList);
                break;
        }
        return validation;
    }

    private boolean proposal(Chain nerveChain, String byzantineTxhash, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        if (nerveChain.getLogger().isDebugEnabled()) {
            nerveChain.getLogger().debug("生成确认提案交易: {}, nerveTxHash: {}, hashList: {}", byzantineTxhash, nerveTxHash, JSONUtils.obj2json(hashList));
        }
        NulsHash proposalHash = NulsHash.fromHex(nerveTxHash);
        ProposalPO proposalPO = proposalStorageService.find(nerveChain, proposalHash);
        NulsHash proposalExeHash = null;
        if (proposalPO.getNerveHash() != null) {
            proposalExeHash = new NulsHash(proposalPO.getNerveHash());
        }
        ProposalTypeEnum proposalType = ProposalTypeEnum.getEnum(proposalPO.getType());
        HeterogeneousHash heterogeneousHash = hashList.get(0);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousHash.getHeterogeneousChainId());
        List<HeterogeneousAddress> signers = null;
        long heterogeneousTxTime = 0L;
        String multySignAddress = proposalPO.getHeterogeneousMultySignAddress();
        switch (proposalType) {
            case UPGRADE:
            case WITHDRAW:
            case EXPELLED:
            case REFUND:
                HeterogeneousConfirmedInfo confirmedTxInfo = docking.getConfirmedTxInfo(heterogeneousHash.getHeterogeneousHash());
                if (confirmedTxInfo != null) {
                    signers = confirmedTxInfo.getSigners();
                    heterogeneousTxTime = confirmedTxInfo.getTxTime();
                }
                break;

        }
        ConfirmProposalTxData txData;
        if (ProposalTypeEnum.UPGRADE == proposalType) {
            txData = this.createUpgradeConfirmProposalTx(proposalHash, heterogeneousHash, proposalPO.getAddress(), multySignAddress, signers, heterogeneousTxTime);
        } else {
            txData = this.createCommonConfirmProposalTx(proposalType, proposalHash, proposalExeHash, heterogeneousHash, proposalPO.getAddress(), signers, heterogeneousTxTime);
        }
        Transaction tx = assembleTxService.createConfirmProposalTxWithoutSign(nerveChain, txData, heterogeneousTxTime);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[提案]拜占庭交易验证失败, 交易详情: {}", tx.format(ConfirmProposalTxData.class));
            return false;
        }
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
        return true;
    }

    private ConfirmProposalTxData createCommonConfirmProposalTx(ProposalTypeEnum proposalType, NulsHash proposalTxHash, NulsHash proposalExeHash, HeterogeneousHash heterogeneousHash, byte[] address, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ProposalExeBusinessData businessData = new ProposalExeBusinessData();
        businessData.setProposalTxHash(proposalTxHash);
        // 只有剔除会新生成nerve交易，从而存在nerve执行交易hash
        if (proposalType == ProposalTypeEnum.EXPELLED) {
            businessData.setProposalExeHash(proposalExeHash);
        }
        businessData.setHeterogeneousChainId(heterogeneousHash.getHeterogeneousChainId());
        businessData.setHeterogeneousTxHash(heterogeneousHash.getHeterogeneousHash());
        businessData.setAddress(address);
        businessData.setListDistributionFee(signers);

        ConfirmProposalTxData txData = new ConfirmProposalTxData();
        txData.setType(proposalType.value());
        try {
            txData.setBusinessData(businessData.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        return txData;
    }

    private ConfirmProposalTxData createUpgradeConfirmProposalTx(NulsHash nerveProposalHash, HeterogeneousHash heterogeneousTxHash, byte[] address, String multiSignAddress, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ConfirmUpgradeTxData businessData = new ConfirmUpgradeTxData();
        businessData.setNerveTxHash(nerveProposalHash);
        businessData.setHeterogeneousChainId(heterogeneousTxHash.getHeterogeneousChainId());
        businessData.setHeterogeneousTxHash(heterogeneousTxHash.getHeterogeneousHash());
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
        return txData;
    }

    private boolean recharge(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("创建[充值]拜占庭交易[{}]消息, 异构链交易hash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo depositTx = docking.getDepositTransaction(hTxHash);
        RechargeTxDTO dto = new RechargeTxDTO();
        dto.setOriginalTxHash(hTxHash);
        dto.setHeterogeneousFromAddress(depositTx.getFrom());
        dto.setToAddress(depositTx.getNerveAddress());
        dto.setAmount(depositTx.getValue());
        dto.setHeterogeneousChainId(hChainId);
        dto.setHeterogeneousAssetId(depositTx.getAssetId());
        dto.setTxtime(depositTx.getTxTime());
        Transaction tx = assembleTxService.createRechargeTxWithoutSign(nerveChain, dto);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[充值]拜占庭交易验证失败, 交易详情: {}", tx.format(RechargeTxData.class));
            return false;
        }
        assembleTxService.createRechargeTx(nerveChain, dto);
        return true;
    }

    private boolean withdraw(Chain nerveChain, String byzantineTxhash, String nerveTxHash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("创建[确认提现]拜占庭交易[{}]消息, nerveTxHash: {}, 异构链交易hash: {}", byzantineTxhash, nerveTxHash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo hTx = docking.getWithdrawTransaction(hTxHash);
        ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
        txData.setHeterogeneousChainId(hChainId);
        txData.setHeterogeneousHeight(hTx.getBlockHeight());
        txData.setHeterogeneousTxHash(hTxHash);
        txData.setWithdrawalTxHash(NulsHash.fromHex(nerveTxHash));
        txData.setListDistributionFee(hTx.getSigners());
        Transaction tx = assembleTxService.createConfirmWithdrawalTxWithoutSign(nerveChain, txData, hTx.getTxTime());
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[确认提现]拜占庭交易验证失败, 交易详情: {}", tx.format(ConfirmWithdrawalTxData.class));
            return false;
        }
        assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, hTx.getTxTime());
        return true;
    }

    private boolean change(Chain nerveChain, String byzantineTxhash, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        nerveChain.getLogger().info("创建[确认变更]拜占庭交易[{}]消息, nerveTxHash: {}, 异构链交易hash: {}", byzantineTxhash, nerveTxHash, JSONUtils.obj2json(hashList));
        HeterogeneousConfirmedChangeVBPo vbPo = heterogeneousConfirmedChangeVBStorageService.findByTxHash(nerveTxHash);
        if(vbPo == null) {
            vbPo = new HeterogeneousConfirmedChangeVBPo();
            vbPo.setNerveTxHash(nerveTxHash);
            vbPo.setHgCollection(new HashSet<>());
        }
        Set<HeterogeneousConfirmedVirtualBank> vbSet = vbPo.getHgCollection();
        int hChainId;
        String hTxHash;
        Long nerveTxTime = null;
        for(HeterogeneousHash hash : hashList) {
            hChainId = hash.getHeterogeneousChainId();
            hTxHash = hash.getHeterogeneousHash();
            IHeterogeneousChainDocking chainDocking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
            HeterogeneousConfirmedInfo confirmedTxInfo;
            if (StringUtils.isBlank(hTxHash) || (confirmedTxInfo = chainDocking.getConfirmedTxInfo(hTxHash)) == null ) {
                if (nerveTxTime == null) {
                    Transaction nerveTx = TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(nerveTxHash));
                    nerveTxTime = nerveTx.getTime();
                }
                HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank( nerveTxHash, hChainId, chainDocking.getCurrentMultySignAddress(), null, nerveTxTime, null);
                vbSet.add(bank);
                continue;
            }
            HeterogeneousConfirmedVirtualBank bank = new HeterogeneousConfirmedVirtualBank(
                    nerveTxHash,
                    hChainId,
                    confirmedTxInfo.getMultySignAddress(),
                    hTxHash,
                    confirmedTxInfo.getTxTime(),
                    confirmedTxInfo.getSigners()
            );
            vbSet.add(bank);
        }
        List<HeterogeneousConfirmedVirtualBank> hList = new ArrayList<>(vbSet);
        VirtualBankUtil.sortListByChainId(hList);
        Transaction tx = assembleTxService.createConfirmedChangeVirtualBankTxWithoutSign(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[确认变更]拜占庭交易验证失败, 交易详情: {}", tx.format(ConfirmedChangeVirtualBankTxData.class));
            return false;
        }
        assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
        return true;
    }
}
