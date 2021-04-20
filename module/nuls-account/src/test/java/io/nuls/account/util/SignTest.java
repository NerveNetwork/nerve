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
        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode("d8cdccd432fd1bb7711505d97c441672c540ccfcdbba17397619702eeef1d403"));
        byte[] data = HexUtil.decode("879a053d4800c6354e76c7985a865d2922c82fb5b3f4577b2fe08b998954f2e0");
        String signValueHex =  "1a8baa0e98b008fa4aff58e72186edc4e06563684427f0e169495a3e29fb906c729862f70f49c213d466ce9f33ea3c91df62279329eb160b73c79e3614b2a7b31c";
//        boolean result = ecKey.verify(data,signValue);
        BigInteger r = new BigInteger(1,HexUtil.decode(signValueHex.substring(0,64)));
        BigInteger s = new BigInteger(1,HexUtil.decode(signValueHex.substring(64,128)));
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r,s);
       boolean result = ecKey.verify(data,sig,ecKey.getPubKey());
        System.out.println(result);
        System.out.println(signValueHex);
        System.out.println(HexUtil.encode(sig.encodeToDER()));
        System.out.println(HexUtil.encode(sig.toCanonicalised().encodeToDER()));

        System.out.println(ecKey.verify(data,sig.toCanonicalised().encodeToDER()));
    }

    @Test
    public void test0() {
        ECKey ecKey = ECKey.fromPublicOnly(HexUtil.decode("02a8272dd5d6583cfec2495231d729e2b61e4a5b71ffaaeefed3e8768689876d5e"));
        byte[] data = HexUtil.decode("80b061ee87e1dfc8b94f75b2bbc1069e51a1475f1bd7080d8ecd6ed75a57eb12");
        String signValueHex = "72f0cdb55a8a51ee5814154b7f57c1eb12c355268c50b9c8a5c337e03c29ec6461c270bf10deab428e514e9cd1b51656e33d3c72318a27dfc75ad70214a72a251b";
        BigInteger r = new BigInteger(1,HexUtil.decode(signValueHex.substring(0, 64)));
        BigInteger s = new BigInteger(1,HexUtil.decode(signValueHex.substring(64, 128)));
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(r, s);
        System.out.println("old: " + signValueHex);
        byte[] newSignValue = sig.toCanonicalised().encodeToDER();
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
