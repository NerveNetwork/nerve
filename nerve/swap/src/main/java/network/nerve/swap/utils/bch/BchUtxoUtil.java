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
package network.nerve.swap.utils.bch;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.swap.utils.fch.BtcSignData;
import network.nerve.swap.utils.fch.UTXOData;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.SchnorrSignature;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/7/11
 */
public class BchUtxoUtil {

    public static long calcFeeSize(int inputNum, int outputNum, int opReturnBytesLen) {
        long length;
        if (opReturnBytesLen == 0) {
            length = 10L + 141L * (long) inputNum + 34L * (long) (outputNum + 1);
        } else {
            length = 10L + 141L * (long) inputNum + 34L * (long) (outputNum + 1) + (long) (opReturnBytesLen + VarInt.sizeOf((long) opReturnBytesLen) + 1 + VarInt.sizeOf((long) (opReturnBytesLen + VarInt.sizeOf((long) opReturnBytesLen) + 1)) + 8);
        }

        return length;
    }

    public static long DustInSatoshi = 1000L;

    public static String createTransactionSign(NetworkParameters networkParameters, String from, List<UTXOData> inputs, byte[] priKey, String to, long amount, long feeRate, byte[] opReturn) {
        String changeToFid = from;
        long fee, feeSize;
        if (opReturn != null) {
            feeSize = calcFeeSize(inputs.size(), 1, opReturn.length);
        } else {
            feeSize = calcFeeSize(inputs.size(), 1, 0);
        }
        fee = feeSize * feeRate;

        Transaction transaction = new Transaction(networkParameters);
        long totalMoney = 0L;
        long totalOutput = 0L;
        ECKey eckey = ECKey.fromPrivate(priKey);
        totalOutput += amount;
        transaction.addOutput(Coin.valueOf(amount), CashAddressFactory.create().getFromFormattedAddress(networkParameters, to));

        if (opReturn != null && opReturn.length > 0) {
            try {
                Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn);
                transaction.addOutput(Coin.ZERO, opreturnScript);
            } catch (Exception var20) {
                throw new RuntimeException(var20);
            }
        }

