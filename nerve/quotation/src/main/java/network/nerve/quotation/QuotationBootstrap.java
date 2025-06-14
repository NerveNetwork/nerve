package network.nerve.quotation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.manager.ChainManager;
import network.nerve.quotation.model.bo.QuConfig;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.util.LoggerUtil;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static network.nerve.quotation.constant.QuotationConstant.QU_PROTOCOL_FILE;

@Component
public class QuotationBootstrap extends RpcModule {

    @Autowired
    private QuConfig quConfig;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ChainManager chainManager;

    public static void main(String[] args) {
        NulsRpcModuleBootstrap.run("io.nuls,network.nerve", args);
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.NC),
                Module.build(ModuleE.SW),
        };
    }

    @Override
    public Module moduleInfo() {
        return Module.build(ModuleE.QU);
    }

    @Override
    public void init() {
        try {
            //Initialize Address Tool
            AddressTool.init(addressPrefixDatas);
            //Initialize system parameters
            initSys();
            //Initialize database configuration file
            initDB();
            initModuleProtocolCfg();
            initQuotationConfig();
            chainManager.initChain();
            ModuleHelper.init(this);

        } catch (Exception e) {
            LoggerUtil.LOG.error("Quotation init error!");
            LoggerUtil.LOG.error(e);
            System.exit(1);
        }
    }

    @Override
    public boolean doStart() {
        try {
            chainManager.runChain();
            while (!isDependencieReady(ModuleE.NC.abbr)){
                LoggerUtil.LOG.info("wait nerve-core modules ready");
                Thread.sleep(2000L);
            }
            LoggerUtil.LOG.info("Transaction Ready...");
            return true;
        } catch (Exception e) {
            LoggerUtil.LOG.error("Transaction init error!");
            LoggerUtil.LOG.error(e);
            return false;
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        LoggerUtil.LOG.info("Quotation onDependenciesReady");
        NulsDateUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public void onDependenciesReady(Module module) {
        if (ModuleE.NC.abbr.equals(module.getName())) {
            chainManager.registerTx();
            LoggerUtil.LOG.info("register tx ...");
            RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
            LoggerUtil.LOG.info("register msg ...");
            chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
            LoggerUtil.LOG.info("register protocol ...");
            chainManager.getChainMap().values().forEach(QuotationCall::subscriptionNewBlockHeight);
            Log.info("subscription new block height");
        }
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }

    /**
     * Initialize system encoding
     */
    private void initSys() throws Exception {
        System.setProperty(QuotationConstant.SYS_FILE_ENCODING, UTF_8.name());
        Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true);
        charset.set(null, UTF_8);
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger("org.web3j.protocol.http.HttpService");
            logger.setLevel(Level.INFO);
        } catch (Exception e) {
            // skip it
            Log.warn("log level setting error", e);
        }
    }

    public void initDB() throws Exception {
        //Data file storage address
        RocksDBService.init(quConfig.getDataRoot());
        RocksDBService.createTable(QuotationConstant.DB_MODULE_CONGIF);
    }

    /**
     * according tochainId Load special protocol configurations
     */
    private void initModuleProtocolCfg() {
        try {
            Map map = JSONUtils.json2map(IoUtils.read(QU_PROTOCOL_FILE + quConfig.getChainId() + ".json"));
            long usdtDaiUsdcPaxKeyHeight = Long.parseLong(map.get("usdtDaiUsdcPaxKeyHeight").toString());
            quConfig.setUsdtDaiUsdcPaxKeyHeight(usdtDaiUsdcPaxKeyHeight);

            long bnbKeyHeight = Long.parseLong(map.get("bnbKeyHeight").toString());
            quConfig.setBnbKeyHeight(bnbKeyHeight);
            long htOkbKeyHeight = Long.parseLong(map.get("htOkbKeyHeight").toString());
            quConfig.setHtOkbKeyHeight(htOkbKeyHeight);
            long oktKeyHeight = Long.parseLong(map.get("OktKeyHeight").toString());
            quConfig.setOktKeyHeight(oktKeyHeight);
            long oneMaticKcsHeight = Long.parseLong(map.get("oneMaticKcsHeight").toString());
            quConfig.setOneMaticKcsHeight(oneMaticKcsHeight);
            long trxKeyHeight = Long.parseLong(map.get("trxKeyHeight").toString());
            quConfig.setTrxKeyHeight(trxKeyHeight);
            long protocol16Height = Long.parseLong(map.get("protocol16Height").toString());
            quConfig.setProtocol16Height(protocol16Height);
            long protocol21Height = Long.parseLong(map.get("protocol21Height").toString());
            quConfig.setProtocol21Height(protocol21Height);
            long protocol22Height = Long.parseLong(map.get("protocol22Height").toString());
            quConfig.setProtocol22Height(protocol22Height);
            long protocol24Height = Long.parseLong(map.get("protocol24Height").toString());
            quConfig.setProtocol24Height(protocol24Height);
            long protocol26Height = Long.parseLong(map.get("protocol26Height").toString());
            quConfig.setProtocol26Height(protocol26Height);
            long protocol27Height = Long.parseLong(map.get("protocol27Height").toString());
            quConfig.setProtocol27Height(protocol27Height);
            long protocol29Height = Long.parseLong(map.get("protocol29Height").toString());
            quConfig.setProtocol29Height(protocol29Height);
            long protocol30Height = Long.parseLong(map.get("protocol30Height").toString());
            quConfig.setProtocol30Height(protocol30Height);
            quConfig.setProtocol31Height(Long.parseLong(map.get("protocol31Height").toString()));
            quConfig.setProtocol34Height(Long.parseLong(map.get("protocol34Height").toString()));
            quConfig.setProtocol40Height(Long.parseLong(map.get("protocol40Height").toString()));

        } catch (Exception e) {
            Log.error(e);
        }
    }
    /**
     * Module Configuration
     */
    private void initQuotationConfig() {
        String startStr = quConfig.getQuoteStartHm();
        if(StringUtils.isNotBlank(startStr)) {
            try {
                String[] startNumber = startStr.split(":");
                QuotationContext.quoteStartH = Integer.parseInt(startNumber[0]);
                QuotationContext.quoteStartM = Integer.parseInt(startNumber[1]);
            } catch (NumberFormatException e) {
                LoggerUtil.LOG.error("Abnormal configuration item for loading price collection start time",e);
            }
        }
        String endStr = quConfig.getQuoteEndHm();
        if(StringUtils.isNotBlank(startStr)) {
            try {
                String[] endNumber = endStr.split(":");
                QuotationContext.quoteEndH = Integer.parseInt(endNumber[0]);
                QuotationContext.quoteEndM = Integer.parseInt(endNumber[1]);
            } catch (NumberFormatException e) {
                LoggerUtil.LOG.error("Abnormal configuration item for loading price collection end time",e);
            }
        }
        QuotationContext.effectiveQuotation = quConfig.getEffectiveQuotation();
        QuotationContext.removeMaxMinCount = quConfig.getRemoveMaxMinCount();
        QuotationContext.usdtDaiUsdcPaxKeyHeight = quConfig.getUsdtDaiUsdcPaxKeyHeight();
        QuotationContext.bnbKeyHeight = quConfig.getBnbKeyHeight();
        QuotationContext.htOkbKeyHeight = quConfig.getHtOkbKeyHeight();
        QuotationContext.oktKeyHeight = quConfig.getOktKeyHeight();
        QuotationContext.oneMaticKcsHeight = quConfig.getOneMaticKcsHeight();
        QuotationContext.trxKeyHeight = quConfig.getTrxKeyHeight();
        QuotationContext.protocol16Height = quConfig.getProtocol16Height();
        QuotationContext.protocol21Height = quConfig.getProtocol21Height();
        QuotationContext.protocol22Height = quConfig.getProtocol22Height();
        QuotationContext.protocol24Height = quConfig.getProtocol24Height();
        QuotationContext.protocol26Height = quConfig.getProtocol26Height();
        QuotationContext.protocol27Height = quConfig.getProtocol27Height();
        QuotationContext.protocol29Height = quConfig.getProtocol29Height();
        QuotationContext.protocol30Height = quConfig.getProtocol30Height();
        QuotationContext.protocol31Height = quConfig.getProtocol31Height();
        QuotationContext.protocol34Height = quConfig.getProtocol34Height();
        QuotationContext.protocol40Height = quConfig.getProtocol40Height();

        LoggerUtil.LOG.info("Get quote start time: {}:{}", QuotationContext.quoteStartH, QuotationContext.quoteStartM);
        LoggerUtil.LOG.info("Get quote end time(Starting time of final quotation statistics): {}:{}", QuotationContext.quoteEndH, QuotationContext.quoteEndM);
        LoggerUtil.LOG.info("effectiveQuotation : {}", QuotationContext.effectiveQuotation);
    }

}
