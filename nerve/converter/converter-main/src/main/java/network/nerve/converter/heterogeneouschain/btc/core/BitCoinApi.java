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
package network.nerve.converter.heterogeneouschain.btc.core;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BtcSignData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.ECKey;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class BitCoinApi extends BitCoinLibApi {
    private IHeterogeneousChainDocking btcDocking;
    private BitCoinLibWalletApi btcWalletApi;
    private BtcParseTxHelper btcParseTxHelper;
    private HtgContext htgContext;
    protected HtgListener htgListener;

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return btcWalletApi;
    }

    @Override
    protected String _createMultiSignWithdrawTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String to, long amount, String nerveTxHash) throws Exception {
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }

        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        // take pubkeys of all managers
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);

        Transaction tx = BitCoinLibUtil.createNativeSegwitMultiSignTx(
                signatures, pubEcKeys,
                amount,
                to,
                UTXOList,
                List.of(HexUtil.decode(nerveTxHash)),
                m, n,
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(),
                getSplitGranularity()
        );
        return HexUtil.encode(tx.serialize());
    }

    @Override
    protected long _calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {
        return BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(inputNum, outputNum, opReturnBytesLen, m, n);
    }

    @Override
    protected long _calcFeeMultiSignSizeWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        return BitCoinLibUtil.calcFeeMultiSignSizeP2WSHWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, opReturnBytesLen, m, n);
    }

    @Override
    protected WithdrawalFeeLog _takeWithdrawalFeeLogFromTxParse(String htgTxHash, boolean nerveInner) throws Exception {
        RawTransaction txInfo = btcWalletApi.getTransactionByHash(htgTxHash);
        if (txInfo.getConfirmations() == null || txInfo.getConfirmations().intValue() == 0) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        List<RawInput> inputList = txInfo.getVIn();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (RawInput input : inputList) {
                String inputAddress = BitCoinLibUtil.takeMultiSignAddressWithP2WSH(input, htgContext.getConverterCoreApi().isNerveMainnet());
                if (htgListener.isListeningAddress(inputAddress)) {
                    txType = HeterogeneousChainTxType.WITHDRAW;
                    break OUT;
                }
            }
            for (RawOutput output : outputList) {
                String outputAddress = output.getScriptPubKey().getAddress();
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
            BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) btcParseTxHelper.parseDepositTransaction(txInfo, null, true);
            if (po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                feeLog = new WithdrawalFeeLog(
                        po.getBlockHeight(), po.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), po.getValue().longValue(), true);
                feeLog.setTxTime(txInfo.getBlockTime());
            }
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            long fee = BitCoinLibUtil.calcTxFee(txInfo, btcWalletApi);
            feeLog = new WithdrawalFeeLog(
                    Long.valueOf(btcWalletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()), txInfo.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), fee, false);
            feeLog.setTxTime(txInfo.getBlockTime());
        }
        if (feeLog != null && htgContext.getConverterCoreApi().isProtocol36()) {
            feeLog.setNerveInner(nerveInner);
        }
        return feeLog;
    }

    @Override
    protected String _createOrSignManagerChangesTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String nerveTxHash, Object[] baseInfo) throws Exception {
        long amount = (long) baseInfo[1];
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }

        Transaction tx = BitCoinLibUtil.createNativeSegwitMultiSignTx(
                signatures,
                (List<ECKey>) baseInfo[0],
                amount,
                (String) baseInfo[2],
                withdrawlUTXO.getUtxoDataList(),
                (List<byte[]>) baseInfo[3],
                (int) baseInfo[4],
                (int) baseInfo[5],
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(), true, null);
        return HexUtil.encode(tx.serialize());
    }

    @Override
    public List<UTXOData> getUTXOs(String address) {
        return btcWalletApi.getAccountUTXOs(address);
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();

        // take pubkeys of all managers
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);
        byte[] signerPub;
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            HeterogeneousAccount account = btcDocking.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            // take pri from local
            ECKey pri = ECKey.fromPrivate(account.getPriKey());
            List<String> signatures = BitCoinLibUtil.createNativeSegwitMultiSignByOne(
                    pri, pubEcKeys,
                    value.longValue(),
                    toAddress,
                    UTXOList,
                    List.of(HexUtil.decode(txHash)),
                    m, n,
                    withdrawlUTXO.getFeeRate(),
                    htgContext.getConverterCoreApi().isNerveMainnet(),
                    getSplitGranularity()
            );
            signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            try {
                return HexUtil.encode(signData.serialize());
            } catch (IOException e) {
                throw new NulsException(ConverterErrorCode.IO_ERROR, e);
            }
        } else {
            // sign machine support
            signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signBtcWithdrawByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
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
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws NulsException {
        BtcSignData signData = new BtcSignData();
        signData.parse(HexUtil.decode(signature), 0);

        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        if (withdrawlUTXO == null) {
            return false;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        ECKey pub = ECKey.fromPublicOnly(signData.getPubkey());
        // take pubkeys of all managers
        List<ECKey> pubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);

        return BitCoinLibUtil.verifyNativeSegwitMultiSign(
                pub,
                signData.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()),
                pubEcKeys,
                amount.longValue(),
                toAddress,
                UTXOList,
                List.of(HexUtil.decode(txHash)),
                m, n,
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(),
                getSplitGranularity());
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo (WithdrawalUTXO withdrawlUTXO) {
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        return BitCoinLibUtil.makeChangeTxBaseInfo(htgContext, withdrawlUTXO, multiSignAddressPubs);
    }

    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws NulsException {
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
        List<ECKey> currentPubs = (List<ECKey>) baseInfo[0];
        long amount = (long) baseInfo[1];
        String toAddress = (String) baseInfo[2];
        List<byte[]> opReturns = (List<byte[]>) baseInfo[3];
        int m = (int) baseInfo[4];
        int n = (int) baseInfo[5];

        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return nerveTxHash;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        byte[] signerPub;
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            HeterogeneousAccount account = btcDocking.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            // take pri from local
            ECKey pri = ECKey.fromPrivate(account.getPriKey());
            /*// take pubkeys of all managers
            List<ECKey> newPubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());

            List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
            List<ECKey> oldPubEcKeys = multiSignAddressPubs.stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());

            String toAddress = BitCoinLibUtil.getNativeSegwitMultiSignAddress(coreApi.getByzantineCount(newPubEcKeys.size()), newPubEcKeys, coreApi.isNerveMainnet());
            // calc the min number of signatures
            int n = oldPubEcKeys.size(), m = coreApi.getByzantineCount(n);
            byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
            long fee = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[]{nerveTxHashBytes.length}, m, n);
            long totalMoney = 0;
            for (int k = 0; k < UTXOList.size(); k++) {
                totalMoney += UTXOList.get(k).getAmount().longValue();
            }*/
            List<String> signatures = BitCoinLibUtil.createNativeSegwitMultiSignByOne(
                    pri,
                    currentPubs,
                    amount,
                    toAddress,
                    UTXOList,
                    opReturns,
                    m,
                    n,
                    withdrawlUTXO.getFeeRate(),
                    coreApi.isNerveMainnet(),
                    true,
                    null
            );
            signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            try {
                return HexUtil.encode(signData.serialize());
            } catch (IOException e) {
                throw new NulsException(ConverterErrorCode.IO_ERROR, e);
            }
        } else {
            // sign machine support
            WithdrawalUTXO data = new WithdrawalUTXO(
                    nerveTxHash,
                    htgContext.HTG_CHAIN_ID(),
                    withdrawlUTXO.getCurrentMultiSignAddress(),
                    withdrawlUTXO.getCurrenVirtualBankTotal(),
                    withdrawlUTXO.getFeeRate(),
                    currentPubs.stream().map(p -> p.getPubKey()).collect(Collectors.toList()),
                    UTXOList);
            signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signBtcChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
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
    public Boolean verifySignManagerChanges(String signAddress, String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signature) throws NulsException {
        //if (htgContext.getConverterCoreApi().checkChangeP35(nerveTxHash)) {
        //    return true;
        //}
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        Object[] baseInfo = this._makeChangeTxBaseInfo(withdrawlUTXO);
        long amount = (long) baseInfo[1];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }

        BtcSignData signData = new BtcSignData();
        signData.parse(HexUtil.decode(signature), 0);

        ECKey pub = ECKey.fromPublicOnly(signData.getPubkey());

        return BitCoinLibUtil.verifyNativeSegwitMultiSign(
                pub,
                signData.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()),
                (List<ECKey>) baseInfo[0],
                (long) baseInfo[1],
                (String) baseInfo[2],
                UTXOList,
                (List<byte[]>) baseInfo[3],
                (int) baseInfo[4],
                (int) baseInfo[5],
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(), true, null);
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws NulsException {
        //if (htgContext.getConverterCoreApi().checkChangeP35(nerveTxHash)) {
        //    return true;
        //}
        // Business validation
        this.changeBaseCheck(nerveTxHash, addPubs, removePubs);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(nerveTxHash);
        if (withdrawlUTXO == null || HtgUtil.isEmptyList(withdrawlUTXO.getUtxoDataList())) {
            return true;
        }
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        Object[] baseInfo = this._makeChangeTxBaseInfo(withdrawlUTXO);
        long amount = (long) baseInfo[1];
        // Check whether the remaining utxo is enough to pay the transfer fee, otherwise the transaction will not be issued.
        if (amount <= ConverterConstant.BTC_DUST_AMOUNT) {
            return true;
        }
        // Assemble and verify sufficient Byzantine signatures
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }
        List<ECKey> pubs = (List<ECKey>) baseInfo[0];
        int verified = BitCoinLibUtil.verifyNativeSegwitMultiSignCount(
                signatures,
                pubs,
                amount,
                (String) baseInfo[2],
                UTXOList,
                (List<byte[]>) baseInfo[3],
                (int) baseInfo[4],
                (int) baseInfo[5],
                withdrawlUTXO.getFeeRate(),
                htgContext.getConverterCoreApi().isNerveMainnet(), true, null);
        int byzantineCount = htgContext.getConverterCoreApi().getByzantineCount(pubs.size());
        return verified >= byzantineCount;
    }

    @Override
    public long getWithdrawalFeeSize(long fromTotal, long transfer, long feeRate, int inputNum) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getConverterCoreApi().getByzantineCount(n);
        return BitCoinLibUtil.calcFeeMultiSignSizeP2WSHWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, new int[]{32}, m, n);
    }

    @Override
    public long getChangeFeeSize(int utxoSize) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getConverterCoreApi().getByzantineCount(n);
        long size = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(utxoSize, 1, new int[]{32}, m, n);
        return size;
    }

}

