package io.nuls.account.util;

import io.nuls.base.data.NulsSignData;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class SignTest {

    @Test
    public void test(){
        //ECKey ecKey = ECKey.fromPrivate(HexUtil.decode("32a78bf8cb9527771f3938c388365d1e084a8d506d39a4cc7036622dbb6344c1"));
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode("02558f01679b65ca1e0cb0ae31b2f0eea3aa001cf36f3fc14a92a95e1fe187334d"));
        byte[] hash = HexUtil.decode("d86cf03a175cdaf761d2eda25a98ce404d96ce0db2a4f25b25d46d604c7cdc5c");
        String signValueHex =  "3045022100921d67caacd15bb8228d59ef5f6d458417bf536d36307eb4fcd99a9f28eb8a4e022044d557f15998b68b2d3d556b01830d0c74c45cbbf94e6aa5d7fa485bfc4da70a";
        //ECKey ecKey = ECKey.fromPrivate(HexUtil.decode("32a78bf8cb9527771f3938c388365d1e084a8d506d39a4cc7036622dbb6344c1"));
        //byte[] data = HexUtil.decode("f78002debf6e4140239d6c5f6115fe5fd7622040c8ca66c8f35fabfd35829ca2");
        //String signValueHex =  "3044022010790986b640e87991d06ec84ba22b28a4474bdfa124fe6e28c94656b41aa58c0220072a9ed5b44cecf13c0702095b594d157e018ab9269f9b9dd9e71a0ee725d68b";
        boolean result = ecKey.verify(hash, HexUtil.decode(signValueHex));
        //BigInteger r = new BigInteger(1,HexUtil.decode(signValueHex.substring(0,64)));
        //BigInteger s = new BigInteger(1,HexUtil.decode(signValueHex.substring(64,128)));
        //ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r,s);
       //boolean result = ecKey.verify(data,sig,ecKey.getPubKey());
        System.out.println(result);
        //System.out.println(signValueHex);
        //System.out.println(HexUtil.encode(sig.encodeToDER()));
        //System.out.println(HexUtil.encode(sig.toCanonicalised().encodeToDER()));

        //System.out.println(ecKey.verify(data,sig.toCanonicalised().encodeToDER()));
    }

    @Test
    public void test0() {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode("02558f01679b65ca1e0cb0ae31b2f0eea3aa001cf36f3fc14a92a95e1fe187334d"));
        byte[] data = HexUtil.decode("d86cf03a175cdaf761d2eda25a98ce404d96ce0db2a4f25b25d46d604c7cdc5c");
        String signValueHex = "921d67caacd15bb8228d59ef5f6d458417bf536d36307eb4fcd99a9f28eb8a4e44d557f15998b68b2d3d556b01830d0c74c45cbbf94e6aa5d7fa485bfc4da70a01";
        BigInteger r = new BigInteger(1,HexUtil.decode(signValueHex.substring(0, 64)));
        BigInteger s = new BigInteger(1,HexUtil.decode(signValueHex.substring(64, 128)));
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r, s);
        System.out.println("old: " + signValueHex);
        byte[] newSignValue = sig.toCanonicalised().encodeToDER();
        //byte[] newSignValue = sig.encodeToDER();
        System.out.println("new: " + HexUtil.encode(newSignValue));
        boolean result = ecKey.verify(data, newSignValue);
        System.out.println(result);
    }

    @Test
    public void test2() {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode("02c2b4e37fa297879c3ed824d021c0ee4692c6f87fcaf1681d712ccd485784b9bd"));
        byte[] data = HexUtil.decode("a85c2e2b118698e88db68a8105b794a8cc7cec074e89ef991cb4f5f533819cc2");
        String signValueHex = "d1374abd173161987e7f8b08ebf669eb10603570424660318ac7825c9689430e256af211609efda4118c7c7d7ab236906a35bca6512976a6b7ded7c86da3429f1c";
        BigInteger r = new BigInteger(1,HexUtil.decode(signValueHex.substring(0, 64)));
        BigInteger s = new BigInteger(1,HexUtil.decode(signValueHex.substring(64, 128)));
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r, s);
        System.out.println("old: " + signValueHex);
        byte[] newSignValue = sig.toCanonicalised().encodeToDER();
        //byte[] newSignValue = sig.encodeToDER();
        System.out.println("new: " + HexUtil.encode(newSignValue));
        boolean result = ecKey.verify(data, newSignValue);
        System.out.println(result);
    }

    @Test
    public void test1() {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode("02a8272dd5d6583cfec2495231d729e2b61e4a5b71ffaaeefed3e8768689876d5e"));
        List<MyCase> list = new ArrayList<>();
        list.add(new MyCase("personal_sign_0","80b061ee87e1dfc8b94f75b2bbc1069e51a1475f1bd7080d8ecd6ed75a57eb12","e8946c4c56f5d9a7d25386f72aaa4b668093be7ef8b754f906debd2230ab1b9375040212ecac2eb2c6fa0bd432b23f28c399a442c8de4fb20de6219a39eca2a11b"));
        list.add(new MyCase("eth_sign_0","80b061ee87e1dfc8b94f75b2bbc1069e51a1475f1bd7080d8ecd6ed75a57eb12","72f0cdb55a8a51ee5814154b7f57c1eb12c355268c50b9c8a5c337e03c29ec6461c270bf10deab428e514e9cd1b51656e33d3c72318a27dfc75ad70214a72a251b"));
        for(MyCase c :list){
            boolean result = ecKey.verify(HexUtil.decode(c.getHash()),HexUtil.decode(turn(c.getSignValue())));
            System.out.println(result);
        }
    }


    public static String turn(String oldSignValueHex){
        BigInteger r = new BigInteger(1,HexUtil.decode(oldSignValueHex.substring(0, 64)));
        BigInteger s = new BigInteger(1,HexUtil.decode(oldSignValueHex.substring(64, 128)));
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r, s);
        return HexUtil.encode(sig.toCanonicalised().encodeToDER());
    }

    static class MyCase{
        public MyCase(String key,String hash,String signValue){
            this.hash = hash;
            this.signValue = signValue;
        }
        private String key;
        private String hash;
        private String signValue;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getSignValue() {
            return signValue;
        }

        public void setSignValue(String signValue) {
            this.signValue = signValue;
        }
    }
}
