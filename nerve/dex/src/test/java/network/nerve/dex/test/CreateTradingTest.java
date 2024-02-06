package network.nerve.dex.test;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class CreateTradingTest {


    static int chainId = 9;
    static int assetId = 1;


    @Before
    public void before() throws Exception {
        NoUse.mockModule(8771);
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8771");
    }
    /**
     * Create transaction pairs：Test Network
     */
    @Test
    public void sendCreateCoinTradingTxBeta() {
        try {
            Map params = new HashMap();
            params.put("address", "TNVTdTSPQvEngihwxqwCNPq3keQL1PwrcLbtj");
            params.put("password", "nuls123456");
            params.put("quoteAssetChainId", 5);
            params.put("quoteAssetId", 1);
            params.put("scaleQuoteDecimal", 6);
            params.put("baseAssetChainId", 2);
            params.put("baseAssetId", 1);
            params.put("scaleBaseDecimal", 6);
            params.put("minBaseAmount", new BigInteger("10000000"));
            params.put("minQuoteAmount", new BigInteger("10000000"));

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createCoinTradingTx", params);

            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createCoinTradingTx");
            String txHash = (String) callResult.get("txHash");
            Log.info("---tradingHash: " + txHash);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * Create transaction pairs:Main network
     */
    @Test
    public void sendCreateCoinTradingTx() {
        try {
            Map params = new HashMap();
            params.put("address", "NERVEepb65rhpGjxAnC3sxBia5mYMvT4kqvrKa");
            params.put("password", "nuls123456");
            params.put("quoteAssetChainId", 9);
            params.put("quoteAssetId", 1);
            params.put("scaleQuoteDecimal", 6);
            params.put("baseAssetChainId", 9);
            params.put("baseAssetId", 4);
            params.put("scaleBaseDecimal", 6);
            params.put("minBaseAmount", new BigInteger("100000000000000000"));
            params.put("minQuoteAmount", new BigInteger("10000000"));

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createCoinTradingTx", params);

            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createCoinTradingTx");
            String txHash = (String) callResult.get("txHash");
            Log.info("---tradingHash: " + txHash);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
