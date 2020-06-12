package network.nerve.dex.manager;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.task.DexInfoTask;
import network.nerve.dex.task.RefreshAssetInfoTask;

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

        executorService.scheduleAtFixedRate(new RefreshAssetInfoTask(dexConfig.getChainId(), dexManager), 5, 30, TimeUnit.SECONDS);

        executorService.scheduleAtFixedRate(new DexInfoTask(dexConfig.getChainId(), dexManager), 10, 30, TimeUnit.SECONDS);
    }
}
