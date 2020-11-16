package network.nerve.pocbft.utils.manager;

import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.ProtocolLoader;
import io.nuls.base.protocol.RegisterHelper;
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
import io.nuls.economic.base.service.EconomicService;
import io.nuls.economic.nuls.constant.ParamConstant;
import io.nuls.economic.nuls.model.bo.ConsensusConfigInfo;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.StackingAsset;
import network.nerve.pocbft.model.bo.config.AssetsStakingLimitCfg;
import network.nerve.pocbft.model.bo.config.AssetsType;
import network.nerve.pocbft.model.bo.config.ChainConfig;
import network.nerve.pocbft.model.bo.config.ConsensusChainConfig;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
//import network.nerve.pocbft.storage.ConfigService;
import network.nerve.pocbft.storage.PubKeyStorageService;
import network.nerve.pocbft.utils.LoggerUtil;

import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链管理类,负责各条链的初始化,运行,启动,参数维护等
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author tag
 * 2018/12/4
 */
@Component
public class ChainManager {
    //    @Autowired
//    private ConfigService configService;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private ThreadManager threadManager;
    @Autowired
    private ConsensusChainConfig config;
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
     * 初始化
     * Initialization chain
     */
    public void initChain() throws Exception {
        //加载可参与抵押的资产信息
        String stackAssetConfigFilePath = ConsensusConstant.STACKING_CONFIG_FILE + "-" + config.getChainId() + ".json";
        URL url = ChainManager.class.getClassLoader().getResource(stackAssetConfigFilePath);
        if (url != null) {
            stackingAssetList = JSONUtils.json2list(IoUtils.read(stackAssetConfigFilePath), StackingAsset.class);
            stackingAssetList.forEach(stackingAsset -> {
                //如果没有配置chainId 则默认为本链资产
                if (stackingAsset.getChainId() == null) {
                    stackingAsset.setChainId(config.getChainId());
                }
            });
        }
        Map<Integer, ChainConfig> configMap = configChain();
        if (configMap == null || configMap.size() == 0) {
            Log.info("链初始化失败！");
            return;
        }
        for (Map.Entry<Integer, ChainConfig> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            ChainConfig chainConfig = entry.getValue();
            chain.setConfig(chainConfig);
            chain.setSeedAddressList(List.of(chainConfig.getSeedNodes().split(ConsensusConstant.SEED_NODE_SEPARATOR)));
            chain.setThreadPool(ThreadUtils.createThreadPool(6, 100, new NulsThreadFactory("consensus" + chainId)));
            /*
             * 初始化链日志对象
             * Initialization Chain Log Objects
             * */
            initLogger(chain);
            /*
            初始化链数据库表
            Initialize linked database tables
            */
            initTable(chain);
            chainMap.put(chainId, chain);
            ProtocolLoader.load(chainId);
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
     * 注册链交易
     * Registration Chain Transaction
     */
    public void registerTx() {
        for (Chain chain : chainMap.values()) {
            /*
             * 链交易注册
             * Chain Trading Registration
             * */
            int chainId = chain.getConfig().getChainId();
            RegisterHelper.registerTx(chainId, ProtocolGroupManager.getCurrentProtocol(chainId));
        }
    }

    /**
     * 加载链缓存数据并启动链
     * Load the chain to cache data and start the chain
     */
    public void runChain() {
        for (Chain chain : chainMap.values()) {
            /*
            加载链缓存数据
            Load chain caching entity
            */
            initCache(chain);

            /*
            创建并启动链内任务
            Create and start in-chain tasks
            */
            threadManager.createChainThread(chain);

            /*
             * 创建定时任务
             * */
            threadManager.createChainScheduler(chain);
        }
    }


    /**
     * 读取配置文件创建并初始化链
     * Read the configuration file to create and initialize the chain
     */
    private Map<Integer, ChainConfig> configChain() {
        try {
            /*
            读取数据库链信息配置
            Read database chain information configuration
             */
            Map<Integer, ChainConfig> configMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_2);
            //加载特殊配置，并设置到config中
            fillSpecialConfig(config);
            configMap.put(config.getChainId(), config);
            return configMap;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    private void fillSpecialConfig(ConsensusChainConfig config) {
        try {
            Map<String, Object> specConfigMap = JSONUtils.json2map(IoUtils.read("spec-cfg-" + config.getChainId() + ".json"));
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
            BigInteger minStakingAmount = new BigInteger("" + specConfigMap.get("minStakingAmount"));
            BigInteger minAppendAndExitAmount = new BigInteger("" + specConfigMap.get("minAppendAndExitAmount"));
            Integer exitStakingLockHours = Integer.parseInt("" + specConfigMap.get("exitStakingLockHours"));

            config.setDepositAwardChangeHeight(depositAwardChangeHeight);
            config.setDepositVerifyHeight(depositVerifyHeight);
            config.setMinRewardHeight(minRewardHeight);
            config.setMaxCoinToOfCoinbase(maxCoinToOfCoinbase.intValue());

            config.setV130Height(v1_3_0Height);
            config.setV1_6_0Height(v1_6_0Height);
            config.setMinStakingAmount(minStakingAmount);
            config.setMinAppendAndExitAmount(minAppendAndExitAmount);
            config.setExitStakingLockHours(exitStakingLockHours);


            loacLimitCfg(specConfigMap, config);

        } catch (Exception e) {
            Log.error(e);
        }
    }

    private void loacLimitCfg(Map<String, Object> specConfigMap, ConsensusChainConfig config) {
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
     * 初始化链相关表
     * Initialization chain correlation table
     *
     * @param chain chain info
     */
    private void initTable(Chain chain) {
        String dbNameSuffix = ConsensusConstant.SEPARATOR + chain.getConfig().getChainId();
        try {
            /*
            创建共识节点表
            Create consensus node tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_AGENT + dbNameSuffix);

            /*
            追加保证金
            Additional margin
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_APPEND_DEPOSIT + dbNameSuffix);

            /*
            减少保证金
            Additional margin
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_REDUCE_DEPOSIT + dbNameSuffix);

            /*
            委托信息表
            Create consensus information tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_DEPOSIT + dbNameSuffix);

            /*
            创建红黄牌信息表
            Creating Red and Yellow Card Information Table
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_PUNISH + dbNameSuffix);

            /*
            节点对应公钥集合
            RocksDBService.createTable(ConsensusConstant.DB_NAME_PUB_KEY + dbNameSuffix);*/

            /*
            创建底层随机数表
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_RANDOM_SEEDS + dbNameSuffix);

            /*
            创建节点保证金nonce表
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
         * 共识模块日志文件对象创建,如果一条链有多类日志文件，可在此添加
         * Creation of Log File Object in Consensus Module，If there are multiple log files in a chain, you can add them here
         * */
        LoggerUtil.initLogger(chain);
    }

    /**
     * 初始化链缓存数据
     * 在poc的共识机制下，由于存在轮次信息，节点信息，以及节点被惩罚的红黄牌信息，
     * 因此需要在初始化的时候，缓存相关的数据，用于计算最新的轮次信息，以及各个节点的信用值等
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
     * 修改节点共识网络状态
     *
     * @param chain 链信息
     * @param state 共识网络状态
     */
    public void netWorkStateChange(Chain chain, boolean state) {
        //修改共识状态
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
