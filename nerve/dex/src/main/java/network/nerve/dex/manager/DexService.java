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
     * During each packaging process, a temporary cache is created because the disk data cannot be directly modified,
     * Cache a portion of the disk data required for this packaging into a temporary cache
     */
    private DexManager tempDexManager = new DexManager();
    //When packaging blocks, how many pending orders are copied from the opening each time, cached in the temporary opening, and the maximum number of transaction orders allowed to be generated per packaging
    private static final int dexPackingSize = 200;
    private List<Transaction> dealTxList = new ArrayList<>();
    //Store transactions that need to be unpacked in each packaged block
    private List<Transaction> removeTxList = new ArrayList<>();
    //Store successfully matched pending transactions for each packaged block
    private Map<String, TradingOrderPo> tempOrderPoMap = new HashMap<>();
    //During each packaging process, the temporary cache needs to cancel the delegation of pending ordersorderHash
    private Set<String> tempCancelOrderSet = new HashSet<>();


    private ReentrantLock lock = new ReentrantLock();

    /**
     * Node packagingdexModule transaction logic：
     * notes： Due to the limited packaging time of a block, the maximum limit for generating transactional transactions in a block is（tempCacheSize）
     * 1. Copy a certain amount of opening data for all transaction pairs during each packaging process（tempCacheSize）Transfer the delegation order to the temporary cache（tempDexManager）
     * 2. Loop through all temporarily cached transaction pairs, and if there are matching pending orders, generate executed transactions
     * 3. If any new entrusted purchases are found(sell)For single transactions, it is necessary to determine whether there are any purchases within the newly added order opening that can be matched for transactions(sell)single
     * Package block matching transaction is not the final block confirmation storage, and cannot directly modify existing purchases in the inventory(sell)Single data, therefore the matching process during packaging is：
     * 1.For this new purchase(sell)Copy a certain number of buying and selling orders within the corresponding inventory to a temporary cache
     * 2.Add new purchases for this package(sell)Add to the corresponding disk slot in the temporary cache
     * 3.Loop to retrieve the buy and sell data of each transaction pair in the temporary cache, perform matching verification, generate successful transactions after matching, and update the buy and sell data in the temporary cache
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
            //First, copy all transaction pairs into the temporary opening
            for (TradingContainer container : dexManager.getAllContainer().values()) {
                TradingContainer copy = container.copy(dexPackingSize);
                tempDexManager.addContainer(copy);
            }
//            time1 = System.currentTimeMillis();
//            if (time1 - time0 > 50) {
//                LoggerUtil.dexLog.info("-------Copying temporary disk data, taking time：{}, block height：{}, isValidate:{}", (time1 - time0), blockHeight, isValidate);
//            }

            List<CancelDeal> cancelList = new ArrayList<>();
            //Before each packaging, check if there are any matching orders in the current temporary inventory. If there are, priority will be given to generating transaction orders
            for (TradingContainer container : tempDexManager.getAllContainer().values()) {
                matchingOrder(container, blockTime);
            }
//            time2 = System.currentTimeMillis();
//            if (time2 - time1 > 100) {
//                LoggerUtil.dexLog.info("-------Processing transactions that were not fully packaged in the previous block, time taken：{}, block height：{}, isValidate:{}", (time2 - time1), blockHeight, isValidate);
//            }
            //If the priority generated transaction has exceeded the packaging limit, the remaining transactions will not be further packaged
            if (dealTxList.size() >= dexPackingSize) {
                LoggerUtil.dexLog.info("-------We have reached the packaging limit,Discard all transactions for this transaction,block height：{}, isValidate:{}, txSize:{}", blockHeight, isValidate, txList.size());
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
                    //Record the cancellation of pending orders during this packaging processorderHashRevoking the order will no longer result in price matching transactions
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
                    //Place the order in the temporary trading position and try to match the transaction
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
//                LoggerUtil.dexLog.info("-------Match all transactions that have been packaged in this block, time taken：{}, block height：{}, isValidate:{}", (time2 - time1), blockHeight, isValidate);
//                LoggerUtil.dexLog.info("-------txList:{}, dealTxList:{}, cancelList:{}", txList.size(), dealTxList.size(), cancelList.size());
//            }

            //Ifindexless thantxList.size(), indicating that due to the transaction reaching the upper limit, some transactions need to be unpacked
            if (index != txList.size() - 1) {
                for (int i = index + 1; i < txList.size(); i++) {
                    removeTxList.add(txList.get(i));
                }
            }

//            time2 = System.currentTimeMillis();
//            if (time2 - time0 > 100) {
//                LoggerUtil.dexLog.info("-------height:{},time consuming:{},isValidate:{}", blockHeight, (time2 - time0), isValidate);
//                LoggerUtil.dexLog.info("-------txList:{}, dealTxList:{}, cancelList:{},removeTxList:{}", txList.size(), dealTxList.size(), cancelList.size(), removeTxList.size());
//            }

            return returnTxMap();
        } catch (IOException e) {
            LoggerUtil.dexLog.error("------Dex service doPacking error-----");
            LoggerUtil.dexLog.info("----height:{},isValidate:{}", blockHeight, isValidate);
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
     * Create cancellation order transaction records
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

        //Add cancellation orderfrom
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

        //Add cancellation orderto
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
     * Loop temporary cache of buy and sell orders within the opening for matching transactions
     */
    private void matchingOrder(TradingContainer container, long blockTime) throws IOException {
        TradingOrderPo buyOrder;
        TradingOrderPo sellOrder;

        boolean b = true;
        while (b) {
            //Once the number of transaction orders generated reaches the upper limit, no further matching will be carried out
            if (dealTxList.size() >= dexPackingSize) {
                return;
            }
            sellOrder = getFirstSellOrder(container.getSellOrderList());
            buyOrder = getFirstBuyOrder(container.getBuyOrderList());

            if (buyOrder == null || sellOrder == null) {
                //Indicating that there are no pending orders to match the buying or selling orders
                b = false;
            } else if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                //If the purchase price is less than the selling price, no matching will be made
                b = false;
            } else {
                //Successfully matched orders generate transaction transactions
                //If taking the initiative to buy, use the selling price as the transaction price, and vice versa, use the buying price as the transaction price
                if (buyOrder.compareTo(sellOrder) > 0) {
                    createDealTx(container, buyOrder, sellOrder, DexConstant.BUY_TAKER, blockTime);
                } else {
                    createDealTx(container, buyOrder, sellOrder, DexConstant.SELL_TAKER, blockTime);
                }
                //Store the matched delegation orders in a temporary cache
                tempOrderPoMap.put(sellOrder.getOrderHash().toHex(), sellOrder);
                tempOrderPoMap.put(buyOrder.getOrderHash().toHex(), buyOrder);
            }
        }
    }

    /**
     * Match transactions, generate transaction transactions
     * The price of the sold order is the final transaction price
     * <p>
     * Generate transaction volume based on purchase and sale orders, as well as the respective transaction volumes of both parties
     * The transaction is actually unlocking the buy and sell orders first,
     * Transfer the corresponding currency to the other party's account based on the final transaction volume
     * The remaining unconsumed portion will continue to be locked
     * In addition, each party also needs to pay a handling fee
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
        //Generate transaction based on transaction datacoinData
        Map<String, Object> map = DexUtil.createDealTxCoinData(tradingPo, price, buyOrder, sellOrder);
        boolean isBuyOver = (boolean) map.get("isBuyOver");
        boolean isSellOver = (boolean) map.get("isSellOver");

        //Generate transaction
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
     * Buying is sorted by price from high to low, and each time a match is made by taking out the highest priced item starting from the first item
     * If the purchase order has been cancelled, continue to retrieve the next item for matching verification
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
     * Selling orders are sorted in descending order of price, and each time they are matched by starting from the lowest selling order price
     * If the sales order has been revoked, continue to retrieve the next one for matching verification
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
