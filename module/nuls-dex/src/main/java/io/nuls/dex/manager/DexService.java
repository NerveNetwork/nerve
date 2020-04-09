package io.nuls.dex.manager;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.TradingDeal;
import io.nuls.dex.model.txData.TradingOrder;
import io.nuls.dex.model.txData.TradingOrderCancel;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.util.DexUtil;
import io.nuls.dex.util.LoggerUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DexService {


    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private DexManager dexManager;
    //每次打包时，临时缓存取消委托的挂单的orderHash
    private Set<String> tempCancelOrderSet = new HashSet<>();
    /**
     * 每次打包时，因为不能直接修改盘口数据，所以创建一个临时缓存，
     * 将本次打包需用到的盘口数据，缓存一部分到临时缓存里
     */
    private DexManager tempDexManager = new DexManager();
    //临时存放每次打包区块中的委托交易
    private Map<String, List<TradingOrderPo>> tempOrdersMap = new HashMap<>();

    //打包区块时，每次复制盘口内多少挂单，到临时盘口缓存
    private static final int tempOrderSize = 5000;
    //存放每次打包区块时生成的成交交易
    private List<Transaction> dealTxList = new ArrayList<>();


    private ReentrantLock lock = new ReentrantLock();

    /**
     * 打包逻辑：
     * 节点在打包dex模块交易时，若发现有新增的委托买(卖)单交易，需要判断新增挂单的盘口内是否有可以匹配成交的买(卖)单
     * <p>
     * 打包区块撮合成交并非是最终的区块确认存储，不能直接去修改盘口内已有买(卖)单数据，因此打包时撮合的流程是：
     * 1.优先处理撤销挂单，撤销的买(卖)单不再参与成交撮合
     * 2.针对本次新增买(卖)单，复制对应盘口内一定数量买盘和卖盘到临时缓存中
     * 3.将本次打包的新增买(卖)单添加到临时缓存中对应的盘口中
     * 4.循环取出临时缓存中各个交易对的买盘和卖盘数据，做撮合验证，撮合成功后生成成交交易，并更新临时缓存中买盘和卖盘的数据
     *
     * @param txList
     * @return
     * @throws NulsException
     */
    public List<Transaction> doPacking(List<Transaction> txList) throws NulsException {
        lock.lock();


        try {
            dealTxList.clear();
            tempOrdersMap.clear();
            tempDexManager.clear();
            tempCancelOrderSet.clear();

            if (txList.isEmpty()) {
                return dealTxList;
            }

            Transaction tx;
            TradingOrderCancel orderCancel;
            TradingOrder order;
            TradingOrderPo po;
            CoinTo coinTo;

            LoggerUtil.dexLog.debug("----本次打包交易数量：" + txList.size());
            long time0, time1, time2;
            time0 = System.currentTimeMillis();

            for (int i = 0; i < txList.size(); i++) {
                tx = txList.get(i);

        //        LoggerUtil.dexLog.debug("---交易类型：" + tx.getType() + ", hash" + tx.getHash().toHex());

                //记录本次打包中撤销挂单的orderHash，撤销挂单的不再进行价格匹配成交
                if (tx.getType() == TxType.TRADING_ORDER_CANCEL) {
                    orderCancel = new TradingOrderCancel();
                    orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                    tempCancelOrderSet.add(HexUtil.encode(orderCancel.getOrderHash()));
                } else if (tx.getType() == TxType.TRADING_ORDER) {
                    //根据新的挂单，创建临时盘口缓存，并将挂放入临时盘口，为撮合成交做准备
                    coinTo = tx.getCoinDataInstance().getTo().get(0);
                    order = new TradingOrder();
                    order.parse(new NulsByteBuffer(tx.getTxData()));
                    po = new TradingOrderPo(tx.getHash(), coinTo.getAddress(), order);
                    if (po.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                        po.setLeftQuoteAmount(coinTo.getAmount());
                    }
                    TradingContainer container = tempDexManager.getTradingContainer(po.getTradingHash().toHex());
                    if (container == null) {
                        time1 = System.currentTimeMillis();
                        container = copyContainerToTemp(po.getTradingHash().toHex());
                        time2 = System.currentTimeMillis();

                        LoggerUtil.dexLog.debug("打包复制交易盘口用时：" + (time2 - time1));
                    }
                    List<TradingOrderPo> orderPoList = tempOrdersMap.get(container.getCoinTrading().getHash().toHex());
                    if (orderPoList == null) {
                        orderPoList = new ArrayList<>();
                        orderPoList.add(po);
                        tempOrdersMap.put(container.getCoinTrading().getHash().toHex(), orderPoList);
                    } else {
                        orderPoList.add(po);
                    }
                }
            }
            time1 = System.currentTimeMillis();
            matchingOrder();
            time2 = System.currentTimeMillis();
            LoggerUtil.dexLog.debug("---匹配订单耗时：" + (time2 - time1) + "-----成交交易个数：" + dealTxList.size());
            LoggerUtil.dexLog.debug("");
            LoggerUtil.dexLog.debug("---打包总耗时：" + (time2 - time0));
            LoggerUtil.dexLog.debug("");
            return dealTxList;
        } catch (IOException e) {
            LoggerUtil.dexLog.error(e);
            throw new NulsException(DexErrorCode.FAILED);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 拷贝盘口数据到临时缓存，供打包使用
     *
     * @param tradingHash
     * @return
     */
    private TradingContainer copyContainerToTemp(String tradingHash) {
        TradingContainer container = dexManager.getTradingContainer(tradingHash);

        TradingContainer tempContainer = new TradingContainer();
        CoinTradingPo tradingPo = container.getCoinTrading().copy();
        tempContainer.setCoinTrading(tradingPo);
        //拷贝卖盘最后101条挂单
        LinkedList<TradingOrderPo> sellOrderList = new LinkedList<>();
        int start = 0;
        if (container.getSellOrderList().size() > tempOrderSize + 1) {
            start = container.getSellOrderList().size() - tempOrderSize - 1;
        }
        for (int i = start; i < container.getSellOrderList().size(); i++) {
            sellOrderList.add(container.getSellOrderList().get(i).copy());
        }
        tempContainer.setSellOrderList(sellOrderList);
        //拷贝买盘最前面101条
        LinkedList<TradingOrderPo> buyOrderList = new LinkedList<>();
        int end = tempOrderSize + 1;
        if (end > container.getBuyOrderList().size()) {
            end = container.getBuyOrderList().size();
        }
        for (int i = 0; i < end; i++) {
            buyOrderList.add(container.getBuyOrderList().get(i).copy());
        }
        tempContainer.setBuyOrderList(buyOrderList);
        tempDexManager.addContainer(tempContainer);

        return tempContainer;
    }

    /**
     * 循环临时缓存盘口内的所有买单和卖单，进行撮合成交
     */
    private void matchingOrder() throws IOException {
        TradingOrderPo buyOrder;
        TradingOrderPo sellOrder;
        boolean b;
        for (TradingContainer container : tempDexManager.getAllContainer().values()) {
            List<TradingOrderPo> orderPoList = tempOrdersMap.get(container.getCoinTrading().getHash().toHex());
            for (int i = 0; i < orderPoList.size(); i++) {
                TradingOrderPo orderPo = orderPoList.get(i);
                b = true;       //用于判断当前挂单(orderPo)是否需要继续匹配
                while (b) {
                    if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                        buyOrder = orderPo;
                        sellOrder = getLastSellOrder(container.getSellOrderList());
                    } else {
                        sellOrder = orderPo;
                        buyOrder = getFirstBuyOrder(container.getBuyOrderList());
                    }
                    if (buyOrder == null || sellOrder == null) {
                        //说明买盘或卖盘已没有挂单可以匹配，直接将委托挂单添加到临时盘口中
                        container.addTradingOrder(orderPo);
                        b = false;
                    } else if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                        //如果买单小于卖单价格，则不撮合，直接将委托挂单添加到临时盘口中
                        container.addTradingOrder(orderPo);
                        b = false;
                    } else {
                        //匹配成功的订单，生成成交交易
                        //如果是买单主动吃单，则用卖单价作为成交价，反之用买单价作为成交价
                        if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                            createDealTx(container, buyOrder, sellOrder, sellOrder.getPrice());
                        } else {
                            createDealTx(container, buyOrder, sellOrder, buyOrder.getPrice());
                        }
                        //挂单已完全成交，则不再继续匹配
                        if (orderPo.isOver()) {
                            b = false;
                        }
                    }
                }
            }
        }
    }

    /**
     * 撮合成交，生成成交交易
     * 已卖单价格为最终成交价
     * <p>
     * 根据买单和卖单以及双方各自的成交量，生成成交交易
     * 成交交易实际上就是将买单和卖单先解锁，
     * 再各自对应的币种，按照最终成交量，转到对方的账户上
     * 剩余未成交的部分，则继续锁定
     * 此外，各自还需支付手续费
     * tx:{
     * type: 31 //成交交易
     * from:[
     * {address: AAA, value: 2000NULS, lockTime: -2},
     * {address: BBB, value: 1BTC, lockTime: -2}
     * ],
     * to:[
     * {address: BBB, value: 999.9NULS, lockTime: 0},
     * {address: AAA, value: 0.9999BTC, lockTime: 0 },
     * {address: CCC, value: 0.01NULS},					//此为AAA本次撮合成交需要支付的手续费
     * {address: CCC, value: 0.0001BTC},				//此为BBB本次撮合成交需要支付的手续费
     * {address: AAA, value: 1000NULS, lockTime: -2},	//此为未成交部分的NULS，继续锁定
     * ]
     * }
     *
     * @return
     */
    private void createDealTx(TradingContainer container, TradingOrderPo buyOrder, TradingOrderPo sellOrder, BigInteger price) throws IOException {
        CoinTradingPo tradingPo = container.getCoinTrading();
        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_DEAL);
        tx.setTime(NulsDateUtils.getCurrentTimeSeconds());

        //根据成交数据，生成交易的coinData
        Map<String, Object> map = DexUtil.createDealTxCoinData(tradingPo, price, buyOrder, sellOrder);
        CoinData coinData = (CoinData) map.get("coinData");
        tx.setCoinData(coinData.serialize());

        boolean isBuyOver = (boolean) map.get("isBuyOver");
        boolean isSellOver = (boolean) map.get("isSellOver");

        //生成成交记录,txData
        TradingDeal deal = new TradingDeal();
        deal.setTradingHash(tradingPo.getHash().getBytes());
        deal.setBuyHash(buyOrder.getOrderHash().getBytes());
        deal.setSellHash(sellOrder.getOrderHash().getBytes());
        deal.setQuoteAmount((BigInteger) map.get("quoteAmount"));
        deal.setBaseAmount((BigInteger) map.get("baseAmount"));
        deal.setPrice(price);
        if (isBuyOver && !isSellOver) {
            deal.setType(DexConstant.ORDER_BUY_OVER);
        } else if (!isBuyOver && isSellOver) {
            deal.setType(DexConstant.ORDER_SELL_OVER);
        } else if (isBuyOver && isSellOver) {
            deal.setType(DexConstant.ORDER_ALL_OVER);
        }
        tx.setTxData(deal.serialize());
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));

        buyOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().subtract(deal.getQuoteAmount()));
        buyOrder.setDealAmount(buyOrder.getDealAmount().add(deal.getBaseAmount()));
        buyOrder.setOver(isBuyOver);

        sellOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        sellOrder.setDealAmount(sellOrder.getDealAmount().add(deal.getBaseAmount()));
        sellOrder.setOver(isSellOver);
        dealTxList.add(tx);
    }


    /**
     * 买盘是价格从高到底排序好的，每次因从价格最高的第一条开始取出进行撮合
     * 若买单已被撤销，继续取出下一条进行撮合验证
     *
     * @param buyOrderList
     * @return
     */
    private TradingOrderPo getFirstBuyOrder(List<TradingOrderPo> buyOrderList) {
        for (int i = 0; i < buyOrderList.size(); i++) {
            TradingOrderPo buyOrder = buyOrderList.get(i);
            if (!buyOrder.isOver() && !tempCancelOrderSet.contains(buyOrder.getOrderHash().toHex())) {
                return buyOrder;
            }
        }
        return null;
    }

    /**
     * 卖盘也是价格从高到低排序好的，每次因从卖单价格最低的一条开始取出进行撮合
     * 若卖单已被撤销，继续取出下一条进行撮合验证
     *
     * @param sellOrderList
     * @return
     */
    private TradingOrderPo getLastSellOrder(List<TradingOrderPo> sellOrderList) {
        for (int i = sellOrderList.size() - 1; i >= 0; i--) {
            TradingOrderPo sellOrder = sellOrderList.get(i);
            if (!sellOrder.isOver() && !tempCancelOrderSet.contains(sellOrder.getOrderHash().toHex())) {
                return sellOrder;
            }
        }
        return null;
    }
}
