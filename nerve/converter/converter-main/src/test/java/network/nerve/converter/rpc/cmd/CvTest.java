package network.nerve.converter.rpc.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.NonceBalance;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CvTest {
    /*
    static String address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";

    static String address30 = "tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ";
    static String address31 = "tNULSeBaMrbmG67VrTJeZswv4P2uXXKoFMa6RH";
    */

    static String address20 = "TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5";
    static String address21 = "TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz";
    static String address22 = "TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw";
    static String address23 = "TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv";
    static String address24 = "TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD";
    static String address25 = "TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5";
    static String address26 = "TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg";
    static String address27 = "TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n";
    static String address28 = "TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu";
    static String address29 = "TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf";
    static String address30 = "TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9";
    static String address31 = "TNVTdTSPRyiWcpbS65NmT5qyGmuqPxuKv8SF4";



    private Chain chain;
    static int chainId = 5;
    static int assetId = 1;
    static int heterogeneousChainId = 101;
    static int heterogeneousAssetId = 1;

    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId, "UTF-8"));
    }

    @Test
    public void importPriKeyTest() {
        // 公钥: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd
        // 公钥: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT
        // 公钥: 028181b7534e613143befb67e9bd1a0fa95ed71b631873a2005ceef2774b5916df
//        importPriKey("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499", password);//种子出块地址 tNULSeBaMmShSTVwbU4rHkZjpD98JgFgg6rmhF TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx

        importPriKey("2349820348023948234982357923561293479238579234792374923472343434", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24 TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1 TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2 TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf

        importPriKey("50a0631304ba75b1519c96169a0250795d985832763b06862167aa6bbcd6171f", password);// 出块 tNULSeBaMrbmG67VrTJeZswv4P2uXXKoFMa6RH TNVTdTSPRyiWcpbS65NmT5qyGmuqPxuKv8SF4 0x18354c726a3ef2b7da89def0fce1d15d679ae16a

        importPriKey("b36097415f57fe0ac1665858e3d007ba066a7c022ec712928d2372b27e8513ff", password);//ETH 测试网地址 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9
    }

    public static void importPriKey(String priKey, String pwd) {
        try {
            // 账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVirtualBank() throws Exception {
        //ConverterCmdConstant.VIRTUAL_BANK_INFO
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("balance", true);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, ConverterCmdConstant.VIRTUAL_BANK_INFO, params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void ledgerAssetQueryAll() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "lg_get_all_asset", params);
        System.out.println(JSONUtils.obj2PrettyJson(cmdResp));
    }

    @Test
    public void getBalance() throws Exception {
        NonceBalance balance = LedgerCall.getBalanceNonce(chain, chainId, 2, address30);
        System.out.println("ETH:" + balance.getAvailable());
        NonceBalance balance3 = LedgerCall.getBalanceNonce(chain, chainId, 3, address30);
        System.out.println("USDX:" + balance3.getAvailable());
        NonceBalance balance4 = LedgerCall.getBalanceNonce(chain, chainId, 1, address30);
        System.out.println("NVT:" + balance4.getAvailable());
    }


    @Test
    public void withdrawalETH() throws Exception {
        //账户已存在则覆盖 If the account exists, it covers.
        for (int i = 1; i <= 1; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("assetId", 2);
            params.put("heterogeneousAddress", "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
            params.put("amount", "100000000000000000");
            params.put("remark", "提现");
            params.put("address", address30);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.debug("number:{}, hash:{}", i , hash);
        }
        /**
         * cmd
         * redeem 2 0xfa27c84eC062b2fF89EB297C24aaEd366079c684 0.3 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
         */
    }

    @Test
    public void withdrawalERC20() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            params.put("assetId", 3);
            params.put("heterogeneousAddress", "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
//            params.put("amount", (10 * i) + "000000");
            params.put("amount", "10000000");
            params.put("remark", "提现");
            params.put("address", address30);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.debug("number:{}, hash:{}", i , hash);
        }
        /**
         * cmd
         * redeem 3 0xfa27c84eC062b2fF89EB297C24aaEd366079c684 100 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
         */
    }

    @Test
    public void createAgent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", address29);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("deposit", "40000000000000"); // 50W
        // 私钥:50a0631304ba75b1519c96169a0250795d985832763b06862167aa6bbcd6171f
        params.put("packingAddress", address30);
        params.put("password", password);
        params.put("rewardAddress", address29);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
        System.out.println(cmdResp.getResponseData());
    }

    /**
     * 追加保证金
     * */
    @Test
    public void appendAgentDeposit()throws Exception{
        Map<String,Object>params = new HashMap<>();
        params.put(Constants.CHAIN_ID,chainId);
        params.put("address",address29);
        params.put("password", password);
        params.put("amount","10000000000000");// 10W
        params.put("agentHash","daa0902b5f1528805d00c65dabc3c381dbbb2470d1fe1b7980479e3db9a17426");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_appendAgentDeposit", params);
        System.out.println(cmdResp.getResponseData());
        //5f03675051ad879731627a1a6a10cf82bea52e0baa527b55d776416847adaa4f
    }

    @Test
    public void stopAgent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, 2);
        params.put("address", address29);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", params);
        System.out.println(cmdResp.getResponseData());
    }


    @Test
    public void proposal() throws Exception {
        //账户已存在则覆盖 If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.REFUND.value());
        params.put("content", "这是一个提案的内容...");
        params.put("heterogeneousChainId", heterogeneousChainId);
        params.put("heterogeneousTxHash", "0x7488d23bf46af751194e1fc3f1794418d61326ab77bf151f68c6c6546c2e20b7");
        params.put("businessAddress", address26);
        params.put("hash", "954dcd9a6ae9a1cc9f341126c3c05c48feb95b47ba191d76f42d4bcf1b27448a");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "提案");
        params.put("address", address20);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_proposal", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_proposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);

    }

    @Test
    public void voteProposal() throws Exception {
        //账户已存在则覆盖 If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("proposalTxHash", "86a8640eac276aa00c5fb8d73ffb723a20c6c74619f1e1895149a85845578286");
        params.put("choice", ProposalVoteChoiceEnum.FAVOR.value());
        params.put("remark", "投票remark");
        params.put("address", "tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp");
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_voteProposal", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_voteProposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);
        // vote <proposalTxHash> <choice> <address>  [remark] --vote
        // vote da2062df25220c390b74fa362fab43232068bf7c4e4cbcd2c59100f97f19bb17 1 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
        // vote <proposalTxHash> 1 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp 投票
    }

    @Test
    public void TxInstance() throws Exception {
        AddressTool.addPrefix(5, "NERVE");
        String str ="2b0027a7e15e2074657374266e6273703b7769746864726177616c266e6273703b4554482e2e2e2b2a307863313144393934333830356535366236333041343031443462643941323935353033353345466131fd16010217050001b9978dbea4abebb613b6be2d0d66b4179d2511cb050002000000c16ff28623000000000000000000000000000000000000000000000000000800000000000000000017050001b9978dbea4abebb613b6be2d0d66b4179d2511cb05000100a06a0d540200000000000000000000000000000000000000000000000000000008000000000000000000021705000129cfc6376255a78451eeb4b129ed8eacffa2feef050002000000c16ff28623000000000000000000000000000000000000000000000000000000000000000000170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e050001006400000000000000000000000000000000000000000000000000000000000000000000000000000069210369b20002bc58c74cb6fd5ef564f603834393f53bed20c3314b4b7aba8286a7e0463044022039da405be1d3c149d96719472aaea4aa9c7fe28390cd41960581dc513d58bc8a02203e0eab8dc4cae721e3acc208d55ce9784290b8535b1c1000968017c0f241ad86";
        Transaction tx = ConverterUtil.getInstance(str, Transaction.class);
        System.out.println(tx.format(WithdrawalTxData.class));
    }

    // resetbank 101 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
}
