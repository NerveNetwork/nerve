package network.nerve.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.TradingOrder;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.tx.v1.validate.TradingOrderValidator;
import network.nerve.dex.util.LoggerUtil;

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
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

    /**
     * 提交挂单委托
     *
     * @param tx
     * @param index
     */
    public void tradingOrderCommit(Transaction tx, int index) {
        try {
            TradingOrder order = new TradingOrder();
            order.parse(new NulsByteBuffer(tx.getTxData()));
            CoinTo coinTo = tx.getCoinDataInstance().getTo().get(0);

            TradingOrderPo orderPo = new TradingOrderPo(tx, index, coinTo.getAddress(), order);
            if (orderPo.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
                orderPo.setLeftQuoteAmount(coinTo.getAmount());
            }

            tradingOrderStorageService.save(orderPo);
            dexManager.addTradingOrder(orderPo);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to TradingOrder commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to TradingOrder commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    public void tradingOrderRollback(Transaction tx, int index) {
        try {
            TradingOrder order = new TradingOrder();
            order.parse(new NulsByteBuffer(tx.getTxData()));
            TradingOrderPo orderPo = new TradingOrderPo(tx, index, null, order);
            if(orderPo != null) {
                tradingOrderStorageService.delete(orderPo.getOrderHash());
                dexManager.removeTradingOrder(orderPo);
            }
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to TradingOrder rollback, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to TradingOrder rollback, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
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
