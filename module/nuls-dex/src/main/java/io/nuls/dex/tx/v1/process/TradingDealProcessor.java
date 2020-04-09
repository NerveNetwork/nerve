package io.nuls.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.manager.TradingContainer;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.po.TradingDealPo;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.TradingDeal;
import io.nuls.dex.storage.TradingDealStorageService;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.util.DexUtil;
import io.nuls.dex.util.LoggerUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component("TradingDealProcessorV1")
public class TradingDealProcessor implements TransactionProcessor {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private TradingOrderStorageService tradingOrderStorageService;
    @Autowired
    private TradingDealStorageService tradingDealStorageService;

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
        String hash = null;
        try {
            Transaction tx;
            TradingDeal deal;
            TradingDealPo dealPo;

            long time1, time2;
            time1 = System.currentTimeMillis();

            for (int i = 0; i < txs.size(); i++) {
                tx = txs.get(i);
                deal = new TradingDeal();
                deal.parse(new NulsByteBuffer(tx.getTxData()));
                hash = tx.getHash().toHex();
                //找到对应的挂单
                dealPo = new TradingDealPo(tx.getHash(), deal);
                TradingContainer container = dexManager.getTradingContainer(dealPo.getTradingHash().toHex());
                TradingOrderPo buyOrder = tradingOrderStorageService.query(dealPo.getBuyHash().getBytes());
                TradingOrderPo sellOrder = tradingOrderStorageService.query(dealPo.getSellHash().getBytes());
                if (container == null) {
                    throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "coinTrading not exist");
                }
                CoinTradingPo tradingPo = container.getCoinTrading();
                validate(tx.getCoinDataInstance(), tradingPo, dealPo, buyOrder, sellOrder);
                save(tx, container, dealPo, buyOrder, sellOrder);
            }
            time2 = System.currentTimeMillis();
            LoggerUtil.dexLog.debug("成交交易总用时：" + (time2 - time1) + ",提交数量：" + txs.size() + ",区块高度：" + blockHeader.getHeight());
            LoggerUtil.dexLog.debug("");
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to TradingDeal commit, hash:" + hash);
            LoggerUtil.dexLog.error(e);
            rollback(chainId, txs, blockHeader);
            return false;
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to TradingDeal commit, hash:" + hash);
            LoggerUtil.dexLog.error(e);
            rollback(chainId, txs, blockHeader);
            return false;
        }
        return true;
    }

    private void save(Transaction tx, TradingContainer container, TradingDealPo dealPo, TradingOrderPo buyOrder, TradingOrderPo sellOrder) throws Exception {
        //持久化成交数据
        tradingDealStorageService.save(dealPo);
        buyOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        buyOrder.setDealAmount(buyOrder.getDealAmount().add(dealPo.getBaseAmount()));
        buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().subtract(dealPo.getQuoteAmount()));
        if (dealPo.getType() == DexConstant.ORDER_BUY_OVER || dealPo.getType() == DexConstant.ORDER_ALL_OVER) {
            buyOrder.setOver(true);
        }

        sellOrder.setNonce(DexUtil.getNonceByHash(tx.getHash()));
        sellOrder.setDealAmount(sellOrder.getDealAmount().add(dealPo.getBaseAmount()));
        if (dealPo.getType() == DexConstant.ORDER_SELL_OVER || dealPo.getType() == DexConstant.ORDER_ALL_OVER) {
            sellOrder.setOver(true);
        }
        //如果未完全成交则更新盘口和数据库
        //已完成成交则从盘口移出，并从数据库移至备份库
        if (!buyOrder.isOver()) {
            tradingOrderStorageService.save(buyOrder);
            container.updateTradingOrder(buyOrder);
        } else {
            tradingOrderStorageService.stop(buyOrder);
            container.removeTradingOrder(buyOrder);
        }

        if (!sellOrder.isOver()) {
            tradingOrderStorageService.save(sellOrder);
            container.updateTradingOrder(sellOrder);
        } else {
            tradingOrderStorageService.stop(sellOrder);
            container.removeTradingOrder(sellOrder);
        }
    }

    /**
     * 验证成交交易合法性
     *
     * @param dealPo
     */
    private void validate(CoinData coinData, CoinTradingPo tradingPo, TradingDealPo dealPo, TradingOrderPo buyOrder, TradingOrderPo sellOrder) throws NulsException {
        //验证成交订单是否存在
        if (buyOrder == null || sellOrder == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "trading order not exist");
        }

        //验证交易from的合法性
        CoinFrom buyFrom = coinData.getFrom().get(0);
        if (buyFrom.getAssetsChainId() != tradingPo.getQuoteAssetChainId() ||
                buyFrom.getAssetsId() != tradingPo.getQuoteAssetId() ||
                buyFrom.getAmount().compareTo(dealPo.getQuoteAmount()) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "Tx's froms and tradingOrder information are inconsistent");
        }
        CoinFrom sellFrom = coinData.getFrom().get(1);
        if (sellFrom.getAssetsChainId() != tradingPo.getBaseAssetChainId() ||
                sellFrom.getAssetsId() != tradingPo.getBaseAssetId() ||
                sellFrom.getAmount().compareTo(dealPo.getBaseAmount()) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "Tx's froms and tradingOrder information are inconsistent");
        }
        //根据成交交易的生成规则，组装coinData，与交易的coinData对比

        Map<String, Object> map = DexUtil.createDealTxCoinData(tradingPo, dealPo.getPrice(), buyOrder, sellOrder);

        CoinData coinData1 = (CoinData) map.get("coinData");
        boolean isBuyOver = (boolean) map.get("isBuyOver");
        boolean isSellOver = (boolean) map.get("isSellOver");
        byte type = 0;
        if (isBuyOver && !isSellOver) {
            type = DexConstant.ORDER_BUY_OVER;
        } else if (!isBuyOver && isSellOver) {
            type = DexConstant.ORDER_SELL_OVER;
        } else if (isBuyOver && isSellOver) {
            type = DexConstant.ORDER_ALL_OVER;
        }
        if (type != dealPo.getType()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "deal type error");
        }

        try {
            if (!Arrays.equals(coinData.serialize(), coinData1.serialize())) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "Tx's coinData and tradingDeal information are inconsistent");
            }
        } catch (IOException e) {
            LoggerUtil.dexLog.error(e);
            throw new NulsException(DexErrorCode.DATA_PARSE_ERROR);
        }
    }


    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Transaction tx;
        TradingDealPo dealPo;
        boolean isBuyOver;
        boolean isSellOver;
        for (int i = txs.size() - 1; i >= 0; i--) {
            tx = txs.get(i);
            isBuyOver = false;
            isSellOver = false;
            try {
                dealPo = tradingDealStorageService.query(tx.getHash());
                if (dealPo == null) {
                    continue;
                }
                //持久化成交数据
                tradingDealStorageService.delete(dealPo.getTradingHash());

                CoinData coinData = tx.getCoinDataInstance();

                TradingOrderPo buyOrder = tradingOrderStorageService.query(dealPo.getBuyHash().getBytes());
                if (buyOrder == null) {
                    buyOrder = tradingOrderStorageService.queryFromBack(dealPo.getBuyHash().getBytes());
                    tradingOrderStorageService.delete(dealPo.getBuyHash());
                    buyOrder.setOver(false);
                    isBuyOver = true;
                }
                CoinFrom buyFrom = coinData.getFrom().get(0);
                buyOrder.setNonce(buyFrom.getNonce());
                buyOrder.setDealAmount(buyOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
                buyOrder.setLeftQuoteAmount(buyOrder.getLeftQuoteAmount().add(dealPo.getQuoteAmount()));
                tradingOrderStorageService.save(buyOrder);

                TradingOrderPo sellOrder = tradingOrderStorageService.query(dealPo.getSellHash().getBytes());
                if (sellOrder == null) {
                    sellOrder = tradingOrderStorageService.queryFromBack(dealPo.getSellHash().getBytes());
                    tradingOrderStorageService.delete(dealPo.getSellHash());
                    sellOrder.setOver(false);
                    isSellOver = true;
                }
                CoinFrom sellFrom = coinData.getFrom().get(1);
                sellOrder.setNonce(sellFrom.getNonce());
                sellOrder.setDealAmount(sellOrder.getDealAmount().subtract(dealPo.getBaseAmount()));
                tradingOrderStorageService.save(sellOrder);

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
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to TradingDeal rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
