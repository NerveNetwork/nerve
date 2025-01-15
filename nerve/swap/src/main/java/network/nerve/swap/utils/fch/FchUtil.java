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
package network.nerve.swap.utils.fch;

import constants.Constants;
import fchClass.Cash;
import fchClass.P2SH;
import io.nuls.core.crypto.HexUtil;
import network.nerve.swap.constant.SwapConstant;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.SchnorrSignature;
import org.bitcoinj.fch.FchMainNetwork;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import txTools.FchTool;
import walletTools.MultiSigData;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2024/5/10
 */
public class FchUtil {

    public static long calcFeeSize(int inputNum, int outputNum, int opReturnBytesLen) {
        long priceInSatoshi = 1L;
        long length = 0L;
        if (opReturnBytesLen == 0) {
            length = 10L + 141L * (long) inputNum + 34L * (long) (outputNum + 1);
        } else {
            length = 10L + 141L * (long) inputNum + 34L * (long) (outputNum + 1) + (long) (opReturnBytesLen + VarInt.sizeOf((long) opReturnBytesLen) + 1 + VarInt.sizeOf((long) (opReturnBytesLen + VarInt.sizeOf((long) opReturnBytesLen) + 1)) + 8);
        }

        return priceInSatoshi * length;
    }

    public static P2SH genMultiP2shForTest(List<byte[]> pubKeyList, int m) {
        return genMultiP2sh(pubKeyList, m, false);
    }

    public static P2SH genMultiP2sh(List<byte[]> pubKeyList, int m, boolean order) {
        List<ECKey> keys = new ArrayList<>();
        Iterator<byte[]> var3 = pubKeyList.iterator();

        byte[] keyBytes;
        while (var3.hasNext()) {
            keyBytes = (byte[]) var3.next();
            ECKey ecKey = ECKey.fromPublicOnly(keyBytes);
            keys.add(ecKey);
        }
        return genMultiP2shByECKey(keys, m, order);
    }

    public static P2SH genMultiP2shByECKey(List<ECKey> keys, int m, boolean order) {

        if (order) {
            keys = new ArrayList<>(keys);
            Collections.sort(keys, ECKey.PUBKEY_COMPARATOR);
        }

        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(m, keys);
        byte[] redeemScriptBytes = multiSigScript.getProgram();

        try {
            P2SH p2sh = P2SH.parseP2shRedeemScript(javaTools.HexUtil.encode(redeemScriptBytes));
            return p2sh;
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }
    }

