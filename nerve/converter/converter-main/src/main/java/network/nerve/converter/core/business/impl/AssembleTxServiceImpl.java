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

package network.nerve.converter.core.business.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.basic.VarInt;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.core.validator.*;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.message.BroadcastHashSignMessage;
import network.nerve.converter.message.NewTxMessage;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.*;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.model.po.WithdrawalAdditionalFeePO;
import network.nerve.converter.model.txdata.*;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterSignUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.config.ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT;
import static network.nerve.converter.enums.ProposalTypeEnum.REFUND;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;
import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component
public class AssembleTxServiceImpl implements AssembleTxService {

    /**
     * Ordinary transactions are non unlocked transactions：0Unlock amount transaction（Exit consensus, exit delegation）：-1
     */
    private static final byte NORMAL_TX_LOCKED = 0;

    @Autowired
    private HeterogeneousChainManager heterogeneousChainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private network.nerve.converter.core.business.HeterogeneousService HeterogeneousService;
    @Autowired
    private TxStorageService txStorageService;
    @Autowired
    private RechargeVerifier rechargeVerifier;
    @Autowired
    private RechargeUnconfirmedVerifier rechargeUnconfirmedVerifier;
    @Autowired
    private ConfirmedChangeVirtualBankVerifier confirmedChangeVirtualBankVerifier;
    @Autowired
    private ConfirmWithdrawalVerifier confirmWithdrawalVerifier;
    @Autowired
    private HeterogeneousContractAssetRegCompleteVerifier heterogeneousContractAssetRegCompleteVerifier;
    @Autowired
    private ProposalVerifier proposalVerifier;
    @Autowired
    private ConfirmProposalVerifier confirmProposalVerifier;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private ProposalExeStorageService proposalExeStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private BitcoinVerifier bitcoinVerifier;

