package io.nuls.consensus.test.rpc;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Node related operation testing
 * Node-related operation testing
 *
 * @author tag
 * 2018/12/1
 * */
public class AgentTest {
    protected  String success = "1";
//    @BeforeClass
    public static void start() throws Exception {
        NoUse.mockModule();
    }

    @Test
    public void test() throws IOException {
        int[] arr = new int[]{1, 2, 5, 9};
        for (int i = 0; i < 100; i++) {
            Agent agent = new Agent();
            agent.setDeposit(BigInteger.valueOf(200000*i));
            agent.setAgentAddress(AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]));
            agent.setPackingAddress(AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]));
            agent.setRewardAddress(AddressTool.getAddress(new ECKey().getPubKey(), arr[i % 4]));
            System.out.println(HexUtil.encode(agent.serialize()));
        }
    }

    @Test
    /**
     * Create nodes
     * */
    public void createAgent()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put("agentAddress","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put(Constants.CHAIN_ID,2);
        params.put("deposit","2000000000000");
        params.put("packingAddress","tNULSeBaMmcSM2rzbZ4HQzv1ge4KT828W3PFKx");
        params.put("password","nuls123456");
        params.put("rewardAddress","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
        System.out.println(cmdResp.getResponseData());
        //acfcb0e25db677da9534bd5dd7643d2c750c4a8f06678a33a20d90bf6eb78f6b
    }

    @Test
    /**
     * Stop node
     * */
    public void stopAgent()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("password", "nuls123456");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", params);
        System.out.println(cmdResp.getResponseData());
    }


    /**
     * Additional margin
     * */
    @Test
    public void appendAgentDeposit()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("password", "nuls123456");
        params.put("amount","30000000000000");
        params.put("agentHash","76bedb5b3a741c006a5687845b9e93440cb24d1b821e242188dda7d46c3935b7");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_appendAgentDeposit", params);
        System.out.println(cmdResp.getResponseData());
        //5f03675051ad879731627a1a6a10cf82bea52e0baa527b55d776416847adaa4f
    }

    /**
     * Withdrawal of margin
     * */
    @Test
    public void reduceAgentDeposit()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        params.put("password", "nuls123456");
        params.put("amount","4000000000000");
        params.put("agentHash","ccc3964b08cf322beca22473236eb6790be4000d75119d36be2b9df4375de7a7");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_reduceAgentDeposit", params);
        System.out.println(cmdResp.getResponseData());
        //029225ad47238f9880f344816dbf7a04dc1d36252e76363a2629259309a8df00
    }

    @Test
    /**
     * Get node list
     * */
    public void getAgentList()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getAgentList", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void getAgentInfo()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("agentHash","d2a70dfb8eb68298fd748c4fab115058e90877bfd0939091ab937e904d027cc6");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getAgentInfo", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void getAgentStatus()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        params.put("agentHash","0020fef3f394953c601f6abe82f223d5c5673d3b4d7461e575f663954a7c4e055317");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getAgentStatus", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void isConsensusAgent()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,2);
        params.put("address","tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_isConsensusAgent", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    public void testBigInteger()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,new BigInteger("26778686868678686867"));
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_testBigInteger", params);
        System.out.println(cmdResp.getResponseData());
    }
}
