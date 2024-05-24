///**
// * MIT License
// * <p>
// * Copyright (c) 2017-2018 nuls.io
// * <p>
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * <p>
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// * <p>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//package network.nerve.converter.heterogeneouschain.btc;
//
//import com.neemre.btcdcli4j.core.BitcoindException;
//import com.neemre.btcdcli4j.core.CommunicationException;
//import com.neemre.btcdcli4j.core.NodeProperties;
//import com.neemre.btcdcli4j.core.client.BtcdClient;
//import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
//import com.neemre.btcdcli4j.core.domain.PeerNode;
//import io.nuls.base.basic.AddressTool;
//import io.nuls.core.crypto.HexUtil;
//import io.nuls.core.io.IoUtils;
//import io.nuls.core.parse.JSONUtils;
//import network.nerve.converter.btc.txdata.RechargeData;
//import org.bitcoinj.base.Address;
//import org.bitcoinj.base.Coin;
//import org.bitcoinj.base.SegwitAddress;
//import org.bitcoinj.base.Sha256Hash;
//import org.bitcoinj.core.*;
//import org.bitcoinj.crypto.SignatureDecodeException;
//import org.bitcoinj.crypto.TransactionSignature;
//import org.bitcoinj.params.MainNetParams;
//import org.bitcoinj.params.TestNet3Params;
//import org.bitcoinj.script.*;
//import org.bouncycastle.math.ec.ECPoint;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.IOException;
//import java.math.BigInteger;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//import java.util.concurrent.ExecutionException;
//
//import static network.nerve.converter.heterogeneouschain.btc.BTCUtilsTest.*;
//import static org.bitcoinj.base.BitcoinNetwork.TESTNET;
//
///**
// * @author: PierreLuo
// * @date: 2024/1/5
// */
//public class BtcTransferTest {
//
//    List<String> pris;
//    List<org.bitcoinj.crypto.ECKey> priEcKeys;
//    List<org.bitcoinj.crypto.ECKey> pubEcKeys;
//    List<byte[]> pubs;
//    byte[] priKeyBytesA;
//    byte[] priKeyBytesB;
//    byte[] priKeyBytesC;
//    byte[] priKeyBytesDemo;
//    org.bitcoinj.crypto.ECKey ecKeyA;
//    org.bitcoinj.crypto.ECKey ecKeyB;
//    org.bitcoinj.crypto.ECKey ecKeyC;
//    org.bitcoinj.crypto.ECKey ecKeyDemo;
//    org.bitcoinj.crypto.ECKey ecKey996;
//    String pubkeyA;
//    String pubkeyB;
//    String pubkeyC;
//    String pubkeyDemo;
//    String pubkey996;
//    Map<String, Object> pMap;
//    String packageAddressPrivateKeyZP;
//    String packageAddressPrivateKeyNE;
//    String packageAddressPrivateKeyHF;
//    String fromPriKey996;
//    String legacyAddressA;
//    String legacyAddressB;
//    String legacyAddressC;
//    String legacyAddressDemo;
//    String legacyAddress996;
//    String multisigAddress;
//
//    private String protocol = "http";
//    private String host = "192.168.5.140";
//    private String port = "18332";
//    private String user = "cobble";
//    private String password = "asdf1234";
//    private String auth_scheme = "Basic";
//
//    private BtcdClient client;
//
//    void initRpc() {
//        client = new BtcdClientImpl(makeCurrentProperties());
//    }
//
//    Properties makeCurrentProperties() {
//        Properties nodeConfig = new Properties();
//        nodeConfig.put(NodeProperties.RPC_PROTOCOL.getKey(), protocol);
//        nodeConfig.put(NodeProperties.RPC_HOST.getKey(), host);
//        nodeConfig.put(NodeProperties.RPC_PORT.getKey(), port);
//        nodeConfig.put(NodeProperties.RPC_USER.getKey(), user);
//        nodeConfig.put(NodeProperties.RPC_PASSWORD.getKey(), password);
//        nodeConfig.put(NodeProperties.HTTP_AUTH_SCHEME.getKey(), auth_scheme);
//        return nodeConfig;
//    }
//
//    void setDev() {
//        protocol = "http";
//        host = "192.168.5.140";
//        port = "18332";
//        user = "cobble";
//        password = "asdf1234";
//        auth_scheme = "Basic";
//        initRpc();
//    }
//    void setTestnet() {
//        protocol = "https";
//        host = "btctest.nerve.network";
//        port = "443";
//        user = "Nerve";
//        password = "9o7fSmXPBfPQoQSbnBB";
//        auth_scheme = "Basic";
//        initRpc();
//    }
//    void setMain() {
//        protocol = "https";
//        host = "btc.nerve.network";
//        port = "443";
//        user = "Nerve";
//        password = "9o7fSmXPBCM4F6cAJsfPQoQSbnBB";
//        auth_scheme = "Basic";
//        initRpc();
//    }
//
//    @Before
//    public void before() throws Exception {
//        try {
//            String evmFrom = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
//            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
//            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
//            pMap = JSONUtils.json2map(pData);
//            String packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
//            String packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
//            String packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
//            packageAddressPrivateKeyZP = pMap.get(packageAddressZP).toString();
//            packageAddressPrivateKeyNE = pMap.get(packageAddressNE).toString();
//            packageAddressPrivateKeyHF = pMap.get(packageAddressHF).toString();
//            fromPriKey996 = pMap.get(evmFrom.toLowerCase()).toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        pris = new ArrayList<>();
//        pubs = new ArrayList<>();
//        priEcKeys = new ArrayList<>();
//        pubEcKeys = new ArrayList<>();
//        pris.add(packageAddressPrivateKeyZP);
//        pris.add(packageAddressPrivateKeyNE);
//        pris.add(packageAddressPrivateKeyHF);
//        for (String pri : pris) {
//            byte[] priBytes = HexUtil.decode(pri);
//            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(priBytes, true);
//            pubs.add(ecKey.getPubKey());
//        }
//
//        String priKeyStrDemo = calcEvmPriByBtcPri("L2uPYXe17xSTqbCjZvL2DsyXPCbXspvcu5mHLDYUgzdUbZGSKrSr", true);
//        priKeyBytesA = HexUtil.decode(pris.get(0));
//        priKeyBytesB = HexUtil.decode(pris.get(1));
//        priKeyBytesC = HexUtil.decode(pris.get(2));
//        priKeyBytesDemo = HexUtil.decode(priKeyStrDemo);
//
//        ecKeyA = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesA);
//        ecKeyB = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesB);
//        ecKeyC = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesC);
//        ecKeyDemo = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesDemo);
//        ecKey996 = org.bitcoinj.crypto.ECKey.fromPrivate(HexUtil.decode(fromPriKey996));
//        priEcKeys.add(ecKeyA);
//        priEcKeys.add(ecKeyB);
//        priEcKeys.add(ecKeyC);
//
//        pubkeyA = ecKeyA.getPublicKeyAsHex();
//        pubkeyB = ecKeyB.getPublicKeyAsHex();
//        pubkeyC = ecKeyC.getPublicKeyAsHex();
//        pubkeyDemo = ecKeyDemo.getPublicKeyAsHex();
//        pubkey996 = ecKey996.getPublicKeyAsHex();
//        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyA.getPubKey()));
//        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyB.getPubKey()));
//        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyC.getPubKey()));
//
//        System.out.println(String.format("pubkeyA: %s", pubkeyA));
//        System.out.println(legacyAddressA = getBtcLegacyAddress(pubkeyA, false));
//        System.out.println(String.format("pubkeyB: %s", pubkeyB));
//        System.out.println(legacyAddressB = getBtcLegacyAddress(pubkeyB, false));
//        System.out.println(String.format("pubkeyC: %s", pubkeyC));
//        System.out.println(legacyAddressC = getBtcLegacyAddress(pubkeyC, false));
//        System.out.println(String.format("pubkeyDemo: %s", pubkeyDemo));
//        System.out.println(legacyAddressDemo = getBtcLegacyAddress(pubkeyDemo, false));
//        System.out.println(String.format("pubkey996: %s", pubkey996));
//        System.out.println(legacyAddress996 = getBtcLegacyAddress(pubkey996, false));
//        System.out.println("multiAddr: " + (multisigAddress = multiAddr(pubs, 2, false)));
//        System.out.println("multiAddr: " + (multisigAddress = makeMultiAddr(pubEcKeys, 2, false)));
//
//        setDev();
//    }
//
//    @Test
//    public void taprootAddressTest() throws Exception {
//        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"));
//        //ecKey.decompress();
//        ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
//        byte[] x = pubKeyPoint.getXCoord().getEncoded();
//        byte[] y = pubKeyPoint.getYCoord().getEncoded();
//        byte[] t = BTCUtilsTest.taggedHash("TapTweak", x);
//        System.out.println("x: " + HexUtil.encode(x));
//        System.out.println("y: " + HexUtil.encode(y));
//        System.out.println("t: " + HexUtil.encode(t));
//
//        System.out.println("px: " + new BigInteger(1, x));
//        System.out.println("py: " + new BigInteger(1, y));
//        System.out.println("px hex: " + new BigInteger(1, x).toString(16));
//        System.out.println("py hex: " + new BigInteger(1, y).toString(16));
//        org.bitcoinj.crypto.ECKey tKey = org.bitcoinj.crypto.ECKey.fromPrivate(t);
//        //ECPoint ecPoint = org.bitcoinj.crypto.ECKey.publicPointFromPrivate(new BigInteger(t));
//        byte[] tweakX = tKey.getPubKeyPoint().getXCoord().getEncoded();
//        byte[] tweakY = tKey.getPubKeyPoint().getYCoord().getEncoded();
//        System.out.println("tx: " + new BigInteger(1, tweakX));
//        System.out.println("ty: " + new BigInteger(1, tweakY));
//        System.out.println("t pub: " + HexUtil.encode(tKey.getPubKey()));
//        BigInteger[] addRe = BTCUtilsTest.add(new BigInteger(1, x), new BigInteger(1, y), BigInteger.ONE,
//                new BigInteger(1, tweakX), new BigInteger(1, tweakY), BigInteger.ONE);
//        BigInteger[] toAffineRe = BTCUtilsTest.toAffine(addRe[0], addRe[1], addRe[2], null);
//        String outKey = toAffineRe[0].toString(16);
//        System.out.println("tweak pub: " + outKey);
//        //String internalKey = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
//        //String outKey = "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
//        //String scriptKey = "5120a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
//        //String address = "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr";
//
//        SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(outKey));
//        System.out.println("toBech32: " + segwitAddress.toBech32());
//        System.out.println();
//    }
//
//    @Test
//    public void createMultisigAddress() {
//        //testnet
//        // 0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260,
//        // 03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5,
//        // 02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750
//        List<org.bitcoinj.crypto.ECKey> pubList = new ArrayList<>();
//        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260")));
//        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5")));
//        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750")));
//        System.out.println(String.format("makeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), makeMultiAddr(pubList, 2, false)));
//
//        //mainnet
//        // 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
//        // 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9 - NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE
//        // 03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4 - NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD
//        // 02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292 - NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB
//        // 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
//        // 02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd - NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49
//        // 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9
//        // 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0 - NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
//        // 028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb
//        // 02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049 - NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64
//        // 03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21 - NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63
//        // 02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9 - NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx
//        // 023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90 - NERVEepb6G71f2K3mPKrds8Be7KzdiCsM23Ewq
//        // 035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803 - NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1
//        // 039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980 - NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt
//        List<org.bitcoinj.crypto.ECKey> pubList1 = new ArrayList<>();
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803")));
//        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980")));
//        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), makeMultiAddr(pubList1, 10, true)));
//    }
//
//    @Test
//    public void btcAddressTest() {
//        //pri: 912b6f010e024327865784dd3388d906d4813c236458183574eda28762373d49
//        boolean mainnet = true;
//        String pubKey = "02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded";
//        System.out.println("Common address: " + getBtcLegacyAddress(pubKey, mainnet));
//        System.out.println("Sigwet compatible address: " + genSegWitCompatibleAddress(pubKey, mainnet));
//        System.out.println("Sigwet native address: " + getBtcSegregatedWitnessAddressByPubkey(pubKey, mainnet));
//        System.out.println("Taproot address: " + BTCUtilsTest.genBtcTaprootAddressByPub(pubKey, mainnet));
//        System.out.println("====Testnet address===");
//        System.out.println("Common address: " + getBtcLegacyAddress(pubKey, !mainnet));
//        System.out.println("Sigwet compatible address: " + genSegWitCompatibleAddress(pubKey, !mainnet));
//        System.out.println("Sigwet native address: " + getBtcSegregatedWitnessAddressByPubkey(pubKey, !mainnet));
//        System.out.println("Taproot address: " + BTCUtilsTest.genBtcTaprootAddressByPub(pubKey, !mainnet));
//    }
//
//    @Test
//    public void btcPriTest() throws Exception {
//        //0c28fca386c7a227600b2fe50b7cae11ec86d3bf1fbe471be89827e19d72aa1d:::KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617
//        //System.out.println(calcEvmPriByBtcPri("KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617", true));
//        System.out.println(calcBtcPriByEvmPri("0c28fca386c7a227600b2fe50b7cae11ec86d3bf1fbe471be89827e19d72aa1d", false));
//    }
//
//    @Test
//    public void printTest() {
//        String asd = "importdescriptors requests\n\nImport descriptors. This will trigger a rescan of the blockchain based on the earliest timestamp of all descriptors being imported. Requires a new wallet backup.\n\nNote: This call can take over an hour to complete if using an early timestamp; during that time, other rpc calls\nmay report that the imported keys, addresses or scripts exist but related transactions are still missing.\nThe rescan is significantly faster if block filters are available (using startup option \\\"-blockfilterindex=1\\\").\n\nArguments:\n1. requests                                 (json array, required) Data to be imported\n     [\n       {                                    (json object)\n         \\\"desc\\\": \\\"str\\\",                     (string, required) Descriptor to import.\n         \\\"active\\\": bool,                    (boolean, optional, default=false) Set this descriptor to be the active descriptor for the corresponding output type/externality\n         \\\"range\\\": n or [n,n],               (numeric or array, optional) If a ranged descriptor is used, this specifies the end or the range (in the form [begin,end]) to import\n         \\\"next_index\\\": n,                   (numeric, optional) If a ranged descriptor is set to active, this specifies the next index to generate addresses from\n         \\\"timestamp\\\": timestamp | \\\"now\\\",    (integer / string, required) Time from which to start rescanning the blockchain for this descriptor, in UNIX epoch time\n                                            Use the string \\\"now\\\" to substitute the current synced blockchain time.\n                                            \\\"now\\\" can be specified to bypass scanning, for outputs which are known to never have been used, and\n                                            0 can be specified to scan the entire blockchain. Blocks up to 2 hours before the earliest timestamp\n                                            of all descriptors being imported will be scanned as well as the mempool.\n         \\\"internal\\\": bool,                  (boolean, optional, default=false) Whether matching outputs should be treated as not incoming payments (e.g. change)\n         \\\"label\\\": \\\"str\\\",                    (string, optional, default=\\\"\\\") Label to assign to the address, only allowed with internal=false. Disabled for ranged descriptors\n       },\n       ...\n     ]\n\nResult:\n[                              (json array) Response is an array with the same size as the input that has the execution result\n  {                            (json object)\n    \\\"success\\\" : true|false,    (boolean)\n    \\\"warnings\\\" : [             (json array, optional)\n      \\\"str\\\",                   (string)\n      ...\n    ],\n    \\\"error\\\" : {                (json object, optional)\n      ...                      JSONRPC error\n    }\n  },\n  ...\n]\n\nExamples:\n> bitcoin-cli importdescriptors '[{ \\\"desc\\\": \\\"<my descriptor>\\\", \\\"timestamp\\\":1455191478, \\\"internal\\\": true }, { \\\"desc\\\": \\\"<my descriptor 2>\\\", \\\"label\\\": \\\"example 2\\\", \\\"timestamp\\\": 1455191480 }]'\n> bitcoin-cli importdescriptors '[{ \\\"desc\\\": \\\"<my descriptor>\\\", \\\"timestamp\\\":1455191478, \\\"active\\\": true, \\\"range\\\": [0,100], \\\"label\\\": \\\"<my bech32 wallet>\\\" }]'\n";
//        System.out.println(asd);
//    }
//
//    @Test
//    public void transferTest1() throws IOException {
//        setTestnet();
//        // This transaction is used to test proposal recharge and reissue
//        // cc0866e50e135f1daccdfe50d6df432c860ad08c5088c21eb4536f0b821969fc - Wrong recharge amount
//        // 21be6dbbc71ef9c2aaf638138bf0d1c7ff38a9cad71c7838f17839086818b688 - Wrong recharge address
//        // d2d4ae1323ce8ff910e104a9af35c5379cdd90c0cda6e46436344c73a6a482cc - Wrong fee address
//        Script script = Script.parse(HexUtil.decode("76a914653e11da6a7f573b8acdd6eced87542ea7c9ecb288ac"));
//        Script scriptDemo = Script.parse(HexUtil.decode("76a9148bbc95d2709c71607c60ee3f097c1217482f518d88ac"));
//        script = scriptDemo;
//        List<UTXO> utxos = new ArrayList<>();
//        UTXO utxo = new UTXO(Sha256Hash.wrap("145ace46187e8b69c0374dad9a1b185cdba78466d7729e101193b92497aea190"),
//                1,
//                Coin.valueOf(120000000),
//                0,
//                false,
//                script);
//
//        utxos.add(utxo);
//        Long amount = 119990000l;
//        List<byte[]> opReturns = new ArrayList<>();
//        if (false) {
//            String feeTo = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
//            Long fee = 0l;
//            RechargeData rechargeData = new RechargeData();
//            rechargeData.setTo(AddressTool.getAddress("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad"));
//            rechargeData.setValue(amount);
//            rechargeData.setExtend0("2024-01-24 09:07");
//            //rechargeData.setExtend0("btc deposit test at 2024-01-17 19:55");
//            if (fee.longValue() > 0) {
//                amount += fee;
//                rechargeData.setFeeTo(AddressTool.getAddress(feeTo));
//            }
//            byte[] opReturnBytes = rechargeData.serialize();
//            int length = opReturnBytes.length;
//            System.out.println(String.format("opReturnBytes size: %s", length));
//            for (int i = 1; ; i++) {
//                if (length > i * 80) {
//                    byte[] result = new byte[80];
//                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, 80);
//                    opReturns.add(result);
//                } else {
//                    int len = length - (i - 1) * 80;
//                    byte[] result = new byte[len];
//                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, len);
//                    opReturns.add(result);
//                    break;
//                }
//            }
//        }
//
//        String from = legacyAddressDemo;
//        String to = legacyAddressDemo;
//        Transaction tx = sendTransactionOffline(HexUtil.encode(priKeyBytesDemo), to, from, amount, utxos, opReturns, false);
//        System.out.println("txId: " + tx.getTxId());
//        //broadcast(tx, opReturns.size());
//    }
//
//    void broadcast(Transaction _tx, int opReturnSize) {
//        try {
//            if (opReturnSize < 2) {
//                String hash = client.sendRawTransaction(HexUtil.encode(_tx.serialize()));
//                System.out.println("broadcast txId: " + hash);
//                return;
//            }
//            List<PeerNode> peerInfo = client.getPeerInfo();
//
//            // Broadcast tx
//            PeerGroup pGroup = new PeerGroup(TESTNET);
//            int minConnections = 3;
//            pGroup.startAsync();
//            peerInfo.stream().forEach(p -> System.out.println(p.getAddr()));
//            peerInfo.stream().forEach(p -> {
//                try {
//                    if (!p.getAddr().startsWith("[")) {
//                        pGroup.addAddress(InetAddress.getByName(p.getAddr().split(":")[0]));
//                    }
//                } catch (UnknownHostException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            var txResult = pGroup.broadcastTransaction(_tx, minConnections, true).broadcastOnly().get();
//            System.out.println("txId: " + _tx.getTxId() + "|awaitSentDone: " + txResult.awaitSent().isDone() + "|awaitRelayedDone: " + txResult.awaitRelayed().isDone());
//
//        } catch (ScriptException ex) {
//            throw new RuntimeException(ex);
//        } catch (VerificationException ex) {
//            throw new RuntimeException(ex);
//        } catch (InterruptedException | ExecutionException ex) {
//            throw new RuntimeException(ex);
//        } catch (CommunicationException e) {
//            throw new RuntimeException(e);
//        } catch (BitcoindException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    @Test
//    public void multiTransferTest1() {
//        setTestnet();
//        // MultiSigAddr: 2N2EfstdVXkxSzxomSGbvwVvKJhvvSkveby
//        //Script script = Script.parse(HexUtil.decode("a9140fdd64eb286106a529e5a3276abb43cf0a5fc84687"));
//        Script script = Script.parse(HexUtil.decode("a914629e53e9fbf2329531f12cfc892c962010c29e1d87"));
//        List<UTXO> utxos = new ArrayList<>();
//        UTXO utxo = new UTXO(Sha256Hash.wrap("d979d98004166549a99ccb69515491a18c7f30f9f09315fea67fae00c2148f54"),
//                1,
//                Coin.valueOf(91406),
//                0,
//                false,
//                script);
//
//        utxos.add(utxo);
//        //utxos.add(new UTXO(Sha256Hash.wrap("39804913a56a1bc7e97b48d77536c1d3913d8e2ee120244d33612dc77db775f1"),
//        //        0,
//        //        Coin.valueOf(102102),
//        //        0,
//        //        false,
//        //        script));
//
//        List<byte[]> opReturns = List.of(
//                "withdraw 581a896871161de3b879853cfef41b3bdbf69113ba66b0b9f699535f72a5ba68".getBytes(StandardCharsets.UTF_8)
//        );
//        String to = legacyAddress996;
//        Transaction tx = testMultiSigTx(priEcKeys, pubEcKeys, 10122, to, utxos, opReturns, 3, 2, false);
//        broadcast(tx, opReturns.size());
//    }
//
//    @Test
//    public void multiTransferByOneTest() throws SignatureDecodeException {
//        setTestnet();
//        // MultiSigAddr: 2N2EfstdVXkxSzxomSGbvwVvKJhvvSkveby
//        //Script script = Script.parse(HexUtil.decode("a9140fdd64eb286106a529e5a3276abb43cf0a5fc84687"));
//        Script script = null;
//        List<UTXO> utxos = new ArrayList<>();
//        UTXO utxo = new UTXO(Sha256Hash.wrap("a1438d0ec6d586ab0ac89a16dde29c71a8a8459f8cc48222ea5096c920fafcb9"),
//                0,
//                Coin.valueOf(103008),
//                0,
//                false,
//                script);
//
//        utxos.add(utxo);
//        utxos.add(new UTXO(Sha256Hash.wrap("39804913a56a1bc7e97b48d77536c1d3913d8e2ee120244d33612dc77db775f1"),
//                0,
//                Coin.valueOf(102102),
//                0,
//                false,
//                script));
//        List<byte[]> opReturns = List.of(
//                "withdraw 581a896871161de3b879853cfef41b3bdbf69113ba66b0b9f699535f72a5ba68".getBytes(StandardCharsets.UTF_8)
//        );
//        String to = "tb1qnwnk40t55dsgfd4nuz5aq8sflj8vanh5nskec5";
//        List<String> sign0 = testMultiSigTxByOne(priEcKeys.get(0), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, false);
//        System.out.println(Arrays.deepToString(sign0.toArray()));
//        List<String> sign1 = testMultiSigTxByOne(priEcKeys.get(1), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, false);
//        System.out.println(Arrays.deepToString(sign1.toArray()));
//        List<String> sign2 = testMultiSigTxByOne(priEcKeys.get(2), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, false);
//        System.out.println(Arrays.deepToString(sign2.toArray()));
//
//        Transaction tx = testMultiSigTxByMulti(Map.of(pubEcKeys.get(0).getPublicKeyAsHex(), sign0, pubEcKeys.get(1).getPublicKeyAsHex(), sign1, pubEcKeys.get(2).getPublicKeyAsHex(), sign2), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, false);
//        broadcast(tx, opReturns.size());
//
//    }
//
//    @Test
//    @Deprecated
//    public void transferTest2() {
//        boolean mainnet = false;
//        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
//        long amount = 123123;
//        Address sourceAddress = Address.fromString(networkParameters, legacyAddressA);
//        Address targetAddress = Address.fromString(networkParameters, legacyAddress996);
//        List<UTXO> utxos = new ArrayList<>();
//        UTXO utxo = new UTXO(Sha256Hash.wrap("e21dbc43897d9c3feff9e76fec92ae40a9da52b49e5cff2ba9607609cd1531f0"),
//                1,
//                Coin.valueOf(2367263),
//                2570790,
//                false,
//                ScriptBuilder.createEmpty());
//        utxos.add(utxo);
//        BigInteger privkey = new BigInteger(1, priKeyBytesA);
//        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(privkey);
//        Transaction tx = new Transaction(networkParameters);
//        tx.addOutput(Coin.valueOf(amount), targetAddress);
//        addInputsToTransaction(sourceAddress, tx, utxos, amount);
//        signInputsOfTransaction(sourceAddress, tx, ecKey);
//
//        Transaction.verify(networkParameters, tx);
//
//        new Context();
//        tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
//        tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
//        String valueToSend = HexUtil.encode(tx.serialize());
//        System.out.println("transferTest2: " + valueToSend);
//    }
//
//    private void addInputsToTransaction(Address sourceAddress, Transaction tx, List<UTXO> unspents, Long amount) {
//        long gatheredAmount = 0L;
//        long requiredAmount = amount + 153;
//        for (UTXO unspent : unspents) {
//            gatheredAmount += unspent.getValue().value;
//            TransactionOutPoint outPoint = new TransactionOutPoint(unspent.getIndex(), unspent.getHash());
//            TransactionInput transactionInput = new TransactionInput(tx, ScriptBuilder.createEmpty().program(),
//                    outPoint, Coin.valueOf(unspent.getValue().value));
//            tx.addInput(transactionInput);
//
//            if (gatheredAmount >= requiredAmount) {
//                break;
//            }
//        }
//        if (gatheredAmount > requiredAmount) {
//            //return change to sender, in real life it should use different address
//            tx.addOutput(Coin.valueOf((gatheredAmount - requiredAmount)), sourceAddress);
//        }
//    }
//
//    private void signInputsOfTransaction(Address sourceAddress, Transaction tx, org.bitcoinj.crypto.ECKey key) {
//        for (int i = 0; i < tx.getInputs().size(); i++) {
//            Script scriptPubKey = ScriptBuilder.createOutputScript(sourceAddress);
//            Sha256Hash hash = tx.hashForSignature(i, scriptPubKey, Transaction.SigHash.ALL, true);
//            org.bitcoinj.crypto.ECKey.ECDSASignature ecdsaSignature = key.sign(hash);
//            TransactionSignature txSignature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, true);
//
//            if (ScriptPattern.isP2PK(scriptPubKey)) {
//                tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature));
//            } else {
//                if (!ScriptPattern.isP2PKH(scriptPubKey)) {
//                    throw new ScriptException(ScriptError.SCRIPT_ERR_UNKNOWN_ERROR, "Unable to sign this scrptPubKey: " + scriptPubKey);
//                }
//                tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature, key));
//            }
//        }
//    }
//
//}
