package io.nuls.dex.test;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.TradingOrder;
import io.nuls.dex.util.DexUtil;
import io.nuls.v2.NulsSDKBootStrap;
import io.nuls.v2.util.HttpClientUtil;
import io.nuls.v2.util.NulsSDKTool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DexTxOfflineTest {
    //地址1
    static String address1 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String priKey1 = "9ce21dad67e0f0af2599b41b515a7f7018059418bab892a7b68f283d489abc4b";
    //地址2
    static String address2 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String priKey2 = "477059f40708313626cccd26f276646e4466032cabceccbf571a7c46f954eb75";


//    static String address3 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
//    static String priKey3 = "ddddb7cb859a467fbe05d5034735de9e62ad06db6557b64d7c139b6db856b200";
//
//    static String address4 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
//    static String priKey4 = "4efb6c23991f56626bc77cdb341d64e891e0412b03cbcb948aba6d4defb4e60a";
//
//    static String address5 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
//    static String priKey5 = "3dadac00b523736f38f8c57deb81aa7ec612b68448995856038bd26addd80ec1";
//
//    static String address6 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
//    static String priKey6 = "27dbdcd1f2d6166001e5a722afbbb86a845ef590433ab4fcd13b9a433af6e66e";

    static int defaultChainId = 2;
    static int defaultAssetId = 1;
    static List<BigInteger> amounts = new ArrayList<>();
    static List<BigInteger> prices = new ArrayList<>();

    static List<CoinTradingPo> tradingPoList = new ArrayList<>();

    static String url = "http://127.0.0.1:8081/coin/trading/get/";


    public void before() {
        NulsSDKBootStrap.initTest("http://127.0.0.1:18004");
//        CoinTradingPo tradingPo = new CoinTradingPo();
//        tradingPo.setHash(NulsHash.fromHex("ef95a75490b31a977b7195024e8309e8f652b27f81b09d274d1f06c28d2e4dd8"));
//        tradingPo.setQuoteAssetChainId(2);
//        tradingPo.setQuoteAssetId(3);
//        tradingPo.setScaleQuoteDecimal((byte) 4);
//        tradingPo.setQuoteDecimal((byte) 8);
//
//        tradingPo.setBaseAssetChainId(2);
//        tradingPo.setBaseAssetId(2);
//        tradingPo.setScaleBaseDecimal((byte) 4);
//        tradingPo.setBaseDecimal((byte) 8);
//
//        tradingPo.setMinTradingAmount(BigInteger.valueOf(100000));
//        tradingPoList.add(tradingPo);

        CoinTradingPo tradingPo2 = new CoinTradingPo();
        tradingPo2.setHash(NulsHash.fromHex("2ff0565619963c63bc5395826bd95db75e0a884d24a550f47a92aa7c0e599770"));
        tradingPo2.setQuoteAssetChainId(2);
        tradingPo2.setQuoteAssetId(2);
        tradingPo2.setScaleQuoteDecimal((byte) 4);
        tradingPo2.setQuoteDecimal((byte) 8);

        tradingPo2.setBaseAssetChainId(2);
        tradingPo2.setBaseAssetId(1);
        tradingPo2.setScaleBaseDecimal((byte) 4);
        tradingPo2.setBaseDecimal((byte) 8);

        tradingPo2.setMinTradingAmount(BigInteger.valueOf(10000000));
        tradingPoList.add(tradingPo2);

        for (int i = 1; i <= 10; i++) {
            amounts.add(BigInteger.valueOf(i * 12340000));
        }
        for (int i = 1; i <= 10; i++) {
            prices.add(BigInteger.valueOf(90000000).add(BigInteger.valueOf(i * 2345000)));
        }
    }

    public static void main(String[] args) {
        final DexTxOfflineTest txOfflineTest = new DexTxOfflineTest();
        txOfflineTest.before();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        CoinTradingPo tradingPo = txOfflineTest.getTrading();
                        BigInteger price = getPrice(tradingPo);
//                        txOfflineTest.testSellOrderTx(address2, priKey2, tradingPo, price);
                        txOfflineTest.testBuyOrderTx(address1, priKey1, tradingPo, price);

//                        txOfflineTest.testSellOrderTx(address3, priKey3, tradingPo, price);
//                        txOfflineTest.testBuyOrderTx(address4, priKey4, tradingPo, price);
//
//                        txOfflineTest.testSellOrderTx(address5, priKey5, tradingPo, price);
//                        txOfflineTest.testBuyOrderTx(address6, priKey6, tradingPo, price);
                        System.out.println("- - - - - - - - - - - ");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void testBuyOrderTx(String address, String priKey, CoinTradingPo tradingPo, BigInteger price) throws Exception {
        int type = 1;
        BigInteger amount = getAmount();
        NulsHash nulsHash = NulsHash.fromHex(tradingPo.getHash().toHex());

        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_ORDER);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        CoinData coinData = createBuyOrderTxCoinData(tradingPo, address, amount, price);
        tx.setCoinData(coinData.serialize());
        TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price);
        tx.setTxData(order.serialize());
        String txStr = RPCUtil.encode(tx.serialize());
        Result<Map> result = NulsSDKTool.sign(txStr, address, priKey);
        txStr = (String) result.getData().get("txHex");
        result = NulsSDKTool.broadcast(txStr);
        if (result.isSuccess()) {
            Log.debug("buy hash:" + result.getData().get("hash"));
        }
    }

    public void testSellOrderTx(String address, String priKey, CoinTradingPo tradingPo, BigInteger price) throws Exception {
        int type = 2;
        BigInteger amount = getAmount();
        NulsHash nulsHash = NulsHash.fromHex(tradingPo.getHash().toHex());

        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_ORDER);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        CoinData coinData = createSellOrderTxCoinData(tradingPo, address, amount);
        tx.setCoinData(coinData.serialize());
        TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price);
        tx.setTxData(order.serialize());
        String txStr = RPCUtil.encode(tx.serialize());
        Result<Map> result = NulsSDKTool.sign(txStr, address, priKey);
        txStr = (String) result.getData().get("txHex");
        result = NulsSDKTool.broadcast(txStr);
        if (result.isSuccess()) {
            Log.debug("sell hash:" + result.getData().get("hash"));
        }
    }

    private CoinData createBuyOrderTxCoinData(CoinTradingPo tradingPo, String address, BigInteger baseAmount, BigInteger priceBigInteger) throws NulsException {
        CoinData coinData = new CoinData();

        //首先通过交易币数量和单价，计算出需要的计价货币总量

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());
        BigDecimal amount = new BigDecimal(baseAmount).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        //如果挂单是本链资产，直接将手续费加在一起

        if (tradingPo.getQuoteAssetChainId() == defaultChainId && tradingPo.getQuoteAssetId() == defaultAssetId) {
            Result result = NulsSDKTool.getAccountBalance(address, defaultChainId, defaultAssetId);
            Map balanceMap = (Map) result.getData();
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            BigInteger fromAmount = quoteAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            if (available.compareTo(fromAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from = new CoinFrom();
            from.setAssetsChainId(defaultChainId);
            from.setAssetsId(defaultAssetId);
            from.setAddress(AddressTool.getAddress(address));
            from.setAmount(fromAmount);
            from.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from);
        } else {
            //如果挂单是外链资产，则需要添加一条本链资产的手续费from
            Result result = NulsSDKTool.getAccountBalance(address, tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId());
            Map balanceMap = (Map) result.getData();
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(quoteAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from1 = new CoinFrom();
            from1.setAssetsChainId(tradingPo.getQuoteAssetChainId());
            from1.setAssetsId(tradingPo.getQuoteAssetId());
            from1.setAddress(AddressTool.getAddress(address));
            from1.setAmount(quoteAmount);
            from1.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from1);


            result = NulsSDKTool.getAccountBalance(address, defaultChainId, defaultAssetId);
            balanceMap = (Map) result.getData();
            nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
            }
            CoinFrom from2 = new CoinFrom();
            from2.setAssetsChainId(defaultChainId);
            from2.setAssetsId(defaultAssetId);
            from2.setAddress(AddressTool.getAddress(address));
            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from2.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from2);
        }

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        to.setAssetsId(tradingPo.getQuoteAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(quoteAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;
    }

    private CoinData createSellOrderTxCoinData(CoinTradingPo tradingPo, String address, BigInteger baseAmount) throws NulsException {
        CoinData coinData = new CoinData();

        //如果挂单是本链资产，直接将手续费加在一起
        if (tradingPo.getBaseAssetChainId() == defaultChainId && tradingPo.getBaseAssetId() == defaultAssetId) {
            Result result = NulsSDKTool.getAccountBalance(address, defaultChainId, defaultAssetId);
            Map balanceMap = (Map) result.getData();

            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            BigInteger fromAmount = baseAmount.add(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            if (available.compareTo(fromAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from = new CoinFrom();
            from.setAssetsChainId(defaultChainId);
            from.setAssetsId(defaultAssetId);
            from.setAddress(AddressTool.getAddress(address));
            from.setAmount(fromAmount);
            from.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from);
        } else {
            //如果挂单是外链资产，则需要添加一条本链资产的手续费from
            Result result = NulsSDKTool.getAccountBalance(address, tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId());
            Map balanceMap = (Map) result.getData();
            BigInteger available = new BigInteger(balanceMap.get("available").toString());
            String nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(baseAmount) < 0) {
                throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
            }
            CoinFrom from1 = new CoinFrom();
            from1.setAssetsChainId(tradingPo.getBaseAssetChainId());
            from1.setAssetsId(tradingPo.getBaseAssetId());
            from1.setAddress(AddressTool.getAddress(address));
            from1.setAmount(baseAmount);
            from1.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from1);

            result = NulsSDKTool.getAccountBalance(address, defaultChainId, defaultAssetId);
            balanceMap = (Map) result.getData();
            available = new BigInteger(balanceMap.get("available").toString());
            nonce = (String) balanceMap.get("nonce");
            if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
                throw new NulsException(DexErrorCode.INSUFFICIENT_FEE);
            }
            CoinFrom from2 = new CoinFrom();
            from2.setAssetsChainId(defaultChainId);
            from2.setAssetsId(defaultAssetId);
            from2.setAddress(AddressTool.getAddress(address));
            from2.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
            from2.setNonce(HexUtil.decode(nonce));
            coinData.addFrom(from2);
        }

        CoinTo to = new CoinTo();
        to.setAssetsChainId(tradingPo.getBaseAssetChainId());
        to.setAssetsId(tradingPo.getBaseAssetId());
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(baseAmount);
        to.setLockTime(DexConstant.DEX_LOCK_TIME);
        coinData.addTo(to);

        return coinData;
    }


    static Random random = new Random();

    public BigInteger getAmount() {
//        int i = random.nextInt(amounts.size());
//        return amounts.get(i);
//        int r = random.nextInt(10);
//        r++;
        return BigInteger.valueOf(100000000000L);
    }

    public static BigInteger getPrice(CoinTradingPo tradingPo) {
//        try {
//            String result = HttpClientUtil.get(url + tradingPo.getHash().toHex());
//            Map<String, Object> resultMap = JSONUtils.jsonToMap(result);
//            Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
//            resultMap = (Map<String, Object>) data.get("result");
//            BigInteger price = new BigInteger(resultMap.get("newPrice").toString());
//
//            int r = random.nextInt(10);
//            if (r >= 5) {
//                return price.subtract(BigInteger.valueOf(10000000 * r)).subtract(price.divide(BigInteger.valueOf(200))).abs();
//            } else {
//                return price.add(BigInteger.valueOf(10000000 * r)).add(price.divide(BigInteger.valueOf(200)));
//            }
////            return BigInteger.valueOf(13994500);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }


        return BigInteger.valueOf(100000000L);
    }

    private CoinTradingPo getTrading() {
        int i = random.nextInt(tradingPoList.size());
        return tradingPoList.get(i);
    }
}
