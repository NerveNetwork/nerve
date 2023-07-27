package io.nuls.ledger.cmd;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.base.api.provider.account.facade.ImportAccountByPrivateKeyReq;
import io.nuls.base.api.provider.transaction.TransferService;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.ledger.model.po.AccountState;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-28 15:09
 * @Description: 功能描述
 */
public class BackupAccountBalanceTest {

    @Before
    public void before() throws Exception {
        ServiceManager.init(5, Provider.ProviderType.RPC);
        NoUse.mockModule();
    }


    /**
     * 备份资产到文件
     * @throws Exception
     */
    @Test
    public void testBackupAccountState() throws Exception {
        AddressTool.addPrefix(5,"TNVT");
        Response response = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr,"getAllAccount", Map.of("chainId",5));
        Map<String,String> data = (Map<String, String>) ((Map)response.getResponseData()).get("getAllAccount");
        Map<String, BigInteger> balance = new HashMap<>();
        data.entrySet().forEach(entry->{
            AccountState accountState = new AccountState();
            try {
                accountState.parse(new NulsByteBuffer(HexUtil.decode(entry.getValue())));
                balance.put(entry.getKey(),accountState.getTotalAmount());
            } catch (NulsException e) {
                e.printStackTrace();
            }
        });
        File file = new File(System.getProperty("user.dir") + File.separator + "account-balance");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        balance.entrySet().forEach(d->{
            try {
                writer.write(d.getKey() + ":" + d.getValue());
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
        Log.info("file:{}",file.getAbsoluteFile());
    }


    /**
     * 从文件中恢复资产，从指定地址转出资产
     * @throws IOException
     */
    @Test
    public void testRestoreAccount() throws IOException {
        String formAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
        File file = new File("/Users/zhoulijun/workspace/nuls/nerve-network-package/" + File.separator + "account-balance.0619");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while(line != null){
            Log.info("{}",line);
            String[] key = line.split(":");
            line = reader.readLine();
            String[] keyAry = key[0].split("-");
            String address = keyAry[0];
            if(address.equals(formAddress)){
                continue ;
            }
            BigInteger amount = new BigInteger(key[1]);
            int chainId = Integer.parseInt(keyAry[1]);
            int assetId = Integer.parseInt(keyAry[2]);
            TransferService transferService = ServiceManager.get(TransferService.class);
            TransferReq.TransferReqBuilder builder =
                    new TransferReq.TransferReqBuilder(5, assetId)
                            .addForm(chainId, assetId, formAddress, "nuls123456", amount)
                            .addTo(chainId, assetId, address, amount);
            Result<String> result = transferService.transfer(builder.build(new TransferReq()));
            if(result.isFailed()){
                Log.error("失败:{}",result.getMessage());
            }else{
                Log.info("{}",result);
            }

        }
    }


    @Test
    public void testMain() throws IOException {
        int chainId = 5;
        int assetId = 1;
        String password = "nuls123456";
        BigInteger amount = BigInteger.valueOf(
                21000000000000L);
        String formAddress = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
        String prikey = "9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b";
        AccountService accountService = ServiceManager.get(AccountService.class);
        accountService.importAccountByPrivateKey(new ImportAccountByPrivateKeyReq(password,prikey,true));
        Map<String,String> ip = new HashMap<>();
        ip.put("nuls01","172.21.42.125");
        ip.put("nuls02","172.21.42.121");
        ip.put("nuls03","172.21.42.124");
        ip.put("nuls04","172.21.42.138");
        ip.put("nuls05","172.21.42.123");
        ip.put("nuls06","172.21.42.150");
        ip.put("nuls07","172.21.42.147");
        ip.put("nuls08","172.21.42.140");
        ip.put("nuls09","172.21.42.130");
        ip.put("nuls10","172.21.42.149");
        String file_path = System.getProperty("user.dir") + "/.temp/";
        File file = new File(file_path);
        if(!file.exists()){
            file.mkdir();
        }
        String addressPrefix = "TNVT";
        System.out.println(String.format("%02d",9));
        BufferedReader ca = new BufferedReader(new FileReader(new File(file_path + "cp")));
        String line = ca.readLine();
        Map<String,String> cprikey = new HashMap<>();
        while(line != null){
            String[] ary = line.split("=");
            cprikey.put(ary[0],ary[1]);
            line = ca.readLine();
        }
        ca.close();
//        BufferedWriter cp = new BufferedWriter(new FileWriter(new File(file_path + "cp")));
//        BufferedWriter pp = new BufferedWriter(new FileWriter(new File(file_path + "pp")));
//        BufferedWriter pa = new BufferedWriter(new FileWriter(new File(file_path + "pa")));
//        BufferedWriter pascript = new BufferedWriter(new FileWriter(new File(file_path + "pascript")));
//        BufferedWriter ccscript = new BufferedWriter(new FileWriter(new File(file_path + "ccscript")));
//        BufferedWriter calias = new BufferedWriter(new FileWriter(new File(file_path + "calias")));

//        pascript.write("#!/bin/bash");
//        pascript.newLine();
        try{
            int count = ip.size();
            for (int i = 0; i < count; i++) {
                String id = String.format("nuls%02d",i+1);
                String nk = ip.get(id);
                String agentPrikey = cprikey.get(nk);
                ECKey key = ECKey.fromPrivate(HexUtil.decode(agentPrikey));
                Address address = new Address(chainId, addressPrefix, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
//                cp.write(nk + "=" + key.getPrivateKeyAsHex());
//                cp.newLine();
                String caddress = AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix());
                TransferService transferService = ServiceManager.get(TransferService.class);
                TransferReq.TransferReqBuilder builder =
                        new TransferReq.TransferReqBuilder(chainId, assetId)
                                .addForm(chainId, assetId, formAddress, password, amount)
                                .addTo(chainId, assetId, caddress, amount);
                Result<String> result = transferService.transfer(builder.build(new TransferReq()));
                if(result.isFailed()){
                    Log.error("失败:{}",result.getMessage());
                }else{
                    Log.info("{}",result);
                }
                ImportAccountByPrivateKeyReq req = new ImportAccountByPrivateKeyReq(password,key.getPrivateKeyAsHex(),true);
                req.setChainId(chainId);
                accountService.importAccountByPrivateKey(req);
//                key = new ECKey();
//                address = new Address(chainId, addressPrefix, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
//                pp.write(nk + "=" +  key.getPrivateKeyAsHex());
//                pp.newLine();
//                String paddress = AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix());
//                pa.write(id + "=" + nk + "=" + paddress);
//                pa.newLine();
//                pascript.write("ssh root@"+nk+" 'bash -s ' < ./remote-import-address " + key.getPrivateKeyAsHex());
//                pascript.newLine();
//                ccscript.write("createagent " + caddress + " " + paddress + " 200000 " + caddress + " " + password);
//                ccscript.newLine();
//                calias.write("setalias " + caddress + " " + String.format("nuls%02d",i+1) + " " + password);
//                calias.newLine();
            }
//            cp.flush();
//            ca.flush();
//            pp.flush();
//            pa.flush();
//            calias.flush();
//            pascript.flush();
//            ccscript.flush();
        }finally {
//            cp.close();
//            ca.close();
//            pp.close();
//            pa.close();
//            calias.close();
//            pascript.close();
//            ccscript.close();
        }
    }

    public static void main(String[] args) throws Exception {
        RocksDBManager.init("/Users/zhoulijun/workspace/nuls/nerve-network-package/NULS_WALLET/data/ledger");
        List<Entry<byte[],byte[]>> list = RocksDBManager.entryList("account_9");
        BigInteger total = list.stream().map(d->{
            try {
                String key = new String(d.getKey(),"UTF8");
                String[] keyAry = key.split("-");

                if("9".equals(keyAry[1]) && "2".equals(keyAry[2]) && !"pb63T1M8JgQ26jwZpZXYL8ZMLdUAK31L".equals(keyAry[0])){
                    io.nuls.ledger.model.po.AccountState accountState = new io.nuls.ledger.model.po.AccountState();
                    accountState.parse(d.getValue(),0);
                    return accountState.getTotalAmount();
                }else{
                    return BigInteger.ZERO;
                }
            } catch (UnsupportedEncodingException | NulsException e) {
                return BigInteger.ZERO;
            }
        }).reduce(BigInteger::add).orElse(BigInteger.ZERO);
        Log.info("{}",total);


    }

}
