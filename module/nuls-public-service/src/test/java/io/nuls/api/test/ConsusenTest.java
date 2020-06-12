package io.nuls.api.test;

import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-09 16:34
 * @Description: 功能描述
 */
public class ConsusenTest extends BaseTestCase {

    @Test
    public void testCmd() throws Exception {
        Map<String,Object> params = new HashMap<>();
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getRateAddition", params);
        Log.info("{}",cmdResp.getResponseData());
    }

}
