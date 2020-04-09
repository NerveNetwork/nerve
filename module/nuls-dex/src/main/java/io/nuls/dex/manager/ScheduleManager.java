package io.nuls.dex.manager;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.task.DexInfoTask;
import io.nuls.dex.task.RefreshAssetInfoTask;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Dex模块定时任务管理器
 */
@Component
public class ScheduleManager {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;

    public void start() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

        executorService.scheduleAtFixedRate(new RefreshAssetInfoTask(dexConfig.getChainId(), dexManager), 5, 60, TimeUnit.SECONDS);

        executorService.scheduleAtFixedRate(new DexInfoTask(dexConfig.getChainId(), dexManager), 10, 20, TimeUnit.SECONDS);
    }
}
