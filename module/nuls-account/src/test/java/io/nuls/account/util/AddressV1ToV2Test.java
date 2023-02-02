package io.nuls.account.util;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.v2.model.dto.RestFulResult;
import io.nuls.v2.util.RestFulUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
public class AddressV1ToV2Test {

    public static void main(String[] args) throws IOException {
        int wrong = 0;
        for (int i = 0; i < 1000000; i++) {
            String params = "{ \"count\": 10, \"password\": \"\"}";
            Map<String, Object> json2map = JSONUtils.json2map(params);
            RestFulResult result = RestFulUtil.post("http://192.168.1.136:6001/api/account/offline", json2map);
            Map<String, Object> map = (Map<String, Object>) result.getData();
            List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("list");
            for (Map<String, Object> account : list) {
                String address = (String) account.get("address");
                String pubHex = (String) account.get("pubKey");
                String priHex = (String) account.get("priKey");
                byte[] addressV1 = AddressTool.getAddress(address);

                byte[] bytesV1 = new byte[20];
                System.arraycopy(addressV1, 3, bytesV1, 0, 20);
                boolean b = AddressTool.checkPublicKeyHash(addressV1, bytesV1);
                byte[] addressV2 = getAddressV2(bytesV1);
                ECKey ecKey = ECKey.fromPrivate(HexUtil.decode(priHex));
                boolean right = b && ecKey.getPublicKeyAsHex().equals(pubHex) && Arrays.equals(bytesV1, SerializeUtils.sha256hash160(ecKey.getPubKey()));
                if (!right) {
                    System.out.println(address + ", " + pubHex + ", " + priHex);
                    wrong++;
                }
                System.out.println(address + "=======" + AddressTool.getStringAddressByBytes(addressV2) + "======" + wrong);
            }
        }
    }

    private static byte[] getAddressV2(byte[] pubKeyHash) {
        Address address = new Address(1, "NULS", BaseConstant.DEFAULT_ADDRESS_TYPE, pubKeyHash);
        return address.getAddressBytes();
    }

}
