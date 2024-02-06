package network.nerve.dex.util;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.CoinTrading;
import network.nerve.dex.model.txData.TradingOrder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * dexModule, general tool class
 */
public class DexUtil {

    public static void createTable(String name) {
        if (!RocksDBService.existTable(name)) {
            try {
                RocksDBService.createTable(name);
            } catch (Exception e) {
                LoggerUtil.dexLog.error(e);
                throw new NulsRuntimeException(DexErrorCode.SYS_UNKOWN_EXCEPTION);
            }
        }
    }

    /**
     * Through currency pairsidInformation generationkey
     *
     * @param chainId1
     * @param assetId1
     * @param chainId2
     * @param assetId2
     * @return
     */
    public static String toCoinTradingKey(int chainId1, int assetId1, int chainId2, int assetId2) {
        return chainId1 + "-" + assetId1 + "-" + chainId2 + "-" + assetId2;
    }

    /**
     * obtainbaseCoin pairs at the beginningid
     *
     * @param po
     * @return
     */
    public static String getCoinTradingKey1(CoinTradingPo po) {
        return po.getBaseAssetChainId() + "-" + po.getBaseAssetId() + "-" + po.getQuoteAssetChainId() + "-" + po.getQuoteAssetId();
    }

    public static String getCoinKey(String address, int assetChainId, int assetId) {
        return address + "-" + assetChainId + "-" + assetId;
    }


    public static String getCoinKey(String address, int assetChainId, int assetId, int locked) {
        return address + "-" + assetChainId + "-" + assetId + "-" + locked;
    }

    public static String getOrderNonceKey(byte[] nonce, byte[] address, int assetChainId, int assetId) {
        return HexUtil.encode(nonce) + "-" + AddressTool.getStringAddressByBytes(address) + "-" + assetChainId + "-" + assetId;
    }

    public static byte[] getNonceByHash(NulsHash hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return out;
    }

    /**
     * Generating transactional transactionscoinData
     *
     * @return
     */
    public static Map<String, Object> createDealTxCoinData(CoinTradingPo tradingPo, BigInteger price,
                                                           TradingOrderPo buyOrder, TradingOrderPo sellOrder) {
        CoinData coinData = new CoinData();
        addCoinFrom(tradingPo, buyOrder, sellOrder, coinData);
        return addCoinTo(tradingPo, price, buyOrder, sellOrder, coinData);
    }

    /**
     * Generating transactional transactionsfrom ,Unlocking the remaining amount of coins locked by the user for order placement
     * fromThe first piece of data is forced to be the unlocking information for buying orders, and the second piece is the unlocking information for selling orders
     *
     * @param tradingPo
     * @param buyOrder
     * @param sellOrder
     */
    public static void addCoinFrom(CoinTradingPo tradingPo, TradingOrderPo buyOrder, TradingOrderPo sellOrder, CoinData coinData) {
        //Add Purchase Orderfrom
        CoinFrom from1 = new CoinFrom();
        from1.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        from1.setAssetsId(tradingPo.getQuoteAssetId());
        from1.setAddress(buyOrder.getAddress());
        from1.setNonce(buyOrder.getNonce());
        from1.setAmount(buyOrder.getLeftQuoteAmount());
        from1.setLocked(DexConstant.ASSET_LOCK_TYPE);
        coinData.addFrom(from1);

        //Add sales orderfrom
        CoinFrom from2 = new CoinFrom();
        from2.setAssetsChainId(tradingPo.getBaseAssetChainId());
        from2.setAssetsId(tradingPo.getBaseAssetId());
        from2.setAddress(sellOrder.getAddress());
        from2.setNonce(sellOrder.getNonce());
        from2.setAmount(sellOrder.getLeftAmount());
        from2.setLocked(DexConstant.ASSET_LOCK_TYPE);
        coinData.addFrom(from2);
    }

    /**
     * Generate transaction volume based on transaction volumeto
     * generatetoThe order is also mandatory, as detailed in the following orderDexService.createDealTx
     *
     * @param tradingPo
     * @param buyOrder
     * @param sellOrder
     */
    private static Map<String, Object> addCoinTo(CoinTradingPo tradingPo, BigInteger priceBigInteger,
                                                 TradingOrderPo buyOrder, TradingOrderPo sellOrder, CoinData coinData) {

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());