    public static long calcFeeMultiSign(int inputNum, int outputNum, int opReturnBytesLen, int m, int n) {

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

    public static final Comparator<Cash> BITCOIN_SYS_COMPARATOR = new Comparator<Cash>() {
        @Override
        public int compare(Cash o1, Cash o2) {
            // order asc
            int compare = Long.valueOf(o1.getValue()).compareTo(o2.getValue());
            if (compare == 0) {
                int compare1 = o1.getBirthTxId().compareTo(o2.getBirthTxId());
                if (compare1 == 0) {
                    return Integer.compare(o1.getBirthIndex(), o2.getBirthIndex());
                }
                return compare1;
            }
            return compare;
        }
    };

    public static Object[] calcFeeAndUTXO(List<Cash> utxos, long amount, long feeRate, int opReturnBytesLen) {
        boolean enoughUTXO = false;
        long _fee = 0, total = 0;
        List<Cash> resultList = new ArrayList<>();
        for (int i = 0; i < utxos.size(); i++) {
            Cash utxo = utxos.get(i);
            total = total + utxo.getValue();
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

    public static long calcFeeWithdrawal(List<Cash> utxos, long amount, long feeRate, boolean mainnet, Long splitGranularity) {
        int opReturnSize = 64;
        int m, n;
        if (mainnet) {
            m = 10;
            n = 15;
        } else {
            m = 2;
            n = 3;
        }
        long fee = 0;
        List<Cash> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        long totalSpend = 0;
        Collections.sort(utxos, BITCOIN_SYS_COMPARATOR);
        for (Cash utxo : utxos) {
            usingUtxos.add(utxo);
            totalMoney += utxo.getValue();
            long feeSize;
            if (splitGranularity != null && splitGranularity > 0) {
                int splitNum = calcSplitNumP2SH(totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
                feeSize = calcFeeMultiSign(usingUtxos.size(), splitNum, opReturnSize, m, n);
            } else {
                feeSize = calcFeeMultiSign(usingUtxos.size(), 1, opReturnSize, m, n);
            }
            fee = feeSize * feeRate;
            totalSpend = amount + fee;
            if (totalMoney >= totalSpend) {
                break;
            }
        }
        if (totalMoney < totalSpend) {
            throw new RuntimeException("not enough utxo, may need more: " + (totalSpend - totalMoney));
        }
        return fee;
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
            if (splitGranularity < SwapConstant.MIN_SPLIT_GRANULARITY) {
                throw new RuntimeException("error splitGranularity: " + splitGranularity);
            }
            int splitNum = calcSplitNumP2SH(fromTotal, transfer, feeRate, splitGranularity, inputNum, opReturnBytesLen, m, n);
            feeSize = calcFeeMultiSign(inputNum, splitNum, opReturnBytesLen, m, n);
        } else {
            feeSize = calcFeeMultiSign(inputNum, 1, opReturnBytesLen, m, n);
        }
        return feeSize;
    }

    public static Object[] createMultiSignRawTxBase(
            List<ECKey> pubEcKeys, List<Cash> inputs,
            String to, long amount, String opReturn,
            int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity) {
        P2SH p2SH = genMultiP2shByECKey(pubEcKeys, m, true);
        String multiSignAddr = p2SH.getFid();
        if (!multiSignAddr.startsWith("3")) {
            throw new RuntimeException("It's not a multisig address.");
        } else {
            long fee = 0;
            int opReturnSize = 0;
            if (opReturn != null) {
                opReturnSize = opReturn.getBytes(StandardCharsets.UTF_8).length;
            }

            Collections.sort(inputs, BITCOIN_SYS_COMPARATOR);

            List<Cash> usingUtxos = new ArrayList<>();
            long totalMoney = 0;
            boolean enoughUTXO = false;
            //int outputNum = 1;
            for (Cash utxo : inputs) {
                usingUtxos.add(utxo);
                totalMoney += utxo.getValue();
                long feeSize = calcFeeMultiSignWithSplitGranularity(totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
                /*if (splitGranularity != null) {
                    if (splitGranularity < SwapConstant.MIN_SPLIT_GRANULARITY) {
                        throw new RuntimeException("error splitGranularity: " + splitGranularity);
                    }
                    int splitNum = calcSplitNumP2SH(totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
                    feeSize = calcFeeMultiSign(usingUtxos.size(), splitNum, opReturnSize, m, n);
                } else {
                    feeSize = calcFeeMultiSign(usingUtxos.size(), outputNum, opReturnSize, m, n);
                }*/

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
                TransactionOutPoint outPoint = new TransactionOutPoint(FchMainNetwork.MAINNETWORK, (long) input.getBirthIndex(), Sha256Hash.wrap(input.getBirthTxId()));
                TransactionInput unsignedInput = new TransactionInput(new fcTools.FchMainNetwork(), transaction, new byte[0], outPoint, Coin.valueOf(input.getValue()));
                transaction.addInput(unsignedInput);
            }

            if (amount + fee > totalMoney) {
                throw new RuntimeException("input is not enough");
            } else {
                long change = totalMoney - amount - fee;
                if (change > Constants.DustInSatoshi) {
                    Address changeAddress = Address.fromBase58(FchMainNetwork.MAINNETWORK, multiSignAddr);
                    List<Long> changes = getChanges(splitGranularity, change);
                    for (Long c : changes) {
                        transaction.addOutput(Coin.valueOf(c), changeAddress);
                    }
                }
                return new Object[]{transaction.bitcoinSerialize(), p2SH, transaction, usingUtxos};
            }
        }
    }

    private static List<Long> getChanges(Long splitGranularity, long change) {
        List<Long> changes = new ArrayList<>();
        if (splitGranularity != null && splitGranularity > Constants.DustInSatoshi) {
            if (change >= 2 * splitGranularity) {
                long remain = change;
                while (remain >= splitGranularity) {
                    changes.add(splitGranularity);
                    remain -= splitGranularity;
                }
                if (remain > 0) {
                    changes.remove(changes.size() - 1);
                    changes.add(remain + splitGranularity);
                }
            } else {
                changes.add(change);
            }
        } else {
            changes.add(change);
        }
        return changes;
    }

    public static Cash converterUTXOToCash(String txid, int vout, long value) {
        Cash cash = new Cash();
        cash.setBirthTxId(txid);
        cash.setBirthIndex(vout);
        cash.setValue(value);
        return cash;
    }

    public static byte[] createMultiSignByOne(
            ECKey privKey,
            List<ECKey> pubEcKeys,
            List<Cash> inputs,
            String to,
            long amount,
            String opReturn,
            int m, int n,
            long feeRate,
            boolean useAllUTXO, Long splitGranularity) throws Exception {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity);
        byte[] unsignedTx = (byte[]) rawTxBase[0];
        P2SH p2sh = (P2SH) rawTxBase[1];
        List<Cash> realInputs = (List<Cash>) rawTxBase[3];
        MultiSigData multiSignDataOne = FchTool.signSchnorrMultiSignTx(new MultiSigData(unsignedTx, p2sh, realInputs), privKey.getPrivKeyBytes());
        Map.Entry<String, List<byte[]>> entry = multiSignDataOne.getFidSigMap().entrySet().iterator().next();
        BtcSignData signData = new BtcSignData(privKey.getPubKey(), entry.getValue());
        return signData.serialize();
    }

    public static boolean verifyMultiSignByOne(
            byte[] pub, List<byte[]> signatures,
            List<ECKey> pubEcKeys,
            List<Cash> inputs,
            String to,
            long amount,
            String opReturn,
            int m, int n,
            long feeRate,
            boolean useAllUTXO, Long splitGranularity) throws Exception {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity);
        byte[] unsignedTx = (byte[]) rawTxBase[0];
        P2SH p2sh = (P2SH) rawTxBase[1];
        Transaction tx = (Transaction) rawTxBase[2];
        byte[] redeemScript = HexUtil.decode(p2sh.getRedeemScript());

        List<TransactionInput> spendTxInputs = tx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            boolean verify = FchTool.rawTxSigVerify(unsignedTx, pub, signatures.get(k), k, input.getValue().value, redeemScript);
            if (!verify) {
                return false;
            }
        }
        return true;
    }

    public static int verifyMultiSignCount(
            Map<String, List<byte[]>> signatures,
            List<ECKey> pubEcKeys,
            List<Cash> inputs,
            String to,
            long amount,
            String opReturn,
            int m, int n,
            long feeRate,
            boolean useAllUTXO, Long splitGranularity) throws Exception {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity);
        P2SH p2sh = (P2SH) rawTxBase[1];
        Transaction tx = (Transaction) rawTxBase[2];
        byte[] redeemScript = HexUtil.decode(p2sh.getRedeemScript());
        Script script = new Script(redeemScript);
        List<TransactionInput> spendTxInputs = tx.getInputs();
        List<Sha256Hash> inputHashList = new ArrayList<>();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = tx.hashForSignatureWitness(input.getIndex(), script, input.getValue(), Transaction.SigHash.ALL, false);
            inputHashList.add(sighash);
        }
        Set<Map.Entry<String, List<byte[]>>> entries = signatures.entrySet();
        int result = 0;
        for (Map.Entry<String, List<byte[]>> entry : entries) {
            byte[] pub = HexUtil.decode(entry.getKey());
            List<byte[]> signatureList = entry.getValue();
            boolean check = true;
            for (int r = 0; r < inputHashList.size(); r++) {
                Sha256Hash sighash = inputHashList.get(r);
                if (!SchnorrSignature.schnorr_verify(sighash.getBytes(), pub, signatureList.get(r))) {
                    check = false;
                    break;
                }
            }
            if (check) {
                result++;
            }
        }
        return result;
    }

    public static String createMultiSignTx(
            Map<String, List<byte[]>> signatures,
            List<ECKey> pubEcKeys,
            List<Cash> inputs,
            String to,
            long amount,
            String opReturn,
            int m, int n,
            long feeRate,
            boolean useAllUTXO, Long splitGranularity) throws Exception {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity);
        byte[] unsignedTx = (byte[]) rawTxBase[0];
        P2SH p2sh = (P2SH) rawTxBase[1];
        //return FchTool.buildSchnorrMultiSignTx(unsignedTx, signatures, p2sh);

        //List<ECKey> _pubKeys = new ArrayList<>(pubEcKeys);
        //Collections.sort(_pubKeys, ECKey.PUBKEY_COMPARATOR);

        Transaction transaction = new Transaction(FchMainNetwork.MAINNETWORK, unsignedTx);

        List<TransactionInput> spendTxInputs = transaction.getInputs();
        for (int i = 0; i < spendTxInputs.size(); ++i) {
            TransactionInput input = spendTxInputs.get(i);
            List<byte[]> sigListByTx = new ArrayList<>();
            String[] fids = p2sh.getFids();
            int fidsLength = fids.length;
            int valid = 0;
            for (int k = 0; k < fidsLength; ++k) {
                if (valid == m) {
                    break;
                }
                String fid = fids[k];
                List<byte[]> signList = signatures.get(fid);
                if (signList == null || signList.isEmpty() || signList.size() < spendTxInputs.size()) {
                    continue;
                }
                byte[] sig = signList.get(i);
                sigListByTx.add(sig);
                valid++;
            }
            if (valid < m) {
                throw new RuntimeException(String.format("WITHDRAWAL_NOT_ENOUGH_SIGNATURE, params: inputIndex-%s, valid-%s, minNeed-%s, opReturn-%s", i, valid, m, opReturn));
            }

            Script inputScript = FchTool.createSchnorrMultiSigInputScriptBytes(sigListByTx, javaTools.HexUtil.decode(p2sh.getRedeemScript()));
            input.setScriptSig(inputScript);
        }

        byte[] signResult = transaction.bitcoinSerialize();
        return Utils.HEX.encode(signResult);
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
}
