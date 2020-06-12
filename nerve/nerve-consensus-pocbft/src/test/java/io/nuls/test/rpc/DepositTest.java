package io.nuls.test.rpc;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Address;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
/**
 * 委托相关操作测试
 * Delegate related operation testing
 *
 * @author tag
 * 2018/12/1
 * */
public class DepositTest {
    protected  String success = "1";

    @BeforeClass
    public static void start() throws Exception {
        NoUse.mockModule();
    }

    @Test
    public void getAssetBySymbol()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("symbol","NVT");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getAssetBySymbol", params);
        System.out.println(cmdResp.getResponseData());
        //1444559ce65de3dd23811fd2170ec126e11416e54dd5468079015dc3c8946245
    }

    @Test
    public void depositAgent()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("deposit","30000000000000");
        params.put("password", "nuls123456");
        params.put("assetChainId",2);
        params.put("assetId",1);
        params.put("depositType",0);
        params.put("timeType",0);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", params);
        System.out.println(cmdResp.getResponseData());
        //1444559ce65de3dd23811fd2170ec126e11416e54dd5468079015dc3c8946245
    }

    @Test
    public void depositCommit()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        //组装交易
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setHeight(100);
        //组装blockHeader
        params.put("blockHeader", RPCUtil.encode(blockHeader.serialize()));
        params.put("tx","05006eaeecca67010049e09304000000000000000000000000000100014a25417a133876da5e0cdd04a983a8a5d8e7017200205d245e366862da82a1bd36745e1719e8b73e45dc320467d8639f9e0c82c397676801170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100801a06000000000000000000000000000800000000000000000001170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100e0930400000000000000000000000000ffffffff00");
        //params.put("tx","0500bdc742a167010049e09304000000000000000000000000000100014a25417a133876da5e0cdd04a983a8a5d8e7017200207d53655ffdb1bd3b5a05bc4d6e14d7c9980ff22e889fa7c2374e2c4b9cd8119f6801170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100801a06000000000000000000000000000800000000000000000001170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100e0930400000000000000000000000000ffffffff00");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositCommit", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void depositRollback()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        //组装交易
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setHeight(100);
        //组装blockHeader
        params.put("blockHeader", RPCUtil.encode(blockHeader.serialize()));
        params.put("tx","0500bdc742a167010049e09304000000000000000000000000000100014a25417a133876da5e0cdd04a983a8a5d8e7017200207d53655ffdb1bd3b5a05bc4d6e14d7c9980ff22e889fa7c2374e2c4b9cd8119f6801170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100801a06000000000000000000000000000800000000000000000001170100014a25417a133876da5e0cdd04a983a8a5d8e7017201000100e0930400000000000000000000000000ffffffff00");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositRollBack", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void withdraw()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("txHash","1444559ce65de3dd23811fd2170ec126e11416e54dd5468079015dc3c8946245");
        params.put("password", "nuls123456");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdraw", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void withdrawCommit()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        params.put("tx","");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdrawCommit", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void withdrawRollback()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        params.put("tx","");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdrawRollBack", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void getDepositList()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getDepositList", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void paramTest()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put("intCount",1);
        params.put("byteCount",125);
        params.put("shortCount",1555);
        params.put("longCount",666666);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.KE.abbr, "paramTestCmd", params);
        System.out.println(cmdResp);

    }

    @Test
    public void getRateAddition()throws Exception{
        Map<String,Object>params = new HashMap<>();
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getRateAddition", params);
        System.out.println(cmdResp.getResponseData());
    }
}
