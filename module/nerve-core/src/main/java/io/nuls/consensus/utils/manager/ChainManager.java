package io.nuls.consensus.utils.manager;

import io.nuls.common.ConfigBean;
import io.nuls.common.NerveCoreConfig;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.economic.base.service.EconomicService;
import io.nuls.consensus.economic.nuls.constant.ParamConstant;
import io.nuls.consensus.economic.nuls.model.bo.ConsensusConfigInfo;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.config.AssetsStakingLimitCfg;
import io.nuls.consensus.model.bo.config.AssetsType;
import io.nuls.consensus.network.service.ConsensusNetService;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.storage.PubKeyStorageService;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chain management,Responsible for initializing each chain,working,start-up,Parameter maintenance, etc
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author tag
 * 2018/12/4
 */
@Component
public class ChainManager {
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private ThreadManager threadManager;
    @Autowired
    private NerveCoreConfig config;
    @Autowired
    private EconomicService economicService;
    @Autowired
    private AgentDepositManager agentDepositManager;
    @Autowired
    private PubKeyStorageService pubKeyService;
    @Autowired
    private ConsensusNetService netService;
    private final Map<Integer, Chain> chainMap = new ConcurrentHashMap<>();
    private List<StackingAsset> stackingAssetList = new ArrayList<>();

    /**
     * initialization
     * Initialization chain
     */
    public void initChain() throws Exception {
        //Load information on assets that can participate in collateral
        String stackAssetConfigFilePath = "consensus" + File.separator + ConsensusConstant.STACKING_CONFIG_FILE + "-" + config.getChainId() + ".json";
        URL url = ChainManager.class.getClassLoader().getResource(stackAssetConfigFilePath);
        if (url != null) {
            stackingAssetList = JSONUtils.json2list(IoUtils.read(stackAssetConfigFilePath), StackingAsset.class);
            stackingAssetList.forEach(stackingAsset -> {
                //If there is no configurationchainId Default to this chain asset
                if (stackingAsset.getChainId() == null) {
                    stackingAsset.setChainId(config.getChainId());
                }
                config.putWeight(stackingAsset.getChainId(), stackingAsset.getAssetId(), stackingAsset.getWeight());
            });
        }
        Map<Integer, ConfigBean> configMap = configChain();
        if (configMap == null || configMap.size() == 0) {
            Log.info("Chain initialization failed！");
            return;
        }
        for (Map.Entry<Integer, ConfigBean> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            ConfigBean chainConfig = entry.getValue();
            chain.setConfig(chainConfig);
            chain.setSeedAddressList(List.of(chainConfig.getSeedNodes().split(ConsensusConstant.SEED_NODE_SEPARATOR)));
            chain.setThreadPool(ThreadUtils.createThreadPool(6, 100, new NulsThreadFactory("consensus" + chainId)));
            /*
             * Initialize Chain Log Object
             * Initialization Chain Log Objects
             * */
            initLogger(chain);
            /*
            Initialize Chain Database Table
            Initialize linked database tables
            */
            initTable(chain);
            chainMap.put(chainId, chain);
            //ProtocolLoader.load(chainId);
            Map<String, Object> param = new HashMap<>(4);
            param.put(ParamConstant.CONSENUS_CONFIG, new ConsensusConfigInfo(chainId, chainConfig.getAssetId(), chainConfig.getPackingInterval(),
                    chainConfig.getInflationAmount(), chainConfig.getTotalInflationAmount(), chainConfig.getInitHeight(), chainConfig.getDeflationRatio(), chainConfig.getDeflationHeightInterval(), chainConfig.getAwardAssetId()));


            economicService.registerConfig(param);
            List<String> seedNodePubKeyList = List.of(chainConfig.getPubKeyList().split(ConsensusConstant.SEED_NODE_SEPARATOR));
            for (String pubKey : seedNodePubKeyList) {
                chain.getSeedNodePubKeyList().add(HexUtil.decode(pubKey));
            }
        }
    }

