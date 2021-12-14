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

package network.nerve.converter.core.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.RechargeTxData;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;

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
        if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
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
        if (info.getDepositIIMainAssetValue().compareTo(mainAmountTo.getAmount()) != 0) {
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
}
