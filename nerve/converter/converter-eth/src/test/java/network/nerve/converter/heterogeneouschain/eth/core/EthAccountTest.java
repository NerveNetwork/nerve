/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.heterogeneouschain.eth.base.Base;
import network.nerve.converter.heterogeneouschain.eth.model.EthAccount;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.crypto.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.*;

import static io.nuls.core.crypto.ECKey.CURVE;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.PUBLIC_KEY_UNCOMPRESSED_PREFIX;
import static network.nerve.converter.heterogeneouschain.eth.utils.EthUtil.leftPadding;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class EthAccountTest extends Base {

    private int chainId;

    @Before
    public void initChainId() {
        chainId = 5;
        AddressTool.addPrefix(chainId, "TNVT");
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
        String prikey = "B36097415F57FE0AC1665858E3D007BA066A7C022EC712928D2372B27E8513FF";
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
    public void errorPubTest() {
        System.out.println("=============ERROR===============");
        String errorPubkey = "029686cb9ea3e4bbecc9e416e5e3a48a131fc559747f26d1392b4b13a59b334126";
        io.nuls.core.crypto.ECKey ecKey2 = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(errorPubkey));
        System.out.println(String.format("error converter from ETH's pubkey: %s", Numeric.toHexString(ecKey2.getPubKeyPoint().getEncoded(true))));
        System.out.println(String.format("error NULS address: %s", AddressTool.getAddressString(ecKey2.getPubKeyPoint().getEncoded(true), chainId)));
    }

    @Test
    public void createEthAccount() throws Exception {
        for (int i = 0; i < 1; i++) {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            String pk = Numeric.encodeQuantity(ecKeyPair.getPrivateKey());
            Credentials credentials = Credentials.create(pk);
            String msg = "address: " + credentials.getAddress()
                    + ", privateKey: " + Numeric.encodeQuantity(ecKeyPair.getPrivateKey());
            System.out.println(msg);
        }
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
    public void NULSVerify() {
        byte[] data = null;
        byte[] signature = null;
        byte[] pubKey = null;
        boolean verify = io.nuls.core.crypto.ECKey.verify(data, signature, pubKey);
        System.out.println(String.format("verify result: %s", verify));
    }

    @Test
    public void NULSAddressByPubkey() {
        String pubkey = "0301b0500627fc23a145317cb3cbcf2fc88c766f086a5cd97086f5d9b169fff98d";
        AddressTool.addPrefix(18, "BBAI");
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pubkey, 18)));

    }

    @Test
    public void ETHAddressByPubkey() {
        //0x044477033a4521efee5f90caf30f8eb3284e8d1bb7fef2923ae21617b24aacc8cbce2450fde8f48910e3ffb1455724d0c3671122c86000bae2840ab38dc7766932
        //0x044477033a4521efee5f90caf30f8eb3284e8d1bb7fef2923ae21617b24aacc8cbce2450fde8f48910e3ffb1455724d0c3671122c86000bae2840ab38dc7766932
        //0x32a6d6b1e968f996757cb49fd4f0b08692f9d7f6
        String pubkey = "02d09924d493ab1e7a2fc7438f48abcd078ff0bf0b82563f1c4917de8879a075c4";
        System.out.println(EthUtil.genEthAddressByCompressedPublickey(pubkey));
    }

    @Test
    public void ETHAddressByPubkeySet() {
        String pubkeySet = "021f23baf681fb72021b59a37ef9d266e0849bfd253fb25028465baa610e81caf3,02ecaaef4c36c7c50dd293d8b942d091203115dbeaa78a412e70aab3240c9a87ad,02dfc728f4c57b3fdab2bce1217eb142e3f84b3859affa6c761d19dc669d828e18";
        String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        System.out.print("[");
        int i = 0;
        for (String key : array) {
            if (i == array.length - 1) {
                System.out.print("\"" + EthUtil.genEthAddressByCompressedPublickey(key) + "\"");
            } else {
                System.out.print("\"" + EthUtil.genEthAddressByCompressedPublickey(key) + "\", ");
            }
            i++;
        }
        System.out.print("]\n");
    }

    @Test
    public void formatETHAddressSet() {
        String pubkeySet = "0x2C53cB522E1115Dce064D365DAc89640De501152,0xb61f272a3228990fB8fCB7eB281bedE8D2582577,0x5c9CCECde85A0F4B75d947dd5Fb462aF526c3ce8,0x97777E110691fB1327de1C1A783f0250F7A0BbF7,0x722f18ed05380854E5C11c9Ad053663DE916f689,0x3cF1D13D157c5286b21A38dd9C2e393dC06d3040,0x683d72D5A85Aa2AEDeb0138a2A2A74b601D44382,0xC6ED8e5ED5C647ecA0dD74B1247203aBcd709ca5,0x05f6f3B6d4a302e666f5717301d70Db8B6679b3e,0xbF62b3de20e1fD2151c9A2B444dF4AFf8D580244";
        String[] array = pubkeySet.split(",");
        System.out.println(String.format("size : %s", array.length));
        System.out.print("[");
        int i = 0;
        for (String address : array) {
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
        System.out.println(EthUtil.genEthAddressByCompressedPublickey("037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863"));
        System.out.println(EthUtil.genEthAddressByCompressedPublickey("036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b"));
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
        String pub = "9f636dab852fc3f68e7e5a24904ba41d0ffbb4dcd8dd18e07d37010311e729824d5a99960e8fa196f6ed7f8b2a7cbb39210a895b44e2b284908d2f82573620c";
        pub = leftPadding(pub, "0", 128);
        String pubkeyFromEth = PUBLIC_KEY_UNCOMPRESSED_PREFIX + pub;
        io.nuls.core.crypto.ECKey ecKey = io.nuls.core.crypto.ECKey.fromPublicOnly(HexUtil.decode(pubkeyFromEth));
        System.out.println(AddressTool.getAddressString(ecKey.getPubKeyPoint().getEncoded(true), 4));
    }

    /**
     * keystore导入测试
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
     * 助记词导入测试
     */
    @Test
    public void importByMnemonicest() throws Exception {
        String mnemonic = "capable delay jacket impulse neck marine exile target spy jar mass slam";
        String password = "qwer!1234";
        System.out.println(JSONUtils.obj2PrettyJson(importByMnemonic("m/44'/60'/0'/0/0", Arrays.asList(mnemonic.split(" ")), password)));
    }

    /**
     * 通过助记词导入钱包
     *
     * @param path      助记词路径  用户提供使用什么协议生成
     * @param mnemonics 助记词
     * @param password  密码
     * @return
     */
    private Map importByMnemonic(String path, List<String> mnemonics, String password) {
        //协议跟路径这个是有规定的，这里也要校验一下
        if (!path.startsWith("m") && !path.startsWith("M")) {
            throw new RuntimeException("请输入正确路径");
        }
        String[] pathArray = path.split("/");
        long creationTimeSeconds = System.currentTimeMillis() / 1000;
        //主要就是这里会有不同，原来是随机生成，这次我们替换成用户助记词构建
        DeterministicSeed ds = new DeterministicSeed(mnemonics, null, "", creationTimeSeconds);
        //根私钥
        byte[] seedBytes = ds.getSeedBytes();
        //助记词
        List<String> mnemonic = ds.getMnemonicCode();
        try {
            //助记词种子
            byte[] mnemonicSeedBytes = MnemonicCode.INSTANCE.toEntropy(mnemonic);
            ECKeyPair mnemonicKeyPair = ECKeyPair.create(mnemonicSeedBytes);
            WalletFile walletFile = Wallet.createLight(password, mnemonicKeyPair);
            ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
            //存这个keystore 用完后删除
            String jsonStr = objectMapper.writeValueAsString(walletFile);
            //验证
            WalletFile checkWalletFile = objectMapper.readValue(jsonStr, WalletFile.class);
            ECKeyPair ecKeyPair = Wallet.decrypt(password, checkWalletFile);
            byte[] checkMnemonicSeedBytes = Numeric.hexStringToByteArray(ecKeyPair.getPrivateKey().toString(16));
            List<String> checkMnemonic = MnemonicCode.INSTANCE.toMnemonic(checkMnemonicSeedBytes);
        } catch (MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException | CipherException | IOException e) {
            Log.error("账号生成异常", e);
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
        list.add("184178c84dfb879606332ccd5fd795478797d720869a1941fb3940c056374010");
        list.add("7ce38177977192924fc45d896aca3f2f694a4e1ba43da6e5fc056b9bae832c21");
        list.add("40fa95bfc159fd92ca680148b8d6dbc4897ee1af6123169ea3872869799de098");
        list.add("bee8ad6c2beacb20bfe07763eaabe39b5f7fa82026d2b60cd35310052bdfbbe7");
        list.add("5398d7c9036fffc2129d4878fbddca170157f4d3a3f3d2bada5a7e39e2d31547");
        list.add("b7bc0066706a7e148860bda21ab7c9cc63f856290df11ce2b2f18b99161a59b8");
        int i = 0;
        for(String prikey : list) {
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

    }

}
