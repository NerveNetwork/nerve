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
package network.nerve.converter.heterogeneouschain.bchutxo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.neemre.btcdcli4j.core.NodeProperties;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.RawInput;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.heterogeneouschain.bchutxo.context.BchUtxoContext;
import network.nerve.converter.heterogeneouschain.bchutxo.core.BchUtxoWalletApi;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.CashAddressFactory;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.TestNet4ParamsForAddr;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.utils.jsonrpc.JsonRpcUtil;
import network.nerve.converter.utils.jsonrpc.RpcResult;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptPattern;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

import static network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil.*;

/**
 * @author: PierreLuo
 * @date: 2024/7/8
 */
public class BchUtxoTransferTest {

    Map<String, Object> pMap;
    String fromPriKey;
    String fromPubKey;
    String fromAddress;

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

    void setDev() throws Exception {
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
            fromPriKey = pMap.get(evmFrom.toLowerCase()).toString();
            fromPubKey = ECKey.fromPrivate(HexUtil.decode(fromPriKey)).getPublicKeyAsHex();
        } catch (Exception e) {
            e.printStackTrace();
        }

        protocol = "http";
        host = "192.168.5.138";
        port = "18332";
        user = "cobble";
        password = "asdf1234";
        auth_scheme = "Basic";
        initRpc();
        walletApi = new BchUtxoWalletApi();
        walletApi.init("http://192.168.5.138:18332,cobble,asdf1234");
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(5, 1, "UTF8"));
        this.beanSetting(walletApi, chain);
        mainnet = false;
    }

    void beanSetting(BitCoinLibWalletApi walletApi, Chain chain) throws Exception {
        BeanMap beanMap = new BeanMap();
        ConverterCoreApi coreApi = new ConverterCoreApi();
        coreApi.setNerveChain(chain);
        BchUtxoContext context = new BchUtxoContext();
        context.setConverterCoreApi(coreApi);

        beanMap.add(BchUtxoWalletApi.class, walletApi);
        beanMap.add(HtgContext.class, context);

        Collection<Object> values = beanMap.values();
        for (Object value : values) {
            if (value instanceof BeanInitial) {
                BeanInitial beanInitial = (BeanInitial) value;
                beanInitial.init(beanMap);
            }
        }
    }

    void setTestnet() throws Exception {
        System.out.println();
        System.out.println("===========testnet init===============");
        System.out.println();
        this.initKeys(
                pMap.get("t1").toString(),
                pMap.get("t2").toString(),
                pMap.get("t3").toString()
        );
        protocol = "https";
        host = "bch.nerve.network";
        port = "443";
        user = "nerve";
        password = "fc85006ba5ae16c";
        auth_scheme = "Basic";
        initRpc();
        walletApi = new BchUtxoWalletApi();
        walletApi.init("https://bch.nerve.network,nerve,fc85006ba5ae16c");
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(5, 1, "UTF8"));
        this.beanSetting(walletApi, chain);
        mainnet = false;
    }

    void setMain() throws Exception {
        protocol = "https";
        host = "btc.nerve.network";
        port = "443";
        user = "Nerve";
        password = "9o7fSmXPBCM4F6cAJsfPQoQSbnBB";
        auth_scheme = "Basic";
        initRpc();
        walletApi = new BchUtxoWalletApi();
        walletApi.init("https://btc.nerve.network,Nerve,9o7fSmXPBCM4F6cAJsfPQoQSbnBB");
        Chain chain = new Chain();
        chain.setConfig(new ConfigBean(9, 1, "UTF8"));
        this.beanSetting(walletApi, chain);
        mainnet = true;
    }

    private BitCoinLibWalletApi walletApi;

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

    @Before
    public void beforeII() {
        try {
            String evmFrom = "0xC9aFB4fA1D7E2B7D324B7cb1178417FF705f5996";
            String path = new File(this.getClass().getClassLoader().getResource("").getFile()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getPath();
            String pData = IoUtils.readBytesToString(new File(path + File.separator + "ethwp.json"));
            pMap = JSONUtils.json2map(pData);
            fromPriKey = pMap.get(evmFrom.toLowerCase()).toString();
            fromPubKey = ECKey.fromPrivate(HexUtil.decode(fromPriKey)).getPublicKeyAsHex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createMultisigAddress() {
        // 0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260,
        // 03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5,
        // 02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750
        // testnet
        List<byte[]> pubList = new ArrayList<>();
        pubList.add(HexUtil.decode("0308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db78198260"));
        pubList.add(HexUtil.decode("03e2029ddf8c0150d8a689465223cdca94a0c84cdb581e39ac13ca41d279c24ff5"));
        pubList.add(HexUtil.decode("02b42a0023aa38e088ffc0884d78ea638b9438362f15c610865dfbed9708347750"));
        String multiAddr = BchUtxoUtil.multiAddr(pubList, 2, false);
        System.out.println(String.format("MakeMultiAddr (%s of %s) for testnet: %s", 2, pubList.size(), multiAddr));

        // mainnet
        //028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb
        //02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
        //02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9
        //02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049 - NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64
        //02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0 - NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
        //03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21 - NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63
        //02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd - NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49
        //029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4 - NERVEepb69pdDv3gZEZtJEmahzsHiQE6CK4xRi
        //02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9 - NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx
        //03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9 - NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE
        //0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
        //03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4 - NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD
        //035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803 - NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1
        //039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980 - NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt
        //02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292 - NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB
        List<byte[]> pubList1 = new ArrayList<>();
        pubList1.add(HexUtil.decode("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7"));
        pubList1.add(HexUtil.decode("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d"));
        pubList1.add(HexUtil.decode("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03"));
        pubList1.add(HexUtil.decode("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049"));
        pubList1.add(HexUtil.decode("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0"));
        pubList1.add(HexUtil.decode("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21"));
        pubList1.add(HexUtil.decode("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd"));
        pubList1.add(HexUtil.decode("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4"));
        pubList1.add(HexUtil.decode("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9"));
        pubList1.add(HexUtil.decode("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9"));
        pubList1.add(HexUtil.decode("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b"));
        pubList1.add(HexUtil.decode("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4"));
        pubList1.add(HexUtil.decode("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803"));
        pubList1.add(HexUtil.decode("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980"));
        pubList1.add(HexUtil.decode("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292"));
        multiAddr = BchUtxoUtil.multiAddr(pubList1, 10, true);
        System.out.println(String.format("MakeMultiAddr (%s of %s) for mainnet: %s", 10, pubList1.size(), multiAddr));
        pubList1.stream().map(ECKey::fromPublicOnly).sorted(ECKey.PUBKEY_COMPARATOR).forEach(
                ecKey -> System.out.println(ecKey.getPublicKeyAsHex()));
        pubList1.stream().map(ECKey::fromPublicOnly).sorted(ECKey.PUBKEY_COMPARATOR).forEach(
                ecKey -> System.out.print(ecKey.getPublicKeyAsHex() + ","));
    }

    @Test
    public void testAddr() {
        String legacyAddressP2PKH = "mtyGixaKTmm4TZp1GGc1kRo9kmo1seB92z";
        String newAddr = CashAddressFactory.create().getFromBase58(TestNet4ParamsForAddr.get(), legacyAddressP2PKH).toString();
        System.out.println(String.format("old: %s, new: %s", legacyAddressP2PKH, newAddr));
    }

    @Test
    public void testAddrByPub() throws Exception {
        String pub = "0347ede10670ddbbfea359242673169528d184f8ef2e586c15d686aa49dddc555f";
        System.out.println(BchUtxoUtil.getBchAddress(pub, false));
        System.out.println(BchUtxoUtil.getBchAddress(pub, true));
    }

    @Test
    public void testP2SHAddrFromSignatureScript() {
        // txid: 6de72a9876a521102796d7b1dbfb4beeef31c0c01f7bbca718ae5c0b5fe11160, from 'bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w' to 'bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n'
        String hex = "00483045022100b3ad10c06aee064504429346204f8e84b1ac4c790669975d8d5d887eee8e7800022007f6c261fab0c379e9f2c3c77ca1ca1abb9df0431bc9c6682d8b096db4041d2e41473044022035d89e937a270b9071c61bf10b63f1224fa78acd0d3bedb81f762b69fda8c3f202203b2393e4055f50672eb938060d405fde859db4b6c50415b8585929d6290cf385414c695221024173f84acb3de56b3ef99894fa3b9a1fe4c48c1bdc39163c37c274cd0334ba752102c0a82ba398612daa4133a891b3f52832114e0d3d6210348543f1872020556ded2103b77df3d540817d20d3414f920ccec61b3395e1dc1ef298194e5ff696e038edd953ae";
        Script script = Script.parse(HexUtil.decode(hex));
        List<ScriptChunk> chunks = script.chunks();
        ScriptChunk scriptChunk = chunks.get(chunks.size() - 1);
        Script last = Script.parse(scriptChunk.data);
        System.out.println("last: " + HexUtil.encode(last.program()));
        Script scriptPubKey = ScriptBuilder.createP2SHOutputScript(last);
        String multiSigAddress = LegacyAddress.fromScriptHash(TestNet3Params.get(), ScriptPattern.extractHashFromP2SH(scriptPubKey)).toString();
        String newAddr = CashAddressFactory.create().getFromBase58(TestNet4ParamsForAddr.get(), multiSigAddress).toString();
        System.out.println(String.format("from, old: %s, new: %s", multisigAddress, newAddr));
    }

    @Test
    public void testP2SHAddrFromTxInfo() {
        // from 'bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w' to 'bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n'
        RawTransaction tx = walletApi.getTransactionByHash("6de72a9876a521102796d7b1dbfb4beeef31c0c01f7bbca718ae5c0b5fe11160");
        RawInput input = tx.getVIn().get(0);
        String addr = BchUtxoUtil.takeMultiSignAddressWithP2SH(input, false);
        System.out.println(addr);
    }

    @Test
    public void testP2PKHAddrFromSignatureScript() {
        // txid: e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686, from 'bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n' to 'bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w'
        String hex = "411b9e714f61851ad4f2c93b5d2ada83dde3449c6758ae24a04a687296ae25497edb32e8af7b5fc9791370e0f7b73acd2e1abfc575233a7217556527abd76895e14121039087cc88855da54458e231c50cb76300334abe46553d3c7a76bc8af0852b87f0";
        Script script = Script.parse(HexUtil.decode(hex));
        List<ScriptChunk> chunks = script.chunks();
        ScriptChunk scriptChunk = chunks.get(chunks.size() - 1);
        byte[] data = scriptChunk.data;
        System.out.println("last: " + HexUtil.encode(data));
        String from = BchUtxoUtil.getBchAddress(HexUtil.encode(data), false);
        System.out.println(from);
    }

    @Test
    public void testP2PKHAddrFromTxInfo() {
        // from 'bchtest:qqla4yswdp3f903jfdpc6egfzglvmrs7nussfx247n' to 'bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w'
        RawTransaction tx = walletApi.getTransactionByHash("e0d26e19bc362696348cce25b7d5ef1cb5c47f140678c878a66d0dd62c56d686");
        RawInput input = tx.getVIn().get(0);
        String addr = BchUtxoUtil.takeMultiSignAddressWithP2SH(input, false);
        System.out.println(addr);
    }

    @Test
    public void testRpc() throws JsonProcessingException {
        String url = "http://bchutxo.nerve.network/jsonrpc";
        RpcResult request = JsonRpcUtil.request(url, "getAddressUTXO", List.of("bchtest:pp3fu5lfl0er99f37yk0ezfvjcspps57r5txs83q6w"));
        List<Map> list = (List<Map>) request.getResult();
        if (list == null || list.isEmpty()) {
            System.err.println("error data");
            return;
        }
        List<UTXOData> resultList = new ArrayList<>();
        for (Map utxo : list) {
            resultList.add(new UTXOData(
                    utxo.get("txid").toString(),
                    Integer.parseInt(utxo.get("vout").toString()),
                    new BigInteger(utxo.get("value").toString())
            ));
        }
        System.out.println(JSONUtils.obj2PrettyJson(resultList));
    }
}
