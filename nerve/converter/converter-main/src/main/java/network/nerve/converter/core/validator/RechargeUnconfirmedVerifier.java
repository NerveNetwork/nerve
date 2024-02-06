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
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.txdata.OneClickCrossChainUnconfirmedTxData;
import network.nerve.converter.model.txdata.RechargeUnconfirmedTxData;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Heterogeneous chain recharge pending confirmation transaction validate
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
            // The original transaction has already been recharged
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business) txHash:{}, originalTxHash:{}",
                    tx.getHash().toHex(), txData.getOriginalTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        int assetChainId = txData.getAssetChainId();
        int assetId = txData.getAssetId();

        // Through in chain assetsid Obtain heterogeneous chain information
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
            // The original transaction has already been recharged
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
            // Verify simultaneous rechargetokenandmain
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
            // Verify only rechargetoken perhaps Recharge only main
            int assetChainId = txData.getAssetChainId();
            int assetId = txData.getAssetId();
            // Through in chain assetsid Obtain heterogeneous chain information
            HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, assetChainId, assetId);
            BigInteger amount = txData.getAmount();
            heterogeneousRechargeValid(info, amount, nerveToAddress, heterogeneousAssetInfo);
        }
    }

    /**
     * Verify heterogeneous chain recharge transactions
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValid(HeterogeneousTransactionInfo info, BigInteger amount, String toAddress, HeterogeneousAssetInfo heterogeneousAssetInfo) throws NulsException {
        if (info.getAssetId() != heterogeneousAssetInfo.getAssetId()) {
            // Recharge asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger infoValue = info.getValue();
        if (converterCoreApi.isProtocol22()) {
            infoValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(heterogeneousAssetInfo, info.getValue());
        }
        if (infoValue.compareTo(amount) != 0) {
            // Recharge amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(toAddress)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    /**
     * Verify heterogeneous chain recharge transactions - CrossOutII
     *
     * @throws NulsException
     */
    private void heterogeneousRechargeValidForCrossOutII(HeterogeneousTransactionInfo info, BigInteger tokenAmount, String toAddress, HeterogeneousAssetInfo tokenInfo, BigInteger mainAmount, HeterogeneousAssetInfo mainInfo) throws NulsException {
        if (info.getAssetId() != tokenInfo.getAssetId()) {
            // RechargetokenAsset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        if (info.getValue().compareTo(tokenAmount) != 0) {
            // RechargetokenAmount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (info.getDepositIIMainAssetAssetId() != mainInfo.getAssetId()) {
            // Recharge main asset error
            throw new NulsException(ConverterErrorCode.RECHARGE_ASSETID_ERROR);
        }
        BigInteger depositIIMainAssetValue = info.getDepositIIMainAssetValue();
        if (converterCoreApi.isProtocol22()) {
            depositIIMainAssetValue = converterCoreApi.checkDecimalsSubtractedToNerveForDeposit(mainInfo, depositIIMainAssetValue);
        }
        if (depositIIMainAssetValue.compareTo(mainAmount) != 0) {
            // Recharge main amount error
            throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
        }
        if (!info.getNerveAddress().equals(toAddress)) {
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
    }

    public void validateOneClickCrossChain(Chain chain, Transaction tx) throws NulsException {
        OneClickCrossChainUnconfirmedTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainUnconfirmedTxData.class);
        if(null == txData){
            throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        HeterogeneousHash originalTxHash = txData.getOriginalTxHash();
        String heterogeneousHash = originalTxHash.getHeterogeneousHash();
        int htgChainId = originalTxHash.getHeterogeneousChainId();
        if(null != rechargeStorageService.find(chain, heterogeneousHash)){
            // The original transaction has already been recharged
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
            chain.getLogger().error("[{}]Heterogeneous Transaction Info is null txHash:{}, heterogeneousHash:{}",
                    htgChainId, tx.getHash().toHex(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        String nerveToAddress = AddressTool.getStringAddressByBytes(txData.getNerveToAddress());
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        // One click cross chainnerveThe receiving address can only be a black hole address
        if (!Arrays.equals(txData.getNerveToAddress(), withdrawalBlackhole)) {
            chain.getLogger().error("[{}]OneClickCrossChain Nerve address error:{}, heterogeneousHash:{}", htgChainId, nerveToAddress, heterogeneousHash);
            throw new NulsException(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR);
        }
        int desChainId = txData.getDesChainId();
        int crossAssetChainId, crossAssetId;
        BigDecimal crossValue;
        if (info.isDepositIIMainAndToken()) {
            // Verify simultaneous rechargetokenandmain
            BigInteger mainAmount = txData.getMainAssetAmount();
            if (mainAmount == null) {
                throw new NulsException(ConverterErrorCode.RECHARGE_AMOUNT_ERROR);
            }
            int mainAssetChainId = txData.getMainAssetChainId();
            int mainAssetId = txData.getMainAssetId();
            HeterogeneousAssetInfo mainInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, mainAssetChainId, mainAssetId);

            int tokenAssetChainId = txData.getErc20AssetChainId();
            int tokenAssetId = txData.getErc20AssetId();
            BigInteger tokenAmount = txData.getErc20Amount();
            HeterogeneousAssetInfo tokenInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, tokenAssetChainId, tokenAssetId);
            heterogeneousRechargeValidForCrossOutII(info, tokenAmount, nerveToAddress, tokenInfo, mainAmount, mainInfo);
            crossAssetChainId = tokenAssetChainId;
            crossAssetId = tokenAssetId;
            crossValue = new BigDecimal(tokenAmount);
        } else {
            // Verify only rechargetoken perhaps Recharge only main
            int assetChainId,assetId;
            BigInteger amount;
            if (txData.getErc20AssetChainId() > 0) {
                assetChainId = txData.getErc20AssetChainId();
                assetId = txData.getErc20AssetId();
                amount = txData.getErc20Amount();
            } else {
                assetChainId = txData.getMainAssetChainId();
                assetId = txData.getMainAssetId();
                amount = txData.getMainAssetAmount();
            }
            // Through in chain assetsid Obtain heterogeneous chain information
            HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, assetChainId, assetId);
            heterogeneousRechargeValid(info, amount, nerveToAddress, heterogeneousAssetInfo);
            crossAssetChainId = assetChainId;
            crossAssetId = assetId;
            crossValue = new BigDecimal(amount);
        }
        // tipping check
        BigInteger tipping = txData.getTipping();
        if (tipping.compareTo(BigInteger.ZERO) > 0) {
            // LegitimateNerveaddress
            if (!converterCoreApi.validNerveAddress(txData.getTippingAddress())) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address error:{}, heterogeneousHash:{}", htgChainId, txData.getTippingAddress(), heterogeneousHash);
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            byte[] tippingAddress = AddressTool.getAddress(txData.getTippingAddress());
            if (Arrays.equals(tippingAddress, withdrawalBlackhole) || Arrays.equals(tippingAddress, withdrawalFeeAddress)) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping address setting error:{}, heterogeneousHash:{}", htgChainId, txData.getTippingAddress(), heterogeneousHash);
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
            // Must not exceed the value of cross chain assets10%
            if (crossValue.multiply(HtgConstant.NUMBER_0_DOT_1).compareTo(new BigDecimal(tipping)) < 0) {
                chain.getLogger().error("[{}]OneClickCrossChain tipping exceed error:{}, crossValue:{}, heterogeneousHash:{}", htgChainId, tipping, crossValue.toPlainString(), heterogeneousHash);
                throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_TIPPING_ERROR);
            }
        }
        // Check target chain data
        IHeterogeneousChainDocking desDocking = heterogeneousDockingManager.getHeterogeneousDocking(desChainId);
        // Check if the recharged assets can cross the chain to the target chain
        HeterogeneousAssetInfo desChainAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(desChainId, crossAssetChainId, crossAssetId);
        if (desChainAssetInfo == null) {
            chain.getLogger().error("[{}]OneClickCrossChain des error, desChainId:{}, desToAddress:{}, heterogeneousHash:{}", htgChainId, txData.getDesChainId(), txData.getDesToAddress(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
        // Check if the target chain address is legal
        boolean validateAddress = desDocking.validateAddress(txData.getDesToAddress());
        if (!validateAddress) {
            chain.getLogger().error("[{}]OneClickCrossChain desToAddress error, desChainId:{}, desToAddress:{}, heterogeneousHash:{}", htgChainId, txData.getDesChainId(), txData.getDesToAddress(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_DES_ERROR);
        }
        // Verify if cross chain transaction fees are sufficient
        if (txData.getMainAssetFeeAmount().compareTo(txData.getMainAssetAmount()) > 0) {
            chain.getLogger().error("[{}]OneClickCrossChain fee error, feeAmount:{}, totalAmount:{}, heterogeneousHash: {}", htgChainId, txData.getMainAssetFeeAmount(), txData.getMainAssetAmount(), heterogeneousHash);
            throw new NulsException(ConverterErrorCode.ONE_CLICK_CROSS_CHAIN_FEE_ERROR);
        }
    }

}
