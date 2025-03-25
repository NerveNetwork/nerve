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
package network.nerve.converter.heterogeneouschain.bchutxo.utils;

import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.SignatureScript;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.CashAddressFactory;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.MainNetParamsForAddr;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.TestNet4ParamsForAddr;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import network.nerve.converter.rpc.call.SwapCall;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.base.VarInt;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptPattern;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;
import static org.bitcoinj.script.ScriptOpCodes.OP_1;
import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;

/**
 * @author: PierreLuo
 * @date: 2024/7/16
 */
public class BchUtxoUtil {

    public static String getBchAddress(String pubKey, boolean mainnet) {
        String btcLegacyAddress = BitCoinLibUtil.getBtcLegacyAddress(HexUtil.decode(pubKey), mainnet);
        return CashAddressFactory.create().getFromBase58(mainnet ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), btcLegacyAddress).toString();
    }

    public static void main(String[] args) {
        String hex = "00483045022100f967b44614cc8a464ba1ac1e2ec058bc1f4ea63a640b5fe00f3ab5276684edea02207d15fd732101ab8dd1178599bcfffe7007c213493641d8ee5924fb0d857f94dd414730440220517ae0decc5424cdcd2f1e56aa420af8b548533b9933028283a31ecb70ba5dfc022023d18e3c5ab32c8df6c803e1e93e91b65e487d33ded130ad7d4dd46f5dcc4bf3414c6302b2ad2b3d34a375bc46a36095c8c16ce9142da988911bdabdf7314ad0b18a116f02dcbc5523285dd50ffc8735d45c43807e88a68b860ceeb7357f0501d10e39469f032321f8ed4e0fa66a3732d147a61713732221fc4d0c9829f94604dd8082d210ff";
        Script script = Script.parse(HexUtil.decode(hex));
        List<ScriptChunk> chunks = script.chunks();
        ScriptChunk scriptChunk = chunks.get(chunks.size() - 1);
        byte[] stBytes = scriptChunk.data;
        if (stBytes.length < 105
                || stBytes[0] < OP_1 + 1
                || stBytes[stBytes.length - 2] < OP_1 + 2
                || (0xff & stBytes[stBytes.length - 1]) != OP_CHECKMULTISIG
        ) {
            System.out.println("error parse");
            return;
        }
        Script redeemScript = Script.parse(scriptChunk.data);
        System.out.println();
    }
    public static String takeMultiSignAddressWithP2SH(RawInput input, boolean mainnet) {
        SignatureScript scriptSig = input.getScriptSig();
        if (scriptSig == null || StringUtils.isBlank(scriptSig.getHex())) {
            return EMPTY_STRING;
        }
        String hex = scriptSig.getHex();
        Script script = Script.parse(HexUtil.decode(hex));
        List<ScriptChunk> chunks = script.chunks();
        ScriptChunk scriptChunk = chunks.get(chunks.size() - 1);
        byte[] stBytes = scriptChunk.data;
        if (stBytes.length < 105
                || stBytes[0] < OP_1 + 1
                || stBytes[stBytes.length - 2] < OP_1 + 2
                || (0xff & stBytes[stBytes.length - 1]) != OP_CHECKMULTISIG
        ) {
            return EMPTY_STRING;
        }

        Script redeemScript = Script.parse(scriptChunk.data);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(mainnet ? MainNetParams.get() : TestNet3Params.get(), ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();
        String newMultiSigAddr = CashAddressFactory.create().getFromBase58(mainnet ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), multiSigAddress).toString();
        return newMultiSigAddr;
    }

    public static String multiAddrByECKey(List<ECKey> pubKeys, int m, boolean mainnet) {
        String base58 = BitCoinLibUtil.makeMultiAddr(pubKeys, m, mainnet);
        return CashAddressFactory.create().getFromBase58(mainnet ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), base58).toString();
    }


    public static String multiAddr(List<byte[]> pubKeyList, int m, boolean mainnet) {
        return multiAddrByECKey(pubKeyList.stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList()), m, mainnet);
    }

    public static long calcFeeSize(int inputNum, int outputNum, int opReturnBytesLen) {
        long priceInSatoshi = 1L;
        long length = 0L;
        if (opReturnBytesLen == 0) {
            length = 10L + 141L * (long)inputNum + 34L * (long)(outputNum + 1);
        } else {
            length = 10L + 141L * (long)inputNum + 34L * (long)(outputNum + 1) + (long)(opReturnBytesLen + VarInt.sizeOf((long)opReturnBytesLen) + 1 + VarInt.sizeOf((long)(opReturnBytesLen + VarInt.sizeOf((long)opReturnBytesLen) + 1)) + 8);
        }

        return priceInSatoshi * length;
    }

    public static Object[] calcFeeAndUTXO(List<UTXOData> utxos, long amount, long feeRate, int opReturnBytesLen) {
        boolean enoughUTXO = false;
        long _fee = 0, total = 0;
        List<UTXOData> resultList = new ArrayList<>();
        for (int i = 0; i < utxos.size(); i++) {
            UTXOData utxo = utxos.get(i);
            total = total + utxo.getAmount().longValue();
            resultList.add(utxo);
            _fee = calcFeeSize(resultList.size(), 1, opReturnBytesLen) * feeRate;
            long totalSpend = amount + _fee;
            if (total >= totalSpend) {
                enoughUTXO = true;
                break;
            }
        }
        if (!enoughUTXO) {
            throw new RuntimeException("not enough utxo, may need more: " + (amount + _fee - total));
        }
        return new Object[]{_fee, resultList};
    }

    public static int getByzantineCount(int count) {
        int directorCount = count;
        int ByzantineRateCount = directorCount * 66;
        int minPassCount = ByzantineRateCount / 100;
        if (ByzantineRateCount % 100 > 0) {
            minPassCount++;
        }
        return minPassCount;
    }

    public static long calcFeeMultiSign(int inputNum, int outputNum, int opReturnBytesLen, int m, int n) {

        long redeemScriptLength = 1 + (n * (33L + 1)) + 1 + 1;
        long redeemScriptVarInt = VarInt.sizeOf(redeemScriptLength);
        long scriptLength = 2 + (m * (1 + 1 + 69L + 1 + 1)) + redeemScriptVarInt + redeemScriptLength;
        long scriptVarInt = VarInt.sizeOf(scriptLength);
        long inputLength = 40 + scriptVarInt + scriptLength;

        int totalOpReturnLen = calcOpReturnLen(opReturnBytesLen);
        long length = 10 + inputLength * inputNum + (long) 43 * (outputNum + 1) + totalOpReturnLen;
        return length;
    }

    public static long calcFeeMultiSignBak(int inputNum, int outputNum, int opReturnBytesLen, int m, int n) {

        long op_mLen = 1;
        long op_nLen = 1;
        long pubKeyLen = 33;
        long pubKeyLenLen = 1;
        long op_checkmultisigLen = 1;

        long redeemScriptLength = op_mLen + (n * (pubKeyLenLen + pubKeyLen)) + op_nLen + op_checkmultisigLen; //105 n=3
        long redeemScriptVarInt = VarInt.sizeOf(redeemScriptLength);//1 n=3

        long op_pushDataLen = 1;
        long sigHashLen = 1;
        long signLen = 64;
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

        long outputValueLen = 8;
        long unlockScriptLen = 25; //If sending to multiSignAddr, it will be 23.
        long unlockScriptLenLen = 1;
        long outPutLen = outputValueLen + unlockScriptLenLen + unlockScriptLen;

        long inputCountLen = 1;
        long outputCountLen = 1;
        long txVerLen = 4;
        long nLockTimeLen = 4;
        long txFixedLen = inputCountLen + outputCountLen + txVerLen + nLockTimeLen;

        long length;
        length = txFixedLen + inputLength * inputNum + outPutLen * (outputNum + 1) + opReturnLen;

        return length;
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

    public static int calcSplitNumP2SH(long fromTotal, long transfer, long feeRate, long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n) {
        // numerator and denominator
        long numerator = fromTotal - transfer - calcFeeMultiSign(inputNum, 1, opReturnBytesLen, m, n) * feeRate + 43 * feeRate;
        long denominator = 43 * feeRate + splitGranularity;
        int splitNum = (int) (numerator / denominator);
        if (splitNum == 0 && numerator % denominator > 0) {
            splitNum = 1;
        }
        return splitNum;
    }

    public static final long MIN_SPLIT_GRANULARITY = 100000;// 0.001
    public static long calcFeeMultiSignWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n) {
        long feeSize;
        if (splitGranularity != null && splitGranularity > 0) {
            if (splitGranularity < MIN_SPLIT_GRANULARITY) {
                throw new RuntimeException("error splitGranularity: " + splitGranularity);
            }
            int splitNum = calcSplitNumP2SH(fromTotal, transfer, feeRate, splitGranularity, inputNum, opReturnBytesLen, m, n);
            feeSize = calcFeeMultiSign(inputNum, splitNum, opReturnBytesLen, m, n);
        } else {
            feeSize = calcFeeMultiSign(inputNum, 1, opReturnBytesLen, m, n);
        }
        return feeSize;
    }

    public static Object[] makeChangeTxBaseInfo (HtgContext htgContext, WithdrawalUTXO withdrawlUTXO, List<String> currentMultiSignAddressPubs) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        String nerveTxHash = withdrawlUTXO.getNerveTxHash();
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        // take pubkeys of all managers
        List<byte[]> newPubEcKeys = withdrawlUTXO.getPubs();

        List<String> multiSignAddressPubs = currentMultiSignAddressPubs;
        List<byte[]> oldPubEcKeys = multiSignAddressPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList());

        String toAddress = multiAddr(newPubEcKeys, coreApi.getByzantineCount(newPubEcKeys.size()), coreApi.isNerveMainnet());
        // calc the min number of signatures
        int n = oldPubEcKeys.size(), m = coreApi.getByzantineCount(n);
        long fee = BchUtxoUtil.calcFeeMultiSign(UTXOList.size(), 1, nerveTxHash.getBytes(StandardCharsets.UTF_8).length, m, n) * withdrawlUTXO.getFeeRate();
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        return new Object[]{oldPubEcKeys, totalMoney - fee, toAddress, m, n};
    }

    public static String signWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signer, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        return SwapCall.signBchWithdraw(nerveChainId, withdrawalUTXO, signer, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity, mainnet);
    }

    public static boolean verifyWithdraw(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        return SwapCall.verifyBchWithdraw(nerveChainId, withdrawalUTXO, signData, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity, mainnet);
    }

    public static int verifyWithdrawCount(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String toAddress, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        return SwapCall.verifyBchWithdrawCount(nerveChainId, withdrawalUTXO, signatureData, toAddress, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity, mainnet);
    }

    public static String createMultiSignWithdrawTx(int nerveChainId, WithdrawalUTXO withdrawalUTXO, String signatureData, String to, long amount, long feeRate, String opReturn, int m, int n, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws Exception {
        return SwapCall.createBchMultiSignWithdrawTx(nerveChainId, withdrawalUTXO, signatureData, to, amount, feeRate, opReturn, m, n, useAllUTXO, splitGranularity, mainnet);
    }
}
