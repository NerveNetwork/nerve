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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.PeerNode;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.RechargeData;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.heterogeneouschain.base.BeanUtilTest;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.model.BtcSignData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import org.bitcoinj.base.*;
import org.bitcoinj.base.internal.ByteUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil.*;
import static org.bitcoinj.base.BitcoinNetwork.MAINNET;
import static org.bitcoinj.base.BitcoinNetwork.TESTNET;
import static org.bitcoinj.core.TransactionInput.NO_SEQUENCE;

/**
 * @author: PierreLuo
 * @date: 2024/1/5
 */
public class BtcTransferTest {

    List<String> pris;
    List<org.bitcoinj.crypto.ECKey> priEcKeys;
    List<org.bitcoinj.crypto.ECKey> pubEcKeys;
    List<byte[]> pubs;
    byte[] priKeyBytesA;
    byte[] priKeyBytesB;
    byte[] priKeyBytesC;
    byte[] priKeyBytesDemo;
    org.bitcoinj.crypto.ECKey ecKeyA;
    org.bitcoinj.crypto.ECKey ecKeyB;
    org.bitcoinj.crypto.ECKey ecKeyC;
    org.bitcoinj.crypto.ECKey ecKeyDemo;
    org.bitcoinj.crypto.ECKey ecKey996;
    String pubkeyA;
    String pubkeyB;
    String pubkeyC;
    String pubkeyDemo;
    String pubkey996;
    Map<String, Object> pMap;
    String packageAddressPrivateKeyZP;
    String packageAddressPrivateKeyNE;
    String packageAddressPrivateKeyHF;
    String fromPriKey996;
    String legacyAddressA; // mpkGu7LBSLf799X91wAZhSyT6hAb4XiTLG
    String legacyAddressB; // mqkFNyzmfVm22a5PJc7HszQsBeZo5GohTv
    String legacyAddressC; // n3NP5XXet1mtBCSsWZcUAj2Px4jRJ9K1Ur
    String legacyAddressDemo;
    String legacyAddress996; // mmLahgkWGHQSKszCDcZXPooWoRuYhQPpCF
    String multisigAddress;
    String p2shMultisigAddress;

    private String protocol = "http";
    private String host = "192.168.5.140";
    private String port = "18332";
    private String user = "cobble";
    private String password = "asdf1234";
    private String auth_scheme = "Basic";

    private BtcdClient client;
    boolean mainnet = false;

    void initRpc() {
        client = new BtcdClientImpl(makeCurrentProperties());
    }

    Properties makeCurrentProperties() {
        Properties nodeConfig = new Properties();
        nodeConfig.put(NodeProperties.RPC_PROTOCOL.getKey(), protocol);
        nodeConfig.put(NodeProperties.RPC_HOST.getKey(), host);
        nodeConfig.put(NodeProperties.RPC_PORT.getKey(), port);
        nodeConfig.put(NodeProperties.RPC_USER.getKey(), user);
        nodeConfig.put(NodeProperties.RPC_PASSWORD.getKey(), password);
        nodeConfig.put(NodeProperties.HTTP_AUTH_SCHEME.getKey(), auth_scheme);
        return nodeConfig;
    }

