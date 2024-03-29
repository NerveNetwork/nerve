package io.nuls.consensus.test.rpc;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Address;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.BlockRoundData;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consensus module related configuration operation test
 * Configuration Operation Test of Consensus Module
 *
 * @author tag
 * 2018/12/1
 * */
public class ConsensusTest {
    private  int chainId = 2;
    private  String success = "1";

    @BeforeClass
    public static void start() throws Exception {
        NoUse.mockModule();
    }

    @Test
    /**
     * Obtain current round information
     * */
    public void getRoundInfo() throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getRoundInfo", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    /**
     * Modify node consensus status
     * */
    public void updateAgentConsensusStatus()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_updateAgentConsensusStatus", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    /**
     * Modify node packaging status
     * */
    public void updateAgentStatus()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("status", 1);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_updateAgentStatus", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    /**
     * Obtain consensus information from the entire network
     * */
    public void getWholeInfo()throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getWholeInfo", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    /**
     * Obtain consensus information for the specified account
     * */
    public void getInfo()throws Exception{
        Address agentAddress = new Address(1,(byte)1, SerializeUtils.sha256hash160("a5WhgP1iu2Qwt5CiaPTV4Fe2Xqmfd".getBytes()));
        Map<String,Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", "tNULSeBaN5tSoHKGxZ8vBZNAPHTFJ77q5qTDR8");
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getInfo", params);
        System.out.println(cmdResp.getResponseData());
    }

    @Test
    /**
     * Obtain penalty information for the entire network or specified account
     * */
    public void  getPunishList()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,1);
        Address agentAddress = new Address(1,(byte)1, SerializeUtils.sha256hash160("a5WhgP1iu2Qwt5CiaPTV4Fe2Xqmfd".getBytes()));
        params.put("address",agentAddress.getBase58());
        params.put("type",1);
        Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getPublishList", params);
        System.out.println(cmdResp.getResponseData());
        //"fb8017b246114784749d46eebff4c34f446b500521f0cb19f66118O2ea8c942f7688180141c5a4feb65e24aedb5b529d";
    }

    /**
     * Node creation to packaging data preparation
     * */
    @Test
    public void packing(){
        //1.Create an account
        List<String> accountList;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, 2);
            params.put("count", 4);
            params.put("password", null);
            Response cmdResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params);
            if (!cmdResp.isSuccess()) {
                return;
            }
            accountList = (List<String>) ((HashMap)((HashMap) cmdResp.getResponseData()).get("ac_createAccount")).get("list");
            if(accountList == null || accountList.size() == 0){
                return;
            }
            System.out.println("accountList:"+accountList);

            //2.Create node transactions
            String packingAddress = accountList.get(0);
            String agentAddress = accountList.get(1);
            String rewardAddress = accountList.get(2);
            Map<String,Object> caParams = new HashMap<>();
            caParams.put("agentAddress",agentAddress);
            caParams.put(Constants.CHAIN_ID,2);
            caParams.put("deposit",20000);
            caParams.put("commissionRate",10);
            caParams.put("packingAddress",packingAddress);
            caParams.put("password","");
            caParams.put("rewardAddress",rewardAddress);
            Response caResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", caParams);
            HashMap caResult = (HashMap)((HashMap) caResp.getResponseData()).get("cs_createAgent");
            String caTxHex = (String)caResult.get("txHex");
            System.out.println("createAgent:"+caResp.getResponseData());

            //3.Create node transaction submission
            Map<String,Object>caTxCommit = new HashMap<>();
            caTxCommit.put("chainId",2);
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.setHeight(0);
            caTxCommit.put("blockHeader", RPCUtil.encode(blockHeader.serialize()));
            caTxCommit.put("tx",caTxHex);
            Response caCommitResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgentCommit", caTxCommit);
            HashMap caCommitResult = (HashMap)((HashMap) caCommitResp.getResponseData()).get("cs_createAgentCommit");
            String agentHash = (String)caCommitResult.get("agentHash");
            System.out.println("createAgentCommit:"+caCommitResp.getResponseData());


            //4.Entrust node transaction creation
            String depositAddress = accountList.get(3);
            Map<String,Object> dpParams = new HashMap<>();
            dpParams.put(Constants.CHAIN_ID,2);
            dpParams.put("address",depositAddress);
            dpParams.put("agentHash",agentHash);
            dpParams.put("deposit","300000");
            Response dpResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
            HashMap dpResult = (HashMap)((HashMap) dpResp.getResponseData()).get("cs_depositToAgent");
            String dpTxHex = (String)dpResult.get("txHex");
            System.out.println("createDeposit"+cmdResp.getResponseData());

            //5.Entrusted transaction submission
            Map<String,Object>dpTxCommitParams = new HashMap<>();
            dpTxCommitParams.put(Constants.CHAIN_ID,2);
            BlockHeader blockHeader1 = new BlockHeader();
            blockHeader.setHeight(0);
            dpTxCommitParams.put("blockHeader", RPCUtil.encode(blockHeader1.serialize()));
            dpTxCommitParams.put("tx",dpTxHex);
            Response dpCommitResp = NerveCoreResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositCommit", dpTxCommitParams);
            System.out.println("deposit transaction commit:"+dpCommitResp.getResponseData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)throws Exception{
        BlockRoundData roundData = new BlockRoundData();
        roundData.setConsensusMemberCount(1);
        roundData.setPackingIndexOfRound(1);
        roundData.setRoundIndex(1);
        roundData.setRoundStartTime(1L);
        System.out.println(RPCUtil.encode(roundData.serialize()));
    }
}
