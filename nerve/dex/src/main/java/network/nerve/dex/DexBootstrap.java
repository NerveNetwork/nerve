package network.nerve.dex;

import io.nuls.base.data.Address;
import io.nuls.base.protocol.*;
import io.nuls.base.protocol.cmd.TransactionDispatcher;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.context.DexDBConstant;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.ScheduleManager;
import network.nerve.dex.tx.DexTxCommitAdvice;
import network.nerve.dex.tx.DexTxRollbackAdvice;
import network.nerve.dex.util.DexUtil;
import network.nerve.dex.util.LoggerUtil;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class DexBootstrap extends RpcModule {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private ScheduleManager scheduleManager;


    public static void main(String[] args) throws Exception {
        NulsRpcModuleBootstrap.run("io.nuls,network.nerve.dex", args);
    }

    /**
     * Return the dependent modules of this module
     *
     * @return
     */
    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.NC),
        };
    }

    /**
     * Return the description information of the current module
     *
     * @return
     */
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.DX.abbr, "1.0");
    }

    /**
     * Initialize module information,For example, initializationRockDBetc.,After initialization here,Can be found in otherbeanofafterPropertiesSetUsed in
     */
    @Override
    public void init() {
        try {
            super.init();
            initSys();
            initDb();
            initCfg();
            DexContext.sysFeeScaleDecimal = new BigDecimal(dexConfig.getSysFeeScale());
            Address address = new Address(dexConfig.getSysFeeAddress());
            DexContext.sysFeeAddress = address.getAddressBytes();
            DexContext.createTradingAmount = new BigInteger(dexConfig.getCreateTradingAmount());
            ProtocolLoader.load(dexConfig.getChainId());
            ModuleHelper.init(this);
            dexManager.init();
        } catch (Exception e) {
            Log.error("NulsDexBootstrap init error!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize system encoding
     * Initialization System Coding
     */
    private void initSys() throws Exception {
        System.setProperty(DexConstant.SYS_FILE_ENCODING, UTF_8.name());
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, UTF_8);
    }


    /**
     * Initialize database
     * Initialization database
     */
    private void initDb() {
        //Read configuration file,Root directory of data storage,Initialize and open all table connections in this directory and place them in cache
        RocksDBService.init(dexConfig.getDataFolder());
        DexUtil.createTable(DexDBConstant.DB_NAME_COIN_TRADING);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER_BACK);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER_CANCEL);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_DEAL);
        DexUtil.createTable(DexDBConstant.DB_NAME_COIN_TRADING_EDIT_INFO);
        DexUtil.createTable(DexDBConstant.DB_NAME_NONCE_ORDER);
        DexUtil.createTable(DexDBConstant.DB_NAME_HEIGHT);
    }


    private void initCfg() {
        try {
            Map map = JSONUtils.json2map(IoUtils.read(DexConstant.DEX_CONFIG_FILE + dexConfig.getChainId() + ".json"));
            long skipHeight = Long.parseLong(map.get("skipHeight").toString());
            long priceSkipHeight = Long.parseLong(map.get("priceSkipHeight").toString());
            long cancelConfirmSkipHeight = Long.parseLong(map.get("cancelConfirmSkipHeight").toString());
            DexContext.skipHeight = skipHeight;
            DexContext.priceSkipHeight = priceSkipHeight;
            DexContext.cancelConfirmSkipHeight = cancelConfirmSkipHeight;
        } catch (Exception e) {
            DexContext.skipHeight = 0;
            DexContext.priceSkipHeight = 0;
        }
    }

    /**
     * Completedspring initinjection,Start module startup
     *
     * @return If the startup is completed, returntrue, The module will enterreadystate, If startup fails, returnfalse, 10This method will be called again in seconds
     */
    @Override
    public boolean doStart() {
        TransactionDispatcher transactionDispatcher = SpringLiteContext.getBean(TransactionDispatcher.class);
        CommonAdvice commitAdvice = SpringLiteContext.getBean(DexTxCommitAdvice.class);
        CommonAdvice rollbackAdvice = SpringLiteContext.getBean(DexTxRollbackAdvice.class);
        transactionDispatcher.register(commitAdvice, rollbackAdvice);
        return true;
    }

    /**
     * All external dependencies enterreadyThis method will be called after the state is reached,Return after normal startupRunningstate
     *
     * @return
     */
    @Override
    public RpcModuleState onDependenciesReady() {
        //Start time service
        NulsDateUtils.getInstance().start();

        scheduleManager.start();

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RpcModuleState.Running;
    }


    @Override
    public void onDependenciesReady(Module module) {
        if (module.getName().equals(ModuleE.NC.abbr)) {
            //Register with the trading moduleDEXModule transactions
            LoggerUtil.dexLog.info("dexlog info ====== >");
            RegisterHelper.registerTx(dexConfig.getChainId(), ProtocolGroupManager.getCurrentProtocol(dexConfig.getChainId()));
            //Registration account module related transactions
            RegisterHelper.registerProtocol(dexConfig.getChainId());
        }
    }


    /**
     * After the loss of an external dependency connection,Will call this method,Controllable module status,If returningReady,This indicates that the module has degraded toReadystate,After the dependency is re prepared,Will be triggered againonDependenciesReadymethod,If the returned status isRunning,Will not trigger againonDependenciesReady
     *
     * @param dependenciesModule
     * @return
     */
    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Running;
    }

    @Override
    protected long getTryRuningTimeout() {
        return 6000;
    }
}