    void setDev() {
        try {
            String evmFrom = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            String packageAddressZP = "TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV";
            String packageAddressNE = "TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT";
            String packageAddressHF = "TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq";
            this.initKeys(pMap.get(packageAddressZP).toString(),
                            pMap.get(packageAddressNE).toString(),
                            pMap.get(packageAddressHF).toString());
            fromPriKey996 = pMap.get(evmFrom.toLowerCase()).toString();
            ecKey996 = org.bitcoinj.crypto.ECKey.fromPrivate(HexUtil.decode(fromPriKey996));
            pubkey996 = ecKey996.getPublicKeyAsHex();
            System.out.println(String.format("pubkey996: %s, pri996: %s", pubkey996, fromPriKey996));
            System.out.println(legacyAddress996 = getBtcLegacyAddress(pubkey996, false));
        } catch (Exception e) {
            e.printStackTrace();
        }


        /*protocol = "http";
        host = "192.168.5.140";
        port = "18332";
        user = "cobble";
        password = "asdf1234";
        auth_scheme = "Basic";
        initRpc();
        btcWalletApi = new BtcWalletApi();
        btcWalletApi.init("http://192.168.5.140:18332,cobble,asdf1234");
        */
        protocol = "https";
        host = "btctest.nerve.network";
        port = "443";
        user = "Nerve";
        password = "9o7fSmXPBfPQoQSbnBB";
        auth_scheme = "Basic";
        initRpc();
        btcWalletApi = new BitCoinLibWalletApi();
        btcWalletApi.init("https://btctest.nerve.network,Nerve,9o7fSmXPBfPQoQSbnBB");

        ConverterCoreApi coreApi = new ConverterCoreApi();
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(5, 1, "UTF8"));
        coreApi.setNerveChain(chain);
        BtcContext context = new BtcContext();
        context.setConverterCoreApi(coreApi);
        BeanUtilTest.setBean(btcWalletApi, "htgContext", context);
        mainnet = false;
    }

    void setTestnet() {
        System.out.println();
        System.out.println("===========testnet init===============");
        System.out.println();
        this.initKeys(
                pMap.get("t1").toString(),
                pMap.get("t2").toString(),
                pMap.get("t3").toString()
        );
        protocol = "https";
        host = "btctest.nerve.network";
        port = "443";
        user = "Nerve";
        password = "9o7fSmXPBfPQoQSbnBB";
        auth_scheme = "Basic";
        initRpc();
        btcWalletApi = new BitCoinLibWalletApi();
        btcWalletApi.init("https://btctest.nerve.network,Nerve,9o7fSmXPBfPQoQSbnBB");
        ConverterCoreApi coreApi = new ConverterCoreApi();
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(5, 1, "UTF8"));
        coreApi.setNerveChain(chain);
        BtcContext context = new BtcContext();
        context.setConverterCoreApi(coreApi);
        BeanUtilTest.setBean(btcWalletApi, "htgContext", context);
        mainnet = false;
    }

    void setMain() {
        protocol = "https";
        host = "btc.nerve.network";
        port = "443";
        user = "Nerve";
        password = "9o7fSmXPBCM4F6cAJsfPQoQSbnBB";
        auth_scheme = "Basic";
        initRpc();
        btcWalletApi = new BitCoinLibWalletApi();
        btcWalletApi.init("https://btc.nerve.network,Nerve,9o7fSmXPBCM4F6cAJsfPQoQSbnBB");
        ConverterCoreApi coreApi = new ConverterCoreApi();
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(9, 1, "UTF8"));
        coreApi.setNerveChain(chain);
        BtcContext context = new BtcContext();
        context.setConverterCoreApi(coreApi);
        BeanUtilTest.setBean(btcWalletApi, "htgContext", context);
        mainnet = true;
    }

    private BitCoinLibWalletApi btcWalletApi;

    @Before
    public void before() throws Exception {
        setDev();
    }

    public void initKeys(String... keys) {
        packageAddressPrivateKeyZP = keys[0];
        packageAddressPrivateKeyNE = keys[1];
        packageAddressPrivateKeyHF = keys[2];
        pris = new ArrayList<>();
        pubs = new ArrayList<>();
        priEcKeys = new ArrayList<>();
        pubEcKeys = new ArrayList<>();
        pris.add(packageAddressPrivateKeyZP);
        pris.add(packageAddressPrivateKeyNE);
        pris.add(packageAddressPrivateKeyHF);
        for (String pri : pris) {
            byte[] priBytes = HexUtil.decode(pri);
            org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(priBytes, true);
            pubs.add(ecKey.getPubKey());
        }

        String priKeyHexA = pris.get(0);
        String priKeyHexB = pris.get(1);
        String priKeyHexC = pris.get(2);
        priKeyBytesA = HexUtil.decode(priKeyHexA);
        priKeyBytesB = HexUtil.decode(priKeyHexB);
        priKeyBytesC = HexUtil.decode(priKeyHexC);

        ecKeyA = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesA);
        ecKeyB = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesB);
        ecKeyC = org.bitcoinj.crypto.ECKey.fromPrivate(priKeyBytesC);
        priEcKeys.add(ecKeyA);
        priEcKeys.add(ecKeyB);
        priEcKeys.add(ecKeyC);

        pubkeyA = ecKeyA.getPublicKeyAsHex();
        pubkeyB = ecKeyB.getPublicKeyAsHex();
        pubkeyC = ecKeyC.getPublicKeyAsHex();
        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyA.getPubKey()));
        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyB.getPubKey()));
        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKeyC.getPubKey()));

        System.out.println(String.format("pubkeyA: %s, priA: %s", pubkeyA, priKeyHexA));
        System.out.println(legacyAddressA = getBtcLegacyAddress(pubkeyA, false));
        System.out.println(String.format("pubkeyB: %s, priB: %s", pubkeyB, priKeyHexB));
        System.out.println(legacyAddressB = getBtcLegacyAddress(pubkeyB, false));
        System.out.println(String.format("pubkeyC: %s, priC: %s", pubkeyC, priKeyHexC));
        System.out.println(legacyAddressC = getBtcLegacyAddress(pubkeyC, false));
        System.out.println("p2sh-MultiAddr: " + (p2shMultisigAddress = makeMultiAddr(pubEcKeys, 2, false)));
        System.out.println("nativeSegwit-MultiAddr: " + (multisigAddress = getNativeSegwitMultiSignAddress(2, pubEcKeys, false)));
    }

    @Test
    public void feeRateTest() throws Exception {
        System.out.println(btcWalletApi.getFeeRate());
    }

    @Test
    public void feeRateRecommentTest() throws Exception {
        /*{
            "fastestFee": 199,
            "halfHourFee": 191,
            "hourFee": 183,
            "economyFee": 22,
            "minimumFee": 11
        }*/
        String url = "https://mempool.space/testnet/api/v1/fees/recommended";
        //String url = "https://mempool.space/api/v1/fees/recommended";
        try {
            String data = HttpClientUtil.get(String.format(url));
            System.out.println(data);
            Map<String, Object> map = JSONUtils.json2map(data);
            Object object = map.get("fastestFee");
            System.out.println(map);
            if (object != null) {
                System.out.println(Long.parseLong(object.toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void taprootAddressTest() throws Exception {
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
        BigInteger[] addRe = BitCoinLibUtil.add(new BigInteger(1, x), new BigInteger(1, y), BigInteger.ONE,
                new BigInteger(1, tweakX), new BigInteger(1, tweakY), BigInteger.ONE);
        BigInteger[] toAffineRe = BitCoinLibUtil.toAffine(addRe[0], addRe[1], addRe[2], null);
        String outKey = toAffineRe[0].toString(16);
        System.out.println("tweak pub: " + outKey);
        //String internalKey = "cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115";
        //String outKey = "a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        //String scriptKey = "5120a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c";
        //String address = "bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr";

        SegwitAddress segwitAddress = SegwitAddress.fromProgram(MainNetParams.get(), 1, HexUtil.decode(outKey));
        System.out.println("toBech32: " + segwitAddress.toBech32());
        System.out.println();
    }

    @Test
    public void createMultisigAddress() {
        //testnet
        // 0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260,
        // 03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5,
        // 02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750
        List<org.bitcoinj.crypto.ECKey> pubList = new ArrayList<>();
        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260")));
        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5")));
        pubList.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750")));
        System.out.println(String.format("makeMultiAddr (%s of %s) for testnet: %s, segwit: %s", 2, pubList.size(), makeMultiAddr(pubList, 2, false), getNativeSegwitMultiSignAddress(2, pubList, false)));

        //mainnet
        // 0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        // 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9 - NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE
        // 03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4 - NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD
        // 02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292 - NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB
        // 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        // 02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd - NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49
        // 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9
        // 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0 - NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        // 028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb
        // 02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049 - NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64
        // 03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21 - NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63
        // 02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9 - NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx
        // 023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90 - NERVEepb6G71f2K3mPKrds8Be7KzdiCsM23Ewq
        // 035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803 - NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1
        // 039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980 - NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt
        List<org.bitcoinj.crypto.ECKey> pubList1 = new ArrayList<>();
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803")));
        pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980")));
        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s, segwit: %s", 10, pubList1.size(), makeMultiAddr(pubList1, 10, true), getNativeSegwitMultiSignAddress(10, pubList1, true)));

        BitcoinNetwork network = TESTNET;
        Wallet basic = Wallet.createBasic(network);
        Address receiveAddress = basic.parseAddress("2NDu3vcpjyiMgvRjDpQfbyh9uF2McfDJ3NF");
        System.out.println(receiveAddress);
    }

    @Test
    public void btcAddressTest() {
        //pri: 912b6f010e024327865784dd3388d906d4813c236458183574eda28762373d49
        boolean mainnet = true;
        String pubKey = "02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded";
        System.out.println("Common address: " + getBtcLegacyAddress(pubKey, mainnet));
        System.out.println("Sigwet compatible address: " + genSegWitCompatibleAddress(pubKey, mainnet));
        System.out.println("Sigwet native address: " + getNativeSegwitAddressByPubkey(pubKey, mainnet));
        System.out.println("Taproot address: " + genBtcTaprootAddressByPub(pubKey, mainnet));
        System.out.println("====Testnet address===");
        System.out.println("Common address: " + getBtcLegacyAddress(pubKey, !mainnet));
        System.out.println("Sigwet compatible address: " + genSegWitCompatibleAddress(pubKey, !mainnet));
        System.out.println("Sigwet native address: " + getNativeSegwitAddressByPubkey(pubKey, !mainnet));
        System.out.println("Taproot address: " + genBtcTaprootAddressByPub(pubKey, !mainnet));
    }

    @Test
    public void feePaymenyAddrTest() {
        String pub = "222222222222222222222222222222222222222222222222222222222222222222";
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pub, 5)));
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pub, 9)));
    }

    @Test
    public void btcPriTest() throws Exception {
        //0c28fca386c7a227600b2fe50b7cae11ec86d3bf1fbe471be89827e19d72aa1d:::KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617
        //System.out.println(calcEvmPriByBtcPri("KwdMAjGmerYanjeui5SHS7JkmpZvVipYvB2LJGU1ZxJwYvP98617", true));
        System.out.println(calcBtcPriByEvmPri("0c28fca386c7a227600b2fe50b7cae11ec86d3bf1fbe471be89827e19d72aa1d", false));
    }

    @Test
    public void printTest() {
        String asd = "importdescriptors requests\n\nImport descriptors. This will trigger a rescan of the blockchain based on the earliest timestamp of all descriptors being imported. Requires a new wallet backup.\n\nNote: This call can take over an hour to complete if using an early timestamp; during that time, other rpc calls\nmay report that the imported keys, addresses or scripts exist but related transactions are still missing.\nThe rescan is significantly faster if block filters are available (using startup option \\\"-blockfilterindex=1\\\").\n\nArguments:\n1. requests                                 (json array, required) Data to be imported\n     [\n       {                                    (json object)\n         \\\"desc\\\": \\\"str\\\",                     (string, required) Descriptor to import.\n         \\\"active\\\": bool,                    (boolean, optional, default=false) Set this descriptor to be the active descriptor for the corresponding output type/externality\n         \\\"range\\\": n or [n,n],               (numeric or array, optional) If a ranged descriptor is used, this specifies the end or the range (in the form [begin,end]) to import\n         \\\"next_index\\\": n,                   (numeric, optional) If a ranged descriptor is set to active, this specifies the next index to generate addresses from\n         \\\"timestamp\\\": timestamp | \\\"now\\\",    (integer / string, required) Time from which to start rescanning the blockchain for this descriptor, in UNIX epoch time\n                                            Use the string \\\"now\\\" to substitute the current synced blockchain time.\n                                            \\\"now\\\" can be specified to bypass scanning, for outputs which are known to never have been used, and\n                                            0 can be specified to scan the entire blockchain. Blocks up to 2 hours before the earliest timestamp\n                                            of all descriptors being imported will be scanned as well as the mempool.\n         \\\"internal\\\": bool,                  (boolean, optional, default=false) Whether matching outputs should be treated as not incoming payments (e.g. change)\n         \\\"label\\\": \\\"str\\\",                    (string, optional, default=\\\"\\\") Label to assign to the address, only allowed with internal=false. Disabled for ranged descriptors\n       },\n       ...\n     ]\n\nResult:\n[                              (json array) Response is an array with the same size as the input that has the execution result\n  {                            (json object)\n    \\\"success\\\" : true|false,    (boolean)\n    \\\"warnings\\\" : [             (json array, optional)\n      \\\"str\\\",                   (string)\n      ...\n    ],\n    \\\"error\\\" : {                (json object, optional)\n      ...                      JSONRPC error\n    }\n  },\n  ...\n]\n\nExamples:\n> bitcoin-cli importdescriptors '[{ \\\"desc\\\": \\\"<my descriptor>\\\", \\\"timestamp\\\":1455191478, \\\"internal\\\": true }, { \\\"desc\\\": \\\"<my descriptor 2>\\\", \\\"label\\\": \\\"example 2\\\", \\\"timestamp\\\": 1455191480 }]'\n> bitcoin-cli importdescriptors '[{ \\\"desc\\\": \\\"<my descriptor>\\\", \\\"timestamp\\\":1455191478, \\\"active\\\": true, \\\"range\\\": [0,100], \\\"label\\\": \\\"<my bech32 wallet>\\\" }]'\n";
        System.out.println(asd);
    }

    @Test
    public void decodeTest() {
        String txHex = "02000000000101ad7c763c3e320079241f8e683934215d9dac18c28e3bd16ad09fc2612f7f80ac0000000000ffffffff03e3270000000000001976a9143fda920e686292be324b438d6509123ecd8e1e9f88ac3337010000000000220020ea1b6f683372648bcf9c11a150ae55a3cb41d410f0fb5c86b28389318166c9520000000000000000226a2024c42295ad91d8157acd24a6d8b70ef970d97d3e6acc528dcbfde749a499eaa70400473044022011541f22a964210d075f6a31deef4214612b64c1680be10098e899fff2f86d52022010ba9086010535d7fecdc0d284a033f8fe21dbb97f8a8fe3fc5b2c42ce38fc080147304402201b531a43a52086f4dff8c12104440df12d796f5f5b5dfc05eb63fd151a3e331102205f39f4a840a975ae70dea7ba291a0422a24efe5f92f0f1e2d95443a88d65007a01695221024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba752102c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded2103b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd953ae00000000";
        Transaction tx = Transaction.read(ByteBuffer.wrap(HexUtil.decode(txHex)));
        System.out.println(tx.getFee());
    }

    @Test
    public void nativeSegwitMultiSignAddr() {
        NetworkParameters network = TestNet3Params.get();
        List<ECKey> sortedPubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(sortedPubKeys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(2, sortedPubKeys);
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address address = scriptPubKey.getToAddress(network);
        System.out.println(address.toString());
    }

    @Test
    public void nativeSegwitMultiSignAddrFromRedeemScriptTest() {
        String st = "522103b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd921024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba752102c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded53ae";
        nativeSegwitMultiSignAddrFromRedeemScript(st);
    }

    public void nativeSegwitMultiSignAddrFromRedeemScript(String st) {
        NetworkParameters network = TestNet3Params.get();
        Script redeemScript = Script.parse(HexUtil.decode(st));
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address address = scriptPubKey.getToAddress(network);
        System.out.println(String.format("p2wsh addr: %s", address.toString()));
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

    @Test
    public void nativeSegwitMultiSignTxFor10Of15Test() throws Exception {
        setTestnet();
        boolean mainnet = false;

        List<UTXO> utxos = new ArrayList<>();
        UTXO utxo = new UTXO(Sha256Hash.wrap("7e8484934ca5358952680926a81785946fc8367bf8f868dfcb067f65f495b462"),
                1,
                Coin.valueOf(172324),
                0,
                false,
                null);

        utxos.add(utxo);

        List<byte[]> opReturns = List.of(
                "withdraw 581a896871161de3b879853cfef41b3bdbf69113ba66b0b9f699535f72a5ba68".getBytes(StandardCharsets.UTF_8)
        );
        System.out.println("opReturns size: " + opReturns.get(0).length);

        List<ECKey> ecKeys = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ecKeys.add(new ECKey());
        }

        String to = legacyAddress996;

        Transaction tx = makeNativeSegwitMultiSignTx(ecKeys, ecKeys, 7100, to,
                utxos,
                opReturns,
                10, 15, btcWalletApi.getFeeRate(),
                mainnet,
                false
        );
        System.out.println(tx.getVsize());
        //broadcast(tx, opReturns.size());
    }

    @Test
    public void testWithdraw() throws Exception {
        //setTestnet();
        //addPri("43DA7C269917207A3CBB564B692CD57E9C72F9FCFDB17EF2190DD15546C4ED9D");
        nativeSegwitMultiSignTx();
    }

    private void addPri(String pri) {
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(pri));
        priEcKeys.add(ecKey);
        pubEcKeys.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(ecKey.getPubKey()));
    }

    @Test
    public void nativeSegwitMultiSignTx() throws Exception {
        NetworkParameters network = TestNet3Params.get();
        List<ECKey> pubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(BitCoinLibUtil.getByzantineCount(pubKeys.size()), pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address address = scriptPubKey.getToAddress(network);
        System.out.println(address.toString());

        boolean mainnet = false;

        List<UTXO> utxos = new ArrayList<>();
        utxos.add(new UTXO(
                Sha256Hash.wrap("3df9e383939f8754df2c1bfa0c6ec0959b050318f9f2c3582386f0ddda5d4100"),
                1, Coin.valueOf(3000),
                0,false,null));
        /*List<UTXOData> accountUTXOs = btcWalletApi.getAccountUTXOs(address.toString());
        long total = 0;
        for (UTXOData data : accountUTXOs) {
            UTXO utxo = new UTXO(Sha256Hash.wrap(data.getTxid()),
                    data.getVout(),
                    Coin.valueOf(data.getAmount().longValue()),
                    0,
                    false,
                    null);
            utxos.add(utxo);
            total += data.getAmount().longValue();
        }*/

        List<byte[]> opReturns = List.of(
                HexUtil.decode("3bd54c256fad94405017690150767e819eb1b6539ea68b864f72d90eea4824e2")
        );
        System.out.println("opReturns size: " + opReturns.get(0).length);
        String to = address.toString();
        //long feeRate = btcWalletApi.getFeeRate();
        long feeRate = 1;
        Transaction tx = makeNativeSegwitMultiSignTx(priEcKeys, pubEcKeys, 1000, to,
                utxos,
                opReturns,
                BitCoinLibUtil.getByzantineCount(pubEcKeys.size()), pubEcKeys.size(),
                feeRate,
                mainnet,
                false
        );
        System.out.println("getVsize: " + tx.getVsize());
        broadcast(tx, opReturns == null ? 0 : opReturns.size());
    }

    @Test
    public void testDeposit() throws Exception {
        String from = legacyAddress996;
        String to = multisigAddress;
        String nerveTo = "TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad";
        //String nerveTo = "TNVTdTSPSShZokMfXRo82TP2Kq6Fc2nhMNmF7";
        Long amount = 20000l;

        List<UTXOData> accountUTXOs = btcWalletApi.getAccountUTXOs(from);
        List<UTXO> utxos = new ArrayList<>();
        for (UTXOData data : accountUTXOs) {
            UTXO utxo = new UTXO(Sha256Hash.wrap(data.getTxid()),
                    data.getVout(),
                    Coin.valueOf(data.getAmount().longValue()),
                    0,
                    false,
                    null);
            utxos.add(utxo);
        }
        List<byte[]> opReturns = new ArrayList<>();
        if (true) {
            String feeTo = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
            Long fee = 0l;
            RechargeData rechargeData = new RechargeData();
            rechargeData.setTo(AddressTool.getAddress(nerveTo));
            rechargeData.setValue(amount);
            if (fee.longValue() > 0) {
                amount += fee;
                rechargeData.setFeeTo(AddressTool.getAddress(feeTo));
            }
            byte[] opReturnBytes = rechargeData.serialize();
            int length = opReturnBytes.length;
            System.out.println(String.format("opReturnBytes size: %s", length));
            for (int i = 1; ; i++) {
                if (length > i * 80) {
                    byte[] result = new byte[80];
                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, 80);
                    opReturns.add(result);
                } else {
                    int len = length - (i - 1) * 80;
                    byte[] result = new byte[len];
                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, len);
                    opReturns.add(result);
                    break;
                }
            }
        }

        Transaction tx = sendTransactionOffline(fromPriKey996, to, from, amount, utxos, opReturns, 1, false);
        System.out.println("txId: " + tx.getTxId());
        broadcast(tx, opReturns.size());
    }

    @Test
    public void calcFeeTest() throws Exception {
        setTestnet();
        String hash = "e59f6e6a4966ce4f0a7cf1ec8a48178736122e4f1729e486a03081d0ebde3112";
        RawTransaction tx = btcWalletApi.getTransactionByHash(hash);
        long txFee = calcTxFee(tx, btcWalletApi);
        System.out.println(txFee);
    }

    @Test
    public void testCalcFeeMultiSignWithSplitGranularity() {
        // long fromTotal, long transfer, long feeRate, Long splitGranularity, int inputNum, int opReturnBytesLen, int m, int n
        long size = BitCoinLibUtil.calcFeeMultiSignSizeP2WSHWithSplitGranularity(
                2537, 1810, 1, 0L, 1, new int[]{32}, 2, 3);
        System.out.println(size);
        // int inputNum, int outputNum, int opReturnBytesLen, int m, int n
        size = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(1, 1, new int[]{32}, 2, 3);
        System.out.println(size);
        size = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(1, 0, new int[]{32}, 2, 3);
        System.out.println(size);
    }

    @Test
    public void calcFeeWithSplitChangeTest() {
        long fromTotal = 200000;
        long transfer = 20000;
        long feeRate = 15;
        long splitGranularity = 172000;
        int inputNum = 1;
        int[] opReturns = new int[]{32};
        int m = 10, n = 15;
        long f1 = calcFeeMultiSignSizeP2WSH(inputNum, 1, opReturns, m, n);
        int splitNum = calcSplitNumP2WSH(fromTotal, transfer, feeRate, splitGranularity, inputNum, opReturns, m, n);
        long fSplitNum = calcFeeMultiSignSizeP2WSH(inputNum, splitNum, opReturns, m, n);
        long change = fromTotal - transfer - fSplitNum * feeRate;
        System.out.println(String.format("f1: %s", f1));
        System.out.println(String.format("splitNum: %s", splitNum));
        System.out.println(String.format("fSplitNum: %s", fSplitNum));
        System.out.println(String.format("fee: %s", fSplitNum * feeRate));
        System.out.println(String.format("change: %s", change));

    }
    @Test
    public void calcFeeMultiSignSizeP2WSHTest() throws Exception {
        String btcDollars = "66041.26063801";
        long feeRate = 33;
        //System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(1, 1, new int[]{32}, 2, 3)));
        //System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(2, 1, new int[]{32}, 2, 3)));
        //System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(3, 1, new int[]{32}, 2, 3)));
        System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(1, 1, new int[]{32}, 10, 15)));
        System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(2, 1, new int[]{32}, 10, 15)));
        //System.out.println(this.calcFeeDollars(btcDollars, feeRate, calcFeeMultiSignSizeP2WSH(3, 1, new int[]{32}, 10, 15)));
    }

    String calcFeeDollars(String btcDollars, long feeRate, long txSize) {
        BigDecimal btcCost = BigDecimal.ONE.multiply(BigDecimal.valueOf(feeRate)).multiply(BigDecimal.valueOf(txSize)).movePointLeft(8);
        System.out.print(String.format("txSize: %s, btcCost: %s, FeeDollars: ", txSize, btcCost));
        return new BigDecimal(btcDollars).multiply(btcCost).stripTrailingZeros().toPlainString();
    }

    public static Transaction makeNativeSegwitMultiSignTx(List<ECKey> privKeys,
                                                          List<ECKey> pubEcKeys,
                                                          long amount,
                                                          String receiveAddr,
                                                          List<UTXO> utxos,
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
        List<ECKey> pubKeys = new ArrayList<>(pubEcKeys);
        Collections.sort(pubKeys, ECKey.PUBKEY_COMPARATOR);

        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(m, pubKeys);
        Script scriptPubKey = ScriptBuilder.createP2WSHOutputScript(redeemScript);
        Address multiSigAddress = scriptPubKey.getToAddress(network);
        System.out.println("multiSigAddress = " + multiSigAddress.toString());

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
        boolean enoughUTXO = false;
        for (int k = 0; k < utxos.size(); k++) {
            UTXO utxo = utxos.get(k);
            usingUtxos.add(utxo);
            totalMoney += utxo.getValue().value;
            long feeSize = calcFeeMultiSignSizeP2WSH(usingUtxos.size(), 1, opReturnSize, m, n);
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
            throw new RuntimeException("not enough utxo");
        }
        //Set tx fee
        Coin txFee = Coin.valueOf(fee);

        Transaction spendTx = new Transaction();
        spendTx.setVersion(2);
        spendTx.addOutput(Coin.valueOf(amount), outputScript);
        // If you need to change, the total amount of the consumption list - the amount that has been transferred - the handling fee
        Address fromAddress = multiSigAddress;
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
            UTXO usingUtxo = usingUtxos.get(i);
            //add input
            TransactionOutPoint outPoint = new TransactionOutPoint(usingUtxo.getIndex(), usingUtxo.getHash());
            TransactionInput unsignedInput = new TransactionInput(spendTx, new byte[0], outPoint, usingUtxo.getValue());
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }
        //System.out.println("redeemScript sha256hash: " + HexUtil.encode(Sha256Hash.hash(redeemScript.program())));
        //Script witnessScript = new ScriptBuilder().data(new byte[]{0x00, 0x20}).data(Sha256Hash.hash(redeemScript.program())).build();

        List<ECKey> _privKeys = new ArrayList<>(privKeys);
        Collections.sort(_privKeys, ECKey.PUBKEY_COMPARATOR);

        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            TransactionSignature[] txSigs = new TransactionSignature[m];
            //Sha256Hash sighash = spendTx.hashForSignature(input.getIndex(), redeemScript, Transaction.SigHash.ALL, false);
            Sha256Hash sighash = spendTx.hashForWitnessSignature(input.getIndex(), redeemScript.program(), input.getValue(), Transaction.SigHash.ALL, false);

            for (int i = 0; i < m; i++) {
                ECKey ecKey = _privKeys.get(i);
                ECKey.ECDSASignature ecdsaSignature = ecKey.sign(sighash);
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);
                txSigs[i] = signature;
                //System.out.println(String.format("i: %s, pri: %s, signature: %s", i, ecKey.getPrivateKeyAsHex(), HexUtil.encode(ecdsaSignature.encodeToDER())));
            }

            //Build tx input script
            input.setWitness(TransactionWitness.redeemP2WSH(redeemScript, txSigs));
        });

        Transaction.verify(networkParameters, spendTx);

        Context.propagate(new Context());
        spendTx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        spendTx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        //System.out.println("spendTx: " + spendTx);
        System.out.println("Tx hex: " + ByteUtils.formatHex(spendTx.serialize()));
        return spendTx;
    }

    @Test
    public void calcMultiSignTxSizeTest() {
        long size = calcFeeMultiSignSize(10, 1, new int[0], 2, 3);
        System.out.println(size);
        //long feeRate = 43l;
        //String btcPrice = "66666.66666";
        //System.out.println(String.format("usd: %s", BigDecimal.valueOf(size).multiply(BigDecimal.valueOf(feeRate)).movePointLeft(8).multiply(new BigDecimal(btcPrice))));

    }

    @Test
    public void createOutputScriptTest() {
        Script script = ScriptBuilder.createP2PKHOutputScript(ecKey996);
        System.out.println(HexUtil.encode(script.program()));
    }

    @Test
    public void convertAddr() {
        System.out.println(calcBtcPriByEvmPri("c48f55dbe619e83502be1f72c875ed616aeaab6108196f0d644d72e992f6a155", false));
        System.out.println(calcBtcPriByEvmPri("30002e81d449f16b69bc3e06918ff6ff088863edef8a0ba3d9b06fe5d02744d7", false));
    }

    @Test
    public void getBestBlockHeight() throws Exception {
        setMain();
        System.out.println(btcWalletApi.getBestBlockHeight());
    }

    @Test
    public void getRawTransactionTest() throws Exception {
        setTestnet();
        System.out.println(client.getRawTransaction("411e120f013165f5a23bca90e57bf2e5f8fb40b9f138b3f388b35d0a3160103f"));
    }

    @Test
    public void transferTest1() throws Exception {
        setTestnet();
        // This transaction is used to test proposal recharge and reissue
        // cc0866e50e135f1daccdfe50d6df432c860ad08c5088c21eb4536f0b821969fc - Wrong recharge amount
        // 21be6dbbc71ef9c2aaf638138bf0d1c7ff38a9cad71c7838f17839086818b688 - Wrong recharge address
        // d2d4ae1323ce8ff910e104a9af35c5379cdd90c0cda6e46436344c73a6a482cc - Wrong fee address
        Script script = ScriptBuilder.createP2PKHOutputScript(ecKey996);
        List<UTXO> utxos = new ArrayList<>();
        utxos.add(new UTXO(Sha256Hash.wrap("4cda7fec079643a500e39fff440fba52901e6d9ba5d6c2637b36c4e4e8e33bbc"),
                1,Coin.valueOf(362674),
                0,false,script));
        Long amount = 199822l;
        List<byte[]> opReturns = new ArrayList<>();
        if (false) {
            String feeTo = "TNVTdTSPRnXkDiagy7enti1KL75NU5AxC9sQA";
            Long fee = 0l;
            RechargeData rechargeData = new RechargeData();
            rechargeData.setTo(AddressTool.getAddress("TNVTdTSPJJMGh7ijUGDqVZyucbeN1z4jqb1ad"));
            rechargeData.setValue(amount);
            rechargeData.setExtend0("2024-04-07 15:27");
            if (fee.longValue() > 0) {
                amount += fee;
                rechargeData.setFeeTo(AddressTool.getAddress(feeTo));
            }
            byte[] opReturnBytes = rechargeData.serialize();
            int length = opReturnBytes.length;
            System.out.println(String.format("opReturnBytes size: %s", length));
            for (int i = 1; ; i++) {
                if (length > i * 80) {
                    byte[] result = new byte[80];
                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, 80);
                    opReturns.add(result);
                } else {
                    int len = length - (i - 1) * 80;
                    byte[] result = new byte[len];
                    System.arraycopy(opReturnBytes, (i - 1) * 80, result, 0, len);
                    opReturns.add(result);
                    break;
                }
            }
        }

        String from = legacyAddress996;
        String to = "mqYkDJboJGMa7XJjrVm3pDxYwB6icxTQrW";
        Transaction tx = sendTransactionOffline(fromPriKey996, to, from, amount, utxos, opReturns, 150, false, false);
        System.out.println("txHex: " + HexUtil.encode(tx.serialize()));
        System.out.println("tx vSize: " + tx.getVsize());
        System.out.println("txId: " + tx.getTxId());
        broadcast(tx, opReturns.size());
    }

    void broadcast(Transaction _tx, int opReturnSize) {
        try {
            if (opReturnSize < 2) {
                String hash = client.sendRawTransaction(HexUtil.encode(_tx.serialize()));
                System.out.println("broadcast txId: " + hash);
                return;
            }
            List<PeerNode> peerInfo = client.getPeerInfo();

            // Broadcast tx
            PeerGroup pGroup = new PeerGroup(TESTNET);
            int minConnections = 3;
            pGroup.startAsync();
            peerInfo.stream().forEach(p -> System.out.println(p.getAddr()));
            peerInfo.stream().forEach(p -> {
                try {
                    if (!p.getAddr().startsWith("[")) {
                        pGroup.addAddress(InetAddress.getByName(p.getAddr().split(":")[0]));
                    }
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            });
            var txResult = pGroup.broadcastTransaction(_tx, minConnections, true).broadcastOnly().get();
            System.out.println("txId: " + _tx.getTxId() + "|awaitSentDone: " + txResult.awaitSent().isDone() + "|awaitRelayedDone: " + txResult.awaitRelayed().isDone());

        } catch (ScriptException ex) {
            throw new RuntimeException(ex);
        } catch (VerificationException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        } catch (BitcoindException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void multiTransferTest1() throws Exception {
        setTestnet();
        List<UTXOData> accountUTXOs = btcWalletApi.getAccountUTXOs(p2shMultisigAddress);

        List<UTXO> utxos = accountUTXOs.stream().map(u -> new UTXO(Sha256Hash.wrap(u.getTxid()),
                u.getVout(),
                Coin.valueOf(u.getAmount().longValue()),
                0,
                false,
                null)).collect(Collectors.toList());
        long total = 0;
        for (UTXO u : utxos) {
            total += u.getValue().value;
        }
        long feeSize = calcFeeMultiSignSize(utxos.size(), 1, new int[0], 2, 3);
        long feeRate = 2;
        long fee = feeSize * feeRate;

        List<byte[]> opReturns = List.of();
        String to = "tb1qtskq8773jlhjqm7ad6a8kxhxleznp0nech0wpk0nxt45khuy0vmqwzeumf";
        Transaction tx = testMultiSigTx(priEcKeys, pubEcKeys, total - fee, to, utxos, opReturns, 2, 3, feeRate, false, true);
        //broadcast(tx, opReturns.size());
    }

    @Test
    public void multiTransferFor10Of15Test() throws Exception {
        setTestnet();
        // MultiSigAddr: 2N2EfstdVXkxSzxomSGbvwVvKJhvvSkveby
        //Script script = Script.parse(HexUtil.decode("a9140fdd64eb286106a529e5a3276abb43cf0a5fc84687"));
        Script script = Script.parse(HexUtil.decode("a914629e53e9fbf2329531f12cfc892c962010c29e1d87"));
        List<UTXO> utxos = new ArrayList<>();
        UTXO utxo = new UTXO(Sha256Hash.wrap("137b3e1f35df6d9a7ab5fe344ac1c093b9ebd0d891a4cfe6b0b7b4cd4842cbb4"),
                0,
                Coin.valueOf(102003),
                0,
                false,
                script);

        utxos.add(utxo);
        //utxos.add(new UTXO(Sha256Hash.wrap("137b3e1f35df6d9a7ab5fe344ac1c093b9ebd0d891a4cfe6b0b7b4cd4842cbb5"),
        //        0,
        //        Coin.valueOf(102003),
        //        0,
        //        false,
        //        script));
        List<ECKey> ecKeys = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ecKeys.add(new ECKey());
        }

        List<byte[]> opReturns = List.of(
                "withdraw 581a896871161de3b879853cfef41b3bdbf69113ba66b0b9f699535f72a5ba68".getBytes(StandardCharsets.UTF_8)
        );
        String to = legacyAddress996;
        long feeRate = 2;
        Transaction tx = testMultiSigTx(ecKeys, ecKeys, 10122, to, utxos, opReturns, 10, 15, feeRate, false, false);
        //broadcast(tx, opReturns.size());
    }

    @Test
    public void multiTransferByOneTest() throws Exception {
        setTestnet();
        // MultiSigAddr: 2N2EfstdVXkxSzxomSGbvwVvKJhvvSkveby
        //Script script = Script.parse(HexUtil.decode("a9140fdd64eb286106a529e5a3276abb43cf0a5fc84687"));
        Script script = null;
        List<UTXOData> utxos = new ArrayList<>();
        UTXOData utxo = new UTXOData("a1438d0ec6d586ab0ac89a16dde29c71a8a8459f8cc48222ea5096c920fafcb9",
                0,
                BigInteger.valueOf(103008));

        utxos.add(utxo);
        utxos.add(new UTXOData("39804913a56a1bc7e97b48d77536c1d3913d8e2ee120244d33612dc77db775f1",
                0,
                BigInteger.valueOf(102102)));
        List<byte[]> opReturns = List.of(
                "withdraw 581a896871161de3b879853cfef41b3bdbf69113ba66b0b9f699535f72a5ba68".getBytes(StandardCharsets.UTF_8)
        );
        String to = "tb1qnwnk40t55dsgfd4nuz5aq8sflj8vanh5nskec5";
        long feeRate = 2;
        List<String> sign0 = createMultiSigTxByOne(priEcKeys.get(0), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, feeRate, false);
        System.out.println(Arrays.deepToString(sign0.toArray()));
        List<String> sign1 = createMultiSigTxByOne(priEcKeys.get(1), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, feeRate, false);
        System.out.println(Arrays.deepToString(sign1.toArray()));
        List<String> sign2 = createMultiSigTxByOne(priEcKeys.get(2), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, feeRate, false);
        System.out.println(Arrays.deepToString(sign2.toArray()));

        Transaction tx = createMultiSigTxByMulti(Map.of(pubEcKeys.get(0).getPublicKeyAsHex(), sign0, pubEcKeys.get(1).getPublicKeyAsHex(), sign1, pubEcKeys.get(2).getPublicKeyAsHex(), sign2), pubEcKeys, 121100 + 21122, to, utxos, opReturns, 3, 2, feeRate, false);
        broadcast(tx, opReturns.size());

    }

    @Test
    @Deprecated
    public void transferTest2() {
        boolean mainnet = false;
        NetworkParameters networkParameters = mainnet ? MainNetParams.get() : TestNet3Params.get();
        long amount = 9822;
        Address sourceAddress = Address.fromString(networkParameters, legacyAddress996);
        Address targetAddress = Address.fromString(networkParameters, legacyAddress996);
        Script script = ScriptBuilder.createEmpty();
        List<UTXO> utxos = new ArrayList<>();
        utxos.add(new UTXO(Sha256Hash.wrap("ac12a2a3c29de92933aeaab1d2eb10d66dbca46670a1dce8d61eda6f99b2379a"),
                0,Coin.valueOf(10211),
                0,false,script));
        utxos.add(new UTXO(Sha256Hash.wrap("666c1cd1bc495604d2ed98fa37b8ca5c9bafe5da7376da68c6e5f9debbff35f4"),
                0,Coin.valueOf(10211),
                0,false,script));
        utxos.add(new UTXO(Sha256Hash.wrap("a359e7ddf94b53a5065982feb6c544e7ad195664aa64740bfb535c31dadc73aa"),
                0,Coin.valueOf(2323),
                0,false,script));
        utxos.add(new UTXO(Sha256Hash.wrap("a04cb934b9a0b4447e6def763c3125dcd40977226630277818088c249a8f855e"),
                0,Coin.valueOf(12123),
                0,false,script));
        utxos.add(new UTXO(Sha256Hash.wrap("d8e0372066bf7695cb6381973691ce3a6c5523fcf80d3232203fa0ac802d2501"),
                1,Coin.valueOf(81019),
                0,false,script));
        BigInteger privkey = new BigInteger(1, HexUtil.decode(fromPriKey996));
        org.bitcoinj.crypto.ECKey ecKey = org.bitcoinj.crypto.ECKey.fromPrivate(privkey);
        Transaction tx = new Transaction(networkParameters);
        tx.addOutput(Coin.valueOf(amount), targetAddress);
        addInputsToTransaction(sourceAddress, tx, utxos, amount);
        signInputsOfTransaction(sourceAddress, tx, ecKey);

        Transaction.verify(networkParameters, tx);

        new Context();
        tx.getConfidence().setSource(TransactionConfidence.Source.SELF);
        tx.setPurpose(Transaction.Purpose.USER_PAYMENT);
        String valueToSend = HexUtil.encode(tx.serialize());
        System.out.println("transferTest2: " + valueToSend);
        broadcast(tx, 0);
    }

    private void addInputsToTransaction(Address sourceAddress, Transaction tx, List<UTXO> unspents, Long amount) {
        long gatheredAmount = 0L;
        long requiredAmount = amount + 20700;
        for (UTXO unspent : unspents) {
            gatheredAmount += unspent.getValue().value;
            TransactionOutPoint outPoint = new TransactionOutPoint(unspent.getIndex(), unspent.getHash());
            TransactionInput transactionInput = new TransactionInput(tx, ScriptBuilder.createEmpty().program(),
                    outPoint, Coin.valueOf(unspent.getValue().value));
            tx.addInput(transactionInput);

            if (gatheredAmount >= requiredAmount) {
                //break;
            }
        }
        if (gatheredAmount > requiredAmount) {
            //return change to sender, in real life it should use different address
            tx.addOutput(Coin.valueOf((gatheredAmount - requiredAmount)), sourceAddress);
        }
    }

    private void signInputsOfTransaction(Address sourceAddress, Transaction tx, org.bitcoinj.crypto.ECKey key) {
        for (int i = 0; i < tx.getInputs().size(); i++) {
            Script scriptPubKey = ScriptBuilder.createOutputScript(sourceAddress);
            Sha256Hash hash = tx.hashForSignature(i, scriptPubKey, Transaction.SigHash.ALL, true);
            org.bitcoinj.crypto.ECKey.ECDSASignature ecdsaSignature = key.sign(hash);
            TransactionSignature txSignature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, true);

            if (ScriptPattern.isP2PK(scriptPubKey)) {
                tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature));
            } else {
                if (!ScriptPattern.isP2PKH(scriptPubKey)) {
                    throw new ScriptException(ScriptError.SCRIPT_ERR_UNKNOWN_ERROR, "Unable to sign this scrptPubKey: " + scriptPubKey);
                }
                tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSignature, key));
            }
        }
    }

    public static Transaction testMultiSigTx(List<ECKey> privKeys,
                                             List<ECKey> pubKeys,
                                             long amount,
                                             String receiveAddr,
                                             List<UTXO> utxos,
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
        System.out.println("multiSigAddress = " + multiSigAddress);

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
        boolean enoughUTXO = false;
        for (int k = 0; k < utxos.size(); k++) {
            usingUtxos.add(utxos.get(k));
            totalMoney += utxos.get(k).getValue().value;
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
            unsignedInput.setSequenceNumber(NO_SEQUENCE - 2);
            spendTx.addInput(unsignedInput);
        }

        List<ECKey> _privKeys = new ArrayList<>(privKeys);
        Collections.sort(_privKeys, ECKey.PUBKEY_COMPARATOR);
        //Sign tx, will fall if no private keys specified
        List<TransactionInput> spendTxInputs = spendTx.getInputs();
        spendTxInputs.iterator().forEachRemaining(input -> {
            ArrayList<TransactionSignature> txSigs = new ArrayList<>();
            Sha256Hash sighash = spendTx.hashForSignature(input.getIndex(), redeemScript, Transaction.SigHash.ALL, false);

            for (int i = 0; i < m; i++) {
                ECKey ecKey = _privKeys.get(i);
                ECKey.ECDSASignature ecdsaSignature = ecKey.sign(sighash);
                TransactionSignature signature = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);
                txSigs.add(signature);
                //System.out.println(String.format("i: %s, pri: %s, signature: %s", i, ecKey.getPrivateKeyAsHex(), HexUtil.encode(ecdsaSignature.encodeToDER())));
            }

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
        return spendTx;
    }

    /**
     * Introduction test for mnemonic words
     */
    @Test
    public void importByMnemonicest() throws Exception {
        String mnemonic = "deny they health custom company worth tank hungry police direct eternal urban";
        String password = "";
        System.out.println(JSONUtils.obj2PrettyJson(importByMnemonic("m/44'/60'/0'/0/0", Arrays.asList(mnemonic.split(" ")), password)));
    }

    /**
     * Importing wallets through mnemonics
     *
     * @param path      Mnemonic path  What protocol does the user provide for generating
     * @param mnemonics Mnemonic words
     * @param password  password
     * @return
     */
    private Map importByMnemonic(String path, List<String> mnemonics, String password) {
        //There are regulations regarding the protocol and path, and it is also necessary to verify them here
        if (!path.startsWith("m") && !path.startsWith("M")) {
            throw new RuntimeException("Please enter the correct path");
        }

        String[] pathArray = path.split("/");
        long creationTimeSeconds = System.currentTimeMillis() / 1000;
        //The main difference here is that it will be different. Originally, it was randomly generated, but this time we will replace it with user mnemonic word construction
        DeterministicSeed ds = new DeterministicSeed(mnemonics, null, "", creationTimeSeconds);
        //Root private key
        byte[] seedBytes = ds.getSeedBytes();
        System.out.println("seedBytes: " + Numeric.toHexString(seedBytes));
        //Mnemonic words
        List<String> mnemonic = ds.getMnemonicCode();
        try {
            //Mnemonic seed
            byte[] mnemonicSeedBytes = MnemonicCode.INSTANCE.toEntropy(mnemonic);
            ECKeyPair mnemonicKeyPair = ECKeyPair.create(mnemonicSeedBytes);
            WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, mnemonicKeyPair);
            ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
            //Save thiskeystore Delete after use
            String jsonStr = objectMapper.writeValueAsString(walletFile);
            //validate
            WalletFile checkWalletFile = objectMapper.readValue(jsonStr, WalletFile.class);
            ECKeyPair ecKeyPair = org.web3j.crypto.Wallet.decrypt(password, checkWalletFile);
            byte[] checkMnemonicSeedBytes = Numeric.hexStringToByteArray(ecKeyPair.getPrivateKey().toString(16));
            List<String> checkMnemonic = MnemonicCode.INSTANCE.toMnemonic(checkMnemonicSeedBytes);
        } catch (MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException |
                 CipherException | IOException e) {
            Log.error("Account generation exception", e);
        }
        if (seedBytes == null) {
            return null;
        }
        DeterministicKey dkKey = HDKeyDerivation.createMasterPrivateKey(seedBytes);
        for (int i = 1; i < pathArray.length; i++) {
            ChildNumber childNumber;
            if (pathArray[i].endsWith("'")) {
                int number = Integer.parseInt(pathArray[i].substring(0,
                        pathArray[i].length() - 1));
                childNumber = new ChildNumber(number, true);
            } else {
                int number = Integer.parseInt(pathArray[i]);
                childNumber = new ChildNumber(number, false);
            }
            dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber);
        }
        ECKeyPair keyPair = ECKeyPair.create(dkKey.getPrivKeyBytes());
        String privateKey = keyPair.getPrivateKey().toString(16);
        String publicKey = keyPair.getPublicKey().toString(16);
        String address = "0x" + Keys.getAddress(publicKey);
        Map resultMap = new LinkedHashMap();
        resultMap.put("mnemonic", mnemonic);
        resultMap.put("privateKey", privateKey);
        resultMap.put("publicKey", publicKey);
        resultMap.put("address", address);
        return resultMap;
    }

    @Test
    public void testBigInOrder() {
        List<UTXOData> utxos = new ArrayList<>();
        utxos.add(new UTXOData("a", 0, BigInteger.valueOf(1)));
        utxos.add(new UTXOData("d", 0, BigInteger.valueOf(4)));
        utxos.add(new UTXOData("e", 0, BigInteger.valueOf(5)));
        utxos.add(new UTXOData("c", 0, BigInteger.valueOf(3)));
        utxos.add(new UTXOData("f", 0, BigInteger.valueOf(2)));
        utxos.add(new UTXOData("g", 0, BigInteger.valueOf(2)));
        utxos.add(new UTXOData("b", 0, BigInteger.valueOf(2)));
        utxos.sort(new Comparator<UTXOData>() {
            @Override
            public int compare(UTXOData o1, UTXOData o2) {
                // order asc
                int compare = o1.getAmount().compareTo(o2.getAmount());
                if (compare == 0) {
                    return o1.getTxid().compareTo(o2.getTxid());
                }
                return compare;
            }
        });
        utxos.forEach(u -> System.out.println(u.getTxid() + "-" + u.getAmount()));
        BigInteger a = new BigInteger("-123");
        System.out.println(new BigInteger(a.toByteArray()));
    }

    @Test
    public void calcManagers() {
        List<String> outs = new ArrayList<>();
        List<String[]> ins = new ArrayList<>();

        List<String> oldManagerList = new ArrayList<>();
        oldManagerList.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b,NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA");
        oldManagerList.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9,NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE");
        oldManagerList.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4,NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD");
        oldManagerList.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292,NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB");
        oldManagerList.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d,NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB");
        oldManagerList.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd,NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49");
        oldManagerList.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03,NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9");
        oldManagerList.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0,NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC");
        oldManagerList.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7,NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb");
        oldManagerList.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049,NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64");
        oldManagerList.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21,NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63");
        oldManagerList.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9,NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx");
        oldManagerList.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90,NERVEepb6G71f2K3mPKrds8Be7KzdiCsM23Ewq");
        oldManagerList.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803,NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1");
        oldManagerList.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980,NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt");
        Set<String> oldManagerAddrSet = oldManagerList.stream().map(o -> o.split(",")[1]).collect(Collectors.toSet());

        List<String> currentNodeList = new ArrayList<>();
        currentNodeList.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803,NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1");
        currentNodeList.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049,NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64");
        currentNodeList.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292,NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB");
        currentNodeList.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21,NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63");
        currentNodeList.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4,NERVEepb69pdDv3gZEZtJEmahzsHiQE6CK4xRi");
        currentNodeList.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a,NERVEepb66oGcmJnrjX5AGzFjXJrLiErHMo1cn");
        currentNodeList.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9,NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE");
        currentNodeList.add("0369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c5,NERVEepb6CY4q799Pqk3dKuxZgU68Hn8WpPdEE");
        currentNodeList.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b,NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA");
        currentNodeList.add("020c60dd7e0016e174f7ba4fc0333052bade8c890849409de7b6f3d26f0ec64528,NERVEepb63mC8F4cwLRbkW3ATbZbtFChbRp2DD");
        currentNodeList.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d,NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB");
        currentNodeList.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90,NERVEepb6G71f2K3mPKrds8Be7KzdiCsM23Ewq");
        currentNodeList.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9,NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx");
        currentNodeList.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980,NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt");
        currentNodeList.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0,NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC");
        currentNodeList.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd,NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49");
        currentNodeList.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4,NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD");
        currentNodeList.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7,NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb");
        currentNodeList.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03,NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9");

        Set<String> currentNonManagerSet = new HashSet<>();
        currentNonManagerSet.add("NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB");
        currentNonManagerSet.add("NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64");
        currentNonManagerSet.add("NERVEepb63mC8F4cwLRbkW3ATbZbtFChbRp2DD");
        currentNonManagerSet.add("NERVEepb6CY4q799Pqk3dKuxZgU68Hn8WpPdEE");

        List<String[]> currentManagerList = new ArrayList<>();
        for (String currentNode : currentNodeList) {
            String[] split = currentNode.split(",");
            String addr = split[1];
            if (currentNonManagerSet.contains(addr)) {
                continue;
            }
            currentManagerList.add(split);
        }
        Set<String> currentManagerAddrSet = currentManagerList.stream().map(o -> o[1]).collect(Collectors.toSet());

        for (String old : oldManagerList) {
            String[] split = old.split(",");
            String addr = split[1];
            if (!currentManagerAddrSet.contains(addr)) {
                outs.add(old);
            }
        }
        for (String[] currentManager : currentManagerList) {
            String addr = currentManager[1];
            System.out.println("currentManagerPub: " + currentManager[0]);
            if (!oldManagerAddrSet.contains(addr)) {
                ins.add(currentManager);
            }
        }

        System.out.println("outs: " + Arrays.toString(outs.toArray()));
        System.out.println("ins: " + Arrays.deepToString(ins.toArray()));

        List<org.bitcoinj.crypto.ECKey> pubList1 = new ArrayList<>();
        for (String[] currentManager : currentManagerList) {
            String pub = currentManager[0];
            pubList1.add(org.bitcoinj.crypto.ECKey.fromPublicOnly(HexUtil.decode(pub)));
        }
        System.out.println(String.format("makeMultiAddr (%s of %s) for mainnet: %s, segwit: %s", 10, pubList1.size(), makeMultiAddr(pubList1, 10, true), getNativeSegwitMultiSignAddress(10, pubList1, true)));

    }

    Object[] baseDataSegWit() throws IOException {
        List<String> oldPubs = new ArrayList<>();
        oldPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        oldPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        oldPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        oldPubs.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292");
        oldPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        oldPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        oldPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        oldPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        oldPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        oldPubs.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
        oldPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        oldPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        oldPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        oldPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        oldPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");

        List<String> newPubs = new ArrayList<>();
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        //String toAddress = "bc1q7l4q8kqekyur4ak3tf4s2rr9rp4nhz6axejxjwrc3f28ywm4tl8smz5dpd";

        List<UTXOData> utxoDataList = new ArrayList<>();
        String utxoJson = "[{\"txid\":\"e4a6686663d9be42130d4153b890c50980e064be1733fc967f39aa8df796f7f1\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":920000},{\"txid\":\"c9f69c4923312e5c316363caec9c33ed290f4eccbb0caaa6bef59e8e5f0e5275\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834519,\"block_hash\":\"00000000000000000002bb94d4d4273e054bdcb716cd989f078728398ef2fbec\",\"block_time\":1710339804},\"value\":4085000},{\"txid\":\"e1c4d372a1b8ba002e39f995b547ca1d2013c33c3dcb3f779a172d64519bf81a\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834507,\"block_hash\":\"000000000000000000017c367acd384aa572a5e10096a9f2de2918b7e3d3f9fc\",\"block_time\":1710333388},\"value\":2060022},{\"txid\":\"a63ab74e05d9c3e2ed2bd3730839bdcab75d0ce2f9e6f7ecf023fd88e287ea40\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834444,\"block_hash\":\"00000000000000000000918db8877a287680c10f431297d4e8a3ac7ba6eb6b8b\",\"block_time\":1710298985},\"value\":500000},{\"txid\":\"011bf09611dc02eb0cad2f05cce17b24c510f3a81b898ada2c82862cb505d44b\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834503,\"block_hash\":\"00000000000000000002d0a420e689635927aed42faa1fbca12459b1c445c65c\",\"block_time\":1710329124},\"value\":40000},{\"txid\":\"066f496b23abb51cb0475721470ad087c6d5bbcb068e15e3f2fcb704ed89327a\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834506,\"block_hash\":\"00000000000000000000e11cf7318981dab3a9e0b2a62299ac830472552be99c\",\"block_time\":1710332872},\"value\":20000},{\"txid\":\"bc2582dc8c43b37cfa377175d7be4dbac8d1a02146dabcccc60881e3d2cc4996\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834507,\"block_hash\":\"000000000000000000017c367acd384aa572a5e10096a9f2de2918b7e3d3f9fc\",\"block_time\":1710333388},\"value\":2060022},{\"txid\":\"1e2d5ddf16cc9ba248b5307d35b828311020423e3f02a84a61f171ca681ca6aa\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834340,\"block_hash\":\"000000000000000000000192b082f43fc989cfb48e37a3c2c292a7771dac138c\",\"block_time\":1710237696},\"value\":930000},{\"txid\":\"794d80b28aa22fd51d2b26401d4347857f435d29d20e1bb9838561c68f1c4ed4\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834514,\"block_hash\":\"0000000000000000000071923ef5a18dc17d47717fae386296dfe8902414ea71\",\"block_time\":1710337707},\"value\":1000000},{\"txid\":\"48f2a0e778c99e7a6dd5653756783a2c3eb2d691d497cc3a74c2bc5a68aeadef\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":4400000},{\"txid\":\"9c6b1df9180238583f9305816cbf102c2d5dcdb4401cd73393f8e109e0a39849\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":836186,\"block_hash\":\"000000000000000000021ae9eabc57563f19d495089af67151e3bb8fa2de1047\",\"block_time\":1711333743},\"value\":5000000},{\"txid\":\"de55fbb329ffd86c42b128b063f4822a79bb3c820e0e42b9f1adaf1b53749375\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834474,\"block_hash\":\"00000000000000000000aa6fe2c4ac729585285d2eb35e9b05959e4d19e4f2ea\",\"block_time\":1710311926},\"value\":2500000},{\"txid\":\"386a2e41ad8290c911cf287305b5b80143c2e201e41526f400f885173bb24fa4\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":835341,\"block_hash\":\"000000000000000000015196ea61074d4acadb829207329519116c5a1f14aa86\",\"block_time\":1710843998},\"value\":2000000},{\"txid\":\"fbac062373464cf0e9fbbd0651e8cb35e368225d2a50efac098f084c7bfe4809\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834284,\"block_hash\":\"0000000000000000000026bac4bd7157cf4b880133b130f47362e6b0c132a091\",\"block_time\":1710200443},\"value\":100000},{\"txid\":\"10f9d9ee4a258e86c9a4c52b77ea9ebdffd3b47789bb9933a9223112cd56f98f\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":15000}]";
        List<Map> utxoList = JSONUtils.json2list(utxoJson, Map.class);
        for (Map map : utxoList) {
            UTXOData utxoData = new UTXOData();
            utxoData.setTxid(map.get("txid").toString());
            utxoData.setVout(Integer.parseInt(map.get("vout").toString()));
            utxoData.setAmount(new BigInteger(map.get("value").toString()));
            utxoDataList.add(utxoData);
        }
        WithdrawalUTXO w = new WithdrawalUTXO();
        w.setHtgChainId(201);
        w.setFeeRate(15);
        w.setPubs(newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList()));
        w.setUtxoDataList(utxoDataList);
        List<UTXOData> UTXOList = w.getUtxoDataList();
        // take pubkeys of all managers
        List<ECKey> newPubEcKeys = w.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        List<ECKey> oldPubEcKeys = oldPubs.stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());
        String toAddress = BitCoinLibUtil.getNativeSegwitMultiSignAddress(10, newPubEcKeys, mainnet);
        // calc the min number of signatures
        int n = oldPubEcKeys.size(), m = 10;
        long fee = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[0], m, n) * w.getFeeRate();
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        List<ECKey> currentPubs = oldPubEcKeys;
        long amount = totalMoney - fee;
        List<byte[]> opReturns = Collections.EMPTY_LIST;
        return new Object[]{currentPubs, amount, toAddress, UTXOList, opReturns, m, n, w.getFeeRate()};
    }

    Object[] baseDataSegWitForWithdraw() throws IOException {

        List<String> newPubs = new ArrayList<>();
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");

        long feeRate = 30;
        String nerveTxHash = "94f1a89e5bf1df196940585c3d312432e00a00a1627ca3af159abd058531fe5d";
        String utxoJson = "[{\"txid\":\"3889ab98bc1be859d706bb9fc1246a10ea5f816c0cb4e764f18d2d91fe24ec47\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":847495,\"block_hash\":\"00000000000000000001f4cd5f8014ebc56a759a400beedaf76277b37bdbd1d9\",\"block_time\":1718116024},\"value\":25083859}]";
        List<UTXOData> utxoDataList = new ArrayList<>();
        List<Map> utxoList = JSONUtils.json2list(utxoJson, Map.class);
        for (Map map : utxoList) {
            UTXOData utxoData = new UTXOData();
            utxoData.setTxid(map.get("txid").toString());
            utxoData.setVout(Integer.parseInt(map.get("vout").toString()));
            utxoData.setAmount(new BigInteger(map.get("value").toString()));
            utxoDataList.add(utxoData);
        }

        WithdrawalUTXO w = new WithdrawalUTXO();
        w.setHtgChainId(201);
        w.setFeeRate(feeRate);
        w.setPubs(newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList()));
        w.setUtxoDataList(utxoDataList);
        List<UTXOData> UTXOList = w.getUtxoDataList();
        // take pubkeys of all managers
        List<ECKey> newPubEcKeys = w.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        String toAddress = "bc1q7l4q8kqekyur4ak3tf4s2rr9rp4nhz6axejxjwrc3f28ywm4tl8smz5dpd";
        // calc the min number of signatures
        int n = newPubEcKeys.size(), m = 10;
        long fee = BitCoinLibUtil.calcFeeMultiSignSizeP2WSH(UTXOList.size(), 1, new int[HexUtil.decode(nerveTxHash).length], m, n) * w.getFeeRate();
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        List<ECKey> currentPubs = newPubEcKeys;
        long amount = totalMoney - fee;
        List<byte[]> opReturns = List.of(HexUtil.decode(nerveTxHash));
        return new Object[]{currentPubs, amount, toAddress, UTXOList, opReturns, m, n, w.getFeeRate()};
    }

    @Test
    public void signDataForSegWitMultiTransferTest() throws Exception {
        setMain();
        List<String> priList = new ArrayList<>();
        priList.add("ae22c8.....");
        priList.add("a2edb5.....");
        priList.add("929732.....");
        priList.add("8c232c.....");
        priList.add("893771.....");

        Object[] baseData = this.baseDataSegWitForWithdraw();
        int i = 0;
        List<ECKey> currentPubs = (List<ECKey>) baseData[i++];
        long amount = (long) baseData[i++];
        String toAddress = (String) baseData[i++];
        List<UTXOData> UTXOList = (List<UTXOData>) baseData[i++];
        List<byte[]> opReturns = (List<byte[]>) baseData[i++];
        int m = (int) baseData[i++];
        int n = (int) baseData[i++];
        long feeRate = (long) baseData[i];

        List<String> signDataList = new ArrayList<>();
        for (String priStr : priList) {
            ECKey pri = ECKey.fromPrivate(HexUtil.decode(priStr));
            List<String> signatures = BitCoinLibUtil.createNativeSegwitMultiSignByOne(
                    pri,
                    currentPubs,
                    amount,
                    toAddress,
                    UTXOList,
                    opReturns,
                    m,
                    n,
                    feeRate,
                    mainnet,
                    true, null
            );
            byte[] signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            signDataList.add(HexUtil.encode(signData.serialize()));
        }
        System.out.println(signDataList);
    }

    @Test
    public void createSegwitMultiSignTxBySignData() throws Exception {
        setMain();
        Object[] baseData = this.baseDataSegWitForWithdraw();
        int i = 0;
        List<ECKey> currentPubs = (List<ECKey>) baseData[i++];
        long amount = (long) baseData[i++];
        String toAddress = (String) baseData[i++];
        List<UTXOData> UTXOList = (List<UTXOData>) baseData[i++];
        List<byte[]> opReturns = (List<byte[]>) baseData[i++];
        int m = (int) baseData[i++];
        int n = (int) baseData[i++];
        long feeRate = (long) baseData[i];

        String signatureData = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b01473045022100ab4df37e66989a1d97bfa47f1f8becf3a1796b9af97c436d20760ed41f5106aa02206d34df24d7f70c48b68116adab920f254071e871a627857bdd23c10841687503, 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d0146304402203144f96460efe2d88531665bd4a5a5a61ddac2e878cea75dc48430621cb377380220167a06f80f45afafad480309e90e4f7ba6f7305d5708bf4cddf301980942fac9, 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f001473045022100f55fb1eeefc7e33f70f3000e74008b5dce55887c26fd768fda5fe3a8328177b402201befa11064656d999b8a33462712264408dd0ddd4070b7cc8d23b9c43fa38296, 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030146304402202ebf998a9c0655145799c193ddd584136fcf932e79d6720ce00d641703c2483f022067054de1f1d93095f191dd82ed2908cc618249a2f91a414cc90abf00fb584bb8, 028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70146304402202ad1bb8df3d2a15d46f9ce175a8c6f28f56accc4ee6a82eb596402f272c8d78902205804cd92ecdee8dd66f3b422f222f2be1d4b9b755f5c7b2669b15fc0af6a0c4a,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd0146304402203d1276667da7c487a2c660dc198867c13a3b68f4dad5723be7c76d987ba9bc63022023c19a0ed030cb977c80295511960119468074b19e650c16e97198c25979a34e, 03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40146304402204f04fe67000d9a1b390d97af001c948db768f33eff50bf8687996fa0133c0f4502203e9b703c84ee81f57b22f85e92dbd2589ad963cfd5046a45f74fb8e0dcb58c22, 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c90147304502210084f164c22dd6dc13091600ce9fb680ca253f4e1de76a1335cef0f1be26eb7536022063db47adf685195c33ef2f253550cfe1336bfe230cbe38710fe6166a7a16276a, 035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b8030146304402200ecba3c1cf4bbd0834fdd6fcb42029f06d888a84f336851d989edb561a2401820220642c9daabef3691080b2617946defcacf9318c85e5a88ede02ce52ade8ad0fb0, 02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b901473045022100985322d8699a2a28a1cf97c201d6eafb75fe105749dc6c56eddcefbaf147526802201f5c3dad9888cc792bb3db7ad600a06137ee6b2633bd3081d6dbfaf3f32c9e6c";
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }
        Transaction tx = BitCoinLibUtil.createNativeSegwitMultiSignTx(
                signatures,
                currentPubs,
                amount,
                toAddress,
                UTXOList,
                opReturns,
                m,
                n,
                feeRate,
                mainnet, true, null);
        System.out.println("Tx hex: " + ByteUtils.formatHex(tx.serialize()));
    }

    @Test
    public void testParseSignData() throws Exception {
        String signDataArray = "03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c901473045022100b88093bd47cca341d48b63702527f13f6f43ef023c32b2177a1442f9dff552830220740a257c5a991142aa1771b7927fbc3d169a6a494cef15397b5ef3b10906a502,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4014730450221009eb1a00d9730c7947cc31fb499eb020a56a6743f5a649d95fef0ed42240758400220560ee6b39f7382e45085f5d42eefcdedd1e6520c5f5f09ec17f19d0e6b7f8359,039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d98001473045022100e37755daf2c7014021c6ddb550325134adfb94c3188885ef5f8e6c4ea9ba425902206aed07e07d6238b8f2333ad388d0d403a640c64e9018b5921f1bafb88c7b066b,03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a01463044022053720de31ee6678eade1e852b3ab06c304e20617695e315a5de8825141bbd8c20220417e3f31c0f6ef548999d0b3c5a9d5b5aabffda1fab846da9b102863d9fc3172,0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b014630440220135b5eb24c0ba9c66f74b3e5c091b6cca0db6fbecc31c6eb01923bc81d14399e022069281ab527fbc0e111b112b705114c59e2d934edded0b86146a51efdfe196fe0,028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b701473045022100cdd8b1cd0f797a4a0138ec3157b038c0c0cc542d7750fb5949b02a0cf3129ac202205c3169da6be4e431cbd35c962f2e6a1528f7b8647f897204175f2c701e015fec,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803014630440220134b1140c342b1c514009a097c73ac43bf1ad4340d0983b5ea130b7cc4626e6402206f83c5997690669ae3a7f4ff9eaa67953a0257f6248a09039826f27bde3b2b8c,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d014730450221008d5506637ba667c816717f2d32cdf62e9b747e3ea4bc70a2ba9febe3d5ac9c31022078d1377316ef1bc6cd1912b56568ca33b92c6fd2b826fcdfa6031a6745b44fcb,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd01463044022002b362f3bd88bca9918cf1676a89103c9847482dcd9945de1f2d65b8f3209997022005c7f82295db66ff68a1eeb15bdf0bddd54daf624d1028388f1372f62c645a7d,02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a0301473045022100de231b42fb551b10cb4a5e60af982592e90d5be3d3158ac9e1d6fe83182990420220418a10d3803390ef856f3931699acd94b51229c850208aa7a4127d96d497fb2a,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b901473045022100ab8f1a3cb0e5b780075fc720db68be592cae04c5098724259a5d96a9c261ea40022066c92bf809d1006fd964a5c0a19744210e7da3670a426c14c3d35449379b0a92,023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe900146304402206d7518f5ade44a47a0b1a903c13839a7d799e3be12073587e797f3b2c436b194022075c4d65ad6ba487db4619fe22ab8161a2e551e6a69811d37a6fcc9751a26c65d,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee210146304402200f7a213ab19dc8c159479d71a0958eb54331c636663f1817b9548cd01d71f34802201a6a4cd95ce0ba2bee1539b295243743a69192e35a48237fa65aa4ad64522632,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f00146304402204ec6a7854ae8a3b21dec9ddffc6a6e9868b7c5e6e1bc8bfa07effb22eec0c71902200f90b2af2d020757e7fae3ff18c8e5e67807c9c94de5c47e512dd7aabeaedc19,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b40146304402206d4bceadad89c9111e39376d1fb3d21882bca4ba84d01bcf501c75148c3b034502200fca13bc9642991dfb1690d210c89d1a22d4db74a61e5daa1a8d907e948af796";
        //String signDataArray = "02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030146304402204dc939c3378279963a5fb4e820ab0c57f3c586cdc81a89a45ab03b354322f00802201c9e75fc8114cf066be5df4f126f8f2f2ac134aa4f02432ec30591b982dbc6a8,028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70147304502210082b86f4ca7bff5f8b3f0a3406ca4bb2e1da9594b0d81f2c5ea6a93697c427b3a02200c6fbfb3616d1ec4ab26a34f3d72dcaf25ec881a3907c5ecf30e4c96663bf96f";
        //String signDataArray = "023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90024730450221009379f4e1706a8c5a0ce1d97775d67afe01d36bf03140358c30054a864de369780220300fe6fa0558a01431a48b4ecb3fa1b8ba9b7e45c913649c35601b02f79b92d8473045022100d56561c41545778146d9d825b43371787febe94124e8d31e25bae0601337b13d022053bb7a54fcc4b565cbb5865d6d21444fb73195e7a708f95be768a3356d1a4c6f,03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c902463044022032d76cb853634cb1be4f5b5bad747a3c2bf1b36159a8b69dd067baf9fa04a8a602202bba12d2622a171c24b95a303abc5952f37f54471b9d71dceeedc3fd656a411547304502210099dfc579e6627b95bb073a9a5719a6a32c504ed2d92d0017ddb313ce10ccf70302204fd278b390340bd34dc85712a0c0aae9028aa5d2a4b237074f91072ae21a2b5e,039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d98002463044022013e7093830404c711a1da42bafc81f064305a0db3f9e3d07c7ec1d36263c8d0102207ac53c71cccd5872e3ee26db83f7c4418135d22de9d8ace5f8f86e8f3977875f463044022047030e538f2b663cb0833c8768ee690081ab76d94c5eb503fd8c763e203a5f0a02201798bafbfd1711c15ea9255031aa14d0a27fd2eb76e4c182eacd27a682012da3,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b90246304402200dd5123cc0ea6d2ac11cc1083cbf5072b07b7386b5b508f9c4ef68e7e9a7ac9502204b2c8960bf2795f661f5399ad54a45152fb844d6567ab88bcde97f128d39a559463044022043f6a127d7396305d8021c9dc8bd7ca32deb55208f0da4df11e61c5d1d49be5d022064cb099e5d283a7e1b37a7d61429f9b4841e203f076388be63f66f1fcc0fc38c,02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c62920247304502210087ed15dfc81b9d57ace52d71ed6ad4889500c42155a8b776e1c630b2fb6b7b81022057b5559d539dad56fba2b357a7f6799eeea86455dace5792f0e6459677aaf0f846304402201f189d3ce1231269f53795ac96e7334dd144a7c023ee941339d710ad147364f5022008ab65b210127771fc61b2b2169b4301e6c7046a7a35a136b76cbe4b2058e5c8,0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0145304302201347c1cf5907baf52955ec31467d90b6be09648ec37002547dcb227c0ac86a81021f733206da06126fd5c0f7662f91a780056e06210f7c408448660f409ffb5d9f,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd02473045022100d36a0e35fd4033d6dce4a4cff941122abf09fef30d295b19517897ee0d52919302204b513a362cfacc589d88019d5715fa626f055ab02319aeeab0c67e9fb173062c473045022100b7ae29ad575b3ddc7b8b449c0bb346ae0a9352205fd4fa46b7658a5b1ecc0c7a02205f8c83d37fd0a6b01a3cadfbf46509c01e5507b0d1b550822dc1a2af45017afd,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee2102473045022100f56778fa55bccb55aea905bfd17f84563c304a8acd141f83c4e748a8834cfbb40220412c9b49d7e7c463fb23a56cf793e102408cdaa174e08808ddaa2ffb2d19bedc463044022005d4da631773e302619b2e5a4404fe151bbcff8f191989f90568dadff4ba12f4022054ed8c2a0e5d3ab0e9831896c399160275af330356de4fc61d3511ff8162e932,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df402463044022035ad577a53110b347abfc9ba4c190b82bf203a91472c1c83e67bff6da7b4d097022069eec4194c44f80f6f07911efa7aca9cf6528fda1f25d2f8d438cf65744577a6463044022076edb2d6309bba0274bce488c38e47386f5376af6790955fd83abdd6a6745c6602207766ffa9c7fb85283ba12097f52c9f698806875f239212717f6b42d32c3d7a67,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b80302463044022034c8b0cb909ee5ebc48b01ff9ed52309201761a0ffa5b6f3962f0924d3682fc80220443cb08cc7fcbaeaa2461256f57aa9f312e7dcfb796efd473e2be16bf427402b473045022100c0b69b44ccdcd39cc27299ca851b40be8d6ad421b1258c22f708f2b71ff6eeec022045b6da10ffbdab322d85cef692f422dc14443bf61f066ae8048a6d27fe81a60c";
        //String signDataArray = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0146304402200a483935202afcdaa7e7214f6374b328876c9b8f71cd7343de40fdc3ae86e2ad022078cd9be79fcba2c78755feb1ee6898e23cc597040f69653009d76f6625dfedf6,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f00247304502210090c54898dba12c021afe2d70bfa38fc9f6b4af5f994aed517ad973e60c45bc0e0220736d3626fc8b377ec4303a7abf20e082d6770d3e7a1f0f13d41b9bac82339019473045022100d8a51c537f93a51cc1965e8282b6048961e38ce52dc8769df11136878016380402204fa737348eecc12ac23e7f2e0a8ed2f8a1ebed0079da91094be28cea805dc90f,029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b402473045022100c339b37cde2c71ec840ce40c2db3e01989d7338689c78c13a04b3e04a35d855702203e4f3c06fc4d30a59273e4228040681308c1af489a294ba680c5183bd4df2029473045022100c73dab7fbed121a32ace9f4875b1739aa6a700c8946e13391e5bad28fc907bfe02202a90efd6120f84fabf7f61bf408fe73e70dda5419ffcaf60e4be698a195299ea,03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a02473045022100ab5f74d04952cc9ed34171523dd79283b2461a21b6f9b44d39382174300c62fc022071d172c5075fa268969fa88d4945cd9fb88d6302f25fcbaa32c582e3a3d1aa82473045022100c732593e2497659d071bb0df13258136da9b328eca8396a83007f21024e7118c022039b36e3071c75185deae745ce5cc72f2b7b0aa8d6d65edde75c7d835b27e6f23,03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4024630440220378bdfccb09ffd439049a929609646762085b383e20a6690f8ece50db510fcae02202446bf1a59ccb115aa67222cdc747e4c6a7ba9cd05bc952dc4cbe7bf90d4f9bb46304402207610e3166772c33624797d6a3c35efd86cabb56b9fb70ac557bda23af609512802201d57d2805969692c8c7458873efdee5688f0225c79f2929bf281b838abbb3bc7,039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d98002473045022100b5eab5c63a22cb7de802873b5ac8691ed40340464e751ee88796641c21fbe4f002203b59ca4f21ddd88c3d691f48059bf47cdd2e70d5c05ef72d6902f2fc146711f8473045022100d63bf0ae231adf6b375df708d19f69aa2a403299638b7224dbafbee2879b470a02203d0c8d8eec0282d27048e5a90d6e76dddad5d1b5bab062450e854a66200d4ebf,03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c902473045022100eaf05c11a52b0f011425556b04a3651cee5487ced9c8bf17d50950ba01c6106502205fe9b551f02171154905d0508f1f96793099ed3dcd748638a7d65a6d9033079f473045022100951c6f90beda821a34cf4b20d91887c43ef180c45cf9be9723a6b4b0555e7bc2022075ea31ff6b50e0125e87a41122efd0da75782d1dec1c97dfb112edc478bed8a2,02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b902473045022100d02ded622bf20ac0004b56cf09fbbf8c9ab484070bbd2920e1a4843a0c815b290220185674e25c0e0b1a6ca018298398d3f2c2b375ef2c0c9d90823bea159846924f473045022100825558d9750bbd821d2ceb4e10e5cb56b65ce7abda805eac6164d90d582a6d9a02200d4d2bf8e63dd9def56e916918c0d5a32129e8db68458a8899b53dc76ca67f66,035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b80302463044022000b158eeec080fbdbaa27d4af927159a6c1f4ae01b2b4426196306f65b4e69880220110ea778c84fadf657180674a2729817033b3010f84e1281be6b2bd0c8698bb047304502210081f59be9f6f7a8f94729fb37d89183a74acbdfc00c1b300ef6f801697c9573430220688349f4d5e5e57c75ce176ce128766400fd4e602eef64364183e83d3db80d24,03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee2102463044022026fd1abaed5faa4e7ea61ac49bd09d8a5d6d46906ff72ffb910c63ab3a633930022016dbb4f6bd5aad6f1a5746de1917db448119237439ff8a5006914e69f96e4f8b46304402201e5a99b84359e44497514593db79427ca03401a009e1d94bc7a90062d53167d302206a9ef642c80fb813e1f9f64c70b18bd1d1abccb8892068628886ec7cf64b5668,02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd0246304402202f642ae0b55560dcfed70670ad0af85b9a5daa2cc884d5ff578db667a788e65502206e718f93c1c3e1de08aa3ca133a6e8145870bee5dd5cbd06cd322499390b768d473045022100c611a1ee0d847183c82364c9286b5a7fc013a6755d8766b91a4d31ae46c33089022040d6b5960c8531c001800eef69c185e8ab2398aaeb450144f4de846a49124a2b,023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe9002463044022066f57550a06d9463d55fb3fdb93f30cb7a4d6d4b69038db55af47790648a981102203db68cd18220c4e145ddb5265d2a6262059b4f72581f9bb2d8deadc2b673041c46304402201d428c34d8d81efac9e6412605ff51714bd6f0d9db0a05cec935a45b41d4a7970220111b26e07571be69ff231477842cdcd5a51221e20862d9e161017cb6a9240391";
        //Map<String, List<byte[]>> signatures = new HashMap<>();
        String[] signDatas = signDataArray.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            List<String> datas = signDataObj.getSignatures().stream().map(HexUtil::encode).collect(Collectors.toList());
            //signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures());
            System.out.println(String.format("pubkey: %s, size: %s, signatures: %s", HexUtil.encode(signDataObj.getPubkey()), datas.size(), datas.toString()));
        }
        System.out.println();
    }

    Object[] baseDataP2SH() throws IOException {

        setMain();
        List<String> oldPubs = new ArrayList<>();
        oldPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        oldPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        oldPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        oldPubs.add("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292");
        oldPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        oldPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        oldPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        oldPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        oldPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        oldPubs.add("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049");
        oldPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        oldPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        oldPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        oldPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        oldPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        List<String> newPubs = new ArrayList<>();
        newPubs.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        newPubs.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        newPubs.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        newPubs.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        newPubs.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        newPubs.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        newPubs.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        newPubs.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        newPubs.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        newPubs.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        newPubs.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        newPubs.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        newPubs.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        newPubs.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        newPubs.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");
        //String toAddress = "bc1q7l4q8kqekyur4ak3tf4s2rr9rp4nhz6axejxjwrc3f28ywm4tl8smz5dpd";
        String utxoJson = "[{\"txid\":\"e4a6686663d9be42130d4153b890c50980e064be1733fc967f39aa8df796f7f1\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":920000},{\"txid\":\"c9f69c4923312e5c316363caec9c33ed290f4eccbb0caaa6bef59e8e5f0e5275\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834519,\"block_hash\":\"00000000000000000002bb94d4d4273e054bdcb716cd989f078728398ef2fbec\",\"block_time\":1710339804},\"value\":4085000},{\"txid\":\"e1c4d372a1b8ba002e39f995b547ca1d2013c33c3dcb3f779a172d64519bf81a\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834507,\"block_hash\":\"000000000000000000017c367acd384aa572a5e10096a9f2de2918b7e3d3f9fc\",\"block_time\":1710333388},\"value\":2060022},{\"txid\":\"a63ab74e05d9c3e2ed2bd3730839bdcab75d0ce2f9e6f7ecf023fd88e287ea40\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834444,\"block_hash\":\"00000000000000000000918db8877a287680c10f431297d4e8a3ac7ba6eb6b8b\",\"block_time\":1710298985},\"value\":500000},{\"txid\":\"011bf09611dc02eb0cad2f05cce17b24c510f3a81b898ada2c82862cb505d44b\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834503,\"block_hash\":\"00000000000000000002d0a420e689635927aed42faa1fbca12459b1c445c65c\",\"block_time\":1710329124},\"value\":40000},{\"txid\":\"066f496b23abb51cb0475721470ad087c6d5bbcb068e15e3f2fcb704ed89327a\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834506,\"block_hash\":\"00000000000000000000e11cf7318981dab3a9e0b2a62299ac830472552be99c\",\"block_time\":1710332872},\"value\":20000},{\"txid\":\"bc2582dc8c43b37cfa377175d7be4dbac8d1a02146dabcccc60881e3d2cc4996\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834507,\"block_hash\":\"000000000000000000017c367acd384aa572a5e10096a9f2de2918b7e3d3f9fc\",\"block_time\":1710333388},\"value\":2060022},{\"txid\":\"1e2d5ddf16cc9ba248b5307d35b828311020423e3f02a84a61f171ca681ca6aa\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834340,\"block_hash\":\"000000000000000000000192b082f43fc989cfb48e37a3c2c292a7771dac138c\",\"block_time\":1710237696},\"value\":930000},{\"txid\":\"794d80b28aa22fd51d2b26401d4347857f435d29d20e1bb9838561c68f1c4ed4\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834514,\"block_hash\":\"0000000000000000000071923ef5a18dc17d47717fae386296dfe8902414ea71\",\"block_time\":1710337707},\"value\":1000000},{\"txid\":\"48f2a0e778c99e7a6dd5653756783a2c3eb2d691d497cc3a74c2bc5a68aeadef\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":4400000},{\"txid\":\"9c6b1df9180238583f9305816cbf102c2d5dcdb4401cd73393f8e109e0a39849\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":836186,\"block_hash\":\"000000000000000000021ae9eabc57563f19d495089af67151e3bb8fa2de1047\",\"block_time\":1711333743},\"value\":5000000},{\"txid\":\"de55fbb329ffd86c42b128b063f4822a79bb3c820e0e42b9f1adaf1b53749375\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834474,\"block_hash\":\"00000000000000000000aa6fe2c4ac729585285d2eb35e9b05959e4d19e4f2ea\",\"block_time\":1710311926},\"value\":2500000},{\"txid\":\"386a2e41ad8290c911cf287305b5b80143c2e201e41526f400f885173bb24fa4\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":835341,\"block_hash\":\"000000000000000000015196ea61074d4acadb829207329519116c5a1f14aa86\",\"block_time\":1710843998},\"value\":2000000},{\"txid\":\"fbac062373464cf0e9fbbd0651e8cb35e368225d2a50efac098f084c7bfe4809\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834284,\"block_hash\":\"0000000000000000000026bac4bd7157cf4b880133b130f47362e6b0c132a091\",\"block_time\":1710200443},\"value\":100000},{\"txid\":\"10f9d9ee4a258e86c9a4c52b77ea9ebdffd3b47789bb9933a9223112cd56f98f\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":834515,\"block_hash\":\"00000000000000000001ee20083bb8df96e256bb46dd856c48f686b692dbc29b\",\"block_time\":1710338617},\"value\":15000}]";

        /*
        setDev();
        List<String> oldPubs = new ArrayList<>();
        oldPubs.add("03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9");
        oldPubs.add("024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba75");
        oldPubs.add("02c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded");
        List<String> newPubs = new ArrayList<>();
        newPubs.add("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260");
        newPubs.add("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5");
        newPubs.add("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750");
        //String toAddress = "tb1qtskq8773jlhjqm7ad6a8kxhxleznp0nech0wpk0nxt45khuy0vmqwzeumf";
        String utxoJson = "[{\"txid\":\"2ca458c0b7dd877a56f5d5d00ac18c3623a298bcdc9bfff3f2ea634c1e1c02ca\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":2580056,\"block_hash\":\"000000000000002267939f2eb9a84c49932280125b2149c9ab1b00b7a7b1c513\",\"block_time\":1709287224},\"value\":80810},{\"txid\":\"78dddebc51390f7af07dfcc6f5b6c2118178487194d1bb8ed37203cf971ca795\",\"vout\":0,\"status\":{\"confirmed\":true,\"block_height\":2573608,\"block_hash\":\"0000000000000037012f920766ffe2f5bbfb35eec4f2668a67e6c3b804e29bd2\",\"block_time\":1705492054},\"value\":102005},{\"txid\":\"3086f9af3d4c39bdda37f1bab9d93540e7f6bd91c5e82d2a09f9cfceb561b2a7\",\"vout\":0,\"status\":{\"confirmed\":true,\"block_height\":2573590,\"block_hash\":\"000000000000000f33f19bff5fab6d9f47c67af445ec021a32c13da68fe30959\",\"block_time\":1705484471},\"value\":102004},{\"txid\":\"2f20279a52fbfe69252418f5e54311c8fdbcea1f0cdfe666685c620c7b7237ff\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":2580054,\"block_hash\":\"000000000000001ad6aeef3c2c57c905013e6547a7a4f7cf0e88c5669ec7adc2\",\"block_time\":1709286243},\"value\":110504},{\"txid\":\"4b266a1e4bbf755939ccb5703211bd9d1a4a6bfbbe67ef30f36545e1842bfa19\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":2580056,\"block_hash\":\"000000000000002267939f2eb9a84c49932280125b2149c9ab1b00b7a7b1c513\",\"block_time\":1709287224},\"value\":92411},{\"txid\":\"30d85e89903c564dfffafece102adbcd9ecd965d61aa40bbd7ffdab46d47998f\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":2580060,\"block_hash\":\"000000000000000bd170f04aeeafe87bb6cdf9a9be204183a63af5add302a458\",\"block_time\":1709287815},\"value\":62102},{\"txid\":\"137b3e1f35df6d9a7ab5fe344ac1c093b9ebd0d891a4cfe6b0b7b4cd4842cbb4\",\"vout\":0,\"status\":{\"confirmed\":true,\"block_height\":2573192,\"block_hash\":\"000000000006cac7433d32e145530c67d6117b1804fa8373afb7f2a96f848eaf\",\"block_time\":1705463740},\"value\":102003},{\"txid\":\"21be6dbbc71ef9c2aaf638138bf0d1c7ff38a9cad71c7838f17839086818b688\",\"vout\":0,\"status\":{\"confirmed\":true,\"block_height\":2573611,\"block_hash\":\"000000000000003263de2d0b7632cc9f8a59b24c96eceb1b20c85ea4200e3ada\",\"block_time\":1705493003},\"value\":102006},{\"txid\":\"159805e8cb4db158f9de944c186465721933db3529ac5d50ba9ad3f1f41ada3d\",\"vout\":0,\"status\":{\"confirmed\":true,\"block_height\":2572889,\"block_hash\":\"000000000000001319c16c9bf84f2e9968bfc40e7152d9b357619e3f34d29ed8\",\"block_time\":1705291361},\"value\":12110},{\"txid\":\"18e57715ab39eee064ec8c1210c28515adb097aef8fcc7e4faf40f45e172f227\",\"vout\":1,\"status\":{\"confirmed\":true,\"block_height\":2580053,\"block_hash\":\"000000000000370752a6d002a594cb149f85595f4f941c13d3e18cb6712bbd20\",\"block_time\":1709285368},\"value\":1404}]";
        */

        List<UTXOData> utxoDataList = new ArrayList<>();
        List<Map> utxoList = JSONUtils.json2list(utxoJson, Map.class);
        for (Map map : utxoList) {
            UTXOData utxoData = new UTXOData();
            utxoData.setTxid(map.get("txid").toString());
            utxoData.setVout(Integer.parseInt(map.get("vout").toString()));
            utxoData.setAmount(new BigInteger(map.get("value").toString()));
            utxoDataList.add(utxoData);
        }
        WithdrawalUTXO w = new WithdrawalUTXO();
        w.setHtgChainId(201);
        w.setFeeRate(12);
        w.setPubs(newPubs.stream().map(p -> HexUtil.decode(p)).collect(Collectors.toList()));
        w.setUtxoDataList(utxoDataList);
        List<UTXOData> UTXOList = w.getUtxoDataList();
        // take pubkeys of all managers
        List<ECKey> newPubEcKeys = w.getPubs().stream().map(p -> ECKey.fromPublicOnly(p)).collect(Collectors.toList());
        List<ECKey> oldPubEcKeys = oldPubs.stream().map(p -> ECKey.fromPublicOnly(HexUtil.decode(p))).collect(Collectors.toList());
        String toAddress = BitCoinLibUtil.getNativeSegwitMultiSignAddress(BitCoinLibUtil.getByzantineCount(newPubEcKeys.size()), newPubEcKeys, mainnet);
        // calc the min number of signatures
        int n = oldPubEcKeys.size(), m = BitCoinLibUtil.getByzantineCount(n);
        long size = BitCoinLibUtil.calcFeeMultiSignSize(UTXOList.size(), 1, new int[0], m, n);
        long fee = size * w.getFeeRate();
        System.out.println(String.format("Calc Fee: %s, Size: %s", fee, size));
        long totalMoney = 0;
        for (int k = 0; k < UTXOList.size(); k++) {
            totalMoney += UTXOList.get(k).getAmount().longValue();
        }
        System.out.println(String.format("UTXO Total: %s", totalMoney));
        List<ECKey> currentPubs = oldPubEcKeys;
        long amount = totalMoney - fee;
        System.out.println(String.format("Transfer Amount: %s", amount));
        List<byte[]> opReturns = Collections.EMPTY_LIST;
        return new Object[]{currentPubs, amount, toAddress, UTXOList, opReturns, m, n, w.getFeeRate()};
    }

    @Test
    public void signDataForP2SHMultiTransferTest() throws Exception {
        setMain();
        List<String> priList = new ArrayList<>();
        priList.add("a");
        priList.add("b");
        priList.add("c");
        priList.add("d");
        priList.add("e");
        //setDev();
        //List<String> priList = this.pris;

        Object[] baseData = this.baseDataP2SH();
        int i = 0;
        List<ECKey> currentPubs = (List<ECKey>) baseData[i++];
        long amount = (long) baseData[i++];
        String toAddress = (String) baseData[i++];
        List<UTXOData> UTXOList = (List<UTXOData>) baseData[i++];
        List<byte[]> opReturns = (List<byte[]>) baseData[i++];
        int m = (int) baseData[i++];
        int n = (int) baseData[i++];
        long feeRate = (long) baseData[i];

        List<String> signDataList = new ArrayList<>();
        for (String priStr : priList) {
            ECKey pri = ECKey.fromPrivate(HexUtil.decode(priStr));
            List<String> signatures = BitCoinLibUtil.createMultiSigTxByOne(
                    pri,
                    currentPubs,
                    amount,
                    toAddress,
                    UTXOList,
                    opReturns,
                    m,
                    n,
                    feeRate,
                    mainnet,
                    true
            );
            byte[] signerPub = pri.getPubKey();
            BtcSignData signData = new BtcSignData(signerPub, signatures.stream().map(s -> HexUtil.decode(s)).collect(Collectors.toList()));
            signDataList.add(HexUtil.encode(signData.serialize()));
        }
        System.out.println(signDataList);
    }

    @Test
    public void verifyP2SHMultiSignTxBySignData() throws Exception {
        setMain();
        //setDev();
        Object[] baseData = this.baseDataP2SH();
        int i = 0;
        List<ECKey> currentPubs = (List<ECKey>) baseData[i++];
        long amount = (long) baseData[i++];
        String toAddress = (String) baseData[i++];
        List<UTXOData> UTXOList = (List<UTXOData>) baseData[i++];
        List<byte[]> opReturns = (List<byte[]>) baseData[i++];
        int m = (int) baseData[i++];
        int n = (int) baseData[i++];
        long feeRate = (long) baseData[i];

        String signatureDataN = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0f463044022005d0626dc2d8be83d50c1ce0073b4ef86fbf7a67ff14fdd23dd165fca630cb1402207da067301504f9d058e2446cd9d97e89b1d3e15ba9217cc6e3b32c9af00cd1f24730450221008f415afd953c0ec5a5ccafacc523cf49d3397985962542dea2978f9784cb295c02207ba94e7bcad83966caec4117c59b7c84ed7a8d00c045f7dd1ad865792c5e5c30473045022100d5c7b170bdfc07a8b56f6857dc57578aaceb00403bf504a972775e328887fc9e022021c3171176fbee1666de5021b5c906f4e96b06a070f923340314ac3eefcbbecb473045022100a49193ae843589dae175dee737b3e1a829f047b4cee079dddc653ac910a19bc9022015a7d9736453b01dfc4838f3bb2fc849a7b5df4c3e3713e5520f7b9d061490f6473045022100bbe6d9751bd8f805a5a8d934bd9ed23a47330b50dcb2e68fb3b0daa8eef4f4d6022066d35a3767d8724538890cf9c60c0514508cbc733fe6c1067dd98fd0aa6f06764730450221009e3a2cc205caba746330f28ebe543d5af9db4e82bbb48dd907d7e7483e5e54fe02205b675dc8107743843b6ef400b3ea702b5dd9e7891e143508af4372f59bd9b57946304402204ad67b9df473bdd68ec14c0b3c7ced4de2e7255f78331d1b23566047b59767df0220431f161b0824e34bb6826ae47e46e371582139d849bf66de43b84d394c859fb6473045022100daf6e7eb32f49313af8c8652831f7a70f91bd9e2f988468f6a12dd5bb35cc23402202f9eae68b0845f7c0aaaf8725e7e570b90f359e2455fef2bcd090b5f12b01f2646304402203461bc5be616c4061cc36137c4211ad8ab4006d202b82285523c569b45d109bf0220269f8646bedaaf12b6daf79854f86de18020096ed54ecc55d4222013de2be9f2473045022100d81a967d5ee4ec78082606f87174524220fc824bfee6054b2404db3bdab1ff1d02205ae231c6a6310323cd8dc9186afa870e561b9a6fa3836ed7ce0b3bbcb5676af64630440220556fbfbe18852207d6e896bfca567bb8f5771893be91804668b22fd1d8ee79a7022023af8a48c615607a267107a0b1b9eae1ce95ce4385140973fd6314c5cf940164473045022100d02191308ce1fd29343b098b8db5c8caab3d286331dd876f5a95558b2aa177d402204e09a22b54e21d23d299741d81a2bef6345006fc27c64ff0c403b343d44bf6104630440220584e2bb88795eb4acadb6026363caa5e97bdb3f9aacd791112ba4d22eece6ecd0220111df10328461613a3172c419f3047e7e04b1c251b3c7f5bdaf6f711736d5a7946304402203b1e606dc14c3e401807f3bab08d7d13045c14f9da1280cecabae1ae70eb414702200e0630d4d61a38b0065379a1d6dc75b297be592d6cd20127fc939c136d1a455e473045022100f653d2d7e3f4f56c9aef2b1ba28b7cfb9b6bde980b7f1d52783a500e5cfc628c022055512b006f200cc5df2dd7b94573cdb7362b72f2b0d674a1273f9e453b796912, 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d0f46304402200d4f1d96ad9f4de7f6dc79aef4ca3e7da4ba4677de6290e1a4c79e25d23adb5d02201165dcd0dea6c5e6de57944bbe9e9565ef253354436d96c5fd0caba08f72c7ea473045022100b55e9d31c983865f63a48f14b5f42ba64a474c72dd42ce634096df0afd7991f50220185667bf35c707d8551c4daa681d90812023253e1e80f94950a773d20ca0a54346304402203b48c700e6315239e83fac3975d1ab1c66b8e7244cb68d426caa3e01288488c002200f20c163169377c609b6752102e8548231f5e7c66c548729472789d7a2ca4611473045022100b27948ca9a8da0d0ebce2ed0a593143cd2a8beb6c40e47b12d6fba3225c27b82022036a19e968e7a226473bfb7b80537f2ddac16f346a751d11a71e25f63f2e5a656473045022100e6095c204d47099ccc8d4af3169c2420c1d0b1737f7001041248a0a84f411237022051f378c4cd21fa346e0834a4ed99abc717f795a9865ef77ecdab3e7c10107c61473045022100dcb2f1b9e6cce25305268754a1b9287ff55da44cafdabf8289b85f7f82f3d89d02205bf6dd171eba497d851ab059e2f536deedac6630d850f693ceacf9c2caec69a64730450221008d6fb8b15826c7e0b08a8a658fe90a9df6fdeb457e26a52bfdeacdaaf6bac03902207adf81ed0d00f4b88ba529b46c6df2db8a4ae6e8ff4534ee5bc23f758f1febe246304402201b9ad60525c731a0b0ac32aa3119c3d829903246c73686f5616d02c124903cb202206877596ae734ac933809c1f6e6ff6ec127613cf9bd24a3fb447bd1181fc4ecf246304402201096baa581dfa7b5eb45b2d9518fdaae5cc56cee809142a362587ace255e4db6022000e3e80202897da84aeced87ba61c8cd9cc9871c33ebd040c75117678974f94b473045022100d302ec70bae224754b0f1d17279f2124136605379e55592cb1d5fc8c15d0403e02204f963d514af55327fb990c036d485bd57b59f32434eaa7d7478132b9e4d78e0246304402201be5fc85b9af376aedaa2dcb09255285060bc6d31e9f0098556af56da03f05a102207b56ab79591b687509f5134effea8835a92ce7c0d27bd3b52c52fba0950b7c23463044022077cad790efa4e40df8a5ba03574a68afa9d9a5863904bcabaa9c23897fc1b7d602203ea1eca844d75f2bbc171512c8e0c3932a92c1a9b81acfff78287e491a85024746304402205d3158adddb8b2def44f474cf1f10575c83cc5c40e29d5cb6e157ca1b81ad21402201c84339b859559d07eb2a3a3821f6d3106472dc61b532e16fbfadd39cdfdf986473045022100fae7922224b2731756f2904dc8646f539108f1e87aa4db5809965885ceaa3d8802202b084688bece392a697fe87ac8f7cb6e6e9ae70f2f7eafffec4b8971e2747500473045022100f4597bf4328791d7b2fd9e65de2f22ae9e111c2a83e9be95735a0753b16783e802203042059e826ed1e0fc14773d73d2e04f8816c8b3136b626ce0769786d3d51d10, 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f00f4630440220332b5210e89928471ad2185d10db234e1c2d4f32de78bfb334a64ff2d45d48570220396c69cfd82a28b75251187dddf4fa74258b984289488806061235442215d954473045022100efede499da788cb73efcea762037852e43a6d0bae3967817ff862309875db07202202f60e696166f4ca6c9684e5599f169a1cefe3e86248fb25afa7f84bc64d7e5b9473045022100dd57bddf1c6a9bb8078b16e43e9f1081c66b6fca0fff8e061d1ebcfd45709c2c02206f2c0cc7f0cf8d5d463b39bcf6e3085189102a7da6303713f98544a2dcbfa86d473045022100ff799532d070b642204d4f2cdd7106cce85929cbb4594b3dcfb9d8cd33b9c1b002205bad612c03ad3b8a1e3f326f28782c1865b81175c37e2dd6597aa51215554f6446304402205764dc29b8ddc48886194583db460c66e5391f2ba3ec77b34bfd56c978aaf85502206483db42615bbf10d08684309636d048ac14ecd9571d8b044a2c636012585ae0473045022100b6ec5379ad34cf6a94b549ddda2154661f8be6fb70c974dd211ff40b99371e88022047bc4871ea4d14fa874cf680488e9823bb55b1af1a78cd16d9837495da216ae24730450221008b164f576ee037b250822e0513228f3705629e8821d15163c41f1c46d0332006022014a3b67aaffc2869cd611110242d2f0e7ad821b2d8ad8ef9e2ebfaa8d091b2ec46304402204c0001b84a77747a8f28d8498c0c4a18b35ada61a6264a1103e28a23f7681e6d02205d91041a5a94f242ab8a96d9cf80e4eea9e16c3c8aa0b86381ce3284fffb196f463044022012e32eaeb9df59158a299b0f895cdd469fd46771c969c2698553d5d94a48786f0220274f8b3bdf82992bfb9c3ee6da1775f4b0a2782a88b5f75c233c1c8d1a088cce463044022034199f80246ae1eb597a26b88e91dd866d77edc93b302df7a9bba3743c71fc5c0220136d38841515806398b67a9f1c1682662ac1c48704e0b01c69e0406a333b84fa46304402207f9121811d1bdf6b13a7c1592996c1fef88dbf8e8ce90f1a833d07f9034622f40220281515148c6818566d9dee830df2072df3515c31fd45353e70486763fc94f87a46304402206098257832bdf14373f71a246f484f12c469c258b53319c2dd7643b95d11b63e0220481d1918d091a2b8510997605eb3cb7963ca3b5f63fb6782676501bb1bed51b746304402202faf9638d593f581bf0149c1204d34a7efb480d184412d178d3e6a50df0c8d4802203d5c2b006824525d792d287c2a6223c8e2dc393743de8b7c8e8b1cc9ae52291f4630440220797a1f4062034e5c2558e32f4a574a667e033e88c0daf9901788a17b3e692a1602205e7766f85012afb9a8ff5dd49eb5d82d5ca209699a0edeff1ddc9c5ba71bbb0746304402205e7ce30b20a10b19e90496344f9bb981a0e41aae37c7e604e23930dfe649a64302204b82bc70c6fb7f3727878e39e9d758d5ccb82c4076d0750d019769798a69a4dd, 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030f46304402202ba1561247fd8f891aef85c22b21be06dad82a3818e59cced433cd3266a3e80a02204bea1dc28168099f695e91f34561bd40781219a12e4ec9b46d8c595825765537473045022100fd8761b62386c56cb0029297cf950576310c436b27d71baa99aed68f91fe48a40220249c40641dd5e768c4150f5dc85063a656837cb3c8cad1ddab34f4cca35e36414730450221008440acc1d85d00e0d0ad2f3f462bd3991f8a31a3aef6076782512f5b86cdc07a022025e875b981c3f3f8037a4d1afa18cd27f1489a3ec6ea3134e18c86e2b165db064630440220498d714c7d12e6a4b23397cc0e6c8bc7c9f52de8535680092b4954f4b47061bf0220601ea42960475f0e6ceeed3ddf043d856ac0c876aa881619d41175922b8002bb4730450221008f73688a0b05f910421d3c44245b7ad982c9e9a95ef0b267af9cacc79a3bf53a02202c4b8f66dfe5844e2b46cbf70aaa8b3fd5737e800904475850235026829fd03c473045022100bfddfef03cb887677b642c2eb534f0b33097751ffb07747fa724c0fd0db4642202205d87a489058e701cabd037804966dfa185a1d3cbfc0186abed4b9365bc26eadc4630440220095a462494a71ad3e8854d4fd3f21be429e526e2555cb9322ac06b35491f9e0e0220360c230e574067a909e381294245f51e4996e96a39bc76285cf5e76e56144760473045022100f965ba8faba6581d83010619e7880eda9a08674c3a1d7c11aefd9e8eda5e153602203545ead786246bd0773c106d54a45b4e9cd007d3197c12b6386e4c1a3e96e9bb46304402201dbea4e12eec045f95a15f18bb992eb99f88b97d79c676489e1b8ee0a26a088a022019bb597807a9b64c78c9eb5e05eb2f6183cd86fcb8a22a16d36afedd0fca19dd46304402204a668d1dff27627c4649721a21014310275ac472bf21d486e4050f121114a0de0220383a39450b9efbc7a69094ba4580223fdff74b2c8292424dd877278246f91a5246304402203bb98194da8dac68fda24418a3452013306accbf7a301fcfb0ac7c8981fe8011022005df45391728d136671dae69c186adc858b04d1a286e4028ad0b431cb048f7924630440220375280f6ee97238d4c2d69922b3f8f707c471413f819efd30979dd98f93c532a02207bf44f5bedc26870970079cd9577a8bd6209a3fe034f4970f917f42f5d8028e546304402207f21c9e9e1cd46db8b9c34247fbcd1df00331c9442657c0fecc1fb489158475f02201694579b160b22a700eafcd4db379181178d4148cbb96c6fcb48a95a7ae4d300473045022100fb09b4ea0ef5c58d4b3956707de3921a0afe79e7bdefae48c124f5a27f59659502207841d9a4d7acb63ca902d592e08d657b8649fe78cbffce496bc8ac7d1c966a22473045022100ae94c89535e052bb9e9536f74e189a0039d216e52c33822c2152a63a2fefb8fd02201edf282d9a364b11fb88498e992781b85ac23fa450854f80a39c9189222a34ee, 028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70f473045022100a1ef88477490b0e44a80cba6714bec67eda7db288bf7ab1b65dc2b901fb522ed022015a19a455a9a3c0a14c95967f6c3871b775ed90742f328d0f0b0936d16caf383473045022100cf48f055ed098d4977443e3c59baccbd2a7c444d5ced81e347878709659f9d2f022050f0e6f54a2fdfb401a8d2893897b53051c3a9a65317b49853d5905b21cbd25746304402207faa4bae863b7e67d58c15dc6d40a652b6e0d5cb853d0b487c2e87810bd4203b02206f7dd263a94c87fb0f8303871e009990b3916f6582e9f71eb51cb596a08634fa473045022100e2df4ce85e84c8ef8fbbca44a0ef95fa32f612e01b924949beab9b4d258c82f802206192b909b14133a780b53d533102449b1ff1f2578eec4e09be7c62960821a3d0463044022040a002d18bfd00cf76b0420550a58eb516a77063c9128879d7be8db222b524cf02207c132d514ad208a643959d66edbc0346b8dd5541149e49af2afc2a6580e7f37b473045022100c34df8582589335e21fc51a26f7e8a432a65e08cacffc4d91456336e7c2cf31e02204ab374b84bca6fca77da6b459f82d8aedd6b14ea50dcba296530838944b87edf46304402205dd313f5cc6b6bba1b18900542fcd9d665ec738890d71fbc7bd068f46f0cd7da022067e5e6d30c44d793f37fa875e0ff93282a59f697c5e625305fbe607ff8d8107c4730450221009e2b54c55c5116ae4f551b5ef77debe02c2757745900b0aa5b62256f6cbf01880220051f9d516df9b220af1438e999ba6b1c7863ecd566e22d0e59a863ab6580f16646304402207bfab64ac52d6e014a1a662ba8cadf2944ad623c5b2ca42a7cbe1fca3bd1200902200a304ef2e9dcc0638f8a9e8003e5ade3654f5f759c8d64a328f78679e3e22d98463044022059ad1d1b45e135a776dc9c8f92da39c6e843a53431f3b6aa8d09ece39eaece5202204e939162ef889296a369b1122bc679cd0b8492b119751bdbbbc01103c9db0f38463044022019c8728d7fc4260d1184434e8f33de37d141d016454b312bd9e33011904a559202207cdf4f16caa4b49bc752a2f981eacd36484bc783062919309a50e05e98a089c746304402206b6e350ce3bb8bf4a5afdd5a417d6c9a91180a6bc86b6517b1358653788e51fd0220042495bd96fc5f0f0e58387cd60cd023b5c389e348d72befb85127747635dfa546304402203be941c6255a0a3d7ee5c1c52861e3bbf639eb9b409f537b629d10b560cf9ce702206ec2b1b1c0a1ad0ef37c55b09d821fa8149bb2192a6067e7294c0a572d1413f3473045022100afc0f6c86770bd25ef265673aa143e5fc7b62bf0701671e48d49fd19013ab50902204ffc64bb0c7fe4232427de14a325a5626486361d4360cdd6844a9dcdc7789321463044022019dc2dd4bc2bf67ea3de69b1809272a7450baaf8e5d6f2eee7c87001ba7463d402204ceaaaf15985d11b91c4eda5abfcc9bf14adae806b18a5cbcc2b6eb9e9fe3e2c";
        String signatureDataL = "02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd0f46304402207ae983fa7ba344289f3e13b293d75b00819b02b46c2a7a1b19c3b4820f5de67402203acb0d2d094c6259579fae07b668f4e550e5a089cea6a1a49c7d9127bbb38c8b473045022100ba5caaf844b1223e8e993a9b22fcf01481d9c26ef70760ac690aa42a484f5cc20220282e1085dcd444f5810a6da9e2afda44947289c9719bf5b3924fa5c4080e1abe473045022100ba0a6325e463584cdfd0ee5d9ec12c1b3bcfcd542600e4734ffd7990c6452ac7022011a05995c8bb8a1cb7f3c28a089c2727ce495305c876375c3a17c22b5bb25e8e463044022100b14dc0ad2ee0a5d54283dc84b84226bc4d730ba0b2f858efbbdc0e516b94c72d021f6d52493c175ddcfb56701041cf31c7e21f68f48a27743680517458a3f07828463044022003a4d482e5139e9c22a239b7f8d776c47a4756d3475ccc441f73111ecc870f22022008a9f04d3deec06b9009048dbd094660d9893cbfaf9681c02ea4810eacd40f61473045022100a62b92d97660fbc67c1e05a590f6999643d71ab7ea400e960687b240dc882c2402204c42aab55aa71c7349cf4db8e10daff83933bbeb3beed263c6224300774c28fe463044022060ae53c8720899f4980f9434708790a90c8b02db820d0e36839a4c1d80e4409a02200e29cf43a091b5fe98d1cff47f2ff178d873b2bf603b031b29e0fe70c5228af1473045022100992182139d45675cf9d37516f8955f583f2c4a310aaab3ade112dc04bb66122b02201519fd85f98eec27e4ec4882006284c6797294d7afb8ee7687705aea7c5773d3463044022027f0aafe0ee9b8851f2516ed71f2a25ab97bf25134b96b562716148a2c1433f502201906f36b8bc0773de1972a51a6964175a27e8538c93468c6dc77a48fbc6ac4284630440220676727bd800bc0be80250b9d79600f3275ce43721416c67da3c4a71e8b3b7cfb02202c38e1c7f5b8aca2f98b741ebd186577f579c10ff8e4c3ca5114728cf12dd3bc473045022100eba0ce70bf18408d17d77048baa645cb66aed0e9092de59480d1e98be43f7a5a0220040303f6bcfe460217ad46bda269872c779e2d360ee9e52b5b1c1cf08959cc51463044022004fe7a4c8835525e56e33b3831900292749276e9323c92abdaa016f364bad6ce02203e800cfecec35222f20710ec4d2e948c3e82463fd93a21ee3d83801a4f8665f247304502210091e0c1801eaa806f43a38051276a569a027ba276fd65558ec6017a284961a00b022003b922258396be5da265b347cdf450fa143f830cacf76cc8762c5eea1e3360a34630440220673d87c72f0266ce47480e04c083530f3ac3c220cb9a35f01a08b9743a40aae702205927899f3b2e3d728806fcdc935a9a751207a3c144281fd5ded511cb7d4eafe446304402201676788b942ee2a8ddd801f63c091d930737d84789045dc7677595e1ce42492802206ef6a64daac0b27992917cb753a943be866ad692464834f1c7802e2effd11efe, 03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40f463044022038f6520732274ca637d7c60ef1a584b241a7637d7d75290e3b2709ccc973938c02206a79f1adfd57950a3137b3a05aa480a79877655d43d9641c52141f9c54a5c324463044022059ddb7d49b1651564adc97b2d9ea5dd9e1b3733712438680c0795266ab47382e02202501599810f8f1fbc09bcbd55e8da8e96d82f75ef396e7a4bc59e8b7c946e1bf463044022049f920ca60264ba1674c0038c01963fe1a4341f496e6258c653ae1277348cccf022041ca7286da2d7fd454bc7bff5b61165c3b69aa163ab3be127cb1af82f8171744473045022100c004df80065bf94434d8c2fa8238d2f092e90d19387aa655567d97cca954f687022048ce1154961c6a185e20b68984e6ad894d9bb9ba4d3f47444bdd4c91beeff714463044022055dda8a1824c7cad17e7fdb1a7e5e5128d0c2642fbc222ca82306b2e393a217c02206c5fd7850af1a9823e2e06456fb30a6d88fcabbe84d15f6fbadab121136a51ee473045022100e027ae6655a8bd3a46feeb36dd477d1f87baabe09792138cc39c45c41f6639eb02206d6c1d1eddee9664338c6a68ce9083cfb506c376124fefe28e62ee97a54c54d0473045022100c61d83a2064901985c8b35ccafd31a84e659f8055cde116c1f613ba54eea2208022026602bab037d3d2f2befe7e6edc5986a324bd3b5f7fd4e1f51ff3475ff98da86473045022100fad0b898820b24b1ca3c0ab4d1c31a4d749d8c4ef89f3f616b239fe5fbf2c244022041df859c65c68deddaf08f4948e3cfe5eeaa36ab75a4228a8c0515720ec1b36a463044022020fe02b5497f6ed60ad0e1d4f98edb7eb6d832b36124a78af9ac985fb1bbbadc02206a8ce0268b5710673ad7e1a2bf078e54a204ea2d48480bb2a1183ea0fb7a814f463044022046f39c2a5e4c4e3ccd6c96c9167f0e33f3fa28c5468b85e2a258c18c737b7dfc02205da01a04f9378c418fa9a9dc4baec34f2b0941914b0200a734686bac574ecb78473045022100d338c6bf56711f6c4207c2d053cb11047b8c157e2fa0baa20ede6ad8585400780220018bb8dd626308a2a2a4b54ccca78304c47ce78501241a3c6737a50b03306fed463044022034347c670dfd9b0d872f8552522e1864c1dec42efff206c1289539478f9f052202205176bd22d583a53fda31654e51e7a38effb33cc0cf188df4e86988c738448d3b463044022073163bd025d811f9d92280aa48c73b1863a7c7870788f4bf264e2c01c5cfe91d0220694c54d76cce9c94159f365b22cf6f755fb7e714c990e40786801666e6ee5821473045022100b4f99f3a6a46d69d3dcd1382c1626b51e6e683ed30b5d0a278448839efeeeeab0220228f7ef6cc41cc99ed07e60308d51a24de312c0838042cd11f89644f9df6539e4730450221008add9f25ae96c913396f4cc9beff038c22f794dc9242822eb945406efcb419b3022030a097d0869e7456963ec17b6952be135d0f796dd9dada0b9a8983f38af24a5d, 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c90f473045022100917db7a8ae3b82de0297d22bfa98cb896e95c2d00e0b1e35cf91b592c6911a6f02207def729a70197b993cc834d3f9c8ca4c21900fddd50259fa765cb57ed18e5b2a46304402200949cc1953ccf2e3c77b7b05e6de301d2ff6b3ab06e6272a931342b50292ba5702203db24b5cd146d210b81fd9485ac269d291cd0286eb1c5fb27c05e5dd0bee510f473045022100dbd3f2fa5a911322035cc828c5afce61a3cbbcb5c5a0bd409fdeb7bab3b1ac8102206ec30059cea447e2e6bd20346edbde235b668d957098168e64b39195899fa7b546304402203bd1efedb089aaf519457a6ec933e76f02b84925fbb90a1468461907067ceafe0220345f7b5f3fe7ddf2bc2d4ba346b9f9e0c9275d28951dd3b05dabb90e40a68698463044022022d35a13bfa132715dfae46d7411fc75f339c13c267650ac3f5bd19e5fff760a02207d20ef8bfd1aa2a7ec5ef773b0e570f660a752ce6fd55d7a85ccc47968b4ca73473045022100fea82cea97f62557b33f84d855ec8e5365876d396a4966e899d95eae78f33c6f022062abb4bf22824203c195cecba5b118fc1a8652a67739757d21275b2f85a8ff304630440220711364e958735feac2089ccdbcbd8acf47a21f4be452299cb293d2143cc8b26102200833e5b1c960deeb8b1d654c6f57af2d50876554d1e1bf365fc1081e144b2f0d47304502210082debf646d9cb351152c2e3f26d77fadf2a649c04a7f2c286002fc6f559c9bf7022022f8d69dd094721e43b030ae3e7a5972916cb1380fba4db8ea866ae005701b2a4630440220344f642f9ec019e86402f5acc7df39cb7cb55b8b3d64a22c38832937fcd7d94502202756b83f46e6d89137fb45616d3d3009ebbb225fd31449de4c421093798aac0346304402201c57356359f27893a41d69cd21b84284465a4d34d177f6335272251384b1499702202fbb41cdd95f26e66ba69c4b407192bb59099e7257259afe211d1cd506bf339f473045022100937bdeb99805401d28656a2d3de4e29873019ea8b47d7f6628f1a157dec09aaf022018b6b224c84b1eb0ab1d4067518a30ea1d7c039c378af8f4c8c509b419b4021946304402204bf6761d289c4dcf209d581089372a715a48bf376378bc742445cca7b019ef8702207fdd1f1a3bed5c7310814e8ccc79a1c38361882e7aa9007e9a04e69471ef486346304402207a5380889658458a0ff997d6ada96ebc398cb9af54b4836365d59eea2444f6fd022023e93dff22026f0b5ecfc3db22c470786a29dccfa6b42e169f3b13c9dfea348246304402205c5b8ded4f6a25fc7da07984e351590174fffeb4a9efc2c548dd9632f2418c1e02203288318e08a05f2a6f2f983f94185743c816fcd42a950fc353341aa3af47e04b46304402202d3c75ce85811f93b578376e4d3c17e6b4c20779535384e2d5041cc782204d5a0220020c339157b5fe07eef900b0182aef9319b870b0a1bde235d88b297eb2edab86, 035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b8030f4630440220388b5eca4fe1fb2d3249bb73d3fca58cddc8e828cda31d021f077a8d9f89c28602203d05f81250e3490fa8125064c385b426b45c4a841529940be0021b9c0328c4e347304502210093d3d8491ba9173ecd9858c64ea50e02ecbea21844efe75217082cd2927f0e7402207833085dc2151b426d4a7dbcf1a2412c409e7a113279bf489d850af20d3d0ea346304402202753bb01134f0cfba018559baac94f4d9324f2bbec874c9e6078671464f80b8402206d3507e523d2f948e031ab56729b071b4d13db4d7d872ccc4e97eac185a999c2473045022100bf959dfc3da2b504a201e79ba40505b77fd9a13561c09c1927a7791d50b7d4a30220505daaa895577303dbfe9a11a5c0ea2281f3bd92f2bac9f4cb7c0909cfed9898473045022100e2e96e5e527d2a1cf6630539d77a681ebb1903e1c3bb1f6d4e7085d04b3c1a3d02203b14f882868f666675cfa11cc6f9914999a5701a175b3578870ed95c13dc361e46304402207fcdc186135c3c697406769060696a762423361ca6751e59400cee59b214b57f0220098a1c13b69589f7db0ff2edac9ac99009c2783ded7caaebea3eec696fe1ba8a4730450221008cd79d1da8df687df982ffff7d4e2f82cdba70ca4f14a4b2ab0c46176565624702200923d36a6927f63d7605559e6d2aaa940a4e9065da6da3b3c7eb9003854ced5f4630440220240495b0b8b811df166ae3b3b75f989f7b72e13bb7cedf9da93ed2867307490f0220094d7b16154a0fa04df273f5fe32357606b4789578955386a38b6be74d23c00646304402201f38673c66471382347535cf26ef6063b0872bc6cb7cb6248dea81ebf4087c2702203dd3ced2cb3681773e3e805422987d36fca97b7ff7a37f30ab3169a3515886f84730450221008f2baff2674c211dc837dc86a04deb68d80a24efd3b901e01c2df012d1532a7b022010920339c3bd4d517d2296c7a8346918b6388384d0c2b7e4b3ec2be6577b2160473045022100f313a5b6beba80834495b635545753368fbc2b9ffa17eba6f36b739066b8345402207270e8c1c6641dae60a275df31fa23a967ddb2b7a201c7f3015a901e947ae99d46304402206f9c3d452fda598007748adef9420c11c26cd280571e788ce2dbdb9485db70000220162ed78fc1d860fc87a935f497194f11029879eceb339a214084d8ebd9994f63473045022100ff9a3c7c152b42fecc531aa3988a20db247a0afcffb040482e8329375223a9f1022066a82d74339f0984393eec4ec3fb8f1c129873eeaee8819eda853cc0d5f763bb473045022100f4844c451b9657a55570dd3808b5c27d9997362b4c393adde745c9073ea42c1c022049711be459076b6a55e20d5ef3aa25481d950338e2cb1ae0daf7e97fe9743ae946304402201cc070434f456c764565612f5119034bca547842a685f7fc9c4fa4233b25479f02202174afb4be88dae1f3572d971d672cc93fdffb3098b0d7b898db93f9f35529b4, 02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b90f47304502210096fedf98db8e18dc07ba9ab4a3b6c37954de0a860ba786348fc2f147f5c6cc13022027920c61385eb36ef712e51019120ce811ac1a4900444143b3fb3ffb0a1f57614630440220058c6a05d5a57499f920f56a5afdc363a9adbcdc8dbf89da3749bb59b1db3d350220591a98df65fa6e27193ad1079a1b34899ff540a2a123b3390783dd7564415f58473045022100d10f5b01613e61b8efd4da1c3169c880d575c59cfb0bcee7c0bdf168a0d61e0902200684fc776b05126e2fd6102096ad0f43ca3791ebb9273b8683b57641a08bc05d473045022100f9b6b6fd333d61b04145b3d28017d552266a4449432654498812c0e5ea80ee810220047b4556582f208381e1ff2d2280272fe1105281aa86b9c56a7fd10290783e94473045022100d0c872f61367a9f936a5f2827e10c28145c3ff1ae52b4a9e5e02490d13ff39cc022044d8db5d051156bf9c594039aa385c4a67efcc3cc17ddf56a65c415e1f73acef473045022100f1d34fa0fb7c7d845d2d10c6133e8b2e7ed6bc21529808143f4a2dfb941285ad02203fdb02fdad4ba6081e4aa18a8c6c18381b5198752996993de55d41cfe92faaa54630440220053d560aee73e419505688b0121c6e035bc07b9e168ed99d4d8999a94bd0945d0220390c20b18e51860476c6f46a9e4b0cb019ac5db9b8d9e7f8f2d979542e672bb14730450221009d325d25640cdb9aa62fa022155b2f8a67f9a40f3dfdc57049cb3c3076ee5d580220217e4a51236d4fe7e8279801882b1a9b3d0e2f0dbf07a8b0555d8bb68f45fc80463044022067b079649ff2e806ce4a4a286f1d0d25055979d00e6be8acae23c618154f42d202203e29935a08457b815dd189d7c191e4f56dbd9882cde6291a16e3cd51760232b746304402203a9dd4977d13f8911d84c8a05fab07e4434e131e052591af3cf0d59da8426f7f02206b910f8b90ccf506e05c08e001b96599787cab2ae424866bc7cad2bbb9481f4947304502210081588a6b204640f9a0db81c7559e43b7cf61657b7d84642a4d7a6521f2a3c9e6022037c0d870e61dfdddcbcb760e84863ad42d39550cc91f44b30a384a8b14e307b5473045022100debc42d763dac111c52d21bb143956e8969c5e4168a4b17e354ff2d3f709b20f022034aa5e1dc53e69c6d043e511af172087f24fea74256b7a6cfa8de37452eaf92346304402202f5c1c8364c15da341f3260b08124141272406e0584e888e6c16dc1c1c8d365a02204153024d03880219c87d90c0c8e22e0f4e00e37ab4228711f005b56daec94d3b473045022100e65c75f81b56e13c7dfba75b356743f309a72aadf054644d3bbd8ad2799d4e91022015a28a44f9c973f8e43ec38787b7c8dfe8ad1fde97b1925d6b57cbc469f4fe7246304402207d2776440361bbd7aa60bae3c7ccdd722fb8d1d8f2508a7a69f35a5a5ca5341f022007b8c54d2e0fee835e13f4399736ce4ef4594c7749580b7590902b02434965b6";
        String signatureData = signatureDataL + "," + signatureDataN;
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            ECKey pub = ECKey.fromPublicOnly(signDataObj.getPubkey());
            boolean verify = BitCoinLibUtil.verifyMultiSigTxByOne(
                    pub,
                    signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()),
                    currentPubs,
                    amount,
                    toAddress,
                    UTXOList,
                    opReturns,
                    m,
                    n,
                    feeRate,
                    mainnet,
                    true
            );
            if (!verify) {
                System.err.println(String.format("===pub: %s", HexUtil.encode(signDataObj.getPubkey())));
            }
        }
    }

    @Test
    public void createP2SHMultiSignTxBySignData() throws Exception {
        setMain();
        //setDev();
        Object[] baseData = this.baseDataP2SH();
        int i = 0;
        List<ECKey> currentPubs = (List<ECKey>) baseData[i++];
        long amount = (long) baseData[i++];
        String toAddress = (String) baseData[i++];
        List<UTXOData> UTXOList = (List<UTXOData>) baseData[i++];
        List<byte[]> opReturns = (List<byte[]>) baseData[i++];
        int m = (int) baseData[i++];
        int n = (int) baseData[i++];
        long feeRate = (long) baseData[i];

        String signatureDataN = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b0f463044022005d0626dc2d8be83d50c1ce0073b4ef86fbf7a67ff14fdd23dd165fca630cb1402207da067301504f9d058e2446cd9d97e89b1d3e15ba9217cc6e3b32c9af00cd1f24730450221008f415afd953c0ec5a5ccafacc523cf49d3397985962542dea2978f9784cb295c02207ba94e7bcad83966caec4117c59b7c84ed7a8d00c045f7dd1ad865792c5e5c30473045022100d5c7b170bdfc07a8b56f6857dc57578aaceb00403bf504a972775e328887fc9e022021c3171176fbee1666de5021b5c906f4e96b06a070f923340314ac3eefcbbecb473045022100a49193ae843589dae175dee737b3e1a829f047b4cee079dddc653ac910a19bc9022015a7d9736453b01dfc4838f3bb2fc849a7b5df4c3e3713e5520f7b9d061490f6473045022100bbe6d9751bd8f805a5a8d934bd9ed23a47330b50dcb2e68fb3b0daa8eef4f4d6022066d35a3767d8724538890cf9c60c0514508cbc733fe6c1067dd98fd0aa6f06764730450221009e3a2cc205caba746330f28ebe543d5af9db4e82bbb48dd907d7e7483e5e54fe02205b675dc8107743843b6ef400b3ea702b5dd9e7891e143508af4372f59bd9b57946304402204ad67b9df473bdd68ec14c0b3c7ced4de2e7255f78331d1b23566047b59767df0220431f161b0824e34bb6826ae47e46e371582139d849bf66de43b84d394c859fb6473045022100daf6e7eb32f49313af8c8652831f7a70f91bd9e2f988468f6a12dd5bb35cc23402202f9eae68b0845f7c0aaaf8725e7e570b90f359e2455fef2bcd090b5f12b01f2646304402203461bc5be616c4061cc36137c4211ad8ab4006d202b82285523c569b45d109bf0220269f8646bedaaf12b6daf79854f86de18020096ed54ecc55d4222013de2be9f2473045022100d81a967d5ee4ec78082606f87174524220fc824bfee6054b2404db3bdab1ff1d02205ae231c6a6310323cd8dc9186afa870e561b9a6fa3836ed7ce0b3bbcb5676af64630440220556fbfbe18852207d6e896bfca567bb8f5771893be91804668b22fd1d8ee79a7022023af8a48c615607a267107a0b1b9eae1ce95ce4385140973fd6314c5cf940164473045022100d02191308ce1fd29343b098b8db5c8caab3d286331dd876f5a95558b2aa177d402204e09a22b54e21d23d299741d81a2bef6345006fc27c64ff0c403b343d44bf6104630440220584e2bb88795eb4acadb6026363caa5e97bdb3f9aacd791112ba4d22eece6ecd0220111df10328461613a3172c419f3047e7e04b1c251b3c7f5bdaf6f711736d5a7946304402203b1e606dc14c3e401807f3bab08d7d13045c14f9da1280cecabae1ae70eb414702200e0630d4d61a38b0065379a1d6dc75b297be592d6cd20127fc939c136d1a455e473045022100f653d2d7e3f4f56c9aef2b1ba28b7cfb9b6bde980b7f1d52783a500e5cfc628c022055512b006f200cc5df2dd7b94573cdb7362b72f2b0d674a1273f9e453b796912, 02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d0f46304402200d4f1d96ad9f4de7f6dc79aef4ca3e7da4ba4677de6290e1a4c79e25d23adb5d02201165dcd0dea6c5e6de57944bbe9e9565ef253354436d96c5fd0caba08f72c7ea473045022100b55e9d31c983865f63a48f14b5f42ba64a474c72dd42ce634096df0afd7991f50220185667bf35c707d8551c4daa681d90812023253e1e80f94950a773d20ca0a54346304402203b48c700e6315239e83fac3975d1ab1c66b8e7244cb68d426caa3e01288488c002200f20c163169377c609b6752102e8548231f5e7c66c548729472789d7a2ca4611473045022100b27948ca9a8da0d0ebce2ed0a593143cd2a8beb6c40e47b12d6fba3225c27b82022036a19e968e7a226473bfb7b80537f2ddac16f346a751d11a71e25f63f2e5a656473045022100e6095c204d47099ccc8d4af3169c2420c1d0b1737f7001041248a0a84f411237022051f378c4cd21fa346e0834a4ed99abc717f795a9865ef77ecdab3e7c10107c61473045022100dcb2f1b9e6cce25305268754a1b9287ff55da44cafdabf8289b85f7f82f3d89d02205bf6dd171eba497d851ab059e2f536deedac6630d850f693ceacf9c2caec69a64730450221008d6fb8b15826c7e0b08a8a658fe90a9df6fdeb457e26a52bfdeacdaaf6bac03902207adf81ed0d00f4b88ba529b46c6df2db8a4ae6e8ff4534ee5bc23f758f1febe246304402201b9ad60525c731a0b0ac32aa3119c3d829903246c73686f5616d02c124903cb202206877596ae734ac933809c1f6e6ff6ec127613cf9bd24a3fb447bd1181fc4ecf246304402201096baa581dfa7b5eb45b2d9518fdaae5cc56cee809142a362587ace255e4db6022000e3e80202897da84aeced87ba61c8cd9cc9871c33ebd040c75117678974f94b473045022100d302ec70bae224754b0f1d17279f2124136605379e55592cb1d5fc8c15d0403e02204f963d514af55327fb990c036d485bd57b59f32434eaa7d7478132b9e4d78e0246304402201be5fc85b9af376aedaa2dcb09255285060bc6d31e9f0098556af56da03f05a102207b56ab79591b687509f5134effea8835a92ce7c0d27bd3b52c52fba0950b7c23463044022077cad790efa4e40df8a5ba03574a68afa9d9a5863904bcabaa9c23897fc1b7d602203ea1eca844d75f2bbc171512c8e0c3932a92c1a9b81acfff78287e491a85024746304402205d3158adddb8b2def44f474cf1f10575c83cc5c40e29d5cb6e157ca1b81ad21402201c84339b859559d07eb2a3a3821f6d3106472dc61b532e16fbfadd39cdfdf986473045022100fae7922224b2731756f2904dc8646f539108f1e87aa4db5809965885ceaa3d8802202b084688bece392a697fe87ac8f7cb6e6e9ae70f2f7eafffec4b8971e2747500473045022100f4597bf4328791d7b2fd9e65de2f22ae9e111c2a83e9be95735a0753b16783e802203042059e826ed1e0fc14773d73d2e04f8816c8b3136b626ce0769786d3d51d10, 02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f00f4630440220332b5210e89928471ad2185d10db234e1c2d4f32de78bfb334a64ff2d45d48570220396c69cfd82a28b75251187dddf4fa74258b984289488806061235442215d954473045022100efede499da788cb73efcea762037852e43a6d0bae3967817ff862309875db07202202f60e696166f4ca6c9684e5599f169a1cefe3e86248fb25afa7f84bc64d7e5b9473045022100dd57bddf1c6a9bb8078b16e43e9f1081c66b6fca0fff8e061d1ebcfd45709c2c02206f2c0cc7f0cf8d5d463b39bcf6e3085189102a7da6303713f98544a2dcbfa86d473045022100ff799532d070b642204d4f2cdd7106cce85929cbb4594b3dcfb9d8cd33b9c1b002205bad612c03ad3b8a1e3f326f28782c1865b81175c37e2dd6597aa51215554f6446304402205764dc29b8ddc48886194583db460c66e5391f2ba3ec77b34bfd56c978aaf85502206483db42615bbf10d08684309636d048ac14ecd9571d8b044a2c636012585ae0473045022100b6ec5379ad34cf6a94b549ddda2154661f8be6fb70c974dd211ff40b99371e88022047bc4871ea4d14fa874cf680488e9823bb55b1af1a78cd16d9837495da216ae24730450221008b164f576ee037b250822e0513228f3705629e8821d15163c41f1c46d0332006022014a3b67aaffc2869cd611110242d2f0e7ad821b2d8ad8ef9e2ebfaa8d091b2ec46304402204c0001b84a77747a8f28d8498c0c4a18b35ada61a6264a1103e28a23f7681e6d02205d91041a5a94f242ab8a96d9cf80e4eea9e16c3c8aa0b86381ce3284fffb196f463044022012e32eaeb9df59158a299b0f895cdd469fd46771c969c2698553d5d94a48786f0220274f8b3bdf82992bfb9c3ee6da1775f4b0a2782a88b5f75c233c1c8d1a088cce463044022034199f80246ae1eb597a26b88e91dd866d77edc93b302df7a9bba3743c71fc5c0220136d38841515806398b67a9f1c1682662ac1c48704e0b01c69e0406a333b84fa46304402207f9121811d1bdf6b13a7c1592996c1fef88dbf8e8ce90f1a833d07f9034622f40220281515148c6818566d9dee830df2072df3515c31fd45353e70486763fc94f87a46304402206098257832bdf14373f71a246f484f12c469c258b53319c2dd7643b95d11b63e0220481d1918d091a2b8510997605eb3cb7963ca3b5f63fb6782676501bb1bed51b746304402202faf9638d593f581bf0149c1204d34a7efb480d184412d178d3e6a50df0c8d4802203d5c2b006824525d792d287c2a6223c8e2dc393743de8b7c8e8b1cc9ae52291f4630440220797a1f4062034e5c2558e32f4a574a667e033e88c0daf9901788a17b3e692a1602205e7766f85012afb9a8ff5dd49eb5d82d5ca209699a0edeff1ddc9c5ba71bbb0746304402205e7ce30b20a10b19e90496344f9bb981a0e41aae37c7e604e23930dfe649a64302204b82bc70c6fb7f3727878e39e9d758d5ccb82c4076d0750d019769798a69a4dd, 02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a030f46304402202ba1561247fd8f891aef85c22b21be06dad82a3818e59cced433cd3266a3e80a02204bea1dc28168099f695e91f34561bd40781219a12e4ec9b46d8c595825765537473045022100fd8761b62386c56cb0029297cf950576310c436b27d71baa99aed68f91fe48a40220249c40641dd5e768c4150f5dc85063a656837cb3c8cad1ddab34f4cca35e36414730450221008440acc1d85d00e0d0ad2f3f462bd3991f8a31a3aef6076782512f5b86cdc07a022025e875b981c3f3f8037a4d1afa18cd27f1489a3ec6ea3134e18c86e2b165db064630440220498d714c7d12e6a4b23397cc0e6c8bc7c9f52de8535680092b4954f4b47061bf0220601ea42960475f0e6ceeed3ddf043d856ac0c876aa881619d41175922b8002bb4730450221008f73688a0b05f910421d3c44245b7ad982c9e9a95ef0b267af9cacc79a3bf53a02202c4b8f66dfe5844e2b46cbf70aaa8b3fd5737e800904475850235026829fd03c473045022100bfddfef03cb887677b642c2eb534f0b33097751ffb07747fa724c0fd0db4642202205d87a489058e701cabd037804966dfa185a1d3cbfc0186abed4b9365bc26eadc4630440220095a462494a71ad3e8854d4fd3f21be429e526e2555cb9322ac06b35491f9e0e0220360c230e574067a909e381294245f51e4996e96a39bc76285cf5e76e56144760473045022100f965ba8faba6581d83010619e7880eda9a08674c3a1d7c11aefd9e8eda5e153602203545ead786246bd0773c106d54a45b4e9cd007d3197c12b6386e4c1a3e96e9bb46304402201dbea4e12eec045f95a15f18bb992eb99f88b97d79c676489e1b8ee0a26a088a022019bb597807a9b64c78c9eb5e05eb2f6183cd86fcb8a22a16d36afedd0fca19dd46304402204a668d1dff27627c4649721a21014310275ac472bf21d486e4050f121114a0de0220383a39450b9efbc7a69094ba4580223fdff74b2c8292424dd877278246f91a5246304402203bb98194da8dac68fda24418a3452013306accbf7a301fcfb0ac7c8981fe8011022005df45391728d136671dae69c186adc858b04d1a286e4028ad0b431cb048f7924630440220375280f6ee97238d4c2d69922b3f8f707c471413f819efd30979dd98f93c532a02207bf44f5bedc26870970079cd9577a8bd6209a3fe034f4970f917f42f5d8028e546304402207f21c9e9e1cd46db8b9c34247fbcd1df00331c9442657c0fecc1fb489158475f02201694579b160b22a700eafcd4db379181178d4148cbb96c6fcb48a95a7ae4d300473045022100fb09b4ea0ef5c58d4b3956707de3921a0afe79e7bdefae48c124f5a27f59659502207841d9a4d7acb63ca902d592e08d657b8649fe78cbffce496bc8ac7d1c966a22473045022100ae94c89535e052bb9e9536f74e189a0039d216e52c33822c2152a63a2fefb8fd02201edf282d9a364b11fb88498e992781b85ac23fa450854f80a39c9189222a34ee, 028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b70f473045022100a1ef88477490b0e44a80cba6714bec67eda7db288bf7ab1b65dc2b901fb522ed022015a19a455a9a3c0a14c95967f6c3871b775ed90742f328d0f0b0936d16caf383473045022100cf48f055ed098d4977443e3c59baccbd2a7c444d5ced81e347878709659f9d2f022050f0e6f54a2fdfb401a8d2893897b53051c3a9a65317b49853d5905b21cbd25746304402207faa4bae863b7e67d58c15dc6d40a652b6e0d5cb853d0b487c2e87810bd4203b02206f7dd263a94c87fb0f8303871e009990b3916f6582e9f71eb51cb596a08634fa473045022100e2df4ce85e84c8ef8fbbca44a0ef95fa32f612e01b924949beab9b4d258c82f802206192b909b14133a780b53d533102449b1ff1f2578eec4e09be7c62960821a3d0463044022040a002d18bfd00cf76b0420550a58eb516a77063c9128879d7be8db222b524cf02207c132d514ad208a643959d66edbc0346b8dd5541149e49af2afc2a6580e7f37b473045022100c34df8582589335e21fc51a26f7e8a432a65e08cacffc4d91456336e7c2cf31e02204ab374b84bca6fca77da6b459f82d8aedd6b14ea50dcba296530838944b87edf46304402205dd313f5cc6b6bba1b18900542fcd9d665ec738890d71fbc7bd068f46f0cd7da022067e5e6d30c44d793f37fa875e0ff93282a59f697c5e625305fbe607ff8d8107c4730450221009e2b54c55c5116ae4f551b5ef77debe02c2757745900b0aa5b62256f6cbf01880220051f9d516df9b220af1438e999ba6b1c7863ecd566e22d0e59a863ab6580f16646304402207bfab64ac52d6e014a1a662ba8cadf2944ad623c5b2ca42a7cbe1fca3bd1200902200a304ef2e9dcc0638f8a9e8003e5ade3654f5f759c8d64a328f78679e3e22d98463044022059ad1d1b45e135a776dc9c8f92da39c6e843a53431f3b6aa8d09ece39eaece5202204e939162ef889296a369b1122bc679cd0b8492b119751bdbbbc01103c9db0f38463044022019c8728d7fc4260d1184434e8f33de37d141d016454b312bd9e33011904a559202207cdf4f16caa4b49bc752a2f981eacd36484bc783062919309a50e05e98a089c746304402206b6e350ce3bb8bf4a5afdd5a417d6c9a91180a6bc86b6517b1358653788e51fd0220042495bd96fc5f0f0e58387cd60cd023b5c389e348d72befb85127747635dfa546304402203be941c6255a0a3d7ee5c1c52861e3bbf639eb9b409f537b629d10b560cf9ce702206ec2b1b1c0a1ad0ef37c55b09d821fa8149bb2192a6067e7294c0a572d1413f3473045022100afc0f6c86770bd25ef265673aa143e5fc7b62bf0701671e48d49fd19013ab50902204ffc64bb0c7fe4232427de14a325a5626486361d4360cdd6844a9dcdc7789321463044022019dc2dd4bc2bf67ea3de69b1809272a7450baaf8e5d6f2eee7c87001ba7463d402204ceaaaf15985d11b91c4eda5abfcc9bf14adae806b18a5cbcc2b6eb9e9fe3e2c";
        String signatureDataL = "02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd0f46304402207ae983fa7ba344289f3e13b293d75b00819b02b46c2a7a1b19c3b4820f5de67402203acb0d2d094c6259579fae07b668f4e550e5a089cea6a1a49c7d9127bbb38c8b473045022100ba5caaf844b1223e8e993a9b22fcf01481d9c26ef70760ac690aa42a484f5cc20220282e1085dcd444f5810a6da9e2afda44947289c9719bf5b3924fa5c4080e1abe473045022100ba0a6325e463584cdfd0ee5d9ec12c1b3bcfcd542600e4734ffd7990c6452ac7022011a05995c8bb8a1cb7f3c28a089c2727ce495305c876375c3a17c22b5bb25e8e463044022100b14dc0ad2ee0a5d54283dc84b84226bc4d730ba0b2f858efbbdc0e516b94c72d021f6d52493c175ddcfb56701041cf31c7e21f68f48a27743680517458a3f07828463044022003a4d482e5139e9c22a239b7f8d776c47a4756d3475ccc441f73111ecc870f22022008a9f04d3deec06b9009048dbd094660d9893cbfaf9681c02ea4810eacd40f61473045022100a62b92d97660fbc67c1e05a590f6999643d71ab7ea400e960687b240dc882c2402204c42aab55aa71c7349cf4db8e10daff83933bbeb3beed263c6224300774c28fe463044022060ae53c8720899f4980f9434708790a90c8b02db820d0e36839a4c1d80e4409a02200e29cf43a091b5fe98d1cff47f2ff178d873b2bf603b031b29e0fe70c5228af1473045022100992182139d45675cf9d37516f8955f583f2c4a310aaab3ade112dc04bb66122b02201519fd85f98eec27e4ec4882006284c6797294d7afb8ee7687705aea7c5773d3463044022027f0aafe0ee9b8851f2516ed71f2a25ab97bf25134b96b562716148a2c1433f502201906f36b8bc0773de1972a51a6964175a27e8538c93468c6dc77a48fbc6ac4284630440220676727bd800bc0be80250b9d79600f3275ce43721416c67da3c4a71e8b3b7cfb02202c38e1c7f5b8aca2f98b741ebd186577f579c10ff8e4c3ca5114728cf12dd3bc473045022100eba0ce70bf18408d17d77048baa645cb66aed0e9092de59480d1e98be43f7a5a0220040303f6bcfe460217ad46bda269872c779e2d360ee9e52b5b1c1cf08959cc51463044022004fe7a4c8835525e56e33b3831900292749276e9323c92abdaa016f364bad6ce02203e800cfecec35222f20710ec4d2e948c3e82463fd93a21ee3d83801a4f8665f247304502210091e0c1801eaa806f43a38051276a569a027ba276fd65558ec6017a284961a00b022003b922258396be5da265b347cdf450fa143f830cacf76cc8762c5eea1e3360a34630440220673d87c72f0266ce47480e04c083530f3ac3c220cb9a35f01a08b9743a40aae702205927899f3b2e3d728806fcdc935a9a751207a3c144281fd5ded511cb7d4eafe446304402201676788b942ee2a8ddd801f63c091d930737d84789045dc7677595e1ce42492802206ef6a64daac0b27992917cb753a943be866ad692464834f1c7802e2effd11efe, 03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df40f463044022038f6520732274ca637d7c60ef1a584b241a7637d7d75290e3b2709ccc973938c02206a79f1adfd57950a3137b3a05aa480a79877655d43d9641c52141f9c54a5c324463044022059ddb7d49b1651564adc97b2d9ea5dd9e1b3733712438680c0795266ab47382e02202501599810f8f1fbc09bcbd55e8da8e96d82f75ef396e7a4bc59e8b7c946e1bf463044022049f920ca60264ba1674c0038c01963fe1a4341f496e6258c653ae1277348cccf022041ca7286da2d7fd454bc7bff5b61165c3b69aa163ab3be127cb1af82f8171744473045022100c004df80065bf94434d8c2fa8238d2f092e90d19387aa655567d97cca954f687022048ce1154961c6a185e20b68984e6ad894d9bb9ba4d3f47444bdd4c91beeff714463044022055dda8a1824c7cad17e7fdb1a7e5e5128d0c2642fbc222ca82306b2e393a217c02206c5fd7850af1a9823e2e06456fb30a6d88fcabbe84d15f6fbadab121136a51ee473045022100e027ae6655a8bd3a46feeb36dd477d1f87baabe09792138cc39c45c41f6639eb02206d6c1d1eddee9664338c6a68ce9083cfb506c376124fefe28e62ee97a54c54d0473045022100c61d83a2064901985c8b35ccafd31a84e659f8055cde116c1f613ba54eea2208022026602bab037d3d2f2befe7e6edc5986a324bd3b5f7fd4e1f51ff3475ff98da86473045022100fad0b898820b24b1ca3c0ab4d1c31a4d749d8c4ef89f3f616b239fe5fbf2c244022041df859c65c68deddaf08f4948e3cfe5eeaa36ab75a4228a8c0515720ec1b36a463044022020fe02b5497f6ed60ad0e1d4f98edb7eb6d832b36124a78af9ac985fb1bbbadc02206a8ce0268b5710673ad7e1a2bf078e54a204ea2d48480bb2a1183ea0fb7a814f463044022046f39c2a5e4c4e3ccd6c96c9167f0e33f3fa28c5468b85e2a258c18c737b7dfc02205da01a04f9378c418fa9a9dc4baec34f2b0941914b0200a734686bac574ecb78473045022100d338c6bf56711f6c4207c2d053cb11047b8c157e2fa0baa20ede6ad8585400780220018bb8dd626308a2a2a4b54ccca78304c47ce78501241a3c6737a50b03306fed463044022034347c670dfd9b0d872f8552522e1864c1dec42efff206c1289539478f9f052202205176bd22d583a53fda31654e51e7a38effb33cc0cf188df4e86988c738448d3b463044022073163bd025d811f9d92280aa48c73b1863a7c7870788f4bf264e2c01c5cfe91d0220694c54d76cce9c94159f365b22cf6f755fb7e714c990e40786801666e6ee5821473045022100b4f99f3a6a46d69d3dcd1382c1626b51e6e683ed30b5d0a278448839efeeeeab0220228f7ef6cc41cc99ed07e60308d51a24de312c0838042cd11f89644f9df6539e4730450221008add9f25ae96c913396f4cc9beff038c22f794dc9242822eb945406efcb419b3022030a097d0869e7456963ec17b6952be135d0f796dd9dada0b9a8983f38af24a5d, 03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c90f473045022100917db7a8ae3b82de0297d22bfa98cb896e95c2d00e0b1e35cf91b592c6911a6f02207def729a70197b993cc834d3f9c8ca4c21900fddd50259fa765cb57ed18e5b2a46304402200949cc1953ccf2e3c77b7b05e6de301d2ff6b3ab06e6272a931342b50292ba5702203db24b5cd146d210b81fd9485ac269d291cd0286eb1c5fb27c05e5dd0bee510f473045022100dbd3f2fa5a911322035cc828c5afce61a3cbbcb5c5a0bd409fdeb7bab3b1ac8102206ec30059cea447e2e6bd20346edbde235b668d957098168e64b39195899fa7b546304402203bd1efedb089aaf519457a6ec933e76f02b84925fbb90a1468461907067ceafe0220345f7b5f3fe7ddf2bc2d4ba346b9f9e0c9275d28951dd3b05dabb90e40a68698463044022022d35a13bfa132715dfae46d7411fc75f339c13c267650ac3f5bd19e5fff760a02207d20ef8bfd1aa2a7ec5ef773b0e570f660a752ce6fd55d7a85ccc47968b4ca73473045022100fea82cea97f62557b33f84d855ec8e5365876d396a4966e899d95eae78f33c6f022062abb4bf22824203c195cecba5b118fc1a8652a67739757d21275b2f85a8ff304630440220711364e958735feac2089ccdbcbd8acf47a21f4be452299cb293d2143cc8b26102200833e5b1c960deeb8b1d654c6f57af2d50876554d1e1bf365fc1081e144b2f0d47304502210082debf646d9cb351152c2e3f26d77fadf2a649c04a7f2c286002fc6f559c9bf7022022f8d69dd094721e43b030ae3e7a5972916cb1380fba4db8ea866ae005701b2a4630440220344f642f9ec019e86402f5acc7df39cb7cb55b8b3d64a22c38832937fcd7d94502202756b83f46e6d89137fb45616d3d3009ebbb225fd31449de4c421093798aac0346304402201c57356359f27893a41d69cd21b84284465a4d34d177f6335272251384b1499702202fbb41cdd95f26e66ba69c4b407192bb59099e7257259afe211d1cd506bf339f473045022100937bdeb99805401d28656a2d3de4e29873019ea8b47d7f6628f1a157dec09aaf022018b6b224c84b1eb0ab1d4067518a30ea1d7c039c378af8f4c8c509b419b4021946304402204bf6761d289c4dcf209d581089372a715a48bf376378bc742445cca7b019ef8702207fdd1f1a3bed5c7310814e8ccc79a1c38361882e7aa9007e9a04e69471ef486346304402207a5380889658458a0ff997d6ada96ebc398cb9af54b4836365d59eea2444f6fd022023e93dff22026f0b5ecfc3db22c470786a29dccfa6b42e169f3b13c9dfea348246304402205c5b8ded4f6a25fc7da07984e351590174fffeb4a9efc2c548dd9632f2418c1e02203288318e08a05f2a6f2f983f94185743c816fcd42a950fc353341aa3af47e04b46304402202d3c75ce85811f93b578376e4d3c17e6b4c20779535384e2d5041cc782204d5a0220020c339157b5fe07eef900b0182aef9319b870b0a1bde235d88b297eb2edab86, 035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b8030f4630440220388b5eca4fe1fb2d3249bb73d3fca58cddc8e828cda31d021f077a8d9f89c28602203d05f81250e3490fa8125064c385b426b45c4a841529940be0021b9c0328c4e347304502210093d3d8491ba9173ecd9858c64ea50e02ecbea21844efe75217082cd2927f0e7402207833085dc2151b426d4a7dbcf1a2412c409e7a113279bf489d850af20d3d0ea346304402202753bb01134f0cfba018559baac94f4d9324f2bbec874c9e6078671464f80b8402206d3507e523d2f948e031ab56729b071b4d13db4d7d872ccc4e97eac185a999c2473045022100bf959dfc3da2b504a201e79ba40505b77fd9a13561c09c1927a7791d50b7d4a30220505daaa895577303dbfe9a11a5c0ea2281f3bd92f2bac9f4cb7c0909cfed9898473045022100e2e96e5e527d2a1cf6630539d77a681ebb1903e1c3bb1f6d4e7085d04b3c1a3d02203b14f882868f666675cfa11cc6f9914999a5701a175b3578870ed95c13dc361e46304402207fcdc186135c3c697406769060696a762423361ca6751e59400cee59b214b57f0220098a1c13b69589f7db0ff2edac9ac99009c2783ded7caaebea3eec696fe1ba8a4730450221008cd79d1da8df687df982ffff7d4e2f82cdba70ca4f14a4b2ab0c46176565624702200923d36a6927f63d7605559e6d2aaa940a4e9065da6da3b3c7eb9003854ced5f4630440220240495b0b8b811df166ae3b3b75f989f7b72e13bb7cedf9da93ed2867307490f0220094d7b16154a0fa04df273f5fe32357606b4789578955386a38b6be74d23c00646304402201f38673c66471382347535cf26ef6063b0872bc6cb7cb6248dea81ebf4087c2702203dd3ced2cb3681773e3e805422987d36fca97b7ff7a37f30ab3169a3515886f84730450221008f2baff2674c211dc837dc86a04deb68d80a24efd3b901e01c2df012d1532a7b022010920339c3bd4d517d2296c7a8346918b6388384d0c2b7e4b3ec2be6577b2160473045022100f313a5b6beba80834495b635545753368fbc2b9ffa17eba6f36b739066b8345402207270e8c1c6641dae60a275df31fa23a967ddb2b7a201c7f3015a901e947ae99d46304402206f9c3d452fda598007748adef9420c11c26cd280571e788ce2dbdb9485db70000220162ed78fc1d860fc87a935f497194f11029879eceb339a214084d8ebd9994f63473045022100ff9a3c7c152b42fecc531aa3988a20db247a0afcffb040482e8329375223a9f1022066a82d74339f0984393eec4ec3fb8f1c129873eeaee8819eda853cc0d5f763bb473045022100f4844c451b9657a55570dd3808b5c27d9997362b4c393adde745c9073ea42c1c022049711be459076b6a55e20d5ef3aa25481d950338e2cb1ae0daf7e97fe9743ae946304402201cc070434f456c764565612f5119034bca547842a685f7fc9c4fa4233b25479f02202174afb4be88dae1f3572d971d672cc93fdffb3098b0d7b898db93f9f35529b4, 02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b90f47304502210096fedf98db8e18dc07ba9ab4a3b6c37954de0a860ba786348fc2f147f5c6cc13022027920c61385eb36ef712e51019120ce811ac1a4900444143b3fb3ffb0a1f57614630440220058c6a05d5a57499f920f56a5afdc363a9adbcdc8dbf89da3749bb59b1db3d350220591a98df65fa6e27193ad1079a1b34899ff540a2a123b3390783dd7564415f58473045022100d10f5b01613e61b8efd4da1c3169c880d575c59cfb0bcee7c0bdf168a0d61e0902200684fc776b05126e2fd6102096ad0f43ca3791ebb9273b8683b57641a08bc05d473045022100f9b6b6fd333d61b04145b3d28017d552266a4449432654498812c0e5ea80ee810220047b4556582f208381e1ff2d2280272fe1105281aa86b9c56a7fd10290783e94473045022100d0c872f61367a9f936a5f2827e10c28145c3ff1ae52b4a9e5e02490d13ff39cc022044d8db5d051156bf9c594039aa385c4a67efcc3cc17ddf56a65c415e1f73acef473045022100f1d34fa0fb7c7d845d2d10c6133e8b2e7ed6bc21529808143f4a2dfb941285ad02203fdb02fdad4ba6081e4aa18a8c6c18381b5198752996993de55d41cfe92faaa54630440220053d560aee73e419505688b0121c6e035bc07b9e168ed99d4d8999a94bd0945d0220390c20b18e51860476c6f46a9e4b0cb019ac5db9b8d9e7f8f2d979542e672bb14730450221009d325d25640cdb9aa62fa022155b2f8a67f9a40f3dfdc57049cb3c3076ee5d580220217e4a51236d4fe7e8279801882b1a9b3d0e2f0dbf07a8b0555d8bb68f45fc80463044022067b079649ff2e806ce4a4a286f1d0d25055979d00e6be8acae23c618154f42d202203e29935a08457b815dd189d7c191e4f56dbd9882cde6291a16e3cd51760232b746304402203a9dd4977d13f8911d84c8a05fab07e4434e131e052591af3cf0d59da8426f7f02206b910f8b90ccf506e05c08e001b96599787cab2ae424866bc7cad2bbb9481f4947304502210081588a6b204640f9a0db81c7559e43b7cf61657b7d84642a4d7a6521f2a3c9e6022037c0d870e61dfdddcbcb760e84863ad42d39550cc91f44b30a384a8b14e307b5473045022100debc42d763dac111c52d21bb143956e8969c5e4168a4b17e354ff2d3f709b20f022034aa5e1dc53e69c6d043e511af172087f24fea74256b7a6cfa8de37452eaf92346304402202f5c1c8364c15da341f3260b08124141272406e0584e888e6c16dc1c1c8d365a02204153024d03880219c87d90c0c8e22e0f4e00e37ab4228711f005b56daec94d3b473045022100e65c75f81b56e13c7dfba75b356743f309a72aadf054644d3bbd8ad2799d4e91022015a28a44f9c973f8e43ec38787b7c8dfe8ad1fde97b1925d6b57cbc469f4fe7246304402207d2776440361bbd7aa60bae3c7ccdd722fb8d1d8f2508a7a69f35a5a5ca5341f022007b8c54d2e0fee835e13f4399736ce4ef4594c7749580b7590902b02434965b6";
        String signatureData = signatureDataL + "," + signatureDataN;
        Map<String, List<String>> signatures = new HashMap<>();
        String[] signDatas = signatureData.split(",");
        for (String signData : signDatas) {
            BtcSignData signDataObj = new BtcSignData();
            signDataObj.parse(HexUtil.decode(signData.trim()), 0);
            signatures.put(HexUtil.encode(signDataObj.getPubkey()), signDataObj.getSignatures().stream().map(s -> HexUtil.encode(s)).collect(Collectors.toList()));
        }
        Transaction tx = BitCoinLibUtil.createMultiSigTxByMulti(
                signatures,
                currentPubs,
                amount,
                toAddress,
                UTXOList,
                opReturns,
                m,
                n,
                feeRate,
                mainnet, true);
        int vsize = tx.getVsize();
        System.out.println(String.format("Tx vsize: %s, hex: %s", vsize, ByteUtils.formatHex(tx.serialize())));
    }

    @Test
    public void testSignBtcWithdrawByMachine() throws Exception {
        List<byte[]> newPubs = new ArrayList<>();
        newPubs.add(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803"));
        newPubs.add(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21"));
        newPubs.add(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4"));
        newPubs.add(HexUtil.decode("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a"));
        newPubs.add(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9"));
        newPubs.add(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b"));
        newPubs.add(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d"));
        newPubs.add(HexUtil.decode("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90"));
        newPubs.add(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"));
        newPubs.add(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980"));
        newPubs.add(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0"));
        newPubs.add(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd"));
        newPubs.add(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4"));
        newPubs.add(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7"));
        //newPubs.add(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03"));
        newPubs.add(HexUtil.decode("03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9"));

        List<UTXOData> utxoDataList = new ArrayList<>();
        utxoDataList.add(new UTXOData("2628825c309dd7320900c03071a9b71d6ded3d0dd508f9fc246ea1590ad1133b", 1, BigInteger.valueOf(100300)));
        utxoDataList.add(new UTXOData("377c3c3781737b791598f64d85a23c003cad81f49a55544ca263eaa8505cc221", 1, BigInteger.valueOf(20996657)));

        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData(
                "aea6772f12ea04bb8233b5621a84f77a58d507ee9b269a401f424f9687ca1aae",
                201,
                "bc1q7l4q8kqekyur4ak3tf4s2rr9rp4nhz6axejxjwrc3f28ywm4tl8smz5dpd",
                15,
                20,
                newPubs,
                utxoDataList);
        String signerPubkey = "03b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd9";
        int n = txData.getPubs().size();
        int m = BitCoinLibUtil.getByzantineCount(n);
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvBitcoinSignWithdraw");
        extend.put("nativeId", 2002010);
        extend.put("signerPubkey", signerPubkey);
        extend.put("txKey", txData.getNerveTxHash());
        extend.put("toAddress", "bc1qyzdyruj0f8xd90zjftfhxrgev3sst3jcdjl30c");
        extend.put("value", String.valueOf(200300));
        extend.put("m", String.valueOf(m));
        extend.put("n", String.valueOf(n));
        extend.put("mainnet", true);
        try {
            extend.put("txData", HexUtil.encode(txData.serialize()));
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.IO_ERROR, e);
        }
        extend.put("address", AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), 9)));

        //String signData = AccountCall.signature(9, AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), 9)), "pwd", "00", extend);

        //BtcSignData signDataObj = new BtcSignData();
        //signDataObj.parse(HexUtil.decode(signData.trim()), 0);
        //List<String> datas = signDataObj.getSignatures().stream().map(HexUtil::encode).collect(Collectors.toList());
        //System.out.println(String.format("pubkey: %s, size: %s, signatures: %s", HexUtil.encode(signDataObj.getPubkey()), datas.size(), datas.toString()));

    }

    @Test
    public void NULSAddressByPubkey() {
        List<String> pubList = new ArrayList<>();
        pubList.add("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803");
        pubList.add("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21");
        pubList.add("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4");
        pubList.add("03743d75ac0ee77257dd11477a737bba2bd01a1b26246c477ef2a59fb8e873572a");
        pubList.add("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9");
        pubList.add("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b");
        pubList.add("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d");
        pubList.add("023ad3fbc7d73473f2eca9c46237988682ebd690ab260077af70357efcf9afbe90");
        pubList.add("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9");
        pubList.add("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980");
        pubList.add("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0");
        pubList.add("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd");
        pubList.add("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4");
        pubList.add("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7");
        pubList.add("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03");

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
            System.out.println(pubkey + ": " + AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 9)));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(EthUtil.genEthAddressByCompressedPublickey(pubkey));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(pubkey + ": " + BitCoinLibUtil.getBtcLegacyAddress(pubkey, true));
        }
        System.out.println();
        for (String pubkey : pubList) {
            System.out.println(pubkey + ": " + BitCoinLibUtil.getBtcLegacyAddress(pubkey, false));
        }
        System.out.println();
    }

    /*
     0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - AA
     02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - BB
     02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - N1
     028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - N2
     */
}
