package io.nuls.dex.manager;

import io.nuls.core.exception.NulsException;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.po.TradingOrderPo;

import java.util.LinkedList;

/**
 * 币对盘口管理容器
 * 存放币对的买单和卖单
 */
public class TradingContainer {
    //盘口分段过滤长度
    private static final int SplitFilterSize = 20;

    private CoinTradingPo coinTrading;

    private LinkedList<TradingOrderPo> sellOrderList;

    private LinkedList<TradingOrderPo> buyOrderList;

    public TradingContainer() {

    }

    public TradingContainer(CoinTradingPo coinTrading) {
        this.coinTrading = coinTrading;
        sellOrderList = new LinkedList<>();
        buyOrderList = new LinkedList<>();
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
            addBuyTradingOrder(po);
        } else {
            addSellTradingOrder(po);
        }
    }

    /**
     * 添加到买盘缓存
     *
     * @param o1
     */
    private void addBuyTradingOrder(TradingOrderPo o1) {
        //盘口数据为空，直接放入
        if (buyOrderList.isEmpty()) {
            buyOrderList.add(o1);
            return;
        }

        TradingOrderPo o2;
        //当盘口内有挂单时，如果当前挂单价格高于第一条挂单时，将当前挂单放在盘口最前面
        o2 = buyOrderList.getFirst();
        // >0 ,可实现买单同样价格，按照时间倒序排列
        if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
            buyOrderList.addFirst(o1);
            return;
        }
        //如果当前挂单价格小于最后一条挂单时，将当前挂单放在盘口最后面
        o2 = buyOrderList.getLast();
        if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            buyOrderList.addLast(o1);
            return;
        }
        //其他情况
        addTradingOrderIntoBuyList(o1, 0, buyOrderList.size());
    }

    /**
     * 大于一定数量时，每次取出盘口记录最中间的一条挂单，和当前挂单做价格比对
     * 高于中间挂单价格，则和前半段挂单继续做价格比对，反之和后半段挂单做价格比对
     * 若再次比对记录的条数还是大于一定数量，则递归处理
     *
     * @param o1
     * @param begin
     * @param end
     */
    private void addTradingOrderIntoBuyList(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        if (end - begin > SplitFilterSize) {
            int middle = (end + begin) / 2;
            o2 = buyOrderList.get(middle);
            // >0 ,可实现买单同样价格，按照时间倒序排列
            if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
                addTradingOrderIntoBuyList(o1, begin, middle);
            } else {
                addTradingOrderIntoBuyList(o1, middle, end);
            }
        } else {
            if (end == buyOrderList.size()) {
                end = end - 1;
            }
            boolean isAdd = false;          //记录是否已经加入到盘口
            for (int i = begin; i <= end; i++) {
                o2 = buyOrderList.get(i);
                //如果o1价格高于o2的价格，直接添加到o2位置上
                // >0 ,可实现买单同样价格，按照时间倒序排列
                if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
                    buyOrderList.add(i, o1);
                    isAdd = true;
                    break;
                }
            }
            //如果未加入盘口，则说明价格最低，添加到最后面
            if (!isAdd) {
                buyOrderList.addLast(o1);
            }
        }
    }

    /**
     * 添加到卖盘缓存，逻辑同添加到买盘一样
     * 注意排序规则有区别
     *
     * @param o1
     */
    private void addSellTradingOrder(TradingOrderPo o1) {
        //盘口数据为空，直接放入
        if (sellOrderList.isEmpty()) {
            sellOrderList.add(o1);
            return;
        }
        TradingOrderPo o2;
        o2 = sellOrderList.getFirst();
        //当盘口内有挂单时，如果当前挂单价格高于第一条挂单时，将当前挂单放在盘口最前面
        // >=0 ,可实现卖单同样价格，按照时间倒序排列
        if (o1.getPrice().compareTo(o2.getPrice()) >= 0) {
            sellOrderList.addFirst(o1);
            return;
        }
        //如果当前挂单价格小于最后一条挂单时，将当前挂单放在盘口最后面
        o2 = sellOrderList.getLast();
        if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            sellOrderList.addLast(o1);
            return;
        }
        //其他情况
        addTradingOrderIntoSellList(o1, 0, sellOrderList.size());
    }


    private void addTradingOrderIntoSellList(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        if (end - begin > SplitFilterSize) {
            int middle = (end + begin) / 2;
            o2 = sellOrderList.get(middle);
            // >0 ,可实现买单同样价格，按照时间倒序排列
            if (o1.getPrice().compareTo(o2.getPrice()) >= 0) {
                addTradingOrderIntoSellList(o1, begin, middle);
            } else {
                addTradingOrderIntoSellList(o1, middle, end);
            }
        } else {
            if (end == sellOrderList.size()) {
                end = end - 1;
            }
            boolean isAdd = false;          //记录是否已经加入到盘口
            for (int i = begin; i <= end; i++) {
                o2 = sellOrderList.get(i);
                //如果o1价格高于o2的价格，直接添加到o2位置上
                // >0 ,可实现买单同样价格，按照时间倒序排列
                if (o1.getPrice().compareTo(o2.getPrice()) >= 0) {
                    sellOrderList.add(i, o1);
                    isAdd = true;
                    break;
                }
            }
            //如果未加入盘口，则说明价格最低，添加到最后面
            if (!isAdd) {
                sellOrderList.addLast(o1);
            }
        }
    }

    /**
     * 更新挂单
     *
     * @param o1
     */
    public void updateTradingOrder(TradingOrderPo o1) throws NulsException {
        if (o1.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            if (buyOrderList.isEmpty()) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            boolean b = updateBuyTradingOrder(o1, 0, buyOrderList.size());
            if (!b) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
        } else {
            if (sellOrderList.isEmpty()) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
            boolean b = updateSellTradingOrder(o1, 0, sellOrderList.size());
            if (!b) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "tradingOrder not found");
            }
        }
    }

    /**
     * 发现并更新盘口内的挂单数据
     * 若没有找到对应盘口内的挂单，返回false
     *
     * @param o1
     * @param begin
     * @param end
     * @return
     */
    private boolean updateBuyTradingOrder(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        if (end - begin <= SplitFilterSize) {
            for (int i = begin; i < end; i++) {
                o2 = buyOrderList.get(i);
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            return false;
        }

        int middle = (end + begin) / 2;
        o2 = buyOrderList.get(middle);
        if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
            return updateBuyTradingOrder(o1, begin, middle);
        } else if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            return updateBuyTradingOrder(o1, middle, end);
        } else {
            //当价格一致时，记住当前挂单下标，然后寻找左右价格一致的挂单
            //再匹配orderHash，匹配成功后，更新挂单
            for (int i = middle; i >= 0; i--) {
                o2 = buyOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            for (int i = middle; i < buyOrderList.size(); i++) {
                o2 = buyOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            //匹配不成功，返回false
            return false;
        }
    }

    private boolean updateSellTradingOrder(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        //需要过滤的挂单少于一定数量时，循环匹配订单hash
        if (end - begin <= SplitFilterSize) {
            for (int i = begin; i < end; i++) {
                o2 = sellOrderList.get(i);
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            return false;
        }

        int middle = (end + begin) / 2;
        o2 = sellOrderList.get(middle);
        if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
            return updateSellTradingOrder(o1, begin, middle);
        } else if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            return updateSellTradingOrder(o1, middle, end);
        } else {
            //当价格一致时，记住当前挂单下标，然后左右价格一致的挂单
            //在匹配orderHash，匹配成功后，移除挂单
            for (int i = middle; i >= 0; i--) {
                o2 = sellOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            for (int i = middle; i < sellOrderList.size(); i++) {
                o2 = sellOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    o2.copyFrom(o1);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 将挂单从盘口内移除
     *
     * @param o1
     */
    public void removeTradingOrder(TradingOrderPo o1) {
        if (o1.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            if (buyOrderList.isEmpty()) {
                return;
            }
            removeOrderFromBuyList(o1, 0, buyOrderList.size());
        } else {
            if (sellOrderList.isEmpty()) {
                return;
            }
            removeOrderFromSellList(o1, 0, sellOrderList.size());
        }
    }

    /**
     * 当盘口缓存的挂单大于一定数量时，为了加快效率
     * 首先按照价格缩小过滤范围，找到相同价格的订单
     * 价格匹配后，再匹配orderHash，找到相同orderHash后移除挂单
     * 当需要过滤的挂单低于一定数量时，直接匹配orderHash
     *
     * @param o1
     * @param begin
     * @param end
     */
    private void removeOrderFromBuyList(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        //需要过滤的挂单少于一定数量时，循环匹配订单hash
        if (end - begin <= SplitFilterSize) {
            for (int i = begin; i < end; i++) {
                o2 = buyOrderList.get(i);
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    buyOrderList.remove(i);
                    break;
                }
            }
            return;
        }
        int middle = (end + begin) / 2;
        o2 = buyOrderList.get(middle);
        if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
            removeOrderFromBuyList(o1, begin, middle);
        } else if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            removeOrderFromBuyList(o1, middle, end);
        } else {
            //当价格一致时，记住当前挂单下标，然后左右价格一致的挂单
            //在匹配orderHash，匹配成功后，移除挂单
            for (int i = middle; i >= 0; i--) {
                o2 = buyOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    buyOrderList.remove(i);
                    return;
                }
            }
            for (int i = middle; i < buyOrderList.size(); i++) {
                o2 = buyOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    buyOrderList.remove(i);
                    return;
                }
            }
        }
    }

    private void removeOrderFromSellList(TradingOrderPo o1, int begin, int end) {
        TradingOrderPo o2;
        //需要过滤的挂单少于一定数量时，循环匹配订单hash
        if (end - begin <= SplitFilterSize) {
            for (int i = begin; i < end; i++) {
                o2 = sellOrderList.get(i);
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    sellOrderList.remove(i);
                    break;
                }
            }
            return;
        }
        int middle = (end + begin) / 2;
        o2 = sellOrderList.get(middle);
        if (o1.getPrice().compareTo(o2.getPrice()) > 0) {
            removeOrderFromSellList(o1, begin, middle);
        } else if (o1.getPrice().compareTo(o2.getPrice()) < 0) {
            removeOrderFromSellList(o1, middle, end);
        } else {
            //当价格一致时，记住当前挂单下标，然后左右价格一致的挂单
            //在匹配orderHash，匹配成功后，移除挂单
            for (int i = middle; i >= 0; i--) {
                o2 = sellOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    sellOrderList.remove(i);
                    return;
                }
            }
            for (int i = middle; i < sellOrderList.size(); i++) {
                o2 = sellOrderList.get(i);
                //当价格不一致时，直接跳出循环
                if (o2.getPrice().compareTo(o1.getPrice()) != 0) {
                    break;
                }
                if (o1.getOrderHash().equals(o2.getOrderHash())) {
                    sellOrderList.remove(i);
                    return;
                }
            }
        }
    }

    public CoinTradingPo getCoinTrading() {
        return coinTrading;
    }

    public void setCoinTrading(CoinTradingPo coinTrading) {
        this.coinTrading = coinTrading;
    }

    public LinkedList<TradingOrderPo> getSellOrderList() {
        return sellOrderList;
    }

    public void setSellOrderList(LinkedList<TradingOrderPo> sellOrderList) {
        this.sellOrderList = sellOrderList;
    }

    public LinkedList<TradingOrderPo> getBuyOrderList() {
        return buyOrderList;
    }

    public void setBuyOrderList(LinkedList<TradingOrderPo> buyOrderList) {
        this.buyOrderList = buyOrderList;
    }
}
