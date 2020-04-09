package nerve.network.quotation;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.quotation.constant.QuotationConstant;
import nerve.network.quotation.constant.QuotationContext;
import nerve.network.quotation.manager.ChainManager;
import nerve.network.quotation.model.bo.QuConfig;
import nerve.network.quotation.util.LoggerUtil;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

import static nerve.network.quotation.util.LoggerUtil.LOG;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class QuotationBootstrap extends RpcModule {

    @Autowired
    private QuConfig quConfig;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ChainManager chainManager;

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"ws://" + HostInfo.getLocalIP() + ":7771"};
        }
        NulsRpcModuleBootstrap.run("io.nuls", args);
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.NW),
                Module.build(ModuleE.LG),
                Module.build(ModuleE.TX),
                Module.build(ModuleE.CS),
                Module.build(ModuleE.AC)
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
            initQuotationConfig();
            chainManager.initChain();
            ModuleHelper.init(this);

        } catch (Exception e) {
            LOG.error("Quotation init error!");
            LOG.error(e);
            System.exit(1);
        }
    }

    @Override
    public boolean doStart() {
        try {
            chainManager.runChain();
            while (!isDependencieReady(ModuleE.NW.abbr)){
                LOG.debug("wait depend modules ready");
                Thread.sleep(2000L);
            }
            LOG.info("Transaction Ready...");
            return true;
        } catch (Exception e) {
            LOG.error("Transaction init error!");
            LOG.error(e);
            return false;
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        LOG.info("Quotation onDependenciesReady");
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
    }

    public void initDB() throws Exception {
        //数据文件存储地址
        RocksDBService.init(quConfig.getDataRoot());
        RocksDBService.createTable(QuotationConstant.DB_MODULE_CONGIF);
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
                LOG.error("加载价格采集开始时间配置项异常",e);
            }
        }
        String endStr = quConfig.getQuoteEndHm();
        if(StringUtils.isNotBlank(startStr)) {
            try {
                String[] endNumber = endStr.split(":");
                QuotationContext.quoteEndH = Integer.parseInt(endNumber[0]);
                QuotationContext.quoteEndM = Integer.parseInt(endNumber[1]);
            } catch (NumberFormatException e) {
                LOG.error("加载价格采集结束时间配置项异常",e);
            }
        }
        QuotationContext.effectiveQuotation = quConfig.getEffectiveQuotation();
        QuotationContext.nerveBasedNuls = quConfig.getNerveBasedNuls();
        LoggerUtil.LOG.info("获取报价开始时间: {}:{}", QuotationContext.quoteStartH, QuotationContext.quoteStartM);
        LoggerUtil.LOG.info("获取报价结束时间(统计最终报价开始时间): {}:{}", QuotationContext.quoteEndH, QuotationContext.quoteEndM);
        LoggerUtil.LOG.info("effectiveQuotation : {}", QuotationContext.effectiveQuotation);
    }

}
