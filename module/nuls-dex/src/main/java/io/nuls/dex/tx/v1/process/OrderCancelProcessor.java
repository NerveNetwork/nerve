package io.nuls.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.model.txData.TradingOrderCancel;
import io.nuls.dex.storage.TradingOrderCancelStorageService;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.util.LoggerUtil;

import java.util.*;

/**
 * 取消委托挂单处理器
 */
@Component("OrderCancelProcessorV1")
public class OrderCancelProcessor implements TransactionProcessor {

    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private TradingOrderCancelStorageService orderCancelStorageService;
    @Autowired
    private DexManager dexManager;

    @Override
    public int getType() {
        return TxType.TRADING_ORDER_CANCEL;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        //存放验证不通过的交易
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        //记录挂单的hash做冲突检测
        Set<String> orderHashSet = new HashSet<>();
        Transaction tx;
        TradingOrderCancel orderCancel;
        TradingOrderPo orderPo;
        CoinFrom coinFrom;
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                orderCancel = new TradingOrderCancel();
                orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                //查询需要取消的订单是否存在
                orderPo = orderStorageService.query(orderCancel.getOrderHash());
                if (orderPo == null) {
                    throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
                }
                //验证取消委托和挂单委托是否是同一人发起交易
                coinFrom = tx.getCoinDataInstance().getFrom().get(0);
                if (!Arrays.equals(coinFrom.getAddress(), orderPo.getAddress())) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "account is invalid");
                }
                //冲突检测，查看是否有相同的订单
                String orderHash = HexUtil.encode(orderCancel.getOrderHash());
                if (orderHashSet.contains(orderHash)) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "The order has been cancelled");
                } else {
                    orderHashSet.add(orderHash);
                }
            } catch (NulsException e) {
                LoggerUtil.dexLog.error(e);
                errorCode = e.getErrorCode();
                invalidTxList.add(tx);
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("txList", invalidTxList);
        resultMap.put("errorCode", errorCode);
        return resultMap;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        Transaction tx;
        TradingOrderCancel orderCancel;
        TradingOrderPo orderPo;
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                orderCancel = new TradingOrderCancel();
                orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                orderPo = orderStorageService.query(orderCancel.getOrderHash());

                orderStorageService.stop(orderPo);
//                //根据订单的剩余数量生成取消委托挂单记录
//                cancelPo = new TradingOrderCancelPo();
//                cancelPo.setHash(tx.getHash());
//                cancelPo.setOrderHash(orderPo.getOrderHash());
//                cancelPo.setCancelAmount(orderPo.getAmount().subtract(orderPo.getDealAmount()));
//                orderCancelStorageService.save(cancelPo);

                //删除盘口对应的订单记录
                dexManager.removeTradingOrder(orderPo);
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
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Transaction tx;
        TradingOrderCancel orderCancel;
        TradingOrderPo orderPo;
        for (int i = txs.size() - 1; i >= 0; i--) {
            tx = txs.get(i);
            try {
                orderCancel = new TradingOrderCancel();
                orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                orderPo = orderStorageService.queryFromBack(orderCancel.getOrderHash());
                //有可能是因为保存区块时，未完整保存需要做回滚，因此数据可能会查询不到
                if (orderPo != null) {
                    orderStorageService.rollbackStop(orderPo);
                }
            } catch (NulsException e) {
                LoggerUtil.dexLog.error("Failure to TradingOrderCancel rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to TradingOrderCancel rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }
        return true;
    }

    /**
     * 挂单委托交易放在第1位处理
     *
     * @return
     */
    @Override
    public int getPriority() {
        return 1;
    }
}
