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
import network.nerve.converter.btc.model.BtcTxInfo;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.AddFeeCrossChainTxDTO;
import network.nerve.converter.model.dto.RechargeTxDTO;
import network.nerve.converter.model.dto.RechargeTxOfBtcSysDTO;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;

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
            case TxType.RECHARGE_UNCONFIRMED:
                hash = hashList.get(0);
                validation = rechargeUnconfirmed(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.ONE_CLICK_CROSS_CHAIN_UNCONFIRMED:
                hash = hashList.get(0);
                validation = oneClickCrossChainUnconfirmed(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.ONE_CLICK_CROSS_CHAIN:
                hash = hashList.get(0);
                validation = oneClickCrossChain(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN:
                hash = hashList.get(0);
                validation = addFeeCrossChain(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
            case TxType.WITHDRAWAL_UTXO:
                hash = hashList.get(0);
                validation = withdrawUTXO(nerveChain, byzantineTxhash, nerveTxHash, hash.getHeterogeneousChainId());
                break;
            case TxType.WITHDRAWAL_UTXO_FEE_PAYMENT:
                hash = hashList.get(0);
                validation = withdrawFee(nerveChain, byzantineTxhash, hash.getHeterogeneousChainId(), hash.getHeterogeneousHash());
                break;
        }
        return validation;
    }

    private boolean withdrawFee(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish [BtcSys-RecordWithdrawFee] Byzantine transactions [{}] news, Heterogeneous Chain Trading hash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        WithdrawalFeeLog feeLog = docking.getBitCoinApi().takeWithdrawalFeeLogFromTxParse(hTxHash);
        if (feeLog == null) {
            return false;
        }
        Transaction tx = assembleTxService.createWithdrawlFeeLogTxWithoutSign(nerveChain, feeLog, feeLog.getTxTime(), String.format("Record withdraw fee [%s], chainId: %s, amount: %s, hash: %s, blockHeight: %s, blockHash: %s",
                feeLog.isRecharge() ? "RECHARGE" : "USED",
                hChainId,
                feeLog.getFee(),
                hTxHash,
                feeLog.getBlockHeight(),
                feeLog.getBlockHash()).getBytes(StandardCharsets.UTF_8));
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[BtcSys-RecordWithdrawFee] Byzantine transaction verification failed, transaction details: {}", tx.format(WithdrawalFeeLog.class));
            return false;
        }
        assembleTxService.createWithdrawlFeeLogTx(nerveChain, feeLog, feeLog.getTxTime(), String.format("Record withdraw fee [%s], chainId: %s, amount: %s, hash: %s, blockHeight: %s, blockHash: %s",
                feeLog.isRecharge() ? "RECHARGE" : "USED",
                hChainId,
                feeLog.getFee(),
                hTxHash,
                feeLog.getBlockHeight(),
                feeLog.getBlockHash()).getBytes(StandardCharsets.UTF_8));
        return true;
    }


    private boolean proposal(Chain nerveChain, String byzantineTxhash, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        if (nerveChain.getLogger().isDebugEnabled()) {
            nerveChain.getLogger().debug("Generate Confirmation Proposal Transaction: {}, nerveTxHash: {}, hashList: {}", byzantineTxhash, nerveTxHash, JSONUtils.obj2json(hashList));
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
            nerveChain.getLogger().error("[proposal]Byzantine transaction verification failed, transaction details: {}", tx.format(ConfirmProposalTxData.class));
            return false;
        }
        assembleTxService.createConfirmProposalTx(nerveChain, txData, heterogeneousTxTime);
        return true;
    }

    private ConfirmProposalTxData createCommonConfirmProposalTx(ProposalTypeEnum proposalType, NulsHash proposalTxHash, NulsHash proposalExeHash, HeterogeneousHash heterogeneousHash, byte[] address, List<HeterogeneousAddress> signers, long heterogeneousTxTime) throws NulsException {
        ProposalExeBusinessData businessData = new ProposalExeBusinessData();
        businessData.setProposalTxHash(proposalTxHash);
        // Only by removing will a new generation be generatednerveTransaction, thus existingnerveExecute transactionshash
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
        int htgChainId = heterogeneousTxHash.getHeterogeneousChainId();
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        ConfirmUpgradeTxData businessData = new ConfirmUpgradeTxData();
        businessData.setNerveTxHash(nerveProposalHash);
        businessData.setHeterogeneousChainId(htgChainId);
        businessData.setHeterogeneousTxHash(heterogeneousTxHash.getHeterogeneousHash());
        businessData.setAddress(address);
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
        return txData;
    }

    private boolean recharge(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        if (hChainId < 200) {
            return rechargeOfEvmSys(nerveChain, byzantineTxhash, hChainId, hTxHash);
        } else if (hChainId < 300) {
            return rechargeOfBtcSys(nerveChain, byzantineTxhash, hChainId, hTxHash);
        } else {
            return false;
        }
    }

    private boolean rechargeOfEvmSys(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish[Recharge]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", byzantineTxhash, hTxHash);
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
        dto.setExtend(depositTx.getDepositIIExtend());
        if (depositTx.isDepositIIMainAndToken()) {
            // Support simultaneous transfer intokenandmain
            dto.setDepositII(true);
            dto.setMainAmount(depositTx.getDepositIIMainAssetValue());
        }
        Transaction tx = assembleTxService.createRechargeTxWithoutSign(nerveChain, dto);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[Recharge-Ethereumsystem]Byzantine transaction verification failed, transaction details: {}", tx.format(RechargeTxData.class));
            return false;
        }
        assembleTxService.createRechargeTx(nerveChain, dto);
        return true;
    }

    private boolean rechargeOfBtcSys(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish[Recharge-BitCoinsystem]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo depositTx = docking.getDepositTransaction(hTxHash);
        RechargeTxOfBtcSysDTO dto = getRechargeTxOfBtcSysDTO(hChainId, (BtcTxInfo) depositTx);
        Transaction tx = assembleTxService.createRechargeTxOfBtcSysWithoutSign(nerveChain, dto);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[RechargeBtcSys-Ethereumsystem]Byzantine transaction verification failed, transaction details: {}", tx.format(RechargeTxData.class));
            return false;
        }
        assembleTxService.createRechargeTxOfBtcSys(nerveChain, dto);
        return true;
    }

    @NotNull
    private static RechargeTxOfBtcSysDTO getRechargeTxOfBtcSysDTO(int hChainId, BtcTxInfo depositTx) {
        BtcTxInfo txInfo = depositTx;
        RechargeTxOfBtcSysDTO dto = new RechargeTxOfBtcSysDTO();
        dto.setHtgTxHash(txInfo.getTxHash());
        dto.setHtgFrom(txInfo.getFrom());
        dto.setHtgChainId(hChainId);
        dto.setHtgAssetId(txInfo.getAssetId());
        dto.setHtgTxTime(txInfo.getTxTime());
        dto.setHtgBlockHeight(txInfo.getBlockHeight());
        dto.setTo(txInfo.getNerveAddress());
        dto.setAmount(txInfo.getValue());
        dto.setFee(txInfo.getFee());
        dto.setFeeTo(txInfo.getNerveFeeTo());
        dto.setExtend(txInfo.getExtend0());
        return dto;
    }

    private boolean rechargeUnconfirmed(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish[Recharge to be confirmed]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo depositTx = docking.getDepositTransaction(hTxHash);
        RechargeUnconfirmedTxData dto = new RechargeUnconfirmedTxData();
        dto.setHeterogeneousHeight(depositTx.getBlockHeight());
        dto.setOriginalTxHash(new HeterogeneousHash(hChainId, hTxHash));
        dto.setHeterogeneousFromAddress(depositTx.getFrom());
        dto.setNerveToAddress(AddressTool.getAddress(depositTx.getNerveAddress()));
        dto.setAmount(depositTx.getValue());
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(
                hChainId,
                depositTx.getAssetId());
        dto.setAssetChainId(nerveAssetInfo.getAssetChainId());
        dto.setAssetId(nerveAssetInfo.getAssetId());
        if (depositTx.isDepositIIMainAndToken()) {
            // Simultaneously rechargetokenandmainRecord main asset recharge data
            NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
            dto.setMainAssetAmount(depositTx.getDepositIIMainAssetValue());
            dto.setMainAssetChainId(mainAsset.getAssetChainId());
            dto.setMainAssetId(mainAsset.getAssetId());
        }
        Transaction tx = assembleTxService.rechargeUnconfirmedTxWithoutSign(nerveChain, dto, depositTx.getTxTime());
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[Recharge]Byzantine transaction verification failed, transaction details: {}", tx.format(RechargeTxData.class));
            return false;
        }
        assembleTxService.rechargeUnconfirmedTx(nerveChain, dto, depositTx.getTxTime());
        return true;
    }

    private boolean oneClickCrossChainUnconfirmed(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish[One click cross chain pending confirmation]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo occcTx = docking.getDepositTransaction(hTxHash);
        String extend = occcTx.getDepositIIExtend();
        HeterogeneousOneClickCrossChainData data = docking.parseOneClickCrossChainData(extend);
        if (data == null) {
            nerveChain.getLogger().error("[One click cross chain pending confirmation]Byzantine transaction verification failed, extend: {}", extend);
            return false;
        }
        BigInteger tipping = data.getTipping();
        String tippingAddress = data.getTippingAddress();
        BigInteger erc20Value, mainAssetValue;
        Integer erc20AssetId;
        if (occcTx.isDepositIIMainAndToken()) {
            erc20Value = occcTx.getValue();
            erc20AssetId = occcTx.getAssetId();
            mainAssetValue = occcTx.getDepositIIMainAssetValue();
        } else if (occcTx.isIfContractAsset()) {
            erc20Value = occcTx.getValue();
            erc20AssetId = occcTx.getAssetId();
            mainAssetValue = BigInteger.ZERO;
        } else {
            erc20Value = BigInteger.ZERO;
            erc20AssetId = 0;
            mainAssetValue = occcTx.getValue();
        }
        Long txTime = occcTx.getTxTime();
        OneClickCrossChainUnconfirmedTxData txData = new OneClickCrossChainUnconfirmedTxData();
        txData.setMainAssetFeeAmount(data.getFeeAmount());
        txData.setDesChainId(data.getDesChainId());
        txData.setDesToAddress(data.getDesToAddress());
        txData.setDesExtend(data.getDesExtend());
        txData.setOriginalTxHash(new HeterogeneousHash(hChainId, hTxHash));
        txData.setHeterogeneousHeight(occcTx.getBlockHeight());
        txData.setHeterogeneousFromAddress(occcTx.getFrom());
        txData.setNerveToAddress(AddressTool.getAddress(occcTx.getNerveAddress()));
        txData.setTipping(tipping);
        txData.setTippingAddress(tippingAddress);
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        txData.setMainAssetAmount(mainAssetValue);
        txData.setMainAssetChainId(mainAsset.getAssetChainId());
        txData.setMainAssetId(mainAsset.getAssetId());
        // recordtokenRecharge data
        txData.setErc20Amount(erc20Value);
        if (erc20AssetId > 1) {
            NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
            txData.setErc20AssetChainId(tokenAsset.getAssetChainId());
            txData.setErc20AssetId(tokenAsset.getAssetId());
            txData.setTippingChainId(tokenAsset.getAssetChainId());
            txData.setTippingAssetId(tokenAsset.getAssetId());
        } else {
            txData.setTippingChainId(mainAsset.getAssetChainId());
            txData.setTippingAssetId(mainAsset.getAssetId());
        }

        Transaction tx = assembleTxService.oneClickCrossChainUnconfirmedTxWithoutSign(nerveChain, txData, txTime);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[One click cross chain pending confirmation]Byzantine transaction verification failed, transaction details: {}", tx.format(OneClickCrossChainUnconfirmedTxData.class));
            return false;
        }
        assembleTxService.oneClickCrossChainUnconfirmedTx(nerveChain, txData, txTime);
        return true;
    }

    private boolean oneClickCrossChain(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        // Create one click cross chainpending check
        nerveChain.getLogger().info("[{}]establish[One click cross chain]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", hChainId, byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo occcTx = docking.getDepositTransaction(hTxHash);
        String extend = occcTx.getDepositIIExtend();
        HeterogeneousOneClickCrossChainData data = docking.parseOneClickCrossChainData(extend);
        if (data == null) {
            nerveChain.getLogger().error("[One click cross chain]Byzantine transaction verification failed, extend: {}", extend);
            return false;
        }
        BigInteger tipping = data.getTipping();
        String tippingAddress = data.getTippingAddress();
        BigInteger erc20Value, mainAssetValue;
        Integer erc20AssetId;
        if (occcTx.isDepositIIMainAndToken()) {
            erc20Value = occcTx.getValue();
            erc20AssetId = occcTx.getAssetId();
            mainAssetValue = occcTx.getDepositIIMainAssetValue();
        } else if (occcTx.isIfContractAsset()) {
            erc20Value = occcTx.getValue();
            erc20AssetId = occcTx.getAssetId();
            mainAssetValue = BigInteger.ZERO;
        } else {
            erc20Value = BigInteger.ZERO;
            erc20AssetId = 0;
            mainAssetValue = occcTx.getValue();
        }
        Long txTime = occcTx.getTxTime();
        OneClickCrossChainUnconfirmedTxData dto = new OneClickCrossChainUnconfirmedTxData();
        dto.setMainAssetFeeAmount(data.getFeeAmount());
        dto.setDesChainId(data.getDesChainId());
        dto.setDesToAddress(data.getDesToAddress());
        dto.setDesExtend(data.getDesExtend());
        dto.setOriginalTxHash(new HeterogeneousHash(hChainId, hTxHash));
        dto.setHeterogeneousHeight(occcTx.getBlockHeight());
        dto.setHeterogeneousFromAddress(occcTx.getFrom());
        dto.setNerveToAddress(AddressTool.getAddress(occcTx.getNerveAddress()));
        dto.setTipping(tipping);
        dto.setTippingAddress(tippingAddress);
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetAmount(mainAssetValue);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        // recordtokenRecharge data
        dto.setErc20Amount(erc20Value);
        if (erc20AssetId > 1) {
            NerveAssetInfo tokenAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, erc20AssetId);
            dto.setErc20AssetChainId(tokenAsset.getAssetChainId());
            dto.setErc20AssetId(tokenAsset.getAssetId());
            dto.setTippingChainId(tokenAsset.getAssetChainId());
            dto.setTippingAssetId(tokenAsset.getAssetId());
        } else {
            dto.setTippingChainId(mainAsset.getAssetChainId());
            dto.setTippingAssetId(mainAsset.getAssetId());
        }

        Transaction tx = assembleTxService.createOneClickCrossChainTxWithoutSign(nerveChain, dto, txTime);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[One click cross chain]Byzantine transaction verification failed, transaction details: {}", tx.format(OneClickCrossChainTxData.class));
            return false;
        }
        assembleTxService.createOneClickCrossChainTx(nerveChain, dto, txTime);
        return true;
    }

    private boolean addFeeCrossChain(Chain nerveChain, String byzantineTxhash, int hChainId, String hTxHash) throws Exception {
        // Create cross chain additional transaction feespending check
        nerveChain.getLogger().info("[{}]establish[Cross chain additional handling fees]Byzantine transactions[{}]news, Heterogeneous Chain Tradinghash: {}", hChainId, byzantineTxhash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo afccTx = docking.getDepositTransaction(hTxHash);
        String extend = afccTx.getDepositIIExtend();
        HeterogeneousAddFeeCrossChainData data = docking.parseAddFeeCrossChainData(extend);
        if (data == null) {
            nerveChain.getLogger().error("[Cross chain additional handling fees]Byzantine transaction verification failed, extend: {}", extend);
            return false;
        }
        BigInteger mainAssetValue = afccTx.getValue();
        Long txTime = afccTx.getTxTime();
        AddFeeCrossChainTxDTO dto = new AddFeeCrossChainTxDTO();
        dto.setOriginalTxHash(new HeterogeneousHash(hChainId, hTxHash));
        dto.setHeterogeneousHeight(afccTx.getBlockHeight());
        dto.setHeterogeneousFromAddress(afccTx.getFrom());
        dto.setNerveToAddress(AddressTool.getAddress(afccTx.getNerveAddress()));
        // Record main asset recharge data
        NerveAssetInfo mainAsset = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, 1);
        dto.setMainAssetChainId(mainAsset.getAssetChainId());
        dto.setMainAssetId(mainAsset.getAssetId());
        dto.setMainAssetAmount(mainAssetValue);
        dto.setNerveTxHash(data.getNerveTxHash());
        dto.setSubExtend(data.getSubExtend());

        Transaction tx = assembleTxService.createAddFeeCrossChainTxWithoutSign(nerveChain, dto, txTime);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[Cross chain additional handling fees]Byzantine transaction verification failed, transaction details: {}", tx.format(WithdrawalAddFeeByCrossChainTxData.class));
            return false;
        }
        assembleTxService.createAddFeeCrossChainTx(nerveChain, dto, txTime);
        return true;
    }

    private boolean withdraw(Chain nerveChain, String byzantineTxhash, String nerveTxHash, int hChainId, String hTxHash) throws Exception {
        nerveChain.getLogger().info("establish [Confirm withdrawal] Byzantine transactions [{}] news, nerveTxHash: {}, Heterogeneous Chain Trading hash: {}", byzantineTxhash, nerveTxHash, hTxHash);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(hChainId);
        HeterogeneousTransactionInfo hTx = docking.getWithdrawTransaction(hTxHash);
        ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
        txData.setHeterogeneousChainId(hChainId);
        txData.setHeterogeneousHeight(hTx.getBlockHeight());
        txData.setHeterogeneousTxHash(hTxHash);
        txData.setWithdrawalTxHash(NulsHash.fromHex(nerveTxHash));
        txData.setListDistributionFee(hTx.getSigners());
        byte[] remark = null;
        if (hChainId > 200) {
            BtcUnconfirmedTxPo btcPo = (BtcUnconfirmedTxPo) hTx;
            remark = btcPo.getCheckWithdrawalUsedUTXOData();
        }
        Transaction tx = assembleTxService.createConfirmWithdrawalTxWithoutSign(nerveChain, txData, hTx.getTxTime(), remark);
        if(!byzantineTxhash.equals(tx.getHash().toHex())) {
            nerveChain.getLogger().error("[Confirm withdrawal]Byzantine transaction verification failed, transaction details: {}", tx.format(ConfirmWithdrawalTxData.class));
            return false;
        }
        assembleTxService.createConfirmWithdrawalTx(nerveChain, txData, hTx.getTxTime(), remark);
        return true;
    }

    private boolean withdrawUTXO(Chain nerveChain, String byzantineTxhash, String nerveTxHash, int _htgChainId) throws Exception {
        nerveChain.getLogger().info("establish [Withdrawal UTXO]Byzantine transactions [{}] news, nerveTxHash: {}", byzantineTxhash, nerveTxHash);
        Transaction withdrawTx = TransactionCall.getConfirmedTx(nerveChain, nerveTxHash);
        WithdrawalUTXOTxData txData = null;
        if (withdrawTx.getType() == TxType.WITHDRAWAL) {
            WithdrawalTxData txData1 = ConverterUtil.getInstance(withdrawTx.getTxData(), WithdrawalTxData.class);
            int htgChainId = txData1.getHeterogeneousChainId();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
            String currentMultySignAddress = docking.getCurrentMultySignAddress();

            WithdrawalUTXOTxData oldUtxoData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);

            if (oldUtxoData != null && oldUtxoData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD) {
                txData = heterogeneousAssetHelper.rebuildWithdrawalUTXOsTxData(nerveChain, withdrawTx, htgChainId);
            } else {
                txData = heterogeneousAssetHelper.makeWithdrawalUTXOsTxData(nerveChain, withdrawTx, htgChainId, currentMultySignAddress, nerveChain.getMapVirtualBank());
            }
        } else if (withdrawTx.getType() == TxType.CHANGE_VIRTUAL_BANK) {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(_htgChainId);
            String currentMultySignAddress = docking.getCurrentMultySignAddress();
            WithdrawalUTXOTxData oldUtxoData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
            if (oldUtxoData != null && oldUtxoData.getFeeRate() > ConverterUtil.FEE_RATE_REBUILD) {
                txData = heterogeneousAssetHelper.rebuildManagerChangeUTXOTxData(nerveChain, withdrawTx, _htgChainId, currentMultySignAddress, nerveChain.getMapVirtualBank());
            } else {
                txData = heterogeneousAssetHelper.makeManagerChangeUTXOTxData(nerveChain, withdrawTx, _htgChainId, currentMultySignAddress, nerveChain.getMapVirtualBank());
            }
        }

        Transaction withdrawlUTXOTx = assembleTxService.createWithdrawUTXOTxWithoutSign(nerveChain, txData, withdrawTx.getTime());
        if(!byzantineTxhash.equals(withdrawlUTXOTx.getHash().toHex())) {
            nerveChain.getLogger().error("[Withdrawal UTXO]Byzantine transaction verification failed, transaction details: {}", withdrawlUTXOTx.format(WithdrawalUTXOTxData.class));
            return false;
        }
        assembleTxService.createWithdrawUTXOTx(nerveChain, txData, withdrawTx.getTime());
        return true;
    }

    private boolean change(Chain nerveChain, String byzantineTxhash, String nerveTxHash, List<HeterogeneousHash> hashList) throws Exception {
        nerveChain.getLogger().info("establish[Confirm changes]Byzantine transactions[{}]news, nerveTxHash: {}, Heterogeneous Chain Tradinghash: {}", byzantineTxhash, nerveTxHash, JSONUtils.obj2json(hashList));
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
            nerveChain.getLogger().error("[Confirm changes]Byzantine transaction verification failed, transaction details: {}", tx.format(ConfirmedChangeVirtualBankTxData.class));
            return false;
        }
        assembleTxService.createConfirmedChangeVirtualBankTx(nerveChain, NulsHash.fromHex(nerveTxHash), hList, hList.get(0).getEffectiveTime());
        return true;
    }
}
