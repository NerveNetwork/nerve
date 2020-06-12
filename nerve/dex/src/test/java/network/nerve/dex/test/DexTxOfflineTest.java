package network.nerve.dex.test;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.txData.TradingOrder;
import network.nerve.dex.model.txData.TradingOrderCancel;
import network.nerve.dex.util.DexUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

public class DexTxOfflineTest {
    //地址1
    static String address1 = "TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA";
    //地址2
    static String address2 = "TNVTdN9iB7VveoFG4GwYMRAFAF2Rsyrj9mjR3";

    static String address3 = "TNVTdN9i3RVt2u8ueS2u7y8aTUt7GzB2SC3HX";

    static String password = "nuls123456";


//    static String address3 = "TNVTdN9i3RVt2u8ueS2u7y8aTUt7GzB2SC3HX";
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

    static int defaultChainId = 4;
    static int defaultAssetId = 1;
    static List<BigInteger> amounts = new ArrayList<>();
    static List<BigInteger> prices = new ArrayList<>();

    static List<CoinTradingPo> tradingPoList = new ArrayList<>();

    static String url = "http://127.0.0.1:8081/coin/trading/get/";


    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");


        CoinTradingPo tradingPo2 = new CoinTradingPo();
        tradingPo2.setHash(NulsHash.fromHex("b20d3bb17538698ac819907c7d09b8c58ea4eddcad482f41fa64ce42e6d8ed6a"));
        tradingPo2.setQuoteAssetChainId(4);
        tradingPo2.setQuoteAssetId(2);
        tradingPo2.setScaleQuoteDecimal((byte) 4);
        tradingPo2.setQuoteDecimal((byte) 8);

        tradingPo2.setBaseAssetChainId(4);
        tradingPo2.setBaseAssetId(1);
        tradingPo2.setScaleBaseDecimal((byte) 4);
        tradingPo2.setBaseDecimal((byte) 8);
        tradingPo2.setMinBaseAmount(BigInteger.valueOf(10000000));
        tradingPo2.setMinQuoteAmount(BigInteger.valueOf(10000000));
        tradingPoList.add(tradingPo2);