    /**
     * Load chain cache data and start the chain
     * Load the chain to cache data and start the chain
     */
    public void runChain() {
        for (Chain chain : chainMap.values()) {
            /*
            Load chain cache data
            Load chain caching entity
            */
            initCache(chain);

            /*
            Create and initiate in chain tasks
            Create and start in-chain tasks
            */
            threadManager.createChainThread(chain);

            /*
             * Create scheduled tasks
             * */
            threadManager.createChainScheduler(chain);
        }
    }


    /**
     * Read configuration file to create and initialize chain
     * Read the configuration file to create and initialize the chain
     */
    private Map<Integer, ConfigBean> configChain() {
        try {
            /*
            Read database chain information configuration
            Read database chain information configuration
             */
            Map<Integer, ConfigBean> configMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
            //Load special configurations and set them toconfigin
            fillSpecialConfig(config);
            configMap.put(config.getChainId(), config);
            return configMap;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    private void fillSpecialConfig(ConfigBean config) {
        try {
            Map<String, Object> specConfigMap = JSONUtils.json2map(IoUtils.read("consensus" + File.separator + "spec-cfg-" + config.getChainId() + ".json"));
            Long depositAwardChangeHeight = Long.parseLong("" + specConfigMap.get("depositAwardChangeHeight"));
            Long depositVerifyHeight = Long.parseLong("" + specConfigMap.get("depositVerifyHeight"));
            Long minRewardHeight = Long.parseLong("" + specConfigMap.get("minRewardHeight"));
            Long maxCoinToOfCoinbase = Long.parseLong("" + specConfigMap.get("maxCoinToOfCoinbase"));

//            "v130Height": 200000,
//                    "minStakingAmount": 100000000000,
//                    "minAppendAndExitAmount":200000000000,
//                    "exitStakingLockHours":168

            Long v1_3_0Height = Long.parseLong("" + specConfigMap.get("v130Height"));
            Long v1_6_0Height = Long.parseLong("" + specConfigMap.get("v160Height"));
            Long v1_7_0Height = Long.parseLong("" + specConfigMap.get("v170Height"));
            BigInteger minStakingAmount = new BigInteger("" + specConfigMap.get("minStakingAmount"));
            BigInteger minAppendAndExitAmount = new BigInteger("" + specConfigMap.get("minAppendAndExitAmount"));
            Integer exitStakingLockHours = Integer.parseInt("" + specConfigMap.get("exitStakingLockHours"));

            config.setDepositAwardChangeHeight(depositAwardChangeHeight);
            config.setDepositVerifyHeight(depositVerifyHeight);
            config.setMinRewardHeight(minRewardHeight);
            config.setMaxCoinToOfCoinbase(maxCoinToOfCoinbase.intValue());

            config.setV130Height(v1_3_0Height);
            config.setV1_6_0Height(v1_6_0Height);
            config.setV1_7_0Height(v1_7_0Height);
            config.setMinStakingAmount(minStakingAmount);
            config.setMinAppendAndExitAmount(minAppendAndExitAmount);
            config.setExitStakingLockHours(exitStakingLockHours);


            loacLimitCfg(specConfigMap, config);

        } catch (Exception e) {
            Log.error(e);
        }
    }

    private void loacLimitCfg(Map<String, Object> specConfigMap, ConfigBean config) {
        List<AssetsStakingLimitCfg> resultList = new ArrayList<>();
        List<Object> limitList = (List<Object>) specConfigMap.get("limit");
        for (Object limit : limitList) {
            Map<String, Object> limitMap = (Map<String, Object>) limit;
            AssetsStakingLimitCfg cfg = new AssetsStakingLimitCfg();
            cfg.setKey((String) limitMap.get("key"));
            cfg.setTotalCount(Long.parseLong("" + limitMap.get("totalCount")));
            cfg.setAssetsTypeList(new ArrayList<>());
            List<Object> assetsList = (List<Object>) limitMap.get("assets");
            for (Object item : assetsList) {
                Map<String, Object> assets = (Map<String, Object>) item;
                AssetsType type = new AssetsType();
                type.setChainId((Integer) assets.get("assetsChainId"));
                type.setAssetsId((Integer) assets.get("assetsId"));
                cfg.getAssetsTypeList().add(type);
            }
            resultList.add(cfg);
        }
        config.setLimitCfgList(resultList);
    }

    /**
     * Initialize Chain Related Tables
     * Initialization chain correlation table
     *
     * @param chain chain info
     */
    private void initTable(Chain chain) {
        String dbNameSuffix = ConsensusConstant.SEPARATOR + chain.getConfig().getChainId();
        try {
            /*
            Create consensus node table
            Create consensus node tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_AGENT + dbNameSuffix);

            /*
            Additional margin
            Additional margin
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_APPEND_DEPOSIT + dbNameSuffix);

            /*
            Reduce margin
            Additional margin
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_REDUCE_DEPOSIT + dbNameSuffix);

            /*
            Commission Information Form
            Create consensus information tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_DEPOSIT + dbNameSuffix);

            /*
            Create a red and yellow card information table
            Creating Red and Yellow Card Information Table
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_PUNISH + dbNameSuffix);

            /*
            Node corresponding public key set
            RocksDBService.createTable(ConsensusConstant.DB_NAME_PUB_KEY + dbNameSuffix);*/

            /*
            Create a low-level random number table
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_RANDOM_SEEDS + dbNameSuffix);

            /*
            Create node marginnoncesurface
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_AGENT_DEPOSIT_NONCE + dbNameSuffix);
        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                chain.getLogger().error(e.getMessage());
            } else {
                chain.getLogger().error(e.getMessage());
            }
        }
    }

    private void initLogger(Chain chain) {
        /*
         * Consensus module log file object creation,If a chain has multiple types of log files, you can add them here
         * Creation of Log File Object in Consensus Module,If there are multiple log files in a chain, you can add them here
         * */
        LoggerUtil.initLogger(chain);
    }

    /**
     * Initialize chain cache data
     * staypocUnder the consensus mechanism, due to the existence of round information, node information, and red and yellow card information for node punishment,
     * Therefore, it is necessary to cache relevant data during initialization to calculate the latest round information, as well as the credit values of each node
     * Initialize chain caching entity
     *
     * @param chain chain info
     */
    private void initCache(Chain chain) {
        try {
            CallMethodUtils.loadBlockHeader(chain);
            agentManager.loadAgents(chain);
            depositManager.loadDeposits(chain);
            punishManager.loadPunishes(chain);
            agentDepositManager.loadAppendDeposits(chain);
            agentDepositManager.loadReduceDeposits(chain);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * Modify node consensus network status
     *
     * @param chain Chain information
     * @param state Consensus Network State
     */
    public void netWorkStateChange(Chain chain, boolean state) {
        //Modify consensus status
        chain.setNetworkStateOk(state);
    }

    public StackingAsset assetStackingVerify(int chainId, int assetId) {

        for (StackingAsset stackingAsset : stackingAssetList) {
            if (stackingAsset.getChainId() == chainId && stackingAsset.getAssetId() == assetId) {
                return stackingAsset;
            }
        }
        return null;
    }

    public StackingAsset getAssetBySymbol(String simple) {
        for (StackingAsset stackingAsset : stackingAssetList) {
            if (simple.equals(stackingAsset.getSimple())) {
                return stackingAsset;
            }
        }
        return null;
    }

    public StackingAsset getAssetByAsset(int chainId, int assetId) {
        for (StackingAsset stackingAsset : stackingAssetList) {
            if (stackingAsset.getChainId() == chainId && stackingAsset.getAssetId() == assetId) {
                return stackingAsset;
            }
        }
        return null;
    }

    public Map<Integer, Chain> getChainMap() {
        return chainMap;
    }

    public List<StackingAsset> getStackingAssetList() {
        return stackingAssetList;
    }

    public void setStackingAssetList(List<StackingAsset> stackingAssetList) {
        this.stackingAssetList = stackingAssetList;
    }
}
