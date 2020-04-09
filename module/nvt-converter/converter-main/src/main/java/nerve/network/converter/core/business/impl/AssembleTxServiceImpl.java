/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.core.business.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.core.business.HeterogeneousService;
import nerve.network.converter.core.context.HeterogeneousChainManager;
import nerve.network.converter.enums.ProposalTypeEnum;
import nerve.network.converter.enums.ProposalVoteChoiceEnum;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import nerve.network.converter.model.bo.NonceBalance;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.dto.ProposalTxDTO;
import nerve.network.converter.model.dto.RechargeTxDTO;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.dto.WithdrawalTxDTO;
import nerve.network.converter.model.txdata.*;
import nerve.network.converter.rpc.call.AccountCall;
import nerve.network.converter.rpc.call.ConsensusCall;
import nerve.network.converter.rpc.call.LedgerCall;
import nerve.network.converter.rpc.call.TransactionCall;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Chino
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
    private HeterogeneousService HeterogeneousService;


    @Override
    public Transaction createChangeVirtualBankTx(Chain chain, List<byte[]> inAgentList, List<byte[]> outAgentList, long txTime) throws NulsException {
        ChangeVirtualBankTxData txData = new ChangeVirtualBankTxData();
        txData.setInAgents(inAgentList);
        txData.setOutAgents(outAgentList);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CHANGE_VIRTUAL_BANK, txDataBytes, txTime);
        signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(ChangeVirtualBankTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createConfirmedChangeVirtualBankTx(Chain chain, NulsHash changeVirtualBankTxHash, List<HeterogeneousConfirmedVirtualBank> listConfirmed, long txTime) throws NulsException {
        ConfirmedChangeVirtualBankTxData txData = new ConfirmedChangeVirtualBankTxData();
        txData.setChangeVirtualBankTxHash(changeVirtualBankTxHash);
        txData.setListConfirmed(listConfirmed);
        List<byte[]> agentList = new ArrayList<>();
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
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
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_CHANGE_VIRTUAL_BANK, txDataBytes, txTime);
        signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(ChangeVirtualBankTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createInitializeHeterogeneousTx(Chain chain, int heterogeneousChainId,/* List<byte[]> listDirector,*/ long txTime) throws NulsException {
        InitializeHeterogeneousTxData txData = new InitializeHeterogeneousTxData();
        txData.setHeterogeneousChainId(heterogeneousChainId);
//        txData.setListDirector(listDirector);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.INITIALIZE_HETEROGENEOUS, txDataBytes, txTime);
        signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(InitializeHeterogeneousTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createRechargeTx(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException {
        RechargeTxData txData = new RechargeTxData(rechargeTxDTO.getHeterogeneousTxHash());
        byte[] toAddress = AddressTool.getAddress(rechargeTxDTO.getToAddress());
        CoinTo coinTo = new CoinTo(
                toAddress,
                rechargeTxDTO.getHeterogeneousChainId(),
                rechargeTxDTO.getHeterogeneousAssetId(),
                rechargeTxDTO.getAmount());
        List<CoinTo> tos = new ArrayList<>();
        tos.add(coinTo);
        CoinData coinData = new CoinData();
        coinData.setTo(tos);
        byte[] coinDataBytes = null;
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
            coinDataBytes = coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.RECHARGE, txDataBytes, rechargeTxDTO.getTxtime());
        tx.setCoinData(coinDataBytes);
        signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(RechargeTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException {
        int heterogeneousChainId = withdrawalTxDTO.getHeterogeneousChainId();
        String heterogeneousAddress = withdrawalTxDTO.getHeterogeneousAddress();
        if (null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        if (StringUtils.isBlank(heterogeneousAddress)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ADDRESS_NULL);
        }
        WithdrawalTxData txData = new WithdrawalTxData(heterogeneousAddress);
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }

        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.WITHDRAWAL, txDataBytes, withdrawalTxDTO.getRemark());
        byte[] coinData = assembleWithdrawalCoinData(chain, withdrawalTxDTO);
        tx.setCoinData(coinData);
        //签名
        signTx(tx, withdrawalTxDTO.getSignAccount());
        chain.getLogger().debug(tx.format(WithdrawalTxData.class));
        //广播
        TransactionCall.newTx(chain, tx);
        return tx;
    }


    @Override
    public Transaction createConfirmWithdrawalTx(Chain chain, ConfirmWithdrawalTxData confirmWithdrawalTxData, long txTime) throws NulsException {
        byte[] txData = null;
        try {
            txData = confirmWithdrawalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.CONFIRM_WITHDRAWAL, txData, txTime);
        signTxCurrentVirtualBankAgent(chain, tx);
        chain.getLogger().debug(tx.format(ConfirmWithdrawalTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }

    @Override
    public Transaction createProposalTx(Chain chain, ProposalTxDTO proposalTxDTO) throws NulsException {
        if (null == ProposalTypeEnum.getEnum(proposalTxDTO.getType())) {
            //枚举
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_INVALID);
        }
        ProposalTxData txData = new ProposalTxData();
        txData.setType(proposalTxDTO.getType());
        txData.setVoteRangeType(proposalTxDTO.getVoteRangeType());
        txData.setContent(proposalTxDTO.getContent());
        txData.setHeterogeneousTxHash(proposalTxDTO.getHeterogeneousTxHash());
        if (StringUtils.isNotBlank(proposalTxDTO.getBusinessAddress())) {
            txData.setAddress(AddressTool.getAddress(proposalTxDTO.getBusinessAddress()));
        }
        byte[] txDataBytes = null;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.PROPOSAL, txDataBytes, proposalTxDTO.getRemark());
        signTx(tx, proposalTxDTO.getSignAccountDTO());
        chain.getLogger().debug(tx.format(ProposalTxData.class));
        TransactionCall.newTx(chain, tx);
        return tx;
    }


    @Override
    public Transaction createVoteProposalTx(Chain chain, NulsHash proposalTxHash, byte choice, String remark) throws NulsException {
        if (null == proposalTxHash) {
            throw new NulsException(ConverterErrorCode.PROPOSAL_TX_HASH_NULL);
        }
        if (null == ProposalVoteChoiceEnum.getEnum(choice)) {
            //枚举
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_INVALID);
        }
        VoteProposalTxData voteProposalTxData = new VoteProposalTxData(proposalTxHash, choice);
        byte[] txData = null;
        try {
            txData = voteProposalTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.VOTE_PROPOSAL, txData, remark);
        signTxCurrentVirtualBankAgent(chain, tx);
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(VoteProposalTxData.class));
        return tx;
    }

    @Override
    public Transaction createDistributionFeeTx(Chain chain, NulsHash basisTxHash, List<byte[]> listRewardAddress, long txTime) throws NulsException {
        DistributionFeeTxData distributionFeeTxData = new DistributionFeeTxData();
        distributionFeeTxData.setBasisTxHash(basisTxHash);
        byte[] txData = null;
        try {
            txData = distributionFeeTxData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = assembleUnsignTxWithoutCoinData(TxType.DISTRIBUTION_FEE, txData, txTime);
        byte[] coinData = assembleDistributionFeeCoinData(chain, listRewardAddress);
        tx.setCoinData(coinData);
        signTxCurrentVirtualBankAgent(chain, tx);
        TransactionCall.newTx(chain, tx);
        chain.getLogger().debug(tx.format(DistributionFeeTxData.class));
        return tx;
    }


    @Override
    public Transaction createHeterogeneousContractAssetRegTx(Chain chain, String from, String password,
                                                             int heterogeneousChainId, int decimals, String symbol,
                                                             String contractAddress, String remark) throws NulsException {
        if (null == heterogeneousChainManager.getHeterogeneousChainByChainId(heterogeneousChainId)) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        HeterogeneousContractAssetRegTxData txData = new HeterogeneousContractAssetRegTxData();
        txData.setChainId(heterogeneousChainId);
        txData.setDecimals((byte) decimals);
        txData.setSymbol(symbol);
        txData.setContractAddress(contractAddress);
        byte[] txDataBytes;
        try {
            txDataBytes = txData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
        Transaction tx = new Transaction(TxType.HETEROGENEOUS_CONTRACT_ASSET_REG);
        tx.setTxData(txDataBytes);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        tx.setRemark(StringUtils.isBlank(remark) ? null : StringUtils.bytes(remark));

        SignAccountDTO signAccountDTO = new SignAccountDTO(from, password);
        byte[] coinData = assembleCoinData(chain, signAccountDTO);
        tx.setCoinData(coinData);
        //签名
        signTx(tx, signAccountDTO);
        //广播
        TransactionCall.newTx(chain, tx);

        chain.getLogger().debug(tx.format(WithdrawalTxData.class));
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
        int heterogeneousChainId = withdrawalTxDTO.getHeterogeneousChainId();
        int heterogeneousAssetId = withdrawalTxDTO.getHeterogeneousAssetId();

        int chainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        BigInteger amount = withdrawalTxDTO.getAmount();
        String address = withdrawalTxDTO.getSignAccount().getAddress();
        //提现资产from
        CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, heterogeneousChainId, heterogeneousAssetId);

        boolean assembleCurrentAssetFee = HeterogeneousService.isAssembleCurrentAssetFee(withdrawalTxDTO.getHeterogeneousChainId(), withdrawalTxDTO.getHeterogeneousAssetId());
        CoinFrom withdrawalFeeCoinFrom = null;
        if (assembleCurrentAssetFee) {
            //手续费from 包含异构链补贴手续费
            withdrawalFeeCoinFrom = getWithdrawalFeeCoinFrom(chain, address, ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE);
        } else {
            //手续费from 不包含异构链补贴手续费
            withdrawalFeeCoinFrom = getWithdrawalFeeCoinFrom(chain, address);
        }
        List<CoinFrom> listFrom = new ArrayList<>();
        listFrom.add(withdrawalCoinFrom);
        listFrom.add(withdrawalFeeCoinFrom);

        //组装to
        List<CoinTo> listTo = new ArrayList<>();
        CoinTo withdrawalCoinTo = new CoinTo(
                AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId()),
                heterogeneousChainId,
                heterogeneousAssetId,
                amount);

        listTo.add(withdrawalCoinTo);
        // 判断组装异构链补贴手续费暂存to
        if (assembleCurrentAssetFee) {
            CoinTo withdrawalFeeCoinTo = new CoinTo(
                    AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId()),
                    chainId,
                    assetId,
                    ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE);
            listTo.add(withdrawalFeeCoinTo);
        }
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
     * @param heterogeneousAssetId
     * @return
     * @throws NulsException
     */
    private CoinFrom getWithdrawalCoinFrom(
            Chain chain,
            String address,
            BigInteger amount,
            int heterogeneousChainId,
            int heterogeneousAssetId) throws NulsException {
        //提现资产
        if (BigIntegerUtils.isEqualOrLessThan(amount, BigInteger.ZERO)) {
            chain.getLogger().error("提现金额不能小于0, amount:{}", amount);
            throw new NulsException(ConverterErrorCode.PARAMETER_ERROR);
        }
        NonceBalance withdrawalNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                heterogeneousChainId,
                heterogeneousAssetId,
                address);

        BigInteger withdrawalAssetBalance = withdrawalNonceBalance.getAvailable();

        if (BigIntegerUtils.isLessThan(withdrawalAssetBalance, amount)) {
            chain.getLogger().error("提现资产余额不足 chainId:{}, assetId:{}",
                    heterogeneousChainId, heterogeneousAssetId);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }

        return new CoinFrom(
                AddressTool.getAddress(address),
                heterogeneousChainId,
                heterogeneousAssetId,
                amount,
                withdrawalNonceBalance.getNonce(),
                (byte) 0);
    }

    /**
     * 组装提现交易手续费(包含链内打包手续费, 异构链补贴手续费)
     *
     * @param chain
     * @param address
     * @param withdrawalSignFeeNvt
     * @return
     * @throws NulsException
     */
    @Deprecated
    private CoinFrom getWithdrawalFeeCoinFrom(Chain chain, String address, BigInteger withdrawalSignFeeNvt) throws NulsException {
        int chainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                chainId,
                assetId,
                address);
        // 本链资产余额
        BigInteger balance = currentChainNonceBalance.getAvailable();

        // 总手续费 = 链内打包手续费 + 异构链转账(或签名)手续费[都以链内主资产结算]
        BigInteger totalFee = TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES.add(withdrawalSignFeeNvt);
        if (BigIntegerUtils.isLessThan(balance, totalFee)) {
            chain.getLogger().error("assemblyCoinFrom insufficient amount");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // 查询账本获取nonce值
        byte[] nonce = currentChainNonceBalance.getNonce();

        return new CoinFrom(AddressTool.getAddress(address), chainId, assetId, totalFee, nonce, (byte) 0);
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
            chain.getLogger().error("assemblyFeeCoinFrom insufficient amount");
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
    private byte[] assembleDistributionFeeCoinData(Chain chain, List<byte[]> listRewardAddress) throws NulsException {
        byte[] feeFromAdddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        List<CoinFrom> listFrom = assembleDistributionFeeCoinFrom(chain, feeFromAdddress);
        List<CoinTo> listTo = assembleDistributionFeeCoinTo(chain, listRewardAddress);
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
    private List<CoinFrom> assembleDistributionFeeCoinFrom(Chain chain, byte[] feeFromAdddress) throws NulsException {
        int assetChainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        //查询手续费暂存地址余额够不够
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                assetChainId,
                assetId,
                AddressTool.getStringAddressByBytes(feeFromAdddress));
        // 余额
        BigInteger balance = currentChainNonceBalance.getAvailable();
        //打包手续费
        if (BigIntegerUtils.isLessThan(balance, ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE)) {
            chain.getLogger().error("Distribution fee address insufficient balance. address:{}", AddressTool.getStringAddressByBytes(feeFromAdddress));
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // 查询账本获取nonce值
        byte[] nonce = currentChainNonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(feeFromAdddress,
                assetChainId,
                assetId,
                ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE,
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
    private List<CoinTo> assembleDistributionFeeCoinTo(Chain chain, List<byte[]> listRewardAddress) throws NulsException {
        // 计算 每个节点补贴多少手续费
        BigInteger count = BigInteger.valueOf(listRewardAddress.size());
        BigInteger amount = ConverterContext.WITHDRAWAL_DISTRIBUTION_FEE.divide(count);

        List<CoinTo> listTo = new ArrayList<>();
        for (byte[] address : listRewardAddress) {
            CoinTo distributionFeeCoinTo = new CoinTo(
                    address,
                    chain.getConfig().getChainId(),
                    chain.getConfig().getAssetId(),
                    amount);
            listTo.add(distributionFeeCoinTo);
        }
        return listTo;
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
     * @param signAccountDTO
     * @return
     * @throws NulsException
     */
    private Transaction assembleUnsignTxWithFee(Chain chain, int type, byte[] txData, SignAccountDTO signAccountDTO) throws NulsException {
        Transaction tx = assembleUnsignTxWithoutCoinData(type, txData);
        tx.setCoinData(assembleCoinData(chain, signAccountDTO));
        return tx;
    }

    /**
     * 组装手续费（CoinData）
     *
     * @param signAccountDTO
     * @return
     * @throws NulsException
     */
    private byte[] assembleCoinData(Chain chain, SignAccountDTO signAccountDTO) throws NulsException {
        String address = signAccountDTO.getAddress();
        //转账交易转出地址必须是本链地址
        if (!AddressTool.validAddress(chain.getChainId(), address)) {
            throw new NulsException(ConverterErrorCode.IS_NOT_CURRENT_CHAIN_ADDRESS);
        }
        NonceBalance nonceBalance = LedgerCall.getBalanceNonce(
                chain,
                chain.getConfig().getChainId(),
                chain.getConfig().getAssetId(),
                address);
        BigInteger balance = nonceBalance.getAvailable();
        if (BigIntegerUtils.isLessThan(balance, TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES)) {
            chain.getLogger().error("assemblyCoinFrom insufficient amount");
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        //查询账本获取nonce值
        byte[] nonce = nonceBalance.getNonce();
        CoinFrom coinFrom = new CoinFrom(
                AddressTool.getAddress(address),
                chain.getConfig().getChainId(),
                chain.getConfig().getAssetId(),
                TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES,
                nonce,
                NORMAL_TX_LOCKED);
        CoinData coinData = new CoinData();
        List<CoinFrom> froms = new ArrayList<>();
        froms.add(coinFrom);
        coinData.setFrom(froms);
        chain.getLogger().debug("CoinData:");
        chain.getLogger().debug(coinData.toString());
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    /**
     * 签名
     *
     * @param tx
     * @param signAccountDTO
     * @throws NulsException
     */
    private void signTx(Transaction tx, SignAccountDTO signAccountDTO) throws NulsException {
        if (null == signAccountDTO) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        TransactionSignature transactionSignature = new TransactionSignature();
        P2PHKSignature p2PHKSignature = AccountCall.signDigest(
                signAccountDTO.getAddress(),
                signAccountDTO.getPassword(),
                tx.getHash().getBytes());
        p2PHKSignatures.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        try {
            tx.setTransactionSignature(transactionSignature.serialize());
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    /**
     * 当前虚拟银行节点签名
     *
     * @param chain
     * @param tx
     */
    private void signTxCurrentVirtualBankAgent(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null == signAccountDTO) {
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_CONSENSUS_AGENT);
        }
        if (!chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
        }
        signTx(tx, signAccountDTO);
    }

}
