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
 * 币对盘口管理容器
 * 存放币对的买单和卖单
 */
public class TradingContainer {

    private CoinTradingPo coinTrading;

    private NavigableMap<BigInteger, Map<String, TradingOrderPo>> sellOrderList;

    private NavigableMap<BigInteger, Map<String, TradingOrderPo>> buyOrderList;

    public TradingContainer() {
        //卖单按照价格正序排列
        sellOrderList = new ConcurrentSkipListMap<>(Comparator.naturalOrder());
        //买单按照价格倒序排列
        buyOrderList = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    }

    public TradingContainer(CoinTradingPo coinTrading) {
        this();
        this.coinTrading = coinTrading;
    }

    /**
     * 添加委托挂单
     * 根据买单和卖单分别缓存到对应盘口
     * 添加挂单规则:
     * 卖单：按照挂单价格倒序排列，相同价格按照时间顺序倒序排列
     * 买单：按照挂单价格倒序排列，相同价格按照时间顺序正序排序
     * 最终效果：
     * 卖单
     * list[0] price: 100, time: 12:00:00
     * list[1] price: 100, time: 11:00:00
     * list[2] price: 99,  time: 11:00:00
     * list[3]price: 99,  time: 10:00:00
     * 买单
     * list[0] price: 98,  time: 10:00:00
     * list[1] price: 98,  time: 11:00:00
     * list[2] price: 97,  time: 11:00:00
     * list[3] price: 97,  time: 12:00:00
     * 这样排序后，打包区块时，就用买单的第一条数据和卖单的最后一条数据做价格匹配验证，生成成交交易
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
     * 更新挂单
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
     * 将挂单从盘口内移除
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
        //复制盘口买单的前orderSize条
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
        //复制盘口卖单的前orderSize条
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
