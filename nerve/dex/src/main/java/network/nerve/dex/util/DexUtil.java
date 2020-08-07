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
 * dex模块，通用工具类
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
     * 通过币对id信息生成key
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
     * 获取base开头的币对id
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

    /**
     * 获取quote开头的币对id
     *
     * @param po
     * @return
     */
    public static String getCoinTradingKey2(CoinTradingPo po) {
        return po.getQuoteAssetChainId() + "-" + po.getQuoteAssetId() + "-" + po.getBaseAssetChainId() + "-" + po.getBaseAssetId();
    }

    public static byte[] getNonceByHash(NulsHash hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return out;
    }

    /**
     * 生成成交交易的coinData
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
     * 生成成交交易的from ,解锁用户锁定挂单剩余数量的币
     * from的第一条数据强制为买单的解锁信息，第二条为卖单的解锁信息
     *
     * @param tradingPo
     * @param buyOrder
     * @param sellOrder
     */
    public static void addCoinFrom(CoinTradingPo tradingPo, TradingOrderPo buyOrder, TradingOrderPo sellOrder, CoinData coinData) {
        //添加买单from
        CoinFrom from1 = new CoinFrom();
        from1.setAssetsChainId(tradingPo.getQuoteAssetChainId());
        from1.setAssetsId(tradingPo.getQuoteAssetId());
        from1.setAddress(buyOrder.getAddress());
        from1.setNonce(buyOrder.getNonce());
        from1.setAmount(buyOrder.getLeftQuoteAmount());
        from1.setLocked(DexConstant.ASSET_LOCK_TYPE);
        coinData.addFrom(from1);

        //添加卖单from
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
     * 根据成交量，生成成交交易的to
     * 生成to的顺序也是强制的，详细顺序见DexService.createDealTx
     *
     * @param tradingPo
     * @param buyOrder
     * @param sellOrder
     */
    private static Map<String, Object> addCoinTo(CoinTradingPo tradingPo, BigInteger priceBigInteger,
                                                 TradingOrderPo buyOrder, TradingOrderPo sellOrder, CoinData coinData) {

        BigDecimal price = new BigDecimal(priceBigInteger).movePointLeft(tradingPo.getQuoteDecimal());

        if (buyOrder.getLeftAmount().compareTo(sellOrder.getLeftAmount()) == 0) {
            //如果两边所剩交易币种余数量刚好相等
            return processWithEqual(coinData, tradingPo, price, buyOrder, sellOrder);
        } else if (buyOrder.getLeftAmount().compareTo(sellOrder.getLeftAmount()) < 0) {
            //如果买单剩余交易币种数量少于卖单剩余数量
            return processBuyLess(coinData, tradingPo, price, buyOrder, sellOrder);
        } else {
            //如果买单剩余交易币种数量大于卖单剩余数量，需要按照卖单实际剩余数量计算，可以兑换多少基础币种
            return processSellLess(coinData, tradingPo, price, buyOrder, sellOrder);
        }
    }

    private static Map<String, Object> processWithEqual(CoinData coinData, CoinTradingPo tradingPo, BigDecimal price,
                                                        TradingOrderPo buyOrder, TradingOrderPo sellOrder) {
        Map<String, Object> map = new HashMap<>();
        //首先用卖单的币种剩余数量 * 单价 ，计算得到可以兑换到的计价币种总量
        BigDecimal amount = new BigDecimal(sellOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, sellOrder.getLeftAmount(), sellOrder);
        //交易完全成交后，也许买单会有剩余未花费的币，这时需要将剩余币退还给买单用户
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
        //如果买单剩余交易币种数量少于卖单剩余数量，那么卖单需要支付的数量就是买单剩余数量
        //再用最终成交价格计算出买单应该支付多少计价货币给卖家
        BigDecimal amount = new BigDecimal(buyOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, buyOrder.getLeftAmount(), sellOrder);
        //交易完全成交后，也许买单会有剩余未花费的币，这时需要将剩余币退还给买单用户
        returnBuyLeftAmountCoinTo(coinData, tradingPo, buyOrder, buyOrder.getLeftQuoteAmount().subtract(quoteAmount));
        //检查卖单剩余币是否支持继续交易，支持则继续锁定，不支持则退还给卖家
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
        //如果买单剩余数量大于卖单剩余数量，则需要按照卖单剩余交易币种数量计算买单应该支付多少计价币
        BigDecimal amount = new BigDecimal(sellOrder.getLeftAmount()).movePointLeft(tradingPo.getBaseDecimal());
        amount = amount.multiply(price).movePointRight(tradingPo.getQuoteDecimal()).setScale(0, RoundingMode.DOWN);
        BigInteger quoteAmount = amount.toBigInteger();     //最终可以兑换到的计价币种总量

        addDealCoinTo(map, coinData, tradingPo, quoteAmount, buyOrder, sellOrder.getLeftAmount(), sellOrder);
        //检查买单剩余币是否支持继续交易，支持则继续锁定，不支持则退还给买家
        boolean isBuyOver = addBuyLeftAmountCoinTo(coinData, tradingPo, buyOrder, buyOrder.getLeftQuoteAmount().subtract(quoteAmount));
        map.put("isBuyOver", isBuyOver);
        map.put("isSellOver", true);
        map.put("quoteAmount", quoteAmount);
        map.put("baseAmount", sellOrder.getLeftAmount());
        map.put("coinData", coinData);
        return map;
    }

    /**
     * 添加成交交易必然生成的4条to记录
     * 买单成交to
     * 卖单成交to
     * 买单手续费to
     * 卖单手续费to
     */
    private static void addDealCoinTo(Map<String, Object> map, CoinData coinData, CoinTradingPo tradingPo,
                                      BigInteger quoteAmount, TradingOrderPo buyOrder,
                                      BigInteger baseAmount, TradingOrderPo sellOrder) {
        //1.首先计算买卖双方各自需要支付的系统手续费，默认系统手续费为 2/10000
        //若计算出手续费小于资产最小支持位数时，默认收取资产最小单位为手续费

        //卖方需要支付的系统手续费
        BigDecimal sellSysFee = new BigDecimal(quoteAmount).multiply(DexContext.sysFeeScaleDecimal).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
        if (sellSysFee.compareTo(BigDecimal.ONE) < 0) {
            sellSysFee = BigDecimal.ONE;
        }
        //买方需要支付的系统手续费
        BigDecimal buySysFee = new BigDecimal(baseAmount).multiply(DexContext.sysFeeScaleDecimal).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
        if (buySysFee.compareTo(BigDecimal.ONE) < 0) {
            buySysFee = BigDecimal.ONE;
        }

        //2.若委托单上有设置运营节点收取手续费地址，也需要计算
        //卖方需要支付的节点手续费
        BigDecimal sellNodeFee = BigDecimal.ZERO;
        if (sellOrder.getFeeAddress() != null && sellOrder.getFeeAddress().length > 0 && sellOrder.getFeeScale() > 0) {
            sellNodeFee = new BigDecimal(quoteAmount).multiply(new BigDecimal(sellOrder.getFeeScale())).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
            if (sellNodeFee.compareTo(BigDecimal.ONE) < 0) {
                sellNodeFee = BigDecimal.ONE;
            }
        }
        //买方需要支付的节点手续费
        BigDecimal buyNodeFee = BigDecimal.ZERO;
        if (buyOrder.getFeeAddress() != null && buyOrder.getFeeAddress().length > 0 && buyOrder.getFeeScale() > 0) {
            buyNodeFee = new BigDecimal(baseAmount).multiply(new BigDecimal(buyOrder.getFeeScale())).divide(DexConstant.PROP, 0, RoundingMode.HALF_DOWN);
            if (buyNodeFee.compareTo(BigDecimal.ONE) < 0) {
                buyNodeFee = BigDecimal.ONE;
            }
        }

        BigInteger sellFee = sellSysFee.add(sellNodeFee).toBigInteger();
        BigInteger buyFee = buySysFee.add(buyNodeFee).toBigInteger();

        //生成交易的to，将成交后的币转到对方的账户上
        //对方实际收到的数量，是减去了手续费之后的
        CoinTo to1, to2, to3, to4, to5 = null, to6 = null;

        //toList[0] 卖方收到计价币种
        to1 = new CoinTo(sellOrder.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), quoteAmount.subtract(sellFee));
        //toList[1] 买方收到交易币种
        to2 = new CoinTo(buyOrder.getAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), baseAmount.subtract(buyFee));
        //支付手续费
        //toList[2]卖方支付系统手续费
        to3 = new CoinTo(DexContext.sysFeeAddress, tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), sellSysFee.toBigInteger());
        //toList[3]买方支付系统手续费
        to4 = new CoinTo(DexContext.sysFeeAddress, tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), buySysFee.toBigInteger());

        //toList[4]卖方支付节点手续费
        if (sellNodeFee.compareTo(BigDecimal.ZERO) > 0) {
            if (Arrays.equals(DexContext.sysFeeAddress, sellOrder.getFeeAddress())) {
                //如果系统收取手续费地址和节点手续费地址配置一致，则直接合并
                to3.setAmount(to3.getAmount().add(sellNodeFee.toBigInteger()));
            } else if (Arrays.equals(sellOrder.getAddress(), sellOrder.getFeeAddress())) {
                //如果卖单地址和节点手续费地址配置一致，则直接合并
                to1.setAmount(to1.getAmount().add(sellNodeFee.toBigInteger()));
            } else {
                to5 = new CoinTo(sellOrder.getFeeAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), sellNodeFee.toBigInteger());
            }
        }
        //toList[5]买方支付节点手续费
        if (buyNodeFee.compareTo(BigDecimal.ZERO) > 0) {
            if (Arrays.equals(DexContext.sysFeeAddress, buyOrder.getFeeAddress())) {
                //如果系统收取手续费地址和节点手续费地址配置一致，则直接合并
                to4.setAmount(to4.getAmount().add(buyNodeFee.toBigInteger()));
            } else if (Arrays.equals(buyOrder.getAddress(), buyOrder.getFeeAddress())) {
                //如果买单地址和节点手续费地址配置一致，则直接合并
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
     * 交易完全成交后，将买单剩余基础币数量退还给买单用户
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
        //存在一种极端情况，就是买家和卖家是同一个账户，这时就将剩余金额加在一起
        if (to0.getAssetsChainId() == to.getAssetsChainId() && to0.getAssetsId() == to.getAssetsId() && Arrays.equals(to0.getAddress(), to.getAddress())) {
            to0.setAmount(to0.getAmount().add(to.getAmount()));
        } else {
            coinData.addTo(to);
        }
    }

    private static boolean addBuyLeftAmountCoinTo(CoinData coinData, CoinTradingPo tradingPo, TradingOrderPo buyOrder, BigInteger leftQuoteAmount) {
        //如果剩余买单未成交的币已经小于一定数量时（最小成交额的1/10),
        //或者剩余币数量已经无法购买到兑换币的最小单位则退还给用户，否则继续锁定

        boolean buyOver = false;
        if (buyOrder.getLeftAmount().compareTo(tradingPo.getMinQuoteAmount().divide(BigInteger.TEN)) <= 0) {
            buyOver = true;
        }
        if (!buyOver) {
            BigDecimal price = new BigDecimal(buyOrder.getPrice()).movePointLeft(tradingPo.getQuoteDecimal());
            BigDecimal buyAmount = new BigDecimal(leftQuoteAmount);
            buyAmount = buyAmount.movePointLeft(tradingPo.getQuoteDecimal());
            buyAmount = buyAmount.divide(price, tradingPo.getBaseDecimal(), RoundingMode.DOWN);
            buyAmount = buyAmount.movePointRight(tradingPo.getBaseDecimal());
            //如果一个都不能兑换，则视为挂单已完全成交
            //这里比较的时候为10，是因为包含手续费需要支付3-8个最小单位
            if (buyAmount.compareTo(new BigDecimal(10)) <= 0) {
                buyOver = true;
            }
        }
        CoinTo to = new CoinTo(buyOrder.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), leftQuoteAmount);
        if (buyOver) {
            CoinTo to0 = coinData.getTo().get(0);
            //存在一种极端情况，就是买家和卖家是同一个账户，这时就将剩余金额加在一起
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
        //如果剩余卖单未成交的币已经小于一定数量时（最小成交额的1/10),
        //或者剩余币数量已经无法购买到兑换币的最小单位则退还给用户，否则继续锁定
        boolean sellOver = false;

        if (leftAmount.compareTo(tradingPo.getMinBaseAmount().divide(BigInteger.TEN)) <= 0) {
            sellOver = true;
        }
        if (!sellOver) {
            //用卖单的价格作为最终成交价, 计算卖单剩余数量的交易币种，可以兑换多少基础币种
            BigDecimal price = new BigDecimal(sellOrder.getPrice()).movePointLeft(tradingPo.getQuoteDecimal());
            BigDecimal sellDecimal = new BigDecimal(leftAmount);
            sellDecimal = sellDecimal.movePointLeft(tradingPo.getBaseDecimal());
            sellDecimal = sellDecimal.multiply(price).movePointRight(tradingPo.getQuoteDecimal());      //总量 * 单价 = 总兑换数量
            sellDecimal = sellDecimal.setScale(0, RoundingMode.DOWN);
            //如果一个都不能兑换，则视为挂单已完全成交
            //这里比较的时候为10，是因为包含手续费需要支付3-8个最小单位
            if (sellDecimal.compareTo(new BigDecimal(10)) <= 0) {
                sellOver = true;
            }
        }
        CoinTo to = new CoinTo(sellOrder.getAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), leftAmount);

        if (sellOver) {
            CoinTo to1 = coinData.getTo().get(1);
            //存在一种极端情况，就是买家和卖家是同一个账户，这时就将剩余金额加在一起
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
     * 组装交易对txData
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
        order.setFeeScale((byte) 5);
        order.setFeeAddress(AddressTool.getAddress("TNVTdN9iEEaQ68quQYtSu6XMUzd2rwUBtYb7k"));
        return order;
    }
}
