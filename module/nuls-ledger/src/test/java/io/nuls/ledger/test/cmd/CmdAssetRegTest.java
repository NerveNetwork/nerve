package io.nuls.ledger.test.cmd;

import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CmdAssetRegTest {
    @Before
    public void before() throws Exception {
        NoUse.mockModule();
    }

    @Test
    public void getAssetRegInfoTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, 2);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfo", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }
    @Test
    public void chainAssetTxRegTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("assetSymbol","USDI");
        params.put("assetName","USDI");
        params.put("initNumber",100000000);
        params.put("decimalPlace",8);
        params.put("txCreatorAddress","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("assetOwnerAddress","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("password","nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }

    @Test
    public void getAssetRegInfoByHashTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("chainId",1);
        params.put("txHash","a2a66c528697d787d9d9a1f9af9db13df264718a1492ceefa26393075bb85c50");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfoByHash", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }
    @Test
    public void getAssetContractAddressTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("chainId",1);
        params.put("assetId",2);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetContractAddress", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }
    @Test
    public void getAssetRegInfoByAssetIdTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("chainId",1);
        params.put("assetId",2);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetRegInfoByAssetId", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }
    @Test
    public void getAssetContractAssetIdTest() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("contractAddress","NULSd6HgYEvN6e8kVdrJ3pBzPFq1T6p6j6pjv");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getAssetContractAssetId", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }

}
