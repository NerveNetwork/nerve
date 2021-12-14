package io.nuls.ledger.test.cmd;

import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.RpcCall;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CmdAssetRegTest {
    @Before
    public void before() throws Exception {
        NoUse.mockModule(8771);
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
        params.put("assetSymbol","VET");
        params.put("assetName","VET");
        params.put("initNumber",5000000);
        params.put("decimalPlace",8);
        params.put("txCreatorAddress","NERVEepb686tCEBDEWSvoifp8swRK6WDMu7TPE");
        params.put("assetOwnerAddress","NERVEepb686tCEBDEWSvoifp8swRK6WDMu7TPE");
        params.put("password","nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }
    @Test
    public void chainAssetTxRegTestBeta() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("assetSymbol","OKB");
        params.put("assetName","OKB");
        params.put("initNumber",1000000);
        params.put("decimalPlace",18);
        params.put("txCreatorAddress","TNVTdTSPQvEngihwxqwCNPq3keQL1PwrcLbtj");
        params.put("assetOwnerAddress","TNVTdTSPQvEngihwxqwCNPq3keQL1PwrcLbtj");
        params.put("password","nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
    }

    @Test
    public void chainAssetTxRegTest2() throws Exception {
        // Build params map
        Map<String,Object> params = new HashMap<>();
        params.put("assetSymbol","USDT");
        params.put("assetName","USDT");
        params.put("initNumber",100000000);
        params.put("decimalPlace",6);
        params.put("txCreatorAddress","TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA");
        params.put("assetOwnerAddress","TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA");
        params.put("password","nuls123456");
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
        Log.debug("response {}", JSONUtils.obj2json(response));
        TimeUnit.SECONDS.sleep(4);
        params = new HashMap<>();
        params.put("assetSymbol","BTC");
        params.put("assetName","BTC");
        params.put("initNumber",100000000);
        params.put("decimalPlace",8);
        params.put("txCreatorAddress","TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA");
        params.put("assetOwnerAddress","TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA");
        params.put("password","nuls123456");
        response = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "chainAssetTxReg", params);
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


    @Test
    public void getAssetInfoTest() throws Exception {
        Map params = new HashMap();
        params.put("chainId",4);
        Map data = (Map) RpcCall.request(ModuleE.LG.abbr, "lg_get_all_asset", params);
        List<Map<String,Object>> list = (List<Map<String, Object>>) data.get("assets");
        Log.info("{}",list);
    }

}
