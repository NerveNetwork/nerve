package network.nerve.dex.tx;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.CommonAdvice;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.storage.TradingDealStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.tx.v1.process.*;

import java.util.List;

@Component
public class DexTxRollbackAdvice implements CommonAdvice {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private CoinTradingStorageService coinTradingStorageService;
    @Autowired
    private TradingOrderStorageService tradingOrderStorageService;
    @Autowired
    private TradingDealStorageService tradingDealStorageService;
    @Autowired
    private CoinTradingProcessor coinTradingProcessor;
    @Autowired
    private TradingOrderProcessor tradingOrderProcessor;
    @Autowired
    private OrderCancelProcessor orderCancelProcessor;
    @Autowired
    private TradingDealProcessor tradingDealProcessor;
    @Autowired
    private EditCoinTradingProcessor editCoinTradingProcessor;
    @Autowired
    private OrderCancelConfirmProcessor orderCancelConfirmProcessor;

    @Override
    public void begin(int chainId, List<Transaction> txList, BlockHeader blockHeader, int syncStatus) {
        if (txList.isEmpty()) {
            return;
        }
        Transaction tx;
        for (int i = txList.size() - 1; i >= 0; i--) {
            tx = txList.get(i);
            tx.setBlockHeight(blockHeader.getHeight());
            if (tx.getType() == TxType.COIN_TRADING) {
                coinTradingProcessor.coinTradingRollback(tx);
            } else if (tx.getType() == TxType.TRADING_ORDER) {
                tradingOrderProcessor.tradingOrderRollback(tx, i);
            } else if (tx.getType() == TxType.TRADING_ORDER_CANCEL) {
                orderCancelProcessor.cancelOrderRollback(tx);
            } else if (tx.getType() == TxType.TRADING_DEAL) {
                tradingDealProcessor.tradingDealRollback(tx);
            } else if (tx.getType() == TxType.EDIT_COIN_TRADING) {
                editCoinTradingProcessor.editCoinTradingRollback(tx);
            } else if (tx.getType() == TxType.ORDER_CANCEL_CONFIRM) {
                orderCancelConfirmProcessor.txRollback(tx);
            }
        }
    }
}
