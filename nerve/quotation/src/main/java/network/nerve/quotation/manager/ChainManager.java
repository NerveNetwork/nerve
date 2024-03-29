package network.nerve.quotation.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.ProtocolLoader;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.heterogeneouschain.BNBWalletApi;
import network.nerve.quotation.heterogeneouschain.ETHWalletApi;
import network.nerve.quotation.heterogeneouschain.HECOWalletApi;
import network.nerve.quotation.heterogeneouschain.context.BnbContext;
import network.nerve.quotation.heterogeneouschain.context.EthContext;
import network.nerve.quotation.heterogeneouschain.context.HtContext;
import network.nerve.quotation.model.bo.*;
import network.nerve.quotation.storage.ConfigStorageService;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.storage.QuotationIntradayStorageService;
import network.nerve.quotation.storage.QuotationStorageService;
import network.nerve.quotation.task.CalculatorTask;
import network.nerve.quotation.task.CollectorTask;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.LoggerUtil;
import network.nerve.quotation.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static network.nerve.quotation.constant.QuotationConstant.QU_CONTRACT_FILE;
import static network.nerve.quotation.heterogeneouschain.constant.BnbConstant.BSC_CHAIN;
import static network.nerve.quotation.heterogeneouschain.constant.EthConstant.ETH_CHAIN;
import static network.nerve.quotation.heterogeneouschain.constant.HtConstant.HECO_CHAIN;
import static network.nerve.quotation.util.LoggerUtil.LOG;

@Component
public class ChainManager {

    @Autowired
    private ConfigStorageService configService;

    @Autowired
    private QuotationStorageService quotationStorageService;

    @Autowired
    private ConfirmFinalQuotationStorageService confirmFinalQuotationStorageService;

    @Autowired
    private QuotationIntradayStorageService quotationIntradayStorageService;

    @Autowired
    private QuConfig quConfig;

    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private HECOWalletApi hecoWalletApi;
    private Map<Integer, Chain> chainMap = new ConcurrentHashMap<>();

