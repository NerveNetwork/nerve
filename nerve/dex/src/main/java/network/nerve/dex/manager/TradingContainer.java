package network.nerve.dex.manager;

import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Coin to disk management container
 * Purchase and sale orders for storing currency pairs
 */
public class TradingContainer {

    private CoinTradingPo coinTrading;

    private NavigableMap<BigInteger, Map<String, TradingOrderPo>> sellOrderList;

    private NavigableMap<BigInteger, Map<String, TradingOrderPo>> buyOrderList;

    public TradingContainer() {
        //Sell orders in positive order of price
        sellOrderList = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
        //Purchase orders are arranged in reverse order of price
        buyOrderList = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    }

    public TradingContainer(CoinTradingPo coinTrading) {
        this();
        this.coinTrading = coinTrading;
    }

    /**
     * Add delegated order placement
     * Cache the corresponding inventory based on the purchase and sale orders separately
     * Add order placement rules:
     * vouchers of sale：Arrange in reverse order according to the listed price, and arrange the same prices in reverse chronological order
     * Pay the bill：Sort in reverse order according to the listing price, and sort the same prices in positive chronological order
     * Final effect：
     * vouchers of sale
     * list[0] price: 100, time: 12:00:00
     * list[1] price: 100, time: 11:00:00
     * list[2] price: 99,  time: 11:00:00
     * list[3]price: 99,  time: 10:00:00
     * Pay the bill
     * list[0] price: 98,  time: 10:00:00
     * list[1] price: 98,  time: 11:00:00
     * list[2] price: 97,  time: 11:00:00
     * list[3] price: 97,  time: 12:00:00
     * After sorting in this way, when packaging blocks, the first data of the purchase order and the last data of the sell order are used for price matching verification, generating transaction transactions
     *
     * @param po
     */
    public void addTradingOrder(TradingOrderPo po) {
        if (po.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            addToTradingOrder(buyOrderList, po);
        } else {
            addToTradingOrder(sellOrderList, po);
        }
    }

    private void addToTradingOrder(NavigableMap<BigInteger, Map<String, TradingOrderPo>> orderList, TradingOrderPo o1) {
        String lock = coinTrading.getHash().toHex() + o1.getPrice();
        orderList.compute(o1.getPrice(), (key, list) -> {
            synchronized (lock) {
                if (list == null) {
                    list = new LinkedHashMap<>();
                }
                list.put(o1.getOrderHash().toHex(), o1);
            }
            return list;
        });
    }

    /**
     * Update order placement
     *
     * @param o1
     */
    public void updateTradingOrder(TradingOrderPo o1) throws NulsException {
        TradingOrderPo oldOrder;
        if (o1.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            if (buyOrderList.isEmpty()) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            oldOrder = getTradingOrder(buyOrderList, o1.getPrice(), o1.getOrderHash().toHex());
            if (oldOrder == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            oldOrder.copyFrom(o1);
        } else {
            if (sellOrderList.isEmpty()) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            oldOrder = getTradingOrder(sellOrderList, o1.getPrice(), o1.getOrderHash().toHex());
            if (oldOrder == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            oldOrder.copyFrom(o1);
        }
    }

    private TradingOrderPo getTradingOrder(NavigableMap<BigInteger, Map<String, TradingOrderPo>> orderList, BigInteger price, String hash) {
        String lock = coinTrading.getHash().toHex() + price;
        synchronized (lock) {
            Map<String, TradingOrderPo> list = orderList.get(price);
            if (list == null) {
                return null;
            }
            return list.get(hash);
        }
    }

    /**
     * Remove the order from the inventory
     *
     * @param o1
     */

    public void removeTradingOrder(TradingOrderPo o1) {
        if (o1.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            removeTradingOrder(buyOrderList, o1);
        } else {
            removeTradingOrder(sellOrderList, o1);
        }
    }

    private TradingOrderPo removeTradingOrder(NavigableMap<BigInteger, Map<String, TradingOrderPo>> orderList, TradingOrderPo o1) {
        String lock = coinTrading.getHash().toHex() + o1.getPrice();
        synchronized (lock) {
            Map<String, TradingOrderPo> list = orderList.get(o1.getPrice());
            if (list == null) {
                return null;
            }
            list.remove(o1.getOrderHash().toHex());
            if (list.isEmpty()) {
                orderList.remove(o1.getPrice());
            }
            return o1;
        }
    }

    public CoinTradingPo getCoinTrading() {
        return coinTrading;
    }

    public void setCoinTrading(CoinTradingPo coinTrading) {
        this.coinTrading = coinTrading;
    }

    public NavigableMap<BigInteger, Map<String, TradingOrderPo>> getSellOrderList() {
        return sellOrderList;
    }

    public void setSellOrderList(NavigableMap<BigInteger, Map<String, TradingOrderPo>> sellOrderList) {
        this.sellOrderList = sellOrderList;
    }

    public NavigableMap<BigInteger, Map<String, TradingOrderPo>> getBuyOrderList() {
        return buyOrderList;
    }

    public void setBuyOrderList(NavigableMap<BigInteger, Map<String, TradingOrderPo>> buyOrderList) {
        this.buyOrderList = buyOrderList;
    }

    public TradingContainer copy(int orderSize) {
        TradingContainer copy = new TradingContainer();
        copy.setCoinTrading(this.coinTrading.copy());
        //Copy the front of the purchase order for the openingorderSizestrip
        int count = 0;

        for (Map.Entry<BigInteger, Map<String, TradingOrderPo>> entry : this.getBuyOrderList().entrySet()) {
            if (count > orderSize) break;
            Map<String, TradingOrderPo> map = entry.getValue();
            for (TradingOrderPo orderPo : map.values()) {
                copy.addTradingOrder(orderPo.copy());
                count++;
                if (count > orderSize) break;
            }
        }
        //Copy the front of the inventory sales orderorderSizestrip
        count = 0;
        for (Map.Entry<BigInteger, Map<String, TradingOrderPo>> entry : this.getSellOrderList().entrySet()) {
            if (count > orderSize) break;
            Map<String, TradingOrderPo> map = entry.getValue();
            for (TradingOrderPo orderPo : map.values()) {
                copy.addTradingOrder(orderPo.copy());
                count++;
                if (count > orderSize) break;
            }
        }
        return copy;
    }
}
