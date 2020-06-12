package network.nerve.dex.task;

import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.util.LoggerUtil;

public class DexInfoTask implements Runnable {

    private int chainId;

    private DexManager dexManager;

    public DexInfoTask(int chainId, DexManager dexManager) {
        this.chainId = chainId;
        this.dexManager = dexManager;
    }

    @Override
    public void run() {
        try {
            for (TradingContainer container : dexManager.getAllContainer().values()) {
                CoinTradingPo tradingPo = container.getCoinTrading();

                LoggerUtil.dexInfoLog.info(tradingPo.toString());
                LoggerUtil.dexInfoLog.info("----------sellOrderList---------size:" + container.getSellOrderList().size());
//                for (int i = 0; i < container.getSellOrderList().size(); i++) {
//                    LoggerUtil.dexInfoLog.info(container.getSellOrderList().get(i).toString());
//                }
                LoggerUtil.dexInfoLog.info("----------buyOrderList---------size:" + container.getBuyOrderList().size());
//                for (int i = 0; i < container.getBuyOrderList().size(); i++) {
//                    LoggerUtil.dexInfoLog.info(container.getBuyOrderList().get(i).toString());
//                }
            }
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
        }
    }
}
