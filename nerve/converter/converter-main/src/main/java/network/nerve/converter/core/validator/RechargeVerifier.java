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

package network.nerve.converter.core.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.model.txdata.RechargeTxData;
import network.nerve.converter.model.txdata.WithdrawalAddFeeByCrossChainTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * 充值交易业务验证器
 * (创建交易后)
 *
 * @author: Loki
 * @date: 2020/4/15
 */
@Component
public class RechargeVerifier {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        if (converterCoreApi.isProtocol16()) {
            this.validateProtocol16(chain, tx);
        } else {
            this._validate(chain, tx);
        }
    }

    public void _validate(Chain chain, Transaction tx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() > 1) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        CoinTo coinTo = listCoinTo.get(0);
        RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
        if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        // 通过链内资产id 获取异构链信息
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());

        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                heterogeneousAssetInfo.getChainId(),
                txData.getOriginalTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null != info) {
            heterogeneousRechargeValid(info, coinTo, heterogeneousAssetInfo);
        } else {
            // 查提案 (OriginalTxHash 不是异构链交易hash, 或可能是一个提案hash)
            ProposalPO proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
            if (null == proposalPO) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            proposalRechargeValid(chain, proposalPO, coinTo, heterogeneousAssetInfo);
        }

    }

    public void validateProtocol16(Chain chain, Transaction tx) throws NulsException {
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() > 2) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
        if(null != rechargeStorageService.find(chain, txData.getOriginalTxHash())){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                txData.getHeterogeneousChainId(),
                txData.getOriginalTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);

        if (null != info) {
            if (info.isDepositIIMainAndToken()) {
                // 校验同时充值token和main
                CoinTo tokenAmountTo, mainAmountTo;
                HeterogeneousAssetInfo tokenInfo, mainInfo;
                CoinTo coin0 = listCoinTo.get(0);
                HeterogeneousAssetInfo info0 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coin0.getAssetsChainId(), coin0.getAssetsId());
                CoinTo coin1 = listCoinTo.get(1);
                HeterogeneousAssetInfo info1 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coin1.getAssetsChainId(), coin1.getAssetsId());
                // 不能两个资产都是主资产, assetId:1 为主资产
                if (info0.getAssetId() == 1 && info1.getAssetId() == 1) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                if (info0.getAssetId() == 1) {
                    mainAmountTo = coin0;
                    mainInfo = info0;
                    tokenAmountTo = coin1;
                    tokenInfo = info1;
                } else if (info1.getAssetId() == 1){
                    mainAmountTo = coin1;
                    mainInfo = info1;
                    tokenAmountTo = coin0;
                    tokenInfo = info0;
                } else {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                this.heterogeneousRechargeValidForCrossOutII(info, tokenAmountTo, tokenInfo, mainAmountTo, mainInfo);
            } else {
                // 校验只充值token 或者 只充值 main
                if (listCoinTo.size() > 1) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
                }
                // 通过链内资产id 获取异构链信息
                CoinTo coinTo = listCoinTo.get(0);
                HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                heterogeneousRechargeValid(info, coinTo, heterogeneousAssetInfo);
            }
        } else {
            // 查提案 (OriginalTxHash 不是异构链交易hash, 或可能是一个提案hash)
            ProposalPO proposalPO = proposalStorageService.find(chain, NulsHash.fromHex(txData.getOriginalTxHash()));
            if (null == proposalPO) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
            }
            proposalRechargeValidProtocol16(chain, proposalPO, listCoinTo);
        }

    }

    /**
     * 验证异构链充值交易
     *
     * @param info
     * @param coinTo
     * @throws NulsException
     */
    private void heterogeneousRechargeValid(HeterogeneousTransactionInfo info, CoinTo coinTo, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // 充值资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger infoValue = info.getValue();
        if (converterCoreApi.isProtocol22()) {
            infoValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(heterogeneousAssetInfo, info.getValue());
        }
        if (infoValue.compareTo(coinTo.getAmount()) != 0) {
            // 充值金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * 验证异构链充值交易 - CrossOutII
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValidForCrossOutII(HeterogeneousTransactionInfo info, CoinTo tokenAmountTo, HeterogeneousAssetInfo tokenInfo, CoinTo mainAmountTo, HeterogeneousAssetInfo mainInfo) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // 充值token资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(tokenAmountTo.getAmount()) != 0) {
            // 充值token金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // 充值主资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger depositIIMainAssetValue = info.getDepositIIMainAssetValue();
        if (converterCoreApi.isProtocol22()) {
            depositIIMainAssetValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(mainInfo, depositIIMainAssetValue);
        }
        if (depositIIMainAssetValue.compareTo(mainAmountTo.getAmount()) != 0) {
            // 充值主金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(tokenAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(mainAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    private void heterogeneousRechargeValidForOneClickCrossChain(Chain chain, HeterogeneousTransactionInfo info, CoinTo tokenAmountTo, HeterogeneousAssetInfo tokenInfo, CoinTo mainAmountTo, HeterogeneousAssetInfo mainInfo, CoinTo coinTipping) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // 充值token资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger tipping = BigInteger.ZERO;
        if (coinTipping != null) {
            tipping = coinTipping.getAmount();
        }
        if (info.getValue().compareTo(tokenAmountTo.getAmount().add(tipping)) != 0) {
            // 充值token金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }

        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // 充值主资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger depositIIMainAssetValue = info.getDepositIIMainAssetValue();
        if (converterCoreApi.isProtocol22()) {
            depositIIMainAssetValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(mainInfo, depositIIMainAssetValue);
        }
        if (depositIIMainAssetValue.compareTo(mainAmountTo.getAmount()) != 0) {
            // 充值主金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(tokenAmountTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * 验证提案转到其他地址
     *
     * @param chain
     * @param proposalPO
     * @param coinTo
     * @throws NulsException
     */
    private void proposalRechargeValid(Chain chain, ProposalPO proposalPO, CoinTo coinTo, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (ProposalTypeEnum.TRANSFER.value() != proposalPO.getType()) {
            // 提案类型错误
            chain.getLogger().error("Proposal type is not transfer");
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // 虚拟银行节点签名数需要达到的拜占庭数
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (proposalPO.getFavorNumber() < byzantineCount) {
            // 提案没有投票通过
            chain.getLogger().error("Proposal type was rejected");
            throw new NulsException(ConverterErrorCode.PROPOSAL_REJECTED);
        }

        // 获取提案中的异构链充值交易
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                proposalPO.getHeterogeneousChainId(),
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }

        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // 充值资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
            // 充值金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!Arrays.equals(proposalPO.getAddress(), coinTo.getAddress())) {
            // 充值交易到账地址不是提案中的到账地址
            chain.getLogger().error("充值交易到账地址不是提案中的到账地址");
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    private void proposalRechargeValidProtocol16(Chain chain, ProposalPO proposalPO, List<CoinTo> listCoinTo) throws NulsException {
        if (ProposalTypeEnum.TRANSFER.value() != proposalPO.getType()) {
            // 提案类型错误
            chain.getLogger().error("Proposal type is not transfer");
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // 虚拟银行节点签名数需要达到的拜占庭数
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        if (proposalPO.getFavorNumber() < byzantineCount) {
            // 提案没有投票通过
            chain.getLogger().error("Proposal type was rejected");
            throw new NulsException(ConverterErrorCode.PROPOSAL_REJECTED);
        }

        // 获取提案中的异构链充值交易
        int htgChainId = proposalPO.getHeterogeneousChainId();
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                proposalPO.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", proposalPO.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        if (info.isDepositIIMainAndToken()) {
            if (listCoinTo.size() != 2) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            // 校验同时充值token和main
            CoinTo tokenAmountTo, mainAmountTo;
            HeterogeneousAssetInfo tokenInfo, mainInfo;
            CoinTo coin0 = listCoinTo.get(0);
            HeterogeneousAssetInfo info0 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coin0.getAssetsChainId(), coin0.getAssetsId());
            CoinTo coin1 = listCoinTo.get(1);
            HeterogeneousAssetInfo info1 = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coin1.getAssetsChainId(), coin1.getAssetsId());
            // 必须为token和main资产
            if (info0.getAssetId() == 1 && info1.getAssetId() == 1) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info0.getAssetId() == 1) {
                mainAmountTo = coin0;
                mainInfo = info0;
                tokenAmountTo = coin1;
                tokenInfo = info1;
            } else if (info1.getAssetId() == 1){
                mainAmountTo = coin1;
                mainInfo = info1;
                tokenAmountTo = coin0;
                tokenInfo = info0;
            } else {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }

            if (info.getAssetId() != tokenInfo.getAssetId()) {
                // 充值token资产错误
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getValue().compareTo(tokenAmountTo.getAmount()) != 0) {
                // 充值token金额错误
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
                // 充值主资产错误
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getDepositIIMainAssetValue().compareTo(mainAmountTo.getAmount()) != 0) {
                // 充值主金额错误
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), tokenAmountTo.getAddress())) {
                chain.getLogger().error("充值交易到账地址不是提案中的到账地址");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), mainAmountTo.getAddress())) {
                chain.getLogger().error("充值交易到账地址不是提案中的到账地址");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        } else {
            CoinTo coinTo = listCoinTo.get(0);
            HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
            if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
                // 充值资产错误
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
                // 充值金额错误
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!Arrays.equals(proposalPO.getAddress(), coinTo.getAddress())) {
                // 充值交易到账地址不是提案中的到账地址
                chain.getLogger().error("充值交易到账地址不是提案中的到账地址");
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        }
    }

    public void validateOneClickCrossChain(Chain chain, Transaction tx) throws NulsException {
        // 验证一键跨链交易, 验证提现金额, 手续费金额, tipping数据, 目标链信息
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || (listCoinTo.size() != 2 && listCoinTo.size() != 3)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        OneClickCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainTxData.class);
        HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
        String originalTxHash = heterogeneousHash.getHeterogeneousHash();
        int htgChainId = heterogeneousHash.getHeterogeneousChainId();

        if(null != rechargeStorageService.find(chain, originalTxHash)){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), originalTxHash);
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                originalTxHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (info == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String extend = info.getDepositIIExtend();
        HeterogeneousOneClickCrossChainData data = docking.parseOneClickCrossChainData(extend);
        if (data == null) {
            chain.getLogger().error("[{}][一键跨链]交易验证失败, originalTxHash: {}, extend: {}", htgChainId, extend);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        BigInteger tipping = data.getTipping();
        boolean hasTipping = tipping.compareTo(BigInteger.ZERO) > 0;
        if (hasTipping) {
            if (listCoinTo.size() != 3) {
                throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
            }
        } else {
            if (listCoinTo.size() != 2) {
                throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
            }
        }
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        CoinTo coinWithdrawal = null, coinFee = null, coinTipping = null;
        for (CoinTo to : listCoinTo) {
            if (Arrays.equals(withdrawalBlackhole, to.getAddress())) {
                coinWithdrawal = to;
            } else if (Arrays.equals(withdrawalFeeAddress, to.getAddress())) {
                coinFee = to;
            } else {
                coinTipping = to;
            }
        }
        if (coinWithdrawal == null || coinFee == null || (hasTipping && coinTipping == null)) {
            chain.getLogger().error("[{}][一键跨链]交易验证失败, CoinData缺失, originalTxHash: {}", htgChainId, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        String desToAddress = data.getDesToAddress();
        desToAddress = ConverterUtil.addressToLowerCase(desToAddress);
        int desChainId = txData.getDesChainId();
        if (txData.getDesChainId() != data.getDesChainId() ||
                !txData.getDesToAddress().equals(desToAddress) ||
                coinFee.getAmount().compareTo(data.getFeeAmount()) != 0) {
            chain.getLogger().error("[{}][一键跨链]交易验证失败, 目标链信息错误, feeAmount: {}, desId: {}, desAddress: {}, originalTxHash: {}", htgChainId, coinFee.getAmount(), txData.getDesChainId(), txData.getDesToAddress(), originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        BigInteger tippingFromCoin = BigInteger.ZERO;
        if (coinTipping != null) {
            tippingFromCoin = coinTipping.getAmount();
            if (coinWithdrawal.getAssetsChainId() != coinTipping.getAssetsChainId() || coinWithdrawal.getAssetsId() != coinTipping.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
        }
        // Nerve一键跨链交易，必有一个资产是主资产，作为跨到目标链的手续费
        boolean hasToken = info.isIfContractAsset();
        if (hasToken) {
            // 源链交易一定充值了token
            HeterogeneousAssetInfo infoWithdrawal = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinWithdrawal.getAssetsChainId(), coinWithdrawal.getAssetsId());
            HeterogeneousAssetInfo infoFee = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinFee.getAssetsChainId(), coinFee.getAssetsId());
            // 跨链资产一定是token资产, 手续费资产一定是主资产, assetId:1 为主资产
            if (infoWithdrawal.getAssetId() == 1 || infoFee.getAssetId() != 1) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            if (info.isDepositIIMainAndToken()) {
                // 校验同时充值token和main
                this.heterogeneousRechargeValidForOneClickCrossChain(chain, info, coinWithdrawal, infoWithdrawal, coinFee, infoFee, coinTipping);
            } else {
                // 只充值了token
                if (info.getAssetId() != infoWithdrawal.getAssetId()) {
                    // 充值资产错误
                    throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
                }
                if (info.getValue().compareTo(coinWithdrawal.getAmount().add(tippingFromCoin)) != 0) {
                    // 充值金额错误
                    throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
                }
                if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinWithdrawal.getAddress()))) {
                    throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
                }
                if (coinFee.getAmount().compareTo(BigInteger.ZERO) != 0) {
                    // 手续费金额错误
                    throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_FEE_ERROR);
                }
            }
        } else {
            // 源链只充值了主资产
            if (coinWithdrawal.getAssetsChainId() != coinFee.getAssetsChainId() || coinWithdrawal.getAssetsId() != coinFee.getAssetsId()) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            HeterogeneousAssetInfo infoMain = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinFee.getAssetsChainId(), coinFee.getAssetsId());
            if (info.getAssetId() != infoMain.getAssetId()) {
                // 充值资产错误
                throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
            }
            BigInteger infoValue = info.getValue();
            if (converterCoreApi.isProtocol22()) {
                infoValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(infoMain, infoValue);
            }
            if (infoValue.compareTo(coinWithdrawal.getAmount().add(coinFee.getAmount()).add(tippingFromCoin)) != 0) {
                // 充值金额错误
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinWithdrawal.getAddress()))) {
                throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
            }
        }
        // 检查目标链数据
        IHeterogeneousChainDocking desDocking = heterogeneousDockingManager.getHeterogeneousDocking(desChainId);
        // 检查充值资产是否能跨链到目标链
        HeterogeneousAssetInfo desChainAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(desChainId, coinWithdrawal.getAssetsChainId(), coinWithdrawal.getAssetsId());
        if (desChainAssetInfo == null) {
            chain.getLogger().error("[{}]OneClickCrossChain des error, desChainId:{}, desToAddress:{}, originalTxHash: {}", htgChainId, desChainId, desToAddress, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
        // 检查目标链地址是否合法
        boolean validateAddress = desDocking.validateAddress(desToAddress);
        if (!validateAddress) {
            chain.getLogger().error("[{}]OneClickCrossChain desToAddress error, desChainId:{}, desToAddress:{}, originalTxHash: {}", htgChainId, desChainId, desToAddress, originalTxHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
    }

    public void validateAddFeeCrossChain(Chain chain, Transaction tx) throws NulsException {
        // 跨链追加手续费的业务校验
        String hash = tx.getHash().toHex();
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
            throw new NulsException(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM);
        }
        List<CoinTo> listCoinTo = coinData.getTo();
        if (null == listCoinTo || listCoinTo.size() != 1) {
            throw new NulsException(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO);
        }
        WithdrawalAddFeeByCrossChainTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalAddFeeByCrossChainTxData.class);
        HeterogeneousHash heterogeneousHash = txData.getHtgTxHash();
        String originalTxHash = heterogeneousHash.getHeterogeneousHash();
        int htgChainId = heterogeneousHash.getHeterogeneousChainId();

        if(null != rechargeStorageService.find(chain, originalTxHash)){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    hash, originalTxHash);
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                originalTxHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (info == null) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String extend = info.getDepositIIExtend();
        HeterogeneousAddFeeCrossChainData data = docking.parseAddFeeCrossChainData(extend);
        if (data == null) {
            chain.getLogger().error("[{}][跨链追加手续费]交易验证失败, originalTxHash: {}, extend: {}", htgChainId, extend);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TX_ERROR);
        }
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        CoinTo coinTo = listCoinTo.get(0);
        if (!Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
            chain.getLogger().error("[{}][跨链追加手续费]交易验证失败, 手续费收集地址与追加交易to地址不匹配, toAddress:{}, withdrawalFeeAddress:{} ",
                    htgChainId,
                    AddressTool.getStringAddressByBytes(coinTo.getAddress()),
                    AddressTool.getStringAddressByBytes(withdrawalFeeAddress));
            throw new NulsException(ConverterErrorCode.DISTRIBUTION_ADDRESS_MISMATCH);
        }
        if (info.isIfContractAsset()) {
            throw new NulsException(ConverterErrorCode.ADD_FEE_CROSS_CHAIN_COIN_ERROR);
        }
        HeterogeneousAssetInfo infoMain = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
        if (info.getAssetId() != infoMain.getAssetId()) {
            // 充值资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
            // 充值金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
        // 验证提现交易
        String basicTxHash = txData.getNerveTxHash();
        if (StringUtils.isBlank(basicTxHash)) {
            chain.getLogger().error("要追加手续费的原始提现交易hash不存在! " + ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST.getMsg());
            // 提现交易hash
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        Transaction basicTx = TransactionCall.getConfirmedTx(chain, basicTxHash);
        if (null == basicTx) {
            chain.getLogger().error("原始交易不存在 , hash:{}", basicTxHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        if (basicTx.getType() != TxType.WITHDRAWAL && basicTx.getType() != TxType.ONE_CLICK_CROSS_CHAIN) {
            // 不是提现交易
            chain.getLogger().error("txdata对应的交易不是提现交易/一键跨链 , hash:{}", basicTxHash);
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        int feeAssetChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        if (basicTx.getType() == TxType.WITHDRAWAL || basicTx.getType() == TxType.ONE_CLICK_CROSS_CHAIN) {
            // 判断该提现交易是否已经有对应的确认提现交易
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, basicTx.getHash());
            if (null != po) {
                // 说明该提现交易 已经发出过确认提现交易, 不能再追加手续费
                chain.getLogger().error("该提现交易已经完成,不能再追加异构链提现手续费, withdrawalTxhash:{}, hash:{}", basicTxHash, hash);
                throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
            }
            // 提现交易的手续费资产ID
            CoinData basicCoinData = ConverterUtil.getInstance(basicTx.getCoinData(), CoinData.class);
            Coin feeCoin = null;
            for (CoinTo basicCoinTo : basicCoinData.getTo()) {
                if (Arrays.equals(withdrawalFeeAddress, basicCoinTo.getAddress())) {
                    feeCoin = basicCoinTo;
                    break;
                }
            }
            feeAssetChainId = feeCoin.getAssetsChainId();
            feeAssetId = feeCoin.getAssetsId();
        }

        // 检查追加的手续费资产，必须与提现交易的手续费资产一致
        if (coinTo.getAssetsChainId() != feeAssetChainId || coinTo.getAssetsId() != feeAssetId) {
            chain.getLogger().error("追加交易资产必须与提现交易的手续费资产一致, AssetsChainId:{}, AssetsId:{}",
                    coinTo.getAssetsChainId(), coinTo.getAssetsId());
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_ADDITIONAL_FEE_COIN_ERROR);
        }
    }
}
