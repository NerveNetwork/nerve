package io.nuls.dex.test;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
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

public class DexTxTest {

    static int chainId = 2;
    static int assetId = 1;
    static String password = "nuls123456";//"nuls123456";

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
    static String address30 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address31 = "tNULSeBaMqT4R7jY9UoFjETex3B5eJu16JTuo2";

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
    }

    @Test
    public void importKeys() {
//        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe
//        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn

        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG
        importPriKey("659a7a6193b5641edf02501557ee22bb1e53468e46dc60c8a56357a4494a74bf", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//30 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
    }

    public void importPriKey(String priKey, String pwd) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
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

    /**
     * 创建交易对
     */
    @Test
    public void sendCreateCoinTradingTx() {
        try {
            Map params = new HashMap();
            params.put("address", address20);
            params.put("password", password);
            params.put("quoteAssetChainId", chainId);
            params.put("quoteAssetId", 2);
            params.put("scaleQuoteDecimal", 4);
            params.put("baseAssetChainId", chainId);
            params.put("baseAssetId", 1);
            params.put("scaleBaseDecimal", 4);
            params.put("minTradingAmount", new BigInteger("100000"));

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createCoinTradingTx", params);

            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createCoinTradingTx");
            String txHash = (String) callResult.get("txHash");
            Log.info("---tradingHash: " + txHash);
            System.out.println("------划重点，记得拷贝hash到下一个测试里----");
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void sendCreateTradingBuyOrderTx() {
        try {
//            for (int i = 1; i < 3; i++) {
            Map params = new HashMap();
            params.put("address", address31);
            params.put("password", password);
            params.put("type", 1);
            params.put("tradingHash", "60c5ace3f8b601231bb23560a7f6f018982ec702286c9437e5a97dcd311375b7");
            params.put("amount", 10000000000L);
            params.put("price", 6600000000L);

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createTradingOrderTx", params);
            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createTradingOrderTx");
            String txHash = (String) callResult.get("txHash");
            System.out.println("---orderHash: " + txHash);
//            }
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendCreateTradingSellOrderTx() {
        try {
//            for (int i = 2; i > 0; i--) {
            Map params = new HashMap();
            params.put("address", address21);
            params.put("password", password);
            params.put("type", 2);
            params.put("tradingHash", "60c5ace3f8b601231bb23560a7f6f018982ec702286c9437e5a97dcd311375b7");
            params.put("amount", 10000000000L);
            params.put("price", 6600000000L);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createTradingOrderTx", params);
            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createTradingOrderTx");
            String txHash = (String) callResult.get("txHash");
            System.out.println("---orderHash: " + txHash);
//            }
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void sendCreateTradingOrderCancelTx() {
        try {
            Map params = new HashMap();
            params.put("address", address20);
            params.put("password", password);
            params.put("orderHash", "c756c32a0f99bda8322060a077e228a2029a1ea737f09914ba70f5f1799fa7ec");

            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_createTradingCancelOrderTx", params);
            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_createTradingCancelOrderTx");
            String txHash = (String) callResult.get("txHash");
            System.out.println("---orderHash: " + txHash);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void sendEditTradingTx() {
        try {
            Map params = new HashMap();
            params.put("address", address20);
            params.put("password", password);
            params.put("tradingHash", "2236070317fef314003d4f68c091d1bbaaf64a588c45cd10b7795a485a8750fd");
            params.put("scaleQuoteDecimal", 6);
            params.put("scaleBaseDecimal", 6);
            params.put("minTradingAmount", 100);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.DX.abbr, "dx_editCoinTradingTx", params);
            HashMap callResult = (HashMap) ((HashMap) response.getResponseData()).get("dx_editCoinTradingTx");
            String txHash = (String) callResult.get("txHash");
            System.out.println("---Hash: " + txHash);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
