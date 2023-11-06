package io.nuls.account.util;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.parse.SerializeUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Eva
 */
public class AddressToolTest {

    @Test
    public void createMultiSigAccount() throws Exception {
        int chainId = 5;
        int minSigns = 2;
        String prikey2 = "3e73f764492e95362cf325bd7168d145110a75e447510c927612586c06b23e91";
        String prikey1 = "6d10f3aa23018de6bc7d1ee52badd696f0db56082c62826ba822978fdf3a59fa";
        String prikey3 = "f7bb391ab82ba9ec7a552955b2fe50d79eea085d7571e5e2480d1777bc171f5e";
        List<String> pubKeys= List.of(ECKey.fromPrivate(HexUtil.decode(prikey1)).getPublicKeyAsHex(),
                ECKey.fromPrivate(HexUtil.decode(prikey2)).getPublicKeyAsHex(),
                ECKey.fromPrivate(HexUtil.decode(prikey3)).getPublicKeyAsHex());

        byte[] pubHash = AddressTool.createMultiSigAccountOriginBytes(chainId, minSigns, pubKeys);
        System.out.println(HexUtil.encode(pubHash));
        pubHash = SerializeUtils.sha256hash160(pubHash);
        System.out.println(HexUtil.encode(pubHash));

        Address address = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE, pubHash);
        System.out.println(address.getBase58());
    }

    @Test
    public void createAgent() {
        List<String> prilist = new ArrayList<>();
        prilist.add("2d2d04ca5f74dd17736a30b7b65ea2dc9eff61136cceec8d3b4da8ddc65314d7");
        prilist.add("5d1ce6ed0460d7470c3d09b980549f5f546352f0b770281cb11fe92870c01587");
        prilist.add("5acab1876e59ef00044fe989bbd709117eb22179b1e4b9b7dcf23b89b149c5d1");
        prilist.add("38b83f5c78c6532cbf200fb1bf73dcbacf6dde555a56a06440f69535f0aca04e");
        prilist.add("c320819698f92ca53382dace76e9d668b1f54c28c989086b1eeec3598b59a6b4");
        prilist.add("217e1b8cafb6bb0a2dcebaa41d8f54db03c3aff546364c039f5785b9f3d32916");
        prilist.add("d9ba26ba205c552778b7363deb55969a6f9a4abe54ad4c955109b0de7cb5a53e");
        prilist.add("0b70ac59f895ff9ad2cfb9af0af86d55e6cfda738c4bedf5d27dc2d3fc0bac48");
        prilist.add("ad19eaaa4ccbc6829416e15ff82a485ca13c88ba08dfa46e400a579549650a95");
        prilist.add("22e76a5ca4cb9d00742ffbb587267b58cbc5befc93fdec121eff4d2256e543be");
        List<String> list = new ArrayList<>();
        for (String pri : prilist) {
            ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(pri));
            String val = AddressTool.getStringAddressByBytes(AddressTool.getAddress(ecKey.getPubKey(), 5));

            list.add(val);
        }
        List<String> result = new ArrayList<>();
        result.add("createagent TNVTdTSPLQeuiKZ7RViZ96A2mcCtiL9faDcJ6 " + list.get(0) + " 200000 TNVTdTSPLQeuiKZ7RViZ96A2mcCtiL9faDcJ6 nuls123456");
        result.add("createagent TNVTdTSPV85z3f7ca6TKrFj8rN2DYyF9LhyB9 " + list.get(1) + " 200000 TNVTdTSPV85z3f7ca6TKrFj8rN2DYyF9LhyB9 nuls123456");
        result.add("createagent TNVTdTSPUPj4mGpCjqaT7jLW8fB67TEwQTqVW " + list.get(2) + " 200000 TNVTdTSPUPj4mGpCjqaT7jLW8fB67TEwQTqVW nuls123456");
        result.add("createagent TNVTdTSPTftjzTzmzVJsRtxyUJecvc7acRW5D " + list.get(3) + " 200000 TNVTdTSPTftjzTzmzVJsRtxyUJecvc7acRW5D nuls123456");
        result.add("createagent TNVTdTSPPypPVVymvKv6eJv1PboiMryjFzCnc " + list.get(4) + " 200000 TNVTdTSPPypPVVymvKv6eJv1PboiMryjFzCnc nuls123456");
        result.add("createagent TNVTdTSPR1CW3fJCubqWQULPC3PxBv7rMpQSM " + list.get(5) + " 200000 TNVTdTSPR1CW3fJCubqWQULPC3PxBv7rMpQSM nuls123456");
        result.add("createagent TNVTdTSPQsLJSDBkR8oEbuBfgk6b5NNKaNfVM " + list.get(6) + " 200000 TNVTdTSPQsLJSDBkR8oEbuBfgk6b5NNKaNfVM nuls123456");
        result.add("createagent TNVTdTSPS2BuZ36ft6g9FqEoji2GQCtYSTvjn " + list.get(7) + " 200000 TNVTdTSPS2BuZ36ft6g9FqEoji2GQCtYSTvjn nuls123456");
        result.add("createagent TNVTdTSPGBwJWXbzAuBuBPprZV6kzdNodf1yn " + list.get(8) + " 200000 TNVTdTSPGBwJWXbzAuBuBPprZV6kzdNodf1yn nuls123456");
        result.add("createagent TNVTdTSPUWnpvU5Hp9WHKoUZnX4qgonDKeVRF " + list.get(9) + " 200000 TNVTdTSPUWnpvU5Hp9WHKoUZnX4qgonDKeVRF nuls123456");

        for (String val : result) {
            System.out.println(val);
        }
    }


    public static void main(String[] args) {
        byte[] bytes = AddressTool.getAddress("NULSd6Hh3JKDpBkXHmwdrjE2quCYCFte2R496");
        System.out.println(bytes);
    }

    @Test
    public void testGetBytesAddress() {
//        for (int i = 0; i < 10; i++) {
            String pubs = "0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b,02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d,02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0,020c60dd7e0016e174f7ba4fc0333052bade8c890849409de7b6f3d26f0ec64528,0369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c5";
            String arr[] = pubs.split(",");
            for(String pub:arr) {
                String address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(HexUtil.decode(pub), 1));
                byte[] real = AddressTool.getAddress(address);
                System.out.println("args{\"" + address + "\"},\"" + HexUtil.encode(real) + "\"");
            }

    }

    @Test
    public void createNerveAddr() {
        ECKey ecKey = new ECKey();
        System.out.println("私钥:" + ecKey.getPrivateKeyAsHex());
        System.out.println("公钥:" + ecKey.getPublicKeyAsHex());
//        f07d26a6cc0e931602f638fdf5b7dfa94ac8e1a0f9b5885feb78db17a4a7400a
//        034e7ebe781dc25d5603c78616c939beabb1bd89ef5a2a182d292535146828c509
//        TNVTdTSPG3sAwUkrGBqoVVzDjHYqvQKMX1khf
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddress(ecKey.getPubKey(), 9)));
//        a572b95153b10141ff06c64818c93bd0e7b4025125b83f15a89a7189248191ca
//        02401b78e28d293ad840f9298c2c7e522c68776e3badf092c2dbf457af1b8ed43e
//                TNERVEfTSPQvEngihwxqwCNPq3keQL1PwrcLbtj

