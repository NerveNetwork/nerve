package io.nuls.api.test;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-22 11:08
 * @Description: 功能描述
 */
public class LedgerTest extends BaseTestCase{

    @Test
    public void testGetAssetInfo(int chainId, String txHash) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", txHash);
        try {
            Map map = (Map) RpcCallUtil.request(ModuleE.LG.abbr, "getAssetRegInfoByHash", params);
            Log.info("{}",map);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public static void main(String[] args) {
//        String priKey = "d9ae0c8a19e64fda99fd9fe2aeb87c5f2c417be909016b4b332cbda4e05236d";
//        BigInteger priint = new BigInteger(1,HexUtil.decode(priKey));
//        System.out.println(priint);
//        ECKey key = ECKey.fromPrivate(HexUtil.decode(priKey));
//        ECKey key = new ECKey();
//        Address address = new Address(1, "NULS", BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
//        System.out.println("=".repeat(100));
//        System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));
//        System.out.println("privateKey:" + key.getPrivateKeyAsHex());
//        System.out.println("priInt:" + key.getPrivKey());
//        System.out.println("=".repeat(100));
//        System.out.println("328905a4eed2a5ddf6499a54ed98b16c883cf17e03a70a441cac9990801fd6c8".length());
//

//        while(true){
//            ECKey key = new ECKey();
//            String priKey = key.getPrivateKeyAsHex();
//            if(key.getPrivKey().compareTo(BigInteger.ZERO) < 0){
//                System.out.println("not 64 " + priKey);
//                return ;
//            }else{
//                System.out.println(priKey);
//            }
//
//        }
        System.out.println("d9ae0c8a19e64fda99fd9fe2aeb87c5f2c417be909016b4b332cbda4e05236d".length());
    }

}
