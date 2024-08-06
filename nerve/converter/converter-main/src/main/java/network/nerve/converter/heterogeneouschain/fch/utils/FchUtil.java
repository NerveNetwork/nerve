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
package network.nerve.converter.heterogeneouschain.fch.utils;

import fchClass.P2SH;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import keyTools.KeyTools;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import network.nerve.converter.rpc.call.SwapCall;
import org.bitcoinj.base.VarInt;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchUtil {

    public static HtgAccount createAccount(String prikey) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(prikey), true);
        String address = KeyTools.pubKeyToFchAddr(ecKey.getPubKey());
        byte[] pubKey = ecKey.getPubKey();
        HtgAccount account = new HtgAccount();
        account.setAddress(address);
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ecKey.getPrivKeyBytes());
        account.setEncryptedPriKey(new byte[0]);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static HtgAccount createAccountByPubkey(String pubkeyStr) {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode(pubkeyStr));
        byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
        HtgAccount account = new HtgAccount();
        account.setAddress(KeyTools.pubKeyToFchAddr(HexUtil.encode(pubKey)));
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ZERO_BYTES);
        account.setEncryptedPriKey(ZERO_BYTES);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static P2SH genMultiP2shForTest(List<byte[]> pubKeyList, int m) {
        return genMultiP2sh(pubKeyList, m, false);
    }

    public static P2SH genMultiP2sh(List<byte[]> pubKeyList, int m, boolean order) {
        List<ECKey> keys = new ArrayList();
        Iterator var3 = pubKeyList.iterator();

        byte[] redeemScriptBytes;
        while (var3.hasNext()) {
            redeemScriptBytes = (byte[]) var3.next();
            ECKey ecKey = ECKey.fromPublicOnly(redeemScriptBytes);
            keys.add(ecKey);
        }

        if (order) {
            keys = new ArrayList<>(keys);
            Collections.sort(keys, ECKey.PUBKEY_COMPARATOR);
        }

        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(m, keys);
        redeemScriptBytes = multiSigScript.getProgram();

        try {
            P2SH p2sh = P2SH.parseP2shRedeemScript(javaTools.HexUtil.encode(redeemScriptBytes));
            return p2sh;
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }
    }

    public static long calcFeeMultiSign(int inputNum, int outputNum, int opReturnBytesLen, int m, int n) {

        long op_mLen =1;
        long op_nLen =1;
        long pubKeyLen = 33;
        long pubKeyLenLen = 1;
        long op_checkmultisigLen = 1;

        long redeemScriptLength = op_mLen + (n * (pubKeyLenLen + pubKeyLen)) + op_nLen + op_checkmultisigLen; //105 n=3
        long redeemScriptVarInt = VarInt.sizeOf(redeemScriptLength);//1 n=3

        long op_pushDataLen = 1;
        long sigHashLen = 1;
        long signLen=64;
        long signLenLen = 1;
        long zeroByteLen = 1;

        long mSignLen = m * (signLenLen + signLen + sigHashLen); //132 m=2

        long scriptLength = zeroByteLen + mSignLen + op_pushDataLen + redeemScriptVarInt + redeemScriptLength;//236 m=2
        long scriptVarInt = VarInt.sizeOf(scriptLength);

        long preTxIdLen = 32;
        long preIndexLen = 4;
        long sequenceLen = 4;

        long inputLength = preTxIdLen + preIndexLen + sequenceLen + scriptVarInt + scriptLength;//240 n=3,m=2


        long opReturnLen = 0;
        if (opReturnBytesLen != 0)
            opReturnLen = calcOpReturnLen(opReturnBytesLen);

        long outputValueLen=8;
        long unlockScriptLen = 25; //If sending to multiSignAddr, it will be 23.
        long unlockScriptLenLen =1;
        long outPutLen = outputValueLen + unlockScriptLenLen + unlockScriptLen;

        long inputCountLen=1;
        long outputCountLen=1;
        long txVerLen = 4;
        long nLockTimeLen = 4;
        long txFixedLen = inputCountLen + outputCountLen + txVerLen + nLockTimeLen;

        long length;
        length = txFixedLen + inputLength * inputNum + outPutLen * (outputNum + 1) + opReturnLen;

        return length;
    }

    public static int calcSplitNumP2SH(long fromTotal, long transfer, long feeRate, long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n) {
        // numerator and denominator
        long numerator = fromTotal - transfer - calcFeeMultiSign(inputNum, 1, opReturnBytesLen, m, n) * feeRate + 34 * feeRate;
        long denominator = 34 * feeRate + splitGranularity;
        int splitNum = (int) (numerator / denominator);
        if (splitNum == 0 && numerator % denominator > 0) {
            splitNum = 1;
        }
        return splitNum;
    }

    public static long calcFeeMultiSignWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n) {
        long feeSize;
        if (splitGranularity != null && splitGranularity > 0) {
            if (splitGranularity < ConverterConstant.MIN_SPLIT_GRANULARITY) {
                throw new RuntimeException("error splitGranularity: " + splitGranularity);
            }
            int splitNum = calcSplitNumP2SH(fromTotal, transfer, feeRate, splitGranularity, inputNum, opReturnBytesLen, m, n);
            feeSize = calcFeeMultiSign(inputNum, splitNum, opReturnBytesLen, m, n);
        } else {
            feeSize = calcFeeMultiSign(inputNum, 1, opReturnBytesLen, m, n);
        }
        return feeSize;
    }

    private static int calcOpReturnLen(int opReturnBytesLen) {
        int dataLen;
        if (opReturnBytesLen < 76) {
            dataLen = opReturnBytesLen + 1;
        } else if (opReturnBytesLen < 256) {
            dataLen = opReturnBytesLen + 2;
        } else dataLen = opReturnBytesLen + 3;
        int scriptLen;
        scriptLen = (dataLen + 1) + VarInt.sizeOf(dataLen + 1);
        int amountLen = 8;
        return scriptLen + amountLen;
    }

    public static String signWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signer, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        return SwapCall.signFchWithdraw(nerveChainId, withdrawalUTXO, signer, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity);
    }

    public static boolean verifyWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        return SwapCall.verifyFchWithdraw(nerveChainId, withdrawalUTXO, signData, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity);
    }

    public static int verifyWithdrawCount(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String toAddress, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        return SwapCall.verifyFchWithdrawCount(nerveChainId, withdrawalUTXO, signatureData, toAddress, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity);
    }

    public static String createMultiSignWithdrawTx(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        return SwapCall.createFchMultiSignWithdrawTx(nerveChainId, withdrawalUTXO, signatureData, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity);
    }

    public static Object[] makeChangeTxBaseInfo (HtgContext htgContext, WithdrawalUTXO withdrawlUTXO, List<String> currentMultiSignAddressPubs) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        String nerveTxHash = withdrawlUTXO.getNerveTxHash();
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        // take pubkeys of all managers
        List<byte[]> newPubEcKeys = withdrawlUTXO.getPubs();

        List<String> multiSignAddressPubs = currentMultiSignAddressPubs;
        List<byte[]> oldPubEcKeys = multiSignAddressPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());

        String toAddress = FchUtil.genMultiP2sh(newPubEcKeys, coreApi.getByzantineCount(newPubEcKeys.size()), true).getFid();
        // calc the min number of signatures
        int n = oldPubEcKeys.size(), m = coreApi.getByzantineCount(n);
        long fee = FchUtil.calcFeeMultiSign(UTXOList.size(), 1, nerveTxHash.getBytes(StandardCharsets.UTF_8).length, m, n) * withdrawlUTXO.getFeeRate();
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        return new Object[]{oldPubEcKeys, totalMoney - fee, toAddress, m, n};
    }


}
