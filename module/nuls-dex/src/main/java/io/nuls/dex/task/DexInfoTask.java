package io.nuls.dex.task;

import io.nuls.dex.manager.DexManager;
import io.nuls.dex.manager.TradingContainer;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.util.LoggerUtil;

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
                System.out.println(tradingPo);
                System.out.println("----------sellOrderList---------");
                System.out.println(container.getSellOrderList().size());
//                for (int i = 0; i < container.getSellOrderList().size(); i++) {
//                    System.out.println(container.getSellOrderList().get(i));
//                }
                System.out.println("----------buyOrderList---------");
                System.out.println(container.getBuyOrderList().size());
//                for (int i = 0; i < container.getBuyOrderList().size(); i++) {
//                    System.out.println(container.getBuyOrderList().get(i));
//                }
            }
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
        }
    }
}