        Iterator var13 = inputs.iterator();
        while (var13.hasNext()) {
            UTXOData input = (UTXOData) var13.next();
            totalMoney += input.getAmount().longValue();
            TransactionOutPoint outPoint = new TransactionOutPoint(networkParameters, (long) input.getVout(), Sha256Hash.wrap(input.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(networkParameters, transaction, new byte[0], outPoint);
            transaction.addInput(unsignedInput);
        }

        if (totalOutput + fee > totalMoney) {
            throw new RuntimeException("input is not enough");
        } else {
            long change = totalMoney - totalOutput - fee;
            if (change > DustInSatoshi) {
                transaction.addOutput(Coin.valueOf(change), CashAddressFactory.create().getFromFormattedAddress(networkParameters, changeToFid));
            }

            for (int i = 0; i < inputs.size(); ++i) {
                UTXOData input = (UTXOData) inputs.get(i);
                Script script = ScriptBuilder.createP2PKHOutputScript(eckey);
                SchnorrSignature signature = transaction.calculateSchnorrSignature(i, eckey, script.getProgram(), Coin.valueOf(input.getAmount().longValue()), Transaction.SigHash.ALL, false);
                Script schnorr = ScriptBuilder.createSchnorrInputScript(signature, eckey);
                transaction.getInput((long) i).setScriptSig(schnorr);
            }

            byte[] signResult = transaction.bitcoinSerialize();
            return Utils.HEX.encode(signResult);
        }
    }

    public static String multiAddrByECKey(List<ECKey> pubKeys, int m, boolean mainnet) {
        return (String) multiAddrInfoByECKey(pubKeys, m, mainnet)[0];
    }

    private static Object[] multiAddrInfoByECKey(List<ECKey> pubKeys, int m, boolean mainnet) {
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet4ParamsForNerve.get();
        int p2SHHeader = networkParameters.getP2SHHeader();
        Script script = genMultiP2SH(pubKeys, m);
        byte[] redeemScriptBytes = script.getProgram();
        String multiAddr = scriptToMultiAddr(HexUtil.encode(redeemScriptBytes), p2SHHeader);
        return new Object[]{multiAddr, script};
    }

    public static String multiAddr(List<byte[]> pubKeyList, int m, boolean mainnet) {
        List<ECKey> pubKeys = pubKeyList.stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        return multiAddrByECKey(pubKeys, m, mainnet);
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

    public static final Comparator<UTXOData> BITCOIN_SYS_COMPARATOR = new Comparator<UTXOData>() {
        @Override
        public int compare(UTXOData o1, UTXOData o2) {
            // order asc
            int compare = o1.getAmount().compareTo(o2.getAmount());
            if (compare == 0) {
                int compare1 = o1.getTxid().compareTo(o2.getTxid());
                if (compare1 == 0) {
                    return Integer.compare(o1.getVout(), o2.getVout());
                }
                return compare1;
            }
            return compare;
        }
    };

    public static Object[] createMultiSignRawTxBase(
            List<ECKey> pubEcKeys, List<UTXOData> inputs,
            String to, long amount, String opReturnHex,
            int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity, boolean mainnet) {
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet4ParamsForNerve.get();
        Object[] multiAddrInfo = multiAddrInfoByECKey(pubEcKeys, m, mainnet);
        String multiSignAddr = (String) multiAddrInfo[0];
        multiSignAddr = CashAddressFactory.create().getFromBase58(networkParameters, multiSignAddr).toString();
        //System.out.println(String.format("from createMultiSignRawTxBase: multiSignAddr: %s", multiSignAddr));
        Script redeemScript = (Script) multiAddrInfo[1];
        long fee = 0;
        int opReturnSize = 0;
        byte[] opReturn = null;
        if (StringUtils.isNotBlank(opReturnHex)) {
            opReturn = HexUtil.decode(opReturnHex);
            opReturnSize = opReturn.length;
        }

        Collections.sort(inputs, BITCOIN_SYS_COMPARATOR);

        List<UTXOData> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        boolean enoughUTXO = false;
        for (UTXOData utxo : inputs) {
            usingUtxos.add(utxo);
            totalMoney += utxo.getAmount().longValue();
            long feeSize = calcFeeMultiSignWithSplitGranularity(totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
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

        Transaction transaction = new Transaction(networkParameters);
        transaction.addOutput(Coin.valueOf(amount), CashAddressFactory.create().getFromFormattedAddress(networkParameters, to));

        if (opReturn != null) {
            try {
                Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn);
                transaction.addOutput(Coin.ZERO, opreturnScript);
            } catch (Exception var16) {
                throw new RuntimeException(var16);
            }
        }

        for (UTXOData usingUtxo : usingUtxos) {
            UTXOData input = usingUtxo;
            TransactionOutPoint outPoint = new TransactionOutPoint(networkParameters, (long) input.getVout(), Sha256Hash.wrap(input.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(networkParameters, transaction, new byte[0], outPoint, Coin.valueOf(input.getAmount().longValue()));
            transaction.addInput(unsignedInput);
        }

        if (amount + fee > totalMoney) {
            throw new RuntimeException("input is not enough");
        } else {
            long change = totalMoney - amount - fee;
            if (change > DustInSatoshi) {
                Address changeAddress = CashAddressFactory.create().getFromFormattedAddress(networkParameters, multiSignAddr);
                List<Long> changes = getChanges(splitGranularity, change);
                for (Long c : changes) {
                    transaction.addOutput(Coin.valueOf(c), changeAddress);
                }
            }
            //System.out.println("createMultiSignRawTxBase: " + HexUtil.encode(transaction.bitcoinSerialize()));
            return new Object[]{transaction.bitcoinSerialize(), redeemScript, transaction, usingUtxos};
        }
    }


    public static byte[] bytesMerger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    private static List<Long> getChanges(Long splitGranularity, long change) {
        List<Long> changes = new ArrayList<>();
        if (splitGranularity != null && splitGranularity > DustInSatoshi) {
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

    private static Script genMultiP2SH(List<ECKey> keys, int m) {
        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(m, keys);
        return multiSigScript;
    }

    private static String scriptToMultiAddr(String scriptHex, int version) {
        byte[] scriptBytes = HexUtil.decode(scriptHex);
        byte[] h = SerializeUtils.sha256hash160(scriptBytes);
        return hash160ToMultiAddr(h, version);
    }

    private static String hash160ToMultiAddr(byte[] hash160Bytes, int version) {

        byte[] d = new byte[]{(byte) version};
        byte[] e = new byte[21];
        System.arraycopy(d, 0, e, 0, 1);
        System.arraycopy(hash160Bytes, 0, e, 1, 20);
        byte[] c = Sha256Hash.hashTwice(e);
        byte[] f = new byte[4];
        System.arraycopy(c, 0, f, 0, 4);
        byte[] addrRaw = new byte[25];
        System.arraycopy(e, 0, addrRaw, 0, 21);
        System.arraycopy(f, 0, addrRaw, 21, 4);
        return Base58.encode(addrRaw);
    }

    public static byte[] createMultiSigTxByOne(ECKey privKey,
                                               List<ECKey> pubEcKeys, List<UTXOData> inputs,
                                               String to, long amount, String opReturn,
                                               int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws IOException {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity, mainnet);
        Transaction spendTx = (Transaction) rawTxBase[2];
        Script redeemScript = (Script) rawTxBase[1];
        List<byte[]> result = new ArrayList<>();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            Sha256Hash sighash = spendTx.hashForSignatureWitness(input.getIndex(), redeemScript, input.getValue(), Transaction.SigHash.ALL, false);
            ECKey.ECDSASignature ecdsaSignature = privKey.sign(sighash);
            result.add(ecdsaSignature.encodeToDER());
        });

        BtcSignData signData = new BtcSignData(privKey.getPubKey(), result);
        return signData.serialize();
    }

    public static boolean verifyMultiSigTxByOne(ECKey pub, List<byte[]> signatures,
                                                List<ECKey> pubEcKeys, List<UTXOData> inputs,
                                                String to, long amount, String opReturn,
                                                int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity, boolean mainnet) {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity, mainnet);
        Transaction spendTx = (Transaction) rawTxBase[2];
        Script redeemScript = (Script) rawTxBase[1];

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = spendTx.hashForSignatureWitness(input.getIndex(), redeemScript, input.getValue(), Transaction.SigHash.ALL, false);

            byte[] signStr = signatures.get(k);
            ECKey.ECDSASignature ecdsaSignature;
            try {
                ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(signStr);
            } catch (SignatureDecodeException e) {
                throw new RuntimeException(e);
            }
            if (!ECKey.verify(sighash.getBytes(), ecdsaSignature, pub.getPubKey())) {
                //System.err.println(String.format("input %s verify error", k));
                return false;
            }
            //System.out.println(String.format("input %s verify pass", k));
        }
        return true;

    }

    public static int verifyMultiSignCount(Map<String, List<byte[]>> signatures,
                                           List<ECKey> pubEcKeys, List<UTXOData> inputs,
                                           String to, long amount, String opReturn,
                                           int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity, boolean mainnet) {
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity, mainnet);
        Transaction spendTx = (Transaction) rawTxBase[2];
        Script redeemScript = (Script) rawTxBase[1];

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        List<Sha256Hash> inputHashList = new ArrayList<>();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = spendTx.hashForSignatureWitness(input.getIndex(), redeemScript, input.getValue(), Transaction.SigHash.ALL, false);
            inputHashList.add(sighash);
        }

        Set<Map.Entry<String, List<byte[]>>> entries = signatures.entrySet();
        int result = 0;
        for (Map.Entry<String, List<byte[]>> entry : entries) {
            ECKey pub = ECKey.fromPublicOnly(HexUtil.decode(entry.getKey()));
            List<byte[]> signatureList = entry.getValue();
            boolean check = true;
            for (int r = 0; r < inputHashList.size(); r++) {
                Sha256Hash sighash = inputHashList.get(r);
                byte[] signStr = signatureList.get(r);
                ECKey.ECDSASignature ecdsaSignature;
                try {
                    ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(signStr);
                } catch (SignatureDecodeException e) {
                    throw new RuntimeException(e);
                }
                if (!ECKey.verify(sighash.getBytes(), ecdsaSignature, pub.getPubKey())) {
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

    public static String createMultiSigTxByMulti(Map<String, List<byte[]>> signatures,
                                                 List<ECKey> pubEcKeys, List<UTXOData> inputs,
                                                 String to, long amount, String opReturn,
                                                 int m, int n, long feeRate, boolean useAllUTXO, Long splitGranularity, boolean mainnet) throws SignatureDecodeException {
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet4ParamsForNerve.get();
        Object[] rawTxBase = createMultiSignRawTxBase(pubEcKeys, inputs, to, amount, opReturn, m, n, feeRate, useAllUTXO, splitGranularity, mainnet);
        Transaction spendTx = (Transaction) rawTxBase[2];
        Script redeemScript = (Script) rawTxBase[1];

        List<ECKey> _pubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(_pubKeys, ECKey.PUBKEY_COMPARATOR);
        //_pubKeys.forEach(p -> System.out.println(p.getPublicKeyAsHex()));

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            ArrayList<TransactionSignature> txSigs = new ArrayList<>();
            int valid = 0;
            for (int i = 0; i < n; i++) {
                if (valid == m) {
                    break;
                }
                ECKey ecKey = _pubKeys.get(i);
                List<byte[]> signList = signatures.get(ecKey.getPublicKeyAsHex());
                if (signList == null || signList.isEmpty() || signList.size() < spendTxInputs.size()) {
                    continue;
                }
                ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(signList.get(k));
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false, true);
                txSigs.add(signature);
                valid++;
            }
            if (valid < m) {
                throw new RuntimeException("WITHDRAWAL_NOT_ENOUGH_SIGNATURE");
            }

            //Build tx input script
            Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(txSigs, redeemScript);
            input.setScriptSig(inputScript);
        }

        spendTx.verify();

        Context.propagate(new Context(networkParameters));
        spendTx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        spendTx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        //System.out.println("Tx hex: " + ByteUtils.formatHex(spendTx.serialize()));
        return HexUtil.encode(spendTx.bitcoinSerialize());
    }
}
