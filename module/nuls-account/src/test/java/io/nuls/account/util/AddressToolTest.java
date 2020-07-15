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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Eva
 */
public class AddressToolTest {

    public static void main(String[] args) {
        System.out.println(AddressTool.getAddressString(HexUtil.decode("03890847cf4c9a22179b217217b306228a80c6c96bcc77e71342e09e00bb0bda30"),9));
    }

    @Test
    public void createNerveAddr(){
        ECKey ecKey = new ECKey();
        System.out.println(ecKey.getPrivateKeyAsHex());
        System.out.println(ecKey.getPublicKeyAsHex());
//        f07d26a6cc0e931602f638fdf5b7dfa94ac8e1a0f9b5885feb78db17a4a7400a
//        034e7ebe781dc25d5603c78616c939beabb1bd89ef5a2a182d292535146828c509
//        TNVTdTSPG3sAwUkrGBqoVVzDjHYqvQKMX1khf
        System.out.println(AddressTool.getAddressString(ecKey.getPubKey(),5));
//        a572b95153b10141ff06c64818c93bd0e7b4025125b83f15a89a7189248191ca
//        02401b78e28d293ad840f9298c2c7e522c68776e3badf092c2dbf457af1b8ed43e
//                TNERVEfTSPQvEngihwxqwCNPq3keQL1PwrcLbtj

//        94f024a7c2c30549b7ee932030e7c38f8a9dff22b4b08809fb9e5e2263974717
//        023d994b0452216d13ae59b4544ea168f63360cf3a2ac1a2d74cfbf37cc6fa4848
//                TNERVEfTSPMT6FsCYgJ2ugkC9Sqhws5YYV4z378
    }

    @Test
    public void test7y(){
        String address = "NULSd6Hgam8YajetEDnCoJBdEFkMNP41PfH7y";
        System.out.println(AddressTool.validAddress(1,address));
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
        while (true){
            ECKey key = new ECKey();
            Address address = new Address(1, (byte) 1, SerializeUtils.sha256hash160(key.getPubKey()));
            String value = address.getBase58();
            if(value.toUpperCase().endsWith("55"))
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
            if(0==monthReward){
                break;
            }
            init = init + monthReward;
            month++;
        }
        System.out.println(init);
        System.out.println(month);
        System.out.println(month/12);
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