        if (buyOrder.getLeftAmount().compareTo(sellOrder.getLeftAmount()) == 0) {
            //If the remaining trading currencies on both sides are exactly equal in quantity
            return processWithEqual(coinData, tradingPo, price, buyOrder, sellOrder);
        } else if (buyOrder.getLeftAmount().compareTo(sellOrder.getLeftAmount()) < 0) {
            //If the remaining transaction currency quantity on the purchase order is less than the remaining quantity on the sell order
            return processBuyLess(coinData, tradingPo, price, buyOrder, sellOrder);
        } else {
            //If the remaining transaction currency quantity on the purchase order is greater than the remaining quantity on the sale order, it needs to be calculated based on the actual remaining quantity on the sale order, and how much base currency can be exchanged
            return processSellLess(coinData, tradingPo, price, buyOrder, sellOrder);
        }
    }

    private static Map<String, Object> processWithEqual(CoinData coinData, CoinTradingPo tradingPo, BigDecimal price,
                                                        TradingOrderPo buyOrder, TradingOrderPo sellOrder) {
        Map<String, Object> map = new HashMap<>();
        //First, use the remaining quantity of the transaction currency in the sales order * unit price Calculate the total amount of pricing currencies that can be exchanged
        BigDecimal amount = new BigDecimal(sellOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //The total amount of pricing currencies that can ultimately be exchanged

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, sellOrder.getLeftAmount(), sellOrder);
        //After the transaction is fully completed, there may be remaining coins that have not been spent on the purchase order, and the remaining coins need to be returned to the user who made the purchase order
        returnBuyLeftAmountCoinTo(coinData, tradingPo, buyOrder, buyOrder.getLeftQuoteAmount().subtract(quoteAmount));

        map.put("isBuyOver", true);
        map.put("isSellOver", true);
        map.put("quoteAmount", quoteAmount);
        map.put("baseAmount", sellOrder.getLeftAmount());
        map.put("coinData", coinData);
        return map;
    }

    private static Map<String, Object> processBuyLess(CoinData coinData, CoinTradingPo tradingPo, BigDecimal price,
                                                      TradingOrderPo buyOrder, TradingOrderPo sellOrder) {
        Map<String, Object> map = new HashMap<>();
        //If the remaining transaction currency quantity on the purchase order is less than the remaining quantity on the sell order, then the quantity to be paid on the sell order is the remaining quantity on the purchase order
        //Calculate how much currency should be paid to the seller for the purchase order based on the final transaction price
        BigDecimal amount = new BigDecimal(buyOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //The total amount of pricing currencies that can ultimately be exchanged

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, buyOrder.getLeftAmount(), sellOrder);
        //After the transaction is fully completed, there may be remaining coins that have not been spent on the purchase order, and the remaining coins need to be returned to the user who made the purchase order
        returnBuyLeftAmountCoinTo(coinData, tradingPo, buyOrder, buyOrder.getLeftQuoteAmount().subtract(quoteAmount));
        //Check if the remaining coins in the sales order support continuing transactions. If yes, continue to lock them. If not, return them to the seller
        boolean isSellOver = addSellLeftAmountCoinTo(coinData, tradingPo, sellOrder, sellOrder.getLeftAmount().subtract(buyOrder.getLeftAmount()));

        map.put("isBuyOver", true);
        map.put("isSellOver", isSellOver);
        map.put("quoteAmount", quoteAmount);
        map.put("baseAmount", buyOrder.getLeftAmount());
        map.put("coinData", coinData);
        return map;
    }

    private static Map<String, Object> processSellLess(CoinData coinData, CoinTradingPo tradingPo, BigDecimal price,
                                                       TradingOrderPo buyOrder, TradingOrderPo sellOrder) {
        Map<String, Object> map = new HashMap<>();
        //If the remaining quantity of the purchase order is greater than the remaining quantity of the sell order, the amount of pricing currency to be paid for the purchase order needs to be calculated based on the remaining transaction currency quantity of the sell order
        BigDecimal amount = new BigDecimal(sellOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //The total amount of pricing currencies that can ultimately be exchanged

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, sellOrder.getLeftAmount(), sellOrder);
        //Check if the remaining coins on the purchase order support continuing transactions. If yes, continue to lock them. If not, return them to the buyer
        boolean isBuyOver = addBuyLeftAmountCoinTo(coinData, tradingPo, buyOrder, buyOrder.getLeftAmount().subtract(sellOrder.getLeftAmount()), buyOrder.getLeftQuoteAmount().subtract(quoteAmount));
        map.put("isBuyOver", isBuyOver);
        map.put("isSellOver", true);
        map.put("quoteAmount", quoteAmount);
        map.put("baseAmount", sellOrder.getLeftAmount());
        map.put("coinData", coinData);
        return map;
    }

    /**
     * Adding transactions that are inevitably generated4striptorecord
     * Purchase transactionto
     * Sales transactionto
     * Payment handling feeto
     * Selling order handling feeto
     */
    private static void addDealCoinTo(Map<String, Object> map, CoinData coinData, CoinTradingPo tradingPo,
                                      BigInteger quoteAmount, TradingOrderPo buyOrder,
                                      BigInteger baseAmount, TradingOrderPo sellOrder) {
        //1.Firstly, calculate the system transaction fees that each buyer and seller need to pay. The default system transaction fee is 2/10000
        //If the calculation shows that the handling fee is less than the minimum number of supported digits of the asset, the default unit for collecting the minimum asset is the handling fee

        //The system transaction fee that the seller needs to pay
        BigDecimal sellSysFee = new BigDecimal(quoteAmount).multiply(DexContext.sysFeeScaleDecimal).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
        if (sellSysFee.compareTo(BigDecimal.ONE) < 0) {
            sellSysFee = BigDecimal.ONE;
        }
        //The system transaction fee that the buyer needs to pay
        BigDecimal buySysFee = new BigDecimal(baseAmount).multiply(DexContext.sysFeeScaleDecimal).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
        if (buySysFee.compareTo(BigDecimal.ONE) < 0) {
            buySysFee = BigDecimal.ONE;
        }

        //2.If there is a service fee collection address set for the operation node on the commission form, it also needs to be calculated
        //Node handling fees that the seller needs to pay
        BigDecimal sellNodeFee = BigDecimal.ZERO;
        if (sellOrder.getFeeAddress() != null && sellOrder.getFeeAddress().length > 0 && sellOrder.getFeeScale() > 0) {
            sellNodeFee = new BigDecimal(quoteAmount).multiply(new BigDecimal(sellOrder.getFeeScale())).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
            if (sellNodeFee.compareTo(BigDecimal.ONE) < 0) {
                sellNodeFee = BigDecimal.ONE;
            }
        }
        //Node handling fees that the buyer needs to pay
        BigDecimal buyNodeFee = BigDecimal.ZERO;
        if (buyOrder.getFeeAddress() != null && buyOrder.getFeeAddress().length > 0 && buyOrder.getFeeScale() > 0) {
            buyNodeFee = new BigDecimal(baseAmount).multiply(new BigDecimal(buyOrder.getFeeScale())).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
            if (buyNodeFee.compareTo(BigDecimal.ONE) < 0) {
                buyNodeFee = BigDecimal.ONE;
            }
        }

        BigInteger sellFee = sellSysFee.add(sellNodeFee).toBigInteger();
        BigInteger buyFee = buySysFee.add(buyNodeFee).toBigInteger();

        //Generating transactionstoTransfer the traded currency to the other party's account
        //The actual quantity received by the other party is after deducting the handling fee
        CoinTo to1, to2, to3, to4, to5 = null, to6 = null;

        //The seller receives the pricing currency
        to1 = new CoinTo(sellOrder.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), quoteAmount.subtract(sellFee));
        //Buyer receives transaction currency
        to2 = new CoinTo(buyOrder.getAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), baseAmount.subtract(buyFee));
        //Payment of handling fees
        //Seller payment system transaction fees
        to3 = new CoinTo(DexContext.sysFeeAddress, tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), sellSysFee.toBigInteger());
        //Buyer payment system transaction fee
        to4 = new CoinTo(DexContext.sysFeeAddress, tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), buySysFee.toBigInteger());

        //Seller pays node handling fees
        if (sellNodeFee.compareTo(BigDecimal.ZERO) > 0) {
            if (Arrays.equals(DexContext.sysFeeAddress, sellOrder.getFeeAddress())) {
                //If the system charges the same handling fee address as the node handling fee address, it will be merged directly
                to3.setAmount(to3.getAmount().add(sellNodeFee.toBigInteger()));
            } else if (Arrays.equals(sellOrder.getAddress(), sellOrder.getFeeAddress())) {
                //If the configuration of the selling address and the node handling fee address are consistent, they will be merged directly
                to1.setAmount(to1.getAmount().add(sellNodeFee.toBigInteger()));
            } else {
                to5 = new CoinTo(sellOrder.getFeeAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), sellNodeFee.toBigInteger());
            }
        }
        //Buyer pays node handling fees
        if (buyNodeFee.compareTo(BigDecimal.ZERO) > 0) {
            if (Arrays.equals(DexContext.sysFeeAddress, buyOrder.getFeeAddress())) {
                //If the system charges the same handling fee address as the node handling fee address, it will be merged directly
                to4.setAmount(to4.getAmount().add(buyNodeFee.toBigInteger()));
            } else if (Arrays.equals(buyOrder.getAddress(), buyOrder.getFeeAddress())) {
                //If the billing address and node handling fee address configuration are consistent, they will be merged directly
                to2.setAmount(to2.getAmount().add(buyNodeFee.toBigInteger()));
            } else {
                to6 = new CoinTo(buyOrder.getFeeAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), buyNodeFee.toBigInteger());
            }
        }

        coinData.addTo(to1);
        coinData.addTo(to2);
        coinData.addTo(to3);
        coinData.addTo(to4);
        if (to5 != null) {
            coinData.addTo(to5);
        }
        if (to6 != null) {
            coinData.addTo(to6);
        }
        map.put("sellFee", sellFee);
        map.put("buyFee", buyFee);
    }

    /**
     * After the transaction is fully completed, the remaining amount of base currency on the purchase order will be returned to the user who made the purchase
     *
     * @param coinData
     * @param tradingPo
     * @param buyOrder
     * @param leftAmount
     * @return
     */
    private static void returnBuyLeftAmountCoinTo(CoinData coinData, CoinTradingPo tradingPo, TradingOrderPo buyOrder, BigInteger leftAmount) {
        if (leftAmount.compareTo(BigInteger.ZERO) == 0) {
            return;
        }
        CoinTo to = new CoinTo(buyOrder.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), leftAmount);
        to.setLockTime(0);

        CoinTo to0 = coinData.getTo().get(0);
        //There is an extreme situation where the buyer and seller are on the same account, and the remaining amount is added together
        if (to0.getAssetsChainId() == to.getAssetsChainId() && to0.getAssetsId() == to.getAssetsId() && Arrays.equals(to0.getAddress(), to.getAddress())) {
            to0.setAmount(to0.getAmount().add(to.getAmount()));
        } else {
            coinData.addTo(to);
        }
    }

    private static boolean addBuyLeftAmountCoinTo(CoinData coinData, CoinTradingPo tradingPo, TradingOrderPo buyOrder, BigInteger leftAmount, BigInteger leftQuoteAmount) {
        //If the remaining amount of unsold coins in the purchase order is already less than a certain amount（Minimum transaction amount1/10),
        //If the remaining amount of coins cannot be purchased to the minimum unit of exchange currency, it will be returned to the user. Otherwise, it will continue to be locked

        boolean buyOver = false;

        if (leftAmount.compareTo(tradingPo.getMinBaseAmount().divide(BigInteger.TEN)) <= 0 || leftAmount.compareTo(BigInteger.TEN) <= 0) {
            buyOver = true;
        }

        if (!buyOver) {

            BigDecimal price = new BigDecimal(buyOrder.getPrice()).movePointLeft(tradingPo.getQuoteDecimal());
            BigDecimal sellDecimal = new BigDecimal(leftAmount);
            sellDecimal = sellDecimal.movePointLeft(tradingPo.getBaseDecimal());
            sellDecimal = sellDecimal.multiply(price).movePointRight(tradingPo.getQuoteDecimal());      //total * unit price = Total Exchange Quantity
            sellDecimal = sellDecimal.setScale(0, RoundingMode.DOWN);
            //If none of them can be exchanged, it is considered that the order has been fully executed
            //When comparing here10, because it includes handling fees that need to be paid3-8Minimum units
            if (sellDecimal.compareTo(BigDecimal.TEN) <= 0) {
                buyOver = true;
            }
        }

        CoinTo to = new CoinTo(buyOrder.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), leftQuoteAmount);
        if (buyOver) {
            CoinTo to0 = coinData.getTo().get(0);
            //There is an extreme situation where the buyer and seller are on the same account, and the remaining amount is added together
            if (to0.getAssetsChainId() == to.getAssetsChainId() && to0.getAssetsId() == to.getAssetsId() && Arrays.equals(to0.getAddress(), to.getAddress())) {
                to0.setAmount(to0.getAmount().add(to.getAmount()));
            } else {
                to.setLockTime(0);
                coinData.addTo(to);
            }
        } else {
            to.setLockTime(DexConstant.DEX_LOCK_TIME);
            coinData.addTo(to);
        }
        return buyOver;
    }


    private static boolean addSellLeftAmountCoinTo(CoinData coinData, CoinTradingPo tradingPo, TradingOrderPo sellOrder, BigInteger leftAmount) {
        //If the remaining unsold coins are already less than a certain quantity（Minimum transaction amount1/10),
        //If the remaining amount of coins cannot be purchased to the minimum unit of exchange currency, it will be returned to the user. Otherwise, it will continue to be locked
        boolean sellOver = false;

        if (leftAmount.compareTo(tradingPo.getMinBaseAmount().divide(BigInteger.TEN)) <= 0 || leftAmount.compareTo(BigInteger.TEN) <= 0) {
            sellOver = true;
        }

        if (!sellOver) {
            //Use the selling price as the final transaction price, Calculate the remaining transaction currency for the sales order and how much base currency can be exchanged
            BigDecimal price = new BigDecimal(sellOrder.getPrice()).movePointLeft(tradingPo.getQuoteDecimal());
            BigDecimal sellDecimal = new BigDecimal(leftAmount);
            sellDecimal = sellDecimal.movePointLeft(tradingPo.getBaseDecimal());
            sellDecimal = sellDecimal.multiply(price).movePointRight(tradingPo.getQuoteDecimal());      //total * unit price = Total Exchange Quantity
            sellDecimal = sellDecimal.setScale(0, RoundingMode.DOWN);
            //If none of them can be exchanged, it is considered that the order has been fully executed
            //When comparing here10, because it includes handling fees that need to be paid3-8Minimum units
            if (sellDecimal.compareTo(BigDecimal.TEN) <= 0) {
                sellOver = true;
            }
        }
        CoinTo to = new CoinTo(sellOrder.getAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), leftAmount);

        if (sellOver) {
            CoinTo to1 = coinData.getTo().get(1);
            //There is an extreme situation where the buyer and seller are on the same account, and the remaining amount is added together
            if (to1.getAssetsChainId() == to.getAssetsChainId() && to1.getAssetsId() == to.getAssetsId() && Arrays.equals(to1.getAddress(), to.getAddress())) {
                to1.setAmount(to1.getAmount().add(to.getAmount()));
            } else {
                to.setLockTime(0);
                coinData.addTo(to);
            }
        } else {
            to.setLockTime(DexConstant.DEX_LOCK_TIME);
            coinData.addTo(to);
        }
        return sellOver;
    }

    /**
     * Assembly transaction pairstxData
     *
     * @param param
     * @return
     */
    public static CoinTrading createCoinTrading(Map<String, Object> param, String address) {
        CoinTrading trading = new CoinTrading();
        trading.setAddress(AddressTool.getAddress(address));
        trading.setQuoteAssetChainId((Integer) param.get("quoteAssetChainId"));
        trading.setQuoteAssetId((Integer) param.get("quoteAssetId"));
        trading.setScaleQuoteDecimal(Byte.parseByte(param.get("scaleQuoteDecimal").toString()));

        trading.setBaseAssetChainId((Integer) param.get("baseAssetChainId"));
        trading.setBaseAssetId((Integer) param.get("baseAssetId"));
        trading.setScaleBaseDecimal(Byte.parseByte(param.get("scaleBaseDecimal").toString()));
        trading.setMinBaseAmount(new BigInteger(param.get("minBaseAmount").toString()));
        trading.setMinQuoteAmount(new BigInteger(param.get("minQuoteAmount").toString()));

        return trading;
    }

    public static TradingOrder createTradingOrder(NulsHash tardingHash, int type, BigInteger amount, BigInteger price, String address) {
        TradingOrder order = new TradingOrder();
        order.setTradingHash(tardingHash.getBytes());
        order.setAmount(amount);
        order.setPrice(price);
        order.setType((byte) type);
        order.setAddress(AddressTool.getAddress(address));
//        order.setFeeScale((byte) 5);
//        order.setFeeAddress(AddressTool.getAddress("TNVTdTSPSrHdJthxhRTCnMZZkUPtPPrrkJKWA"));
        return order;
    }
}
