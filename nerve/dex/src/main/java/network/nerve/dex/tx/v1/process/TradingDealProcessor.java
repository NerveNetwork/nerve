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
     * 系统交易不单独验证，提交时一并验证
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
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
            //找到对应的买单和卖单
            TradingContainer container = dexManager.getTradingContainer(dealPo.getTradingHash().toHex());
            TradingOrderPo buyOrder = orderStorageService.query(deal.getBuyHash());
            TradingOrderPo sellOrder = orderStorageService.query(deal.getSellHash());

            save(tx, container, dealPo, buyOrder, sellOrder);

//            for (int i = 0; i < dealTxData.getCancelDealList().size(); i++) {
//                CancelDeal cancelDeal = dealTxData.getCancelDealList().get(i);
//                if (cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
//                    TradingOrderPo orderPo = orderStorageService.query(cancelDeal.getOrderHash());
//                    //删除盘口对应的订单记录
//                    dexManager.removeTradingOrder(orderPo);
//                    //持久化数据库
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
        //先更新盘口，如果未完全成交则更新盘口，已完成成交则从盘口移出
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

        //再统一更新数据库，如果交易已完全成交，需要从数据库移至备份库
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

        //存储成交信息
        tradingDealStorageService.save(dealPo);
    }

    public void tradingDealRollback(Transaction tx) {
        try {
            boolean isBuyOver = false;
            boolean isSellOver = false;
            TradingDealPo dealPo = tradingDealStorageService.query(tx.getHash());
            //未查询到持久化的成交数据直接返回
            if (dealPo == null) {
                return;
            }
            //1.处理买单
            //查询持久化的买单数据
            TradingOrderPo buyOrder = orderStorageService.query(dealPo.getBuyHash().getBytes());
            if (buyOrder == null) {
                //委托单表查询不到，就到历史备份表里去查询
                buyOrder = orderStorageService.queryFromBack(dealPo.getBuyHash().getBytes());
                buyOrder.setOver(false);
                isBuyOver = true;
            }
            //如果还是查询不到，说明就是本区块打包的委托单，并没有被保存，这样就直接返回，不做处理
            if (buyOrder == null) {
                return;
            }
            buyOrder.setNonce(dealPo.getBuyNonce());
            buyOrder.setDealAmount(buyOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
            buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().add(dealPo.getQuoteAmount()));

            //2.处理卖单
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
            //将挂单重新放回盘口
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

            //回滚持久化数据
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
//            //回滚的处理和commit的处理顺序完全相反
//            //1.先反向处理取消委托挂单
//            CancelDeal cancelDeal;
//            for (int i = dealTxData.getCancelDealList().size() - 1; i >= 0; i--) {
//                cancelDeal = dealTxData.getCancelDealList().get(i);
//                cancelDeal = orderCancelStorageService.query(cancelDeal.getOrderHash());
//                if (cancelDeal != null && cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
//                    TradingOrderPo orderPo = orderStorageService.queryFromBack(cancelDeal.getOrderHash());
//                    //有可能是因为保存区块时，未完整保存需要做回滚，因此数据可能会查询不到
//                    if (orderPo != null) {
//                        orderStorageService.rollbackStop(orderPo);
//                    }
//                }
//                orderCancelStorageService.delete(cancelDeal.getOrderHash());
//            }
//
//            //2.再反向处理成交单
//            for (int i = dealTxData.getTradingDealList().size() - 1; i >= 0; i--) {
//                TradingDealPo dealPo = tradingDealStorageService.query(tx.getHash());
//                //未查询到持久化的成交数据直接返回
//                if (dealPo == null) {
//                    return;
//                }
//                //1.处理买单
//                //查询持久化的买单数据
//                TradingOrderPo buyOrder = orderStorageService.query(dealPo.getBuyHash().getBytes());
//                if (buyOrder == null) {
//                    //委托单表查询不到，就到历史备份表里去查询
//                    buyOrder = orderStorageService.queryFromBack(dealPo.getBuyHash().getBytes());
//                    buyOrder.setOver(false);
//                    isBuyOver = true;
//                }
//                //如果还是查询不到，说明就是本区块打包的委托单，并没有被保存，这样就直接跳过不做处理
//                if (buyOrder == null) {
//                    tradingDealStorageService.delete(dealPo.getTradingHash());
//                    continue;
//                }
//                buyOrder.setNonce(dealPo.getBuyNonce());
//                buyOrder.setDealAmount(buyOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
//                buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().add(dealPo.getQuoteAmount()));
//
//                //2.处理卖单
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
//                //将挂单重新放回盘口
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
//                //回滚持久化数据
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
