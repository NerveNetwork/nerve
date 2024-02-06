package network.nerve.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.po.TradingDealPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.TradingDeal;
import network.nerve.dex.storage.NonceOrderStorageService;
import network.nerve.dex.storage.TradingDealStorageService;
import network.nerve.dex.storage.TradingOrderCancelStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.DexUtil;
import network.nerve.dex.util.LoggerUtil;

import java.util.List;
import java.util.Map;

@Component("TradingDealProcessorV1")
public class TradingDealProcessor implements TransactionProcessor {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private TradingOrderCancelStorageService orderCancelStorageService;
    @Autowired
    private TradingDealStorageService tradingDealStorageService;
    @Autowired
    private NonceOrderStorageService nonceOrderStorageService;

    @Override
    public int getType() {
        return TxType.TRADING_DEAL;
    }

    /**
     * System transactions are not validated separately, but are validated together when submitted
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return null;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

    /**
     * @param tx
     */
    public void tradingDealCommit(Transaction tx) {
        try {
            TradingDeal deal = new TradingDeal();
            deal.parse(new NulsByteBuffer(tx.getTxData()));
            TradingDealPo dealPo = new TradingDealPo(tx.getHash(), deal);
            //Find the corresponding buy and sell orders
            TradingContainer container = dexManager.getTradingContainer(dealPo.getTradingHash().toHex());
            TradingOrderPo buyOrder = orderStorageService.query(deal.getBuyHash());
            TradingOrderPo sellOrder = orderStorageService.query(deal.getSellHash());

            save(tx, container, dealPo, buyOrder, sellOrder);

//            for (int i = 0; i < dealTxData.getCancelDealList().size(); i++) {
//                CancelDeal cancelDeal = dealTxData.getCancelDealList().get(i);
//                if (cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
//                    TradingOrderPo orderPo = orderStorageService.query(cancelDeal.getOrderHash());
//                    //Delete order records corresponding to inventory opening
//                    dexManager.removeTradingOrder(orderPo);
//                    //Persistent database
//                    orderStorageService.stop(orderPo);
//                }
//                orderCancelStorageService.save(cancelDeal);
//                LoggerUtil.dexCoinLog.debug("-----deal cancel----");
//                LoggerUtil.dexCoinLog.debug("-----cancel order:" + HexUtil.encode(cancelDeal.getOrderHash()) + ",amount:" + cancelDeal.getCancelAmount() + ", status:" + cancelDeal.getStatus());
//            }

        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to TradingDeal commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to TradingDeal commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    private void save(Transaction tx, TradingContainer container, TradingDealPo dealPo, TradingOrderPo buyOrder, TradingOrderPo sellOrder) throws Exception {
        byte[] nonce = DexUtil.getNonceByHash(tx.getHash());
        buyOrder.setNonce(nonce);
        buyOrder.setDealAmount(buyOrder.getDealAmount().add(dealPo.getBaseAmount()));
        buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().subtract(dealPo.getQuoteAmount()));
        if (dealPo.getType() == DexConstant.ORDER_BUY_OVER || dealPo.getType() == DexConstant.ORDER_ALL_OVER) {
            buyOrder.setOver(true);
        }

        sellOrder.setNonce(nonce);
        sellOrder.setDealAmount(sellOrder.getDealAmount().add(dealPo.getBaseAmount()));
        if (dealPo.getType() == DexConstant.ORDER_SELL_OVER || dealPo.getType() == DexConstant.ORDER_ALL_OVER) {
            sellOrder.setOver(true);
        }
        //Update the trading position first. If the transaction is not fully completed, update the trading position. If the transaction is completed, remove it from the trading position
        if (!buyOrder.isOver()) {
            container.updateTradingOrder(buyOrder);
        } else {
            container.removeTradingOrder(buyOrder);
        }
        if (!sellOrder.isOver()) {
            container.updateTradingOrder(sellOrder);
        } else {
            container.removeTradingOrder(sellOrder);
        }

        //Update the database uniformly again. If the transaction has been fully completed, it needs to be moved from the database to the backup database
        if (!buyOrder.isOver()) {
            orderStorageService.save(buyOrder);
        } else {
            orderStorageService.stop(buyOrder);
        }
        if (!sellOrder.isOver()) {
            orderStorageService.save(sellOrder);
        } else {
            orderStorageService.stop(sellOrder);
        }

        //Store transaction information
        tradingDealStorageService.save(dealPo);
    }

    public void tradingDealRollback(Transaction tx) {
        try {
            boolean isBuyOver = false;
            boolean isSellOver = false;
            TradingDealPo dealPo = tradingDealStorageService.query(tx.getHash());
            //No persistent transaction data found, directly returned
            if (dealPo == null) {
                return;
            }
            //1.Processing Purchase Orders
            //Query persistent payment data
            TradingOrderPo buyOrder = orderStorageService.query(dealPo.getBuyHash().getBytes());
            if (buyOrder == null) {
                //If the entrusted table cannot be queried, go to the historical backup table to query it
                buyOrder = orderStorageService.queryFromBack(dealPo.getBuyHash().getBytes());
                buyOrder.setOver(false);
                isBuyOver = true;
            }
            //If it still cannot be found, it indicates that the delegation order packaged in this block has not been saved, so it will be returned directly without processing
            if (buyOrder == null) {
                return;
            }
            buyOrder.setNonce(dealPo.getBuyNonce());
            buyOrder.setDealAmount(buyOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
            buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().add(dealPo.getQuoteAmount()));

            //2.Processing sales orders
            TradingOrderPo sellOrder = orderStorageService.query(dealPo.getSellHash().getBytes());
            if (sellOrder == null) {
                sellOrder = orderStorageService.queryFromBack(dealPo.getSellHash().getBytes());
                sellOrder.setOver(false);
                isSellOver = true;
            }
            if (sellOrder == null) {
                return;
            }
            sellOrder.setNonce(dealPo.getSellNonce());
            sellOrder.setDealAmount(sellOrder.getDealAmount().subtract(dealPo.getBaseAmount()));

            TradingContainer container = dexManager.getTradingContainer(dealPo.getTradingHash().toHex());
            //Put the order back into the inventory
            if (isBuyOver) {
                container.addTradingOrder(buyOrder);
            } else {
                container.updateTradingOrder(buyOrder);
            }
            if (isSellOver) {
                container.addTradingOrder(sellOrder);
            } else {
                container.updateTradingOrder(sellOrder);
            }

            //Rolling back persistent data
            orderStorageService.deleteBackData(dealPo.getBuyHash().getBytes());
            orderStorageService.save(buyOrder);
            orderStorageService.deleteBackData(dealPo.getSellHash().getBytes());
            orderStorageService.save(sellOrder);
            tradingDealStorageService.delete(dealPo.getTradingHash());
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to DealOrder rollback, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to DealOrder rollback, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

//
//
//    public void tradingDealRollback(Transaction tx) {
//        try {
//            boolean isBuyOver = false;
//            boolean isSellOver = false;
//
//            TradingDealTxData dealTxData = new TradingDealTxData();
//            dealTxData.parse(new NulsByteBuffer(tx.getTxData()));
//            //Rollback processing andcommitThe processing order is completely opposite
//            //1.Reverse the process of canceling the entrusted order first
//            CancelDeal cancelDeal;
//            for (int i = dealTxData.getCancelDealList().size() - 1; i >= 0; i--) {
//                cancelDeal = dealTxData.getCancelDealList().get(i);
//                cancelDeal = orderCancelStorageService.query(cancelDeal.getOrderHash());
//                if (cancelDeal != null && cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
//                    TradingOrderPo orderPo = orderStorageService.queryFromBack(cancelDeal.getOrderHash());
//                    //It is possible that when saving the block, incomplete saving requires a rollback, so the data may not be queried
//                    if (orderPo != null) {
//                        orderStorageService.rollbackStop(orderPo);
//                    }
//                }
//                orderCancelStorageService.delete(cancelDeal.getOrderHash());
//            }
//
//            //2.Reverse processing of transaction orders
//            for (int i = dealTxData.getTradingDealList().size() - 1; i >= 0; i--) {
//                TradingDealPo dealPo = tradingDealStorageService.query(tx.getHash());
//                //No persistent transaction data found, directly returned
//                if (dealPo == null) {
//                    return;
//                }
//                //1.Processing Purchase Orders
//                //Query persistent payment data
//                TradingOrderPo buyOrder = orderStorageService.query(dealPo.getBuyHash().getBytes());
//                if (buyOrder == null) {
//                    //If the entrusted table cannot be queried, go to the historical backup table to query it
//                    buyOrder = orderStorageService.queryFromBack(dealPo.getBuyHash().getBytes());
//                    buyOrder.setOver(false);
//                    isBuyOver = true;
//                }
//                //If it still cannot be found, it indicates that the delegation order packaged in this block has not been saved, so it will be skipped and not processed directly
//                if (buyOrder == null) {
//                    tradingDealStorageService.delete(dealPo.getTradingHash());
//                    continue;
//                }
//                buyOrder.setNonce(dealPo.getBuyNonce());
//                buyOrder.setDealAmount(buyOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
//                buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().add(dealPo.getQuoteAmount()));
//
//                //2.Processing sales orders
//                TradingOrderPo sellOrder = orderStorageService.query(dealPo.getSellHash().getBytes());
//                if (sellOrder == null) {
//                    sellOrder = orderStorageService.queryFromBack(dealPo.getSellHash().getBytes());
//                    sellOrder.setOver(false);
//                    isSellOver = true;
//                }
//                if (sellOrder == null) {
//                    tradingDealStorageService.delete(dealPo.getTradingHash());
//                    return;
//                }
//                sellOrder.setNonce(dealPo.getSellNonce());
//                sellOrder.setDealAmount(sellOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
//
//                TradingContainer container = dexManager.getTradingContainer(dealPo.getTradingHash().toHex());
//                //Put the order back into the inventory
//                if (isBuyOver) {
//                    container.addTradingOrder(buyOrder);
//                } else {
//                    container.updateTradingOrder(buyOrder);
//                }
//                if (isSellOver) {
//                    container.addTradingOrder(sellOrder);
//                } else {
//                    container.updateTradingOrder(sellOrder);
//                }
//
//                //Rolling back persistent data
//                orderStorageService.deleteBackData(dealPo.getBuyHash().getBytes());
//                orderStorageService.save(buyOrder);
//                orderStorageService.deleteBackData(dealPo.getSellHash().getBytes());
//                orderStorageService.save(sellOrder);
//                tradingDealStorageService.delete(dealPo.getTradingHash());
//            }
//
//        } catch (NulsException e) {
//            LoggerUtil.dexLog.error("Failure to DealOrder rollback, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e.getErrorCode());
//        } catch (Exception e) {
//            LoggerUtil.dexLog.error("Failure to DealOrder rollback, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e);
//        }
//    }

    @Override
    public int getPriority() {
        return 3;
    }
}
