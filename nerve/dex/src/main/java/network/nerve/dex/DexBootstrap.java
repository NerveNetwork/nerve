package network.nerve.dex;

import io.nuls.base.data.Address;
import io.nuls.base.protocol.*;
import io.nuls.base.protocol.cmd.TransactionDispatcher;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
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
     * 返回此模块的依赖模块
     *
     * @return
     */
    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.TX),
                Module.build(ModuleE.LG),
                Module.build(ModuleE.CC),
                Module.build(ModuleE.AC),
                new Module(ModuleE.PU.abbr, ROLE)
        };
    }

    /**
     * 返回当前模块的描述信息
     *
     * @return
     */
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.DX.abbr, "1.0");
    }

    /**
     * 初始化模块信息,比如初始化RockDB等,在此处初始化后,可在其他bean的afterPropertiesSet中使用
     */
    @Override
    public void init() {
        try {
            super.init();
            initSys();
            initDb();
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
     * 初始化系统编码
     * Initialization System Coding
     */
    private void initSys() throws Exception {
        System.setProperty(DexConstant.SYS_FILE_ENCODING, UTF_8.name());
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, UTF_8);
    }


    /**
     * 初始化数据库
     * Initialization database
     */
    private void initDb() {
        //读取配置文件,数据存储根目录,初始化打开该目录下所有表连接并放入缓存
        RocksDBService.init(dexConfig.getDataFolder());
        DexUtil.createTable(DexDBConstant.DB_NAME_COIN_TRADING);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER_BACK);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_ORDER_CANCEL);
        DexUtil.createTable(DexDBConstant.DB_NAME_TRADING_DEAL);
        DexUtil.createTable(DexDBConstant.DB_NAME_COIN_TRADING_EDIT_INFO);
        DexUtil.createTable(DexDBConstant.DB_NAME_NONCE_ORDER);
    }

    /**
     * 已完成spring init注入,开始启动模块
     *
     * @return 如果启动完成返回true, 模块将进入ready状态, 若启动失败返回false, 10秒后会再次调用此方法
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
     * 所有外部依赖进入ready状态后会调用此方法,正常启动后返回Running状态
     *
     * @return
     */
    @Override
    public RpcModuleState onDependenciesReady() {
        //启动时间服务
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
        //向交易模块注册DEX模块交易
        LoggerUtil.dexLog.info("dexlog info ====== >");
        if (module.getName().equals(ModuleE.TX.abbr)) {
            boolean b = RegisterHelper.registerTx(dexConfig.getChainId(), ProtocolGroupManager.getCurrentProtocol(dexConfig.getChainId()));
        } else if (ModuleE.PU.abbr.equals(module.getName())) {
            //注册账户模块相关交易
            RegisterHelper.registerProtocol(dexConfig.getChainId());
        }
    }


    /**
     * 某个外部依赖连接丢失后,会调用此方法,可控制模块状态,如果返回Ready,则表明模块退化到Ready状态,当依赖重新准备完毕后,将重新触发onDependenciesReady方法,若返回的状态是Running,将不会重新触发onDependenciesReady
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
