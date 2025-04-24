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

import com.neemre.btcdcli4j.core.domain.RawOutput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.btc.txdata.FtUTXOData;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.Base58;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.base.internal.ByteUtils;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.wallet.Wallet;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;
import static org.bitcoinj.base.BitcoinNetwork.MAINNET;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class TbcUtil {

    public static final BigInteger TBC_FEE = BigInteger.valueOf(300);
    public static final BigInteger PAY_FEE = BigInteger.valueOf(10000);
    public static final int BYZANTINERATIO = 60;

    public static final Map<String, Integer> opcodes = new HashMap<>();

    static {
        // push value
        opcodes.put("OP_FALSE", 0);
        opcodes.put("OP_0", 0);
        opcodes.put("OP_PUSHDATA1", 76);
        opcodes.put("OP_PUSHDATA2", 77);
        opcodes.put("OP_PUSHDATA4", 78);
        opcodes.put("OP_1NEGATE", 79);
        opcodes.put("OP_RESERVED", 80);
        opcodes.put("OP_TRUE", 81);
        opcodes.put("OP_1", 81);
        opcodes.put("OP_2", 82);
        opcodes.put("OP_3", 83);
        opcodes.put("OP_4", 84);
        opcodes.put("OP_5", 85);
        opcodes.put("OP_6", 86);
        opcodes.put("OP_7", 87);
        opcodes.put("OP_8", 88);
        opcodes.put("OP_9", 89);
        opcodes.put("OP_10", 90);
        opcodes.put("OP_11", 91);
        opcodes.put("OP_12", 92);
        opcodes.put("OP_13", 93);
        opcodes.put("OP_14", 94);
        opcodes.put("OP_15", 95);
        opcodes.put("OP_16", 96);

        // control
        opcodes.put("OP_NOP", 97);
        opcodes.put("OP_VER", 98);
        opcodes.put("OP_IF", 99);
        opcodes.put("OP_NOTIF", 100);
        opcodes.put("OP_VERIF", 101);
        opcodes.put("OP_VERNOTIF", 102);
        opcodes.put("OP_ELSE", 103);
        opcodes.put("OP_ENDIF", 104);
        opcodes.put("OP_VERIFY", 105);
        opcodes.put("OP_RETURN", 106);

        // stack ops
        opcodes.put("OP_TOALTSTACK", 107);
        opcodes.put("OP_FROMALTSTACK", 108);
        opcodes.put("OP_2DROP", 109);
        opcodes.put("OP_2DUP", 110);
        opcodes.put("OP_3DUP", 111);
        opcodes.put("OP_2OVER", 112);
        opcodes.put("OP_2ROT", 113);
        opcodes.put("OP_2SWAP", 114);
        opcodes.put("OP_IFDUP", 115);
        opcodes.put("OP_DEPTH", 116);
        opcodes.put("OP_DROP", 117);
        opcodes.put("OP_DUP", 118);
        opcodes.put("OP_NIP", 119);
        opcodes.put("OP_OVER", 120);
        opcodes.put("OP_PICK", 121);
        opcodes.put("OP_ROLL", 122);
        opcodes.put("OP_ROT", 123);
        opcodes.put("OP_SWAP", 124);
        opcodes.put("OP_TUCK", 125);

        // splice ops
        opcodes.put("OP_CAT", 126);
        opcodes.put("OP_SPLIT", 127);
        opcodes.put("OP_NUM2BIN", 128);
        opcodes.put("OP_BIN2NUM", 129);
        opcodes.put("OP_SIZE", 130);

        // bit logic
        opcodes.put("OP_INVERT", 131);
        opcodes.put("OP_AND", 132);
        opcodes.put("OP_OR", 133);
        opcodes.put("OP_XOR", 134);
        opcodes.put("OP_EQUAL", 135);
        opcodes.put("OP_EQUALVERIFY", 136);
        opcodes.put("OP_RESERVED1", 137);
        opcodes.put("OP_RESERVED2", 138);

        // numeric
        opcodes.put("OP_1ADD", 139);
        opcodes.put("OP_1SUB", 140);
        opcodes.put("OP_2MUL", 141);
        opcodes.put("OP_2DIV", 142);
        opcodes.put("OP_NEGATE", 143);
        opcodes.put("OP_ABS", 144);
        opcodes.put("OP_NOT", 145);
        opcodes.put("OP_0NOTEQUAL", 146);

        opcodes.put("OP_ADD", 147);
        opcodes.put("OP_SUB", 148);
        opcodes.put("OP_MUL", 149);
        opcodes.put("OP_DIV", 150);
        opcodes.put("OP_MOD", 151);
        opcodes.put("OP_LSHIFT", 152);
        opcodes.put("OP_RSHIFT", 153);

        opcodes.put("OP_BOOLAND", 154);
        opcodes.put("OP_BOOLOR", 155);
        opcodes.put("OP_NUMEQUAL", 156);
        opcodes.put("OP_NUMEQUALVERIFY", 157);
        opcodes.put("OP_NUMNOTEQUAL", 158);
        opcodes.put("OP_LESSTHAN", 159);
        opcodes.put("OP_GREATERTHAN", 160);
        opcodes.put("OP_LESSTHANOREQUAL", 161);
        opcodes.put("OP_GREATERTHANOREQUAL", 162);
        opcodes.put("OP_MIN", 163);
        opcodes.put("OP_MAX", 164);

        opcodes.put("OP_WITHIN", 165);

        // crypto
        opcodes.put("OP_RIPEMD160", 166);
        opcodes.put("OP_SHA1", 167);
        opcodes.put("OP_SHA256", 168);
        opcodes.put("OP_HASH160", 169);
        opcodes.put("OP_HASH256", 170);
        opcodes.put("OP_CODESEPARATOR", 171);
        opcodes.put("OP_CHECKSIG", 172);
        opcodes.put("OP_CHECKSIGVERIFY", 173);
        opcodes.put("OP_CHECKMULTISIG", 174);
        opcodes.put("OP_CHECKMULTISIGVERIFY", 175);

        opcodes.put("OP_CHECKLOCKTIMEVERIFY", 177);
        opcodes.put("OP_CHECKSEQUENCEVERIFY", 178);

        // expansion
        opcodes.put("OP_NOP1", 176);
        opcodes.put("OP_NOP2", 177);
        opcodes.put("OP_NOP3", 178);
        opcodes.put("OP_NOP4", 179);
        opcodes.put("OP_NOP5", 180);
        opcodes.put("OP_NOP6", 181);
        opcodes.put("OP_NOP7", 182);
        opcodes.put("OP_NOP8", 183);
        opcodes.put("OP_NOP9", 184);
        opcodes.put("OP_NOP10", 185);

        opcodes.put("OP_PUSH_META", 186);
        opcodes.put("OP_PARTIAL_HASH", 187);

        // template matching params
        opcodes.put("OP_PUBKEYHASH", 253);
        opcodes.put("OP_PUBKEY", 254);
        opcodes.put("OP_INVALIDOPCODE", 255);
    }

    public static HtgAccount createAccount(String prikey) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(prikey), true);
        String address = getBtcLegacyAddress(ecKey.getPubKey());
        byte[] pubKey = ecKey.getPubKey();
        HtgAccount account = new HtgAccount();
        account.setAddress(address);
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ecKey.getPrivKeyBytes());
        account.setEncryptedPriKey(new byte[0]);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static boolean validateAddress(String addr) {
        BitcoinNetwork network = MAINNET;
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

    public static String getBtcLegacyAddress(byte[] pubKeyBytes) {
        int version = 0;
        byte[] bytes = SerializeUtils.sha256hash160(pubKeyBytes);
        return Base58.encodeChecked(version, bytes);
    }

    public static HtgAccount createAccountByPubkey(String pubkeyStr) {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode(pubkeyStr));
        byte[] pubKey = ecKey.getPubKeyPoint().getEncoded(true);
        HtgAccount account = new HtgAccount();
        account.setAddress(getBtcLegacyAddress(pubKey));
        account.setPubKey(ecKey.getPubKeyPoint().getEncoded(false));
        account.setPriKey(ZERO_BYTES);
        account.setEncryptedPriKey(ZERO_BYTES);
        account.setCompressedPublicKey(HexUtil.encode(pubKey));
        return account;
    }

    public static String getBtcLegacyAddress(String pubKey) {
        byte[] pubKeyBytes = Numeric.hexStringToByteArray(pubKey);
        LegacyAddress legacyAddress = LegacyAddress.fromPubKeyHash(MAINNET , SerializeUtils.sha256hash160(pubKeyBytes));
        return legacyAddress.toString();
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


    private static byte[] getHash(String[] pubkeys) {
        // Concatenate all public keys
        StringBuilder multiPublicKeys = new StringBuilder();
        for (String pubkey : pubkeys) {
            multiPublicKeys.append(pubkey);
        }
        // Convert to byte buffer
        byte[] buf = HexUtil.decode(multiPublicKeys.toString());
        // Perform SHA-256 followed by RIPEMD-160
        return SerializeUtils.sha256hash160(buf);
    }

    public static String getCombineHash(String address) throws IOException {
        String multisigLockScript = getMultisigLockScript(address);
        byte[] lockScript = decodeASM(multisigLockScript);
        byte[] sha256Hash = Sha256Hash.hash(lockScript);
        byte[] combinedHash = SerializeUtils.sha256hash160(sha256Hash);
        return HexUtil.encode(combinedHash) + "01";
    }

    public static String getAddressHash(String address) throws IOException {
        String multisigLockScript = getMultisigLockScript(address);
        byte[] lockScript = decodeASM(multisigLockScript);
        byte[] sha256Hash = Sha256Hash.hash(lockScript);
        return HexUtil.encode(ByteUtils.reverseBytes(sha256Hash));
    }

    public static byte[] decodeASM(String script) throws IOException {
        String[] parts = script.split(" ");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(byteArrayOutputStream);

        for (String part : parts) {
            if (opcodes.containsKey(part)) {
                writer.writeByte(opcodes.get(part));
            } else if (part.equals("0")) {
                writer.writeByte(opcodes.get("OP_0"));
            } else if (part.equals("-1")) {
                writer.writeByte(opcodes.get("OP_1NEGATE"));
            } else {
                byte[] buf = HexUtil.decode(part);
                writePushData(writer, buf);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }


    private static void writePushData(DataOutputStream writer, byte[] buffer) throws IOException {
        if (buffer.length == 0) {
            writer.write(0); // OP_0
        } else if (buffer.length <= 75) {
            writer.write(buffer.length); // OP_PUSH(buffer.length)
            writer.write(buffer);
        } else if (buffer.length <= 0xFF) {
            writer.write(76); // OP_PUSHDATA1
            writer.write(buffer.length);
            writer.write(buffer);
        } else if (buffer.length <= 0xFFFF) {
            writer.write(77); // OP_PUSHDATA2
            writer.write(buffer.length % 256);
            writer.write(buffer.length >> 8);
            writer.write(buffer);
        } else if (buffer.length <= 0xFFFFFFFF) {
            byte[] prefix = new byte[5];
            prefix[0] = 78; // OP_PUSHDATA4
            int n = buffer.length;
            prefix[1] = (byte)(n % 256);
            n = n / 256;
            prefix[2] = (byte)(n % 256);
            n = n / 256;
            prefix[3] = (byte)(n % 256);
            n = n / 256;
            prefix[4] = (byte)n;
            writer.write(prefix);
            writer.write(buffer);
        } else {
            throw new IOException("data too large");
        }
    }

    public static String genMultisigAddress(int threshold, List<byte[]> pubECKeys) {
        String[] hexPubs = new String[pubECKeys.size()];
        pubECKeys.stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()).toArray(hexPubs);
        return createMultisigAddress(hexPubs, threshold, pubECKeys.size());
    }

    private static String createMultisigAddress(String[] pubkeys, int signatureCount, int publicKeyCount) {
        if (signatureCount < 1 || signatureCount > 15) {
            throw new IllegalArgumentException("Invalid signatureCount.");
        }
        if (publicKeyCount < 3 || publicKeyCount > 15) {
            throw new IllegalArgumentException("Invalid publicKeyCount.");
        }
        pubkeys = sortPubs(pubkeys);
        //System.out.println(Arrays.toString(pubkeys));
        //Arrays.asList(pubkeys).forEach(p -> System.out.println(p));
        byte[] hash = getHash(pubkeys);
        byte prefix = (byte) ((signatureCount << 4) | (publicKeyCount & 0x0F));
        byte[] addressBuffer = new byte[hash.length + 1];
        addressBuffer[0] = prefix;
        System.arraycopy(hash, 0, addressBuffer, 1, hash.length);

        // Calculate checksum
        byte[] doubleHash = Sha256Hash.hashTwice(addressBuffer);
        byte[] checksum = Arrays.copyOfRange(doubleHash, 0, 4);

        // Combine address and checksum
        byte[] finalAddress = new byte[addressBuffer.length + checksum.length];
        System.arraycopy(addressBuffer, 0, finalAddress, 0, addressBuffer.length);
        System.arraycopy(checksum, 0, finalAddress, addressBuffer.length, checksum.length);
        return Base58.encode(finalAddress);
    }

    public static int[] getSignatureAndPublicKeyCount(String address) {
        byte[] buf = Base58.decode(address);
        int prefix = buf[0] & 0xFF;
        int signatureCount = (prefix >> 4) & 0x0F;
        int publicKeyCount = prefix & 0x0F;
        return new int[]{signatureCount, publicKeyCount};
    }

    public static String getMultisigLockScript(String address) {
        byte[] buf = Base58.decode(address);
        int[] counts = getSignatureAndPublicKeyCount(address);
        int signatureCount = counts[0];
        int publicKeyCount = counts[1];

        if (signatureCount < 1 || signatureCount > 6) {
            throw new IllegalArgumentException("Invalid signatureCount.");
        }
        if (publicKeyCount < 3 || publicKeyCount > 10) {
            throw new IllegalArgumentException("Invalid publicKeyCount.");
        }

        String hash = HexUtil.encode(Arrays.copyOfRange(buf, 1, 21));
        StringBuilder lockScriptPrefix = new StringBuilder();

        for (int i = 0; i < publicKeyCount - 1; i++) {
            lockScriptPrefix.append("21 OP_SPLIT ");
        }
        for (int i = 0; i < publicKeyCount; i++) {
            lockScriptPrefix.append("OP_").append(publicKeyCount - 1).append(" OP_PICK ");
        }
        for (int i = 0; i < publicKeyCount - 1; i++) {
            lockScriptPrefix.append("OP_CAT ");
        }

        return String.format(
                "OP_%d OP_SWAP %sOP_HASH160 %s OP_EQUALVERIFY OP_%d OP_CHECKMULTISIG",
                signatureCount, lockScriptPrefix.toString(), hash, publicKeyCount
        );
    }

    private static String[] sortPubs(String[] pubkeys) {
        List<ECKey> pubs = Arrays.asList(pubkeys).stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());
        List<ECKey> sortedPubKeys = new ArrayList<>(pubs);
        Collections.sort(sortedPubKeys, ECKey.PUBKEY_COMPARATOR);
        String[] hexPubs = new String[pubs.size()];
        sortedPubKeys.stream().map(s -> s.getPublicKeyAsHex()).collect(Collectors.toList()).toArray(hexPubs);
        return hexPubs;
    }

    public static boolean verifyMultisigAddress(String[] pubkeys, String address) {
        pubkeys = sortPubs(pubkeys);
        String hashFromPubkeys = HexUtil.encode(getHash(pubkeys));
        byte[] buf = Base58.decode(address);
        String hashFromAddress = HexUtil.encode(Arrays.copyOfRange(buf, 1, 21));
        return hashFromPubkeys.equals(hashFromAddress);
    }

    public static String convertP2MSScriptToMSAddress(String p2msScript) {
        try {
            String[] msScriptList = p2msScript.split(" ");
            int sigNeededCount = Integer.parseInt(msScriptList[0]);
            int sigTotalCount = Integer.parseInt(msScriptList[msScriptList.length - 2]);
            String msPubkeysHash = msScriptList[msScriptList.length - 4];

            byte versionByte = (byte) ((sigNeededCount << 4) | (sigTotalCount & 0x0f));
            //byte[] data = new byte[1 + msPubkeysHash.length() / 2];
            //data[0] = versionByte;
            //System.arraycopy(HexUtil.decode(msPubkeysHash), 0, data, 1, msPubkeysHash.length() / 2);

            String msAddress = Base58.encodeChecked(versionByte, HexUtil.decode(msPubkeysHash));
            return msAddress;

        } catch (Exception e) {
            return "Invalid ms script '" + p2msScript + "': " + e.getMessage();
        }
    }

    public static Object[] parseFTProtocol(RawTransaction decodeTx, String txid, int outputIndex) {
        // Check if the script starts with "9 OP_PICK OP_TOALTSTACK"
        RawOutput output = decodeTx.getVOut().get(outputIndex);
        String scriptAsm = output.getScriptPubKey().getAsm();
        if (!scriptAsm.startsWith("9 OP_PICK OP_TOALTSTACK")) {
            return new Object[0];
        }

        // Check if the FT asset has two outputs
        if (decodeTx.getVOut().size() - outputIndex <= 1) {
            return new Object[0];
        }

        // Get receiver address
        String combineScriptHex = output.getScriptPubKey().getHex();
        String voutCombineScript = combineScriptHex.substring(combineScriptHex.length() - 54, combineScriptHex.length() - 12);
        String pubkeyHash = voutCombineScript.substring(0, voutCombineScript.length() - 2);
        String receiverAddress = Base58.encodeChecked(0, HexUtil.decode(pubkeyHash));

        // Get FT balance
        long ftBalance = 0;
        String ftBalanceTape = decodeTx.getVOut().get(outputIndex + 1).getScriptPubKey().getAsm().substring(12, 108);
        for (int i = 0; i < ftBalanceTape.length(); i += 16) {
            String segment = ftBalanceTape.substring(i, i + 16);
            segment = reverseHexString(segment);
            ftBalance += Long.parseLong(segment, 16);
        }

        // Get code UTXO balance
        double codeTbcValue = output.getValue().doubleValue();
        long codeTbcBalance = (long) (codeTbcValue * 1_000_000);

        return new Object[]{receiverAddress, ftBalance, codeTbcBalance};
    }

    public static String reverseHexString(String segment) {
        StringBuilder reversed = new StringBuilder();
        for (int i = 0; i < segment.length(); i += 2) {
            reversed.insert(0, segment.substring(i, i + 2));
        }
        return reversed.toString();
    }

    public static String calcAddressFromOutput(RawOutput output) {
        List<String> addresses = output.getScriptPubKey().getAddresses();
        if (addresses != null && !addresses.isEmpty()) {
            return addresses.get(0);
        }
        String combineScriptHex = output.getScriptPubKey().getHex();
        String voutCombineScript = combineScriptHex.substring(combineScriptHex.length() - 54, combineScriptHex.length() - 12);
        String pubkeyHash = voutCombineScript.substring(0, voutCombineScript.length() - 2);
        String receiverAddress = Base58.encodeChecked(0, HexUtil.decode(pubkeyHash));
        return receiverAddress;
    }

    public static String calcAddressFromOutput(RawTransaction previousTx, Integer vOut) {
        RawOutput output = previousTx.getVOut().get(vOut);
        List<String> addresses = output.getScriptPubKey().getAddresses();
        if (addresses != null && !addresses.isEmpty()) {
            return addresses.get(0);
        }
        String scriptAsm = output.getScriptPubKey().getAsm();
        if (scriptAsm.startsWith("9 OP_PICK OP_TOALTSTACK") && previousTx.getVOut().size() - vOut > 1) {
            String combineScriptHex = output.getScriptPubKey().getHex();
            String voutCombineScript = combineScriptHex.substring(combineScriptHex.length() - 54, combineScriptHex.length() - 12);
            String pubkeyHash = voutCombineScript.substring(0, voutCombineScript.length() - 2);
            String receiverAddress = Base58.encodeChecked(0, HexUtil.decode(pubkeyHash));
            return receiverAddress;
        }
        if (scriptAsm.endsWith("OP_CHECKMULTISIG")) {
            return TbcUtil.convertP2MSScriptToMSAddress(scriptAsm);
        }
        return null;
    }

    private static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte[] buildFTtransferCode(String code, String multiAddrCombineHash) {
        byte[] codeBuffer = HexUtil.decode(code);
        // 如果接收者是哈希
        if (multiAddrCombineHash.length() != 42) {
            throw new IllegalArgumentException("Invalid multisign hash");
        }
        String hash = multiAddrCombineHash;
        byte[] hashBuffer = HexUtil.decode(hash);
        // 将 hashBuffer 复制到 codeBuffer 的第 1537 字节位置
        System.arraycopy(hashBuffer, 0, codeBuffer, 1537, 21);
        return codeBuffer;
    }

    public static final Comparator<FtUTXOData> FT_BALANCE_COMPARATOR = new Comparator<FtUTXOData>() {
        @Override
        public int compare(FtUTXOData o1, FtUTXOData o2) {
            // order asc
            int compare = o1.getFtBalance().compareTo(o2.getFtBalance());
            if (compare == 0) {
                int compare1 = o1.getTxId().compareTo(o2.getTxId());
                if (compare1 == 0) {
                    return Integer.compare(o1.getOutputIndex(), o2.getOutputIndex());
                }
                return compare1;
            }
            return compare;
        }
    };

    public static List<FtUTXOData> fetchFtUtxosOfMultiSig(List<FtUTXOData> ftUtxoList, BigInteger amount) {
        try {
            // 按 ftBalance 升序排序
            List<FtUTXOData> sortedData = new ArrayList<>(ftUtxoList);
            sortedData.sort(FT_BALANCE_COMPARATOR);

            // 转换为 ftutxos 列表
            List<FtUTXOData> ftutxos = new ArrayList<>(sortedData);

            // 提取 ftBalance 数组并转为 BigInteger
            BigInteger[] ftBalanceArray = ftutxos.stream().map(u -> u.getFtBalance()).toArray(BigInteger[]::new);

            // 根据 ftBalanceArray 长度处理
            switch (ftBalanceArray.length) {
                case 1:
                    if (ftBalanceArray[0].compareTo(amount) >= 0) {
                        return List.of(ftutxos.get(0));
                    } else {
                        throw new RuntimeException("Insufficient FT balance");
                    }
                case 2:
                    BigInteger sum2 = ftBalanceArray[0].add(ftBalanceArray[1]);
                    if (sum2.compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient FT balance");
                    } else if (ftBalanceArray[0].compareTo(amount) >= 0) {
                        return List.of(ftutxos.get(0));
                    } else if (ftBalanceArray[1].compareTo(amount) >= 0) {
                        return List.of(ftutxos.get(1));
                    } else {
                        return List.of(ftutxos.get(0), ftutxos.get(1));
                    }
                case 3:
                    BigInteger sum3 = ftBalanceArray[0].add(ftBalanceArray[1]).add(ftBalanceArray[2]);
                    if (sum3.compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient FT balance");
                    } else if (findMinTwoSum(ftBalanceArray, amount) != null) {
                        int[] result = findMinTwoSum(ftBalanceArray, amount);
                        if (ftBalanceArray[result[0]].compareTo(amount) >= 0) {
                            return List.of(ftutxos.get(result[0]));
                        } else if (ftBalanceArray[result[1]].compareTo(amount) >= 0) {
                            return List.of(ftutxos.get(result[1]));
                        } else {
                            return List.of(ftutxos.get(result[0]), ftutxos.get(result[1]));
                        }
                    } else {
                        return List.of(ftutxos.get(0), ftutxos.get(1), ftutxos.get(2));
                    }
                case 4:
                    BigInteger sum4 = ftBalanceArray[0].add(ftBalanceArray[1]).add(ftBalanceArray[2]).add(ftBalanceArray[3]);
                    if (sum4.compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient FT balance");
                    } else if (findMinThreeSum(ftBalanceArray, amount) != null) {
                        int[] resultThree = findMinThreeSum(ftBalanceArray, amount);
                        BigInteger[] subArray3 = new BigInteger[]{
                                ftBalanceArray[resultThree[0]], ftBalanceArray[resultThree[1]], ftBalanceArray[resultThree[2]]
                        };
                        if (findMinTwoSum(subArray3, amount) != null) {
                            int[] resultTwo = findMinTwoSum(subArray3, amount);
                            int idx0 = resultThree[resultTwo[0]], idx1 = resultThree[resultTwo[1]];
                            if (ftBalanceArray[idx0].compareTo(amount) >= 0) {
                                return List.of(ftutxos.get(idx0));
                            } else if (ftBalanceArray[idx1].compareTo(amount) >= 0) {
                                return List.of(ftutxos.get(idx1));
                            } else {
                                return List.of(ftutxos.get(idx0), ftutxos.get(idx1));
                            }
                        } else {
                            return List.of(ftutxos.get(resultThree[0]), ftutxos.get(resultThree[1]), ftutxos.get(resultThree[2]));
                        }
                    } else {
                        return List.of(ftutxos.get(0), ftutxos.get(1), ftutxos.get(2), ftutxos.get(3));
                    }
                case 5:
                    BigInteger sum5 = ftBalanceArray[0].add(ftBalanceArray[1]).add(ftBalanceArray[2]).add(ftBalanceArray[3]).add(ftBalanceArray[4]);
                    if (sum5.compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient FT balance");
                    } else if (findMinFourSum(ftBalanceArray, amount) != null) {
                        int[] resultFour = findMinFourSum(ftBalanceArray, amount);
                        BigInteger[] subArray4 = new BigInteger[]{
                                ftBalanceArray[resultFour[0]], ftBalanceArray[resultFour[1]],
                                ftBalanceArray[resultFour[2]], ftBalanceArray[resultFour[3]]
                        };
                        if (findMinThreeSum(subArray4, amount) != null) {
                            int[] resultThree = findMinThreeSum(subArray4, amount);
                            BigInteger[] subArray3 = new BigInteger[]{
                                    subArray4[resultThree[0]], subArray4[resultThree[1]], subArray4[resultThree[2]]
                            };
                            if (findMinTwoSum(subArray3, amount) != null) {
                                int[] resultTwo = findMinTwoSum(subArray3, amount);
                                int idx0 = resultFour[resultThree[resultTwo[0]]], idx1 = resultFour[resultThree[resultTwo[1]]];
                                if (ftBalanceArray[idx0].compareTo(amount) >= 0) {
                                    return List.of(ftutxos.get(idx0));
                                } else if (ftBalanceArray[idx1].compareTo(amount) >= 0) {
                                    return List.of(ftutxos.get(idx1));
                                } else {
                                    return List.of(ftutxos.get(idx0), ftutxos.get(idx1));
                                }
                            } else {
                                return List.of(ftutxos.get(resultFour[resultThree[0]]), ftutxos.get(resultFour[resultThree[1]]),
                                        ftutxos.get(resultFour[resultThree[2]]));
                            }
                        } else {
                            return List.of(ftutxos.get(resultFour[0]), ftutxos.get(resultFour[1]),
                                    ftutxos.get(resultFour[2]), ftutxos.get(resultFour[3]));
                        }
                    } else {
                        return List.of(ftutxos.get(0), ftutxos.get(1), ftutxos.get(2), ftutxos.get(3), ftutxos.get(4));
                    }
                default:
                    int[] resultFive = findMinFiveSum(ftBalanceArray, amount);
                    if (resultFive != null) {
                        BigInteger[] subArray5 = new BigInteger[]{
                                ftBalanceArray[resultFive[0]], ftBalanceArray[resultFive[1]], ftBalanceArray[resultFive[2]],
                                ftBalanceArray[resultFive[3]], ftBalanceArray[resultFive[4]]
                        };
                        if (findMinFourSum(subArray5, amount) != null) {
                            int[] resultFour = findMinFourSum(subArray5, amount);
                            BigInteger[] subArray4 = new BigInteger[]{
                                    subArray5[resultFour[0]], subArray5[resultFour[1]],
                                    subArray5[resultFour[2]], subArray5[resultFour[3]]
                            };
                            if (findMinThreeSum(subArray4, amount) != null) {
                                int[] resultThree = findMinThreeSum(subArray4, amount);
                                BigInteger[] subArray3 = new BigInteger[]{
                                        subArray4[resultThree[0]], subArray4[resultThree[1]], subArray4[resultThree[2]]
                                };
                                if (findMinTwoSum(subArray3, amount) != null) {
                                    int[] resultTwo = findMinTwoSum(subArray3, amount);
                                    int idx0 = resultFive[resultFour[resultThree[resultTwo[0]]]];
                                    int idx1 = resultFive[resultFour[resultThree[resultTwo[1]]]];
                                    if (ftBalanceArray[idx0].compareTo(amount) >= 0) {
                                        return List.of(ftutxos.get(idx0));
                                    } else if (ftBalanceArray[idx1].compareTo(amount) >= 0) {
                                        return List.of(ftutxos.get(idx1));
                                    } else {
                                        return List.of(ftutxos.get(idx0), ftutxos.get(idx1));
                                    }
                                } else {
                                    return List.of(ftutxos.get(resultFive[resultFour[resultThree[0]]]),
                                            ftutxos.get(resultFive[resultFour[resultThree[1]]]),
                                            ftutxos.get(resultFive[resultFour[resultThree[2]]]));
                                }
                            } else {
                                return List.of(ftutxos.get(resultFive[resultFour[0]]), ftutxos.get(resultFive[resultFour[1]]),
                                        ftutxos.get(resultFive[resultFour[2]]), ftutxos.get(resultFive[resultFour[3]]));
                            }
                        } else {
                            return List.of(ftutxos.get(resultFive[0]), ftutxos.get(resultFive[1]),
                                    ftutxos.get(resultFive[2]), ftutxos.get(resultFive[3]), ftutxos.get(resultFive[4]));
                        }
                    } else {
                        throw new RuntimeException("Insufficient FT balance");
                    }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // findMinFiveSum
    public static int[] findMinFiveSum(BigInteger[] balances, BigInteger target) {
        BigInteger[] sortedBalances = balances.clone();
        Arrays.sort(sortedBalances);
        int n = sortedBalances.length;
        int[] minFive = new int[0];
        BigInteger minSum = BigInteger.valueOf(Long.MAX_VALUE);
        for (int i = 0; i <= n - 5; i++) {
            for (int j = i + 1; j <= n - 4; j++) {
                int left = j + 1;
                int right = n - 1;
                while (left < right - 1) {
                    BigInteger sum = sortedBalances[i].add(sortedBalances[j])
                            .add(sortedBalances[left]).add(sortedBalances[right]).add(sortedBalances[right - 1]);
                    if (sum.compareTo(target) >= 0 && sum.compareTo(minSum) < 0) {
                        minSum = sum;
                        minFive = new int[]{i, j, left, right - 1, right};
                    }
                    if (sum.compareTo(target) < 0) left++;
                    else right--;
                }
            }
        }
        return minFive.length == 5 ? minFive : null;
    }

    // findMinFourSum
    public static int[] findMinFourSum(BigInteger[] balances, BigInteger target) {
        BigInteger[] sortedBalances = balances.clone();
        Arrays.sort(sortedBalances);
        int n = sortedBalances.length;
        int[] minFour = new int[0];
        BigInteger minSum = BigInteger.valueOf(Long.MAX_VALUE);
        for (int i = 0; i <= n - 4; i++) {
            for (int j = i + 1; j <= n - 3; j++) {
                int left = j + 1;
                int right = n - 1;
                while (left < right) {
                    BigInteger sum = sortedBalances[i].add(sortedBalances[j])
                            .add(sortedBalances[left]).add(sortedBalances[right]);
                    if (sum.compareTo(target) >= 0 && sum.compareTo(minSum) < 0) {
                        minSum = sum;
                        minFour = new int[]{i, j, left, right};
                    }
                    if (sum.compareTo(target) < 0) left++;
                    else right--;
                }
            }
        }
        return minFour.length == 4 ? minFour : null;
    }

    // findMinThreeSum
    public static int[] findMinThreeSum(BigInteger[] balances, BigInteger target) {
        BigInteger[] sortedBalances = balances.clone();
        Arrays.sort(sortedBalances);
        int n = sortedBalances.length;
        int[] minThree = new int[0];
        BigInteger minSum = BigInteger.valueOf(Long.MAX_VALUE);
        for (int i = 0; i <= n - 3; i++) {
            int left = i + 1;
            int right = n - 1;
            while (left < right) {
                BigInteger sum = sortedBalances[i].add(sortedBalances[left]).add(sortedBalances[right]);
                if (sum.compareTo(target) >= 0 && sum.compareTo(minSum) < 0) {
                    minSum = sum;
                    minThree = new int[]{i, left, right};
                }
                if (sum.compareTo(target) < 0) left++;
                else right--;
            }
        }
        return minThree.length == 3 ? minThree : null;
    }

    // findMinTwoSum
    public static int[] findMinTwoSum(BigInteger[] balances, BigInteger target) {
        BigInteger[] sortedBalances = balances.clone();
        Arrays.sort(sortedBalances);
        int n = sortedBalances.length;
        int[] minTwo = new int[0];
        BigInteger minSum = BigInteger.valueOf(Long.MAX_VALUE);
        int left = 0;
        int right = n - 1;
        while (left < right) {
            BigInteger sum = sortedBalances[left].add(sortedBalances[right]);
            if (sum.compareTo(target) >= 0 && sum.compareTo(minSum) < 0) {
                minSum = sum;
                minTwo = new int[]{left, right};
            }
            if (sum.compareTo(target) < 0) left++;
            else right--;
        }
        return minTwo.length == 2 ? minTwo : null;
    }

}
