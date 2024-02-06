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
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;
import network.nerve.converter.model.txdata.OneClickCrossChainTxData;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * Confirming withdrawal transaction validator
 * (After creating the transaction)
 *
 * @author: Loki
 * @date: 2020/4/15
 */
@Component
public class ConfirmWithdrawalVerifier {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        byte[] coinData = tx.getCoinData();
        if (coinData != null && coinData.length > 0) {
            // coindataExisting data(coinDataThere should be no data available)
            throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
        }
        //Check for duplicate transactions within the block business
        ConfirmWithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmWithdrawalTxData.class);
        // Determine if there is already a corresponding confirmed withdrawal transaction for the withdrawal transaction
        ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, txData.getWithdrawalTxHash());
        if (null != po) {
            // Explain the withdrawal transaction Confirmed withdrawal transaction has already been sent out,This transaction is a duplicate confirmed withdrawal transaction
            // NerveWithdrawal transaction does not exist
            throw new NulsException(ConverterErrorCode.CFM_IS_DUPLICATION);
        }
        //Obtain withdrawal transactions
        Transaction withdrawalTx = TransactionCall.getConfirmedTx(chain, txData.getWithdrawalTxHash());
        if (null == withdrawalTx) {
            // NerveWithdrawal transaction does not exist
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        CoinData withdrawalCoinData = ConverterUtil.getInstance(withdrawalTx.getCoinData(), CoinData.class);
        CoinTo withdrawalTo = null;
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        for (CoinTo coinTo : withdrawalCoinData.getTo()) {
            if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                withdrawalTo = coinTo;
                break;
            }
        }
        int htgChainId = 0;
        String toAddress = null;
        if (TxType.WITHDRAWAL == withdrawalTx.getType()) {
            WithdrawalTxData txData1 = ConverterUtil.getInstance(withdrawalTx.getTxData(), WithdrawalTxData.class);
            htgChainId = txData1.getHeterogeneousChainId();
            toAddress = txData1.getHeterogeneousAddress();
        } else if (TxType.ONE_CLICK_CROSS_CHAIN == withdrawalTx.getType()) {
            OneClickCrossChainTxData txData1 = ConverterUtil.getInstance(withdrawalTx.getTxData(), OneClickCrossChainTxData.class);
            htgChainId = txData1.getDesChainId();
            toAddress = txData1.getDesToAddress();
        }
        // Based on withdrawal transactions Asset information acquisition heterogeneous chain transaction information(Conversion acquisition) Re validation
        HeterogeneousAssetInfo heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, withdrawalTo.getAssetsChainId(), withdrawalTo.getAssetsId());
        if(null == heterogeneousAssetInfo){
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        // Obtain corresponding withdrawal transactions in heterogeneous chains
        HeterogeneousTransactionInfo info = HeterogeneousUtil.getTxInfo(chain,
                heterogeneousAssetInfo.getChainId(),
                txData.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.WITHDRAWAL,
                this.heterogeneousDockingManager);
        if (null == info) {
            // Heterogeneous chain withdrawal transaction does not exist
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }

        // validate Heterogeneous Chain Signature List
        List<HeterogeneousAddress> listDistributionFee = txData.getListDistributionFee();
        if (null == listDistributionFee || listDistributionFee.isEmpty()) {
            // The heterogeneous chain signature list is empty
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY);
        }
        List<HeterogeneousAddress> listSigners = info.getSigners();
        if (!HeterogeneousUtil.listHeterogeneousAddressEquals(listDistributionFee, listSigners)) {
            // Heterogeneous chain signature list mismatch
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH);
        }

        if (!addressToLowerCase(toAddress).equals(addressToLowerCase(info.getTo()))) {
            // The data of the receiving address in the withdrawal transaction confirmation transaction does not match the receiving address in the heterogeneous confirmation transaction
            throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_ARRIVE_ADDRESS_MISMATCH);
        }

        if (txData.getHeterogeneousHeight() != info.getBlockHeight()) {
            // Withdrawal transaction confirmation transaction in progressheightConfirm transactions with heterogeneous entitiesheightData mismatch
            throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_HEIGHT_MISMATCH);
        }
        /**
         * Matching withdrawal amounts involves handling fee issues
         * At present, the transaction amount in the information obtained by heterogeneous chains includes transaction fees, So it's a comparison of the amount including handling fees
         */
        BigInteger fee = new BigInteger("0");
        BigInteger arrivedAmount = withdrawalTo.getAmount().subtract(fee);
        if (converterCoreApi.isProtocol22()) {
            // Amount Conversion for Cross Chain Assets with Different Precision
            arrivedAmount = converterCoreApi.checkDecimalsSubtractedToNerveForWithdrawal(heterogeneousAssetInfo, arrivedAmount);
            if (arrivedAmount.compareTo(info.getValue()) < 0) {
                // The amount in the withdrawal transaction confirmation transaction does not match the amount data of heterogeneous confirmation transactions
                throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH);
            }
        } else if (converterCoreApi.isSupportProtocol12ERC20OfTransferBurn()) {
            if (arrivedAmount.compareTo(info.getValue()) < 0) {
                // The amount in the withdrawal transaction confirmation transaction does not match the amount data of heterogeneous confirmation transactions
                throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH);
            }
        } else {
            if (arrivedAmount.compareTo(info.getValue()) != 0) {
                // The amount in the withdrawal transaction confirmation transaction does not match the amount data of heterogeneous confirmation transactions
                throw new NulsException(ConverterErrorCode.CFM_WITHDRAWAL_AMOUNT_MISMATCH);
            }
        }
    }

}
