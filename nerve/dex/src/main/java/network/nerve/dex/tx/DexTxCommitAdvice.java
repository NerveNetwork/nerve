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
import network.nerve.dex.util.LoggerUtil;

import java.util.List;

@Component
public class DexTxCommitAdvice implements CommonAdvice {

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
//        long time1, time2;
//        time1 = System.currentTimeMillis();

        Transaction tx;
        for (int i = 0; i < txList.size(); i++) {
            tx = txList.get(i);
            tx.setBlockHeight(blockHeader.getHeight());

            if (tx.getType() == TxType.COIN_TRADING) {
                coinTradingProcessor.coinTradingCommit(tx);
            } else if (tx.getType() == TxType.TRADING_ORDER) {
                tradingOrderProcessor.tradingOrderCommit(tx, i);
            } else if (tx.getType() == TxType.TRADING_ORDER_CANCEL) {
                orderCancelProcessor.cancelOrderCommit(tx);
            } else if (tx.getType() == TxType.TRADING_DEAL) {
                tradingDealProcessor.tradingDealCommit(tx);
            } else if (tx.getType() == TxType.EDIT_COIN_TRADING) {
                editCoinTradingProcessor.editCoinTradingCommit(tx);
            } else if (tx.getType() == TxType.ORDER_CANCEL_CONFIRM) {
                orderCancelConfirmProcessor.txCommit(tx, blockHeader.getHeight());
            }
        }
        tradingOrderStorageService.saveHeight(blockHeader.getHeight());
//        time2 = System.currentTimeMillis();
//        if (time2 - time1 > 100) {
//            LoggerUtil.dexLog.info("----dex commit---- block height:{}, txCount:{}, use:{} ", blockHeader.getHeight(), txList.size(), (time2 - time1));
//        }
    }
}
