package io.nuls;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.account.util.LoggerUtil;
import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.common.ConfigManager;
import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.InvokeBean;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.transaction.constant.TxConstant;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static io.nuls.transaction.utils.LoggerUtil.LOG;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: qinyifeng
 * @date: 2018/10/15
 */
@Component
public class NerveCoreBootstrap extends RpcModule {

    private static String[] args;
    @Autowired
    private NerveCoreConfig config;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ConfigManager configManager;

    public static void main(String[] args) throws Exception {
        initSys();
        NerveCoreBootstrap.args = args;
        NulsRpcModuleBootstrap.run("io.nuls", args);
    }

    /**
     * 返回此模块的依赖模块
     * 可写作 return new Module[]{new Module(ModuleE.LG.abbr, "1.0"),new Module(ModuleE.TX.abbr, "1.0")}
     *
     * @return
     */
    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.NC)
        };
    }

    /**
     * 返回当前模块的描述信息
     *
     * @return
     */
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.NC.abbr, "1.0");
    }


    /**
     * 初始化模块信息，比如初始化RockDB等，在此处初始化后，可在其他bean的afterPropertiesSet中使用
     */
    @Override
    public void init() {
        try {
            super.init();
            initDB();
            //初始化配置项
            initCfg();
            ModuleHelper.init(this);
        } catch (Exception e) {
            LoggerUtil.LOG.error("AccountBootsrap init error!");
            throw new RuntimeException(e);
        }
    }

    private void initDB() {
        try {
            //数据文件存储地址
            RocksDBService.init(getTxDataRoot());
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private String getTxDataRoot() {
        return config.getDataPath() + File.separator + ModuleE.TX.name;
    }

    /**
     * 已完成spring init注入，开始启动模块
     *
     * @return 如果启动完成返回true，模块将进入ready状态，若启动失败返回false，10秒后会再次调用此方法
     */
    @Override
    public boolean doStart() {
        try {
            configManager.init();
            NulsDateUtils.getInstance().start();
            LoggerUtil.LOG.info("Nerve-Core onDependenciesReady");
            LoggerUtil.LOG.info("START-SUCCESS");
            try {
                Collection<Object> list = SpringLiteContext.getAllBeanList();
                List<INerveCoreBootstrap> coreList = new ArrayList<>();
                for (Object object : list) {
                    if(object instanceof INerveCoreBootstrap) {
                        coreList.add((INerveCoreBootstrap) object);
                    }
                }
                // 按指定顺序执行异构链注册
                coreList.sort(new Comparator<INerveCoreBootstrap>() {
                    @Override
                    public int compare(INerveCoreBootstrap o1, INerveCoreBootstrap o2) {
                        if (o1.order() > o2.order()) {
                            return 1;
                        } else if (o1.order() < o2.order()) {
                            return -1;
                        }
                        return 0;
                    }
                });
                initCores(coreList);
                configManager.registerProtocol();
                runCores(coreList);
            } catch (Exception e) {
                LOG.error(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public void onDependenciesReady(Module module) {
    }

    /**
     * 所有外部依赖进入ready状态后会调用此方法，正常启动后返回Running状态
     *
     * @return
     */
    @Override
    public RpcModuleState onDependenciesReady() {
        return RpcModuleState.Running;
    }

    /**
     * 某个外部依赖连接丢失后，会调用此方法，可控制模块状态，如果返回Ready,则表明模块退化到Ready状态，当依赖重新准备完毕后，将重新触发onDependenciesReady方法，若返回的状态是Running，将不会重新触发onDependenciesReady
     *
     * @param module
     * @return
     */
    @Override
    public RpcModuleState onDependenciesLoss(Module module) {
        return RpcModuleState.Ready;
    }

    @Override
    protected long getTryRuningTimeout() {
        return 60000;
    }

    private void initCfg() {
        try {
            Provider.ProviderType providerType = Provider.ProviderType.RPC;
            ServiceManager.init(config.getChainId(), providerType);
            /**
             * 地址工具初始化
             */
            AddressTool.init(addressPrefixDatas);
            AddressTool.addPrefix(config.getChainId(), config.getAddressPrefix());

            // 核心模块cmd集合
            List<BaseCmd> cmdList = SpringLiteContext.getBeanList(BaseCmd.class);
            for (BaseCmd cmd : cmdList) {
                Class<?> clazs = cmd.getClass();
                NerveCoreCmd nerveCoreCmd = clazs.getAnnotation(NerveCoreCmd.class);
                if (nerveCoreCmd == null) {
                    continue;
                }
                ModuleE module = nerveCoreCmd.module();
                String moduleAbbr = module.abbr;
                Method[] methods = clazs.getMethods();
                for (Method method : methods) {
                    CmdAnnotation annotation = method.getAnnotation(CmdAnnotation.class);
                    if (annotation == null) {
                        continue;
                    }
                    String cmdName = annotation.cmd();
                    //System.out.println(String.format(
                    //        "moduleName: %s, cmd: %s, class instance: %s, method: %s",
                    //        moduleAbbr, cmdName, cmd.getClass().getName(), method.getName()
                    //));
                    ResponseMessageProcessor.INVOKE_BEAN_MAP.put(moduleAbbr + "_" + cmdName, new InvokeBean(cmd, method));
                }
            }
            // 配置合并模块前，每个模块下的交易
            Object[][] txTypeModules = new Object[][] {
                    new Object[]{ModuleE.AC.abbr, new int[]{2, 3, 78, 79}},
                    new Object[]{ModuleE.BL.abbr, new int[]{}},
                    new Object[]{ModuleE.CS.abbr, new int[]{1, 4, 5, 6, 7, 8, 9, 28, 29, 32, 33}},
                    new Object[]{ModuleE.CC.abbr, new int[]{10, 24, 25, 26, 60, 76}},
                    new Object[]{ModuleE.LG.abbr, new int[]{27}},
                    new Object[]{ModuleE.NW.abbr, new int[]{}},
                    new Object[]{ModuleE.PU.abbr, new int[]{}},
                    new Object[]{ModuleE.TX.abbr, new int[]{}}
            };
            for (Object[] txTypeModule : txTypeModules) {
                String moduleAbbr = (String) txTypeModule[0];
                int[] txTypes = (int[]) txTypeModule[1];
                if (txTypes.length == 0) {
                    continue;
                }
                for (int txType : txTypes) {
                    ResponseMessageProcessor.TX_TYPE_MODULE_MAP.put(txType, moduleAbbr);
                }
            }

        } catch (Exception e) {
            LoggerUtil.LOG.error("NerveCore Bootstrap initCfg failed :{}", e.getMessage(), e);
            throw new RuntimeException("NerveCore Bootstrap initCfg failed");
        }
    }

    /**
     * 初始化系统编码
     */
    private static void initSys() throws Exception {
        try {
            Class.forName("io.nuls.core.rpc.netty.processor.ResponseMessageProcessor");
            ConfigurationLoader configurationLoader = new ConfigurationLoader();
            configurationLoader.load();
            int defaultChainId = Integer.parseInt(configurationLoader.getValue("chainId"));
            ServiceManager.init(defaultChainId, Provider.ProviderType.RPC);
            System.setProperty(TxConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(TxConstant.SYS_FILE_ENCODING, UTF_8.name());
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, UTF_8);
            ObjectMapper objectMapper = JSONUtils.getInstance();
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } catch (Exception e) {
            Log.error(e);
            throw e;
        }
    }

    private void initCores(List<INerveCoreBootstrap> coreList) {
        for (INerveCoreBootstrap core : coreList) {
            LOG.info("Nerve core module [{}] init", core.moduleInfo().getName());
            core.mainFunction(args);
        }
    }

    private void runCores(List<INerveCoreBootstrap> coreList) {
        for (INerveCoreBootstrap core : coreList) {
            LOG.info("Nerve core module ready [{}]", core.moduleInfo().getName());
            core.onDependenciesReady();
        }
    }

}
