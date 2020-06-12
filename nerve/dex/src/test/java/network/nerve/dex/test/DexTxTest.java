package network.nerve.dex.test;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.dex.model.txData.TradingOrderCancel;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class DexTxTest {

    static int chainId = 4;
    static int assetId = 1;
    static String password = "nuls123456";//"nuls123456";

    static String address20 = "TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA";
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
//        importPriKey("d3413fcc17913623256b6451698ca1d50629a3c8a760ad86d03b6fcda2d3a1af", password); //TNVTdN9i7xo3PmLj376B17Qntng3DyVio4Bqd


//        importPriKey("b54db432bba7e13a6c4a28f65b925b18e63bcb79143f7b894fa735d5d3d09db5", password);//种子出块地址 tNULSeBaMkrt4z9FYEkkR9D6choPVvQr94oYZp   TNVTdN9i97WuLcebKwEYrdiZJJ4NWAvC7B77i
//        importPriKey("188b255c5a6d58d1eed6f57272a22420447c3d922d5765ebb547bc6624787d9f", password);//种子出块地址 tNULSeBaMoGr2RkLZPfJeS5dFzZeNj1oXmaYNe   TNVTdN9iBXUrnDjcUqnn9WFCb4KFJmsaox6vY
//        importPriKey("76b7beaa98db863fb680def099af872978209ed9422b7acab8ab57ad95ab218b", password);//29 tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn            TNVTdN9iEEaQ68quQYtSu6XMUzd2rwUBtYb7k

        importPriKey("9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b", password);//20 tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG  TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA
        importPriKey("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75", password);//21 tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD  TNVTdN9iB7VveoFG4GwYMRAFAF2Rsyrj9mjR3
//        importPriKey("7d7347f49eb41fadae814415e777666a5adac2eaa3b41eb6c58fb6c705098d1a", password);// TNVTdN9i3RVt2u8ueS2u7y8aTUt7GzB2SC3HX
//        importPriKey("01e32f257c851a3553ca627c229eedfebef5ff4f359195978f56896b01ab3069", password);
//        importPriKey("8212e7ba23c8b52790c45b0514490356cd819db15d364cbe08659b5888339e78", password);//22 tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24
//        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//23 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
//        importPriKey("bec819ef7d5beeb1593790254583e077e00f481982bce1a43ea2830a2dc4fdf7", password);//24 tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL
//        importPriKey("ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200", password);//25 tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL
//        importPriKey("4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a", password);//26 tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm
//        importPriKey("3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1", password);//27 tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1
//        importPriKey("27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e", password);//28 tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2
//        importPriKey("4100e2f88c3dba08e5000ed3e8da1ae4f1e0041b856c09d35a26fb399550f530", password);//30 tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD
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
            params.put("address", "TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA");
            params.put("password", password);
            params.put("quoteAssetChainId", 4);
            params.put("quoteAssetId", 3);
            params.put("scaleQuoteDecimal", 6);
            params.put("baseAssetChainId", 4);
            params.put("baseAssetId", 4);
            params.put("scaleBaseDecimal", 8);
            params.put("minBaseAmount", new BigInteger("1000"));
            params.put("minQuoteAmount", new BigInteger("1000"));

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
            params.put("tradingHash", "f21a9ef245f9243e579cdd2269feb5425283b9ac507a54967914aaa749978ac6");
            params.put("scaleQuoteDecimal", 6);
            params.put("scaleBaseDecimal", 6);
            params.put("minTradingAmount", 100000);
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

    @Test
    public void testS() throws NulsException {
        Transaction tx = new Transaction();
        byte[] bytes = HexUtil.decode("e600781ac65e00201df712f49a813654b78a06840d26714be9aa6400330a4e532fea72f0845fd96d8c01170400015884fa407da3005067ce4bd6d29a8e4a2af7846104000100a086010000000000000000000000000000000000000000000000000000000000086397016b249054270001170400015884fa407da3005067ce4bd6d29a8e4a2af78461040001000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        tx.parse(new NulsByteBuffer(bytes));

        TradingOrderCancel orderCancel = new TradingOrderCancel();
        orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
        System.out.println(HexUtil.encode(orderCancel.getOrderHash()) );
    }


        public static void main(String[] args) throws NulsException {
//            String txHex = "e5007a8bd45e007a07174ccb22d8b990d51a2a9d3e54c43c36afe1c189c47b8c87434b199939e21104000198980dd5d44f66d249c1e16bdf96e392d88309390100e1f505000000000000000000000000000000000000000000000000000000000050d6dc010000000000000000000000000000000000000000000000000000000000d2021704000198980dd5d44f66d249c1e16bdf96e392d8830939040002000050d6dc01000000000000000000000000000000000000000000000000000000082eb33b6db9beaee7001704000198980dd5d44f66d249c1e16bdf96e392d883093904000100a086010000000000000000000000000000000000000000000000000000000000082eb33b6db9beaee700011704000198980dd5d44f66d249c1e16bdf96e392d8830939040002000050d6dc01000000000000000000000000000000000000000000000000000000feffffffffffffff6a2103af80fe5894a4bc170abdad90bbaad38840576136b3fc498a291bb92335c4ad0e473045022100f9166111735ba879bd3072249d66c5505bdfb6d789671444202a3474c6afba3f0220649320415ad42d7261ca21e123b429595c0c711d0a7fefa5bebfa433d1743020";
//            Transaction transaction = new Transaction();
//            transaction.parse(new NulsByteBuffer(HexUtil.decode(txHex)));
//            byte[] bytes = transaction.getTransactionSignature();
//            System.out.println(HexUtil.encode(bytes));tradingOrder
//            System.out.println(transaction.getCoinDataInstance());
//            TradingOrder tradingOrder = new TradingOrder();
//            tradingOrder.parse(new NulsByteBuffer(transaction.getTxData()));
//            System.out.println(tradingOrder);
////            SignatureUtil.validateCtxSignture(transaction);
//            TransactionSignature signature = new TransactionSignature();
//            signature.parse(new NulsByteBuffer(bytes));
//            Address address = new Address(4, "TNVT", BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(signature.getP2PHKSignatures().get(0).getPublicKey()));
//            System.out.println("=".repeat(100));
//            System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));
            int count = 1;
            String addressPrefix = "NERVE";
            int chainId = 5;
            for (var i= 0;i<count;i++){
                    ECKey key = ECKey.fromPrivate(HexUtil.decode("477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75"));
                    Address address = new Address(chainId, addressPrefix, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
                    System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));
                    System.out.println("publicKey :" + key.getPublicKeyAsHex());
                    System.out.println("privateKey:" + key.getPrivateKeyAsHex());
                    System.out.println("=".repeat(100));
            }

        }

}
