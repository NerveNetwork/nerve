package network.nerve.dex.manager;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.CoinFromComparator;
import network.nerve.dex.util.CoinToComparator;
import network.nerve.dex.util.DexUtil;
import network.nerve.dex.util.LoggerUtil;
import network.nerve.dex.model.txData.*;

import java.io.IOException;
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
    /**
     * 每次打包时，因为不能直接修改盘口数据，所以创建一个临时缓存，
     * 将本次打包需用到的盘口数据，缓存一部分到临时缓存里
     */
    private DexManager tempDexManager = new DexManager();
    //打包区块时，每次复制盘口内多少挂单，到临时盘口缓存，以及每次打包最多允许生成成交单的数量
    private static final int dexPackingSize = 200;
    private List<Transaction> dealTxList = new ArrayList<>();
    //存放每次打包的区块中，需要取消打包的交易
    private List<Transaction> removeTxList = new ArrayList<>();
    //存放每次打包区块时匹配成功的挂单交易
    private Map<String, TradingOrderPo> tempOrderPoMap = new HashMap<>();
    //每次打包时，临时缓存需要取消委托挂单的orderHash
    private Set<String> tempCancelOrderSet = new HashSet<>();


    private ReentrantLock lock = new ReentrantLock();

    /**
     * 节点打包dex模块交易逻辑：
     * 注： 由于一个区块打包时间有限，因此一个区块最多允许生成成交交易的上限为（tempCacheSize）
     * 1. 每次打包时，将所有交易对的盘口数据复制一定数量（tempCacheSize）的委托单到临时缓存里（tempDexManager）
     * 2. 循环所有临时缓存的交易对盘口，若有可以匹配的挂单，则生成成交交易
     * 3. 若发现有新增的委托买(卖)单交易，需要判断新增挂单的盘口内是否有可以匹配成交的买(卖)单
     * 打包区块撮合成交并非是最终的区块确认存储，不能直接去修改盘口内已有买(卖)单数据，因此打包时撮合的流程是：
     * 1.针对本次新增买(卖)单，复制对应盘口内一定数量买盘和卖盘到临时缓存中
     * 2.将本次打包的新增买(卖)单添加到临时缓存中对应的盘口中
     * 3.循环取出临时缓存中各个交易对的买盘和卖盘数据，做撮合验证，撮合成功后生成成交交易，并更新临时缓存中买盘和卖盘的数据
     *
     * @param txList
     * @return
     * @throws NulsException
     */
    public Map<String, List<Transaction>> doPacking(List<Transaction> txList, long blockTime, long blockHeight, boolean isValidate) throws NulsException {
        lock.lock();

        try {
            clear();

//            long time0, time1, time2;
//            time0 = System.currentTimeMillis();
            //首先复制所有交易对到临时盘口中
            for (TradingContainer container : dexManager.getAllContainer().values()) {
                TradingContainer copy = container.copy(dexPackingSize);
                tempDexManager.addContainer(copy);
            }
//            time1 = System.currentTimeMillis();
//            if (time1 - time0 > 50) {
//                LoggerUtil.dexLog.info("-------复制临时盘口数据，用时：{}, 区块高度：{}, isValidate:{}", (time1 - time0), blockHeight, isValidate);
//            }

            List<CancelDeal> cancelList = new ArrayList<>();
            //每次打包前先判断当前所有临时盘口里是否还有可以匹配的订单，如果有则优先生成成交订单
            for (TradingContainer container : tempDexManager.getAllContainer().values()) {
                matchingOrder(container, blockTime);
            }
//            time2 = System.currentTimeMillis();
//            if (time2 - time1 > 100) {
//                LoggerUtil.dexLog.info("-------处理之前区块未能打包完的成交交易，用时：{}, 区块高度：{}, isValidate:{}", (time2 - time1), blockHeight, isValidate);
//            }
            //如果优先生成的成交交易已经超过打包上限，则不再继续打包剩余交易
            if (dealTxList.size() >= dexPackingSize) {
                LoggerUtil.dexLog.info("-------达到打包上限了,丢弃本次的所有交易,区块高度：{}, isValidate:{}， txSize:{}", blockHeight, isValidate, txList.size());
                removeTxList.addAll(txList);
                return returnTxMap();
            }

            Transaction tx;
            TradingOrderCancel orderCancel;
            TradingOrder order;
            TradingOrderPo po;
            CoinTo coinTo;
            Map<String, CoinFrom> coinFromMap = new HashMap<>();
            Map<String, CoinTo> coinToMap = new HashMap<>();
            int index = 0;
//            time1 = System.currentTimeMillis();

            for (int i = 0; i < txList.size(); i++) {
                tx = txList.get(i);
                tx.setBlockHeight(blockHeight);
                index = i;

                if (tx.getType() == TxType.TRADING_ORDER_CANCEL) {
                    //记录本次打包中撤销挂单的orderHash，撤销挂单的不再进行价格匹配成交
                    orderCancel = new TradingOrderCancel();
                    orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                    tempCancelOrderSet.add(HexUtil.encode(orderCancel.getOrderHash()));

                    String orderKey = HexUtil.encode(orderCancel.getOrderHash());
                    TradingOrderPo orderPo = tempOrderPoMap.get(orderKey);
                    if (orderPo == null) {
                        orderPo = orderStorageService.query(orderCancel.getOrderHash());
                        if (orderPo != null) {
                            tempOrderPoMap.put(orderPo.getOrderHash().toHex(), orderPo);
                        }
                    }

                    createCancelDeal(tx, cancelList, orderCancel, orderPo, coinFromMap, coinToMap);

                } else if (tx.getType() == TxType.TRADING_ORDER) {
                    //将挂单放入临时盘口，尝试撮合成交
                    coinTo = tx.getCoinDataInstance().getTo().get(0);
                    order = new TradingOrder();
                    order.parse(new NulsByteBuffer(tx.getTxData()));
                    po = new TradingOrderPo(tx, i, coinTo.getAddress(), order);
                    if (po.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                        po.setLeftQuoteAmount(coinTo.getAmount());
                    } else {
                        po.setLeftQuoteAmount(BigInteger.ZERO);
                    }
                    TradingContainer container = tempDexManager.getTradingContainer(po.getTradingHash().toHex());
                    container.addTradingOrder(po);

                    matchingOrder(container, blockTime);
                    if (dealTxList.size() >= dexPackingSize) {
                        break;
                    }
                }
            }
            if (cancelList.size() > 0) {
                createCancelTx(cancelList, coinFromMap, coinToMap, blockTime);
            }
//            time2 = System.currentTimeMillis();
//            if (time2 - time1 > 100) {
//                LoggerUtil.dexLog.info("-------匹配本次区块打包完的所有交易，用时：{}, 区块高度：{}, isValidate:{}", (time2 - time1), blockHeight, isValidate);
//                LoggerUtil.dexLog.info("-------txList:{}, dealTxList:{}, cancelList:{}", txList.size(), dealTxList.size(), cancelList.size());
//            }

            //如果index小于txList.size()，说明因为成交交易达到上限，导致有一部分交易需要取消打包
            if (index != txList.size() - 1) {
                for (int i = index + 1; i < txList.size(); i++) {
                    removeTxList.add(txList.get(i));
                }
            }

//            time2 = System.currentTimeMillis();
//            if (time2 - time0 > 100) {
//                LoggerUtil.dexLog.info("-------高度:{},耗时:{},isValidate:{}", blockHeight, (time2 - time0), isValidate);
//                LoggerUtil.dexLog.info("-------txList:{}, dealTxList:{}, cancelList:{},removeTxList:{}", txList.size(), dealTxList.size(), cancelList.size(), removeTxList.size());
//            }

            return returnTxMap();
        } catch (IOException e) {
            LoggerUtil.dexLog.error("------Dex service doPacking error-----");
            LoggerUtil.dexLog.info("----高度:{},isValidate:{}", blockHeight, isValidate);
            LoggerUtil.dexLog.error(e);
            throw new NulsException(DexErrorCode.FAILED);
        } finally {
            lock.unlock();
        }
    }

    private void createCancelTx(List<CancelDeal> cancelList, Map<String, CoinFrom> coinFromMap, Map<String, CoinTo> coinToMap, long blockTime) throws NulsException, IOException {
        Transaction cancelTx = new Transaction();
        cancelTx.setType(TxType.ORDER_CANCEL_CONFIRM);
        cancelTx.setTime(blockTime);

        CoinData coinData = createCoinData(coinFromMap, coinToMap);
        cancelTx.setCoinData(coinData.serialize());

        TradingCancelTxData txData = new TradingCancelTxData();
        txData.setCancelDealList(cancelList);
        cancelTx.setTxData(txData.serialize());
        cancelTx.setHash(NulsHash.calcHash(cancelTx.serializeForHash()));
        dealTxList.add(cancelTx);
    }

    private CoinData createCoinData(Map<String, CoinFrom> coinFromMap, Map<String, CoinTo> coinToMap) throws NulsException {
        CoinData coinData = new CoinData();

        List<CoinFrom> froms = new ArrayList<>(coinFromMap.values());
        List<CoinTo> tos = new ArrayList<>(coinToMap.values());

        Collections.sort(froms, CoinFromComparator.getInstance());
        Collections.sort(tos, CoinToComparator.getInstance());

        coinData.setFrom(froms);
        coinData.setTo(tos);

        BigInteger from1 = BigInteger.ZERO;
        for (CoinFrom from : coinData.getFrom()) {
            from1 = from1.add(from.getAmount());
        }

        BigInteger to1 = BigInteger.ZERO;
        for (CoinTo to : coinData.getTo()) {
            to1 = to1.add(to.getAmount());
        }

        if (from1.compareTo(to1) != 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "canceltx coindata from to amount not equals");
        }
        return coinData;
    }

    /**
     * 创建取消订单成交记录
     *
     * @param orderPo
     * @param coinFromMap
     * @param coinToMap
     */
    private void createCancelDeal(Transaction tx, List<CancelDeal> cancelList, TradingOrderCancel orderCancel, TradingOrderPo orderPo,
                                  Map<String, CoinFrom> coinFromMap, Map<String, CoinTo> coinToMap) {
        CancelDeal cancelDeal = new CancelDeal();
        cancelDeal.setCancelHash(tx.getHash().getBytes());
        cancelDeal.setOrderHash(orderCancel.getOrderHash());
        if (orderPo == null || orderPo.isOver()) {
            cancelDeal.setStatus(DexConstant.CANCEL_ORDER_FAIL);
            cancelDeal.setCancelAmount(BigInteger.ZERO);
            cancelList.add(cancelDeal);
            return;
        }
        orderPo.setOver(true);
        cancelDeal.setStatus(DexConstant.CANCEL_ORDER_SUCC);
        cancelDeal.setCancelAmount(orderPo.getLeftAmount());
        cancelList.add(cancelDeal);

        TradingContainer container = tempDexManager.getTradingContainer(orderPo.getTradingHash().toHex());
        CoinTradingPo tradingPo = container.getCoinTrading();

        //添加撤销单from
        String key = orderPo.getOrderHash().toHex();
        if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            if (!coinFromMap.containsKey(key)) {
                CoinFrom from = new CoinFrom();
                from.setAssetsChainId(tradingPo.getQuoteAssetChainId());
                from.setAssetsId(tradingPo.getQuoteAssetId());
                from.setAddress(orderPo.getAddress());
                from.setNonce(orderPo.getNonce());
                from.setAmount(orderPo.getLeftQuoteAmount());
                from.setLocked(DexConstant.ASSET_LOCK_TYPE);
                coinFromMap.put(key, from);
            }
        } else {
            if (!coinFromMap.containsKey(key)) {
                CoinFrom from = new CoinFrom();
                from.setAssetsChainId(tradingPo.getBaseAssetChainId());
                from.setAssetsId(tradingPo.getBaseAssetId());
                from.setAddress(orderPo.getAddress());
                from.setNonce(orderPo.getNonce());
                from.setAmount(orderPo.getLeftAmount());
                from.setLocked(DexConstant.ASSET_LOCK_TYPE);
                coinFromMap.put(key, from);
            }
        }

        //添加撤销单to
        if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            key = DexUtil.getCoinKey(AddressTool.getStringAddressByBytes(orderPo.getAddress()), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId());
            CoinTo to = coinToMap.get(key);
            if (to == null) {
                to = new CoinTo(orderPo.getAddress(), tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId(), orderPo.getLeftQuoteAmount());
                coinToMap.put(key, to);
            } else {
                to.setAmount(to.getAmount().add(orderPo.getLeftQuoteAmount()));
            }
        } else {
            key = DexUtil.getCoinKey(AddressTool.getStringAddressByBytes(orderPo.getAddress()), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId());
            CoinTo to = coinToMap.get(key);
            if (to == null) {
                to = new CoinTo(orderPo.getAddress(), tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId(), orderPo.getLeftAmount());
                coinToMap.put(key, to);
            } else {
                to.setAmount(to.getAmount().add(orderPo.getLeftAmount()));
            }
        }
    }

    /**
     * 循环临时缓存盘口内的买单和卖单，进行撮合成交
     */
    private void matchingOrder(TradingContainer container, long blockTime) throws IOException {
        TradingOrderPo buyOrder;
        TradingOrderPo sellOrder;

        boolean b = true;
        while (b) {
            //每次生成成交单数量，达到上限后，不再继续撮合
            if (dealTxList.size() >= dexPackingSize) {
                return;
            }
            sellOrder = getFirstSellOrder(container.getSellOrderList());
            buyOrder = getFirstBuyOrder(container.getBuyOrderList());

            if (buyOrder == null || sellOrder == null) {
                //说明买盘或卖盘已没有挂单可以匹配
                b = false;
            } else if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                //如果买单小于卖单价格，则不撮合
                b = false;
            } else {
                //匹配成功的订单，生成成交交易
                //如果是买单主动吃单，则用卖单价作为成交价，反之用买单价作为成交价
                if (buyOrder.compareTo(sellOrder) > 0) {
                    createDealTx(container, buyOrder, sellOrder, DexConstant.BUY_TAKER, blockTime);
                } else {
                    createDealTx(container, buyOrder, sellOrder, DexConstant.SELL_TAKER, blockTime);
                }
                //将匹配过的委托单存放在临时缓存里
                tempOrderPoMap.put(sellOrder.getOrderHash().toHex(), sellOrder);
                tempOrderPoMap.put(buyOrder.getOrderHash().toHex(), buyOrder);
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
     * }
     *
     * @return
     */
    private void createDealTx(TradingContainer container, TradingOrderPo buyOrder, TradingOrderPo sellOrder, byte taker, long blockTime) throws IOException {
        CoinTradingPo tradingPo = container.getCoinTrading();
        BigInteger price;
        if (taker == DexConstant.BUY_TAKER) {
            price = sellOrder.getPrice();
        } else {
            price = buyOrder.getPrice();
        }
        //根据成交数据，生成交易的coinData
        Map<String, Object> map = DexUtil.createDealTxCoinData(tradingPo, price, buyOrder, sellOrder);
        boolean isBuyOver = (boolean) map.get("isBuyOver");
        boolean isSellOver = (boolean) map.get("isSellOver");

        //生成成交交易
        Transaction tx = new Transaction();
        tx.setType(TxType.TRADING_DEAL);
        tx.setTime(blockTime);
        CoinData coinData = (CoinData) map.get("coinData");
        try {
            tx.setCoinData(coinData.serialize());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("---  coindata serialize error--------", e);
            LoggerUtil.dexLog.error("buyOrder:{}", buyOrder.toString());
            LoggerUtil.dexLog.error("sellOrder:{}", sellOrder.toString());
            LoggerUtil.dexLog.error(coinData.toString());
        }


        TradingDeal deal = new TradingDeal();
        deal.setTradingHash(container.getCoinTrading().getHash().getBytes());
        deal.setBuyHash(buyOrder.getOrderHash().getBytes());
        deal.setBuyNonce(buyOrder.getNonce());
        deal.setSellHash(sellOrder.getOrderHash().getBytes());
        deal.setSellNonce(sellOrder.getNonce());
        deal.setQuoteAmount((BigInteger) map.get("quoteAmount"));
        deal.setBaseAmount((BigInteger) map.get("baseAmount"));
        deal.setBuyFee((BigInteger) map.get("buyFee"));
        deal.setSellFee((BigInteger) map.get("sellFee"));
        deal.setPrice(price);
        deal.setTaker(taker);
        if (isBuyOver && !isSellOver) {
            deal.setType(DexConstant.ORDER_BUY_OVER);
        } else if (!isBuyOver && isSellOver) {
            deal.setType(DexConstant.ORDER_SELL_OVER);
        } else if (isBuyOver && isSellOver) {
            deal.setType(DexConstant.ORDER_ALL_OVER);
        }
        tx.setTxData(deal.serialize());
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        dealTxList.add(tx);

        buyOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().subtract(deal.getQuoteAmount()));
        buyOrder.setDealAmount(buyOrder.getDealAmount().add(deal.getBaseAmount()));
        buyOrder.setOver(isBuyOver);

        sellOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        sellOrder.setDealAmount(sellOrder.getDealAmount().add(deal.getBaseAmount()));
        sellOrder.setOver(isSellOver);
    }

    /**
     * 买盘是价格从高到底排序好的，每次因从价格最高的第一条开始取出进行撮合
     * 若买单已被撤销，继续取出下一条进行撮合验证
     *
     * @param buyOrderList
     * @return
     */
    private TradingOrderPo getFirstBuyOrder(NavigableMap<BigInteger, Map<String, TradingOrderPo>> buyOrderList) {
        if (buyOrderList.isEmpty()) {
            return null;
        }
        for (Map.Entry<BigInteger, Map<String, TradingOrderPo>> entry : buyOrderList.entrySet()) {
            Map<String, TradingOrderPo> map = entry.getValue();
            for (TradingOrderPo buyOrder : map.values()) {
                if (!buyOrder.isOver() && !tempCancelOrderSet.contains(buyOrder.getOrderHash().toHex())) {
                    return buyOrder;
                }
            }
        }
        return null;
    }

    /**
     * 卖盘是价格从低到高排序好的，每次因从卖单价格最低的一条开始取出进行撮合
     * 若卖单已被撤销，继续取出下一条进行撮合验证
     *
     * @param sellOrderList
     * @return
     */
    private TradingOrderPo getFirstSellOrder(NavigableMap<BigInteger, Map<String, TradingOrderPo>> sellOrderList) {
        if (sellOrderList.isEmpty()) {
            return null;
        }
        for (Map.Entry<BigInteger, Map<String, TradingOrderPo>> entry : sellOrderList.entrySet()) {
            Map<String, TradingOrderPo> map = entry.getValue();
            for (TradingOrderPo buyOrder : map.values()) {
                if (!buyOrder.isOver() && !tempCancelOrderSet.contains(buyOrder.getOrderHash().toHex())) {
                    return buyOrder;
                }
            }
        }
        return null;
    }

    private Map<String, List<Transaction>> returnTxMap() {
        Map<String, List<Transaction>> resultMap = new HashMap<>();
//        dealTxList.forEach(tx -> {
//            Set<String> nonce = new HashSet<>();
//            try {
//                if (tx.getType() == TxType.ORDER_CANCEL_CONFIRM) {
//                    tx.getCoinDataInstance().getFrom().forEach(f -> {
//                        System.out.println();
//                    });
//                }
//            } catch (NulsException e) {
//                Log.error(e);
//            }
//        });
        resultMap.put("dealTxList", dealTxList);
        resultMap.put("removeTxList", removeTxList);
        return resultMap;
    }

    private void clear() {
        dealTxList.clear();
        tempDexManager.clear();
        tempCancelOrderSet.clear();
        tempOrderPoMap.clear();
        removeTxList.clear();
    }
}
