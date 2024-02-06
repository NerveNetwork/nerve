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
package network.nerve.converter.heterogeneouschain.btc;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.SerializeUtils;
import org.bitcoinj.base.*;
import org.bitcoinj.base.internal.ByteUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Test;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bitcoinj.base.BitcoinNetwork.MAINNET;
import static org.bitcoinj.base.BitcoinNetwork.TESTNET;

/**
 * @author: PierreLuo
 * @date: 2023/11/16
 */
public class BTCUtilsTest {

    private static final BigInteger _0n = BigInteger.ZERO;
    private static final BigInteger _1n = BigInteger.ONE;
    private static final BigInteger _2n = BigInteger.valueOf(2);
    private static final BigInteger _3n = BigInteger.valueOf(3);
    private static final BigInteger _8n = BigInteger.valueOf(8);
    private static final BigInteger _Pn = new BigInteger("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16);

    private static BigInteger mod(BigInteger a, BigInteger b) {
        if (b == null) b = _Pn;
        BigInteger result = a.mod(b);
        return result.compareTo(_0n) >= 0 ? result : b.add(result);
    }

    private static BigInteger invert(BigInteger number, BigInteger modulo) {
        if (modulo == null) modulo = _Pn;
        if (number.compareTo(_0n) == 0 || modulo.compareTo(_0n) <= 0) {
            throw new IllegalArgumentException("invert: expected positive integers");
        }
        BigInteger a = mod(number, modulo);
        BigInteger b = modulo;
        BigInteger x = _0n, y = _1n, u = _1n, v = _0n;
        while (a.compareTo(_0n) != 0) {
            BigInteger[] divAndRemainder = b.divideAndRemainder(a);
            BigInteger q = divAndRemainder[0];
            BigInteger r = divAndRemainder[1];
            BigInteger m = x.subtract(u.multiply(q));
            BigInteger n = y.subtract(v.multiply(q));
            b = a;
            a = r;
            x = u;
            y = v;
            u = m;
            v = n;
        }
        BigInteger gcd = b;
        if (gcd.compareTo(_1n) != 0)
            throw new ArithmeticException("invert: does not exist");
        return mod(x, modulo);
    }

    public static BigInteger[] add(BigInteger X1, BigInteger Y1, BigInteger Z1, BigInteger X2, BigInteger Y2, BigInteger Z2) {
        if (X2.compareTo(_0n) == 0 || Y2.compareTo(_0n) == 0)
            return new BigInteger[]{_0n};
        if (X1.compareTo(_0n) == 0 || Y1.compareTo(_0n) == 0)
            return new BigInteger[]{_1n};
        BigInteger Z1Z1 = mod(Z1.multiply(Z1), _Pn);
        BigInteger Z2Z2 = mod(Z2.multiply(Z2), _Pn);
        BigInteger U1 = mod(X1.multiply(Z2Z2), _Pn);
        BigInteger U2 = mod(X2.multiply(Z1Z1), _Pn);
        BigInteger S1 = mod(Y1.multiply(Z2).multiply(Z2Z2), _Pn);
        BigInteger S2 = mod(Y2.multiply(Z1).multiply(Z1Z1), _Pn);
        BigInteger H = mod(U2.subtract(U1), _Pn);
        BigInteger r = mod(S2.subtract(S1), _Pn);

        if (H.compareTo(_0n) == 0) {
            if (r.compareTo(_0n) == 0) {
                return doublePoint(X1, Y1, Z1);
            } else {
                return new BigInteger[]{_2n};
            }
        }

        BigInteger HH = mod(H.multiply(H), _Pn);
        BigInteger HHH = mod(H.multiply(HH), _Pn);
        BigInteger V = mod(U1.multiply(HH), _Pn);
        BigInteger X3 = mod(r.multiply(r).subtract(HHH).subtract(_2n.multiply(V)), _Pn);
        BigInteger Y3 = mod(r.multiply(V.subtract(X3)).subtract(S1.multiply(HHH)), _Pn);
        BigInteger Z3 = mod(Z1.multiply(Z2).multiply(H), _Pn);
        return new BigInteger[]{X3, Y3, Z3};
    }

    private static BigInteger[] doublePoint(BigInteger X1, BigInteger Y1, BigInteger Z1) {
        BigInteger A = mod(X1.multiply(X1), _Pn);
        BigInteger B = mod(Y1.multiply(Y1), _Pn);
        BigInteger C = mod(B.multiply(B), _Pn);
        BigInteger x1b = X1.add(B);
        BigInteger D = mod(_2n.multiply(mod(x1b.multiply(x1b), _Pn)).subtract(A).subtract(C), _Pn);
        BigInteger E = mod(_3n.multiply(A), _Pn);
        BigInteger F = mod(E.multiply(E), _Pn);
        BigInteger X3 = mod(F.subtract(_2n.multiply(D)), _Pn);
        BigInteger Y3 = mod(E.multiply(D.subtract(X3)).subtract(_8n.multiply(C)), _Pn);
        BigInteger Z3 = mod(_2n.multiply(Y1).multiply(Z1), _Pn);
        return new BigInteger[]{X3, Y3, Z3};
    }

    public static BigInteger[] toAffine(BigInteger x, BigInteger y, BigInteger z, BigInteger invZ) {
        boolean isZero = x.compareTo(_0n) == 0 && y.compareTo(_1n) == 0 && z.compareTo(_0n) == 0;
        if (invZ == null)
            invZ = isZero ? _8n : invert(z, _Pn);
        BigInteger iz1 = invZ;
        BigInteger iz2 = mod(iz1.multiply(iz1), _Pn);
        BigInteger iz3 = mod(iz2.multiply(iz1), _Pn);
        BigInteger ax = mod(x.multiply(iz2), _Pn);
        BigInteger ay = mod(y.multiply(iz3), _Pn);
        BigInteger zz = mod(z.multiply(iz1), _Pn);
        if (isZero)
            return new BigInteger[]{_0n, _0n};
        if (zz.compareTo(_1n) != 0)
            throw new ArithmeticException("invZ was invalid");
        return new BigInteger[]{ax, ay};
    }

    @Test
    public void taprootOutKey() {
        BigInteger px = new BigInteger("92516168983136813688791982656401458074689984250153488059681564220650190520597");
        BigInteger py = new BigInteger("57188118985598196732645388381499269207388237457597072595414086001651093223476");
        BigInteger tx = new BigInteger("29166793149529803724344256992636175012260386412772270463291912978755956864517");
        BigInteger ty = new BigInteger("35717118580948425330164644954948624541559750813176332794893238924373456641500");
        BigInteger[] addRe = add(px, py, _1n, tx, ty, _1n);
        System.out.println(Arrays.toString(addRe));
        BigInteger[] toAffineRe = toAffine(addRe[0], addRe[1], addRe[2], null);
        System.out.println(Arrays.toString(toAffineRe));
        System.out.println(toAffineRe[0].toString());
        System.out.println(HexUtil.encode(toAffineRe[0].toByteArray()));
        System.out.println(toAffineRe[0].toString(16).equals("a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c"));
        System.out.println(new BigInteger("75098798818242655252486134754694785223032048533939306124635785683558282848332").toString(16));
    }

    public static String makeMultiAddr(List<org.bitcoinj.crypto.ECKey> pubKeys, int n, boolean mainnet) {
        NetworkParameters network = mainnet ? MainNetParams.get() : TestNet3Params.get();
        pubKeys = new ArrayList<>(pubKeys);
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(n, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();
        return multiSigAddress;
    }

    public static String multiAddr(List<byte[]> pubKeyList, int n, boolean mainnet) {
        List<org.bitcoinj.crypto.ECKey> pubKeys = pubKeyList.stream().map(p -> org.bitcoinj.crypto.ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        int p2SHHeader = networkParameters.getP2SHHeader();
        Script script = genMultiP2SH(pubKeys, n);
        byte[] redeemScriptBytes = script.program();
        String multiAddr = scriptToMultiAddr(HexUtil.encode(redeemScriptBytes), p2SHHeader);
        return multiAddr;
    }

    public static Script genMultiP2SH(List<org.bitcoinj.crypto.ECKey> keys, int n) {
        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(n, keys);
        return multiSigScript;
    }

    public static String scriptToMultiAddr(String script, int version) {
        byte[] scriptBytes = HexUtil.decode(script);
        byte[] h = SerializeUtils.sha256hash160(scriptBytes);
        return hash160ToMultiAddr(h, version);
    }

    public static String hash160ToMultiAddr(byte[] hash160Bytes, int version) {

        byte[] d = new byte[]{(byte) version};
        byte[] e = new byte[21];
        System.arraycopy(d, 0, e, 0, 1);
        System.arraycopy(hash160Bytes, 0, e, 1, 20);
        byte[] c = org.bitcoinj.base.Sha256Hash.hashTwice(e);
        byte[] f = new byte[4];
        System.arraycopy(c, 0, f, 0, 4);
        byte[] addrRaw = new byte[25];
        System.arraycopy(e, 0, addrRaw, 0, 21);
        System.arraycopy(f, 0, addrRaw, 21, 4);
        return Base58.encode(addrRaw);
    }

    public static byte[] taggedHash(String tag, byte[] msg) {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        //System.out.println("tagHash===" + HexUtil.encode(tagHash));
        byte[] doubleTagHash = Arrays.copyOf(tagHash, tagHash.length * 2);
        System.arraycopy(tagHash, 0, doubleTagHash, tagHash.length, tagHash.length);
        //System.out.println("doubleTagHash===" + HexUtil.encode(doubleTagHash));
        byte[] input = new byte[tagHash.length * 2 + msg.length];
        System.arraycopy(doubleTagHash, 0, input, 0, doubleTagHash.length);
        System.arraycopy(msg, 0, input, doubleTagHash.length, msg.length);
        //System.out.println("input===" + HexUtil.encode(input));
        return Sha256Hash.hash(input);
    }

    public static String genBtcTaprootAddressByPub(String pub, boolean mainnet) {
        byte[] pubBytes = Numeric.hexStringToByteArray(pub);
        if (!org.bitcoinj.crypto.ECKey.isPubKeyCompressed(pubBytes)) {
            throw new RuntimeException("Error Compressed PubKey");
        }
        if (pubBytes[0] == 0x03) {
            pubBytes[0] = 0x02;
        }
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(pubBytes);
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] y = pubKeyPoint.getYCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);
        org.bitcoinj.crypto.ECKey tKey = org.bitcoinj.crypto.ECKey.fromPrivate(t);
        byte[] tweakX = tKey.getPubKeyPoint().getXCoord().getEncoded();
        byte[] tweakY = tKey.getPubKeyPoint().getYCoord().getEncoded();
        BigInteger px = new BigInteger(1, x);
        BigInteger py = new BigInteger(1, y);
        BigInteger tx = new BigInteger(1, tweakX);
        BigInteger ty = new BigInteger(1, tweakY);
        BigInteger[] addRe = add(px, py, _1n, tx, ty, _1n);
        BigInteger[] toAffineRe = toAffine(addRe[0], addRe[1], addRe[2], null);
        String outKey = toAffineRe[0].toString(16);
        SegwitAddress segwitAddress = SegwitAddress.fromProgram(mainnet ? MainNetParams.get() : TestNet3Params.get(), 1, Numeric.hexStringToByteArray(outKey));
        return segwitAddress.toBech32();
    }

    /**
     * Generate native Segwit address using private key
     */
    public static String getBtcSegregatedWitnessAddressByPri(String prikey, boolean mainnet) {
        String address = "";
        try {
            NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
            BigInteger privkeybtc = new BigInteger(1, HexUtil.decode(prikey));
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(privkeybtc);
            SegwitAddress segwitAddress = SegwitAddress.fromKey(networkParameters, ecKey);
            address = segwitAddress.toBech32();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }


    public static String getBtcLegacyAddress(String pubKey, boolean mainnet) {
        byte[] pubKeyBytes = Numeric.hexStringToByteArray(pubKey);
        LegacyAddress legacyAddress = LegacyAddress.fromPubKeyHash(mainnet ? MAINNET : TESTNET, SerializeUtils.sha256hash160(pubKeyBytes));
        return legacyAddress.toString();
    }

    /**
     * Generate SegWit compatible address using public key
     */
    public static String genSegWitCompatibleAddress(String pubKey, boolean mainnet) {
        byte[] rip = SerializeUtils.sha256hash160(HexUtil.decode(pubKey));
        byte[] redeem_script = new byte[22];
        redeem_script[0] = 0x0;
        redeem_script[1] = 0x14;
        System.arraycopy(rip, 0, redeem_script, 2, 20);
        byte[] redeem_rip = SerializeUtils.sha256hash160(redeem_script);
        String address = org.bitcoinj.base.Base58.encodeChecked(mainnet ? 0x05 : 0xc4, redeem_rip);
        return address;
    }

    /**
     * Generate native Segwit address using public key
     */
    public static String getBtcSegregatedWitnessAddressByPubkey(String pubKey, boolean mainnet) {
        String address = "";
        try {
            NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(Numeric.hexStringToByteArray(pubKey));
            SegwitAddress segwitAddress = SegwitAddress.fromKey(networkParameters, ecKey);
            address = segwitAddress.toBech32();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    /**
     * Calculate btc private key using evm system private key
     *
     * @param evmPrikey Ethernet private key
     * @param mainnet   mainnet
     * @return
     */
    public static String calcBtcPriByEvmPri(String evmPrikey, boolean mainnet) {
        String prefix = "ef";
        if (mainnet) {
            prefix = "80";
        }
        String cleanHexPrefix = Numeric.cleanHexPrefix(evmPrikey);
        if (cleanHexPrefix.length() != 64) {
            throw new RuntimeException("Error Private Key");
        }
        String extendedKey = prefix + cleanHexPrefix + "01";
        String hashTwiceHex = Numeric.toHexStringNoPrefix(Sha256Hash.hashTwice(Numeric.hexStringToByteArray(extendedKey)));
        String checksum = hashTwiceHex.substring(0, 8);
        extendedKey += checksum;
        return Base58.encode(Numeric.hexStringToByteArray(extendedKey));
    }

    /**
     * Use btc private key to calculate evm system private key
     *
     * @param btcPrikey btc network private key
     * @param mainnet   mainnet
     * @return
     */
    public static String calcEvmPriByBtcPri(String btcPrikey, boolean mainnet) {
        String hex;
        try {
            hex = Numeric.toHexStringNoPrefix(Base58.decode(btcPrikey));
        } catch (Exception e) {
            throw new RuntimeException("Error Private Key");
        }
        String prefix = hex.substring(0, 2);
        if (mainnet) {
            if (!prefix.equals("80")) {
                throw new RuntimeException("Error Private Key");
            }
        } else {
            if (!prefix.equalsIgnoreCase("ef")) {
                throw new RuntimeException("Error Private Key");
            }
        }
        String checksum = hex.substring(hex.length() - 8, hex.length());
        String extendedKey = hex.substring(0, hex.length() - 8);
        String hashTwiceHex = Numeric.toHexStringNoPrefix(Sha256Hash.hashTwice(Numeric.hexStringToByteArray(extendedKey)));
        String calcChecksum = hashTwiceHex.substring(0, 8);
        if (!checksum.equalsIgnoreCase(calcChecksum)) {
            throw new RuntimeException("Error checksum");
        }
        return extendedKey.substring(2, 66);
    }

    public static Transaction sendTransactionOffline(
            String priKey,
            String receiveAddr,
            String fromAddr,
            long amount,
            List<UTXO> utxos,
            List<byte[]> opReturns,
            boolean mainnet) {
        long fee;
        if (opReturns != null) {
            int[] opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
            fee = calcFeeSize(utxos.size(), 1, opReturnSize);
        } else {
            fee = calcFeeSize(utxos.size(), 1, null);
        }
        //fee += 60;
        System.out.println("fee: " + fee);
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Wallet basic = Wallet.createBasic(network);
        if (!utxos.isEmpty() && null != utxos) {
            BigInteger privkey = new BigInteger(1, HexUtil.decode(priKey));
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(privkey);
            Address receiveAddress = basic.parseAddress(receiveAddr);
            Transaction tx = new Transaction();

            //First arrange the obtained utxo array from large to small
            Comparator<UTXO> comparator = (o1, o2) -> {
                if (o1.getValue().value < o2.getValue().value)
                    return 1;
                else
                    return -1;
            };
            Collections.sort(utxos, comparator);
            List<UTXO> usingUtxos = new ArrayList<>();
            long totalMoney = 0;
            for (int k = 0; k < utxos.size(); k++) {
                if (totalMoney >= (amount + fee))
                    break;
                usingUtxos.add(utxos.get(k));
                totalMoney += utxos.get(k).getValue().value;
            }
            // Output of transfer amount
            tx.addOutput(Coin.valueOf(amount), receiveAddress);
            // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
            Address toAddress = basic.parseAddress(fromAddr);
            long leave = totalMoney - amount - fee;
            if (leave > 0) {
                //Change output
                tx.addOutput(Coin.valueOf(leave), toAddress);
            }
            //Add OP_RETURN
            if (opReturns != null) {
                for (byte[] opReturn : opReturns) {
                    try {
                        Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn);
                        tx.addOutput(Coin.ZERO, opreturnScript);
                    } catch (Exception var20) {
                        throw new RuntimeException(var20);
                    }
                }
            }

            for (int i = 0; i < usingUtxos.size(); i++) {
                UTXO usingUtxo = usingUtxos.get(i);
                //Add input
                TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getIndex(), usingUtxo.getHash());
                tx.addSignedInput(outPoint, usingUtxo.getScript(), usingUtxo.getValue(), ecKey);
            }

            Transaction.verify(network, tx);

            Context.propagate(new Context());
            tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
            tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
            String hex = HexUtil.encode(tx.bitcoinSerialize());
            System.out.println(tx.getInput(0).serialize().length);
            System.out.println(tx.getOutput(0).serialize().length);
            System.out.println("transferTest1: " + hex);
            return tx;
        }
        return null;
    }

    public static long calcFeeSize(int inputNum, int outputNum, int[] opReturnBytesLen) {
        long baseLength = 10;
        long inputLength = 148 * (long) inputNum;
        long outputLength = 39 * (long) (outputNum + 1); // Include change output

        int opReturnLen = 0;
        if (opReturnBytesLen != null && opReturnBytesLen.length > 0) {
            for (int len : opReturnBytesLen) {
                opReturnLen += calcOpReturnLen(len);
            }
        }

        return baseLength + inputLength + outputLength + opReturnLen;
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

    public static long calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {

        /*Multiple signature single input length:
             Base Bytes 40 (preTxId 32, preIndex 4, sequence 4),
             Variable script length:?
             script:
                 op_0 1
                 Signature: m * (1+64+1) // length + pubKeyLength + sigHash ALL
                 Variable redeemScript length:?
                 redeem script：
                     op_m 1
                     pubKeys n * 33
                     op_n 1
                     OP_CHECKMULTISIG 1
          */

        long redeemScriptLength = 1 + (n * 33L) + 1 + 1;
        long redeemScriptVarInt = VarInt.sizeOf(redeemScriptLength);
        long scriptLength = 1 + (m * 66L) + redeemScriptVarInt + redeemScriptLength;
        long scriptVarInt = VarInt.sizeOf(scriptLength);
        long inputLength = 40 + scriptVarInt + scriptLength;

        long length;
        if (opReturnBytesLen == null || opReturnBytesLen.length == 0) {
            length = 10 + inputLength * inputNum + (long) 34 * (outputNum + 1);
        } else {
            int totalOpReturnLen = 0;
            for (int len : opReturnBytesLen) {
                totalOpReturnLen += calcOpReturnLen(len);
            }
            length = 10 + inputLength * inputNum + (long) 34 * (outputNum + 1) + totalOpReturnLen;
        }
        return length;
    }

    public static void testMultiSigTx(List<org.bitcoinj.crypto.ECKey> privKeys,
                                      List<org.bitcoinj.crypto.ECKey> pubKeys,
                                      long amount,
                                      String receiveAddr,
                                      List<UTXO> utxos,
                                      List<byte[]> opReturns,
                                      int m, int n,
                                      boolean mainnet) {
        long fee;
        if (opReturns != null) {
            int[] opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
            fee = calcFeeMultiSignSize(utxos.size(), 1, opReturnSize, m, n);
        } else {
            fee = calcFeeMultiSignSize(utxos.size(), 1, null, m, n);
        }
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Script redeemScript = ScriptBuilder.createRedeemScript(n, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();
        System.out.println("multiSigAddress = " + multiSigAddress);

        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Address address = Wallet.createBasic(networkParameters).parseAddress(receiveAddr);
        Script outputScript = ScriptBuilder.createOutputScript(address);

        //Build spend tx
        //First arrange the obtained utxo array from large to small
        Comparator<UTXO> comparator = (o1, o2) -> {
            if (o1.getValue().value < o2.getValue().value)
                return 1;
            else
                return -1;
        };
        Collections.sort(utxos, comparator);
        List<UTXO> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        for (int k = 0; k < utxos.size(); k++) {
            if (totalMoney >= (amount + txFee.value))
                break;
            usingUtxos.add(utxos.get(k));
            totalMoney += utxos.get(k).getValue().value;
        }

        Transaction spendTx = new Transaction();
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = Wallet.createBasic(networkParameters).parseAddress(multiSigAddress);
        long leave = totalMoney - amount - txFee.value;
        if (leave > 0) {
            //Change output
            spendTx.addOutput(Coin.valueOf(leave), fromAddress);
        }

        //Add OP_RETURN
        if (opReturns != null) {
            for (byte[] opReturn : opReturns) {
                try {
                    Script opreturnScript = ScriptBuilder.createOpReturnScript(opReturn);
                    spendTx.addOutput(Coin.ZERO, opreturnScript);
                } catch (Exception var20) {
                    throw new RuntimeException(var20);
                }
            }
        }

        for (int i = 0; i < usingUtxos.size(); i++) {
            UTXO usingUtxo = usingUtxos.get(i);
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getIndex(), usingUtxo.getHash());
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint);
            spendTx.addInput(unsignedInput);
        }

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            ArrayList<TransactionSignature> txSigs = new ArrayList<>();
            Sha256Hash sighash = spendTx.hashForSignature(input.getIndex(), redeemScript, Transaction.SigHash.ALL, false);
            IntStream.range(0, 2).forEach(i -> txSigs.add(new TransactionSignature(privKeys.get(i).sign(sighash), Transaction.SigHash.ALL, false)));
            //Build tx input script
            Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(txSigs, redeemScript);
            input.setScriptSig(inputScript);
        });

        Transaction.verify(networkParameters, spendTx);

        Context.propagate(new Context());
        spendTx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        spendTx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        //System.out.println("spendTx: " + spendTx);
        System.out.println("Tx hex: " + ByteUtils.formatHex(spendTx.serialize()));
    }
}