    @Override
    public Transaction createChangeVirtualBankTx(Chain chain, List<byte[]> inAgentList, List<byte[]> outAgentList, long outHeight, long txTime) throws NulsException {
        Transaction tx = assembleChangeVirtualBankTx(chain, inAgentList, outAgentList, outHeight, txTime);
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction assembleChangeVirtualBankTx(Chain chain, List<byte[]> inAgentList, List<byte[]> outAgentList, long outHeight, long txTime) throws NulsException {
        ChangeVirtualBankTxData txData = new ChangeVirtualBankTxData();
        txData.setInAgents(inAgentList);
        txData.setOutAgents(outAgentList);
        txData.setOutHeight(outHeight);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CHANGE_VIRTUAL_BANK, txDataBytes, txTime);
        ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(ChangeVirtualBankTxData.class));
        return tx;
    }

    @Override
    public Transaction createConfirmedChangeVirtualBankTx(Chain chain, NulsHash changeVirtualBankTxHash, List<HeterogeneousConfirmedVirtualBank> listConfirmed, long txTime) throws NulsException {
        Transaction tx = this.createConfirmedChangeVirtualBankTxWithoutSign(chain, changeVirtualBankTxHash, listConfirmed, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
//        confirmedChangeVirtualBankVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, changeVirtualBankTxHash.toHex());
        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        for (HeterogeneousConfirmedVirtualBank bank : listConfirmed) {
            heterogeneousHashList.add(new HeterogeneousHash(bank.getHeterogeneousChainId(), bank.getHeterogeneousTxHash()));
        }
        message.setHeterogeneousHashList(heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        if (chain.getLatestBasicBlock().getHeight() % 100 == 0) {
            chain.getLogger().info(tx.format(ConfirmedChangeVirtualBankTxData.class));    
        } else {
            chain.getLogger().debug(tx.format(ConfirmedChangeVirtualBankTxData.class));
        }
        return tx;
    }

    @Override
    public Transaction createConfirmedChangeVirtualBankTxWithoutSign(Chain chain, NulsHash changeVirtualBankTxHash, List<HeterogeneousConfirmedVirtualBank> listConfirmed, long txTime) throws NulsException {
        ConfirmedChangeVirtualBankTxData txData = new ConfirmedChangeVirtualBankTxData();
        txData.setChangeVirtualBankTxHash(changeVirtualBankTxHash);
        txData.setListConfirmed(listConfirmed);
        List<byte[]> agentList = new ArrayList<>();
        List<VirtualBankDirector> directorList = new ArrayList<>(chain.getMapVirtualBank().values());
        directorList.sort((o1, o2) -> o2.getAgentAddress().compareTo(o1.getAgentAddress()));
        for (VirtualBankDirector director : directorList) {
            byte[] address = AddressTool.getAddress(director.getAgentAddress());
            agentList.add(address);
        }
        txData.setListAgents(agentList);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        return assembleUnsignTxWithoutCoinData(TxType.CONFIRM_CHANGE_VIRTUAL_BANK, txDataBytes, txTime);
    }

    @Override
    public Transaction createInitializeHeterogeneousTx(Chain chain, int heterogeneousChainId, long txTime) throws NulsException {
        InitializeHeterogeneousTxData txData = new InitializeHeterogeneousTxData();
        txData.setHeterogeneousChainId(heterogeneousChainId);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.INITIALIZE_HETEROGENEOUS, txDataBytes, txTime);
        ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(InitializeHeterogeneousTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createRechargeTx(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException {
        // Support simultaneous transfer intokenandmain
        Transaction tx = this.createRechargeTxWithoutSign(chain, rechargeTxDTO);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Verifier verification
        rechargeVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(rechargeTxDTO.getHeterogeneousChainId(), rechargeTxDTO.getOriginalTxHash()));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(RechargeTxData.class));
        return tx;
    }

    @Override
    public Transaction createRechargeTxWithoutSign(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException {
        RechargeTxData txData = new RechargeTxData(rechargeTxDTO.getOriginalTxHash());
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            txData.setHeterogeneousChainId(rechargeTxDTO.getHeterogeneousChainId());
        }
        if (rechargeTxDTO.getExtend() != null) {
            txData.setExtend(rechargeTxDTO.getExtend());
        }
        List<CoinTo> tos = new ArrayList<>();
        byte[] toAddress = AddressTool.getAddress(rechargeTxDTO.getToAddress());
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(
                rechargeTxDTO.getHeterogeneousChainId(),
                rechargeTxDTO.getHeterogeneousAssetId());
        CoinTo coinTo = new CoinTo(
                toAddress,
                nerveAssetInfo.getAssetChainId(),
                nerveAssetInfo.getAssetId(),
                converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                        rechargeTxDTO.getHeterogeneousChainId(),
                        nerveAssetInfo.getAssetChainId(),
                        nerveAssetInfo.getAssetId(),
                        rechargeTxDTO.getAmount())
        );
        tos.add(coinTo);
        // Simultaneously rechargetokenandmain, increasemainSupport for
        if (rechargeTxDTO.isDepositII()) {
            NerveAssetInfo mainInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(rechargeTxDTO.getHeterogeneousChainId(), 1);
            CoinTo mainTo = new CoinTo(
                    toAddress,
                    mainInfo.getAssetChainId(),
                    mainInfo.getAssetId(),
                    converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                            rechargeTxDTO.getHeterogeneousChainId(),
                            mainInfo.getAssetChainId(),
                            mainInfo.getAssetId(),
                            rechargeTxDTO.getMainAmount())
            );
            tos.add(mainTo);
        }

        CoinData coinData = new CoinData();
        coinData.setTo(tos);
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.RECHARGE, txDataBytes, rechargeTxDTO.getTxtime());
        tx.setCoinData(coinDataBytes);
        if (StringUtils.isNotBlank(rechargeTxDTO.getHeterogeneousFromAddress())) {
            tx.setRemark(rechargeTxDTO.getHeterogeneousFromAddress().getBytes(StandardCharsets.UTF_8));
        }
        return tx;
    }

    @Override
    public Transaction rechargeUnconfirmedTx(Chain chain, RechargeUnconfirmedTxData txData, long txTime) throws NulsException {
        Transaction tx = rechargeUnconfirmedTxWithoutSign(chain, txData, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Call validator validation
        rechargeUnconfirmedVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(txData.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(RechargeUnconfirmedTxData.class));
        return tx;
    }

    private boolean greaterTranZero(BigInteger amount) {
        if (amount != null && amount.compareTo(BigInteger.ZERO) > 0) {
            return true;
        }
        return false;
    }

    @Override
    public Transaction rechargeUnconfirmedTxWithoutSign(Chain chain, RechargeUnconfirmedTxData txData, long txTime) throws NulsException {
        byte[] txDataBytes;
        try {
            txData.setAmount(
                    converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                            txData.getOriginalTxHash().getHeterogeneousChainId(),
                            txData.getAssetChainId(),
                            txData.getAssetId(),
                            txData.getAmount())
            );
            if (greaterTranZero(txData.getMainAssetAmount())) {
                txData.setMainAssetAmount(
                        converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                                txData.getOriginalTxHash().getHeterogeneousChainId(),
                                txData.getMainAssetChainId(),
                                txData.getMainAssetId(),
                                txData.getMainAssetAmount())
                );
            }
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.RECHARGE_UNCONFIRMED, txDataBytes, txTime);
        return tx;
    }

    @Override
    public Transaction oneClickCrossChainUnconfirmedTx(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long txTime) throws NulsException {
        Transaction tx = oneClickCrossChainUnconfirmedTxWithoutSign(chain, txData, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Call validator validation
        rechargeUnconfirmedVerifier.validateOneClickCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(txData.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(OneClickCrossChainUnconfirmedTxData.class));
        return tx;
    }

    @Override
    public Transaction oneClickCrossChainUnconfirmedTxWithoutSign(Chain chain, OneClickCrossChainUnconfirmedTxData txData, long txTime) throws NulsException {
        byte[] txDataBytes;
        try {
            if (greaterTranZero(txData.getErc20Amount())) {
                txData.setErc20Amount(
                        converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                                txData.getOriginalTxHash().getHeterogeneousChainId(),
                                txData.getErc20AssetChainId(),
                                txData.getErc20AssetId(),
                                txData.getErc20Amount())
                );
            }
            if (greaterTranZero(txData.getTipping())) {
                txData.setTipping(
                        converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                                txData.getOriginalTxHash().getHeterogeneousChainId(),
                                txData.getTippingChainId(),
                                txData.getTippingAssetId(),
                                txData.getTipping())
                );
            }
            if (greaterTranZero(txData.getMainAssetFeeAmount())) {
                txData.setMainAssetFeeAmount(
                        converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                                txData.getOriginalTxHash().getHeterogeneousChainId(),
                                txData.getMainAssetChainId(),
                                txData.getMainAssetId(),
                                txData.getMainAssetFeeAmount())
                );
            }
            if (greaterTranZero(txData.getMainAssetAmount())) {
                txData.setMainAssetFeeAmount(
                        converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                                txData.getOriginalTxHash().getHeterogeneousChainId(),
                                txData.getMainAssetChainId(),
                                txData.getMainAssetId(),
                                txData.getMainAssetAmount())
                );
            }
            String desToAddress = ConverterUtil.addressToLowerCase(txData.getDesToAddress());
            txData.setDesToAddress(desToAddress);
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.ONE_CLICK_CROSS_CHAIN_UNCONFIRMED, txDataBytes, txTime);
        return tx;
    }

    @Override
    public Transaction createOneClickCrossChainTx(Chain chain, OneClickCrossChainUnconfirmedTxData dto, long txTime) throws NulsException {
        Transaction tx = this.createOneClickCrossChainTxWithoutSign(chain, dto, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Call validator validation
        rechargeVerifier.validateOneClickCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(dto.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(OneClickCrossChainTxData.class));
        return tx;
    }

    @Override
    public Transaction createOneClickCrossChainTxWithoutSign(Chain chain, OneClickCrossChainUnconfirmedTxData dto, long txTime) throws NulsException {
        String desToAddress = ConverterUtil.addressToLowerCase(dto.getDesToAddress());
        OneClickCrossChainTxData txData = new OneClickCrossChainTxData();
        txData.setDesChainId(dto.getDesChainId());
        txData.setDesToAddress(desToAddress);
        txData.setDesExtend(dto.getDesExtend());
        txData.setOriginalTxHash(dto.getOriginalTxHash());
        txData.setHeterogeneousHeight(dto.getHeterogeneousHeight());
        txData.setHeterogeneousFromAddress(dto.getHeterogeneousFromAddress());

        BigInteger mainAssetAmount = dto.getMainAssetAmount();
        BigInteger mainAssetFeeAmount = dto.getMainAssetFeeAmount();
        if (mainAssetFeeAmount.compareTo(mainAssetAmount) > 0) {
            // Transaction fee error across the target chain
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_FEE_ERROR);
        }
        int withdrawalAssetChainId, withdrawalAssetId;
        BigInteger withdrawalAmount;
        int feeAssetChainId = dto.getMainAssetChainId();
        int feeAssetId = dto.getMainAssetId();
        BigInteger feeAmount;
        if (dto.getErc20AssetChainId() > 0) {
            withdrawalAssetChainId = dto.getErc20AssetChainId();
            withdrawalAssetId = dto.getErc20AssetId();
            withdrawalAmount = dto.getErc20Amount();
            feeAmount = dto.getMainAssetAmount();
        } else {
            withdrawalAssetChainId = dto.getMainAssetChainId();
            withdrawalAssetId = dto.getMainAssetId();
            withdrawalAmount = mainAssetAmount.subtract(mainAssetFeeAmount);
            feeAmount = mainAssetFeeAmount;
        }
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        byte[] withdrawalBlackhole = dto.getNerveToAddress();
        CoinData coinData = new CoinData();
        //assembleto
        List<CoinTo> tos = new ArrayList<>();
        CoinTo withdrawalCoinTo = new CoinTo(
                dto.getNerveToAddress(),
                withdrawalAssetChainId,
                withdrawalAssetId,
                converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                        txData.getOriginalTxHash().getHeterogeneousChainId(),
                        withdrawalAssetChainId,
                        withdrawalAssetId,
                        withdrawalAmount)
        );
        tos.add(withdrawalCoinTo);
        // Determine the temporary storage of subsidy fees for assembling heterogeneous chainsto
        CoinTo withdrawalFeeCoinTo = new CoinTo(
                withdrawalFeeAddress,
                feeAssetChainId,
                feeAssetId,
                converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                        txData.getOriginalTxHash().getHeterogeneousChainId(),
                        feeAssetChainId,
                        feeAssetId,
                        feeAmount)
        );
        tos.add(withdrawalFeeCoinTo);
        // assembletipping
        BigInteger tipping = dto.getTipping();
        if (tipping.compareTo(BigInteger.ZERO) > 0) {
            // LegitimateNerveaddress
            if (!converterCoreApi.validNerveAddress(dto.getTippingAddress())) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address error:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), dto.getTippingAddress(), dto.getOriginalTxHash().getHeterogeneousHash());
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            byte[] tippingAddress = AddressTool.getAddress(dto.getTippingAddress());
            if (Arrays.equals(tippingAddress, withdrawalBlackhole) || Arrays.equals(tippingAddress, withdrawalFeeAddress)) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address setting error:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), dto.getTippingAddress(), dto.getOriginalTxHash().getHeterogeneousHash());
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            // Must not exceed the value of cross chain assets10%
            if (new BigDecimal(withdrawalAmount).multiply(HtgConstant.NUMBER_0_DOT_1).compareTo(new BigDecimal(tipping)) < 0) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping exceed error:{}, crossValue:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), tipping, withdrawalAmount, dto.getOriginalTxHash().getHeterogeneousHash());
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            CoinTo tippingCoinTo = new CoinTo(
                    tippingAddress,
                    dto.getTippingChainId(),
                    dto.getTippingAssetId(),
                    converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                            txData.getOriginalTxHash().getHeterogeneousChainId(),
                            dto.getTippingChainId(),
                            dto.getTippingAssetId(),
                            tipping)
            );
            tos.add(tippingCoinTo);
            withdrawalCoinTo.setAmount(withdrawalCoinTo.getAmount().subtract(tippingCoinTo.getAmount()));
        }
        coinData.setTo(tos);
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.ONE_CLICK_CROSS_CHAIN, txDataBytes, txTime);
        tx.setCoinData(coinDataBytes);
        if (StringUtils.isNotBlank(dto.getHeterogeneousFromAddress())) {
            tx.setRemark(dto.getHeterogeneousFromAddress().getBytes(StandardCharsets.UTF_8));
        }
        return tx;
    }

    @Override
    public Transaction createAddFeeCrossChainTx(Chain chain, AddFeeCrossChainTxDTO dto, long txTime) throws NulsException {
        Transaction tx = this.createAddFeeCrossChainTxWithoutSign(chain, dto, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Call validator validation
        rechargeVerifier.validateAddFeeCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(dto.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(WithdrawalAddFeeByCrossChainTxData.class));
        return tx;
    }

    @Override
    public Transaction createAddFeeCrossChainTxWithoutSign(Chain chain, AddFeeCrossChainTxDTO dto, long txTime) throws NulsException {
        WithdrawalAddFeeByCrossChainTxData txData = new WithdrawalAddFeeByCrossChainTxData();
        txData.setHtgTxHash(dto.getOriginalTxHash());
        txData.setNerveTxHash(dto.getNerveTxHash());
        txData.setSubExtend(dto.getSubExtend());
        txData.setHeterogeneousHeight(dto.getHeterogeneousHeight());
        txData.setHeterogeneousFromAddress(dto.getHeterogeneousFromAddress());

        int feeAssetChainId = dto.getMainAssetChainId();
        int feeAssetId = dto.getMainAssetId();
        BigInteger feeAmount = dto.getMainAssetAmount();
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        if (!Arrays.equals(dto.getNerveToAddress(), withdrawalFeeAddress)) {
            chain.getLogger().error("[{}]AddFeeCrossChain address setting error:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), AddressTool.getStringAddressByBytes(dto.getNerveToAddress()), dto.getOriginalTxHash().getHeterogeneousHash());
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
        }
        CoinData coinData = new CoinData();
        //assembleto
        List<CoinTo> tos = new ArrayList<>();
        CoinTo withdrawalFeeCoinTo = new CoinTo(
                withdrawalFeeAddress,
                feeAssetChainId,
                feeAssetId,
                converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                        dto.getOriginalTxHash().getHeterogeneousChainId(),
                        feeAssetChainId,
                        feeAssetId,
                        feeAmount)
        );
        tos.add(withdrawalFeeCoinTo);
        coinData.setTo(tos);
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN, txDataBytes, txTime);
        tx.setCoinData(coinDataBytes);
        if (StringUtils.isNotBlank(dto.getHeterogeneousFromAddress())) {
            tx.setRemark(dto.getHeterogeneousFromAddress().getBytes(StandardCharsets.UTF_8));
        }
        return tx;
    }

    /**
     * Transaction verification successful, save and broadcast signature,Then wait for processing
     *
     * @param chain
     * @param tx
     */
    private void saveWaitingProcess(Chain chain, Transaction tx) {
        // Save intxStorageService
        TransactionPO po = txStorageService.get(chain, tx.getHash());
        if (po == null) {
            txStorageService.save(chain, new TransactionPO(tx));
        }
        // If the signature set to be processed There is a signature list for this transaction in the Then take it out and put it in the processing queue
        List<UntreatedMessage> listMsg = chain.getFutureMessageMap().get(tx.getHash());
        if (null != listMsg) {
            for (UntreatedMessage msg : listMsg) {
                chain.getSignMessageByzantineQueue().offer(msg);
            }
            // Clear cached signatures
            chain.getFutureMessageMap().remove(tx.getHash());
        }
    }

    @Override
    public Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(withdrawalTxDTO.getHeterogeneousChainId(), withdrawalTxDTO.getAssetChainId(), withdrawalTxDTO.getAssetId());

        String heterogeneousAddress = addressToLowerCase(withdrawalTxDTO.getHeterogeneousAddress());
        if (null == heterogeneousAssetInfo ||
                null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousAssetInfo.getChainId())) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        if (StringUtils.isBlank(heterogeneousAddress)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
        }
        WithdrawalTxData txData = new WithdrawalTxData(heterogeneousAddress);
        if (LATEST_BLOCK_HEIGHT >= WITHDRAWAL_RECHARGE_CHAIN_HEIGHT) {
            txData.setHeterogeneousChainId(withdrawalTxDTO.getHeterogeneousChainId());
        }
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL, txDataBytes, withdrawalTxDTO.getRemark());
        byte[] coinData = assembleWithdrawalCoinData(chain, withdrawalTxDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction withdrawalAdditionalFeeTx(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        if (converterCoreApi.isProtocol35()) {
            return withdrawalAdditionalFeeTxV35(chain, withdrawalAdditionalFeeTxDTO);
        } else if (converterCoreApi.isProtocol21()) {
            return withdrawalAdditionalFeeTxV21(chain, withdrawalAdditionalFeeTxDTO);
        } else if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
            return withdrawalAdditionalFeeTxV15(chain, withdrawalAdditionalFeeTxDTO);
        } else if (converterCoreApi.isSupportProtocol13NewValidationOfERC20()) {
            return withdrawalAdditionalFeeTxV13(chain, withdrawalAdditionalFeeTxDTO);
        } else {
            return withdrawalAdditionalFeeTxV0(chain, withdrawalAdditionalFeeTxDTO);
        }
    }

    @Override
    public Transaction withdrawalHeterogeneousSendTx(Chain chain, WithdrawalHeterogeneousSendTxData txData, long txTime) throws NulsException {
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_HETEROGENEOUS_SEND, txDataBytes, txTime);
        ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(WithdrawalHeterogeneousSendTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createConfirmWithdrawalTx(Chain chain, ConfirmWithdrawalTxData confirmWithdrawalTxData, long txTime, byte[] remark) throws NulsException {
        Transaction tx = this.createConfirmWithdrawalTxWithoutSign(chain, confirmWithdrawalTxData, txTime, remark);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        confirmWithdrawalVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(confirmWithdrawalTxData.getHeterogeneousChainId(), confirmWithdrawalTxData.getHeterogeneousTxHash()));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, confirmWithdrawalTxData.getWithdrawalTxHash().toHex(), heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);

        chain.getLogger().debug(tx.format(ConfirmWithdrawalTxData.class));
        return tx;
    }

    @Override
    public Transaction createConfirmWithdrawalTxWithoutSign(Chain chain, ConfirmWithdrawalTxData confirmWithdrawalTxData, long txTime, byte[] remark) throws NulsException {
        byte[] txDataBytes = null;
        try {
            txDataBytes = confirmWithdrawalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_WITHDRAWAL, txDataBytes, txTime);
        tx.setRemark(remark);
        return tx;
    }

    @Override
    public Transaction processProposalTx(Chain chain, Transaction tx) throws NulsException {
        return processProposalTx(chain, tx, null, null);
    }

    public Transaction processProposalTx(Chain chain, Transaction tx, Integer heterogeneousChainId, String heterogeneousTxHash) throws NulsException {
        proposalVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);
        //Broadcast complete transactions
        NewTxMessage newTxMessage = new NewTxMessage();
        newTxMessage.setTx(tx);
        NetWorkCall.broadcast(chain, newTxMessage, ConverterCmdConstant.NEW_TX_MESSAGE);

        if (VirtualBankUtil.isCurrentDirector(chain)) {
            if (null == heterogeneousChainId || StringUtils.isBlank(heterogeneousTxHash)) {
                ProposalTxData txdata = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
                heterogeneousChainId = txdata.getHeterogeneousChainId();
                heterogeneousTxHash = txdata.getHeterogeneousTxHash();
            }
            // If it is currently a virtual banking node, Start processing directly.
            List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
            heterogeneousHashList.add(new HeterogeneousHash(heterogeneousChainId, heterogeneousTxHash));
            NulsHash txHash = tx.getHash();
            P2PHKSignature p2PHKSignature = ConverterSignUtil.getSignatureByDirector(chain, tx);
            BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
            NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);

            UntreatedMessage untreatedMessage = new UntreatedMessage(chain.getChainId(), null, message, txHash);
            chain.getSignMessageByzantineQueue().offer(untreatedMessage);
        }
        chain.getLogger().debug(tx.format(ProposalTxData.class));
        return tx;
    }

    @Override
    public Transaction createProposalTx(Chain chain, ProposalTxDTO proposalTxDTO) throws NulsException {
        if (null == ProposalTypeEnum.getEnum(proposalTxDTO.getType())) {
            //enumeration
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        ProposalTxData txData = new ProposalTxData();
        txData.setType(proposalTxDTO.getType());
        if (ProposalTypeEnum.getEnum(proposalTxDTO.getType()) == ProposalTypeEnum.OTHER) {
            txData.setVoteRangeType(proposalTxDTO.getVoteRangeType());
        } else {
            txData.setVoteRangeType(ProposalVoteRangeTypeEnum.BANK.value());
        }
        txData.setContent(proposalTxDTO.getContent());
        txData.setHeterogeneousChainId(proposalTxDTO.getHeterogeneousChainId());
        String heterogeneousTxHash = proposalTxDTO.getHeterogeneousTxHash();
        if (StringUtils.isNotBlank(heterogeneousTxHash)) {
            txData.setHeterogeneousTxHash(heterogeneousTxHash.toLowerCase());
        }
        String businessAddress = proposalTxDTO.getBusinessAddress();
        if (StringUtils.isNotBlank(businessAddress)) {
            // Compatible with non Ethernet addresses update by pierre at 2021/11/16
            if (ProposalTypeEnum.getEnum(proposalTxDTO.getType()) == ProposalTypeEnum.UPGRADE) {
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(proposalTxDTO.getHeterogeneousChainId());
                txData.setAddress(docking.getAddressBytes(businessAddress));
            } else {
                if (Numeric.containsHexPrefix(businessAddress)) {
                    txData.setAddress(Numeric.hexStringToByteArray(businessAddress));
                } else {
                    txData.setAddress(AddressTool.getAddress(proposalTxDTO.getBusinessAddress()));
                }
            }
        }

        String hash = proposalTxDTO.getHash();
        if (StringUtils.isNotBlank(hash)) {
            txData.setHash(HexUtil.decode(proposalTxDTO.getHash()));
        }

        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.PROPOSAL, txDataBytes, proposalTxDTO.getRemark());
        byte[] coinData = assembleProposalFeeCoinData(chain, tx.size(), proposalTxDTO.getSignAccountDTO(), ConverterContext.PROPOSAL_PRICE);
        tx.setCoinData(coinData);
        ConverterSignUtil.signTx(chain.getChainId(), proposalTxDTO.getSignAccountDTO(), tx);
        return processProposalTx(chain, tx, proposalTxDTO.getHeterogeneousChainId(), proposalTxDTO.getHeterogeneousTxHash());
    }

    @Override
    public Transaction createVoteProposalTx(Chain chain, NulsHash proposalTxHash, byte choice, String remark, SignAccountDTO signAccount) throws NulsException {
        if (null == proposalTxHash) {
            throw new NulsException(ConverterErrorCode.PROPOSAL_TX_HASH_NULL);
        }
        if (null == ProposalVoteChoiceEnum.getEnum(choice)) {
            //enumeration
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_INVALID);
        }
        VoteProposalTxData voteProposalTxData = new VoteProposalTxData(proposalTxHash, choice);
        byte[] txDataBytes = null;
        try {
            txDataBytes = voteProposalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithFee(chain, TxType.VOTE_PROPOSAL, txDataBytes, remark, signAccount);
        ConverterSignUtil.signTx(chain.getChainId(), signAccount, tx);
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(VoteProposalTxData.class));
        return tx;
    }


    @Override
    public Transaction createConfirmProposalTx(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException {
        Transaction tx = this.createConfirmProposalTxWithoutSign(chain, confirmProposalTxData, txTime);
        // Byzantine transaction process
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        confirmProposalVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);
        byte proposalType = confirmProposalTxData.getType();
        BroadcastHashSignMessage message;
        if (ProposalTypeEnum.REFUND.value() == proposalType
                || ProposalTypeEnum.EXPELLED.value() == proposalType
                || ProposalTypeEnum.WITHDRAW.value() == proposalType) {
            List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
            ProposalExeBusinessData business = ConverterUtil.getInstance(
                    confirmProposalTxData.getBusinessData(),
                    ProposalExeBusinessData.class);
            heterogeneousHashList.add(new HeterogeneousHash(business.getHeterogeneousChainId(), business.getHeterogeneousTxHash()));
            message = new BroadcastHashSignMessage(tx, p2PHKSignature, business.getProposalTxHash().toHex(), heterogeneousHashList);
        } else if (ProposalTypeEnum.UPGRADE.value() == proposalType) {
            List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
            ConfirmUpgradeTxData business = ConverterUtil.getInstance(
                    confirmProposalTxData.getBusinessData(),
                    ConfirmUpgradeTxData.class);
            heterogeneousHashList.add(new HeterogeneousHash(business.getHeterogeneousChainId(), business.getHeterogeneousTxHash()));
            message = new BroadcastHashSignMessage(tx, p2PHKSignature, business.getNerveTxHash().toHex(), heterogeneousHashList);
        } else {
            ProposalExeBusinessData business = ConverterUtil.getInstance(
                    confirmProposalTxData.getBusinessData(),
                    ProposalExeBusinessData.class);
            message = new BroadcastHashSignMessage(tx, p2PHKSignature, business.getProposalTxHash().toHex());
        }
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        chain.getLogger().debug(tx.format(ConfirmProposalTxData.class));
        return tx;
    }

    @Override
    public Transaction createConfirmProposalTxWithoutSign(Chain chain, ConfirmProposalTxData confirmProposalTxData, long txTime) throws NulsException {
        ProposalTypeEnum typeEnum = ProposalTypeEnum.getEnum(confirmProposalTxData.getType());
        if (null == typeEnum) {
            //enumeration
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        byte[] txDataBytes;
        try {
            txDataBytes = confirmProposalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_PROPOSAL, txDataBytes, txTime);
        return tx;
    }

    @Override
    public Transaction createDistributionFeeTx(Chain chain, NulsHash basisTxHash, List<byte[]> listRewardAddress, long txTime, boolean isProposal) throws NulsException {
        DistributionFeeTxData distributionFeeTxData = new DistributionFeeTxData();
        distributionFeeTxData.setBasisTxHash(basisTxHash);
        byte[] txData = null;
        try {
            txData = distributionFeeTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.DISTRIBUTION_FEE, txData, txTime);
        byte[] coinData = assembleDistributionFeeCoinData(chain, basisTxHash, listRewardAddress, isProposal);
        tx.setCoinData(coinData);
        ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(DistributionFeeTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }


    @Override
    public Transaction createHeterogeneousContractAssetRegPendingTx(Chain chain, String from, String password,
                                                                    int heterogeneousChainId, int decimals, String symbol,
                                                                    String contractAddress, String remark) throws NulsException {
        if (null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        HeterogeneousContractAssetRegPendingTxData txData = new HeterogeneousContractAssetRegPendingTxData();
        txData.setChainId(heterogeneousChainId);
        txData.setDecimals((byte) decimals);
        txData.setSymbol(symbol);
        txData.setContractAddress(addressToLowerCase(contractAddress));
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setRemark(StringUtils.isBlank(remark) ? null : StringUtils.bytes(remark));

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleFeeCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //broadcast
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(HeterogeneousContractAssetRegPendingTxData.class));
        return tx;
    }

    @Override
    public Transaction createHeterogeneousMainAssetRegTx(Chain chain, String from, String password, int heterogeneousChainId, String remark) throws NulsException {
        if (null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        HeterogeneousMainAssetRegTxData txData = new HeterogeneousMainAssetRegTxData();
        txData.setChainId(heterogeneousChainId);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.HETEROGENEOUS_MAIN_ASSET_REG);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setRemark(StringUtils.isBlank(remark) ? null : StringUtils.bytes(remark));

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleFeeCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //broadcast
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(HeterogeneousMainAssetRegTxData.class));
        return tx;
    }

    @Override
    public Transaction createHeterogeneousMainAssetBindTx(Chain chain, String from, String password, int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId, String remark) throws NulsException {
        if (null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        HeterogeneousMainAssetBindTxData txData = new HeterogeneousMainAssetBindTxData();
        txData.setChainId(heterogeneousChainId);
        txData.setNerveAssetChainId(nerveAssetChainId);
        txData.setNerveAssetId(nerveAssetId);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.HETEROGENEOUS_MAIN_ASSET_BIND);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setRemark(StringUtils.isBlank(remark) ? null : StringUtils.bytes(remark));

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleFeeCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //broadcast
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(HeterogeneousMainAssetBindTxData.class));
        return tx;
    }

    @Override
    public Transaction createHeterogeneousContractAssetRegCompleteTx(Chain chain, Transaction pendingTx) throws NulsException {
        if (pendingTx == null || pendingTx.getType() != TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING) {
            chain.getLogger().error("Transaction information is empty or the type is incorrect");
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        HeterogeneousContractAssetRegPendingTxData pendingTxData = new HeterogeneousContractAssetRegPendingTxData();
        pendingTxData.parse(pendingTx.getTxData(), 0);

        HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
        txData.setPendingHash(pendingTx.getHash());
        txData.setChainId(pendingTxData.getChainId());
        txData.setDecimals(pendingTxData.getDecimals());
        txData.setSymbol(pendingTxData.getSymbol());
        txData.setContractAddress(pendingTxData.getContractAddress());
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_COMPLETE);
        tx.setTxData(txDataBytes);
        tx.setTime(pendingTx.getTime());
        tx.setRemark(pendingTx.getRemark());
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);

        // Verifier verification
        heterogeneousContractAssetRegCompleteVerifier.validate(chain.getChainId(), tx);
        saveWaitingProcess(chain, tx);
        BroadcastHashSignMessage message = new BroadcastHashSignMessage();
        message.setHash(tx.getHash());
        message.setP2PHKSignature(p2PHKSignature);
        message.setType(tx.getType());
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);

        // complete
        if (chain.getLogger().isDebugEnabled()) {
            chain.getLogger().debug(tx.format(HeterogeneousContractAssetRegCompleteTxData.class));
        }
        return tx;
    }

    @Override
    public Transaction createResetVirtualBankTx(Chain chain, int heterogeneousChainId, SignAccountDTO signAccount) throws NulsException {
        ResetVirtualBankTxData txData = new ResetVirtualBankTxData();
        txData.setHeterogeneousChainId(heterogeneousChainId);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.RESET_HETEROGENEOUS_VIRTUAL_BANK, txDataBytes);
        ConverterSignUtil.signTx(chain.getChainId(), signAccount, tx);
        chain.getLogger().debug(tx.format(ResetVirtualBankTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createConfirmResetVirtualBankTx(Chain chain, ConfirmResetVirtualBankTxData txData, long txTime) throws NulsException {
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_HETEROGENEOUS_RESET_VIRTUAL_BANK, txDataBytes, txTime);
        ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(ConfirmResetVirtualBankTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * Assembly withdrawal transactionCoinData
     *
     * @param chain
     * @param withdrawalTxDTO
     * @return
     * @throws NulsException
     */
    private byte[] assembleWithdrawalCoinData(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException {
        if (BigIntegerUtils.isLessThan(withdrawalTxDTO.getDistributionFee(), TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES)) {
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
        }
        int withdrawalAssetId = withdrawalTxDTO.getAssetId();
        int withdrawalAssetChainId = withdrawalTxDTO.getAssetChainId();

        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        boolean isNvtFeeCoin = chain.getChainId() == withdrawalTxDTO.getFeeChainId();
        int feeAssetChainId, feeAssetId;
        if (isNvtFeeCoin) {
            feeAssetChainId = chain.getConfig().getChainId();
            feeAssetId = chain.getConfig().getAssetId();
        } else {
            NerveAssetInfo htgMainAsset = converterCoreApi.getHtgMainAsset(withdrawalTxDTO.getFeeChainId());
            if (htgMainAsset.isEmpty()) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
            }
            feeAssetChainId = htgMainAsset.getAssetChainId();
            feeAssetId = htgMainAsset.getAssetId();
        }

        BigInteger amount = withdrawalTxDTO.getAmount();
        String address = withdrawalTxDTO.getSignAccount().getAddress();
        //Withdrawal of assetsfrom
        List<CoinFrom> listFrom = new ArrayList<>();
        if (withdrawalAssetChainId == feeAssetChainId && withdrawalAssetId == feeAssetId) {
            // When the withdrawal assets and handling fee assets are consistent, they are merged into one itemfrom
            CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, withdrawalAssetChainId, withdrawalAssetId, withdrawalTxDTO.getDistributionFee());
            listFrom.add(withdrawalCoinFrom);
        } else {
            CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, withdrawalAssetChainId, withdrawalAssetId, BigInteger.ZERO);
            listFrom.add(withdrawalCoinFrom);
            // As long as it is not the current chain master asset All require additional assemblycoinFrom
            CoinFrom withdrawalFeeCoinFrom;
            //Handling feesfrom Including heterogeneous chain subsidy handling fees
            withdrawalFeeCoinFrom = getWithdrawalFeeCoinFrom(chain, address, withdrawalTxDTO.getDistributionFee(), feeAssetChainId, feeAssetId);

            listFrom.add(withdrawalFeeCoinFrom);
        }
        //assembleto
        List<CoinTo> listTo = new ArrayList<>();
        CoinTo withdrawalCoinTo = new CoinTo(
                AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId()),
                withdrawalAssetChainId,
                withdrawalAssetId,
                amount);

        listTo.add(withdrawalCoinTo);
        // Determine the temporary storage of subsidy fees for assembling heterogeneous chainsto
        CoinTo withdrawalFeeCoinTo = new CoinTo(
                AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId()),
                feeAssetChainId,
                feeAssetId,
                withdrawalTxDTO.getDistributionFee());
        listTo.add(withdrawalFeeCoinTo);
        CoinData coinData = new CoinData(listFrom, listTo);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }


    /**
     * Assembling withdrawal assetsCoinFrom
     *
     * @param chain
     * @param address
     * @param amount
     * @param withdrawalAssetChainId
     * @param withdrawalAssetId
     * @return
     * @throws NulsException
     */
    private CoinFrom getWithdrawalCoinFrom(
            Chain chain,
            String address,
            BigInteger amount,
            int withdrawalAssetChainId,
            int withdrawalAssetId,
            BigInteger withdrawalHeterogeneousFee) throws NulsException {
        //Withdrawal of assets
        if (BigIntegerUtils.isEqualOrLessThan(amount, BigInteger.ZERO)) {
            chain.getLogger().error("The withdrawal amount cannot be less than0, amount:{}", amount);
            throw new NulsException(ConverterErrorCode.PARAMETER_ERROR);
        }
        NonceBalance withdrawalNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                withdrawalAssetChainId,
                withdrawalAssetId,
                address);
        BigInteger withdrawalAssetBalance = withdrawalNonceBalance.getAvailable();
        amount = withdrawalHeterogeneousFee.add(amount);
        if (BigIntegerUtils.isLessThan(withdrawalAssetBalance, amount)) {
            chain.getLogger().error("Insufficient balance of withdrawn assets chainId:{}, assetId:{}, withdrawal amount:{}, available balance:{} ",
                    withdrawalAssetChainId, withdrawalAssetId, amount, withdrawalAssetBalance);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }

        return new CoinFrom(
                AddressTool.getAddress(address),
                withdrawalAssetChainId,
                withdrawalAssetId,
                amount,
                withdrawalNonceBalance.getNonce(),
                (byte) 0);
    }

    /**
     * Assembly withdrawal transaction fees(Including in chain packaging fees, Heterogeneous chain subsidy handling fee)
     *
     * @param chain
     * @param address
     * @param withdrawalHeterogeneousFee
     * @return
     * @throws NulsException
     */
    private CoinFrom getWithdrawalFeeCoinFrom(Chain chain, String address, BigInteger withdrawalHeterogeneousFee, int feeAssetChainId, int feeAssetId) throws NulsException {
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                feeAssetChainId,
                feeAssetId,
                address);
        // Balance of assets in this chain
        BigInteger balance = currentChainNonceBalance.getAvailable();

        // Total handling fee = Heterogeneous chain transfer(Or signature)Handling fees[All settle with the main assets in the chain]
        BigInteger totalFee = withdrawalHeterogeneousFee;
        if (BigIntegerUtils.isLessThan(balance, totalFee)) {
            chain.getLogger().error("Insufficient balance of withdrawal fee. amount to be paid:{}, available balance:{} ", totalFee, balance);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // Query ledger to obtainnoncevalue
        byte[] nonce = currentChainNonceBalance.getNonce();
        return new CoinFrom(AddressTool.getAddress(address), feeAssetChainId, feeAssetId, totalFee, nonce, (byte) 0);
    }

    /**
     * Assembly withdrawal transaction packaging fee(Only including in chain packaging fees)
     *
     * @param chain
     * @param address
     * @return
     * @throws NulsException
     */
    private CoinFrom getWithdrawalFeeCoinFrom(Chain chain, String address) throws NulsException {
        int chainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                chainId,
                assetId,
                address);
        // Balance of assets in this chain
        BigInteger balance = currentChainNonceBalance.getAvailable();
        //Packaging handling fee
        if (BigIntegerUtils.isLessThan(balance, TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES)) {
            chain.getLogger().error("The balance is insufficient to cover the package fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // Query ledger to obtainnoncevalue
        byte[] nonce = currentChainNonceBalance.getNonce();

        return new CoinFrom(
                AddressTool.getAddress(address),
                chainId,
                assetId,
                TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES,
                nonce,
                (byte) 0);
    }


    /**
     * Assembly subsidy handling fee transactionCoinData
     *
     * @param chain
     * @param listRewardAddress
     * @return
     * @throws NulsException
     */
    private byte[] assembleDistributionFeeCoinData(Chain chain, NulsHash basisTxHash, List<byte[]> listRewardAddress, boolean isProposal) throws NulsException {
        byte[] feeFromAdddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        // If it is a proposal basisTxHash It is to confirm the proposed transactionhash, Instead of proposing transactionshash
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, basisTxHash.toHex());

        if (isProposal) {
            // If it is a proposal Subsidy handling fee, Need to obtain proposal transaction
            try {
                ConfirmProposalTxData txData = ConverterUtil.getInstance(basicTx.getTxData(), ConfirmProposalTxData.class);
                if (txData.getType() == REFUND.value()) {
                    ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
                    basicTx = TransactionCall.getConfirmedTx(chain, businessData.getProposalTxHash().toHex());
                }
            } catch (NulsException e) {
                chain.getLogger().error(ConverterErrorCode.DESERIALIZE_ERROR.getMsg());
                throw new NulsException(ConverterErrorCode.DESERIALIZE_ERROR);
            }
        }
        // The total amount of handling fees to be subsidized
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        WithdrawalTotalFeeInfo distributionFee = calculateFee(chain, basicTx, isProposal);
        List<CoinFrom> listFrom = assembleDistributionFeeCoinFrom(chain, distributionFee, feeFromAdddress);
        List<CoinTo> listTo = assembleDistributionFeeCoinTo(chain, distributionFee, listRewardAddress);
        CoinData coinData = new CoinData(listFrom, listTo);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    /**
     * Assembly subsidy handling fee transactionCoinFrom
     *
     * @param chain
     * @param feeFromAdddress
     * @return
     * @throws NulsException
     */
    private List<CoinFrom> assembleDistributionFeeCoinFrom(Chain chain, WithdrawalTotalFeeInfo distributionFee, byte[] feeFromAdddress) throws NulsException {
        int assetChainId, assetId;
        if (distributionFee.isNvtAsset()) {
            assetChainId = chain.getConfig().getChainId();
            assetId = chain.getConfig().getAssetId();
        } else {
            Coin feeCoin = distributionFee.getFeeCoin();
            assetChainId = feeCoin.getAssetsChainId();
            assetId = feeCoin.getAssetsId();
        }
        //Check if the balance of the temporary storage address for handling fees is sufficient
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                AddressTool.getStringAddressByBytes(feeFromAdddress));
        // Query ledger to obtainnoncevalue
        byte[] nonce = currentChainNonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(feeFromAdddress,
                assetChainId,
                assetId,
                distributionFee.getFee(),
                nonce,
                (byte) 0);
        List<CoinFrom> listFrom = new ArrayList<>();
        listFrom.add(coinFrom);
        return listFrom;
    }

    /**
     * Assembly subsidy handling fee transactionCoinTo
     *
     * @param chain
     * @param listRewardAddress
     * @return
     * @throws NulsException
     */
    private List<CoinTo> assembleDistributionFeeCoinTo(Chain chain, WithdrawalTotalFeeInfo distributionFeeInfo, List<byte[]> listRewardAddress) throws NulsException {
        int assetChainId, assetId;
        if (distributionFeeInfo.isNvtAsset()) {
            assetChainId = chain.getConfig().getChainId();
            assetId = chain.getConfig().getAssetId();
        } else {
            Coin feeCoin = distributionFeeInfo.getFeeCoin();
            assetChainId = feeCoin.getAssetsChainId();
            assetId = feeCoin.getAssetsId();
        }
        // calculate How much commission is subsidized for each node
        BigInteger distributionFee = distributionFeeInfo.getFee();
        BigInteger count = BigInteger.valueOf(listRewardAddress.size());
        BigInteger amount = distributionFee.divide(count);
        Map<String, BigInteger> map = calculateDistributionFeeCoinToAmount(listRewardAddress, amount);
        // assemblecointo
        List<CoinTo> listTo = new ArrayList<>();
        for (Map.Entry<String, BigInteger> entry : map.entrySet()) {
            CoinTo distributionFeeCoinTo = new CoinTo(
                    AddressTool.getAddress(entry.getKey()),
                    assetChainId,
                    assetId,
                    entry.getValue());
            listTo.add(distributionFeeCoinTo);
        }
        return listTo;
    }

    @Override
    public Map<String, BigInteger> calculateDistributionFeeCoinToAmount(List<byte[]> listRewardAddress, BigInteger amount) {
        Map<String, BigInteger> map = new HashMap<>();
        for (byte[] address : listRewardAddress) {
            String addr = AddressTool.getStringAddressByBytes(address);
            map.computeIfPresent(addr, (k, v) -> v.add(amount));
            map.putIfAbsent(addr, amount);
        }
        return map;
    }

    /**
     * Assembly not includedCoinDataTransaction
     *
     * @param txData
     * @return
     * @throws NulsException
     */
    private Transaction assembleUnsignTxWithoutCoinData(int type, byte[] txData, Long txTime, String remark) throws NulsException {
        Transaction tx = new Transaction(type);
        tx.setTxData(txData);
        tx.setTime(null == txTime ? NulsDateUtils.getCurrentTimeSeconds() : txTime);
        tx.setRemark(StringUtils.isBlank(remark) ? null : StringUtils.bytes(remark));
        return tx;
    }

    private Transaction assembleUnsignTxWithoutCoinData(int type, byte[] txData, long txTime) throws NulsException {
        return assembleUnsignTxWithoutCoinData(type, txData, txTime, null);
    }

    private Transaction assembleUnsignTxWithoutCoinData(int type, byte[] txData, String remark) throws NulsException {
        return assembleUnsignTxWithoutCoinData(type, txData, null, remark);
    }

    private Transaction assembleUnsignTxWithoutCoinData(int type, byte[] txData) throws NulsException {
        return assembleUnsignTxWithoutCoinData(type, txData, null, null);
    }

    /**
     * Assembly transactions,CoinDataOnly including handling fees
     *
     * @param chain
     * @param type
     * @param txData
     * @param remark
     * @param signAccountDTO
     * @return
     * @throws NulsException
     */
    private Transaction assembleUnsignTxWithFee(Chain chain, int type, byte[] txData, String remark, SignAccountDTO signAccountDTO) throws NulsException {
        Transaction tx = assembleUnsignTxWithoutCoinData(type, txData, remark);
        tx.setCoinData(assembleFeeCoinData(chain, signAccountDTO));
        return tx;
    }

    /**
     * Assembly handling fee（CoinData）
     *
     * @param chain
     * @param signAccountDTO
     * @return
     * @throws NulsException
     */
    private byte[] assembleFeeCoinData(Chain chain, SignAccountDTO signAccountDTO) throws NulsException {
        return assembleFeeCoinData(chain, signAccountDTO, null);
    }

    /**
     * Assembly handling fee（CoinData）
     *
     * @param chain
     * @param signAccountDTO
     * @param extraFee       Address for collecting public service fees Pay additional business expenses(For example, proposal fees, etc), Compensation for subsequent expenses
     * @return
     * @throws NulsException
     */
    private byte[] assembleFeeCoinData(Chain chain, SignAccountDTO signAccountDTO, BigInteger extraFee) throws NulsException {
        String address = signAccountDTO.getAddress();
        //The transfer transaction transfer address must be a local chain address
        if (!AddressTool.validAddress(chain.getChainId(), address)) {
            throw new NulsException(ConverterErrorCode.IS_NOT_CURRENT_CHAIN_ADDRESS);
        }

        int assetChainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        NonceBalance nonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                address);
        BigInteger balance = nonceBalance.getAvailable();
        BigInteger amount = TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES;
        if (greaterTranZero(extraFee)) {
            amount = amount.add(extraFee);
        }
        if (BigIntegerUtils.isLessThan(balance, amount)) {
            chain.getLogger().error("The balance is insufficient to cover the package fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        //Query ledger to obtainnoncevalue
        byte[] nonce = nonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                assetChainId,
                assetId,
                amount,
                nonce,
                NORMAL_TX_LOCKED);
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);

        List<CoinTo> tos = new ArrayList<>();
        if (greaterTranZero(extraFee)) {
            // additional costs If there is any
            if (BigIntegerUtils.isLessThan(balance, TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES.add(extraFee))) {
                chain.getLogger().error("The balance is insufficient to cover the package fee and the extra fee");
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
            }
            CoinTo extraFeeCoinTo = new CoinTo(
                    AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId()),
                    assetChainId,
                    assetId,
                    extraFee);
            tos.add(extraFeeCoinTo);
        } else {
            // If there is no additional cost to assemble it intocointo ,Then it is necessary to assemble a product with an amount of0ofcoinTo, coinToThe amount is0, fromThe amount becomes a handling fee
            CoinTo coinTo = new CoinTo(
                    AddressTool.getAddress(address),
                    assetChainId,
                    assetId,
                    BigInteger.ZERO);
            tos.add(coinTo);
        }

        coinData.setFrom(froms);
        coinData.setTo(tos);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }


    private byte[] assembleProposalFeeCoinData(Chain chain, int size, SignAccountDTO signAccountDTO, BigInteger extraFee) throws NulsException {
        String address = signAccountDTO.getAddress();
        //The transfer transaction transfer address must be a local chain address
        if (!AddressTool.validAddress(chain.getChainId(), address)) {
            throw new NulsException(ConverterErrorCode.IS_NOT_CURRENT_CHAIN_ADDRESS);
        }
        int assetChainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        NonceBalance nonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                address);
        BigInteger balance = nonceBalance.getAvailable();
        int totalSize = size + 69 + 68 + VirtualBankUtil.getByzantineCount(chain) * 110;
        BigInteger amount = TransactionFeeCalculator.getNormalTxFee(totalSize);
        amount = amount.add(extraFee);
        if (BigIntegerUtils.isLessThan(balance, amount)) {
            chain.getLogger().error("The balance is insufficient to cover the package fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        //Query ledger to obtainnoncevalue
        byte[] nonce = nonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                assetChainId,
                assetId,
                amount,
                nonce,
                NORMAL_TX_LOCKED);

        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);

        List<CoinTo> tos = new ArrayList<>();
        if (BigIntegerUtils.isLessThan(balance, TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES.add(extraFee))) {
            chain.getLogger().error("The balance is insufficient to cover the package fee and the extra fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        CoinTo extraFeeCoinTo = new CoinTo(
                AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId()),
                assetChainId,
                assetId,
                extraFee);
        tos.add(extraFeeCoinTo);

        coinData.setFrom(froms);
        coinData.setTo(tos);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    @Override
    public WithdrawalTotalFeeInfo calculateFee(Chain chain, Long height, Transaction basicTx, boolean isProposal) throws NulsException {
        WithdrawalTotalFeeInfo fee;
        if (null == height) {
            height = chain.getLatestBasicBlock().getHeight();
        }
        if (!isProposal) {
            if (height >= ConverterContext.FEE_ADDITIONAL_HEIGHT) {
                // The handling fee is dynamic, Payment for withdrawal transactions+Additional
                fee = calculateWithdrawalTotalFee(chain, basicTx);
            } else if (height >= ConverterContext.FEE_EFFECTIVE_HEIGHT_SECOND) {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_10);
            } else if (height >= ConverterContext.FEE_EFFECTIVE_HEIGHT_FIRST) {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_100);
            } else {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_10);
            }
        } else {
            // If the proposal is returned in its original way,Need to obtain the additional amount of handling fee for it
            if (height >= ConverterContext.FEE_ADDITIONAL_HEIGHT) {
                ProposalPO proposalPO = proposalStorageService.find(chain, basicTx.getHash());
                if (null != proposalPO && proposalPO.getType() == REFUND.value()) {
                    return new WithdrawalTotalFeeInfo(calculateRefundTotalFee(chain, basicTx.getHash().toHex()));
                }
            }
            fee = new WithdrawalTotalFeeInfo(ConverterContext.PROPOSAL_PRICE);
        }
        return fee;
    }


    @Override
    public WithdrawalTotalFeeInfo calculateFee(Chain chain, Transaction basisTx, boolean isProposal) throws NulsException {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        return calculateFee(chain, null, basisTx, isProposal);
    }

    @Override
    public BigInteger calculateRefundTotalFee(Chain chain, String hash) {
        BigInteger feeTo = ConverterContext.PROPOSAL_PRICE;
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, hash);
        if (null != po && null != po.getMapAdditionalFee()) {
            for (BigInteger fee : po.getMapAdditionalFee().values()) {
                feeTo = feeTo.add(fee);
            }
        }
        return feeTo;
    }

    /**
     * Obtain the total amount of heterogeneous chain transaction fees for withdrawal transaction payments(Excluding in chain transaction packaging fees)
     * Handling fees in withdrawal transactions + Additional handling fees for this transaction(If there is any)
     *
     * @param chain
     * @param withdrawalTx
     * @return
     * @throws NulsException
     */
    @Override
    public WithdrawalTotalFeeInfo calculateWithdrawalTotalFee(Chain chain, Transaction withdrawalTx) throws NulsException {
        if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
            // protocol15: Support heterogeneous chain master assets as transaction fees
            return this.calculateWithdrawalTotalFeeProtocol15(chain, withdrawalTx);
        } else {
            return this._calculateWithdrawalTotalFee(chain, withdrawalTx);
        }
    }

    private WithdrawalTotalFeeInfo calculateWithdrawalTotalFeeProtocol15(Chain chain, Transaction withdrawalTx) throws NulsException {
        WithdrawalTotalFeeInfo info = new WithdrawalTotalFeeInfo();
        // Modify the handling fee mechanism
        CoinData coinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        // Subsidy handling fee collection and distribution address
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        BigInteger feeTo = BigInteger.ZERO;
        for (CoinTo coinTo : coinData.getTo()) {
            if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                // ThistoyesNVTOr heterogeneous chain main assets
                boolean nvtFeeAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                        && coinTo.getAssetsId() == chain.getConfig().getAssetId();
                info.setNvtAsset(nvtFeeAsset);
                if (!nvtFeeAsset) {
                    // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
                    AssetName htgMainAssetName;
                    boolean otherFeeAsset = (htgMainAssetName = converterCoreApi.getHtgMainAssetName(coinTo)) != null;
                    if (!otherFeeAsset) {
                        throw new NulsException(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST);
                    }
                    info.setHtgMainAssetName(htgMainAssetName);
                } else {
                    info.setHtgMainAssetName(AssetName.NVT);
                }
                // Subsidies for assembly and handling feescoinTo
                feeTo = coinTo.getAmount();
                info.setFeeCoin(coinTo);
                break;
            }
        }
        // Plus all additional handling fees
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, withdrawalTx.getHash().toHex());
        if (null != po && null != po.getMapAdditionalFee()) {
            for (BigInteger fee : po.getMapAdditionalFee().values()) {
                feeTo = feeTo.add(fee);
            }
        }
        info.setFee(feeTo);
        return info;
    }

    private WithdrawalTotalFeeInfo _calculateWithdrawalTotalFee(Chain chain, Transaction withdrawalTx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        // Subsidy handling fee collection and distribution address
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        BigInteger feeTo = BigInteger.ZERO;
        Coin feeCoin = null;
        for (CoinTo coinTo : coinData.getTo()) {
            // ThistoIt is the main asset of the current chain
            boolean currentChainAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                    && coinTo.getAssetsId() == chain.getConfig().getAssetId();
            if (currentChainAsset && Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                // Subsidies for assembly and handling feescoinTo
                feeTo = coinTo.getAmount();
                feeCoin = coinTo;
                break;
            }
        }
        // Plus all additional handling fees
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, withdrawalTx.getHash().toHex());
        if (null != po && null != po.getMapAdditionalFee()) {
            for (BigInteger fee : po.getMapAdditionalFee().values()) {
                feeTo = feeTo.add(fee);
            }
        }
        return new WithdrawalTotalFeeInfo(feeTo, true, feeCoin);
    }

    private Transaction withdrawalAdditionalFeeTxV0(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // Verify parameters
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[Additional heterogeneous chain handling fees]The original transaction does not exist -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // Not a withdrawal transaction
            chain.getLogger().error("This transaction is not a withdrawal/Proposal transaction -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            CoinData withdrawalTxCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            CoinFrom withdrawalTxCoinFrom = withdrawalTxCoinData.getFrom().get(0);
            byte[] withdrawalTxAddress = withdrawalTxCoinFrom.getAddress();
            byte[] sendAddress = AddressTool.getAddress(withdrawalAdditionalFeeTxDTO.getSignAccount().getAddress());
            if (!Arrays.equals(sendAddress, withdrawalTxAddress)) {
                chain.getLogger().error("The withdrawal transaction does not match the user of the additional transaction -withdrawalAdditionalFeeTx, withdrawalTxHash:{}, withdrawalTxAddress:{}, AdditionalFeeAddress:{} ",
                        txHash,
                        AddressTool.getStringAddressByBytes(withdrawalTxAddress),
                        AddressTool.getStringAddressByBytes(sendAddress));
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_UNMATCHED);
            }
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);

            }
        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.PROPOSAL_CONFIRMED);
            }
        }
        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData(txHash);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        BigInteger additionalFee = withdrawalAdditionalFeeTxDTO.getAmount();
        if (null == additionalFee || additionalFee.compareTo(BigInteger.ZERO) <= 0) {
            chain.getLogger().error("Additional amount error -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinData(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * protocolv1.13 Any address can add additional transaction fees for cross chain transfer transactions
     */
    private Transaction withdrawalAdditionalFeeTxV13(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // Verify parameters
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[Additional heterogeneous chain handling fees]The original transaction does not exist -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // Not a withdrawal transaction
            chain.getLogger().error("This transaction is not a withdrawal/Proposal transaction -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.PROPOSAL_CONFIRMED);
            }
        }
        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData(txHash);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        BigInteger additionalFee = withdrawalAdditionalFeeTxDTO.getAmount();
        if (null == additionalFee || additionalFee.compareTo(BigInteger.ZERO) <= 0) {
            chain.getLogger().error("Additional amount error -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinData(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * protocolv1.15 Support heterogeneous chain master assets as transaction fees
     */
    private Transaction withdrawalAdditionalFeeTxV15(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        // Verify parameters
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[Additional heterogeneous chain handling fees]The original transaction does not exist -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // Not a withdrawal transaction
            chain.getLogger().error("This transaction is not a withdrawal/Proposal transaction -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            CoinData coinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
            Coin feeCoin = null;
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                    feeCoin = coinTo;
                    break;
                }
            }
            // The current additional transaction fee asset chainID
            int feeChainId = withdrawalAdditionalFeeTxDTO.getFeeChainId();
            boolean isAddNvtFeeCoin = feeChainId == chain.getChainId();
            if (!isAddNvtFeeCoin) {
                NerveAssetInfo htgMainAsset = converterCoreApi.getHtgMainAsset(feeChainId);
                if (htgMainAsset.isEmpty()) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
                }
                feeAssetChainId = htgMainAsset.getAssetChainId();
                feeAssetId = htgMainAsset.getAssetId();
            }
            // The additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            if (feeAssetChainId != feeCoin.getAssetsChainId() || feeAssetId != feeCoin.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
            }

        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.PROPOSAL_CONFIRMED);
            }
        }
        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData(txHash);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        BigInteger additionalFee = withdrawalAdditionalFeeTxDTO.getAmount();
        if (null == additionalFee || additionalFee.compareTo(BigInteger.ZERO) <= 0) {
            chain.getLogger().error("Additional amount error -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinDataForWithdrawalAdditional(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee, feeAssetChainId, feeAssetId);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * protocolv21 One click cross chain
     */
    private Transaction withdrawalAdditionalFeeTxV21(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        // Verify parameters
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[Additional heterogeneous chain handling fees]The original transaction does not exist -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN) {
            // Not a withdrawal transaction
            chain.getLogger().error("This transaction is not a withdrawal/Proposal transaction -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            CoinData coinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
            Coin feeCoin = null;
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                    feeCoin = coinTo;
                    break;
                }
            }
            // The current additional transaction fee asset chainID
            int feeChainId = withdrawalAdditionalFeeTxDTO.getFeeChainId();
            boolean isAddNvtFeeCoin = feeChainId == chain.getChainId();
            if (!isAddNvtFeeCoin) {
                NerveAssetInfo htgMainAsset = converterCoreApi.getHtgMainAsset(feeChainId);
                if (htgMainAsset.isEmpty()) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
                }
                feeAssetChainId = htgMainAsset.getAssetChainId();
                feeAssetId = htgMainAsset.getAssetId();
            }
            // The additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            if (feeAssetChainId != feeCoin.getAssetsChainId() || feeAssetId != feeCoin.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
            }

        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.PROPOSAL_CONFIRMED);
            }
        }
        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData(txHash);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        BigInteger additionalFee = withdrawalAdditionalFeeTxDTO.getAmount();
        if (null == additionalFee || additionalFee.compareTo(BigInteger.ZERO) <= 0) {
            chain.getLogger().error("Additional amount error -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinDataForWithdrawalAdditional(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee, feeAssetChainId, feeAssetId);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    private Transaction withdrawalAdditionalFeeTxV35(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
        // Verify parameters
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[Additional heterogeneous chain handling fees]The original transaction does not exist -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN && basicTx.getType() != TxType.CHANGE_VIRTUAL_BANK) {
            // Not a withdrawal transaction
            chain.getLogger().error("This transaction is not a withdrawal/Proposal transaction -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        byte[] extend = null;
        byte[] feePub = ConverterContext.FEE_PUBKEY;
        if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
            // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("The withdrawal transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // Check that the additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            CoinData coinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
            Coin feeCoin = null;
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                    feeCoin = coinTo;
                    break;
                }
            }
            // The current additional transaction fee asset chainID
            int feeChainId = withdrawalAdditionalFeeTxDTO.getFeeChainId();
            boolean isAddNvtFeeCoin = feeChainId == chain.getChainId();
            if (!isAddNvtFeeCoin) {
                NerveAssetInfo htgMainAsset = converterCoreApi.getHtgMainAsset(feeChainId);
                if (htgMainAsset.isEmpty()) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
                }
                feeAssetChainId = htgMainAsset.getAssetChainId();
                feeAssetId = htgMainAsset.getAssetId();
            }
            // The additional handling fee assets must be consistent with the handling fee assets of the withdrawal transaction
            if (feeAssetChainId != feeCoin.getAssetsChainId() || feeAssetId != feeCoin.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
            }
            if (basicTx.getType() == TxType.WITHDRAWAL) {
                extend = HexUtil.decode(ConverterConstant.BTC_ADDING_FEE_WITHDRAW_REBUILD_MARK);
            }

        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("The proposed transaction has been completed,No additional fees for heterogeneous chain withdrawals allowed, proposalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.PROPOSAL_CONFIRMED);
            }
        } else if (basicTx.getType() == TxType.CHANGE_VIRTUAL_BANK) {
            int htgChainId = withdrawalAdditionalFeeTxDTO.getHtgChainId();
            VarInt varInt = new VarInt(htgChainId);
            extend = HexUtil.decode(ConverterConstant.BTC_ADDING_FEE_CHANGE_REBUILD_MARK + HexUtil.encode(varInt.encode()));
            feePub = HexUtil.decode(converterCoreApi.getBtcFeeReceiverPub());
        }
        WithdrawalAdditionalFeeTxData txData = new WithdrawalAdditionalFeeTxData(txHash);
        byte[] txDataBytes;
        try {
            if (withdrawalAdditionalFeeTxDTO.isRebuild()) {
                txData.setExtend(extend);
            }
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        BigInteger additionalFee = withdrawalAdditionalFeeTxDTO.getAmount();
        if (null == additionalFee || additionalFee.compareTo(BigInteger.ZERO) <= 0) {
            chain.getLogger().error("Additional amount error -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinDataForWithdrawalAdditional(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee, feeAssetChainId, feeAssetId, feePub);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //broadcast
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * Assembly additional transaction feesCoinData
     *
     */
    private byte[] assembleFeeCoinDataForWithdrawalAdditional(Chain chain, SignAccountDTO signAccountDTO, BigInteger extraFee, int assetChainId, int assetId) throws NulsException {
        return assembleFeeCoinDataForWithdrawalAdditional(chain, signAccountDTO, extraFee, assetChainId, assetId, ConverterContext.FEE_PUBKEY);
    }

    private byte[] assembleFeeCoinDataForWithdrawalAdditional(Chain chain, SignAccountDTO signAccountDTO, BigInteger extraFee, int assetChainId, int assetId, byte[] feePub) throws NulsException {
        String address = signAccountDTO.getAddress();
        //The transfer transaction transfer address must be a local chain address
        if (!AddressTool.validAddress(chain.getChainId(), address)) {
            throw new NulsException(ConverterErrorCode.IS_NOT_CURRENT_CHAIN_ADDRESS);
        }
        NonceBalance nonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                address);
        BigInteger balance = nonceBalance.getAvailable();
        BigInteger amount = extraFee;
        if (BigIntegerUtils.isLessThan(balance, amount)) {
            chain.getLogger().error("The balance is insufficient to cover the package fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        //Query ledger to obtainnoncevalue
        byte[] nonce = nonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                assetChainId,
                assetId,
                amount,
                nonce,
                NORMAL_TX_LOCKED);
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);

        List<CoinTo> tos = new ArrayList<>();
        if (greaterTranZero(extraFee)) {
            CoinTo extraFeeCoinTo = new CoinTo(
                    AddressTool.getAddress(feePub, chain.getChainId()),
                    assetChainId,
                    assetId,
                    extraFee);
            tos.add(extraFeeCoinTo);
        } else {
            // If there is no additional cost to assemble it intocointo ,Then it is necessary to assemble a product with an amount of0ofcoinTo, coinToThe amount is0, fromThe amount becomes a handling fee
            CoinTo coinTo = new CoinTo(
                    AddressTool.getAddress(address),
                    assetChainId,
                    assetId,
                    BigInteger.ZERO);
            tos.add(coinTo);
        }

        coinData.setFrom(froms);
        coinData.setTo(tos);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    @Override
    public Transaction createRechargeTxOfBtcSys(Chain chain, RechargeTxOfBtcSysDTO rechargeTxDTO) throws NulsException {
        Transaction tx = this.createRechargeTxOfBtcSysWithoutSign(chain, rechargeTxDTO);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Verifier verification
        rechargeVerifier.validateTxOfBtcSys(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(rechargeTxDTO.getHtgChainId(), rechargeTxDTO.getHtgTxHash()));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(RechargeTxData.class));
        return tx;
    }

    @Override
    public Transaction createRechargeTxOfBtcSysWithoutSign(Chain chain, RechargeTxOfBtcSysDTO dto) throws NulsException {
        RechargeTxData txData = new RechargeTxData(dto.getHtgTxHash());
        txData.setHeterogeneousChainId(dto.getHtgChainId());
        if (dto.getExtend() != null) {
            txData.setExtend(dto.getExtend());
        }
        List<CoinTo> tos = new ArrayList<>();
        byte[] toAddress = AddressTool.getAddress(dto.getTo());
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(
                dto.getHtgChainId(),
                dto.getHtgAssetId());
        CoinTo coinTo = new CoinTo(
                toAddress,
                nerveAssetInfo.getAssetChainId(),
                nerveAssetInfo.getAssetId(),
                converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                        dto.getHtgChainId(),
                        nerveAssetInfo.getAssetChainId(),
                        nerveAssetInfo.getAssetId(),
                        dto.getAmount())
        );
        tos.add(coinTo);

        // Simultaneously rechargetokenandmain, increasemainSupport for
        if (dto.isDepositII()) {
            NerveAssetInfo mainInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(dto.getHtgChainId(), 1);
            CoinTo mainTo = new CoinTo(
                    toAddress,
                    mainInfo.getAssetChainId(),
                    mainInfo.getAssetId(),
                    converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                            dto.getHtgChainId(),
                            mainInfo.getAssetChainId(),
                            mainInfo.getAssetId(),
                            dto.getMainAmount())
            );
            tos.add(mainTo);
        }
        // There is a handling fee
        if (StringUtils.isNotBlank(dto.getFeeTo()) && dto.getFee().compareTo(BigInteger.ZERO) > 0) {
            CoinTo mainTo = new CoinTo(
                    AddressTool.getAddress(dto.getFeeTo()),
                    nerveAssetInfo.getAssetChainId(),
                    nerveAssetInfo.getAssetId(),
                    converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(
                            dto.getHtgChainId(),
                            nerveAssetInfo.getAssetChainId(),
                            nerveAssetInfo.getAssetId(),
                            dto.getFee())
            );
            tos.add(mainTo);
        }

        CoinData coinData = new CoinData();
        coinData.setTo(tos);
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.RECHARGE, txDataBytes, dto.getHtgTxTime());
        tx.setCoinData(coinDataBytes);
        if (StringUtils.isNotBlank(dto.getHtgFrom())) {
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(dto.getHtgChainId());
            HeterogeneousAssetInfo asset = docking.getMainAsset();
            tx.setRemark(String.format("Chain: %s-%s, from: %s, originalTxHash: %s", asset.getChainId(), asset.getSymbol(), dto.getHtgFrom(), dto.getHtgTxHash()).getBytes(StandardCharsets.UTF_8));
        }
        return tx;
    }

    @Override
    public Transaction createWithdrawUTXOTx(Chain chain, WithdrawalUTXOTxData txData, long txTime) throws NulsException {
        Transaction tx = createWithdrawUTXOTxWithoutSign(chain, txData, txTime);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Verifier verification
        bitcoinVerifier.validateWithdrawlUTXO(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(txData.getHtgChainId(), EMPTY_STRING));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, txData.getNerveTxHash(), heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(WithdrawalUTXOTxData.class));
        return tx;
    }

    @Override
    public Transaction createWithdrawUTXOTxWithoutSign(Chain chain, WithdrawalUTXOTxData txData, long txTime) throws NulsException {
        CoinData coinData = new CoinData();
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_UTXO, txDataBytes, txTime);
        tx.setCoinData(coinDataBytes);
        return tx;
    }

    @Override
    public Transaction createWithdrawlFeeLogTx(Chain chain, WithdrawalFeeLog txData, long txTime, byte[] remark) throws NulsException {
        Transaction tx = createWithdrawlFeeLogTxWithoutSign(chain, txData, txTime, remark);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // Verifier verification
        bitcoinVerifier.validateWithdrawFee(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(txData.getHtgChainId(), txData.getHtgTxHash()));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        if (txData.isNerveInner()) {
            message.setOriginalHash(txData.getHtgTxHash());
        }
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // complete
        chain.getLogger().debug(tx.format(WithdrawalFeeLog.class));
        return tx;
    }

    @Override
    public Transaction createWithdrawlFeeLogTxWithoutSign(Chain chain, WithdrawalFeeLog txData, long txTime, byte[] remark) throws NulsException {
        CoinData coinData = new CoinData();
        byte[] coinDataBytes;
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_UTXO_FEE_PAYMENT, txDataBytes, txTime);
        tx.setCoinData(coinDataBytes);
        tx.setRemark(remark);
        return tx;
    }

    @Override
    public Transaction createUnlockUTXOTx(Chain chain, String from, String password, String nerveTxHash, int forceUnlock, Integer htgChainId) throws NulsException {
        GeneralBusTxData txData = new GeneralBusTxData();
        txData.setType(1);
        txData.setData(HexUtil.decode(nerveTxHash + (forceUnlock == 0 ? "00" : "01") + (htgChainId == null ? "" : Integer.toHexString(htgChainId))));
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.GENERAL_BUS);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleFeeCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //broadcast
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(GeneralBusTxData.class));
        return tx;
    }

    @Override
    public Transaction createSkipTx(Chain chain, String from, String password, String nerveTxHash) throws NulsException {
        GeneralBusTxData txData = new GeneralBusTxData();
        txData.setType(2);
        txData.setData(HexUtil.decode(nerveTxHash));
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.GENERAL_BUS);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleFeeCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //autograph
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //broadcast
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(GeneralBusTxData.class));
        return tx;
    }
}
