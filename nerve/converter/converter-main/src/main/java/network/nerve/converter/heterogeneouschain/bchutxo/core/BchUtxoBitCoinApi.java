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
package network.nerve.converter.heterogeneouschain.bchutxo.core;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
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
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.IBitCoinLibParseTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.WithdrawalUTXO;

import java.math.BigInteger;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2024/2/29
 */
public class BchUtxoBitCoinApi extends BitCoinLibApi {
    private IHeterogeneousChainDocking docking;
    private BitCoinLibWalletApi walletApi;
    private IBitCoinLibParseTxHelper parseTxHelper;
    private HtgContext htgContext;
    protected HtgListener htgListener;

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return walletApi;
    }

    @Override
    protected String _createMultiSignWithdrawTx(WithdrawalUTXO withdrawlUTXO, String signatureData, String to, long amount, String nerveTxHash) throws Exception {
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);

        String txHex = BchUtxoUtil.createMultiSignWithdrawTx(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                withdrawlUTXO, signatureData,
                to,
                amount,
                withdrawlUTXO.getFeeRate(),
                nerveTxHash,
                m, n, false, getSplitGranularity(),
                htgContext.getConverterCoreApi().isNerveMainnet()
        );
        return txHex;
    }

    @Override
    protected long _calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {
        return BchUtxoUtil.calcFeeMultiSign(inputNum, outputNum, opReturnBytesLen[0], m, n);
    }

    @Override
    protected long _calcFeeMultiSignSizeWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        return BchUtxoUtil.calcFeeMultiSignWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, opReturnBytesLen[0], m, n);
    }

    @Override
    protected WithdrawalFeeLog _takeWithdrawalFeeLogFromTxParse(String htgTxHash, boolean nerveInner) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(htgTxHash);
        if (txInfo.getConfirmations() == null || txInfo.getConfirmations().intValue() == 0) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        List<RawInput> inputList = txInfo.getVIn();
        HeterogeneousChainTxType txType = null;
        OUT:
        do {
            for (RawInput input : inputList) {
                String inputAddress = BchUtxoUtil.takeMultiSignAddressWithP2SH(input, htgContext.getConverterCoreApi().isNerveMainnet());
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
            BtcUnconfirmedTxPo po = (BtcUnconfirmedTxPo) parseTxHelper.parseDepositTransaction(txInfo, null, true);
            if (po.getNerveAddress().equals(ConverterContext.BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS)) {
                // Record chain fee entry
                feeLog = new WithdrawalFeeLog(
                        po.getBlockHeight(), po.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), po.getValue().longValue(), true);
                feeLog.setTxTime(txInfo.getBlockTime());
            }
        } else if (txType == HeterogeneousChainTxType.WITHDRAW) {
            // All transactions with nerve multi-signature addresses in from must record handling fee expenditures.
            long fee = BitCoinLibUtil.calcTxFee(txInfo, walletApi);
            feeLog = new WithdrawalFeeLog(
                    Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()), txInfo.getBlockHash(), htgTxHash, htgContext.HTG_CHAIN_ID(), fee, false);
            feeLog.setTxTime(txInfo.getBlockTime());
        }
        if (feeLog != null && htgContext.getConverterCoreApi().isProtocol36()) {
            feeLog.setNerveInner(nerveInner);
        }
        return feeLog;
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo (WithdrawalUTXO withdrawlUTXO) {
        List<String> multiSignAddressPubs = this.getMultiSignAddressPubs(withdrawlUTXO.getCurrentMultiSignAddress());
        return BchUtxoUtil.makeChangeTxBaseInfo(htgContext, withdrawlUTXO, multiSignAddressPubs);
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
        return BchUtxoUtil.createMultiSignWithdrawTx(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO, signatureData,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null, htgContext.getConverterCoreApi().isNerveMainnet());
    }

    @Override
    public List<UTXOData> getUTXOs(String address) {
        return walletApi.getAccountUTXOs(address);
    }

    @Override
    public String signWithdraw(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        logger().info("pierre test==={}", 10);
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);
        boolean mainnet = htgContext.getConverterCoreApi().isNerveMainnet();
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            logger().info("pierre test==={}", 11);
            String signatures = BchUtxoUtil.signWithdraw(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    withdrawlUTXO,
                    AddressTool.getAddressString(HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY()), htgContext.NERVE_CHAINID()),
                    toAddress,
                    value.longValue(),
                    withdrawlUTXO.getFeeRate(),
                    txHash,
                    m, n, false, getSplitGranularity(), mainnet);
            logger().info("pierre test==={}, sign==={}", 12, signatures);
            return signatures;
        } else {
            logger().info("pierre test==={}", 13);
            // sign machine support
            byte[] signerPub = HexUtil.decode(htgContext.ADMIN_ADDRESS_PUBLIC_KEY());
            String signatures = htgContext.getConverterCoreApi().signBchWithdrawByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
                    htgContext.HTG_CHAIN_ID(),
                    HexUtil.encode(signerPub),
                    txHash,
                    toAddress,
                    value.longValue(),
                    withdrawlUTXO,
                    getSplitGranularity());
            logger().info("pierre test==={}, sign==={}", 14, signatures);
            return signatures;
        }
    }


    @Override
    public Boolean verifySignWithdraw(String signAddress, String txHash, String toAddress, BigInteger amount, int assetId, String signature) throws NulsException {
        WithdrawalUTXO withdrawlUTXO = this.getWithdrawalUTXO(txHash);
        // calc the min number of signatures
        int n = withdrawlUTXO.getPubs().size(), m = htgContext.getConverterCoreApi().getByzantineCount(n);
        return BchUtxoUtil.verifyWithdraw(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                withdrawlUTXO,
                signature,
                toAddress,
                amount.longValue(),
                withdrawlUTXO.getFeeRate(),
                txHash,
                m, n, false, getSplitGranularity(), htgContext.getConverterCoreApi().isNerveMainnet());
    }

    @Override
    public String signManagerChanges(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount) throws NulsException {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        if (coreApi.checkChangeP35(nerveTxHash)) {
            return nerveTxHash;
        }
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
            String signatures = BchUtxoUtil.signWithdraw(
                    htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                    cloneWithdrawalUTXO,
                    nerveSigner,
                    toAddress,
                    amount,
                    cloneWithdrawalUTXO.getFeeRate(),
                    nerveTxHash,
                    m, n, true, null, htgContext.getConverterCoreApi().isNerveMainnet());
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
            String signatures = htgContext.getConverterCoreApi().signBchChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(),
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

        return BchUtxoUtil.verifyWithdraw(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO,
                signature,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null, htgContext.getConverterCoreApi().isNerveMainnet());
    }

    @Override
    public boolean validateManagerChangesTx(String nerveTxHash, String[] addPubs, String[] removePubs, int orginTxCount, String signatureData) throws NulsException {
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

        int verified = BchUtxoUtil.verifyWithdrawCount(
                htgContext.getConverterCoreApi().getConverterConfig().getChainId(),
                cloneWithdrawalUTXO,
                signatureData,
                toAddress,
                amount,
                cloneWithdrawalUTXO.getFeeRate(),
                nerveTxHash,
                m, n, true, null, htgContext.getConverterCoreApi().isNerveMainnet());
        int byzantineCount = htgContext.getConverterCoreApi().getByzantineCount(currentPubs.size());
        return verified >= byzantineCount;
    }

    @Override
    public long getWithdrawalFeeSize(long fromTotal, long transfer, long feeRate, int inputNum) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getConverterCoreApi().getByzantineCount(n);
        return BchUtxoUtil.calcFeeMultiSignWithSplitGranularity(
                fromTotal, transfer, feeRate, getSplitGranularity(), inputNum, 32, m, n);
    }

    @Override
    public long getChangeFeeSize(int utxoSize) {
        int n = htgContext.getConverterCoreApi().getVirtualBankSize();
        int m = htgContext.getConverterCoreApi().getByzantineCount(n);
        long size = BchUtxoUtil.calcFeeMultiSign(utxoSize, 1, 32, m, n);
        return size;
    }

}

