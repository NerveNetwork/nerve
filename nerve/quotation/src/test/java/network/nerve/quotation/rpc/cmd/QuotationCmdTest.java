package network.nerve.quotation.rpc.cmd;

import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.ConfigBean;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.util.CommonUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class QuotationCmdTest {

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

    private Chain chain;
    static int chainId = 2;
    static int assetChainId = 2;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";

    @Test
    public void importPriKeyTest() {
        //公钥: 037fae74d15153c3b55857ca0abd5c34c865dfa1c0d0232997c545bae5541a0863
        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp
        //公钥: 036c0c9ae792f043e14d6a3160fa37e9ce8ee3891c34f18559e20d9cb45a877c4b
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe
        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD
        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn
    }

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
        chain = new Chain();
        chain.setConfigBean(new ConfigBean(chainId, assetId));
    }

    @Test
    public void getTxs() throws Exception {
        String txStr = (String) (getTxCfmClient("8cbc547447c195259741118bd61f0e0a552f1f791d5a362cf46f403f2767ac08").get("tx"));
        Transaction tx = CommonUtil.getInstance(txStr, Transaction.class);//最后一条
        System.out.println(tx.format(Quotation.class));
    }

    @Test
    public void getQuotation() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("key", "NERVE_PRICE");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.QU.abbr, "qu_final_quotation", params, 1000000L);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("qu_final_quotation");
        System.out.println("qu_quote result:" + JSONUtils.obj2json(result));
    }

    @Test
    public void testFinal() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("token", "NULS");
        params.put("price", "0.6666");
        params.put("date", "20191215");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.QU.abbr, "test_final", params, 1000000L);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("test_final");
        System.out.println("qu_quote result:" + JSONUtils.obj2json(result));

        Map<String, Object> params2 = new HashMap<>();
        params2.put(Constants.VERSION_KEY_STR, "1.0");
        params2.put(Constants.CHAIN_ID, chainId);
        params2.put("token", "BTC");
        params2.put("price", "7060.6666");
        params2.put("date", "20191215");
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.QU.abbr, "test_final", params2, 1000000L);
        HashMap result2 = (HashMap) ((HashMap) cmdResp2.getResponseData()).get("test_final");
        System.out.println("qu_quote result:" + JSONUtils.obj2json(result2));
    }


    @Test
    public void quoteTest() throws Exception {
        Map<String, Double> pricesMap = new HashMap<>();
        pricesMap.putIfAbsent("nuls".toUpperCase(), 2.12);
        pricesMap.putIfAbsent("usdt".toUpperCase(), 7.12);

        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put(Constants.CHAIN_ID, chainId);
        params.put("address", address29);
        params.put("password", password);
        params.put("pricesMap", pricesMap);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.QU.abbr, "qu_quote", params, 1000000L);
        System.out.println("qu_quote result:" + JSONUtils.obj2json(cmdResp));
        assertNotNull(cmdResp);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("qu_quote");
        String txHash = (String) result.get("value");
        Log.debug("hash:{}", txHash);
    }

    /**
     * 查交易
     */
    private Map<String, Object> getTxCfmClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.CHAIN_ID, chainId);
        params.put("txHash", hash);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.debug(JSONUtils.obj2PrettyJson(record));
        return (Map) record.get("tx_getConfirmedTxClient");
    }

    public static void importPriKey(String priKey, String pwd) {
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


    @Test
    public void testGetQoute() {
//        RocksDBService.init("/Users/zhoulijun/workspace/nuls/nerve-network-package/NULS_WALLET/data/account");
//        List<byte[]> list = RocksDBService.entryList("account");
//        list.stream().forEach(d->{
//            try {
//                Log.info("{}",new String(d,"UTF8"));
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        });
    }

    public static void main(String[] args) throws NulsException {
//        String hex = "1e0031eb2d5f006017090001c37c948cea7bc5472dfebcdf4a69603304d0c736014604084e56542d555344544025d7e28d2bc33f084254432d5553445485eb51b832a1c640094e554c532d5553445473def756c1bbdf3f084554482d55534454d7a3703d8aac7740006a2102ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f04730450221009baf056810ed0542a3d867cf4f36d18a60e5c0c0afcd5e869ab5b093a6f1dd4002207955e5bd54d7c8ef8c88755dad5e6b513c70685eeb026f6e0ae5244f111e150c";
//        Transaction tx = new Transaction();
//        tx.parse(HexUtil.decode(hex), 0);
//        Quotation quotation = new Quotation();
//        quotation.parse(tx.getTxData(), 0);
//
//        Log.info("{}", quotation.getPrices().getPrices().size());
//        Log.info("{}", AddressTool.getStringAddressByBytes(quotation.getAddress()));
//        quotation.getPrices().getPrices().forEach(d -> {
//            Log.info("{}:{}", d.getKey(), d.getValue());
//        });

//        String txHex = "17090001f79dd4abaf8dfe2241128c56d7d6c4fedb88381a01fd0e010e084e56542d55534454cf6bec12d55bb33f084254432d55534454b81e856b4d51e84009555344432d5553445489bc61b223fbef3f084f4b422d55534454105839b4c8963240085041582d5553445462156f641ef9ef3f124e5654455448554e4956324c502d555344545e2ee2256418404108424e422d555344546197348887e46f40114e5654424e4243616b654c502d5553445419e6043d06942941094e554c532d55534454d2448e63da350c4009555344542d55534454000000000000f03f0748542d55534454a9a3e36a64b73340124e565448555344484d44584c502d55534454177303ca785ef441084441492d55534454daacfa5c6d25f03f084554482d55534454713d0ad7a3649940";
//        Transaction transaction = new Transaction();
//        transaction.parse(HexUtil.decode(txHex),0);
//        Quotation quotation = new Quotation();
//        quotation.parse(transaction.getTxData(),0);
//        Log.info("{}",quotation);

//
//        // 最终报价txdata
        String txDataStr = "010a4d415449432d55534454dd3b30a1fe6af03f";
//        Prices price = CommonUtil.getInstance(txDataStr, Prices.class);
//        price.getPrices().forEach(d -> {
//            Log.info("{}:{}", d.getKey(), d.getValue());
//        });
        /** 喂价*/
        Prices quotation = CommonUtil.getInstance(txDataStr, Prices.class);
        Log.info("{}", quotation.toString());
    }


}