        for (int i = 1; i <= 10; i++) {
            amounts.add(BigInteger.valueOf(i * 12340000));
        }
        for (int i = 1; i <= 10; i++) {
            prices.add(BigInteger.valueOf(90000000).add(BigInteger.valueOf(i * 2345000)));
        }
    }

    public static void main(String[] args) throws Exception {
        final DexTxOfflineTest txOfflineTest = new DexTxOfflineTest();
        txOfflineTest.before();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        CoinTradingPo tradingPo = txOfflineTest.getTrading();
//                        BigInteger price = getPrice(tradingPo);
//                        BigInteger amount = getAmount();

                        BigInteger price = new BigInteger("300000000");
//                        BigInteger buyAmount = new BigInteger("1000000000");
                        BigInteger sellAmount = new BigInteger("200000000");
//                        txOfflineTest.testBuyOrderTx(address1, tradingPo, price, buyAmount);
                        txOfflineTest.testSellOrderTx(address1, tradingPo, price, sellAmount);

//                        price = new BigInteger("200000000");
//                        txOfflineTest.testBuyOrderTx(address1, tradingPo, price, buyAmount);
//                        txOfflineTest.testSellOrderTx(address1, tradingPo, price, sellAmount);
//                        txOfflineTest.testBuyOrderTx(address2, tradingPo, price, amount);
//                        txOfflineTest.testBuyOrderTx(address1, tradingPo, price, amount);

//
                        txOfflineTest.cancelOrder(address1, "84982df56691d3cb62b071c177c7dab94191e9502725bc0f4bdea50292471247");
                        txOfflineTest.cancelOrder(address1, "e0fe0cb3fc88992781ceba51d69a444fc2bd3d27790f3087cee8617e4f1a2004");


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
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public String testBuyOrderTx(String address, CoinTradingPo tradingPo, BigInteger price, BigInteger amount) throws Exception {
        int type = 1;
        NulsHash nulsHash = NulsHash.fromHex(tradingPo.getHash().toHex());

        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_ORDER);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        CoinData coinData = createBuyOrderTxCoinData(tradingPo, address, amount, price);
        tx.setCoinData(coinData.serialize());
        TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price, address);
        tx.setTxData(order.serialize());
        Map result = sign(address, password, tx.getHash().toHex());
        String signature = (String) result.get("signature");
        tx.setTransactionSignature(RPCUtil.decode(signature));
        String txStr = RPCUtil.encode(tx.serialize());
        result = broadcast(txStr);
        Log.debug("buy hash:" + result.get("hash"));
        return result.get("hash").toString();
    }

    public String testSellOrderTx(String address, CoinTradingPo tradingPo, BigInteger price, BigInteger amount) throws Exception {
        int type = 2;
        NulsHash nulsHash = NulsHash.fromHex(tradingPo.getHash().toHex());

        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_ORDER);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
        CoinData coinData = createSellOrderTxCoinData(tradingPo, address, amount);
        tx.setCoinData(coinData.serialize());
        TradingOrder order = DexUtil.createTradingOrder(nulsHash, type, amount, price, address);
        tx.setTxData(order.serialize());
        Map result = sign(address, password, tx.getHash().toHex());
        String signature = (String) result.get("signature");
        tx.setTransactionSignature(RPCUtil.decode(signature));
        String txStr = RPCUtil.encode(tx.serialize());
        result = broadcast(txStr);
        Log.debug("sell hash:" + result.get("hash"));
        return result.get("hash").toString();
    }

    private CoinData createBuyOrderTxCoinData(CoinTradingPo tradingPo, String address, BigInteger baseAmount, BigInteger priceBigInteger) throws NulsException {
        CoinData coinData = new CoinData();

        //首先通过交易币数量和单价，计算出需要的计价货币总量

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());
        BigDecimal amount = new BigDecimal(baseAmount).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.UP);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        //如果挂单是本链资产，直接将手续费加在一起

        if (tradingPo.getQuoteAssetChainId() == defaultChainId && tradingPo.getQuoteAssetId() == defaultAssetId) {
            getAccountBalance(address, defaultChainId, defaultAssetId);
            Map balanceMap = getAccountBalance(address, defaultChainId, defaultAssetId);
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
            Map balanceMap = getAccountBalance(address, tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId());
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

            balanceMap = getAccountBalance(address, defaultChainId, defaultAssetId);
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
            Map balanceMap = getAccountBalance(address, defaultChainId, defaultAssetId);

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
            Map balanceMap = getAccountBalance(address, tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId());
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

            balanceMap = getAccountBalance(address, defaultChainId, defaultAssetId);
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


    public void cancelOrder(String address, String orderHash) throws Exception {

        NulsHash nulsHash = NulsHash.fromHex(orderHash);

        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_ORDER_CANCEL);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());

        CoinData coinData = createOrderCancelCoinData(address);
        tx.setCoinData(coinData.serialize());

        TradingOrderCancel order = new TradingOrderCancel();
        order.setOrderHash(nulsHash.getBytes());
        tx.setTxData(order.serialize());
        Map result = sign(address, password, tx.getHash().toHex());
        String signature = (String) result.get("signature");
        tx.setTransactionSignature(RPCUtil.decode(signature));
        String txStr = RPCUtil.encode(tx.serialize());
        result = broadcast(txStr);
        Log.debug("cancel hash:" + result.get("hash"));

    }

    private CoinData createOrderCancelCoinData(String address) throws NulsException {
        CoinData coinData = new CoinData();
        Map balanceMap = getAccountBalance(address, defaultChainId, defaultAssetId);
        BigInteger available = new BigInteger(balanceMap.get("available").toString());
        String nonce = (String) balanceMap.get("nonce");
        if (available.compareTo(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES) < 0) {
            throw new NulsException(DexErrorCode.BALANCE_NOT_ENOUGH);
        }
        CoinFrom from = new CoinFrom();
        from.setAssetsChainId(defaultChainId);
        from.setAssetsId(defaultAssetId);
        from.setAddress(AddressTool.getAddress(address));
        from.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
        from.setNonce(HexUtil.decode(nonce));
        coinData.addFrom(from);

        CoinTo to = new CoinTo();
        to.setAssetsChainId(defaultChainId);
        to.setAssetsId(defaultAssetId);
        to.setAddress(AddressTool.getAddress(address));
        to.setAmount(BigInteger.ZERO);
        to.setLockTime(0);
        coinData.addTo(to);

        return coinData;
    }


    public Map<String, Object> getAccountBalance(String address, int assetChainId, int assetId) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, defaultChainId);
            params.put("assetChainId", assetChainId);
            params.put("assetId", assetId);
            params.put("address", address);

            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getBalanceNonce", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("getBalanceNonce");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> sign(String address, String password, String txHex) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, defaultChainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", txHex);

            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_signDigest");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public Map<String, Object> broadcast(String txHex) {
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, defaultChainId);
            params.put("tx", txHex);

            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("tx_newTx");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Random random = new Random();

    public static BigInteger getAmount() {
//        int i = random.nextInt(amounts.size());
//        return amounts.get(i);
//        int r = random.nextInt(10);
//        r++;
//        return BigInteger.valueOf(2000000000L);
        return BigInteger.valueOf(1000000000L);
    }

    static int i = 0;
    static int s = 1;

    public static BigInteger getPrice(CoinTradingPo tradingPo) {
        try {
            return BigInteger.valueOf(10000000000L);
//            String result = HttpClientUtil.get(url + tradingPo.getHash().toHex());
//            Map<String, Object> resultMap = JSONUtils.jsonToMap(result);
//            Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
//            resultMap = (Map<String, Object>) data.get("result");
//            BigInteger price = new BigInteger(resultMap.get("newPrice").toString());
//            if (price == null || price.compareTo(BigInteger.ZERO) <= 0) {
//                return BigInteger.valueOf(100000000L);
//            }
//
//            while (i == 0) {
//                i = random.nextInt(10);
//            }
//            i = i - 1;
//            int r = random.nextInt(10);
//            BigInteger calc = price.divide(BigInteger.valueOf(120)).multiply(BigInteger.valueOf(r));
//
//            int b = i % 200;
//            if (b < 100) {
//                if (b % 7 != 0) {
//                    return price.add(calc);
//                } else {
//                    return price.subtract(calc);
//                }
//            } else {
//                if (b % 7 != 0) {
//                    return price.subtract(calc);
//                } else {
//                    return price.add(calc);
//                }
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        i++;
//        i = 1;
//        return BigInteger.valueOf(1234000);
    }

    private CoinTradingPo getTrading() {
        int i = random.nextInt(tradingPoList.size());
        return tradingPoList.get(i);
    }


}
