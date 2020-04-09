package io.nuls.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.dex.context.DexConstant;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.TradingOrder;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.tx.v1.validate.TradingOrderValidator;
import io.nuls.dex.util.LoggerUtil;

import java.util.List;
import java.util.Map;

/**
 * 用户挂单委托交易处理器
 */
@Component("TradingOrderProcessorV1")
public class TradingOrderProcessor implements TransactionProcessor {

    @Autowired
    private TradingOrderValidator validator;
    @Autowired
    private TradingOrderStorageService tradingOrderStorageService;
    @Autowired
    private DexManager dexManager;


    @Override
    public int getType() {
        return TxType.TRADING_ORDER;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return validator.validateTxs(txs);
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        TradingOrder order = null;
        Transaction tx;
        CoinTo coinTo;
        long time1, time2;
        time1 = System.currentTimeMillis();
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                order = new TradingOrder();
                order.parse(new NulsByteBuffer(tx.getTxData()));
                coinTo = tx.getCoinDataInstance().getTo().get(0);
                TradingOrderPo orderPo = new TradingOrderPo(tx.getHash(), coinTo.getAddress(), order);
                if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                    orderPo.setLeftQuoteAmount(coinTo.getAmount());
                }
                tradingOrderStorageService.save(orderPo);
                dexManager.addTradingOrder(orderPo);
            } catch (NulsException e) {
                LoggerUtil.dexLog.error("Failure to TradingOrder commit, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to TradingOrder commit, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }
        time2 = System.currentTimeMillis();
        LoggerUtil.dexLog.debug("挂单交易提交用时：" + (time2 - time1) + ",验证数量：" + txs.size());
        return true;
    }

    /**
     * 回滚时，做提交时的相反操作
     * 需要注意的是，可能由于区块提交保存时，未完整保存,
     * 因此数据可能未被保存到数据库或者缓存中，在这里不做数据为空校验
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param blockHeader 区块头
     * @return
     */
    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        TradingOrder order;
        Transaction tx;
        for (int i = txs.size() - 1; i >= 0; i--) {
            tx = txs.get(i);
            try {
                order = new TradingOrder();
                order.parse(new NulsByteBuffer(txs.get(i).getTxData()));
                TradingOrderPo orderPo = new TradingOrderPo(tx.getHash(), null, order);
                tradingOrderStorageService.delete(orderPo.getOrderHash());
                dexManager.removeTradingOrder(orderPo);
            } catch (NulsException e) {
                LoggerUtil.dexLog.error("Failure to TradingOrder rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to TradingOrder rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }

        return true;
    }

    /**
     * 挂单委托交易放在第2位处理
     *
     * @return
     */
    @Override
    public int getPriority() {
        return 2;
    }
}