//        94f024a7c2c30549b7ee932030e7c38f8a9dff22b4b08809fb9e5e2263974717
//        023d994b0452216d13ae59b4544ea168f63360cf3a2ac1a2d74cfbf37cc6fa4848
//                TNERVEfTSPMT6FsCYgJ2ugkC9Sqhws5YYV4z378
    }

    @Test
    public void test7y() {
        String address = "TNVTdTSPMsBqXoEVtkAyPBVyduftKvZVQopFD";
        System.out.println(AddressTool.validAddress(5, address));
    }

    @Test
    public void createAccountByPrefix() {
        AddressTool.addPrefix(4, "LJS");
        for (int i = 0; i < 10; i++) {
            ECKey key = new ECKey();
            Address address = new Address(4, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
            System.out.println(address.toString() + "================" + address.getBase58() + "===========" + key.getPrivateKeyAsHex());
        }
    }

    @Test
    public void creaateMainNetAccount() {
        System.out.println("=======================main net=======================");
        while (true) {
            ECKey key = new ECKey();
            Address address = new Address(1, (byte) 1, SerializeUtils.sha256hash160(key.getPubKey()));
            String value = address.getBase58();
            if (value.toUpperCase().endsWith("55"))
                System.out.println(value + "===========" + key.getPrivateKeyAsHex());
        }
    }

    @Test
    public void getBlackWhole() {
        Address address = new Address(1, (byte) 1, SerializeUtils.sha256hash160(HexUtil.decode("000000000000000000000000000000000000000000000000000000000000000000")));
        System.out.println(address);
    }

    /**
     * 通缩计算
     */
    @Test
    public void calc() {
        double rate = 0.996;
        long total = 21000000000000000l;
        long init = 11000000000000000l;
        long month = 1;
        long monthReward = 41095890410959L;
        while (init < total) {
            monthReward = (long) DoubleUtils.mul(monthReward, rate);
            if (0 == monthReward) {
                break;
            }
            init = init + monthReward;
            month++;
        }
        System.out.println(init);
        System.out.println(month);
        System.out.println(month / 12);
    }

    @Test
    public void createAccount() throws NulsException {
        System.out.println("=======================test net=======================");
        for (int i = 0; i < 100; i++) {
            ECKey key = new ECKey();
            Address address = new Address(2, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
            System.out.println(address.getBase58() + "===========" + key.getPrivateKeyAsHex());
        }
        System.out.println("=======================main net=======================");
        for (int i = 0; i < 100; i++) {
            ECKey key = new ECKey();
            Address address = new Address(1, (byte) 1, SerializeUtils.sha256hash160(key.getPubKey()));
            System.out.println(address.getBase58() + "===========" + key.getPrivateKeyAsHex());
        }
        System.out.println("=======================other net=======================");
        for (int i = 3; i < 100; i++) {
            ECKey key = new ECKey();
            Address address = new Address(i, (byte) 1, SerializeUtils.sha256hash160(key.getPubKey()));
            System.out.println(i + "==========" + address.getBase58() + "===========" + key.getPrivateKeyAsHex());
        }
        for (int i = 65535; i > 65400; i--) {
            ECKey key = new ECKey();
            Address address = new Address(i, (byte) 1, SerializeUtils.sha256hash160(key.getPubKey()));
            System.out.println(i + "==========" + address.getBase58() + "===========" + key.getPrivateKeyAsHex());
        }
    }


    @Test
    public void testValid() {
        String address1 = "tNULSeBaMrNbr7kDHan5tBVms4fUZbfzed6851";
        boolean result = AddressTool.validAddress(2, address1);
        assertTrue(!result);

        address1 = "NULSeBaMrNbr7kDHan5tBVms4fUZbfzed685k";
        result = AddressTool.validAddress(1, address1);
        assertTrue(!result);

        address1 = "AHUcC84FN4CWrhuMgvvGPy6UacBvcutgQ4rAR";
        result = AddressTool.validAddress(65401, address1);
        assertTrue(!result);

    }

    @Test
    public void testGetAddress() {
        String address = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";

        byte[] bytes = AddressTool.getAddress(address);

        String address1 = AddressTool.getStringAddressByBytes(bytes);

        assertTrue(address.equalsIgnoreCase(address1));

    }

    @Test
    public void testChainId() {
        String address = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
        int id = AddressTool.getChainIdByAddress(address);
        System.out.println(id);

        boolean result = AddressTool.validAddress(2, address);
        assertTrue(result);
    }

    @Test
    public void testGetPrefix() {
        String address1 = "tNULSeBaMrNbr7kDHan5tBVms4fUZbfzed6851";
        String address2 = "NULSeBaMrNbr7kDHan5tBVms4fUZbfzed685k";
        String address3 = "APNcCm4yik6XXquTHUNbHqfPhGrfcSoGoMudc";


        assertEquals("tNULS", AddressTool.getPrefix(address1));
        assertEquals("NULS", AddressTool.getPrefix(address2));
        assertEquals("APN", AddressTool.getPrefix(address3));


    }
}