    /**
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void initChain() throws Exception {
        Map<Integer, ConfigBean> configMap = configChain();
        if (configMap == null || configMap.size() == 0) {
            return;
        }
        for (Map.Entry<Integer, ConfigBean> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            chain.setConfigBean(entry.getValue());
            initLogger(chain);
            initTable(chain);
            chainMap.put(chainId, chain);
            chain.getLogger().debug("Chain:{} init success..", chainId);
            ProtocolLoader.load(chainId);
            loadQuoteCfgJson(chain);
            loadQuoteContractCfgJson(chain);
            loadCollectorCfgJson(chain);
            loadIntradayQuotedToken(chain);

            //Initialize heterogeneous chain information
        }
    }

    /**
     * Load transaction pairs that have completed the final quotation on the same day, To prevent secondary quotation
     *
     * @param chain
     */
    private void loadIntradayQuotedToken(Chain chain) {
        List<QuotationActuator> quteList = chain.getQuote();
        for (QuotationActuator qa : quteList) {
            String date = TimeUtil.nowUTCDate();
            String anchorToken = qa.getAnchorToken();
            String dbKey = CommonUtil.assembleKey(date, anchorToken);
            if (null != confirmFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey)) {
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.add(anchorToken);
                QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.add(anchorToken);
            }
        }
        // swapContract quotation
        for (QuotationContractCfg qa : chain.getContractQuote()) {
            String date = TimeUtil.nowUTCDate();
            String anchorToken = qa.getAnchorToken();
            String dbKey = CommonUtil.assembleKey(date, anchorToken);
            if (null != confirmFinalQuotationStorageService.getCfrFinalQuotation(chain, dbKey)) {
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.add(anchorToken);
                QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.add(anchorToken);
            }
        }
        try {
            chain.getLogger().info("{}", JSONUtils.obj2json(QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS));
            chain.getLogger().info("{}", JSONUtils.obj2json(QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // Quotations executed on the same daytoken(Not necessarily completed the final quotation)
        List<String> allTokens = quotationIntradayStorageService.getAll(chain);
        QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.addAll(allTokens);
    }


    /**
     * Registration transaction
     */
    public void registerTx() {
        try {
            for (Chain chain : chainMap.values()) {
                int chainId = chain.getConfigBean().getChainId();
                RegisterHelper.registerTx(chainId, ProtocolGroupManager.getCurrentProtocol(chainId));
            }
        } catch (Exception e) {
            LoggerUtil.LOG.error("registerTx error!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void runChain() {
        for (Chain chain : chainMap.values()) {
            createScheduler(chain);
            chainMap.put(chain.getChainId(), chain);
            chain.getLogger().debug("Chain:{} runChain success..", chain.getChainId());
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
            Map<Integer, ConfigBean> configMap = configService.getList();

            /*
            If the system is running for the first time and there is no storage chain information in the local database, it is necessary to read the main chain configuration information from the configuration file
            If the system is running for the first time, the local database does not have chain information,
            and the main chain configuration information needs to be read from the configuration file at this time.
            */
            if (configMap.isEmpty()) {
                ConfigBean configBean = quConfig;

                boolean saveSuccess = configService.save(configBean, configBean.getChainId());
                if (saveSuccess) {
                    configMap.put(configBean.getChainId(), configBean);
                }
            }
            return configMap;
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * Load pricing configuration
     *
     * @param chain
     * @throws Exception
     */
    private void loadQuoteCfgJson(Chain chain) throws Exception {
        String quotationConfigJson = IoUtils.read(QuotationConstant.QU_CONFIG_FILE);
        QuotationCfg quotationCfg = JSONUtils.json2pojo(quotationConfigJson, QuotationCfg.class);
        List<QuotationActuator> quote = new ArrayList<>();
        for (QuoteCfg quoteCfg : quotationCfg.getQuote()) {
            List<TokenCfg> tokenCfgsList = quoteCfg.getItems();
            for (TokenCfg tokenCfg : tokenCfgsList) {
                QuotationActuator qa = new QuotationActuator();
                qa.setType(quoteCfg.getType());
                qa.setDataParser(quoteCfg.getDataParser());
                qa.setKey(tokenCfg.getKey());
                qa.setCollector(tokenCfg.getCollector());
                qa.setCalculator(tokenCfg.getCalculator());
                qa.setAnchorToken(tokenCfg.getAnchorToken().toUpperCase());
                qa.setDesc(tokenCfg.getDesc());
                quote.add(qa);
            }
        }
        chain.setQuote(quote);
        chain.getLogger().info("quotation-config : {}", JSONUtils.obj2json(quote));
    }

    /**
     * Load Contract(swap)Feeding configuration
     *
     * @param chain
     * @throws Exception
     */
    private void loadQuoteContractCfgJson(Chain chain) throws Exception {
        String quotationConfigJson = IoUtils.read(QU_CONTRACT_FILE + chain.getChainId() + ".json");
        List<QuotationContractCfg> quotationContractCfg = JSONUtils.json2list(quotationConfigJson, QuotationContractCfg.class);

        for (QuotationContractCfg quContractCfg : quotationContractCfg) {
            String rpcAddress = quContractCfg.getRpcAddress();
            switch (quContractCfg.getChain()) {
                case ETH_CHAIN:
                    if (StringUtils.isNotBlank(rpcAddress)) {
                        EthContext.rpcAddress = rpcAddress;
                    }
                    ethWalletApi.init();
                    break;
                case BSC_CHAIN:
                    if (StringUtils.isNotBlank(rpcAddress)) {
                        BnbContext.rpcAddress = rpcAddress;
                    }
                    bnbWalletApi.init();
                    break;
                case HECO_CHAIN:
                    if (StringUtils.isNotBlank(rpcAddress)) {
                        HtContext.rpcAddress = rpcAddress;
                    }
                    hecoWalletApi.init();
                    break;
                default:
            }
        }
        chain.setContractQuote(quotationContractCfg);
        chain.getLogger().info("quotation-contract-config : {}", quotationConfigJson);
    }

    /**
     * Third party price collectionapiallocation
     *
     * @param chain
     * @throws Exception
     */
    private void loadCollectorCfgJson(Chain chain) throws Exception {
        String configJson = IoUtils.read(QuotationConstant.COLLECTOR_CONFIG_FILE);
        List<QuerierCfg> list = JSONUtils.json2list(configJson, QuerierCfg.class);
        chain.setCollectors(list);
        chain.getLogger().info("collector-config : {}", JSONUtils.obj2json(list));
    }

    /**
     * Initialize Chain Related Tables
     * Initialization chain correlation table
     *
     * @param chain
     */
    private void initTable(Chain chain) {
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getConfigBean().getChainId();
        try {
            RocksDBService.createTable(QuotationConstant.DB_QUOTATION_NODE_PREFIX + chainId);
            RocksDBService.createTable(QuotationConstant.DB_QUOTATION_FINAL_PREFIX + chainId);
            RocksDBService.createTable(QuotationConstant.DB_LAST_QUOTATION_PREFIX + chainId);
            RocksDBService.createTable(QuotationConstant.DB_CONFIRM_FINAL_QUOTATION_PREFIX + chainId);
            RocksDBService.createTable(QuotationConstant.DB_CONFIRM_LAST_FINAL_QUOTATION_PREFIX + chainId);
            RocksDBService.createTable(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chainId);
        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                logger.error(e);
            }
        }
    }

    private void initLogger(Chain chain) {
        LoggerUtil.init(chain);
    }

    public Map<Integer, Chain> getChainMap() {
        return chainMap;
    }

    public Chain getChain(int key) {
        return this.chainMap.get(key);
    }

    /**
     * Start scheduled tasks for price collection and statistics
     *
     * @param chain
     * @return
     */
    public void createScheduler(Chain chain) {
        ScheduledThreadPoolExecutor collectorExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(QuotationConstant.QU_COLLECTOR_THREAD));
        collectorExecutor.scheduleWithFixedDelay(new CollectorTask(chain),
                QuotationConstant.QU_TASK_INITIALDELAY, QuotationConstant.QUTASK_PERIOD, TimeUnit.MINUTES);

        ScheduledThreadPoolExecutor calculatorExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(QuotationConstant.QU_CALCULATOR_THREAD));
        calculatorExecutor.scheduleWithFixedDelay(new CalculatorTask(chain),
                QuotationConstant.QU_TASK_INITIALDELAY, QuotationConstant.QUTASK_PERIOD, TimeUnit.MINUTES);
    }
}
