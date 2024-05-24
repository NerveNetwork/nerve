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
package network.nerve.converter.heterogeneouschain.fch;

import constants.Constants;
import fcTools.ParseTools;
import fchClass.Cash;
import fchClass.P2SH;
import org.bitcoinj.core.*;
import org.bitcoinj.fch.FchMainNetwork;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import txTools.FchTool;
import walletTools.SendTo;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2024/5/10
 */
public class FchUtil {

    public static P2SH genMultiP2sh(List<byte[]> pubKeyList, int m) {
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


    public static byte[] createMultiSignRawTx(List<Cash> inputs, String to, long amount, String opReturn, P2SH p2SH, int m, int n, boolean useAllUTXO) {
        String changeToFid = ((Cash)inputs.get(0)).getOwner();
        if (!changeToFid.startsWith("3")) {
            throw new RuntimeException("It's not a multisig address.");
        } else {
            long fee = 0;
            int opReturnSize = 0;
            if (opReturn != null) {
                opReturnSize = opReturn.getBytes(StandardCharsets.UTF_8).length;
            }

            Comparator<Cash> comparator = (o1, o2) -> {
                if (o1.getValue() < o2.getValue())
                    return 1;
                else
                    return -1;
            };
            Collections.sort(inputs, comparator);

            List<Cash> usingUtxos = new ArrayList<>();
            long feeRate = 1;
            long totalMoney = 0;
            boolean enoughUTXO = false;
            int outputNum = 1;
            if (useAllUTXO ) {
                outputNum = 0;
            }
            for (Cash utxo : inputs) {
                usingUtxos.add(utxo);
                totalMoney += utxo.getValue();
                long feeSize = calcFeeMultiSign(usingUtxos.size(), outputNum, opReturnSize, m, n);
                fee = feeSize * feeRate;

                if (totalMoney >= (amount + fee)) {
                    enoughUTXO = true;
                    if (!useAllUTXO) {
                        break;
                    }
                } else {
                    enoughUTXO = false;
                }
            }
            if (!enoughUTXO) {
                throw new RuntimeException("not enough utxo, may need more: " + (amount + fee - totalMoney));
            }


            Transaction transaction = new Transaction(fcTools.FchMainNetwork.MAINNETWORK);
            transaction.addOutput(Coin.valueOf(amount), Address.fromBase58(FchMainNetwork.MAINNETWORK, to));

            if (opReturn != null && !"".equals(opReturn)) {
                try {
                    Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn.getBytes(StandardCharsets.UTF_8));
                    transaction.addOutput(Coin.ZERO, opreturnScript);
                } catch (Exception var16) {
                    throw new RuntimeException(var16);
                }
            }

            for (Cash usingUtxo : usingUtxos) {
                Cash input = usingUtxo;
                TransactionOutPoint outPoint = new TransactionOutPoint(FchMainNetwork.MAINNETWORK, (long)input.getBirthIndex(), Sha256Hash.wrap(input.getBirthTxId()));
                TransactionInput unsignedInput = new TransactionInput(new fcTools.FchMainNetwork(), transaction, new byte[0], outPoint);
                transaction.addInput(unsignedInput);
            }

            if (amount + fee > totalMoney) {
                throw new RuntimeException("input is not enough");
            } else {
                long change = totalMoney - amount - fee;
                if (change > Constants.DustInSatoshi) {
                    transaction.addOutput(Coin.valueOf(change), Address.fromBase58(FchMainNetwork.MAINNETWORK, changeToFid));
                }
                return transaction.bitcoinSerialize();
            }
        }
    }
}
