/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.core;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import nerve.network.converter.heterogeneouschain.eth.utils.EthUtil;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.SerializeUtils;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.crypto.ECKey;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import static io.nuls.core.crypto.ECKey.CURVE;

/**
 * @author: Chino
 * @date: 2020-02-26
 */
public class EthAccountTest {

    private int chainId = 1;

    @Test
    public void importPriKeyTest() {
        // 0xf173805F1e3fE6239223B17F0807596Edc283012
        //String prikey = "0xD15FDD6030AB81CEE6B519645F0C8B758D112CD322960EE910F65AD7DBB03C2B";
        // 0x008a19c84c6755801f90280706033ecc299edf8dea24693142822db795681de503::::::::0x11eFE2A9CF96175AB241e4A88A6b79C4f1c70389
        //String prikey = "0x008a19c84c6755801f90280706033ecc299edf8dea24693142822db795681de5";
        //String prikey = "53b02c91605451ea35175df894b4c47b7d1effbd05d6b269b3e7c785f3f6dc18";
        //String prikey = "b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5";
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
        System.out.println(String.format("eth Credentials " + msg));


        System.out.println("=============NULS===============");

        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(HexUtil.decode(Numeric.cleanHexPrefix(prikey)));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(false))));
        System.out.println(String.format("NULS ECKey pubkey: %s", Numeric.toHexString(nEckey.getPubKeyPoint().getEncoded(true))));
        Address address = new Address(chainId, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("NULS address: %s", address.toString()));
        System.out.println();


        System.out.println("=============Converter===============");
        String pubkeyFromEth = "04" + Numeric.toHexStringNoPrefix(ecKeyPair.getPublicKey());
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
        String pubkey = "037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863";
        System.out.println(EthUtil.genEthAddressByCompressedPublickey(pubkey));
    }

}
