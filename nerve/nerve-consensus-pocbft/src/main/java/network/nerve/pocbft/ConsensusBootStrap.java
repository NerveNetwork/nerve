package network.nerve.pocbft;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.model.bo.config.ConsensusChainConfig;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.utils.enumeration.ConsensusStatus;
import network.nerve.pocbft.utils.manager.ChainManager;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 共识模块启动及初始化管理
 * Consensus Module Startup and Initialization Management
 *
 * @author tag
 * 2018/3/4
 */
@Component
public class ConsensusBootStrap extends RpcModule {

    @Autowired
    private ConsensusChainConfig consensusConfig;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;

    public static void main(String[] args) {
        NulsRpcModuleBootstrap.run(ConsensusConstant.BOOT_PATH, args);
    }

    /**
     * 初始化模块，比如初始化RockDB等，在此处初始化后，可在其他bean的afterPropertiesSet中使用
     * 在onStart前会调用此方法n
     */
    @Override
    public void init() {
        try {
            initSys();
            AddressTool.init(addressPrefixDatas);
            initDB();
            chainManager.initChain();
            ModuleHelper.init(this);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.BL),
                Module.build(ModuleE.AC),
                Module.build(ModuleE.NW),
                Module.build(ModuleE.LG),
                Module.build(ModuleE.TX),
                new Module(ModuleE.PU.abbr, ROLE),
                new Module(ModuleE.QU.abbr, ROLE)
        };
    }

    /**
     * 指定RpcCmd的包名
     * 可以不实现此方法，若不实现将使用spring init扫描的包
     *
     * @return
     */
    @Override
    public Set<String> getRpcCmdPackage() {
        return Set.of(ConsensusConstant.RPC_PATH);
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.CS.abbr, ConsensusConstant.RPC_VERSION);
    }

    @Override
    public boolean doStart() {
        try {
            while (!isDependencieReady(ModuleE.TX.abbr) || !isDependencieReady(ModuleE.BL.abbr) || !isDependencieReady(ModuleE.NW.abbr)) {
                Log.debug("wait depend modules ready");
                Thread.sleep(2000L);
            }
            chainManager.runChain();
            return true;
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    @Override
    public void onDependenciesReady(Module module) {
        try {
            //共识交易注册
            if (module.getName().equals(ModuleE.TX.abbr)) {
                chainManager.registerTx();
            }
            //网络转发消息注册
            if (ModuleE.NW.abbr.equals(module.getName())) {
                RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
            }
            //协议注册
            if (module.getName().equals(ModuleE.PU.abbr)) {
                chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        for (Chain chain : chainManager.getChainMap().values()) {
            chain.setConsensusStatus(ConsensusStatus.RUNNING);
            //chainManager.initConsensusNet(chain);
        }
        Log.debug("cs onDependenciesReady");
        NulsDateUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        if (dependenciesModule.getName().equals(ModuleE.TX.abbr) || dependenciesModule.getName().equals(ModuleE.BL.abbr)) {
            for (Chain chain : chainManager.getChainMap().values()) {
                chain.setConsensusStatus(ConsensusStatus.WAIT_RUNNING);
            }
        }
        return RpcModuleState.Ready;
    }

    /**
     * 初始化系统编码
     * Initialization System Coding
     */
    private void initSys() throws Exception {
        System.setProperty(ConsensusConstant.SYS_FILE_ENCODING, UTF_8.name());
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, UTF_8);
    }

    /**
     * 初始化数据库
     * Initialization database
     */
    private void initDB() throws Exception {
        RocksDBService.init(consensusConfig.getDataFolder());
        RocksDBService.createTable(ConsensusConstant.DB_NAME_CONFIG);
        RocksDBService.createTable(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD);
        RocksDBService.createTable(ConsensusConstant.DB_NAME_VIRTUAL_AGENT_CHANGE);
    }
}
