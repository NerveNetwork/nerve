package io.nuls.cmd.client.test.account;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.base.api.provider.account.facade.CreateAccountReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.cmd.client.CmdClientBootstrap;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.enums.DepositTimeType;
import io.nuls.cmd.client.processor.account.CreateProcessor;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.parse.SerializeUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-07 16:23
 * @Description: Function Description
 */
public class TestCreateProcessor {

    AccountService accountService = ServiceManager.get(AccountService.class);

    @Before
    public void before(){
        CmdClientBootstrap.main(new String[]{});
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreate(){
        CreateAccountReq req = new CreateAccountReq(1,"nuls123456");
        Result<String> res = accountService.createAccount(req);
        Assert.assertTrue(res.isSuccess());
        Assert.assertTrue(res.getList().size() == 1);
        res.getList().forEach(System.out::println);
    }

    @Test public void testCreateForCmd(){
        CreateProcessor cp = new CreateProcessor();
        CommandResult res = cp.execute(new String[0]);
        Assert.assertTrue(res.isSuccess());
        System.out.println(res.getMessage());
    }

    public static void main(String[] args) {
        int chainId = 4;
        String addressPrefix = "TNVT";
        String cmd = "address";


        ECKey ecKey = ECKey.fromPrivate(HexUtil.decode("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b"));
        Address address = new Address(chainId, addressPrefix, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(ecKey.getPubKey()));
        System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));
        address =  new Address(2, "tNULS", BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(ecKey.getPubKey()));
        System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));

        int count = 4;
        switch (cmd) {
            case "address": {

                if (args.length >= 3) {
                    chainId = Integer.parseInt(args[2]);
                }
                if (args.length >= 2) {
                    count = Integer.parseInt(args[1]);
                }
                System.out.println("chainId:" + chainId);
                System.out.println("number:" + count);

                for (int i = 0; i < count; i++) {
                    ECKey key = new ECKey();
                    address = new Address(chainId, addressPrefix, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
                    System.out.println("=".repeat(100));
                    System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));
                    System.out.println("privateKey:" + key.getPrivateKeyAsHex());
                    System.out.println("publicKey:" + key.getPublicKeyAsHex());
                    System.out.println("=".repeat(100));

                }
                System.exit(0);
            }
            default:
                System.out.println("error command :" + args[0]);
                System.exit(0);
        }

    }

}
