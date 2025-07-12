/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.eth.core;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.Base58;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.crypto.Sha512Hash;
import io.nuls.core.io.IoUtils;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.heterogeneouschain.eth.base.Base;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthAccount;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.crypto.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static io.nuls.core.crypto.ECKey.CURVE;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.PUBLIC_KEY_UNCOMPRESSED_PREFIX;
import static network.nerve.converter.heterogeneouschain.eth.utils.EthUtil.leftPadding;
import static org.web3j.crypto.Hash.sha256;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthAccountTest extends Base {

    private int chainId;

    @Before
    public void initChainId() {
        chainId = 1;
        AddressTool.addPrefix(5, "TNVT");
    }

    @Test
    public void importPriKeyTest() {
        // 0xf173805F1e3fE6239223B17F0807596Edc283012
        //String prikey = "0xD15FDD6030AB81CEE6B519645F0C8B758D112CD322960EE910F65AD7DBB03C2B";
        // 0x008a19c84c6755801f90280706033ecc299edf8dea24693142822db795681de503::::::::0x11eFE2A9CF96175AB241e4A88A6b79C4f1c70389
        //String prikey = "0x008a19c84c6755801f90280706033ecc299edf8dea24693142822db795681de5";
        //String prikey = "53b02c91605451ea35175df894b4c47b7d1effbd05d6b269b3e7c785f3f6dc18";
        //String prikey = "b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5";
        //String prikey = "B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF";
        // 94f024a7c2c30549b7ee932030e7c38f8a9dff22b4b08809fb9e5e2263974717::::::::::0xc99039f0b5e1c8a6a4bb7349cdcfef63288164cc
        // a572b95153b10141ff06c64818c93bd0e7b4025125b83f15a89a7189248191ca::::::::::0x20a495b1f92b135373cd080a60bd58f7dd073d33
        // 7b44f568ca9fc376d12e86e48ef7f4ba66bc709f276bd778e95e0967bd3fc27b::::::::::0xb7c574220c7aaa5d16b9072cf5821bf2ee8930f4
        String prikey = "c48f55dbe619e83502be1f72c875ed616aeaab6108196f0d644d72e992f6a155";
        //String prikey = "71361500124b2e4ca11f68c9148a064bb77fe319d8d27b798af4dda3f4d910cc";
        //String prikey = "1523eb8a85e8bb6641f8ae53c429811ede7ea588c4b8933fed796c667c203c06";
        System.out.println("=========eth==============");
        Credentials credentials = Credentials.create(prikey);
        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        ECKey ecKey = ECKey.fromPrivate(Numeric.hexStringToByteArray(prikey));
        System.out.println(String.format("eth ECKey pubkey: %s", Numeric.toHexString(ecKey.getPubKeyPoint().getEncoded(false))));
        System.out.println(String.format("eth ECKey pubkey: %s", Numeric.toHexString(ecKey.getPubKeyPoint().getEncoded(true))));
        System.out.println();
        ECPoint ecPoint = Sign.publicPointFromPrivate(Numeric.decodeQuantity(Numeric.prependHexPrefix(prikey)));
        System.out.println(String.format("eth Sign util pubkey: %s", Numeric.toHexString(ecPoint.getEncoded(false))));
        System.out.println(String.format("eth Sign util pubkey: %s", Numeric.toHexString(ecPoint.getEncoded(true))));
        System.out.println();
        String msg = "address:\n" + credentials.getAddress()
                + "\nprivateKey:\n" + Numeric.encodeQuantity(ecKeyPair.getPrivateKey())
                + "\nPublicKey:\n" + Numeric.encodeQuantity(ecKeyPair.getPublicKey());
        System.out.println(String.format("eth " + msg));


        System.out.println("=============NULS===============");

        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(false))));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(true))));
        Address address = new Address(chainId, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("NULS address: %s", address.toString()));
        Address testAddress = new Address(2, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("Test NULS address: %s", testAddress.toString()));
        Address nerveAddress = new Address(9, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("Nerve address: %s", nerveAddress.toString()));
        Address testNerveAddress = new Address(5, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("Test Nerve address: %s", testNerveAddress.toString()));
        System.out.println();


        System.out.println("=============Converter===============");
        String pub = Numeric.toHexStringNoPrefix(ecKeyPair.getPublicKey());
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = "04" + pub;
        System.out.println(String.format("pubkeyFromEth: %s", pubkeyFromEth));
        ECPublicKeyParameters publicKey = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(HexUtil.decode(pubkeyFromEth)), CURVE);
        System.out.println(String.format("converter from ETH's pubkey: %s", Numeric.toHexString(publicKey.getQ().getEncoded(true))));
        io.nuls.core.crypto.ECKey ecKey1 = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(pubkeyFromEth));
        System.out.println(String.format("converter1 from ETH's pubkey: %s", Numeric.toHexString(ecKey1.getPubKeyPoint().getEncoded(true))));
        System.out.println(String.format("NULS address: %s", AddressTool.getAddressString(ecKey1.getPubKeyPoint().getEncoded(true), chainId)));
        System.out.println();

    }

    @Test
    public void reverseBytes() {
        String x = "9e66a8c9371278966124a1e4f5f93b1fc8573b33661145f42936f8346c4c376f";
        byte[] xBytes = HexUtil.decode(x);
        int xLength = xBytes.length;
        byte[] reverseX = new byte[xLength];
        for (int i = 0; i < xLength; i++) {
            reverseX[xLength - i - 1] = xBytes[i];
        }
        System.out.println(HexUtil.encode(reverseX));
        String y = "d3dd3438b8adaf553cc1fdfea6d3a4990e792c307928c0fc48a970f35200105d";
        byte[] yBytes = HexUtil.decode(y);
        int yLength = yBytes.length;
        byte[] reverseY = new byte[yLength];
        for (int i = 0; i < yLength; i++) {
            reverseY[yLength - i - 1] = yBytes[i];
        }
        System.out.println(HexUtil.encode(reverseY));
        byte[] concatenate = ArraysTool.concatenate(reverseX, reverseY);
        System.out.println(HexUtil.encode(Sha512Hash.sha512(concatenate)));
    }

    @Test
    public void compressedPubkey() {
        String[] ps = new String[]{"0x71a594ebf01f3b39f5410d384ff17b42e24cd9a8eba0a1f3dcb2a9c2fca81f9a7b7622daee723bc52eec124308a65909e9c8b6fa6e5b887df5a7814d6fe0daf2"};
        for (String p : ps) {
            String compressedPubkey = calcCompressedPubkey(p);
            System.out.println(String.format("compressed pubkey: %s", compressedPubkey));
            System.out.println(String.format("eth address: %s", EthUtil.genEthAddressByCompressedPublickey(compressedPubkey)));
        }
    }

    private String calcCompressedPubkey(String orginalPubkey) {
        String pub = Numeric.cleanHexPrefix(orginalPubkey);
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = "04" + pub;
        //System.out.println(String.format("pubkey From Eth: %s", pubkeyFromEth));
        ECPublicKeyParameters publicKey = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(HexUtil.decode(pubkeyFromEth)), CURVE);
        String compressedPubkey = Numeric.toHexStringNoPrefix(publicKey.getQ().getEncoded(true));
        return compressedPubkey;
    }

    @Test
    public void signDataAndVerifyTest() throws Exception {
        String prikey = "B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF";
        String data = "202008111807";
        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
        byte[] signBytes = nEckey.sign(data.getBytes(StandardCharsets.UTF_8));

        String sign = HexUtil.encode(signBytes);
        String pub = "02e2dbc415264fa490edcc184782ad13af9394ff9559eca8800da4c06b68997784";

        boolean verify = io.nuls.core.crypto.ECKey.verify(
                data.getBytes(StandardCharsets.UTF_8),
                HexUtil.decode(Numeric.cleanHexPrefix(sign)),
                HexUtil.decode(Numeric.cleanHexPrefix(pub)));
        System.out.println(String.format("verify result: %s", verify));
    }

    @Test
    public void verifySignDataTest() throws Exception {
        String data = "abcdefghijk123456789";
        byte[] signBytes = HexUtil.decode("1be2eb61aed3e5d50c0e041db8012bc3e8ba79b2b81aa4d76155880ccfac9d893b61ce8b74f8ad99aa8cea3fa224e7880f50ac317389169568a6f3e21c176e9641");
        String pub = "026887958bcc4cb6f8c04ea49260f0d10e312c41baf485252953b14724db552aac";
        ECDSASignature signature = new ECDSASignature(Numeric.decodeQuantity("0x1be2eb61aed3e5d50c0e041db8012bc3e8ba79b2b81aa4d76155880ccfac9d89"), Numeric.decodeQuantity("0x3b61ce8b74f8ad99aa8cea3fa224e7880f50ac317389169568a6f3e21c176e96"));
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < 4; i++) {
            BigInteger recover = Sign.recoverFromSignature(i, signature, dataBytes);
            System.out.println(String.format("order: %s", HexUtil.encode(recover.toByteArray())));
        }
    }

    @Test
    public void NULSVerify() {
        byte[] data = null;
        byte[] signature = null;
        byte[] pubKey = null;
        boolean verify = io.nuls.core.crypto.ECKey.verify(data, signature, pubKey);
        System.out.println(String.format("verify result: %s", verify));
    }

    @Test
    public void errorPubTest() {
        System.out.println("=============ERROR===============");
        String errorPubkey = "029686cb9ea3e4bbecc9e416e5e3a48a131fc559747f26d1392b4b13a59b334126";
        io.nuls.core.crypto.ECKey ecKey2 = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(errorPubkey));
        System.out.println(String.format("error converter from ETH's pubkey: %s", Numeric.toHexString(ecKey2.getPubKeyPoint().getEncoded(true))));
        System.out.println(String.format("error NULS address: %s", AddressTool.getAddressString(ecKey2.getPubKeyPoint().getEncoded(true), chainId)));
    }

    @Test
    public void createEthAccount() throws Exception {
        String addresses = "";
        String pubkeys = "";
        for (int i = 0; i < 1; i++) {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            String pk = Numeric.encodeQuantity(ecKeyPair.getPrivateKey());
            Credentials credentials = Credentials.create(pk);
            String pub = calcCompressedPubkey(Numeric.encodeQuantity(ecKeyPair.getPublicKey()));
            addresses += credentials.getAddress() + ",";
            pubkeys += pub + ",";
            String msg = "address: " + credentials.getAddress()
                    + ", privateKey: " + Numeric.encodeQuantity(ecKeyPair.getPrivateKey())
                    + ", pubkey: " + pub;
            System.out.println(msg);
        }
        System.out.println(addresses);
        System.out.println(pubkeys);
    }

    @Test
    public void NULSAccountTest() {
        chainId = 2;
        String prikey = "53b02c91605451ea35175df894b4c47b7d1effbd05d6b269b3e7c785f3f6dc18";
        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(false))));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(true))));
        Address address = new Address(chainId, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("NULS address: %s", address.toString()));
        System.out.println();
    }



    @Test
    public void NULSAddressByPubkey() {
        // 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b  NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        // 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d  NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        // 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0  NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        // 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863,
        // 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b,
        // 028181b7534e613143befb67e9bd1a0fa95ed71b631873a2005ceef2774b5916df
        // 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0
        List<String> pubList = new ArrayList<>();
        pubList.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        pubList.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        pubList.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        pubList.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
        pubList.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        pubList.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        pubList.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        pubList.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        pubList.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        pubList.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        pubList.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        pubList.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        pubList.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        pubList.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        pubList.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292");

        chainId = 1;
        String pubkeys = "";
        for (String pubkey : pubList) {
            pubkeys += pubkey + ",";
        }
        System.out.println(pubkeys);
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 1)));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 2)));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 5)));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 9)));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(EthUtil.genEthAddressByCompressedPublickey(pubkey));
        }
        System.out.println();
    }

    @Test
    public void NULSAddressByPrikey() {
        List<String> priList = new ArrayList<>();
        priList.add("71361500124b2e4ca11f68c9148a064bb77fe319d8d27b798af4dda3f4d910cc");
        priList.add("71361500124b2e4ca11f68c9148a064bb77fe319d8d27b798af4dda3f4d910cc");
        priList.add("71361500124b2e4ca11f68c9148a064bb77fe319d8d27b798af4dda3f4d910cc");
        chainId = 1;
        String pubkeys = "", evmAddresses = "", testNulsAddresses = "";
        for (String prikey : priList) {
            io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
            String pubkey = Numeric.toHexStringNoPrefix(nEckey.getPubKeyPoint().getEncoded(true));
            System.out.println("------");
            pubkeys += pubkey + ",";
            evmAddresses += EthUtil.genEthAddressByCompressedPublickey(pubkey) + ",";
            testNulsAddresses += AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 2)) + ",";
            //System.out.println(pubkey);
            System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 1)));
            //System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 2)));
            //System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 9)));
            //System.out.println(EthUtil.genEthAddressByCompressedPublickey(pubkey));
            //System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 5)));
        }
        System.out.println(pubkeys);
        System.out.println(evmAddresses);
        System.out.println(testNulsAddresses);
    }

    @Test
    public void test0() throws Exception {
        /*
        String key = "xpub6H3W6JmYJXN49h5TfcVjLC3onS6uPeUTTJoVvRC8oG9vsTn2J8LwigLzq5tHbrwAzH9DGo6ThGUdWsqce8dGfwHVBxSbixjDADGGdzF7t2B";
        byte[] decode = Base58.decode(key);
        System.out.println("hex: " + HexUtil.encode(decode));
        io.nuls.core.crypto.ECKey ecKey1 = io.nuls.core.crypto.ECKey.fromPublicOnly(decode);
        System.out.println(String.format("converter1 from ETH's pubkey: %s", Numeric.toHexString(ecKey1.getPubKeyPoint().getEncoded(true))));
        */

        // 0488b21e05dd94e1d000000000e2869a9849f8d49b64f8dcc5cb374c10f9a288255907ce20f66af502b895b98b03cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc11534f24c60
        //     cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115
        // 0x04cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc1158190abf51fae206f0a1c825717ed512366620dad8c82b09807e7f27986e5c3fb
        String pub = "02cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
        io.nuls.core.crypto.ECKey ecKey2 = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        System.out.println(String.format("converter1 from ETH's pubkey: %s", Numeric.toHexString(ecKey2.getPubKeyPoint().getEncoded(true))));
        System.out.println(String.format("converter1 from ETH's pubkey: %s", Numeric.toHexString(ecKey2.getPubKeyPoint().getEncoded(false))));

        String internalKey = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
        String outKey = "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        String scriptKey = "5120a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        String address = "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr";

        // 79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798

        //SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(outKey));
        //System.out.println("toBech32: " + segwitAddress.toBech32());
        //
        //Bech32.Bech32Data decode = Bech32.decode(address);
        //System.out.println("data: " + HexUtil.encode(decode.data));
        //System.out.println("hrp: " + decode.hrp);
        //System.out.println("encoding: " + decode.encoding.toString());

        //org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        //ecKey.decompress();
        //ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        //System.out.println(HexUtil.encode(pubKeyPoint.getEncoded(true)));
        //byte[] x = pubKeyPoint.getXCoord().getEncoded();
        //System.out.println(HexUtil.encode(x));
        //byte[] t = taggedHash("TapTweak", x);
        //
        //BigInteger bigT = new BigInteger(t);
        //ECPoint tMultiplyG = point_mul(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        //ECPoint Q = point_add(pubKeyPoint, tMultiplyG);

        //BigInteger bigT = new BigInteger(1, t);
        //ECPoint tMultiplyG = new FixedPointCombMultiplier().multiply(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        //ECPoint Q = pubKeyPoint.add(tMultiplyG);

        //BigInteger bigT = new BigInteger(1, t);
        //ECPoint tMultiplyG = new FixedPointCombMultiplier().multiply(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        //tMultiplyG = org.bitcoinj.crypto.ECKey.CURVE.validatePublicPoint(tMultiplyG);
        //ECPoint Q = pubKeyPoint.add(tMultiplyG);

        //org.bitcoinj.crypto.ECKey ecKeyT = org.bitcoinj.crypto.ECKey.fromPrivate(t);
        //ECPoint Q = pubKeyPoint.add(ecKeyT.getPubKeyPoint());

        //byte[] resultBytes = Q.getXCoord().getEncoded();
        //System.out.println("resultBytes:" + HexUtil.encode(resultBytes));
        //SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, resultBytes);
        //System.out.println("toBech32: " + segwitAddress.toBech32());
    }

    /*@Test
    public void testTapTweak() throws Exception {
        test1();
        //test2();
        test3();
        test4();
        //test5();
    }*/

    //String pub = "02872aa57efbea3a0e1ec23eccb74bb9c64b9d35e4dc75319506ae52cc80b2391d";
    String pub = "02cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
    /*@Test
    public void test5() throws Exception {
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);

        BigInteger bigT = new BigInteger(1, t);
        org.bitcoinj.crypto.ECKey tKey = org.bitcoinj.crypto.ECKey.fromPrivate(bigT);
        System.out.println(tKey.getPublicKeyAsHex());
        ECPoint Q = pubKeyPoint.add(tKey.getPubKeyPoint());

        byte[] resultBytes = Q.getXCoord().getEncoded();
        System.out.println("resultBytes:" + HexUtil.encode(resultBytes));
        SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(HexUtil.encode(resultBytes)));
        System.out.println("toBech32: " + segwitAddress.toBech32());
        System.out.println();

    }

    void printQxcoordAndSegwitAddress(ECPoint Q) {
        byte[] resultBytes = Q.getXCoord().getEncoded();
        System.out.println("resultBytes:" + HexUtil.encode(resultBytes));
        SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(HexUtil.encode(resultBytes)));
        System.out.println("toBech32: " + segwitAddress.toBech32());
        System.out.println();
    }
    @Test
    public void test2() throws Exception {
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        ecKey.decompress();
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);

        BigInteger bigT = new BigInteger(t);
        ECPoint tMultiplyG = point_mul(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        ECPoint Q = point_add(pubKeyPoint, tMultiplyG);

        printQxcoordAndSegwitAddress(Q);
    }
    @Test
    public void test1() throws Exception {
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);
        System.out.println("t: " + HexUtil.encode(t));

        BigInteger bigT = new BigInteger(1, t);
        ECPoint tMultiplyG = new FixedPointCombMultiplier().multiply(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        ECPoint Q = pubKeyPoint.add(tMultiplyG);

        printQxcoordAndSegwitAddress(Q);
    }*/
    /** taprootAddress generation rules (ECPoint.addMethod error, related totaprootIncompatible rules, unknown reason)
    Compute the public key P = privkey * G.
    Let Pb be the serialization of P in x-only form.
    Compute the tweak t = SHA256(SHA256("TapTweak") || SHA256("TapTweak") || Pb), interpreted as 32-byte big endian encoded integer.
    Compute the tweaked public key Q = P + (t * G).
    Let Qb be the serialization of Q in x-only form.
    The address is bech32m_encode(payload=Qb, version=1).
    */
    /*@Test
    public void test3() throws Exception {
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub));
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);

        BigInteger bigT = new BigInteger(1, t);
        ECPoint tMultiplyG = new FixedPointCombMultiplier().multiply(org.bitcoinj.crypto.ECKey.CURVE.getG(), bigT);
        tMultiplyG = org.bitcoinj.crypto.ECKey.CURVE.validatePublicPoint(tMultiplyG);
        ECPoint Q = pubKeyPoint.add(tMultiplyG);

        printQxcoordAndSegwitAddress(Q);
    }

    @Test
    public void tweakHash() {
        byte[] tweakHash = taggedHash("TapTweak", Numeric.hexStringToByteArray("cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"));
        System.out.println(Numeric.toHexStringNoPrefix(tweakHash).equals("2ca01ed85cf6b6526f73d39a1111cd80333bfdc00ce98992859848a90a6f0258"));
    }
    @Test
    public void test4() throws Exception {
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"));
        //ecKey.decompress();
        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
        byte[] x = pubKeyPoint.getXCoord().getEncoded();
        byte[] y = pubKeyPoint.getYCoord().getEncoded();
        byte[] t = taggedHash("TapTweak", x);
        System.out.println("x: " + HexUtil.encode(x));
        System.out.println("y: " + HexUtil.encode(y));
        System.out.println("t: " + HexUtil.encode(t));

        System.out.println("px: " + new BigInteger(1, x));
        System.out.println("py: " + new BigInteger(1, y));
        System.out.println("px hex: " + new BigInteger(1, x).toString(16));
        System.out.println("py hex: " + new BigInteger(1, y).toString(16));
        org.bitcoinj.crypto.ECKey tKey = org.bitcoinj.crypto.ECKey.fromPrivate(t);
        //ECPoint ecPoint = org.bitcoinj.crypto.ECKey.publicPointFromPrivate(new BigInteger(t));
        byte[] tweakX = tKey.getPubKeyPoint().getXCoord().getEncoded();
        byte[] tweakY = tKey.getPubKeyPoint().getYCoord().getEncoded();
        System.out.println("tx: " + new BigInteger(1, tweakX));
        System.out.println("ty: " + new BigInteger(1, tweakY));
        System.out.println("t pub: " + HexUtil.encode(tKey.getPubKey()));
        BigInteger[] addRe = BTCUtilsTest.add(new BigInteger(1, x), new BigInteger(1, y), BigInteger.ONE,
                new BigInteger(1, tweakX), new BigInteger(1, tweakY), BigInteger.ONE);
        BigInteger[] toAffineRe = BTCUtilsTest.toAffine(addRe[0], addRe[1], addRe[2], null);
        String outKey = toAffineRe[0].toString(16);
        System.out.println("tweak pub: " + outKey);
        //String internalKey = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
        //String outKey = "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        //String scriptKey = "5120a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        //String address = "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr";

        SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(outKey));
        System.out.println("toBech32: " + segwitAddress.toBech32());
        System.out.println();


    }*/

    /*private static final BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);

    public static ECPoint point_mul(ECPoint P, BigInteger n) {
        ECPoint R = null;
        for (int i = 0; i < 256; i++) {
            if ((n.shiftRight(i).and(BigInteger.ONE)).compareTo(BigInteger.ONE) == 0) {
                R = point_add(R, P);
            }
            P = point_add(P, P);
        }
        return R;
    }

    public static BigInteger x(ECPoint p) {
        return p.getXCoord().toBigInteger();
    }

    public static BigInteger y(ECPoint p) {
        return p.getYCoord().toBigInteger();
    }

    public static ECPoint point_add(ECPoint P1, ECPoint P2) {
        if (P1 == null) {
            return P2;
        }
        if (P2 == null) {
            return P1;
        }
        if (x(P1).compareTo(x(P2)) == 0 && (y(P1).compareTo(y(P2)) != 0))
            return null;
        BigInteger lam;
        if (P1.equals(P2)) {
            lam = (BigInteger.valueOf(3).multiply(x(P1).multiply(x(P1)).multiply((BigInteger.valueOf(2).multiply(y(P1))).modPow(p.subtract(BigInteger.valueOf(2)), p)))).mod(p);
        } else {
            lam = ((y(P2).subtract(y(P1))).multiply((x(P2).subtract(x(P1))).modPow(p.subtract(BigInteger.valueOf(2)), p))).mod(p);
        }
        BigInteger x3 = (lam.multiply(lam).subtract(x(P1)).subtract(x(P2))).mod(p);
        return org.bitcoinj.crypto.ECKey.CURVE.getCurve().createPoint(x3, (lam.multiply(x(P1).subtract(x3)).subtract(y(P1))).mod(p));
    }*/

    public static byte[] taggedHash(String tag, byte[] msg) {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        System.out.println("tagHash===" + HexUtil.encode(tagHash));
        byte[] doubleTagHash = Arrays.copyOf(tagHash, tagHash.length * 2);
        System.arraycopy(tagHash, 0, doubleTagHash, tagHash.length, tagHash.length);
        System.out.println("doubleTagHash===" + HexUtil.encode(doubleTagHash));
        byte[] input = new byte[tagHash.length * 2 + msg.length];
        System.arraycopy(doubleTagHash, 0, input, 0, doubleTagHash.length);
        System.arraycopy(msg, 0, input, doubleTagHash.length, msg.length);
        System.out.println("input===" + HexUtil.encode(input));
        return Sha256Hash.hash(input);
    }

    /*public static byte[] taggedHash5(String tag, byte[] msg) throws Exception {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        byte[] input = new byte[tagHash.length + msg.length];
        System.arraycopy(tagHash, 0, input, 0, tagHash.length);
        System.arraycopy(msg, 0, input, tagHash.length, msg.length);
        return Sha256Hash.hash(input);
    }

    public static byte[] taggedHash2(String tag, byte[] msg) throws Exception {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        String tagHashStr = HexUtil.encode(tagHash);
        String input = tagHashStr + tagHashStr + HexUtil.encode(msg);
        return Sha256Hash.hash(input.getBytes());
    }

    public static byte[] taggedHash3(String tag, byte[] msg) throws Exception {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        BigInteger tagHashBig = new BigInteger(1, tagHash);
        BigInteger input = tagHashBig.add(tagHashBig).add(new BigInteger(1, msg));
        return Sha256Hash.hash(input.toByteArray());
    }

    public static byte[] taggedHash4(String tag, byte[] msg) throws Exception {
        byte[] tagHash = Sha256Hash.hash(tag.getBytes());
        BigInteger tagHashBig = new BigInteger(tagHash);
        BigInteger input = tagHashBig.add(tagHashBig).add(new BigInteger(msg));
        return Sha256Hash.hash(input.toByteArray());
    }*/

    /**
     * Isolation Witness Address Native
     * @param pubKey
     * @return
     */
    //public static String getBtcSegregatedWitnessAddress(byte[] pubKey) {
    //    try {
    //        PublicKey publicKey = new PublicKey(pubKey, PublicKeyType.SECP256K1);
    //        String address = CoinType.BITCOIN.deriveAddressFromPublicKey(publicKey);
    //        return address;
    //    }catch (Exception e){
    //        return "";
    //    }
    //}
    //
    ///**
    // * Isolation Witness Address compatible
    // * @param pubKey
    // * @return
    // */
    //public static String getBtcSegregatedWitness2Address(byte[] pubKey) {
    //    try {
    //        PublicKey publicKey = new PublicKey(pubKey, PublicKeyType.SECP256K1);
    //        String address = new BitcoinAddress(publicKey,CoinType.BITCOIN.p2shPrefix()).description();
    //        return address;
    //    }catch (Exception e){
    //        return "";
    //    }
    //}

    /*@Test
    public void btcAddressTest() {
        boolean mainnet = true;
        String pubKey = "03c08a1dc38138e28be8af6268da3d3bb58db64fd13c8cd2bc9870ac4ad59787b5";
        System.out.println(genSegWitCompatibleAddress(pubKey, mainnet));
        System.out.println(getBtcSegregatedWitnessAddressByPubkey(pubKey));
    }*/
    /**
     * Generate native isolation witness address
     * @param mnemonic
     * @return
     */
    /*public static String getBtcSegregatedWitnessAddress(String prikey) {
        String address = "";
        try {
            NetworkParameters networkParameters = MainNetParams.get();
            BigInteger privkeybtc = new BigInteger(1, HexUtil.decode(prikey));
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(privkeybtc);
            SegwitAddress segwitAddress = SegwitAddress.fromKey(networkParameters, ecKey);
            address = segwitAddress.toBech32();
            System.out.println("Native Isolation Witness Address："+address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }*/

    /*@Test
    public void segWitCompatibleAddressTest() {
        boolean mainnet = true;
        //String pubKey = "03f028892bad7ed57d2fb57bf33081d5cfcf6f9ed3d3d7f159c2e2fff579dc341a";
        String pubKey = "03c08a1dc38138e28be8af6268da3d3bb58db64fd13c8cd2bc9870ac4ad59787b5";
        System.out.println(genSegWitCompatibleAddress(pubKey, mainnet));
    }

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

    public static String getBtcSegregatedWitnessAddressByPubkey(String pubKey) {
        String address = "";
        try {
            NetworkParameters networkParameters = MainNetParams.get();
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(Numeric.hexStringToByteArray(pubKey));
            SegwitAddress segwitAddress = SegwitAddress.fromKey(networkParameters, ecKey);
            address = segwitAddress.toBech32();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }*/

    @Test
    public void btcPriTest() throws Exception {
        //0c28fca386c7a227600b2fe50b7cae11ec86d3bf1fbe471be89827e19d72aa1d:::KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617
        System.out.println(calcEvmPriByBtcPri("KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617", true));
    }

    private String calcBtcPri(String evmPrikey) {
        return calcBtcPriByEvmPri(evmPrikey, true);
    }

    /**
     * applyevmSystem private key calculationbtcPrivate key
     * @param evmPrikey Ethereum private key
     * @param mainnet Is it the main network
     * @return
     */
    public String calcBtcPriByEvmPri(String evmPrikey, boolean mainnet) {
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
     * applybtcPrivate key calculationevmSeries private key
     * @param btcPrikey btcNetwork private key
     * @param mainnet Is it the main network
     * @return
     */
    public String calcEvmPriByBtcPri(String btcPrikey, boolean mainnet) {
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

    @Test
    public void addressByPubkey() {
        String pubHex = "0x0400fc78c84f04ba951bca59c40e781c7c540b8d543bad1ea4431522b8853e7dae3b17f092d6c21addb3f8b9fbbd4231db129f76d047b9f483fb5b3ff855b88a99";
        io.nuls.core.crypto.ECKey ecKey = io.nuls.core.crypto.ECKey.fromPublicOnly(Numeric.hexStringToByteArray(pubHex));
        String uncompressedPubHex = Numeric.toHexStringNoPrefix(ecKey.getPubKeyPoint().getEncoded(false));
        String compressedPubHex = Numeric.toHexStringNoPrefix(ecKey.getPubKeyPoint().getEncoded(true));
        System.out.println(String.format("uncompressedPubHex : %s", uncompressedPubHex));
        System.out.println(String.format("compressedPubHex : %s", compressedPubHex));
        String address = Numeric.prependHexPrefix(Keys.getAddress(Numeric.prependHexPrefix(uncompressedPubHex.substring(2))));
        System.out.println(address);
    }

    @Test
    public void ETHAddressByPubkey() {
        //0x044477033a4521efee5f90caf30f8eb3284e8d1bb7fef2923ae21617b24aacc8cbce2450fde8f48910e3ffb1455724d0c3671122c86000bae2840ab38dc7766932
        //0x044477033a4521efee5f90caf30f8eb3284e8d1bb7fef2923ae21617b24aacc8cbce2450fde8f48910e3ffb1455724d0c3671122c86000bae2840ab38dc7766932
        //0x32a6d6b1e968f996757cb49fd4f0b08692f9d7f6
        String pubkey = "02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03";
        System.out.println(Keys.toChecksumAddress(EthUtil.genEthAddressByCompressedPublickey(pubkey)));
    }

    @Test
    public void ETHAndNVTAddressByPubkeys() {
        String[] ps = new String[]{
                "0xEe3F3607AEc6d6326de596d8181C04a367E4D2B2, 0x407bde02185657b7cf27516770d2bb0a817c738a0c7c9b28e64134a80f440a56beef86a04d8bab1ffd0b543054c2be43684ee1ee1ff5d87da8fc3077d06193a4",
                "0x189bc7eE35e675FEC9C26Af4F890898e65eBd483, 0x9f253339dd1dc7a2f79ec016b28f99a7c0f6e546a2d4b605ccfa1b4655efbf3e88e88c712b16cecd493368d165201070e018e536b6e54f3024ed2c23978fcf00",
                "0xe00616e7FAAB5Bcc3c7A63746BEa412834BF3676, 0xa996e9ba32d1729799bb6da4de0baff09a2e5b600066749c00c9da0151c5877a8e788d44fb3ca70389ba2fa5b9bb9cda6969a644387bd8bf009c1d4758e7dd37"
        };

        for (String it : ps) {
            String[] s = it.split(",");
            String pub = Numeric.cleanHexPrefix(s[1].trim());
            pub = leftPadding(pub, "0", 128);
            String pubkeyFromEth = "04" + pub;
//                    System.out.println(String.format("pubkey From Eth: %s", pubkeyFromEth));
            ECPublicKeyParameters publicKey = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(HexUtil.decode(pubkeyFromEth)), CURVE);
            String compressedPubkey = Numeric.toHexStringNoPrefix(publicKey.getQ().getEncoded(true));
//                    System.out.println(String.format("compressed pubkey: %s", compressedPubkey));
            String ethAddress = EthUtil.genEthAddressByCompressedPublickey(compressedPubkey);
            if (s[0].equalsIgnoreCase(ethAddress)) {
                System.out.println(String.format("address: %s == %s", ethAddress, AddressTool.getAddressString(HexUtil.decode(compressedPubkey), 9)));
            } else {
                System.out.println(String.format("error: %s != %s", ethAddress, s[0]));
            }
//                    String pub = Numeric.toHexStringNoPrefix(ecKeyPair.getPublicKey());
//                    byte[] bytes = HexUtil.decode(pub);
//                    System.out.println(EthUtil.genEthAddressByCompressedPublickey()+"===="+
//                            AddressTool.getAddressString(,9));
        }

    }

    @Test
    public void ETHAddressByPubkeySet() {
        String[] array = new String[] {
        "039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980",
        "029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4",
        "02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd",
        "03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9",
        "028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7",
        "02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03",
        "035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803",
        "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b",
        "02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d",
        "02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0",
        "03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4",
        "02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049",
        "02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292",
        "02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"};
        // 0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260,03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5,02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750
        // 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863,036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b,028181b7534e613143befb67e9bd1a0fa95ed71b631873a2005ceef2774b5916df
        //String pubkeySet = "03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9,024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75,02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded";
        //String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        System.out.print("[");
        int i = 0;
        for (String key : array) {
            String address = EthUtil.genEthAddressByCompressedPublickey(key);
            if (i == array.length - 1) {
                System.out.print("\"" + address + "\"");
            } else {
                System.out.print("\"" + address + "\", ");
            }
            i++;
        }
        System.out.print("]\n");
        System.out.print("[");
        i = 0;
        for (String key : array) {
            String address = AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(key, 2));
            if (i == array.length - 1) {
                System.out.print("\"" + address + "\"");
            } else {
                System.out.print("\"" + address + "\", ");
            }
            i++;
        }
        System.out.print("]\n");
    }

    @Test
    public void formatETHAddressSet() {
        String pubkeySet = "0xd87f2ad3ef011817319fd25454fc186ca71b3b56, 0x0eb9e4427a0af1fa457230bef3481d028488363e, 0xd6946039519bccc0b302f89493bec60f4f0b4610, 0xb12a6716624431730c3ef55f80c458371954fa52, 0x1f13e90daa9548defae45cd80c135c183558db1f, 0x66fb6d6df71bbbf1c247769ba955390710da40a5, 0x6c9783cc9c9ff9c0f1280e4608afaadf08cfb43d, 0xaff68cd458539a16b932748cf4bdd53bf196789f, 0xc8dcc24b09eed90185dbb1a5277fd0a389855dae, 0xa28035bb5082f5c00fa4d3efc4cb2e0645167444, 0x5c44e5113242fc3fe34a255fb6bdd881538e2ad1, 0x5fbf7793196efbf7066d99fa29dc64dc23052451, 0x33d8ca8e9129dd1c4c9c5ecefaaf362b057b9e74, 0x1e2fd2912bd017824e0179d74c3c28f22ee49add, 0x7c4b783a0101359590e6153df3b58c7fe24ea468";
        String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        System.out.print("[");
        int i = 0;
        for (String address : array) {
            address = address.trim();
            if (i == array.length - 1) {
                System.out.print("\"" + address + "\"");
            } else {
                System.out.print("\"" + address + "\", ");
            }
            i++;
        }
        System.out.print("]\n");
    }

    @Test
    public void formatETHAddressSetII() {
        String pubkeySet = "0x4cAa0869a4E0A4A860143b366F336Fcc5D11d4D8,0x78c30FA073F6CBE9E544f1997B91DD616D66C590,0xb12a6716624431730c3Ef55f80C458371954fA52,0x659EC06A7AeDF09b3602E48D0C23cd3Ed8623a88,0x1F13E90daa9548DEfae45cd80C135C183558db1f,0x8047eC58521dBafF785203Ea070cd23b77257c02,0x66fB6D6dF71bBBf1c247769BA955390710da40A5,0xbB5bA69105a330218E4a433F5e2a273bf0075E64,0x6C9783CC9C9fF9C0F1280E4608AfAaDF08cFb43D,0xA28035Bb5082f5c00fa4d3EFc4CB2e0645167444";
        String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        System.out.print("[");
        int i = 0;
        for (String address : array) {
            if (i == array.length - 1) {
                System.out.print("new Address(\"" + address + "\")");
            } else {
                System.out.print("new Address(\"" + address + "\"), ");
            }
            i++;
        }
        System.out.print("]\n");
    }

    @Test
    public void balanceOfETHAddressSet() throws Exception {
        setMain();
        String pubkeySet = "0x26e6a054a74E6427f135A38C7229f2B31A75e997,0xa83346Ce373678B9a02F186C43a26aa6A2D825eE,0xD571666db5EC21cAafcEdf0c8AFB61CdFB0B0E25";
        String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        for (String address : array) {
            System.out.println(String.format("address %s : %s", address, ethWalletApi.getBalance(address).movePointLeft(18).toPlainString()));
        }
    }

    @Test
    public void formatRpc() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("xxx");
        System.out.println(String.format("size : %s", list.size()));
        int i = 0;
        for (String id : list) {
            if (i == list.size() - 1) {
                System.out.print("https://mainnet.infura.io/v3/" + id);
            } else {
                System.out.print("https://mainnet.infura.io/v3/" + id + ",");
            }
            i++;
        }
    }

    @Test
    public void ethAddress() {
        // ["0xdd7cbedde731e78e8b8e4b2c212bc42fa7c09d03","0xd16634629c638efd8ed90bb096c216e7aec01a91","0x16534991E80117Ca16c724C991aad9EAbd1D7ebe"]
        System.out.println(EthUtil.genEthAddressByCompressedPublickey("a3557f3db11e0005e3c04aa6e8ee2170d7e2469d34243cbb2e7d8f47de3c01e46a29338b80c7c69ee5400267cf64f7f0ced48d18e30b8d76fa3bced37a3a677c"));
    }

    @Test
    public void substringTest() {
        String asd = "https://ropsten.infura.io/v3/cf9ce39514724372bfeac13262e164af";
        System.out.println(asd.substring(asd.indexOf("infura.io") - 1));
    }

    @Test
    public void ethAccountTest() {
        String priKey = "50a0631304ba75b1519c96169a0250795d985832763b06862167aa6bbcd6171f";
        String password = "nuls123456";
        EthAccount account = EthUtil.createAccount(priKey);
        account.encrypt(password);
        System.out.println(account.validatePassword(password));
    }

    @Test
    public void publicKeyTest() {
        EthContext.NERVE_CHAINID = 9;
        String pub = "";
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = PUBLIC_KEY_UNCOMPRESSED_PREFIX + pub;
        //String pubkeyFromEth = "06" + pub;
        io.nuls.core.crypto.ECKey ecKey = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(pubkeyFromEth));
        System.out.println(AddressTool.getAddressString(ecKey.getPubKeyPoint().getEncoded(true), EthContext.NERVE_CHAINID));
    }

    /**
     * keystoreImport Test
     */
    @Test
    public void importKeystoreTest() throws Exception {
        String keystore = "keystoreTest.json";
        String password = "qwer!1234";
        Credentials credentials;
        credentials = WalletUtils.loadJsonCredentials(password, IoUtils.read(keystore));
        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        System.out.println();
        String msg = "address:\n" + credentials.getAddress()
                + "\nprivateKey:\n" + Numeric.encodeQuantity(ecKeyPair.getPrivateKey())
                + "\nPublicKey:\n" + Numeric.encodeQuantity(ecKeyPair.getPublicKey());
        System.out.println(String.format("eth " + msg));
    }

    /**
     * Introduction test for mnemonic wordsII
     */
    @Test
    public void importByMnemonicestII() throws Exception {
        String mnemonic = "deny they health custom company worth tank hungry police direct eternal urban";
        String password = "";
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        System.out.println(Numeric.toHexString(seed));
        ECKeyPair privateKey = ECKeyPair.create(sha256(seed));
        System.out.println(Numeric.toHexString(privateKey.getPrivateKey().toByteArray()));
    }

    @Test
    public void test() {
        String b64 = "ZGVueSB0aGV5IGhlYWx0aCBjdXN0b20gY29tcGFueSB3b3J0aCB0YW5rIGh1bmdyeSBwb2xpY2UgZGlyZWN0IGV0ZXJuYWwgdXJiYW4=";
        System.out.println(Numeric.toHexString(Base64.getDecoder().decode(b64)));
    }


    @Test
    public void batchGenAddress() throws Exception {
        List<String> orderList = new ArrayList<>();
        orderList.add("130");
        orderList.add("131");
        orderList.add("132");
        orderList.add("133");
        orderList.add("134");
        orderList.add("135");
        orderList.add("136");
        orderList.add("137");
        orderList.add("138");
        orderList.add("139");
        orderList.add("140");
        orderList.add("143");
        orderList.add("148");
        orderList.add("146");
        orderList.add("150");
        List<String> list = new ArrayList<>();
        list.add("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5");
        list.add("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f");
        list.add("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499");
        list.add("f89e2563ec7c977cafa2efa41551bd3651d832d587b7d8d8912ebc2b91e24bbd");
        list.add("8c6715620151478cdd4ee8c95b688f2c2112a21a266f060973fa776be3f0ebd7");
        list.add("fe4b382664ff5947eb01287168ad74de2fe6d4c2205034f5e252f57556d0d69f");
        list.add("8654762fcab3ee948a47da4195fee877b8cc7be3fd6c73b81f4cd068805ebd5e");
        list.add("d04808897f8ab1963118019604f691fcde848f093b1fbbdf0092b6a46c0e4b2e");
        list.add("8eaf166834f729194932693ff2328ff2ea00092c60555ef32a9078da4f1b4f39");//nuls138
        list.add("184178c84dfb879606332ccd5fd795478797d720869a1941fb3940c056374010");//139
        list.add("7ce38177977192924fc45d896aca3f2f694a4e1ba43da6e5fc056b9bae832c21");//140
        list.add("40fa95bfc159fd92ca680148b8d6dbc4897ee1af6123169ea3872869799de098");//143
        list.add("bee8ad6c2beacb20bfe07763eaabe39b5f7fa82026d2b60cd35310052bdfbbe7");//146
        list.add("5398d7c9036fffc2129d4878fbddca170157f4d3a3f3d2bada5a7e39e2d31547");//148
        list.add("b7bc0066706a7e148860bda21ab7c9cc63f856290df11ce2b2f18b99161a59b8");//150
        int i = 0;
        for (String prikey : list) {
            System.out.print("nuls" + orderList.get(i++) + ": ");
            io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
            Address address = new Address(chainId, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
            System.out.print(String.format("NULS address: %s", address.toString()));
            Credentials credentials = Credentials.create(prikey);
            System.out.println(String.format(", ETH address: %s", credentials.getAddress()));
        }
    }

    @Test
    public void batchCreateAddress() throws Exception {
        BigDecimal big = new BigDecimal("32.0962374719999");
        System.out.println(big.movePointRight(18).longValue());
    }

    @Test
    public void addressTest() {
        String address = "NERVEepb6E2gaL9pMMocwFFfdiTKSsQar3ycGB";
        System.out.println(AddressTool.validAddress(5, address));
    }

    @Test
    public void order() {
        List<String> list = new ArrayList<>();
        list.add("0xea46968e05c8b204daecd5840fb1aabf8a6f12bd7b8962be4606c21fe47a7cde");
        list.add("0xcd7f8cc1c63e1259b8a18de042f38fda2cd688aa1a5cdcfa51ec1a270af3071a");
        list.add("0xde776843b5eede77cbbcf0eff3beda2dbdf0499d4f01bf4bcfcefeef2a13f1a4");
        list.add("0x03899912203330d3c70a387aa9b6adfe5dd9937d8ba3198fc7cd5a6f2ce1b8b6");
        list.add("0x137fcc5d2d6e7b9afac7b2f1b29756da58337c1e450c2a4b1359f9d3d866b484");
        list.add("0x7eb61c659555895638388b4cb7d19d9788253139d7eb3e1346318bbe7a113a27");
        list.add("0xe19c0bd5ed996680c1ef6a3156a047d12041555318e4be2da573daa0782b8f17");
        list.add("0xfcdf12b63bdebe59db5e6d5683d92268f7288e4f6ada23bafec219e1095c1368");
        list.add("0xd1dfa69f9587199984e56ed641d93184e988b6051167ebc74e5f4a562ce473cd");
        list.add("0xc76468aec1c3745f8971ae10f8bd019bf3d44260bc391d725f52d8d653939fec");
        list.add("0x54964d0b4f06c0e4d49c6533fc442d7796a2e536b51340757a330759262ee0ec");
        list.add("0x47019e5b5c63706b4c92c26c0180f2b804985a155ada085beab3cf45298661bd");
        list.add("0x6d5d9ed20d3a63fdd2770be4801a050bb69ce3407d1be7d053ca722812410c41");
        list.add("0x298e7ca6d31ee2e7b35596e8c4efbfaeeffe2217dcfb523b28327b0f14c33d54");
        list.add("0xc9eb95b5648ac9326ee729b5b447a07fed7afabd1333d2b4cd16227a00f8b514");
        list.add("0xe31e4229dedcd3ab3107c33222e9d93af0450897fd41aa220e1d29ea11d3794c");
        list.add("0x60989a2d308303b38ac3b4213a6934ae1541381c4dd02e8ccc9b3b03cf469002");
        list.add("0xf458a79908db8a2732a77eb4bfbf4f46e998eeb4a20e7fe63b6e0b9d4179136c");
        list.add("0x4204482f2841f97d6094b58df28bba38fbd2dfd5d21e744c4c7db2025ab7f067");
        list.add("0x9ce8a8e2fa4c912f00e4bd4b2a2197850cdf44e8b09993698a5ad750cf34367f");
        list.add("0x9a9512e5642f2981469ba150d35a78d3642162f692ad381ab6f62bbe782463b0");
        list.add("0xc852d08c265b68af80526f9af2c664427161875ef0b8a4f2e8fd0f465a0b1630");
        list.add("0x7c1d0bc7b85e1a5a557f2e40c0acf92bddce694d92a6e1b2b9367e45ecfb74b9");
        list.add("0x1a2473fffa630f32d39fdf7c36a60f5bd6d03f44a67010d3ee355d59c24aae72");
        list.add("0x56c0aff48179e457fa6c2a9c81601fe93d19ce04089eb2e1b5543b3b65662aa9");
        list.add("0x59146c19214f33bb49567e7f4004b3c854b43553731cd4c3d7224520a83b9474");
        list.add("0x4c7077c8050e1d6ba8b48690523690925f5b61c318f5edb586037ea5a6d175de");
        list.add("0xa01f6c8fbf107766b5e5ef366bd8fbdf540afdab128176a4bc99f3e32926fdc4");
        list.add("0x49be112eef5fc5b0fb5d8426215ba5479b6045533871f6bf2d26b6f3444274aa");
        list.add("0x2e07e2a33db44e0de23a2765e77f3541bf3bfcec4afb1459b193062ac8dbe498");
        list.add("0xa36068c6b12f1dab980230b9f084b483d4bb432d1b2449fd4486da36a94bcf99");
        list.add("0x1c5ebf8f56608a0589a106eb4a12e87b5361ec7d7033ad4a8a47e05cbdc9b25d");
        list.add("0x4ddc50956d7c440681163c92cb2ed66a47ae396be2ae906c5a0c269e8233fd6a");
        list.add("0x4c2964e6decebf4db91f1aad4582f43e6ce31c04cb3dc96f72251697cc6dd4a8");
        list.add("0x9b2c3be7f8bdd2f6b449e289a7015ffae64f4d444d2db36b2f7ae2b5f6d7b83c");
        list.add("0x68b76b6cc5cf673674974ab4fc18374c75a545e0e80fc4c3ac942c43fe495cc1");
        list.add("0x9cef3c092162a6c975db05b966bec389cdad5ef33af8e22a073d9293a8af73c8");
        list.add("0x7efad1d8bd87e9005ae698c553e1acb6a7dda25e3a33e20e39704d9fc45031cf");
        list.add("0xf5749d718b64752a17b61dae8ccb30beb9f69ffc463b63a3ed92c2fd8b10e5a9");
        list.add("0x6264d3711ee99e9f3bfe79c79530a9e770afa200866c6c663e34b9f44bd0c464");
        list.add("0x7f594b25e613283520f5baa68916913da38e6e7b07c3817801ddb3a617eb0179");
        list.add("0x0c0418eb2c5e4aa6637f58bc6a5df8b67a876c4d293395d8576c0b45a54647a2");
        list.add("0xa330c0103ea7ecc82d21f0ef3003bd40c398bef6511efdccfdd5090e98469332");
        list.add("0x92a255bd35f555bc6d41f33f88bffcd295353cb9536ba622633c3212f93762a9");
        list.add("0x3ffe867ae3845b68e013770ca83b4a5829de567d9ce17675d1348df177ac6a2e");
        list.add("0x34706e5fa7a28ddebb293c934fdcc114d956b973b29a8228a975a65099f9a320");
        list.add("0x8453dbd831e0da687eefa51b3adc38f94962436b5c4a2392119c1147527d3c9d");
        list.add("0xd8d3ff1f96be2453108358df86f165eec4465233670aaa8e98d6f0e2feb6c82d");
        list.add("0x241d913308f78094e3fe58735e2db1f163524eef0a5c051b9bcf2d8bb082137c");
        list.add("0xfbba4906011a2fefa4e97edf0eb38e20030c822e37164c530b0a08b30276beb8");
        list.add("0xe1ef5325ea0539dce7e11e841c25ff6fb8ffa7d22f07fff101991522be7729ee");
        list.add("0xa4b73f2d619203f5bb034d62a83fe69c82b3397f7cc7330b5900d53e03ba9652");
        list.add("0xe7f7c53fc60ea812b7c50a0ecb373ed0deac090e05771cfb9bebdd92d57700c4");
        list.add("0xe041f807de20bd4a23564eb4ea86f6345b97acfcb50498ccb5e21d6709c73aa5");
        list.add("0xc4cb3ce94354c9d9a7f101ba1f2dd0bda3232cda82b02176603a3614a4444ad6");
        list.add("0xca9de456dbe90a57392cfc6c06183be448d4aec7da1856d6e189b9910b82a87d");
        list.add("0x2073f522e1f563654a1ee774b30b05c3530fd487daf4de49be02ebcba6372615");
        list.add("0xd1a54c6974daea70d03538bdd69250e2a51adc2ab373557944d19b241bffadea");
        list.add("0x8acc47184e3c80b71ad02ea7d8aeeb0dc02bf4291f8f29380e0be0dbffa8e9a6");
        list.add("0x2007366d03bbe9c8d75b3d671dcc430345311950131c77e64e962a7e685ef61e");
        list.add("0x2c36f75b070cee9def53c9830d0d0a513c021fa5a95e5d11391fdca70ab7ebde");
        list.add("0x70fa3abb5e2baf4cb9a2b6b53c1b3a224622ec97724a8e3d46d01a9e3fddb71c");
        list.add("0x37f32834dcbe524ae8cae42bd76de4d35e4b54ca5809ebec0c461984c9f870c4");
        list.add("0xca6f5b884448f897e5200d34866c8161d47bf1cf4706f3db5a0c7134ca9669a2");
        list.add("0xf2027e47f54d5f5b4a504e9b5b159f96c0ebac656afabf1329e4280ed0bfb107");
        list.add("0xc0c60f48823122b5cf41b1fb06fcf8ba15000f50c548bd65f1d4a7fdc157bc4e");
        list.add("0xdb40d6b2215839b0c75114b072b8de28b6395e77b2aac23ba7e680a4a38ec5ab");
        list.add("0xe010792ddb47812329bc47271cf999ef3f1eeefd041539228727f895677090a3");
        list.add("0x09e94c9660385ed82b07a39e63b04d89d151c3ada75b039fc137d7249ebfd06a");
        list.add("0x7be491db9c417912fbf8ed7e6a26c17eccfee449b23a406339f74dc3b07eef04");
        list.add("0xc569fe9d4b54767f836cb6804d424e468481ff7b9ee7af00d7da128201139758");
        list.add("0x6151f49257e52a0e7506c70aef6c6f95dfbcfb27aa33cffc796a531b35cf9929");
        list.add("0xc7bf3d3849ec9e3ffe1b6fbfbdbd43bdbd72535bc984d5d2c315df745bca7ae8");
        list.add("0x53ff72c1a12faee610145fd46e0d3eb4c595b9f9e2e73952e4f05cb2893c08b9");
        list.add("0xe3161c22d7cb35a818ab462acb0951581111b7cace1b8447dbe467440dd38c8b");
        list.add("0xd14f9b94e4286f694f6eed009b74c492060c853ff685020b31eeebce68d25b81");
        list.add("0x79a0970690a2990c2360a05458934da9f3d218f289dffb1ef7121b2b96f9543c");
        list.add("0xf0ca2628f40d733264633f2d1977dd78b8dd49f6e3a52d6544ca839cb7aebed4");
        list.add("0xd4596df8b67a1f8aa533fcb6808a757a8d853a2478380e2d51d729b61a87c14f");
        list.add("0x8a9e3c4e32f31ec3a01671ae8ba0a9ddb6d61ba391a15e2aa2ce8ac07bfc0688");
        list.add("0xf9944ac6ff3e5b492e924b2eef67e80c521e12d57303c1ed6c557c0e5ed26048");
        list.add("0xef1465d7e0dc1a9a40577a2b564977f19524f81549c96d5fa474ca37d8400e9b");
        list.add("0x0f7a7a94ab02b5635b58796865258128e031adc9d0669d16b28c66a74eaa40cf");
        list.add("0xb60666414ea70dd1ae439481b34a50ffa3a937e796663b66aba166b8b50f258f");
        list.add("0xa60cf2367c2a390a6ff6f6e12df3925e7c1d09eadd153b19f78e68c7bcbdfc60");
        list.add("0xec76cae9e21b4f3590fe316cd38a256b9f349850bf0693ca3d478f305d5bd276");
        list.add("0x94de94a333838c0233fc97b6f069a2accda068ea9c78423e04f855cb709b1f28");
        list.add("0x3cbcf98201f7bf4501b0590af11177919055e69e66f510034bd61ab926581d60");
        list.add("0xbebdf69abd096c8f53cb86c6189279bcfb3db6d765f904d3556e01432f47093a");
        list.add("0x1686112eaff5e1ba79d5f6545f6d6b617889a198dc55c125f3dfffc0a4ec25b9");
        list.add("0xd45c5aaed27c8dcafd4ffd402e45db54df19d131081d23526fd15636df553d75");
        list.add("0x67b1e45a8bfe35f37907c5a33c80acf6a132ef3899646e7fed957e1f83662168");
        list.add("0xce7bea1e5543a9c919c163b6acde61138cd04ae82bd4f05a8c70257dcd4f862c");
        list.add("0x3cd3bba566e4f8768f934da69aab650bd04e1e61244ddeab40c55d2828e841d9");
        list.add("0x6a45b60200e92585fafe6e5907cc0fd6c4c1ecca0233b34788ed272fd204f32e");
        list.add("0x1586837a61c389eec676aedeb2b52b0e611ab9495ae286e93a72a580c32047c2");
        list.add("0xd0255b9f269738476d15760ebf14f502848fbe23d0e2bd22d08461b2ab6609dc");
        list.add("0x2f8fb47d6987a15a3ee16abad16f84b774c36676d510f7b53e623525149466b3");
        list.add("0xafe7d3f78a6cf1a1f36b3410f84a1dbfa328f6dc10739e569aa5c86e8b1e2b21");
        list.add("0x3cb41c6cb6ac8b07c2b50ff39148ee9842d1b322ee58fd872f02d95d572cb977");
        list.add("0x04ff4e046332875729122499d00e38c3435ed7bec8837c978392344582eeaa9a");
        list.add("0x273803d4100be58422210953c961f1868a4fbaa88d913a829e2669bfea3e9215");
        list.add("0x711e88e1f720e34687565f5388f165b97acbdc422b87bad2114ee42f8f93e11c");
        list.add("0xf5d9161e78780d935c29d3b8f4465fe2e00cae1389bb1adae5a056a1ab8b2a9b");
        list.add("0x5e97d2153543403f3ba3c45cf2085a56c69a71a86a06406dd35dbd139d7c8e07");
        list.add("0x2dbe9369eb4e56c2fb01fd2f61d7fb0ed6fb16cd8653330705ceeefdacd21a8a");
        list.add("0x80f3a2e98749112482dc1d1786350db20082f33381531dc3d268d70d12bfe8b4");
        list.add("0x793b50d968bdea8a7f8190c24e388739dd886a02d1e6b649354db7ffc77018c1");
        list.add("0x73b329eb258e34e43b40f45750a5f02bff007a4ebb87a9b94beff8c497d64d76");
        list.add("0x7437757f65af38bba5a03f14fe1ad2000ca7e892e2ae168cd5771ce79db22282");
        list.add("0xfd0701585b118dcb68f43fc267feb9805841a7ebf164c0c59d36fe8c0ce316b7");
        list.add("0x18fefde794d24e01917adaee1179d507c463cab09a11eb1158120a2880f103e5");
        list.add("0xad6aa8067fb5d258d718fcdd029feecc4c34c94f5d7aeedb762e22bd4be850e1");
        list.add("0x9e1381899c571c30e4d77de54797a419bf9b11e6c3d8bfd0baabbd9843eff084");
        list.add("0xee64458d361b6bcb28d38ba25aa9271ff44575630ab093ae63db648a3ea9cf7d");
        list.add("0x4418e0ca0382f2b2e65fcef65512cadcbcebc501cad5cc03645ff10cbbc57099");
        list.add("0x046558da0f699ab5acc567629cb6c6b3a406cc81206f1e697d7ef7ba74346c48");
        list.add("0x05a79a14e644dae78b75c5d8312cb21d8c9de31266ddbbced3b319802a5f6f9d");
        list.add("0xdefeaa623297d42084eac717340c06f5631dcd348b9f2abd9177e5d98aaa6933");
        list.add("0x8efda64d4dfbcf0b89adb4bcd1c5b4471c30d52539695aad0337ad327e7f1459");
        list.add("0xc73ce9b1bee52f6eece80a41496f31d1553fbf80497cef4963daf4d2117bd86d");
        list.add("0xcc36f202081229116d64005efc9b508f1ed4302d42198287ea23dacb78d6e23f");
        list.sort((o1, o2) -> o1.compareTo(o2));
        list.stream().forEach(o -> System.out.println(o));
    }
}
