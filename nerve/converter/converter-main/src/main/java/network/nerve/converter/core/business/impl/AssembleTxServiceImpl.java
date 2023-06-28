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
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
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
import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * @author: Loki
 * @date: 2020-02-28
 */
@Component
public class AssembleTxServiceImpl implements AssembleTxService {

    /**
     * 普通交易为非解锁交易：0，解锁金额交易（退出共识，退出委托）：-1
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
        chain.getLogger().debug(tx.format(ConfirmedChangeVirtualBankTxData.class));
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
        // 支持同时转入token和main
        Transaction tx = this.createRechargeTxWithoutSign(chain, rechargeTxDTO);
        P2PHKSignature p2PHKSignature = ConverterSignUtil.signTxCurrentVirtualBankAgent(chain, tx);
        // 调验证器验证
        rechargeVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(new HeterogeneousHash(rechargeTxDTO.getHeterogeneousChainId(), rechargeTxDTO.getOriginalTxHash()));
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // 完成
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
        // 同时充值token和main，增加main的支持
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
        // 调用验证器验证
        rechargeUnconfirmedVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(txData.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // 完成
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
        // 调用验证器验证
        rechargeUnconfirmedVerifier.validateOneClickCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(txData.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // 完成
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
        // 调用验证器验证
        rechargeVerifier.validateOneClickCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(dto.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // 完成
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
            // 跨到目标链的手续费错误
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
        //组装to
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
        // 判断组装异构链补贴手续费暂存to
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
        // 组装tipping
        BigInteger tipping = dto.getTipping();
        if (tipping.compareTo(BigInteger.ZERO) > 0) {
            // 合法的Nerve地址
            if (!converterCoreApi.validNerveAddress(dto.getTippingAddress())) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address error:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), dto.getTippingAddress(), dto.getOriginalTxHash().getHeterogeneousHash());
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            byte[] tippingAddress = AddressTool.getAddress(dto.getTippingAddress());
            if (Arrays.equals(tippingAddress, withdrawalBlackhole) || Arrays.equals(tippingAddress, withdrawalFeeAddress)) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address setting error:{}, heterogeneousHash:{}", dto.getOriginalTxHash().getHeterogeneousChainId(), dto.getTippingAddress(), dto.getOriginalTxHash().getHeterogeneousHash());
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            // 不得大于跨链资产的10%
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
        // 调用验证器验证
        rechargeVerifier.validateAddFeeCrossChain(chain, tx);
        saveWaitingProcess(chain, tx);

        List<HeterogeneousHash> heterogeneousHashList = new ArrayList<>();
        heterogeneousHashList.add(dto.getOriginalTxHash());
        BroadcastHashSignMessage message = new BroadcastHashSignMessage(tx, p2PHKSignature, heterogeneousHashList);
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        // 完成
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
        //组装to
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
     * 交易验证成功保存并广播签名,然后等待处理
     *
     * @param chain
     * @param tx
     */
    private void saveWaitingProcess(Chain chain, Transaction tx) {
        // 保存进txStorageService
        txStorageService.save(chain, new TransactionPO(tx));
        // 如果待处理签名集合 中有此交易的签名列表 则拿出来放入处理队列
        List<UntreatedMessage> listMsg = chain.getFutureMessageMap().get(tx.getHash());
        if (null != listMsg) {
            for (UntreatedMessage msg : listMsg) {
                chain.getSignMessageByzantineQueue().offer(msg);
            }
            // 清空缓存的签名
            chain.getFutureMessageMap().remove(tx.getHash());
        }
    }

