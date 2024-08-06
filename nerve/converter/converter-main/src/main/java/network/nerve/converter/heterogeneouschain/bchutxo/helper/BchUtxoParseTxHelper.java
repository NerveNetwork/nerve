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
package network.nerve.converter.heterogeneouschain.bchutxo.helper;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import com.neemre.btcdcli4j.core.domain.TxOutInfo;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.IBitCoinLibParseTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BchUtxoParseTxHelper implements IBitCoinLibParseTxHelper, BeanInitial {

    private IBitCoinApi bitCoinApi;
    private BitCoinLibWalletApi walletApi;
    private HtgListener htgListener;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    @Override
    public boolean isCompletedTransaction(String nerveTxHash) {
        WithdrawalUTXOTxData utxoTxData = bitCoinApi.takeWithdrawalUTXOs(nerveTxHash);
        if (utxoTxData == null) {
            return false;
        }
        List<UTXOData> utxoDataList = utxoTxData.getUtxoDataList();
        if (HtgUtil.isEmptyList(utxoDataList)) {
            return true;
        }
        Collections.sort(utxoDataList, ConverterUtil.BITCOIN_SYS_COMPARATOR);
        UTXOData utxoData = utxoDataList.get(0);
        TxOutInfo txOutInfo = walletApi.getTxOutInfo(utxoData.getTxid(), utxoData.getVout());
        return txOutInfo == null;
    }

    @Override
    public HeterogeneousTransactionInfo parseDepositTransaction(Object txInfoObj, Long blockHeight, boolean validate) throws Exception {
        RawTransaction txInfo = (RawTransaction) txInfoObj;
        if (txInfo == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        String htgTxHash = txInfo.getTxId();
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        if (HtgUtil.isEmptyList(outputList)) {
            return null;
        }
        BigInteger value = BigInteger.ZERO;
        String txTo = null;
        for (RawOutput output : outputList) {
            List<String> addresses = output.getScriptPubKey().getAddresses();
            if (HtgUtil.isEmptyList(addresses)) {
                continue;
            }
            String outputAddress = addresses.get(0);
            if (htgListener.isListeningAddress(outputAddress)) {
                if (txTo == null) {
                    txTo = outputAddress;
                }
                value = value.add(output.getValue().movePointRight(htgContext.ASSET_NAME().decimals()).toBigInteger());
            }
        }
        if (value.compareTo(BigInteger.ZERO) == 0) {
            return null;
        }
        RechargeData rechargeData = null;
        boolean error = false;
        do {
            String opReturnInfo = walletApi.getOpReturnHex(txInfo);
            if (StringUtils.isBlank(opReturnInfo)) {
                logger().warn("Recharge informationDataIllegal transaction[{}], opReturnInfo: {}", htgTxHash, opReturnInfo);
                error = true;
                break;
            }
            try {
                rechargeData = new RechargeData();
                rechargeData.parse(HexUtil.decode(opReturnInfo), 0);
            } catch (Exception e) {
                logger().warn(String.format("Illegal recharge information[1] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo), e);
                error = true;
                break;
            }
            byte[] rechargeDataTo = rechargeData.getTo();
            if (rechargeDataTo == null) {
                logger().warn("[Abnormal recharge address] transaction[{}], [0]The recharge address is empty", htgTxHash);
                error = true;
                break;
            }
            if (!AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeDataTo)) {
                logger().warn("[Abnormal recharge address] transaction[{}], [1]Recharge addressHexData: {}", htgTxHash, HexUtil.encode(rechargeDataTo));
                error = true;
                break;
            }
            boolean hasFeeTo = rechargeData.getFeeTo() != null;
            if (hasFeeTo && !AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeData.getFeeTo())) {
                logger().warn("[FeeToAddress abnormality] transaction[{}], [0]addressHexData: {}", htgTxHash, HexUtil.encode(rechargeData.getFeeTo()));
                error = true;
                break;
            }
            BigInteger rechargeValue = BigInteger.valueOf(rechargeData.getValue());
            if (rechargeValue.compareTo(value) > 0) {
                logger().warn("[Abnormal recharge amount] transaction[{}], [0]Registration amount: {}, Actual amount: {}", htgTxHash, rechargeData.getValue(), value);
                error = true;
                break;
            }
            // If the actual amount is greater than the specified recharge amount and no handling fee address is filled in, the excess amount will be transferred to the team address
            if (value.compareTo(rechargeValue) > 0 && !hasFeeTo) {
                rechargeData.setFeeTo(ConverterContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0);
            }
        } while (false);
        if (validate && error) {
            return null;
        }

        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        po.setFee(BigInteger.ZERO);
        po.setValue(value);
        // analysisfromaddress
        RawInput rawInput = txInfo.getVIn().get(0);
        RawTransaction previousTx = walletApi.getTransactionByHash(rawInput.getTxId());
        RawOutput previousTxOutput = previousTx.getVOut().get(rawInput.getVOut());
        po.setFrom(previousTxOutput.getScriptPubKey().getAddresses().get(0));
        po.setTo(txTo);
        if (rechargeData != null) {
            po.setNerveAddress(rechargeData.getTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getTo()) : null);
            po.setValue(BigInteger.valueOf(rechargeData.getValue()));
            po.setFee(value.subtract(BigInteger.valueOf(rechargeData.getValue())));
            po.setNerveFeeTo(rechargeData.getFeeTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getFeeTo()) : null);
            po.setExtend0(rechargeData.getExtend0());
            po.setExtend1(rechargeData.getExtend1());
            po.setExtend2(rechargeData.getExtend2());
            po.setExtend3(rechargeData.getExtend3());
            po.setExtend4(rechargeData.getExtend4());
            po.setExtend5(rechargeData.getExtend5());
        }
        po.setTxType(HeterogeneousChainTxType.DEPOSIT);
        po.setTxHash(htgTxHash);
        if (blockHeight != null) {
            po.setBlockHeight(blockHeight);
        } else {
            po.setBlockHeight(Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()));
        }
        po.setBlockHash(txInfo.getBlockHash());
        po.setTxTime(txInfo.getBlockTime());
        po.setDecimals(htgContext.ASSET_NAME().decimals());
        po.setIfContractAsset(false);
        po.setAssetId(1);
        return po;
    }

    @Override
    public HeterogeneousTransactionInfo parseWithdrawalTransaction(Object txInfoObj, Long blockHeight, boolean validate) throws Exception {
        RawTransaction txInfo = (RawTransaction) txInfoObj;
        if (txInfo == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        String htgTxHash = txInfo.getTxId();
        if (HtgUtil.isEmptyList(txInfo.getVOut())) {
            return null;
        }
        if (HtgUtil.isEmptyList(txInfo.getVIn())) {
            return null;
        }

        String multiAddr = null;
        List<UsedUTXOData> usedUTXOList = new ArrayList<>();
        List<RawInput> inputList = txInfo.getVIn();
        for (RawInput input : inputList) {
            String inputAddress = BchUtxoUtil.takeMultiSignAddressWithP2SH(input, htgContext.getConverterCoreApi().isNerveMainnet());
            if (htgListener.isListeningAddress(inputAddress)) {
                multiAddr = inputAddress;
                usedUTXOList.add(new UsedUTXOData(input.getTxId(), input.getVOut()));
            }
        }
        if (!htgContext.MULTY_SIGN_ADDRESS().equals(multiAddr)) {
            return null;
        }
        List<RawOutput> outputList = txInfo.getVOut();
        BigInteger value = BigInteger.ZERO;
        String txTo = null;
        for (RawOutput output : outputList) {
            List<String> addresses = output.getScriptPubKey().getAddresses();
            if (HtgUtil.isEmptyList(addresses)) {
                continue;
            }
            String outputAddress = addresses.get(0);
            if (StringUtils.isBlank(outputAddress)) {
                continue;
            }
            // except current nerve multi-signature address
            if (!htgListener.isListeningAddress(outputAddress)) {
                if (txTo == null) {
                    txTo = outputAddress;
                } else if (!txTo.equals(outputAddress)) {
                    // Only one receiver is allowed here
                    return null;
                }
                value = value.add(output.getValue().movePointRight(htgContext.ASSET_NAME().decimals()).toBigInteger());
            }
        }
        if (value.compareTo(BigInteger.ZERO) == 0) {
            if (htgContext.getConverterCoreApi().isProtocol36()
                    && "726f6061f8929f6d180a5fb6efc14242417fca40069ce8be750919b2d190cdf9".equals(htgTxHash)) {
                txTo = "bc1p2h4xfk2k2t0u0pzeupw57c3852pd8lwf0u9ul3x5c02x63n0urassnvssn";
            } else {
                logger().warn("Withdrawal amount 0, Illegal transaction [{}]", htgTxHash);
                return null;
            }
        }
        // check withdrawal info
        String nerveTxHash = null;
        boolean error = true;
        Transaction nerveTx = null;
        do {
            String opReturnInfo = walletApi.getOpReturnHex(txInfo);
            if (StringUtils.isBlank(opReturnInfo)) {
                logger().warn("Withdrawal information Data Illegal transaction[{}], opReturnInfo: {}", htgTxHash, opReturnInfo);
                break;
            }
            byte[] opReturnInfoBytes;
            try {
                opReturnInfoBytes = HexUtil.decode(opReturnInfo);
            } catch (Exception e) {
                logger().warn(String.format("Illegal withdrawal information[1] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo), e);
                break;
            }
            if (opReturnInfoBytes.length != NulsHash.HASH_LENGTH) {
                logger().warn(String.format("Illegal withdrawal information[2] transaction[%s], opReturnInfo: %s", htgTxHash, opReturnInfo));
                break;
            }
            nerveTxHash = opReturnInfo;
            if ((nerveTx = htgContext.getConverterCoreApi().getNerveTx(nerveTxHash)) == null) {
                htgListener.removeListeningTx(htgTxHash);
                htgContext.logger().warn("Illegal transaction business[{}], not found NERVE Transaction, Type: WITHDRAWAL, Key: {}", htgTxHash, nerveTxHash);
                break;
            }
            if (nerveTx.getType() != TxType.WITHDRAWAL && nerveTx.getType() != TxType.CHANGE_VIRTUAL_BANK) {
                htgContext.logger().warn("Illegal transaction business[{}], not found NERVE Transaction, Type: WITHDRAWAL, Key: {}", htgTxHash, nerveTxHash);
                break;
            }
            error = false;
        } while (false);
        if (validate && error) {
            return null;
        }

        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        po.setCheckWithdrawalUsedUTXOData(new CheckWithdrawalUsedUTXOData(usedUTXOList).serialize());
        po.setFrom(multiAddr);
        po.setTo(txTo);
        po.setFee(BigInteger.ZERO);
        po.setValue(value);
        po.setDecimals(htgContext.getConfig().getDecimals());
        po.setAssetId(htgContext.HTG_ASSET_ID());
        if (nerveTx.getType() == TxType.WITHDRAWAL) {
            po.setTxType(HeterogeneousChainTxType.WITHDRAW);
        } else {
            po.setTxType(HeterogeneousChainTxType.CHANGE);
        }
        po.setTxHash(htgTxHash);
        if (blockHeight != null) {
            po.setBlockHeight(blockHeight);
        } else {
            po.setBlockHeight(Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight()));
        }
        po.setBlockHash(txInfo.getBlockHash());
        po.setTxTime(txInfo.getBlockTime());
        po.setDecimals(htgContext.ASSET_NAME().decimals());
        po.setIfContractAsset(false);
        po.setNerveTxHash(nerveTxHash);
        String btcFeeReceiverPub = htgContext.getConverterCoreApi().getBtcFeeReceiverPub();
        String btcFeeReceiver = BchUtxoUtil.getBchAddress(btcFeeReceiverPub, htgContext.getConverterCoreApi().isNerveMainnet());
        List<HeterogeneousAddress> signers = new ArrayList<>();
        signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), btcFeeReceiver));
        po.setSigners(signers);
        return po;
    }

    @Override
    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash, boolean validate) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(txHash);
        return this.parseDepositTransaction(txInfo, null, validate);
    }

    @Override
    public HeterogeneousTransactionInfo parseWithdrawalTransaction(String txHash, boolean validate) throws Exception {
        RawTransaction txInfo = walletApi.getTransactionByHash(txHash);
        return this.parseWithdrawalTransaction(txInfo, null, validate);
    }
}
