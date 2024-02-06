package network.nerve.converter.rpc.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.model.bo.NonceBalance;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.dto.WithdrawalTxDTO;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.BaseCall;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.ConverterUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.converter.constant.ConverterConstant.DISTRIBUTION_FEE_10;

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
    static int bnbChainId = 102;
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
        // Public key: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//Seed block address tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd
        // Public key: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//Seed block address tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe TNVTdTSPNeoGxTS92S2r1DZAtJegbeucL8tCT
        // Public key: 028181b7534e613143befb67e9bd1a0fa95ed71b631873a2005ceef2774b5916df
//        importPriKey("fbcae491407b54aa3904ff295f2d644080901fda0d417b2b427f5c1487b2b499", password);//Seed block address tNULSeBaMmShSTVwbU4rHkZjpD98JgFgg6rmhF TNVTdTSPLpegzD3B6qaVKhfj6t8cYtnkfR7Wx

        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD TNVTdTSPNEpLq2wnbsBcD8UDTVMsArtkfxWgz
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24 TNVTdTSPRyJgExG4HQu5g1sVxhVVFcpCa6fqw
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD TNVTdTSPUR5vYdstWDHfn5P8MtHB6iZZw3Edv
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL TNVTdTSPPXtSg6i5sPPrSg3TfFrhYHX5JvMnD
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL TNVTdTSPT5KdmW1RLzRZCa5yc7sQCznp6fES5
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm TNVTdTSPPBao2pGRc5at7mSdBqnypJbMqrKMg
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1 TNVTdTSPLqKoNh2uiLAVB76Jyq3D6h3oAR22n
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2 TNVTdTSPNkjaFbabm5P73m7VHBRQef4NDsgYu
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn TNVTdTSPRMtpGNYRx98WkoqKnExU9pWDQjNPf

        importPriKey("50a0631304ba75b1519c96169a0250795d985832763b06862167aa6bbcd6171f", password);// Chunking tNULSeBaMrbmG67VrTJeZswv4P2uXXKoFMa6RH TNVTdTSPRyiWcpbS65NmT5qyGmuqPxuKv8SF4 0x18354c726a3ef2b7da89def0fce1d15d679ae16a

        importPriKey("b36097415f57fe0ac1665858e3d007ba066a7c022ec712928d2372b27e8513ff", password);//ETH Test network address tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9
    }

    public static void importPriKey(String priKey, String pwd) {
        try {
            // Overwrite if account already exists If the account exists, it covers.
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
        System.out.println("BNB:" + balance3.getAvailable());
//        NonceBalance balance0 = LedgerCall.getBalanceNonce(chain, chainId, 3, address25);
//        System.out.println(address25 + " USDX:" + balance0.getAvailable());
        NonceBalance balance4 = LedgerCall.getBalanceNonce(chain, chainId, 4, address30);
        System.out.println("USDX:" + balance4.getAvailable());
        NonceBalance balance5 = LedgerCall.getBalanceNonce(chain, chainId, 1, address30);
        System.out.println("NVT:" + balance5.getAvailable());
        NonceBalance balance6 = LedgerCall.getBalanceNonce(chain, chainId, 5, address30);
        System.out.println("USDI:" + balance6.getAvailable());
        NonceBalance balance7 = LedgerCall.getBalanceNonce(chain, chainId, 6, address30);
        System.out.println("ENVT:" + balance7.getAvailable());
    }

    @Test
    public void withdrawalNVT() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        for (int i = 1; i <= 1; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            // Withdrawal amountETH
            String amount = "100000";
            BigDecimal am = new BigDecimal(amount).movePointRight(8);
            amount = am.toPlainString();
            params.put("assetChainId", chainId);
            params.put("assetId", 1);
            params.put("heterogeneousAddress", "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
            params.put("distributionFee", new BigInteger("1000000000"));
            params.put("amount", amount);
            params.put("remark", "Withdrawal");
            params.put("address", address30);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.debug("number:{}, hash:{}", i, hash);
        }
        /**
         * cmd
         * redeem 2 0xfa27c84eC062b2fF89EB297C24aaEd366079c684 0.3 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
         */
    }

    @Test
    public void withdrawalETH() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            // Withdrawal amountETH
            String amount = "0.2";
            BigDecimal am = new BigDecimal(amount).movePointRight(18);
            amount = am.toPlainString();
            params.put("assetChainId", chainId);
            params.put("assetId", 2);
            params.put("heterogeneousAddress", "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
            params.put("distributionFee", new BigInteger("1000000000"));
            params.put("amount", amount);
            params.put("remark", "Withdrawal");
            params.put("address", address30);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.debug("number:{}, hash:{}", i, hash);
        }
        /**
         * cmd
         * redeem 2 0xfa27c84eC062b2fF89EB297C24aaEd366079c684 0.3 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
         */
    }

    @Test
    public void withdrawalERC20() throws Exception {
        for (int i = 1; i <= 1; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);

            // Withdrawal amountERC20
            String amount = "10";
            int decimal = 6; //Decimal places
            BigDecimal am = new BigDecimal(amount).movePointRight(decimal);
            amount = am.toPlainString();

            params.put("assetChainId", chainId);
            params.put("assetId", 5);
            params.put("heterogeneousAddress", "0xfa27c84eC062b2fF89EB297C24aaEd366079c684");
            params.put("distributionFee", new BigInteger("100000000"));
            params.put("amount", amount);
            params.put("remark", "Withdrawal");
            params.put("address", address30);
            params.put("password", password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal");
            String hash = (String) result.get("value");
            String txHex = (String) result.get("hex");
            Log.debug("number:{}, hash:{}", i, hash);
        }
        /**
         * cmd
         * redeem 3 0xfa27c84eC062b2fF89EB297C24aaEd366079c684 100 tNULSeBaMfQ6VnRxrCwdU6aPqdiPii9Ks8ofUQ
         */
    }


    @Test
    public void withdrawalAdditionalFee() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        // AddnvtHandling fees
        // Withdrawal amountETH
        String amount = "100";
        BigDecimal am = new BigDecimal(amount).movePointRight(8);
        amount = am.toPlainString();

        params.put("txHash", "5515d3af62b402560040f46b96e4caf5b84a6cd96da6f5a34f13e487d78e6dce");
        params.put("amount", amount);
        params.put("remark", "Additional handling fees");
        params.put("address", address30);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_withdrawal_additional_fee", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_withdrawal_additional_fee");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("txHex:{}", txHex);
        Log.debug("hash:{}", hash);
    }


    @Test
    public void createAgent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", address29);
        params.put(Constants.CHAIN_ID, chainId);
        params.put("deposit", "40000000000000"); // 50W
        // Private key:50a0631304ba75b1519c96169a0250795d985832763b06862167aa6bbcd6171f
        params.put("packingAddress", address30);
        params.put("password", password);
        params.put("rewardAddress", address29);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", params);
        System.out.println(cmdResp.getResponseData());
    }

    /**
     * Additional margin
     */
    @Test
    public void appendAgentDeposit() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address29);
        params.put("password", password);
        params.put("amount", "10000000000000");// 10W
        params.put("agentHash", "daa0902b5f1528805d00c65dabc3c381dbbb2470d1fe1b7980479e3db9a17426");
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
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);

        params.put("type", ProposalTypeEnum.EXPELLED.value());
        params.put("content", "This is the content of a proposal...");
        //params.put("heterogeneousChainId", bnbChainId);
        //params.put("heterogeneousTxHash", "0x178373fc06f888487790b9d7fa256f4a166451c5f3facf17a65fb250a7cd2ea1");
        params.put("businessAddress", "TNVTdTSPQvEngihwxqwCNPq3keQL1PwrcLbtj");
        //params.put("hash", "fac6cf4924910b3d30ff2509d43420bf34c030f6c4869e14bb5d94ed12d370a0");
        params.put("voteRangeType", ProposalVoteRangeTypeEnum.BANK.value());
        params.put("remark", "proposal");
        params.put("address", address20);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_proposal", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_proposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);
        // upgradeProposal <heterogeneousId> <newContractAddress> <address>
        // upgradeProposal 101 0xdcb777E7491f03D69cD10c1FeE335C9D560eb5A2 TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5
        // upgradeProposal 101 0xD02a06065596b48174A37087ea93fE9889E84636 TNVTdTSPVcqUCdfVYWwrbuRtZ1oM6GpSgsgF5
    }

    @Test
    public void voteProposal() throws Exception {
        //Overwrite if account already exists If the account exists, it covers.
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("proposalTxHash", "b4ae0561ebf1a196706960cc20c90f34400b2ee7f440ef4675cf7e84a1b0d485");
        params.put("choice", ProposalVoteChoiceEnum.FAVOR.value());
        params.put("remark", "voteremark");
        params.put("address", "TNVTdTSPLEqKWrM7sXUciM2XbYPoo3xDdMtPd");
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CV.abbr, "cv_voteProposal", params);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cv_voteProposal");
        String hash = (String) result.get("value");
        String txHex = (String) result.get("hex");
        Log.debug("hash:{}", hash);
        Log.debug("txHex:{}", txHex);
        // vote <proposalTxHash> <choice> <address>  [remark] --vote
        // vote da2062df25220c390b74fa362fab43232068bf7c4e4cbcd2c59100f97f19bb17 1 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
        // vote <proposalTxHash> 1 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp vote
    }

    @Test
    public void TxInstance() throws Exception {
//        AddressTool.addPrefix(5, "NERVE");
//        String str ="2d002b4a6c5f06e68f90e6a1889c071ee8bf99e698afe4b880e4b8aae68f90e6a188e79a84e58685e5aeb92e2e2e65004230783235393065306439343535383061346237363365636139616531386166653539373064643333343134343466303033626332363964313635386366623566386614dcb777e7491f03d69cd10c1fee335c9d560eb5a220fac6cf4924910b3d30ff2509d43420bf34c030f6c4869e14bb5d94ed12d370a0018c0117050001f7ec6473df12e751d64cf20a8baa7edd50810f810500010040d79d3b000000000000000000000000000000000000000000000000000000000800000000000000000001170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e0500010000ca9a3b000000000000000000000000000000000000000000000000000000000000000000000000692103958b790c331954ed367d37bac901de5c2f06ac8368b37d7bd6cd5ae143c1d7e3463044022075b8265017dce68982308b7af691c440dbcaafd3aeb85b3694d3c2bcc15190c202202f68676f6ff217373dc54e8cb9782a9b5461116e48d2939d73d89d81693b2024";
//        Transaction tx = ConverterUtil.getInstance(str, Transaction.class);
//        System.out.println(tx.getHash().toHex());
//        System.out.println(tx.format(ProposalTxData.class));
        //06889e8ac40c14d3f4e83a58f78986b29522c742f8c483d1fe088fec2501d3e0
/*
        AddressTool.addPrefix(9, "NERVE");
        String str = "210369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c54630440220268f58cd001602bc291e4a207461b50091a3661c86b4e72f8e47cbac90beaa7f0220758c55209bf489613f3b9e7269e139e546a848084c1e00aaaa7ee119d3261e9e21035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b80346304402200dc97ae858097570b43d85d4afe8b02a97f4f6a5e8704ee6e7461305b83384ab02205d2738a060b8279df6fce64c8a9067420c4e2f8a9e5cf112a2877cf6fbef8ef62102ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b946304402202e6be474267c601457858ef4253b218c1ccb918e74ec4a9fdbf801bc6a22121702205f8dc844cda548aa560680b3e38f784b391408366a37f6a3444f60af620070ab210351a8fc85a6c475b102f3fe5bd2479c1d08e58237463f6c6ccf84e95ad396b783463044022055c2eac0081d300a8c851630b1283a0cca0f2a800d84034848cc947803d891b902204b9ce06d1858d25ed48c2d3723e8f62b35a0b147c53f97a7ffbe80f6d47952982103c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee2147304502210088be74db21ddcace414e466d722b4d7bd98d996c234a482b4e84c855be9dc84a022057d45015158f9eb2e34c0bde2eaafe9a299df06a35618706584f5bea23a4f9c22103ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c946304402206382c9cd84b5918e9b4029b81eb1eea8f888c5b74242d68fef023763fc9360e802204dff1389f05a29d4266b0a0c062eaee88a868a0b8b33fbb80c1ac1b58bffd0f32102dda7bf54b7843aef842222f5c79405ca91313ac8c59296cf7b38203c09b40ba846304402206b2324f9d44a015a45a43aa513c531c2857cd3ecb3ec65f2329997e284ef154c022042a9cde9459aeb04ca3623ec120049c3257872c8d605a000ec60d4b1c45703c721020c60dd7e0016e174f7ba4fc0333052bade8c890849409de7b6f3d26f0ec64528473045022100bd8204aaa79c5f1cf59e321899cceafc646c55936b5d2a266dcf46526ec33a0802203c08aa0f33728eb5f1310464b0c3d76a2cb3ea6256abcde85cc76488861b2667210308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b473045022100f0fa5aebcf61c75a23359ddf193d50729c71b2cb0c4a8efa99580cd1c4b956bf02202e943b76f993267d3e0ea16f7a12211b0196162b445428d1d0f0fe4dbbfcd2982102c8ab66541215350c4e82073c825d0d96dfe21aed1acfca3bdd91ac4d48cb3499473045022100b70c9e1749073f3ded48e34e7d5a3bb71fd76aac184d4af7c6a0904f04ddbf26022061e1a7b32acca7d663530b33276145769aec547f689d02bac5c511798451c1f5";
        TransactionSignature instance = ConverterUtil.getInstance(str, TransactionSignature.class);

        for (P2PHKSignature signature : instance.getP2PHKSignatures()) {
            byte[] address = AddressTool.getAddress(signature.getPublicKey(), 9);
            String addr = AddressTool.getStringAddressByBytes(address);
            System.out.println(addr);
        }*/

//
//        String signStr = "21037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a086346304402201c2a12016971ba7045c83e164648705c7e073813b90d3b56fe77db549604b7920220236f13bcb01cac09d4fedad70c740805bc22325b05fa0695225b8e37178af276";
//        P2PHKSignature sign = ConverterUtil.getInstance(signStr, P2PHKSignature.class);
//        byte[] address = AddressTool.getAddress(sign.getPublicKey(), 5);
//        String addr = AddressTool.getStringAddressByBytes(address);
//        System.out.println(addr);

        String str1="2d002b4a6c5f06e68f90e6a1889c071ee8bf99e698afe4b880e4b8aae68f90e6a188e79a84e58685e5aeb92e2e2e65004230783235393065306439343535383061346237363365636139616531386166653539373064643333343134343466303033626332363964313635386366623566386614dcb777e7491f03d69cd10c1fee335c9d560eb5a220fac6cf4924910b3d30ff2509d43420bf34c030f6c4869e14bb5d94ed12d370a0018c0117050001f7ec6473df12e751d64cf20a8baa7edd50810f810500010040d79d3b000000000000000000000000000000000000000000000000000000000800000000000000000001170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e0500010000ca9a3b000000000000000000000000000000000000000000000000000000000000000000000000692103958b790c331954ed367d37bac901de5c2f06ac8368b37d7bd6cd5ae143c1d7e3463044022075b8265017dce68982308b7af691c440dbcaafd3aeb85b3694d3c2bcc15190c202202f68676f6ff217373dc54e8cb9782a9b5461116e48d2939d73d89d81693b2024";
        String str2="2d002b4a6c5f06e68f90e6a1889c071ee8bf99e698afe4b880e4b8aae68f90e6a188e79a84e58685e5aeb92e2e2e65004230783235393065306439343535383061346237363365636139616531386166653539373064643333343134343466303033626332363964313635386366623566386614dcb777e7491f03d69cd10c1fee335c9d560eb5a220fac6cf4924910b3d30ff2509d43420bf34c030f6c4869e14bb5d94ed12d370a0018c0117050001f7ec6473df12e751d64cf20a8baa7edd50810f810500010040d79d3b000000000000000000000000000000000000000000000000000000000800000000000000000001170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e0500010000ca9a3b000000000000000000000000000000000000000000000000000000000000000000000000692103958b790c331954ed367d37bac901de5c2f06ac8368b37d7bd6cd5ae143c1d7e3463044022075b8265017dce68982308b7af691c440dbcaafd3aeb85b3694d3c2bcc15190c202202f68676f6ff217373dc54e8cb9782a9b5461116e48d2939d73d89d81693b2024";
        String str3="2d002b4a6c5f06e68f90e6a1889c071ee8bf99e698afe4b880e4b8aae68f90e6a188e79a84e58685e5aeb92e2e2e65004230783235393065306439343535383061346237363365636139616531386166653539373064643333343134343466303033626332363964313635386366623566386614dcb777e7491f03d69cd10c1fee335c9d560eb5a220fac6cf4924910b3d30ff2509d43420bf34c030f6c4869e14bb5d94ed12d370a0018c0117050001f7ec6473df12e751d64cf20a8baa7edd50810f810500010040d79d3b000000000000000000000000000000000000000000000000000000000800000000000000000001170500018ec4cf3ee160b054e0abb6f5c8177b9ee56fa51e0500010000ca9a3b000000000000000000000000000000000000000000000000000000000000000000000000692103958b790c331954ed367d37bac901de5c2f06ac8368b37d7bd6cd5ae143c1d7e3463044022075b8265017dce68982308b7af691c440dbcaafd3aeb85b3694d3c2bcc15190c202202f68676f6ff217373dc54e8cb9782a9b5461116e48d2939d73d89d81693b2024";

        System.out.println(str1.equals(str2));
        System.out.println(str1.equals(str3));


        AddressTool.addPrefix(5, "NERVE");
        Transaction tx1 = ConverterUtil.getInstance(str1, Transaction.class);
        System.out.println(tx1.getHash().toHex());
        Transaction tx2 = ConverterUtil.getInstance(str2, Transaction.class);
        System.out.println(tx2.getHash().toHex());
        Transaction tx3 = ConverterUtil.getInstance(str3, Transaction.class);
        System.out.println(tx3.getHash().toHex());
//        System.out.println(tx.format(ProposalTxData.class));


    }

    // resetbank 101 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp


    @Test
    public void createWithdrawal() throws Exception {
        Transaction tx = new Transaction(TxType.WITHDRAWAL);
        tx.setTxData(new WithdrawalTxData("0xfa27c84eC062b2fF89EB297C24aaEd366079c684").serialize());
        WithdrawalTxDTO txDTO = new WithdrawalTxDTO();
        txDTO.setAmount(new BigInteger("1000000"));
        txDTO.setAssetChainId(5);
        txDTO.setAssetId(3);
        txDTO.setSignAccount(new SignAccountDTO(address30, password));
        tx.setCoinData(assembleWithdrawalCoinData(chain,  txDTO));
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        sign(tx, txDTO.getSignAccount().getAddress(), txDTO.getSignAccount().getPassword());
        newTx(tx);
    }
    private void newTx(Transaction tx) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("tx", RPCUtil.encode(tx.serialize()));
        HashMap result = (HashMap) BaseCall.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
        System.out.println(result.get("hash"));
    }


    /**
     * Regarding transactionshashautograph(on line)
     * @param tx
     * @param address
     * @param password
     */
    public void sign(Transaction tx, String address, String password) throws Exception {
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address);
        params.put("password", password);
        params.put("data", RPCUtil.encode(tx.getHash().getBytes()));
        HashMap result = (HashMap) BaseCall.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
        String signatureStr = (String) result.get("signature");
        P2PHKSignature signature = new P2PHKSignature();
        signature.parse(new NulsByteBuffer(RPCUtil.decode(signatureStr)));
        p2PHKSignatures.add(signature);
        //Transaction signature
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        tx.setTransactionSignature(transactionSignature.serialize());
    }


    private byte[] assembleWithdrawalCoinData(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException {
        int withdrawalAssetId = withdrawalTxDTO.getAssetId();
        int withdrawalAssetChainId = withdrawalTxDTO.getAssetChainId();


        int chainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        BigInteger amount = withdrawalTxDTO.getAmount();
        String address = withdrawalTxDTO.getSignAccount().getAddress();
        //Withdrawal of assetsfrom
        CoinFrom withdrawalCoinFrom = getWithdrawalCoinFrom(chain, address, amount, withdrawalAssetChainId, withdrawalAssetId);
        List<CoinFrom> listFrom = new ArrayList<>();
        listFrom.add(withdrawalCoinFrom);
        if(withdrawalAssetChainId != chainId || assetId != withdrawalAssetId) {
            // As long as it is not the current chain master asset All require additional assemblycoinFrom
            CoinFrom withdrawalFeeCoinFrom = null;
            //Handling feesfrom Including heterogeneous chain subsidy handling fees
            withdrawalFeeCoinFrom = getWithdrawalFeeCoinFrom(chain, address, DISTRIBUTION_FEE_10);

            listFrom.add(withdrawalFeeCoinFrom);
        }
        //------------------------------------------------------------------
//        BigInteger amount2 = new BigInteger("1000000000000000000");
//        CoinFrom withdrawalCoinFrom2 = getWithdrawalCoinFrom(chain, address, amount2, withdrawalAssetChainId, 5);
//        listFrom.add(withdrawalCoinFrom2);
        //------------------------------------------------------------------

        String fee = "111111111111111111111111111111111111111111111111111111111111111111";
        String black = "000000000000000000000000000000000000000000000000000000000000000000";
        //assembleto

        List<CoinTo> listTo = new ArrayList<>();
        //==============
        CoinTo withdrawalCoinTo3 = new CoinTo(
                AddressTool.getAddress(HexUtil.decode(black), chain.getChainId()),
                withdrawalAssetChainId,
                5,
                new BigInteger("1000000000000000000"));
        listTo.add(withdrawalCoinTo3);
        //==============

        CoinTo withdrawalCoinTo = new CoinTo(
                AddressTool.getAddress(HexUtil.decode(black), chain.getChainId()),
                withdrawalAssetChainId,
                withdrawalAssetId,
                amount);

        listTo.add(withdrawalCoinTo);
        // Determine the temporary storage of subsidy fees for assembling heterogeneous chainsto
        CoinTo withdrawalFeeCoinTo = new CoinTo(
                AddressTool.getAddress(HexUtil.decode(fee), chain.getChainId()),
                chainId,
                assetId,
                DISTRIBUTION_FEE_10);
        listTo.add(withdrawalFeeCoinTo);
        CoinData coinData = new CoinData(listFrom, listTo);
        try {
            return coinData.serialize();
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }


    private CoinFrom getWithdrawalCoinFrom(
            Chain chain,
            String address,
            BigInteger amount,
            int withdrawalAssetChainId,
            int withdrawalAssetId) throws NulsException {
        //Withdrawal of assets
        if (BigIntegerUtils.isEqualOrLessThan(amount, BigInteger.ZERO)) {
            chain.getLogger().error("The withdrawal amount cannot be less than0, amount:{}", amount);
            throw new NulsException(ConverterErrorCode.PARAMETER_ERROR);
        }
        NonceBalance withdrawalNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                withdrawalAssetChainId,
                withdrawalAssetId,
                address);

        BigInteger withdrawalAssetBalance = withdrawalNonceBalance.getAvailable();

        if (BigIntegerUtils.isLessThan(withdrawalAssetBalance, amount)) {
            chain.getLogger().error("Insufficient balance of withdrawn assets chainId:{}, assetId:{}, withdrawal amount:{}, available balance:{} ",
                    withdrawalAssetChainId, withdrawalAssetId, amount, withdrawalAssetBalance);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }

        if(withdrawalAssetChainId == chain.getConfig().getChainId() && chain.getConfig().getAssetId() == withdrawalAssetId) {
            // Heterogeneous transfer of main assets within the chain, Merge directly into onecoinFrom
            // Total handling fee = In chain packaging fees + Heterogeneous chain transfer(Or signature)Handling fees[All settle with the main assets in the chain]
            BigInteger totalFee = TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES.add(DISTRIBUTION_FEE_10);
            amount = totalFee.add(amount);
            if (BigIntegerUtils.isLessThan(withdrawalAssetBalance, amount)) {
                chain.getLogger().error("Insufficient balance of withdrawal fee. amount to be paid:{}, available balance:{} ", amount, withdrawalAssetBalance);
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
            }
        }

        return new CoinFrom(
                AddressTool.getAddress(address),
                withdrawalAssetChainId,
                withdrawalAssetId,
                amount,
                withdrawalNonceBalance.getNonce(),
                (byte) 0);
    }

    private CoinFrom getWithdrawalFeeCoinFrom(Chain chain, String address, BigInteger withdrawalSignFeeNvt) throws NulsException {
        int chainId = chain.getConfig().getChainId();
        int assetId = chain.getConfig().getAssetId();
        NonceBalance currentChainNonceBalance = LedgerCall.getBalanceNonce(
                chain,
                chainId,
                assetId,
                address);
        // Balance of assets in this chain
        BigInteger balance = currentChainNonceBalance.getAvailable();

        // Total handling fee = In chain packaging fees + Heterogeneous chain transfer(Or signature)Handling fees[All settle with the main assets in the chain]
        BigInteger totalFee = TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES.add(withdrawalSignFeeNvt);
        if (BigIntegerUtils.isLessThan(balance, totalFee)) {
            chain.getLogger().error("Insufficient balance of withdrawal fee. amount to be paid:{}, available balance:{} ", totalFee, balance);
            throw new NulsException(ConverterErrorCode.INSUFFICIENT_BALANCE);
        }
        // Query ledger to obtainnoncevalue
        byte[] nonce = currentChainNonceBalance.getNonce();

        return new CoinFrom(AddressTool.getAddress(address), chainId, assetId, totalFee, nonce, (byte) 0);
    }
}
