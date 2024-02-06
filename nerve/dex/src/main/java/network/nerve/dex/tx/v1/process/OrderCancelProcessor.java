package network.nerve.dex.tx.v1.process;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.storage.TradingOrderCancelStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.tx.v1.validate.OrderCancelValidator;
import network.nerve.dex.util.LoggerUtil;

import java.util.*;

/**
 * Cancel delegated order processing
 */
@Component("OrderCancelProcessorV1")
public class OrderCancelProcessor implements TransactionProcessor {

    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private TradingOrderCancelStorageService orderCancelStorageService;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private OrderCancelValidator orderCancelValidator;

    @Override
    public int getType() {
        return TxType.TRADING_ORDER_CANCEL;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return orderCancelValidator.validateTxs(txs);
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
     * Submit cancellation of entrusted order transaction
     *
     * @param tx
     */
    public void cancelOrderCommit(Transaction tx) {
//        try {
//            TradingOrderCancel orderCancel = new TradingOrderCancel();
//            orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
//
//            TradingOrderPo orderPo = orderStorageService.query(orderCancel.getOrderHash());
//            orderStorageService.stop(orderPo);
//            //Delete order records corresponding to inventory opening
//            dexManager.removeTradingOrder(orderPo);
//        } catch (NulsException e) {
//            LoggerUtil.dexLog.error("Failure to TradingOrderCancel commit, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e.getErrorCode());
//        } catch (Exception e) {
//            LoggerUtil.dexLog.error("Failure to TradingOrderCancel commit, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e);
//        }
    }

    public void cancelOrderRollback(Transaction tx) {
//        try {
//            TradingOrderCancel orderCancel = new TradingOrderCancel();
//            orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
//            TradingOrderPo orderPo = orderStorageService.queryFromBack(orderCancel.getOrderHash());
//            //It is possible that when saving the block, incomplete saving requires a rollback, so the data may not be queried
//            if (orderPo != null) {
//                orderStorageService.rollbackStop(orderPo);
//            }
//        } catch (NulsException e) {
//            LoggerUtil.dexLog.error("Failure to TradingOrderCancel rollback, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e.getErrorCode());
//        } catch (Exception e) {
//            LoggerUtil.dexLog.error("Failure to TradingOrderCancel rollback, hash:" + tx.getHash().toHex());
//            LoggerUtil.dexLog.error(e);
//            throw new NulsRuntimeException(e);
//        }
    }

    /**
     * Place the entrusted transaction on the order placement page1Bit processing
     *
     * @return
     */
    @Override
    public int getPriority() {
        return 1;
    }
}
