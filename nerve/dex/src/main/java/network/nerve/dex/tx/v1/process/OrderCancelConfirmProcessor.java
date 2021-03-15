package network.nerve.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.CancelDeal;
import network.nerve.dex.model.txData.TradingCancelTxData;
import network.nerve.dex.storage.NonceOrderStorageService;
import network.nerve.dex.storage.TradingDealStorageService;
import network.nerve.dex.storage.TradingOrderCancelStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.util.List;
import java.util.Map;

@Component("OrderCancelConfirmProcessorV1")
public class OrderCancelConfirmProcessor implements TransactionProcessor {
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
        return TxType.ORDER_CANCEL_CONFIRM;
    }

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

    public void txCommit(Transaction tx, long blockHeight) {
        try {
            TradingCancelTxData txData = new TradingCancelTxData();
            txData.parse(new NulsByteBuffer(tx.getTxData()));
            for (int i = 0; i < txData.getCancelDealList().size(); i++) {
                CancelDeal cancelDeal = txData.getCancelDealList().get(i);
                if (cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
                    TradingOrderPo orderPo = orderStorageService.query(cancelDeal.getOrderHash());
                    if (blockHeight > DexContext.cancelConfirmSkipHeight) {
                        if (orderPo != null) {
                            //删除盘口对应的订单记录
                            dexManager.removeTradingOrder(orderPo);
                            //持久化数据库
                            orderStorageService.stop(orderPo);
                        }
                    } else {
                        //删除盘口对应的订单记录
                        dexManager.removeTradingOrder(orderPo);
                        //持久化数据库
                        orderStorageService.stop(orderPo);
                    }
                }
                orderCancelStorageService.save(cancelDeal);
            }
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to cancelConfirm commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to cancelConfirm commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    public void txRollback(Transaction tx) {
        try {
            TradingCancelTxData txData = new TradingCancelTxData();
            txData.parse(new NulsByteBuffer(tx.getTxData()));

            //回滚的处理和commit的处理顺序完全相反
            //1.先反向处理取消委托挂单
            CancelDeal cancelDeal;
            for (int i = txData.getCancelDealList().size() - 1; i >= 0; i--) {
                cancelDeal = txData.getCancelDealList().get(i);
                cancelDeal = orderCancelStorageService.query(cancelDeal.getOrderHash());
                if (cancelDeal != null && cancelDeal.getStatus() == DexConstant.CANCEL_ORDER_SUCC) {
                    TradingOrderPo orderPo = orderStorageService.queryFromBack(cancelDeal.getOrderHash());
                    //有可能是因为保存区块时，未完整保存需要做回滚，因此数据可能会查询不到
                    if (orderPo != null) {
                        orderStorageService.rollbackStop(orderPo);
                    }
                }
                orderCancelStorageService.delete(cancelDeal.getOrderHash());
            }
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to cancelConfirm commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to cancelConfirm commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }
}
