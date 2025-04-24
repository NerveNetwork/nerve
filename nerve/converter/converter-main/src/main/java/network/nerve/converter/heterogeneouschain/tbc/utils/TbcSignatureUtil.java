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
package network.nerve.converter.heterogeneouschain.tbc.utils;

import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.SignatureDecodeException;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TbcSignatureUtil {
    public static final int SIGHASH_ALL = 0x01;
    public static final int SIGHASH_NONE = 0x02;
    public static final int SIGHASH_SINGLE = 0x03;
    public static final int SIGHASH_FORKID = 0x40;
    public static final int SIGHASH_ANYONECANPAY = 0x80;

    public static final int ALL = 0x41;
    public static final int NONE = 0x42;
    public static final int SINGLE = 0x43;
    public static final int ANYONECANPAY_ALL = 0xc1;
    public static final int ANYONECANPAY_NONE = 0xc2;
    public static final int ANYONECANPAY_SINGLE = 0xc3;
    public static final byte[] SIGHASH_SINGLE_BUG = HexUtil.decode("0000000000000000000000000000000000000000000000000000000000000001");


    public static class HashCache {
        public byte[] prevoutsHashBuf;
        public byte[] sequenceHashBuf;
        public byte[] outputsHashBuf;

        public HashCache() {
            this.prevoutsHashBuf = null;
            this.sequenceHashBuf = null;
            this.outputsHashBuf = null;
        }
    }

    public static List<String> signMultiTBC(String multiAddr, String txHex, String pri, List<BigInteger> fromAmounts) throws Exception {
        ECKey privKey = ECKey.fromPrivate(HexUtil.decode(pri));
        Transaction tx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(txHex)));
        String scriptAsm = TbcUtil.getMultisigLockScript(multiAddr);
        byte[] decodeASM = TbcUtil.decodeASM(scriptAsm);
        List<String> result = new ArrayList<>();
        List<TransactionInput> spendTxInputs = tx.getInputs();
        for (int i = 0; i < spendTxInputs.size(); i++) {
            TransactionInput input = spendTxInputs.get(i);
            Sha256Hash sighash = TbcSignatureUtil.hashForSignature(tx, 0x41, input.getIndex(), decodeASM, fromAmounts.get(i));
            ECKey.ECDSASignature ecdsaSignature = privKey.sign(sighash);
            result.add(HexUtil.encode(ecdsaSignature.encodeToDER()) + "41");
        }
        return result;
    }

    public static boolean verifySignMultiTBC(ECKey pub, List<String> signatures,
            String multiAddr, String txHex, List<BigInteger> fromAmounts) throws Exception {
        Transaction tx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(txHex)));
        String scriptAsm = TbcUtil.getMultisigLockScript(multiAddr);
        byte[] decodeASM = TbcUtil.decodeASM(scriptAsm);
        List<TransactionInput> spendTxInputs = tx.getInputs();
        for (int i = 0; i < spendTxInputs.size(); i++) {
            TransactionInput input = spendTxInputs.get(i);
            Sha256Hash sighash = TbcSignatureUtil.hashForSignature(tx, 0x41, input.getIndex(), decodeASM, fromAmounts.get(i));

            String signStr = signatures.get(i);
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

    public static List<String> signMultiFT(String multiAddr, String txHex, String pri, List<BigInteger> fromAmounts) throws Exception {
        ECKey privKey = ECKey.fromPrivate(HexUtil.decode(pri));
        Transaction tx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(txHex)));
        String scriptAsm = TbcUtil.getMultisigLockScript(multiAddr);
        byte[] decodeASM = TbcUtil.decodeASM(scriptAsm);
        List<String> result = new ArrayList<>();
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = tx.getInputs();
        TransactionInput input = spendTxInputs.get(0);
        Sha256Hash sighash = TbcSignatureUtil.hashForSignature(tx, 0x41, input.getIndex(), decodeASM, fromAmounts.get(0));
        ECKey.ECDSASignature ecdsaSignature = privKey.sign(sighash);
        result.add(HexUtil.encode(ecdsaSignature.encodeToDER()) + "41");
        return result;
    }

    public static boolean verifySignMultiFT(ECKey pub, List<String> signatures,
            String multiAddr, String txHex, List<BigInteger> fromAmounts) throws Exception {
        Transaction tx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(txHex)));
        String scriptAsm = TbcUtil.getMultisigLockScript(multiAddr);
        byte[] decodeASM = TbcUtil.decodeASM(scriptAsm);
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = tx.getInputs();
        TransactionInput input = spendTxInputs.get(0);
        Sha256Hash sighash = TbcSignatureUtil.hashForSignature(tx, 0x41, input.getIndex(), decodeASM, fromAmounts.get(0));

        String signStr = signatures.get(0);
        ECKey.ECDSASignature ecdsaSignature;
        try {
            ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(HexUtil.decode(signStr));
        } catch (SignatureDecodeException e) {
            throw new NulsException(ConverterErrorCode.FAILED, e);
        }
        if (!ECKey.verify(sighash.getBytes(), ecdsaSignature, pub.getPubKey())) {
            return false;
        }
        return true;
    }

    public static Sha256Hash hashForSignature(Transaction transaction, int sighashType, int inputNumber,
                                                  byte[] subscript, BigInteger satoshisBN) throws Exception {
        byte[] preimage = sighashPreimageForForkId(transaction, sighashType, inputNumber, subscript, satoshisBN, null);
        //System.out.println(HexUtil.encode(preimage));
        if (Arrays.equals(preimage, SIGHASH_SINGLE_BUG)) {
            throw new RuntimeException("SIGHASH_SINGLE_BUG");
        }
        byte[] hashBuf = Sha256Hash.hashTwice(preimage);
        return Sha256Hash.wrap(hashBuf);
    }

    public static byte[] sighashPreimageForForkId(Transaction transaction, int sighashType, int inputNumber,
                                                  byte[] subscript, BigInteger satoshisBN, HashCache hashCache) throws Exception {
        if (hashCache == null) {
            hashCache = new HashCache();
        }

        TransactionInput input = transaction.getInputs().get(inputNumber);
        if (!(satoshisBN instanceof BigInteger)) {
            throw new IllegalArgumentException("For ForkId=0 signatures, satoshis or complete input must be provided");
        }

        byte[] hashPrevouts = new byte[32];
        byte[] hashSequence = new byte[32];
        byte[] hashOutputs = new byte[32];

        if ((sighashType & SIGHASH_ANYONECANPAY) == 0) {
            hashPrevouts = hashCache.prevoutsHashBuf != null ? hashCache.prevoutsHashBuf : getPrevoutHash(transaction);
            hashCache.prevoutsHashBuf = hashPrevouts;
        }

        if ((sighashType & SIGHASH_ANYONECANPAY) == 0 &&
                (sighashType & 31) != SIGHASH_SINGLE &&
                (sighashType & 31) != SIGHASH_NONE) {
            hashSequence = hashCache.sequenceHashBuf != null ? hashCache.sequenceHashBuf : getSequenceHash(transaction);
            hashCache.sequenceHashBuf = hashSequence;
        }

        if ((sighashType & 31) != SIGHASH_SINGLE && (sighashType & 31) != SIGHASH_NONE) {
            hashOutputs = hashCache.outputsHashBuf != null ? hashCache.outputsHashBuf : getOutputsHash(transaction, -1);
            hashCache.outputsHashBuf = hashOutputs;
        } else if ((sighashType & 31) == SIGHASH_SINGLE && inputNumber < transaction.getOutputs().size()) {
            hashOutputs = getOutputsHash(transaction, inputNumber);
        }

        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
        NulsOutputStreamBuffer writer = new NulsOutputStreamBuffer(bos);
        writer.writeUint32(transaction.getVersion());
        writer.write(hashPrevouts);
        writer.write(hashSequence);


        TransactionOutPoint outpoint = input.getOutpoint();

        writer.write(outpoint.hash().serialize());
        writer.writeUint32(outpoint.index());

        // Write subscript with varint length
        writer.writeBytesWithLength(subscript);

        // Write satoshis (8-byte little-endian)
        SerializeUtils.uint64ToByteStreamLE(satoshisBN, bos);

        writer.writeUint32(input.getSequenceNumber());

        writer.write(hashOutputs);
        writer.writeUint32(transaction.lockTime().rawValue());
        writer.writeUint32(sighashType >>> 0);

        return bos.toByteArray();
    }

    private static byte[] getPrevoutHash(Transaction tx) throws Exception {
        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(tx.getInputs().size() * 36);
        NulsOutputStreamBuffer writer = new NulsOutputStreamBuffer(bos);
        for (TransactionInput input : tx.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            writer.write(outpoint.hash().serialize());
            writer.writeUint32(outpoint.index());
        }
        return Sha256Hash.hashTwice(bos.toByteArray());
    }

    private static byte[] getSequenceHash(Transaction tx) throws Exception {
        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(tx.getInputs().size() * 4);
        NulsOutputStreamBuffer writer = new NulsOutputStreamBuffer(bos);
        for (TransactionInput input : tx.getInputs()) {
            writer.writeUint32(input.getSequenceNumber());
        }
        return Sha256Hash.hashTwice(bos.toByteArray());
    }

    private static byte[] getOutputsHash(Transaction tx, int n) throws Exception {
        ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(1024);
        NulsOutputStreamBuffer writer = new NulsOutputStreamBuffer(bos);

        if (n == -1) {
            for (TransactionOutput output : tx.getOutputs()) {
                writer.write(output.serialize());
            }
        } else {
            writer.write(tx.getOutputs().get(n).serialize());
        }
        return Sha256Hash.hashTwice(bos.toByteArray());
    }

}
