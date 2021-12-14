/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.txdata.RechargeUnconfirmedTxData;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.math.BigInteger;

/**
 * 异构链充值待确认交易 验证
 * @author: Loki
 * @date: 2020/9/27
 */
@Component
public class RechargeUnconfirmedVerifier {

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
        RechargeUnconfirmedTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeUnconfirmedTxData.class);
        if(null == txData){
            throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        HeterogeneousHash originalTxHash = txData.getOriginalTxHash();
        String heterogeneousHash = originalTxHash.getHeterogeneousHash();
        if(null != rechargeStorageService.find(chain, heterogeneousHash)){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        int assetChainId = txData.getAssetChainId();
        int assetId = txData.getAssetId();

        // 通过链内资产id 获取异构链信息
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(originalTxHash.getHeterogeneousChainId(), assetChainId, assetId);

        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                heterogeneousAssetInfo.getChainId(),
                heterogeneousHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("Heterogeneous Transaction Info is null txHash:{}, heterogeneousHash:{}",
                    tx.getHash().toHex(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        String nerveToAddress = AddressTool.getStringAddressByBytes(txData.getNerveToAddress());
        BigInteger amount = txData.getAmount();
        heterogeneousRechargeValid(info, amount, nerveToAddress, heterogeneousAssetInfo);
    }

    public void validateProtocol16(Chain chain, Transaction tx) throws NulsException {
        RechargeUnconfirmedTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeUnconfirmedTxData.class);
        if(null == txData){
            throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        HeterogeneousHash originalTxHash = txData.getOriginalTxHash();
        String heterogeneousHash = originalTxHash.getHeterogeneousHash();
        int htgChainId = originalTxHash.getHeterogeneousChainId();
        if(null != rechargeStorageService.find(chain, heterogeneousHash)){
            // 该原始交易已执行过充值
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }

        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                htgChainId,
                heterogeneousHash,
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == info) {
            chain.getLogger().error("Heterogeneous Transaction Info is null txHash:{}, heterogeneousHash:{}",
                    tx.getHash().toHex(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        String nerveToAddress = AddressTool.getStringAddressByBytes(txData.getNerveToAddress());
        if (info.isDepositIIMainAndToken()) {
            // 校验同时充值token和main
            BigInteger mainAmount = txData.getMainAssetAmount();
            if (mainAmount == null) {
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            int mainAssetChainId = txData.getMainAssetChainId();
            int mainAssetId = txData.getMainAssetId();
            HeterogeneousAssetInfo mainInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, mainAssetChainId, mainAssetId);

            int tokenAssetChainId = txData.getAssetChainId();
            int tokenAssetId = txData.getAssetId();
            BigInteger tokenAmount = txData.getAmount();
            HeterogeneousAssetInfo tokenInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, tokenAssetChainId, tokenAssetId);
            heterogeneousRechargeValidForCrossOutII(info, tokenAmount, nerveToAddress, tokenInfo, mainAmount, mainInfo);
        } else {
            // 校验只充值token 或者 只充值 main
            int assetChainId = txData.getAssetChainId();
            int assetId = txData.getAssetId();
            // 通过链内资产id 获取异构链信息
            HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, assetChainId, assetId);
            BigInteger amount = txData.getAmount();
            heterogeneousRechargeValid(info, amount, nerveToAddress, heterogeneousAssetInfo);
        }
    }

    /**
     * 验证异构链充值交易
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValid(HeterogeneousTransactionInfo info, BigInteger amount, String toAddress, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // 充值资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(amount) != 0) {
            // 充值金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(toAddress)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * 验证异构链充值交易 - CrossOutII
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValidForCrossOutII(HeterogeneousTransactionInfo info, BigInteger tokenAmount, String toAddress, HeterogeneousAssetInfo tokenInfo, BigInteger mainAmount, HeterogeneousAssetInfo mainInfo) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // 充值token资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(tokenAmount) != 0) {
            // 充值token金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }

        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // 充值主资产错误
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getDepositIIMainAssetValue().compareTo(mainAmount) != 0) {
            // 充值主金额错误
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(toAddress)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

}