    @Override
    public Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException {
        // 修改手续费机制，支持异构链主资产作为手续费
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
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction withdrawalAdditionalFeeTx(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        if (converterCoreApi.isProtocol21()) {
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
    public Transaction createConfirmWithdrawalTx(Chain chain, ConfirmWithdrawalTxData confirmWithdrawalTxData, long txTime) throws NulsException {
        Transaction tx = this.createConfirmWithdrawalTxWithoutSign(chain, confirmWithdrawalTxData, txTime);
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
    public Transaction createConfirmWithdrawalTxWithoutSign(Chain chain, ConfirmWithdrawalTxData confirmWithdrawalTxData, long txTime) throws NulsException {
        byte[] txDataBytes = null;
        try {
            txDataBytes = confirmWithdrawalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_WITHDRAWAL, txDataBytes, txTime);
        return tx;
    }

    @Override
    public Transaction processProposalTx(Chain chain, Transaction tx) throws NulsException {
        return processProposalTx(chain, tx, null, null);
    }

    public Transaction processProposalTx(Chain chain, Transaction tx, Integer heterogeneousChainId, String heterogeneousTxHash) throws NulsException {
        proposalVerifier.validate(chain, tx);
        saveWaitingProcess(chain, tx);
        //广播完整交易
        NewTxMessage newTxMessage = new NewTxMessage();
        newTxMessage.setTx(tx);
        NetWorkCall.broadcast(chain, newTxMessage, ConverterCmdConstant.NEW_TX_MESSAGE);

        if (VirtualBankUtil.isCurrentDirector(chain)) {
            if (null == heterogeneousChainId || StringUtils.isBlank(heterogeneousTxHash)) {
                ProposalTxData txdata = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
                heterogeneousChainId = txdata.getHeterogeneousChainId();
                heterogeneousTxHash = txdata.getHeterogeneousTxHash();
            }
            // 如果当前是虚拟银行节点, 直接开始处理.
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
            //枚举
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
            // 兼容非以太系地址 update by pierre at 2021/11/16
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
            //枚举
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
        // 拜占庭交易流程
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
            //枚举
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
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //广播
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
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //广播
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
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), signAccountDTO, tx);
        //广播
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(HeterogeneousMainAssetBindTxData.class));
        return tx;
    }

    @Override
    public Transaction createHeterogeneousContractAssetRegCompleteTx(Chain chain, Transaction pendingTx) throws NulsException {
        if (pendingTx == null || pendingTx.getType() != TxType.HETEROGENEOUS_CONTRACT_ASSET_REG_PENDING) {
            chain.getLogger().error("交易信息为空或类型不正确");
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

        // 调验证器验证
        heterogeneousContractAssetRegCompleteVerifier.validate(chain.getChainId(), tx);
        saveWaitingProcess(chain, tx);
        BroadcastHashSignMessage message = new BroadcastHashSignMessage();
        message.setHash(tx.getHash());
        message.setP2PHKSignature(p2PHKSignature);
        message.setType(tx.getType());
        NetWorkCall.broadcast(chain, message, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);

        // 完成
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
     * 组装提现交易CoinData
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

        // 修改手续费机制，支持异构链主资产作为手续费
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
        //提现资产from
        List<CoinFrom> listFrom = new ArrayList<>();
        if (withdrawalAssetChainId == feeAssetChainId && withdrawalAssetId == feeAssetId) {
            // 提现资产和手续费资产一致时，合并成一条from
            CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, withdrawalAssetChainId, withdrawalAssetId, withdrawalTxDTO.getDistributionFee());
            listFrom.add(withdrawalCoinFrom);
        } else {
            CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, withdrawalAssetChainId, withdrawalAssetId, BigInteger.ZERO);
            listFrom.add(withdrawalCoinFrom);
            // 只要不是当前链主资产 都要组装额外的coinFrom
            CoinFrom withdrawalFeeCoinFrom;
            //手续费from 包含异构链补贴手续费
            withdrawalFeeCoinFrom = getWithdrawalFeeCoinFrom(chain, address, withdrawalTxDTO.getDistributionFee(), feeAssetChainId, feeAssetId);

            listFrom.add(withdrawalFeeCoinFrom);
        }
        //组装to
        List<CoinTo> listTo = new ArrayList<>();
        CoinTo withdrawalCoinTo = new CoinTo(
                AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId()),
                withdrawalAssetChainId,
                withdrawalAssetId,
                amount);

        listTo.add(withdrawalCoinTo);
        // 判断组装异构链补贴手续费暂存to
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
     * 组装提现资产CoinFrom
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
        //提现资产
        if (BigIntegerUtils.isEqualOrLessThan(amount, BigInteger.ZERO)) {
            chain.getLogger().error("提现金额不能小于0, amount:{}", amount);
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
            chain.getLogger().error("提现资产余额不足 chainId:{}, assetId:{}, withdrawal amount:{}, available balance:{} ",
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
     * 组装提现交易手续费(包含链内打包手续费, 异构链补贴手续费)
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
        // 本链资产余额
        BigInteger balance = currentChainNonceBalance.getAvailable();

        // 总手续费 = 异构链转账(或签名)手续费[都以链内主资产结算]
        BigInteger totalFee = withdrawalHeterogeneousFee;
        if (BigIntegerUtils.isLessThan(balance, totalFee)) {
            chain.getLogger().error("Insufficient balance of withdrawal fee. amount to be paid:{}, available balance:{} ", totalFee, balance);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // 查询账本获取nonce值
        byte[] nonce = currentChainNonceBalance.getNonce();
        return new CoinFrom(AddressTool.getAddress(address), feeAssetChainId, feeAssetId, totalFee, nonce, (byte) 0);
    }

    /**
     * 组装提现交易打包手续费(只包含链内打包手续费)
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
        // 本链资产余额
        BigInteger balance = currentChainNonceBalance.getAvailable();
        //打包手续费
        if (BigIntegerUtils.isLessThan(balance, TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES)) {
            chain.getLogger().error("The balance is insufficient to cover the package fee");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // 查询账本获取nonce值
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
     * 组装补贴手续费交易CoinData
     *
     * @param chain
     * @param listRewardAddress
     * @return
     * @throws NulsException
     */
    private byte[] assembleDistributionFeeCoinData(Chain chain, NulsHash basisTxHash, List<byte[]> listRewardAddress, boolean isProposal) throws NulsException {
        byte[] feeFromAdddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        // 如果是提案 basisTxHash 是确认提案交易hash, 而非提案交易hash
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, basisTxHash.toHex());

        if (isProposal) {
            // 如果是提案 的补贴手续费, 需要获取提案交易
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
        // 要补贴的手续费总额
        // 修改手续费机制，支持异构链主资产作为手续费
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
     * 组装补贴手续费交易CoinFrom
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
        //查询手续费暂存地址余额够不够
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                AddressTool.getStringAddressByBytes(feeFromAdddress));
        // 查询账本获取nonce值
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
     * 组装补贴手续费交易CoinTo
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
        // 计算 每个节点补贴多少手续费
        BigInteger distributionFee = distributionFeeInfo.getFee();
        BigInteger count = BigInteger.valueOf(listRewardAddress.size());
        BigInteger amount = distributionFee.divide(count);
        Map<String, BigInteger> map = calculateDistributionFeeCoinToAmount(listRewardAddress, amount);
        // 组装cointo
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
     * 组装不含CoinData的交易
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
     * 组装交易，CoinData只包含手续费
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
     * 组装手续费（CoinData）
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
     * 组装手续费（CoinData）
     *
     * @param chain
     * @param signAccountDTO
     * @param extraFee       向公共手续费收集地址 支付额外的业务费用(例如提案费用等), 用于后续费用的补偿
     * @return
     * @throws NulsException
     */
    private byte[] assembleFeeCoinData(Chain chain, SignAccountDTO signAccountDTO, BigInteger extraFee) throws NulsException {
        String address = signAccountDTO.getAddress();
        //转账交易转出地址必须是本链地址
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
        //查询账本获取nonce值
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
            // 额外费用 如果有
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
            // 如果没有额外费用组装到cointo ,则需要组装一个金额为0的coinTo, coinTo金额为0, from的金额成为手续费
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
        //转账交易转出地址必须是本链地址
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
        //查询账本获取nonce值
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
                // 手续费为动态的, 即提现交易的支付的+追加的
                fee = calculateWithdrawalTotalFee(chain, basicTx);
            } else if (height >= ConverterContext.FEE_EFFECTIVE_HEIGHT_SECOND) {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_10);
            } else if (height >= ConverterContext.FEE_EFFECTIVE_HEIGHT_FIRST) {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_100);
            } else {
                fee = new WithdrawalTotalFeeInfo(ConverterConstant.DISTRIBUTION_FEE_10);
            }
        } else {
            // 如果是提案原路退回,需要获取对它的手续费追加金额
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
        // 修改手续费机制，支持异构链主资产作为手续费
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
     * 获取提现交易支付的异构链手续费总额(不包含链内交易打包手续费)
     * 提现交易中的手续费 + 对该交易追加的手续费(如果有)
     *
     * @param chain
     * @param withdrawalTx
     * @return
     * @throws NulsException
     */
    @Override
    public WithdrawalTotalFeeInfo calculateWithdrawalTotalFee(Chain chain, Transaction withdrawalTx) throws NulsException {
        if (converterCoreApi.isSupportProtocol15TrxCrossChain()) {
            // 协议15: 支持异构链主资产作为手续费
            return this.calculateWithdrawalTotalFeeProtocol15(chain, withdrawalTx);
        } else {
            return this._calculateWithdrawalTotalFee(chain, withdrawalTx);
        }
    }

    private WithdrawalTotalFeeInfo calculateWithdrawalTotalFeeProtocol15(Chain chain, Transaction withdrawalTx) throws NulsException {
        WithdrawalTotalFeeInfo info = new WithdrawalTotalFeeInfo();
        // 修改手续费机制
        CoinData coinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        // 补贴手续费收集分发地址
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        BigInteger feeTo = BigInteger.ZERO;
        for (CoinTo coinTo : coinData.getTo()) {
            if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                // 该to是NVT或者异构链主资产
                boolean nvtFeeAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                        && coinTo.getAssetsId() == chain.getConfig().getAssetId();
                info.setNvtAsset(nvtFeeAsset);
                if (!nvtFeeAsset) {
                    // 可使用其他异构网络的主资产作为手续费, 比如提现到ETH，支付BNB作为手续费
                    AssetName htgMainAssetName;
                    boolean otherFeeAsset = (htgMainAssetName = converterCoreApi.getHtgMainAssetName(coinTo)) != null;
                    if (!otherFeeAsset) {
                        throw new NulsException(ConverterErrorCode.WITHDRAWAL_FEE_NOT_EXIST);
                    }
                    info.setHtgMainAssetName(htgMainAssetName);
                }
                // 组装的补贴手续费的coinTo
                feeTo = coinTo.getAmount();
                info.setFeeCoin(coinTo);
                break;
            }
        }
        // 加上所有追加的手续费
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
        // 补贴手续费收集分发地址
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        BigInteger feeTo = BigInteger.ZERO;
        Coin feeCoin = null;
        for (CoinTo coinTo : coinData.getTo()) {
            // 该to是当前链的主资产
            boolean currentChainAsset = coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                    && coinTo.getAssetsId() == chain.getConfig().getAssetId();
            if (currentChainAsset && Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                // 组装的补贴手续费的coinTo
                feeTo = coinTo.getAmount();
                feeCoin = coinTo;
                break;
            }
        }
        // 加上所有追加的手续费
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, withdrawalTx.getHash().toHex());
        if (null != po && null != po.getMapAdditionalFee()) {
            for (BigInteger fee : po.getMapAdditionalFee().values()) {
                feeTo = feeTo.add(fee);
            }
        }
        return new WithdrawalTotalFeeInfo(feeTo, true, feeCoin);
    }

    private Transaction withdrawalAdditionalFeeTxV0(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // 验证参数
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[追加异构链手续费]原始交易不存在 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // 不是提现交易
            chain.getLogger().error("该交易不是提现/提案交易 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            CoinData withdrawalTxCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            CoinFrom withdrawalTxCoinFrom = withdrawalTxCoinData.getFrom().get(0);
            byte[] withdrawalTxAddress = withdrawalTxCoinFrom.getAddress();
            byte[] sendAddress = AddressTool.getAddress(withdrawalAdditionalFeeTxDTO.getSignAccount().getAddress());
            if (!Arrays.equals(sendAddress, withdrawalTxAddress)) {
                chain.getLogger().error("该提现交易与追加交易用户不匹配 -withdrawalAdditionalFeeTx, withdrawalTxHash:{}, withdrawalTxAddress:{}, AdditionalFeeAddress:{} ",
                        txHash,
                        AddressTool.getStringAddressByBytes(withdrawalTxAddress),
                        AddressTool.getStringAddressByBytes(sendAddress));
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_UNMATCHED);
            }
            // 判断该提现交易是否已经有对应的确认提现交易
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);

            }
        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("该提案交易已经完成,不能再追加异构链提现手续费, proposalTxhash:{}", basicTx.getHash().toHex());
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
            chain.getLogger().error("追加金额错误 -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinData(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee);
        tx.setCoinData(coinData);
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * 协议v1.13 任意地址可给跨链转出交易追加手续费
     */
    private Transaction withdrawalAdditionalFeeTxV13(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // 验证参数
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[追加异构链手续费]原始交易不存在 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // 不是提现交易
            chain.getLogger().error("该交易不是提现/提案交易 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            // 判断该提现交易是否已经有对应的确认提现交易
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("该提案交易已经完成,不能再追加异构链提现手续费, proposalTxhash:{}", basicTx.getHash().toHex());
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
            chain.getLogger().error("追加金额错误 -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinData(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee);
        tx.setCoinData(coinData);
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * 协议v1.15 支持异构链主资产作为手续费
     */
    private Transaction withdrawalAdditionalFeeTxV15(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // 修改手续费机制，支持异构链主资产作为手续费
        // 验证参数
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[追加异构链手续费]原始交易不存在 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL) {
            // 不是提现交易
            chain.getLogger().error("该交易不是提现/提案交易 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL) {
            // 判断该提现交易是否已经有对应的确认提现交易
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // 检查追加的手续费资产，必须与提现交易的手续费资产一致
            CoinData coinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
            Coin feeCoin = null;
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                    feeCoin = coinTo;
                    break;
                }
            }
            // 当前追加的手续费资产链ID
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
            // 追加的手续费资产，必须与提现交易的手续费资产一致
            if (feeAssetChainId != feeCoin.getAssetsChainId() || feeAssetId != feeCoin.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
            }

        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("该提案交易已经完成,不能再追加异构链提现手续费, proposalTxhash:{}", basicTx.getHash().toHex());
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
            chain.getLogger().error("追加金额错误 -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinDataForWithdrawalAdditional(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee, feeAssetChainId, feeAssetId);
        tx.setCoinData(coinData);
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * 协议v21 一键跨链
     */
    private Transaction withdrawalAdditionalFeeTxV21(Chain chain, WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO) throws NulsException {
        // 修改手续费机制，支持异构链主资产作为手续费
        // 验证参数
        String txHash = withdrawalAdditionalFeeTxDTO.getTxHash();
        if (StringUtils.isBlank(txHash)) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, txHash);
        if (null == basicTx) {
            chain.getLogger().error("[追加异构链手续费]原始交易不存在 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.PROPOSAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN) {
            // 不是提现交易
            chain.getLogger().error("该交易不是提现/提案交易 -withdrawalAdditionalFeeTx, hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
            // 判断该提现交易是否已经有对应的确认提现交易
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}", basicTx.getHash().toHex());
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // 检查追加的手续费资产，必须与提现交易的手续费资产一致
            CoinData coinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
            Coin feeCoin = null;
            for (CoinTo coinTo : coinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                    feeCoin = coinTo;
                    break;
                }
            }
            // 当前追加的手续费资产链ID
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
            // 追加的手续费资产，必须与提现交易的手续费资产一致
            if (feeAssetChainId != feeCoin.getAssetsChainId() || feeAssetId != feeCoin.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
            }

        } else if(basicTx.getType() == TxType.PROPOSAL){
            String confirmProposalHash = proposalExeStorageService.find(chain, basicTx.getHash().toHex());
            if (StringUtils.isNotBlank(confirmProposalHash)) {
                chain.getLogger().error("该提案交易已经完成,不能再追加异构链提现手续费, proposalTxhash:{}", basicTx.getHash().toHex());
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
            chain.getLogger().error("追加金额错误 -withdrawalAdditionalFeeTx, hash:{}, additionalFee:{}", txHash, additionalFee);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL_ADDITIONAL_FEE, txDataBytes, withdrawalAdditionalFeeTxDTO.getRemark());
        byte[] coinData = assembleFeeCoinDataForWithdrawalAdditional(chain, withdrawalAdditionalFeeTxDTO.getSignAccount(), additionalFee, feeAssetChainId, feeAssetId);
        tx.setCoinData(coinData);
        //签名
        ConverterSignUtil.signTx(chain.getChainId(), withdrawalAdditionalFeeTxDTO.getSignAccount(), tx);
        chain.getLogger().debug(tx.format(WithdrawalAdditionalFeeTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    /**
     * 组装追加手续费交易的CoinData
     *
     */
    private byte[] assembleFeeCoinDataForWithdrawalAdditional(Chain chain, SignAccountDTO signAccountDTO, BigInteger extraFee, int assetChainId, int assetId) throws NulsException {
        String address = signAccountDTO.getAddress();
        //转账交易转出地址必须是本链地址
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
        //查询账本获取nonce值
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
                    AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId()),
                    assetChainId,
                    assetId,
                    extraFee);
            tos.add(extraFeeCoinTo);
        } else {
            // 如果没有额外费用组装到cointo ,则需要组装一个金额为0的coinTo, coinTo金额为0, from的金额成为手续费
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

}
