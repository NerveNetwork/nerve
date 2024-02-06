package io.nuls.block;

import io.nuls.block.manager.ChainManager;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.thread.BlockSynchronizer;
import io.nuls.block.thread.monitor.*;
import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.nuls.block.constant.Constant.*;

/**
 * Block module startup class
 *
 * @author captain
 * @version 1.0
 * @date 19-3-4 afternoon4:09
 */
@Component
public class BlockBootstrap implements INerveCoreBootstrap {

    @Autowired
    public static NerveCoreConfig blockConfig;
    @Autowired
    private ChainManager chainManager;
    public static boolean started = false;

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();
    }

    /**
     * Return the description information of the current module
     *
     * @return
     */
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.BL.abbr, "1.0");
    }

    /**
     * Initialize module information,For example, initializationRockDBetc.,After initialization here,Can be found in otherbeanofafterPropertiesSetUsed in
     */
    private void init() {
        try {
            initDb();
            chainManager.initChain();
        } catch (Exception e) {
            Log.error("BlockBootstrap init error!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize database
     * Initialization database
     */
    private void initDb() throws Exception {
        //Read configuration file,Root directory of data storage,Initialize and open all table connections in this directory and place them in cache
        RocksDBService.init(blockConfig.getDataPath() + File.separator + ModuleE.BL.name);
        RocksDBService.createTable(CHAIN_LATEST_HEIGHT);
        RocksDBService.createTable(PROTOCOL_CONFIG);
        RocksDBService.createTable(ROLLBACK_HEIGHT);
    }

    private boolean doStart() {
        try {
            //Start Chain
            chainManager.runChain();
        } catch (Exception e) {
            Log.error("block module doStart error!" + e);
            return false;
        }
        Log.info("block module ready");
        return true;
    }

    @Override
    public void onDependenciesReady() {
        Log.info("block onDependenciesReady");
        doStart();
        NulsDateUtils.getInstance().start();
        if (started) {
            List<Integer> chainIds = ContextManager.CHAIN_ID_LIST;
            for (Integer chainId : chainIds) {
                BlockSynchronizer.syn(chainId);
            }
        } else {
            //Enable block synchronization thread
            List<Integer> chainIds = ContextManager.CHAIN_ID_LIST;
            for (Integer chainId : chainIds) {
                BlockSynchronizer.syn(chainId);
            }
            //Enable fork chain processing thread
            /*ScheduledThreadPoolExecutor forkExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("fork-chains-monitor"));
            forkExecutor.scheduleWithFixedDelay(ForkChainsMonitor.getInstance(), 0, blockConfig.getForkChainsMonitorInterval(), TimeUnit.MILLISECONDS);*/
            //Enable orphan chain processing thread
            /*ScheduledThreadPoolExecutor orphanExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("orphan-chains-monitor"));
            orphanExecutor.scheduleWithFixedDelay(OrphanChainsMonitor.getInstance(), 0, blockConfig.getOrphanChainsMonitorInterval(), TimeUnit.MILLISECONDS);*/
            //Enable orphan chain maintenance thread
            /*ScheduledThreadPoolExecutor maintainExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("orphan-chains-maintainer"));
            maintainExecutor.scheduleWithFixedDelay(OrphanChainsMaintainer.getInstance(), 0, blockConfig.getOrphanChainsMaintainerInterval(), TimeUnit.MILLISECONDS);*/
            //Enable database size monitoring thread
            ScheduledThreadPoolExecutor dbSizeExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("storage-size-monitor"));
            dbSizeExecutor.scheduleWithFixedDelay(StorageSizeMonitor.getInstance(), 0, blockConfig.getStorageSizeMonitorInterval(), TimeUnit.MILLISECONDS);
            //Enable block monitoring thread
            ScheduledThreadPoolExecutor monitorExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("network-monitor"));
            monitorExecutor.scheduleWithFixedDelay(NetworkResetMonitor.getInstance(), 0, blockConfig.getNetworkResetMonitorInterval(), TimeUnit.MILLISECONDS);
            //Start the transaction group acquisition thread
            ScheduledThreadPoolExecutor txGroupExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("txGroup-requestor"));
            txGroupExecutor.scheduleWithFixedDelay(TxGroupRequestor.getInstance(), 0, blockConfig.getTxGroupRequestorInterval(), TimeUnit.MILLISECONDS);
            //Enable node count monitoring thread
            ScheduledThreadPoolExecutor nodesExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("nodes-monitor"));
            nodesExecutor.scheduleWithFixedDelay(NodesMonitor.getInstance(), 0, blockConfig.getNodesMonitorInterval(), TimeUnit.MILLISECONDS);
            //openbztCleaning of cached data
            ScheduledThreadPoolExecutor bztClearExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("blockBZTClear-monitor"));
            bztClearExecutor.scheduleWithFixedDelay(BlockBZTClearMonitor.getInstance(), 0, blockConfig.getBlockBZTClearMonitorInterval(), TimeUnit.MILLISECONDS);

            ScheduledThreadPoolExecutor stopingExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("stoping-check"));
            stopingExecutor.scheduleWithFixedDelay(StopingMonitor.getInstance(), 0,2, TimeUnit.SECONDS);

            started = true;
        }
    }

}
