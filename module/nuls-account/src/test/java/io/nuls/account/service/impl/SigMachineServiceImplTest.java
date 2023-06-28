package io.nuls.account.service.impl;

import io.nuls.account.config.AccountConfig;

import java.util.HashMap;
import java.util.Map;

public class SigMachineServiceImplTest extends SigMachineServiceImpl{


    public static void main(String[] args) throws Exception {
        SigMachineServiceImplTest service = new SigMachineServiceImplTest();
        service.config = new AccountConfig();
        service.config.setSigMacApiKey("25eb1cb9-d19c-43d4-8899-32bef8b9b006");
        service.config.setSigMacUrl("http://127.0.0.1:8085");

        Map<String,Object> map = new HashMap<>();
        map.put("method","blockSign");
        map.put("header","89ee9397ee6d37031c8bf7223d2882639d7289c22445cfb4ed368940781f0dc11668c64cc76653840ff3a0108e3375354192bf4fefc32325f5896da9950654ab6b557d648a2daa0201000000342a95230013004a557d641000010001003c6400209a1d057bc18993eb0c2435d1fee6696e0654bc08a1c2ccc3e92ca3387175758c");
        String result = service.request(map);
        System.out.println(result);
    }
}