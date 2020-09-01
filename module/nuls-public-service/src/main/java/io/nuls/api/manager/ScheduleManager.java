package io.nuls.api.manager;

import io.nuls.api.ApiContext;
import io.nuls.api.task.*;
import io.nuls.core.core.annotation.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduleManager {

    public void start() {

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(6);
        executorService.scheduleAtFixedRate(new DeleteTxsTask(ApiContext.defaultChainId), 2, 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new QueryChainInfoTask(ApiContext.defaultChainId), 2, 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new SyncBlockTask(ApiContext.defaultChainId), 5, 2, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new StatisticalNulsTask(ApiContext.defaultChainId), 10, 5*60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new StatisticalTask(ApiContext.defaultChainId), 10, 1 * 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new UnConfirmTxTask(ApiContext.defaultChainId), 10, 2 * 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new StatisticalRewardTask(ApiContext.defaultChainId), 10, 60 * 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new GetGlobalInfoTask(ApiContext.defaultChainId), 5, 2, TimeUnit.SECONDS);

        //每2分钟执行一次币种兑USDT价格采集
        executorService.scheduleAtFixedRate(new ActualSymbolUsdtPriceTask(),10,10 * 60,TimeUnit.SECONDS);

        //每半小时执行一次统计数据缓存
        executorService.scheduleAtFixedRate(new ReportTask(),10,1 * 60,TimeUnit.SECONDS);

        //每5分钟更新虚拟银行节点的ETH余额
        executorService.scheduleAtFixedRate(new QueryHeterogeneousChainBalanceTask(),10,5 * 60,TimeUnit.SECONDS);
    }
}
