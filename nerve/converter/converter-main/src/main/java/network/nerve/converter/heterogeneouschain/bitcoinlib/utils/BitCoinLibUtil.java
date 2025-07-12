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
package network.nerve.converter.heterogeneouschain.bitcoinlib.utils;

import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.P2WSHMSInfo;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import network.nerve.converter.utils.ConverterUtil;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.bitcoinj.base.Base58;
import org.bitcoinj.base.*;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.SignatureDecodeException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.*;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.utils.Numeric;

import javax.net.ssl.*;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;
import static org.bitcoinj.base.BitcoinNetwork.MAINNET;
import static org.bitcoinj.base.BitcoinNetwork.TESTNET;
import static org.bitcoinj.core.TransactionInput.NO_SEQUENCE;
import static org.bitcoinj.script.ScriptOpCodes.OP_1;
import static org.bitcoinj.script.ScriptOpCodes.OP_CHECKMULTISIG;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BitCoinLibUtil {

    public static final long TBC_FEE = 10000;
    public static final BigInteger _0n = BigInteger.ZERO;
    public static final BigInteger _1n = BigInteger.ONE;
    public static final BigInteger _2n = BigInteger.valueOf(2);
    public static final BigInteger _3n = BigInteger.valueOf(3);
    public static final BigInteger _8n = BigInteger.valueOf(8);
    public static final BigInteger _Pn = new BigInteger("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16);


    public static HtgAccount createAccount(String prikey, boolean mainnet) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(prikey), true);
        String address = getBtcLegacyAddress(ecKey.getPubKey(), mainnet);
        byte[] pubKey = ecKey.getPubKey();
        HtgAccount account = new HtgAccount();
        account.setAddress(address);
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ecKey.getPrivKeyBytes());
        account.setEncryptedPriKey(new byte[0]);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static boolean validateAddress(String addr, boolean mainnet) {
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Wallet basic = Wallet.createBasic(network);
        try {
            Address address = basic.parseAddress(addr);
            if (address != null) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static String getBtcLegacyAddress(byte[] pubKeyBytes, boolean mainnet) {
        int version = mainnet ? 0 : 111;
        byte[] bytes = SerializeUtils.sha256hash160(pubKeyBytes);
        return Base58.encodeChecked(version, bytes);
    }

    public static HtgAccount createAccountByPubkey(String pubkeyStr, boolean mainnet) {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode(pubkeyStr));
        byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
        HtgAccount account = new HtgAccount();
        account.setAddress(getBtcLegacyAddress(pubKey, mainnet));
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ZERO_BYTES);
        account.setEncryptedPriKey(ZERO_BYTES);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

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

    public static String makeMultiAddr(List<ECKey> pubKeys, int m, boolean mainnet) {
        NetworkParameters network = mainnet ? MainNetParams.get() : TestNet3Params.get();
        pubKeys = new ArrayList<>(pubKeys);
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(m, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();
        return multiSigAddress;
    }

    public static String multiAddr(List<byte[]> pubKeyList, int m, boolean mainnet) {
        List<ECKey> pubKeys = pubKeyList.stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        int p2SHHeader = networkParameters.getP2SHHeader();
        Script script = genMultiP2SH(pubKeys, m);
        byte[] redeemScriptBytes = script.program();
        String multiAddr = scriptToMultiAddr(HexUtil.encode(redeemScriptBytes), p2SHHeader);
        return multiAddr;
    }

    private static Script genMultiP2SH(List<ECKey> keys, int n) {
        Script multiSigScript = ScriptBuilder.createMultiSigOutputScript(n, keys);
        return multiSigScript;
    }

    private static String scriptToMultiAddr(String script, int version) {
        byte[] scriptBytes = HexUtil.decode(script);
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

    public static byte[] taggedHash(String tag, byte[] msg) {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        byte[] doubleTagHash = Arrays.copyOf(tagHash, tagHash.length * 2);
        System.arraycopy(tagHash, 0, doubleTagHash, tagHash.length, tagHash.length);
        byte[] input = new byte[tagHash.length * 2 + msg.length];
        System.arraycopy(doubleTagHash, 0, input, 0, doubleTagHash.length);
        System.arraycopy(msg, 0, input, doubleTagHash.length, msg.length);
        return Sha256Hash.hash(input);
    }

    public static String genBtcTaprootAddressByPub(String pub, boolean mainnet) {
        byte[] pubBytes = Numeric.hexStringToByteArray(pub);
        if (!ECKey.isPubKeyCompressed(pubBytes)) {
            throw new RuntimeException("Error Compressed PubKey");
        }
        if (pubBytes[0] == 0x03) {
            pubBytes[0] = 0x02;
        }
        ECKey ecKey = ECKey.fromPublicOnly(pubBytes);
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] y = pubKeyPoint.getYCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);
        ECKey tKey = ECKey.fromPrivate(t);
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
            ECKey ecKey = ECKey.fromPrivate(privkeybtc);
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
        String address = Base58.encodeChecked(mainnet ? 0x05 : 0xc4, redeem_rip);
        return address;
    }

    /**
     * Generate native Segwit address using public key
     */
    public static String getNativeSegwitAddressByPubkey(String pubKey, boolean mainnet) {
        String address = "";
        try {
            NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
            ECKey ecKey = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(pubKey));
            SegwitAddress segwitAddress = SegwitAddress.fromKey(networkParameters, ecKey);
            address = segwitAddress.toBech32();
        } catch (Exception e) {}
        return address;
    }

    public static String getNativeSegwitMultiSignAddress(int threshold, List<ECKey> pubECKeys, boolean mainnet) {
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        List<ECKey> sortedPubKeys = new ArrayList<>(pubECKeys);
        Collections.sort(sortedPubKeys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(threshold, sortedPubKeys);
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address multiSigAddress = scriptPubKey.getToAddress(network);
        return multiSigAddress.toString();
    }

    public static String getNativeSegwitAddressByRedeemScript(String redeemScriptProgram, boolean mainnet) {
        String address = "";
        try {
            NetworkParameters network = mainnet ? MainNetParams.get() : TestNet3Params.get();
            Script redeemScript = Script.parse(HexUtil.decode(redeemScriptProgram));
            Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
            Address addressObj = scriptPubKey.getToAddress(network);
            address = addressObj.toString();
        } catch (Exception e) {}
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

    public static int getByzantineCount(int count) {
        int directorCount = count;
        int ByzantineRateCount = directorCount * ConverterContext.BYZANTINERATIO;
        int minPassCount = ByzantineRateCount / ConverterConstant.MAGIC_NUM_100;
        if (ByzantineRateCount % ConverterConstant.MAGIC_NUM_100 > 0) {
            minPassCount++;
        }
        return minPassCount;
    }

    public static Transaction sendTransactionOffline(
            String priKey,
            String receiveAddr,
            String fromAddr,
            long amount,
            List<UTXO> utxos,
            List<byte[]> opReturns,
            long feeRate,
            boolean mainnet) {
        return sendTransactionOffline(priKey, receiveAddr, fromAddr, amount, utxos, opReturns, feeRate, mainnet, false);
    }

    public static Transaction sendTransactionOffline(
            String priKey,
            String receiveAddr,
            String fromAddr,
            long amount,
            List<UTXO> utxos,
            List<byte[]> opReturns,
            long feeRate,
            boolean mainnet,
            boolean useAllUTXO) {
        long fee = 0;
        int[] opReturnSize = null;
        if (opReturns != null) {
            opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
        }
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Wallet basic = Wallet.createBasic(network);
        if (!utxos.isEmpty() && null != utxos) {
            BigInteger privkey = new BigInteger(1, HexUtil.decode(priKey));
            ECKey ecKey = ECKey.fromPrivate(privkey);
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
            boolean enoughUTXO = false;
            for (int k = 0; k < utxos.size(); k++) {
                UTXO utxo = utxos.get(k);
                usingUtxos.add(utxo);
                totalMoney += utxo.getValue().value;
                long feeSize = calcFeeSize(usingUtxos.size(), 1, opReturnSize);
                //System.out.println(String.format("uSize: %s, feeSize: %s", usingUtxos.size(), feeSize));
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
            // Output of transfer amount
            tx.addOutput(Coin.valueOf(amount), receiveAddress);
            // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
            Address fromAddress = basic.parseAddress(fromAddr);
            long leave = totalMoney - amount - fee;
            if (leave > ConverterConstant.BTC_DUST_AMOUNT) {
                //Change output
                tx.addOutput(Coin.valueOf(leave), fromAddress);
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
                UTXO unspent = usingUtxos.get(i);
                TransactionOutPoint outPoint = new TransactionOutPoint(unspent.getIndex(), unspent.getHash());
                TransactionInput transactionInput = new TransactionInput(tx, ScriptBuilder.createEmpty().program(), outPoint, unspent.getValue());
                transactionInput.setSequenceNumber(NO_SEQUENCE - 2);
                tx.addInput(transactionInput);
            }
            for (int i = 0; i < tx.getInputs().size(); i++) {
                Script scriptPubKey = ScriptBuilder.createOutputScript(fromAddress);
                Sha256Hash hash = tx.hashForSignature(i, scriptPubKey, Transaction.SigHash.ALL, true);
                ECKey.ECDSASignature ecdsaSignature = ecKey.sign(hash);
                TransactionSignature txSignature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, true);

                if (ScriptPattern.isP2PK(scriptPubKey)) {
                    tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature));
                } else {
                    if (!ScriptPattern.isP2PKH(scriptPubKey)) {
                        throw new ScriptException(ScriptError.SCRIPT_ERR_UNKNOWN_ERROR, "Unable to sign this scrptPubKey: " + scriptPubKey);
                    }
                    tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature, ecKey));
                }
            }

            Transaction.verify(network, tx);

            Context.propagate(new Context());
            tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
            tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
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

    public static List<String> createMultiSigTxByOne(ECKey privKey,
                                                     List<ECKey> pubKeys,
                                                     long amount,
                                                     String receiveAddr,
                                                     List<UTXOData> utxos,
                                                     List<byte[]> opReturns,
                                                     int m, int n,
                                                     long feeRate,
                                                     boolean mainnet) {
        return createMultiSigTxByOne(privKey, pubKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false);
    }

    public static List<String> createMultiSigTxByOne(ECKey privKey,
                                                     List<ECKey> pubKeys,
                                                     long amount,
                                                     String receiveAddr,
                                                     List<UTXOData> utxos,
                                                     List<byte[]> opReturns,
                                                     int m, int n,
                                                     long feeRate,
                                                     boolean mainnet,
                                                     boolean useAllUTXO) {
        long fee = 0;
        int[] opReturnSize = null;
        if (opReturns != null) {
            opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
        }

        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Script redeemScript = ScriptBuilder.createRedeemScript(m, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();

        Address address = Wallet.createBasic(networkParameters).parseAddress(receiveAddr);
        Script outputScript = ScriptBuilder.createOutputScript(address);

        //Build spend tx
        //First arrange the obtained utxo array from small to large
        Comparator<UTXOData> comparator = ConverterUtil.BITCOIN_SYS_COMPARATOR;

        List<UTXOData> _utxos = new ArrayList<>(utxos);
        Collections.sort(_utxos, comparator);

        List<UTXOData> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        boolean enoughUTXO = false;
        for (int k = 0; k < _utxos.size(); k++) {
            usingUtxos.add(_utxos.get(k));
            totalMoney += _utxos.get(k).getAmount().longValue();
            long feeSize = calcFeeMultiSignSize(usingUtxos.size(), 1, opReturnSize, m, n);
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
        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Transaction spendTx = new Transaction();
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = Wallet.createBasic(networkParameters).parseAddress(multiSigAddress);
        long leave = totalMoney - amount - txFee.value;
        if (leave > ConverterConstant.BTC_DUST_AMOUNT) {
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
            UTXOData usingUtxo = usingUtxos.get(i);
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getVout(), Sha256Hash.wrap(usingUtxo.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint);
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }

        List<String> result = new ArrayList<>();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            Sha256Hash sighash = spendTx.hashForSignature(input.getIndex(), redeemScript, Transaction.SigHash.ALL, false);
            ECKey.ECDSASignature ecdsaSignature = privKey.sign(sighash);
            result.add(HexUtil.encode(ecdsaSignature.encodeToDER()));
        });

        return result;
    }

    public static boolean verifyMultiSigTxByOne(ECKey pub, List<String> signatures,
                                                     List<ECKey> pubKeys,
                                                     long amount,
                                                     String receiveAddr,
                                                     List<UTXOData> utxos,
                                                     List<byte[]> opReturns,
                                                     int m, int n,
                                                     long feeRate,
                                                     boolean mainnet,
                                                     boolean useAllUTXO) {
        long fee = 0;
        int[] opReturnSize = null;
        if (opReturns != null) {
            opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
        }

        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Script redeemScript = ScriptBuilder.createRedeemScript(m, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();

        Address address = Wallet.createBasic(networkParameters).parseAddress(receiveAddr);
        Script outputScript = ScriptBuilder.createOutputScript(address);

        //Build spend tx
        //First arrange the obtained utxo array from small to large
        Comparator<UTXOData> comparator = ConverterUtil.BITCOIN_SYS_COMPARATOR;

        List<UTXOData> _utxos = new ArrayList<>(utxos);
        Collections.sort(_utxos, comparator);

        List<UTXOData> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        boolean enoughUTXO = false;
        for (int k = 0; k < _utxos.size(); k++) {
            usingUtxos.add(_utxos.get(k));
            totalMoney += _utxos.get(k).getAmount().longValue();
            long feeSize = calcFeeMultiSignSize(usingUtxos.size(), 1, opReturnSize, m, n);
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
        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Transaction spendTx = new Transaction();
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = Wallet.createBasic(networkParameters).parseAddress(multiSigAddress);
        long leave = totalMoney - amount - txFee.value;
        if (leave > ConverterConstant.BTC_DUST_AMOUNT) {
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
            UTXOData usingUtxo = usingUtxos.get(i);
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getVout(), Sha256Hash.wrap(usingUtxo.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint);
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }

        System.out.println(String.format("verify start, pub: %s", pub.getPublicKeyAsHex()));
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = spendTx.hashForSignature(input.getIndex(), redeemScript, Transaction.SigHash.ALL, false);

            String signStr = signatures.get(k);
            ECKey.ECDSASignature ecdsaSignature;
            try {
                ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signStr));
            } catch (SignatureDecodeException e) {
                throw new RuntimeException(e);
            }
            if (!ECKey.verify(sighash.getBytes(), ecdsaSignature, pub.getPubKey())) {
                System.err.println(String.format("input %s verify error", k));
                return false;
            }
            System.out.println(String.format("input %s verify pass", k));
        }
        return true;
    }

    public static Transaction createMultiSigTxByMulti(Map<String, List<String>> signatures,
                                                      List<ECKey> pubKeys,
                                                      long amount,
                                                      String receiveAddr,
                                                      List<UTXOData> utxos,
                                                      List<byte[]> opReturns,
                                                      int m, int n,
                                                      long feeRate,
                                                      boolean mainnet) throws SignatureDecodeException {
        return createMultiSigTxByMulti(signatures, pubKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false);
    }

    public static Transaction createMultiSigTxByMulti(Map<String, List<String>> signatures,
                                                      List<ECKey> pubKeys,
                                                      long amount,
                                                      String receiveAddr,
                                                      List<UTXOData> utxos,
                                                      List<byte[]> opReturns,
                                                      int m, int n,
                                                      long feeRate,
                                                      boolean mainnet,
                                                      boolean useAllUTXO) throws SignatureDecodeException {
        long fee = 0;
        int[] opReturnSize = null;
        if (opReturns != null) {
            opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
        }

        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        Script redeemScript = ScriptBuilder.createRedeemScript(m, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(redeemScript);
        String multiSigAddress = LegacyAddress.fromScriptHash(network, ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();

        Address address = Wallet.createBasic(networkParameters).parseAddress(receiveAddr);
        Script outputScript = ScriptBuilder.createOutputScript(address);

        //Build spend tx
        //First arrange the obtained utxo array from small to large
        Comparator<UTXOData> comparator = ConverterUtil.BITCOIN_SYS_COMPARATOR;

        List<UTXOData> _utxos = new ArrayList<>(utxos);
        Collections.sort(_utxos, comparator);

        List<UTXOData> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        boolean enoughUTXO = false;
        for (int k = 0; k < _utxos.size(); k++) {
            usingUtxos.add(_utxos.get(k));
            totalMoney += _utxos.get(k).getAmount().longValue();
            long feeSize = calcFeeMultiSignSize(usingUtxos.size(), 1, opReturnSize, m, n);
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
        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Transaction spendTx = new Transaction();
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = Wallet.createBasic(networkParameters).parseAddress(multiSigAddress);
        long leave = totalMoney - amount - txFee.value;
        if (leave > ConverterConstant.BTC_DUST_AMOUNT) {
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
            UTXOData usingUtxo = usingUtxos.get(i);
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getVout(), Sha256Hash.wrap(usingUtxo.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint);
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }

        List<ECKey> _pubKeys = new ArrayList<>(pubKeys);
        Collections.sort(_pubKeys, ECKey.PUBKEY_COMPARATOR);

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            ArrayList<TransactionSignature> txSigs = new ArrayList<>();
            /*
            for (int i = 0; i < n; i++) {
                ECKey ecKey = _pubKeys.get(i);
                ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signatures.get(ecKey.getPublicKeyAsHex()).get(k)));
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);
                txSigs.add(signature);
            }
*/
            int valid = 0;
            for (int i = 0; i < n; i++) {
                if (valid == m) {
                    break;
                }
                ECKey ecKey = _pubKeys.get(i);
                List<String> signList = signatures.get(ecKey.getPublicKeyAsHex());
                if (signList == null || signList.isEmpty() || signList.size() < spendTxInputs.size()) {
                    continue;
                }
                ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signList.get(k)));
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);
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

        Transaction.verify(networkParameters, spendTx);

        Context.propagate(new Context());
        spendTx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        spendTx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        //System.out.println("Tx hex: " + ByteUtils.formatHex(spendTx.serialize()));
        return spendTx;
    }

    public static long calcFeeMultiSignSize(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {

        long redeemScriptLength = 1 + (n * (33L + 1)) + 1 + 1;
        long redeemScriptVarInt = VarInt.sizeOf(redeemScriptLength);
        long scriptLength = 2 + (m * (1 + 1 + 69L + 1 + 1)) + redeemScriptVarInt + redeemScriptLength;
        long scriptVarInt = VarInt.sizeOf(scriptLength);
        long inputLength = 40 + scriptVarInt + scriptLength;

        int totalOpReturnLen = 0;
        if (opReturnBytesLen != null && opReturnBytesLen.length > 0) {
            for (int len : opReturnBytesLen) {
                totalOpReturnLen += calcOpReturnLen(len);
            }
        }
        long length = 10 + inputLength * inputNum + (long) 43 * (outputNum + 1) + totalOpReturnLen;
        return length;

    }

    public static int calcOpReturnLen(int opReturnBytesLen) {
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

    public static long calcFeeMultiSignSizeP2WSH(int inputNum, int outputNum, int[] opReturnBytesLen, int m, int n) {

        long redeemScriptLength = 1 + (n * 33L) + 1 + 1;
        long scriptLength = 1 + (m * (1 + 1 + 69L + 1)) + VarInt.sizeOf(redeemScriptLength) + redeemScriptLength;
        long inputLength = 40 + (scriptLength / 4 + 1);

        long length;
        if (opReturnBytesLen == null || opReturnBytesLen.length == 0) {
            length = 12 + inputLength * inputNum + (long) 43 * (outputNum + 1);
        } else {
            int totalOpReturnLen = 0;
            for (int len : opReturnBytesLen) {
                totalOpReturnLen += calcOpReturnLen(len);
            }
            length = 12 + inputLength * inputNum + (long) 43 * (outputNum + 1) + totalOpReturnLen;
        }
        if (ConverterContext.converterCoreApi.isProtocol42()) {
            return length + length / 20;
        } else {
            return length;
        }
    }

    /**
     * calc split number of Transaction Change Spliting by splitGranularity
     *      change = fromTotal - transfer - fee
     *      change = splitNum * splitGranularity
     *      fee = txSize * feeRate
     *      txSize = f(splitNum)
     *      f(splitNum) = f(1) + 43 * (splitNum - 1) ==> Derived from calcFeeMultiSignSizeP2WSH(inputNum, splitNum, opReturnBytesLen, m, n)
     *  In summary:
     *      splitNum = (fromTotal - transfer - calcFeeMultiSignSizeP2WSH(inputNum, 1, opReturnBytesLen, m, n) * feeRate + 43 * feeRate) / (43 * feeRate + splitGranularity)
     *
     * @param fromTotal
     * @param transfer
     * @param feeRate
     * @param splitGranularity
     * @param inputNum
     * @param opReturnBytesLen
     * @param m
     * @param n
     * @return
     */
    public static int calcSplitNumP2WSH(long fromTotal, long transfer, long feeRate, long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        // numerator and denominator
        long numerator = fromTotal - transfer - calcFeeMultiSignSizeP2WSH(inputNum, 1, opReturnBytesLen, m, n) * feeRate + 43 * feeRate;
        long denominator = 43 * feeRate + splitGranularity;
        int splitNum = (int) (numerator / denominator);
        if (splitNum == 0 && numerator % denominator > 0) {
            splitNum = 1;
        }
        return splitNum;
    }

    public static List<String> createNativeSegwitMultiSignByOne(ECKey privKey,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, Long splitGranularity) {
        return createNativeSegwitMultiSignByOne(privKey, pubEcKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false, splitGranularity);
    }

    public static List<String> createNativeSegwitMultiSignByOne(ECKey privKey,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, boolean useAllUTXO, Long splitGranularity) {
        P2WSHMSInfo info = createNativeSegwitMultiSignBase(pubEcKeys,
                amount,
                receiveAddr,
                utxos,
                opReturns,
                m, n,
                feeRate,
                mainnet, useAllUTXO, splitGranularity);
        Transaction spendTx = info.getTx();
        Script redeemScript = info.getRedeemScript();
        //Sign tx, will fall if no private keys specified
        List<String> result = new ArrayList<>();
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            Sha256Hash sighash = spendTx.hashForWitnessSignature(input.getIndex(), redeemScript.program(), input.getValue(), Transaction.SigHash.ALL, false);
            ECKey.ECDSASignature ecdsaSignature = privKey.sign(sighash);
            result.add(HexUtil.encode(ecdsaSignature.encodeToDER()));
        });

        return result;
    }

    public static boolean verifyNativeSegwitMultiSign(ECKey pub, List<String> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, Long splitGranularity) throws NulsException {
        return verifyNativeSegwitMultiSign(pub, signatures, pubEcKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false, splitGranularity);
    }

    public static boolean verifyNativeSegwitMultiSign(ECKey pub, List<String> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        P2WSHMSInfo info = createNativeSegwitMultiSignBase(pubEcKeys,
                amount,
                receiveAddr,
                utxos,
                opReturns,
                m, n,
                feeRate,
                mainnet, useAllUTXO, splitGranularity);
        Transaction spendTx = info.getTx();
        Script redeemScript = info.getRedeemScript();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = spendTx.hashForWitnessSignature(input.getIndex(), redeemScript.program(), input.getValue(), Transaction.SigHash.ALL, false);
            String signStr = signatures.get(k);
            ECKey.ECDSASignature ecdsaSignature;
            try {
                ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signStr));
            } catch (SignatureDecodeException e) {
                throw new NulsException(ConverterErrorCode.FAILED, e);
            }
            if (!ECKey.verify(sighash.getBytes(), ecdsaSignature, pub.getPubKey())) {
                return false;
            }
        }
        return true;
    }

    public static int verifyNativeSegwitMultiSignCount(Map<String, List<String>> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, Long splitGranularity) throws NulsException {
        return verifyNativeSegwitMultiSignCount(signatures, pubEcKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false, splitGranularity);
    }

    public static int verifyNativeSegwitMultiSignCount(Map<String, List<String>> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, boolean useAllUTXO, Long splitGranularity) throws NulsException {
        P2WSHMSInfo info = createNativeSegwitMultiSignBase(pubEcKeys,
                amount,
                receiveAddr,
                utxos,
                opReturns,
                m, n,
                feeRate,
                mainnet, useAllUTXO, splitGranularity);
        Set<Map.Entry<String, List<String>>> entries = signatures.entrySet();
        Transaction spendTx = info.getTx();
        Script redeemScript = info.getRedeemScript();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        List<Sha256Hash> inputHashList = new ArrayList<>();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            Sha256Hash sighash = spendTx.hashForWitnessSignature(input.getIndex(), redeemScript.program(), input.getValue(), Transaction.SigHash.ALL, false);
            inputHashList.add(sighash);

        }
        int result = 0;
        for (Map.Entry<String, List<String>> entry : entries) {
            String pubHex = entry.getKey();
            ECKey pub = ECKey.fromPublicOnly(HexUtil.decode(pubHex));
            List<String> signatureList = entry.getValue();
            boolean check = true;
            for (int r = 0; r < inputHashList.size(); r++) {
                Sha256Hash sighash = inputHashList.get(r);
                String signStr = signatureList.get(r);
                ECKey.ECDSASignature ecdsaSignature;
                try {
                    ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signStr));
                } catch (SignatureDecodeException e) {
                    throw new NulsException(ConverterErrorCode.FAILED, e);
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

    /**
     * @param signatures key: pub of user, value: signature of per input
     */
    public static Transaction createNativeSegwitMultiSignTx(Map<String, List<String>> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, Long splitGranularity) throws Exception {
        return createNativeSegwitMultiSignTx(signatures, pubEcKeys, amount, receiveAddr, utxos, opReturns, m, n, feeRate, mainnet, false, splitGranularity);
    }

    public static Transaction createNativeSegwitMultiSignTx(Map<String, List<String>> signatures,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXOData> utxos,
                                                          List<byte[]> opReturns,
                                                          int m, int n,
                                                          long feeRate,
                                                          boolean mainnet, boolean useAllUTXO, Long splitGranularity) throws Exception {
        P2WSHMSInfo info = createNativeSegwitMultiSignBase(pubEcKeys,
                amount,
                receiveAddr,
                utxos,
                opReturns,
                m, n,
                feeRate,
                mainnet, useAllUTXO, splitGranularity);
        Transaction spendTx = info.getTx();
        List<ECKey> sortedPubKeys = info.getSortedPubKeys();
        Script redeemScript = info.getRedeemScript();
        NetworkParameters network = info.getNetwork();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        for (int k = 0; k < spendTxInputs.size(); k++) {
            TransactionInput input = spendTxInputs.get(k);
            TransactionSignature[] txSigs = new TransactionSignature[m];
            int valid = 0;
            for (int i = 0; i < n; i++) {
                if (valid == m) {
                    break;
                }
                ECKey ecKey = sortedPubKeys.get(i);
                List<String> signList = signatures.get(ecKey.getPublicKeyAsHex());
                if (signList == null || signList.isEmpty() || signList.size() < spendTxInputs.size()) {
                    continue;
                }
                ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signList.get(k)));
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);
                txSigs[valid++] = signature;
            }
            if (valid < m) {
                throw new RuntimeException(String.format("WITHDRAWAL_NOT_ENOUGH_SIGNATURE, params: inputIndex-%s, valid-%s, minNeed-%s, opReturn-%s", k, valid, m, !HtgUtil.isEmptyList(opReturns) ? HexUtil.encode(opReturns.get(0)) : "emptyOpReturn"));
            }
            //Build tx input script
            input.setWitness(TransactionWitness.redeemP2WSH(redeemScript, txSigs));
        }
        Transaction.verify(network, spendTx);
        Context.propagate(new Context());
        spendTx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        spendTx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        //System.out.println("Tx hex: " + ByteUtils.formatHex(spendTx.serialize()));
        return spendTx;
    }

    public static long calcFeeMultiSignSizeP2WSHWithSplitGranularity(long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int[] opReturnBytesLen, int m, int n) {
        long feeSize;
        if (splitGranularity != null && splitGranularity > 0) {
            if (splitGranularity < ConverterConstant.MIN_SPLIT_GRANULARITY) {
                throw new RuntimeException("error splitGranularity: " + splitGranularity);
            }
            int splitNum = calcSplitNumP2WSH(fromTotal, transfer, feeRate, splitGranularity, inputNum, opReturnBytesLen, m, n);
            feeSize = calcFeeMultiSignSizeP2WSH(inputNum, splitNum, opReturnBytesLen, m, n);
        } else {
            feeSize = calcFeeMultiSignSizeP2WSH(inputNum, 1, opReturnBytesLen, m, n);
        }
        return feeSize;
    }

    private static P2WSHMSInfo createNativeSegwitMultiSignBase(List<ECKey> pubEcKeys,
                                                               long amount,
                                                               String receiveAddr,
                                                               List<UTXOData> utxos,
                                                               List<byte[]> opReturns,
                                                               int m, int n,
                                                               long feeRate,
                                                               boolean mainnet,
                                                               boolean useAllUTXO,
                                                               Long splitGranularity) {
        long fee = 0;
        int[] opReturnSize = null;
        if (opReturns != null) {
            opReturnSize = new int[opReturns.size()];
            int i = 0;
            for (byte[] opReturn : opReturns) {
                opReturnSize[i++] = opReturn.length;
            }
        }
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        BitcoinNetwork network = mainnet ? MAINNET : TESTNET;
        List<ECKey> sortedPubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(sortedPubKeys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(m, sortedPubKeys);
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address multiSigAddress = scriptPubKey.getToAddress(network);

        Address address = Wallet.createBasic(networkParameters).parseAddress(receiveAddr);
        Script outputScript = ScriptBuilder.createOutputScript(address);

        //Build spend tx
        //First arrange the obtained utxo array from small to large
        Comparator<UTXOData> comparator = ConverterUtil.BITCOIN_SYS_COMPARATOR;

        Collections.sort(utxos, comparator);
        List<UTXOData> usingUtxos = new ArrayList<>();
        long totalMoney = 0;
        boolean enoughUTXO = false;
        for (UTXOData utxo : utxos) {
            usingUtxos.add(utxo);
            totalMoney += utxo.getAmount().longValue();
            long feeSize = calcFeeMultiSignSizeP2WSHWithSplitGranularity(
                    totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
            /*if (splitGranularity != null) {
                if (splitGranularity < ConverterConstant.MIN_SPLIT_GRANULARITY) {
                    throw new RuntimeException("error splitGranularity: " + splitGranularity);
                }
                int splitNum = calcSplitNumP2WSH(totalMoney, amount, feeRate, splitGranularity, usingUtxos.size(), opReturnSize, m, n);
                feeSize = calcFeeMultiSignSizeP2WSH(usingUtxos.size(), splitNum, opReturnSize, m, n);
            } else {
                feeSize = calcFeeMultiSignSizeP2WSH(usingUtxos.size(), 1, opReturnSize, m, n);
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
        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Transaction spendTx = new Transaction();
        spendTx.setVersion(2);
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = multiSigAddress;
        long leave = totalMoney - amount - txFee.value;
        // Bitcoin, for example, has a dust limit imposed by Bitcoin Core,
        // the Bitcoin blockchain software, of around 546 satoshis (0.00000546 BTC),
        // the smaller denomination of Bitcoin (BTC).
        // The wallets' nodes that apply such a limit may reject transactions equal to or smaller than 546 satoshis
        if (leave > ConverterConstant.BTC_DUST_AMOUNT) {
            //Change output
            List<Long> changes = getChanges(splitGranularity, leave);
            for (Long c : changes) {
                spendTx.addOutput(Coin.valueOf(c), fromAddress);
            }
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

        for (UTXOData usingUtxo : usingUtxos) {
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getVout(), Sha256Hash.wrap(usingUtxo.getTxid()));
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint, Coin.valueOf(usingUtxo.getAmount().longValue()));
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }

        return new P2WSHMSInfo(spendTx, sortedPubKeys, redeemScript, networkParameters);
    }

    private static List<Long> getChanges(Long splitGranularity, long change) {
        List<Long> changes = new ArrayList<>();
        if (splitGranularity != null && splitGranularity > ConverterConstant.BTC_DUST_AMOUNT) {
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

    public static String takeMultiSignAddressWithP2WSH(RawInput input, boolean mainnet) {
        List<String> txInWitness = input.getTxInWitness();
        if (txInWitness == null || txInWitness.isEmpty()) {
            return EMPTY_STRING;
        }
        String st = txInWitness.get(txInWitness.size() - 1);
        byte[] stBytes = HexUtil.decode(st);
        if (stBytes.length < 105
                || stBytes[0] < OP_1 + 1
                || stBytes[stBytes.length - 2] < OP_1 + 2
                || (0xff & stBytes[stBytes.length - 1]) != OP_CHECKMULTISIG
        ) {
            return EMPTY_STRING;
        }
        return BitCoinLibUtil.getNativeSegwitAddressByRedeemScript(st, mainnet);
    }

    public static String removeTxHashPrefix(String txHash) {
        int prefixLength = ConverterConstant.BTC_WITHDRAW_FEE_TX_HASH_PREFIX.length();
        int length = NulsHash.HASH_LENGTH * 2 + prefixLength;
        if (txHash.length() == length && txHash.startsWith(ConverterConstant.BTC_WITHDRAW_FEE_TX_HASH_PREFIX)) {
            return txHash.substring(prefixLength);
        }
        return txHash;
    }

    public static String addTxHashPrefix(String txHash) {
        int length = NulsHash.HASH_LENGTH * 2;
        if (txHash.length() == length) {
            return txHash + ConverterConstant.BTC_WITHDRAW_FEE_TX_HASH_PREFIX;
        }
        return txHash;
    }

    public static long calcTxFee(RawTransaction txInfo, BitCoinLibWalletApi htgWalletApi) {
        List<RawInput> inputList = txInfo.getVIn();
        long fromTotal = 0;
        for (RawInput input : inputList) {
            String txId = input.getTxId();
            Integer vOut = input.getVOut();
            RawTransaction prevTx = htgWalletApi.getTransactionByHash(txId);
            fromTotal += prevTx.getVOut().get(vOut).getValue().movePointRight(8).longValue();
        }
        List<RawOutput> outputList = txInfo.getVOut();
        long toTotal = 0;
        for (RawOutput output : outputList) {
            toTotal += output.getValue().movePointRight(8).longValue();
        }
        long fee = fromTotal - toTotal;
        return fee;
    }

    public static Object[] makeChangeTxBaseInfo (HtgContext htgContext, WithdrawalUTXO withdrawlUTXO, List<String> currentMultiSignAddressPubs) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        String nerveTxHash = withdrawlUTXO.getNerveTxHash();
        List<UTXOData> UTXOList = withdrawlUTXO.getUtxoDataList();
        // take pubkeys of all managers
        List<ECKey> newPubEcKeys = withdrawlUTXO.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());

        List<String> multiSignAddressPubs = currentMultiSignAddressPubs;
        List<ECKey> oldPubEcKeys = multiSignAddressPubs.stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());

        String toAddress = BitCoinLibUtil.getNativeSegwitMultiSignAddress(htgContext.getByzantineCount(newPubEcKeys.size()), newPubEcKeys, coreApi.isNerveMainnet());
        // calc the min number of signatures
        int n = oldPubEcKeys.size(), m = htgContext.getByzantineCount(n);
        byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
        long fee = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[]{nerveTxHashBytes.length}, m, n) * withdrawlUTXO.getFeeRate();
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        return new Object[]{oldPubEcKeys, totalMoney - fee, toAddress, List.of(nerveTxHashBytes), m, n};
    }

    public static BtcdClientImpl newInstanceBtcdClient(Properties nodeConfig) {
        CloseableHttpClient closeableHttpClient = HttpClientUtil.createHttpClient(200, 40, 100, nodeConfig.getProperty(NodeProperties.RPC_HOST.getKey()), Integer.parseInt(nodeConfig.getProperty(NodeProperties.RPC_PORT.getKey())), disableVerifySSLFactory());
        return new BtcdClientImpl(closeableHttpClient, nodeConfig);
    }

    private static SSLConnectionSocketFactory disableVerifySSLFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {return null;}
            };
            sslContext.init(null,new TrustManager[]{tm},null);
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier(){
                @Override
                public boolean verify(String s, SSLSession sslSession) {return true;}

                @Override
                public void verify(String host, SSLSocket ssl) throws IOException {}

                @Override
                public void verify(String host, X509Certificate cert) throws SSLException {}

                @Override
                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {}
            });
            return sslSocketFactory;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
