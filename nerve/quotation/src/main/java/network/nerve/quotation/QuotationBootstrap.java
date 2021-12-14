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
                Module.build(ModuleE.NW),
                Module.build(ModuleE.TX),
                Module.build(ModuleE.CS),
                Module.build(ModuleE.AC),
                Module.build(ModuleE.BL),
                Module.build(ModuleE.SW),
                new Module(ModuleE.PU.abbr, ROLE)
        };
    }

    @Override
    public Module moduleInfo() {
        return Module.build(ModuleE.QU);
    }

    @Override
    public void init() {
        try {
            //初始化地址工具
            AddressTool.init(addressPrefixDatas);
            //初始化系统参数
            initSys();
            //初始化数据库配置文件
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
            while (!isDependencieReady(ModuleE.NW.abbr)){
                LoggerUtil.LOG.debug("wait depend modules ready");
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
        if (ModuleE.TX.abbr.equals(module.getName())) {
            chainManager.registerTx();
            LoggerUtil.LOG.info("register tx ...");
        }
        if (ModuleE.NW.abbr.equals(module.getName())) {
            RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
            LoggerUtil.LOG.info("register msg ...");
        }
        if (ModuleE.PU.abbr.equals(module.getName())) {
            chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
            LoggerUtil.LOG.info("register protocol ...");
        }
        if (ModuleE.BL.abbr.equals(module.getName())) {
            chainManager.getChainMap().values().forEach(QuotationCall::subscriptionNewBlockHeight);
            Log.info("subscription new block height");
        }
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }

    /**
     * 初始化系统编码
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
        //数据文件存储地址
        RocksDBService.init(quConfig.getDataRoot());
        RocksDBService.createTable(QuotationConstant.DB_MODULE_CONGIF);
    }

    /**
     * 根据chainId 加载特殊的协议配置
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

        } catch (Exception e) {
            Log.error(e);
        }
    }
    /**
     * 模块配置
     */
    private void initQuotationConfig() {
        String startStr = quConfig.getQuoteStartHm();
        if(StringUtils.isNotBlank(startStr)) {
            try {
                String[] startNumber = startStr.split(":");
                QuotationContext.quoteStartH = Integer.parseInt(startNumber[0]);
                QuotationContext.quoteStartM = Integer.parseInt(startNumber[1]);
            } catch (NumberFormatException e) {
                LoggerUtil.LOG.error("加载价格采集开始时间配置项异常",e);
            }
        }
        String endStr = quConfig.getQuoteEndHm();
        if(StringUtils.isNotBlank(startStr)) {
            try {
                String[] endNumber = endStr.split(":");
                QuotationContext.quoteEndH = Integer.parseInt(endNumber[0]);
                QuotationContext.quoteEndM = Integer.parseInt(endNumber[1]);
            } catch (NumberFormatException e) {
                LoggerUtil.LOG.error("加载价格采集结束时间配置项异常",e);
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

        LoggerUtil.LOG.info("获取报价开始时间: {}:{}", QuotationContext.quoteStartH, QuotationContext.quoteStartM);
        LoggerUtil.LOG.info("获取报价结束时间(统计最终报价开始时间): {}:{}", QuotationContext.quoteEndH, QuotationContext.quoteEndM);
        LoggerUtil.LOG.info("effectiveQuotation : {}", QuotationContext.effectiveQuotation);
    }

}
