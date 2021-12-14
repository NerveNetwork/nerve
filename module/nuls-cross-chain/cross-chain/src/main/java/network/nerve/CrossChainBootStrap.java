package network.nerve;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.crosschain.base.BaseCrossChainBootStrap;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import network.nerve.constant.NulsCrossChainConfig;
import network.nerve.constant.NulsCrossChainConstant;
import network.nerve.model.bo.Chain;
import network.nerve.rpc.call.AccountCall;
import network.nerve.rpc.call.ChainManagerCall;
import network.nerve.rpc.call.NetWorkCall;
import network.nerve.srorage.RegisteredCrossChainService;
import network.nerve.utils.manager.ChainManager;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashSet;

import static network.nerve.constant.NulsCrossChainConstant.*;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * 跨链模块启动类
 * Cross Chain Module Startup and Initialization Management
 *
 * @author tag
 * 2019/4/10
 */
@Component
public class CrossChainBootStrap extends BaseCrossChainBootStrap {
    @Autowired
    private NulsCrossChainConfig nulsCrossChainConfig;
    @Autowired
    private RegisteredCrossChainService registeredCrossChainService;

    @Autowired
    private ChainManager chainManager;

    public static void main(String[] args) {
        ConfigurationLoader configurationLoader = new ConfigurationLoader();
        configurationLoader.load();
        int defaultChainId = Integer.parseInt(configurationLoader.getValue("chainId"));
        ServiceManager.init(defaultChainId, Provider.ProviderType.RPC);
        NulsRpcModuleBootstrap.run(CONTEXT_PATH, args);
    }

    /**
     * 初始化模块，比如初始化RockDB等，在此处初始化后，可在其他bean的afterPropertiesSet中使用
     * 在onStart前会调用此方法
     */
    @Override
    public void init() {
        try {
            super.init();
            initSys();
            //增加地址工具类初始化
            AddressTool.init(new AddressPrefixDatas());
            initDB();
            /**
             * 添加RPC接口目录
             * Add RPC Interface Directory
             * */
            registerRpcPath(RPC_PATH);
            chainManager.initChain();
            ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
            String version160Height = configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_6_0");
            if (StringUtils.isNotBlank(version160Height)) {
                nulsCrossChainConfig.setVersion1_6_0_height(Long.parseLong(version160Height));
            } else {
                nulsCrossChainConfig.setVersion1_6_0_height(0L);
            }
            String pubKeys = configurationLoader.getValue(ModuleE.Constant.CONSENSUS, "pubKeyList");
            HashSet<String> seedSet = new HashSet<>();
            nulsCrossChainConfig.setSeedNodeSet(seedSet);
            if (StringUtils.isBlank(pubKeys)) {
                return;
            }
            String[] pubs = pubKeys.split(",");
            for (String pub : pubs) {
                seedSet.add(AddressTool.getAddressString(HexUtil.decode(pub), nulsCrossChainConfig.getChainId()));
            }

        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                new Module(ModuleE.NW.abbr, VERSION),
                new Module(ModuleE.TX.abbr, VERSION),
                new Module(ModuleE.AC.abbr, VERSION),
                new Module(ModuleE.CS.abbr, VERSION),
                new Module(ModuleE.LG.abbr, VERSION),
                new Module(ModuleE.BL.abbr, VERSION),
                new Module(ModuleE.PU.abbr, VERSION)
        };
    }

    @Override
    public boolean doStart() {
        try {
            while (!isDependencieReady(ModuleE.NW.abbr) || !isDependencieReady(ModuleE.TX.abbr) || !isDependencieReady(ModuleE.CS.abbr)) {
                Log.debug("wait depend modules ready");
                Thread.sleep(2000L);
            }
            return true;
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    @Override
    public void onDependenciesReady(Module module) {
        try {
            /*
             * 注册交易
             * Registered transactions
             */
            if (module.getName().equals(ModuleE.TX.abbr)) {
                for (Integer chainId : chainManager.getChainMap().keySet()) {
                    RegisterHelper.registerTx(chainId, ProtocolGroupManager.getCurrentProtocol(chainId));
                }
            }
            /*
             * 注册协议,如果为非主网则需激活跨链网络
             */
            if (ModuleE.NW.abbr.equals(module.getName())) {
                RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
                for (Chain chain : chainManager.getChainMap().values()) {
                    if (!chain.isMainChain()) {
                        NetWorkCall.activeCrossNet(chain.getChainId(), chain.getConfig().getMaxOutAmount(), chain.getConfig().getMaxInAmount(), nulsCrossChainConfig.getCrossSeedIps());
                    }
                }
            }
            /*
             * 如果为主网，向链管理模块获取完整的跨链注册信息
             */
            if (nulsCrossChainConfig.isMainNet() && (ModuleE.CM.abbr.equals(module.getName()))) {
                RegisteredChainMessage registeredChainMessage = registeredCrossChainService.get();
                if (registeredChainMessage != null && registeredChainMessage.getChainInfoList() != null) {
                    chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());
                } else {
                    registeredChainMessage = ChainManagerCall.getRegisteredChainInfo();
                    registeredCrossChainService.save(registeredChainMessage);
                    chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());

                }
            }

            /*
             * 如果为账户模块启动，向账户模块发送链前缀
             */
            if (ModuleE.AC.abbr.equals(module.getName())) {
                AccountCall.addAddressPrefix(chainManager.getPrefixList());
            }

        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        Log.debug("cc onDependenciesReady");
        chainManager.runChain();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }

    /**
     * 初始化系统编码
     * Initialization System Coding
     */
    private void initSys() throws Exception {
        System.setProperty(SYS_FILE_ENCODING, UTF_8.name());
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, UTF_8);
    }

    /**
     * 初始化数据库
     * Initialization database
     */
    private void initDB() throws Exception {
        RocksDBService.init(nulsCrossChainConfig.getDataFolder());
        RocksDBService.createTable(DB_NAME_CONSUME_LANGUAGE);
        RocksDBService.createTable(DB_NAME_CONSUME_CONGIF);
        RocksDBService.createTable(DB_NAME_LOCAL_VERIFIER);
        RocksDBService.createTable(DB_NAME_TOTAL_OUT_AMOUNT);
        /*
            已注册跨链的链信息操作表
            Registered Cross-Chain Chain Information Operating Table
            key：RegisteredChain
            value:已注册链信息列表
            */
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN);
    }

}
