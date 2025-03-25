package io.nuls.crosschain;

import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.rpc.call.AccountCall;
import io.nuls.crosschain.rpc.call.ChainManagerCall;
import io.nuls.crosschain.rpc.call.NetWorkCall;
import io.nuls.crosschain.srorage.RegisteredCrossChainService;
import io.nuls.crosschain.utils.manager.ChainManager;

import java.io.File;
import java.util.HashSet;


/**
 * Cross chain module startup class
 * Cross Chain Module Startup and Initialization Management
 *
 * @author tag
 * 2019/4/10
 */
@Component
public class CrossChainBootStrap implements INerveCoreBootstrap {
    @Autowired
    private NerveCoreConfig nulsCrossChainConfig;
    @Autowired
    private RegisteredCrossChainService registeredCrossChainService;

    @Autowired
    private ChainManager chainManager;

    @Override
    public int order() {
        return 7;
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();
    }
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.CC.name, "1.0");
    }

    /**
     * Initialize modules, such as initializationRockDBWait, after initialization here, you can use other optionsbeanofafterPropertiesSetUsed in
     * stayonStartI will call this method before
     */
    private void init() {
        try {
            initDB();
            chainManager.initChain();
            ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
            String version160Height = configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_6_0");
            if (StringUtils.isNotBlank(version160Height)) {
                nulsCrossChainConfig.setVersion1_6_0_height(Long.parseLong(version160Height));
            } else {
                nulsCrossChainConfig.setVersion1_6_0_height(0L);
            }
            String version38Height = configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_38_0");
            if (StringUtils.isNotBlank(version38Height)) {
                nulsCrossChainConfig.setVersion1_38_0_height(Long.parseLong(version38Height));
            } else {
                nulsCrossChainConfig.setVersion1_38_0_height(0L);
            }

            String version39Height = configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_39_0");
            if (StringUtils.isNotBlank(version39Height)) {
                nulsCrossChainConfig.setVersion1_39_0_height(Long.parseLong(version39Height));
            } else {
                nulsCrossChainConfig.setVersion1_39_0_height(0L);
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
    public void onDependenciesReady() {
        try {
            /*
             * Registration Agreement,If it is a non main network, cross chain network activation is required
             */
            for (Chain chain : chainManager.getChainMap().values()) {
                if (!chain.isMainChain()) {
                    NetWorkCall.activeCrossNet(chain.getChainId(), chain.getConfig().getMaxOutAmount(), chain.getConfig().getMaxInAmount(), nulsCrossChainConfig.getCrossSeedIps());
                }
            }
            /*
             * If it is the main network, obtain complete cross chain registration information from the chain management module
             */
            if (nulsCrossChainConfig.isMainNet()) {
                RegisteredChainMessage registeredChainMessage = registeredCrossChainService.get();
                if (registeredChainMessage != null && registeredChainMessage.getChainInfoList() != null) {
                    chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());
                } else {
                    registeredChainMessage = ChainManagerCall.getRegisteredChainInfo();
                    registeredCrossChainService.save(registeredChainMessage);
                    chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());

                }
            }

            AccountCall.addAddressPrefix(chainManager.getPrefixList());
            Log.debug("cc onDependenciesReady");
            chainManager.runChain();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * Initialize database
     * Initialization database
     */
    private void initDB() throws Exception {
        RocksDBService.init(nulsCrossChainConfig.getDataPath() + File.separator + ModuleE.CC.name);
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_CONSUME_LANGUAGE);
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_CONSUME_CONGIF);
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_LOCAL_VERIFIER);
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_TOTAL_OUT_AMOUNT);
        /*
        Registered Cross Chain Chain Information Operation Table
        Registered Cross-Chain Chain Information Operating Table
        key：RegisteredChain
        value:Registered Chain Information List
        */
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN);
    }

}
