package io.nuls.ledger.test.cmd;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.transaction.TransferService;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.ledger.model.po.AccountState;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-28 15:09
 * @Description: 功能描述
 */
public class BackupAccountBalanceTest {

    @Before
    public void before() throws Exception {
        ServiceManager.init(4, Provider.ProviderType.RPC);
        NoUse.mockModule();
    }


    /**
     * 备份资产到文件
     * @throws Exception
     */
    @Test
    public void testBackupAccountState() throws Exception {
        AddressTool.addPrefix(4,"TNVT");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr,"getAllAccount", Map.of("chainId",4));
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
        String formAddress = "TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA";
        File file = new File("/Users/zhoulijun/workspace/nuls/nerve-network-package" + File.separator + "account-balance");
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
                    new TransferReq.TransferReqBuilder(4, assetId)
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


}
