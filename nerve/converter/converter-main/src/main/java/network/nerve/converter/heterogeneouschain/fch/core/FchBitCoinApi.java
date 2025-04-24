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
package network.nerve.converter.heterogeneouschain.fch.core;

import apipClass.TxInfo;
import fchClass.Cash;
import fchClass.CashMark;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.fch.helper.FchParseTxHelper;
import network.nerve.converter.heterogeneouschain.fch.utils.FchUtil;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgPendingTxHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgAccountStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.WithdrawalUTXO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class FchBitCoinApi extends BitCoinLibApi implements BeanInitial {
    private IHeterogeneousChainDocking docking;
    private FchWalletApi fchWalletApi;
    private FchParseTxHelper fchParseTxHelper;
    private HtgContext htgContext;
    private HtgInvokeTxHelper htgInvokeTxHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgAccountStorageService htgAccountStorageService;
    private HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    protected HtgCallBackManager htgCallBackManager;
    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected HtgListener htgListener;

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return fchWalletApi;
    }

    @Override
    protected long _calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {
        return FchUtil.calcFeeMultiSign(inputNum, outputNum, opReturnBytesLen[0], m, n);
    }

    @Override
    protected long _calcFeeMultiSignSizeWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        return FchUtil.calcFeeMultiSignWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, opReturnBytesLen[0], m, n);
    }

    @Override
    protected WithdrawalFeeLog _takeWithdrawalFeeLogFromTxParse(String htgTxHash, boolean nerveInner) throws Exception {
        TxInfo txInfo = fchWalletApi.getTransactionByHash(htgTxHash);
        if (txInfo.getHeight() <= 0) {
            return null;
        }
        ArrayList<CashMark> inputList = txInfo.getSpentCashes();
        ArrayList<CashMark> outputList = txInfo.getIssuedCashes();

        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (CashMark input : inputList) {
                String inputAddress = input.getOwner();
                if (htgListener.isListeningAddress(inputAddress)) {
                    txType = HeterogeneousChainTxType.WITHDRAW;
                    break OUT;
                }
            }
            for (CashMark output : outputList) {
                String outputAddress = output.getOwner();
                if (htgListener.isListeningAddress(outputAddress)) {
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    break OUT;
                }
            }
        } while (false);
        if (txType == null) {
            return null;
        }
        WithdrawalFeeLog feeLog = null;
        if (txType == HeterogeneousChainTxType.DEPOSIT) {
            BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) fchParseTxHelper.parseDepositTransaction(txInfo, null, true);
            if (po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                feeLog = new WithdrawalFeeLog(
                        txInfo.getHeight(), txInfo.getBlockId(), htgTxHash, htgContext.HTG_CHAIN_ID(), po.getValue().longValue(), true);
                feeLog.setTxTime(txInfo.getBlockTime());
            }
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            long fee = txInfo.getFee();
            feeLog = new WithdrawalFeeLog(
                    txInfo.getHeight(), txInfo.getBlockId(), htgTxHash, htgContext.HTG_CHAIN_ID(), fee, false);
            feeLog.setTxTime(txInfo.getBlockTime());
        }
        if (feeLog != null && htgContext.getConverterCoreApi().isProtocol36()) {
            feeLog.setNerveInner(nerveInner);
        }
        return feeLog;
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo(WithdrawalUTXO withdrawlUTXO) {
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        return FchUtil.makeChangeTxBaseInfo(htgContext, withdrawlUTXO, multiSignAddressPubs);
    }

    @Override
    public List<UTXOData> getUTXOs(String address) {
        List<Cash> accountUTXOs = fchWalletApi.getAccountUTXOs(address);
        Long bestHeight = fchWalletApi.getBestHeightForCurrentThread();
        return accountUTXOs.stream()
                .filter(cash -> cash.getBirthHeight() > 0)
                .filter(cash -> !("coinbase".equals(cash.getIssuer()) && bestHeight <= cash.getBirthHeight() + 14400))
                .map(cash -> new UTXOData(cash.getBirthTxId(), cash.getBirthIndex(), BigInteger.valueOf(cash.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws Exception {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getByzantineCount(n);
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            String signatures = FchUtil.signWithdraw(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    AddressTool.getAddressString(HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY()), htgContext.NERVE_CHAINID()),
                    toAddress,
                    value.longValue(),
                    withdrawlUTXO.getFeeRate(),
                    txHash,
                    m, n, false, getSplitGranularity());
            return signatures;
        } else {
            // sign machine support
            byte[] signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signFchWithdrawByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    HexUtil.encode(signerPub),
                    txHash,
                    toAddress,
                    value.longValue(),
                    withdrawlUTXO,
                    getSplitGranularity());
            return signatures;
        }
    }

    @Override
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws Exception {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getByzantineCount(n);
        return FchUtil.verifyWithdraw(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                withdrawlUTXO,
                signature,
                toAddress,
                amount.longValue(),
                withdrawlUTXO.getFeeRate(),
                txHash,
                m, n, false, getSplitGranularity());
    }

    @Override
    protected String _createMultiSignWithdrawTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String to, BigInteger _amount, String nerveTxHash, int assetId) throws Exception {
        long amount = _amount.longValue();
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size();
        int m = htgContext.getByzantineCount(n);

        return FchUtil.createMultiSignWithdrawTx(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                withdrawlUTXO, signatureData,
                to,
                amount,
                withdrawlUTXO.getFeeRate(),
                nerveTxHash,
                m, n, false, getSplitGranularity());
    }

    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws Exception {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        //if (coreApi.checkChangeP35(nerveTxHash)) {
        //    return nerveTxHash;
        //}
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return nerveTxHash;
        }
        Object[] baseInfo = this._makeChangeTxBaseInfo(withdrawlUTXO);
        List<byte[]> currentPubs = (List<byte[]>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        int m = (int) baseInfo[3];
        int n = (int) baseInfo[4];

        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return nerveTxHash;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        byte[] signerPub;
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            String nerveSigner = AddressTool.getAddressString(HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY()), htgContext.NERVE_CHAINID());
            WithdrawalUTXO cloneWithdrawalUTXO = withdrawlUTXO.clone();
            cloneWithdrawalUTXO.setPubs(currentPubs);
            String signatures = FchUtil.signWithdraw(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    cloneWithdrawalUTXO,
                    nerveSigner,
                    toAddress,
                    amount,
                    cloneWithdrawalUTXO.getFeeRate(),
                    nerveTxHash,
                    m, n, true, null);
            return signatures;
        } else {
            // sign machine support
            WithdrawalUTXO data = new WithdrawalUTXO(
                    nerveTxHash,
                    htgContext.HTG_CHAIN_ID(),
                    withdrawlUTXO.getCurrentMultiSignAddress(),
                    withdrawlUTXO.getCurrenVirtualBankTotal(),
                    withdrawlUTXO.getFeeRate(),
                    currentPubs,
                    UTXOList);
            signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signFchChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    HexUtil.encode(signerPub),
                    nerveTxHash,
                    toAddress,
                    amount,
                    data);
            return signatures;
        }
    }

    @Override
    public Boolean verifySignManagerChanges(String signAddress, String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signature) throws Exception {
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        Object[] baseInfo = this._makeChangeTxBaseInfo(withdrawlUTXO);
        List<byte[]> currentPubs = (List<byte[]>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        int m = (int) baseInfo[3];
        int n = (int) baseInfo[4];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }
        WithdrawalUTXO cloneWithdrawalUTXO = withdrawlUTXO.clone();
        cloneWithdrawalUTXO.setPubs(currentPubs);

        return FchUtil.verifyWithdraw(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO,
                signature,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null);
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws Exception {
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        Object[] baseInfo = this._makeChangeTxBaseInfo(withdrawlUTXO);
        List<byte[]> currentPubs = (List<byte[]>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        int m = (int) baseInfo[3];
        int n = (int) baseInfo[4];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }
        WithdrawalUTXO cloneWithdrawalUTXO = withdrawlUTXO.clone();
        cloneWithdrawalUTXO.setPubs(currentPubs);

        int verified = FchUtil.verifyWithdrawCount(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO,
                signatureData,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null);
        int byzantineCount = htgContext.getByzantineCount(currentPubs.size());
        return verified >= byzantineCount;
    }

    @Override
    protected String _createOrSignManagerChangesTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String nerveTxHash, Object[] baseInfo) throws Exception {
        WithdrawalUTXO cloneWithdrawalUTXO = withdrawlUTXO.clone();
        List<byte[]> currentPubs = (List<byte[]>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        int m = (int) baseInfo[3];
        int n = (int) baseInfo[4];
        cloneWithdrawalUTXO.setPubs(currentPubs);
        return FchUtil.createMultiSignWithdrawTx(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO, signatureData,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null);
    }

    @Override
    public long getWithdrawalFeeSize(long fromTotal, long transfer, long feeRate, int inputNum) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getByzantineCount(n);
        return FchUtil.calcFeeMultiSignWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, 64, m, n);
    }

    @Override
    public long getChangeFeeSize(int utxoSize) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getByzantineCount(n);
        long size = FchUtil.calcFeeMultiSign(utxoSize, 1, 64, m, n);
        return size;
    }

}

