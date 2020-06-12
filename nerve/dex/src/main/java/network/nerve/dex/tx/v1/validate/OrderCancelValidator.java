package network.nerve.dex.tx.v1.validate;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.TradingOrderCancel;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.util.*;

@Component
public class OrderCancelValidator {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;

    public Map<String, Object> validateTxs(List<Transaction> txs) {
        //存放验证不通过的交易
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
//        //记录挂单的hash做冲突检测
//        Set<String> orderHashSet = new HashSet<>();
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
//                if(orderPo == null) {
//                    orderPo = orderStorageService.queryFromBack(orderCancel.getOrderHash());
//                }
//
//                if (orderPo == null) {
//                    LoggerUtil.dexLog.error("----validate cancel order tx count:" + txs.size());
//                    LoggerUtil.dexLog.error("-------The order not found ---, height:" + tx.getBlockHeight() + "txHash:" + tx.getHash().toHex() + ", cancelOrderHash:" + HexUtil.encode(orderCancel.getOrderHash()));
//                    throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
//                }
                if(orderPo != null) {
                    if (tx.getCoinDataInstance().getFrom().size() != 1) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                    //验证取消委托和挂单委托是否是同一人发起交易
                    coinFrom = tx.getCoinDataInstance().getFrom().get(0);
                    if (!Arrays.equals(coinFrom.getAddress(), orderPo.getAddress())) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                    if (coinFrom.getAssetsChainId() != dexConfig.getChainId() || coinFrom.getAssetsId() != dexConfig.getAssetId() || coinFrom.getLocked() == -1) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                }

//                //冲突检测，查看是否有相同的订单
//                String orderHash = HexUtil.encode(orderCancel.getOrderHash());
//                if (orderHashSet.contains(orderHash)) {
//                    LoggerUtil.dexLog.error("-------The order has been cancelled---, height:" + tx.getBlockHeight() + "txHash:" + tx.getHash().toHex() + ", cancelOrderHash:" + orderHash);
//                    throw new NulsException(DexErrorCode.DATA_ERROR, "The order has been cancelled");
//                } else {
//                    orderHashSet.add(orderHash);
//                }
            } catch (NulsException e) {
                LoggerUtil.dexLog.error(e);
                errorCode = e.getErrorCode();
                invalidTxList.add(tx);
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("txList", invalidTxList);
        resultMap.put("errorCode", errorCode == null ? null : errorCode.getCode());
        return resultMap;
    }

}
