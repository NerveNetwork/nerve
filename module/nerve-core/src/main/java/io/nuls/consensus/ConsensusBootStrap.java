package io.nuls.consensus;

import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.PocbftConstant;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.enumeration.ConsensusStatus;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;

import java.io.File;

/**
 * 共识模块启动及初始化管理
 * Consensus Module Startup and Initialization Management
 *
 * @author tag
 * 2018/3/4
 */
@Component
public class ConsensusBootStrap implements INerveCoreBootstrap {

    @Autowired
    private NerveCoreConfig consensusConfig;
    @Autowired
    private ChainManager chainManager;

    @Override
    public int order() {
        return 4;
    }

    /**
     * 初始化模块，比如初始化RockDB等，在此处初始化后，可在其他bean的afterPropertiesSet中使用
     * 在onStart前会调用此方法
     */
    private void init() {
        try {
            initDB();
            initProtocolUpdate();
            chainManager.initChain();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    private void initProtocolUpdate() {
        ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);

        try {
            long heightVersion1_19_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_19_0"));
            PocbftConstant.VERSION_1_19_0_HEIGHT = heightVersion1_19_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_19_0", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.CS.abbr, ConsensusConstant.RPC_VERSION);
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();
    }

    @Override
    public void onDependenciesReady() {
        try {
            chainManager.runChain();
            for (Chain chain : chainManager.getChainMap().values()) {
                chain.setConsensusStatus(ConsensusStatus.RUNNING);
            }
            Log.debug("cs onDependenciesReady");
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * 初始化数据库
     * Initialization database
     */
    private void initDB() throws Exception {
        RocksDBService.init(consensusConfig.getDataPath() + File.separator + ModuleE.CS.name);
        RocksDBService.createTable(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD);
        RocksDBService.createTable(ConsensusConstant.DB_NAME_VIRTUAL_AGENT_CHANGE);
    }
}
