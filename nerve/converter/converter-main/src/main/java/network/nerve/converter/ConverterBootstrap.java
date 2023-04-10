/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.rpc.call.BlockCall;
import network.nerve.converter.rpc.call.ConsensusCall;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static network.nerve.converter.constant.ConverterConstant.*;

/**
 * @author: Loki
 * @date: 2019/3/4
 */
@Component
public class ConverterBootstrap extends RpcModule {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private HeterogeneousChainManager heterogeneousChainManager;

    public static void main(String[] args) throws Exception {
        initSys();
        NulsRpcModuleBootstrap.run("io.nuls,network.nerve.converter", args);
    }

    @Override
    public void init() {
        try {
            //增加地址工具类初始化
            AddressTool.init(addressPrefixDatas);
            initDB();
            initProtocolUpdate();
            initConverterContext();
            chainManager.initChain();
            initHeterogeneousChainInfo();
            ModuleHelper.init(this);
        } catch (Exception e) {
            Log.error("Converter init error!", e);
            throw new RuntimeException(e);
        }
    }

    private void initProtocolUpdate() {
        ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
        try {
            long heightVersion1_6_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_6_0"));
            // 第三次协议升级高度 提现异构链手续费改为(自定义(不低于最小值) + 追加的方式)
            ConverterContext.FEE_ADDITIONAL_HEIGHT = heightVersion1_6_0;
            // 协议升级高度 修改提现和充值交易协议,增加异构链id
            ConverterContext.WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = heightVersion1_6_0;
            ConverterContext.protocolHeightMap.put(6, heightVersion1_6_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_6_0", e);
        }
        try {
            long heightVersion1_8_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_8_0"));
            // v1.8.0 协议升级高度 支持火币生态链跨链
            ConverterContext.PROTOCOL_8_HUOBI_CROSS_CHAIN_HEIGHT = heightVersion1_8_0;
            ConverterContext.protocolHeightMap.put(8, heightVersion1_8_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_8_0", e);
        }
        try {
            long heightVersion1_11_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_11_0"));
            // v1.11.0 协议升级高度 支持欧科生态链跨链
            ConverterContext.PROTOCOL_11_OKT_CROSS_CHAIN_HEIGHT = heightVersion1_11_0;
            ConverterContext.protocolHeightMap.put(11, heightVersion1_11_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_11_0", e);
        }
        try {
            long heightVersion1_12_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_12_0"));
            // v1.12.0 协议升级高度 支持转账即销毁部分的ERC20
            ConverterContext.PROTOCOL_12_ERC20_OF_TRANSFER_BURN_HEIGHT = heightVersion1_12_0;
            ConverterContext.protocolHeightMap.put(12, heightVersion1_12_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_12_0", e);
        }
        try {
            long heightVersion1_13_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_13_0"));
            // v1.13.0 协议升级高度 支持异构链ERC20充值的新验证方式，支持Harmony,Polygon,Kucoin生态链跨链
            ConverterContext.PROTOCOL_13_NEW_VALIDATION_OF_ERC20 = heightVersion1_13_0;
            ConverterContext.PROTOCOL_13_ONE_CROSS_CHAIN_HEIGHT = heightVersion1_13_0;
            ConverterContext.PROTOCOL_13_POLYGON_CROSS_CHAIN_HEIGHT = heightVersion1_13_0;
            ConverterContext.PROTOCOL_13_KUCOIN_CROSS_CHAIN_HEIGHT = heightVersion1_13_0;
            ConverterContext.protocolHeightMap.put(13, heightVersion1_13_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_13_0", e);
        }
        try {
            long heightVersion1_14_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_14_0"));
            // v1.14.0 协议升级高度
            ConverterContext.PROTOCOL_1_14_0 = heightVersion1_14_0;
            ConverterContext.protocolHeightMap.put(14, heightVersion1_14_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_14_0", e);
        }
        try {
            long heightVersion1_15_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_15_0"));
            // v1.15.0 协议升级高度 支持波场生态链跨链
            ConverterContext.PROTOCOL_15_TRX_CROSS_CHAIN_HEIGHT = heightVersion1_15_0;
            ConverterContext.protocolHeightMap.put(15, heightVersion1_15_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_15_0", e);
        }
        try {
            long heightVersion1_16_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_16_0"));
            // v1.16.0 协议升级高度
            ConverterContext.PROTOCOL_1_16_0 = heightVersion1_16_0;
            ConverterContext.protocolHeightMap.put(16, heightVersion1_16_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_16_0", e);
        }
        try {
            long heightVersion1_21_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_21_0"));
            // v1.21.0 协议升级高度
            ConverterContext.PROTOCOL_1_21_0 = heightVersion1_21_0;
            ConverterContext.protocolHeightMap.put(21, heightVersion1_21_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_21_0", e);
        }
        try {
            long heightVersion1_22_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_22_0"));
            // v1.22.0 协议升级高度
            ConverterContext.PROTOCOL_1_22_0 = heightVersion1_22_0;
            ConverterContext.protocolHeightMap.put(22, heightVersion1_22_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_22_0", e);
        }
        try {
            long heightVersion1_23_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_23_0"));
            // v1.23.0 协议升级高度
            ConverterContext.PROTOCOL_1_23_0 = heightVersion1_23_0;
            ConverterContext.protocolHeightMap.put(23, heightVersion1_23_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_23_0", e);
        }
        try {
            long heightVersion1_24_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_24_0"));
            // v1.24.0 协议升级高度
            ConverterContext.PROTOCOL_1_24_0 = heightVersion1_24_0;
            ConverterContext.protocolHeightMap.put(24, heightVersion1_24_0);
        } catch (Exception e) {
            Log.warn("Failed to get height_1_4_0", e);
        }
    }

    @Override
    public boolean doStart() {
        try {
            Log.info("Converter Ready...");
            return true;
        } catch (Exception e) {
            Log.error("Converter init error!");
            Log.error(e);
            return false;
        }
    }


    @Override
    public void onDependenciesReady(Module module) {
        Log.info("dependencies [{}] ready", module.getName());
        if (module.getName().equals(ModuleE.TX.abbr)) {
            Map<Integer, Chain> chainMap = chainManager.getChainMap();
            for (Chain chain : chainMap.values()) {
                int chainId = chain.getChainId();
                boolean registerTx = RegisterHelper.registerTx(chainId, ProtocolGroupManager.getCurrentProtocol(chainId));
                Log.info("register tx type to tx module, chain id is {}, result is {}", chainId, registerTx);
            }
        }
        if (ModuleE.NW.abbr.equals(module.getName())) {
            RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
        }
        if (ModuleE.PU.abbr.equals(module.getName())) {
            chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
            Log.info("register to protocol-update module");
        }
        if (ModuleE.BL.abbr.equals(module.getName())) {
            chainManager.getChainMap().values().forEach(BlockCall::subscriptionNewBlockHeight);
            Log.info("subscription new block height");
        }

        if (ModuleE.LG.abbr.equals(module.getName())) {
            Log.info("onDependenciesReady ledger");
            heterogeneousChainManager.init2LedgerAsset();
        }

        if (ModuleE.CS.abbr.equals(module.getName())) {
            // 获取种子节点个数
            // 根据协议来
            Chain chain = chainManager.getChainMap().get(converterConfig.getChainId());
            if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                Result<Map> result = ConsensusCall.getConsensusConfig(converterConfig.getChainId());
                if(result.isSuccess()){
                    Map<String, Object> configMap = result.getData();
                    ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT = (int)configMap.get("seed_count");
                }
            } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT = ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST.size();
            }
            ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED =
                    ConverterContext.VIRTUAL_BANK_AGENT_TOTAL - ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT;
            chain.getLogger().debug("初始化: 虚拟银行总席位:{}, 虚拟银行种子成员数:{}, 虚拟银行非种子成员数:{}",
                    ConverterContext.VIRTUAL_BANK_AGENT_TOTAL,
                    ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT,
                    ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED);
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        Log.info("all dependency module ready");
        NulsDateUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module module) {
        return RpcModuleState.Ready;
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.CS),
                Module.build(ModuleE.TX),
                Module.build(ModuleE.NW),
                Module.build(ModuleE.LG),
                Module.build(ModuleE.BL),
                Module.build(ModuleE.AC),
                new Module(ModuleE.PU.abbr, ROLE),
                new Module(ModuleE.QU.abbr, ROLE)
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.CV.abbr, ConverterConstant.RPC_VERSION);
    }

    @Override
    public Set<String> getRpcCmdPackage() {
        return Set.of(ConverterConstant.CONVERTER_CMD_PATH);
    }

    /**
     * 初始化系统编码
     */
    private static void initSys() throws Exception {
        try {
            Class.forName("org.web3j.protocol.core.methods.response.Transaction");
            System.setProperty(ConverterConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(ConverterConstant.SYS_FILE_ENCODING, UTF_8.name());
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
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger("org.web3j.protocol.http.HttpService");
            logger.setLevel(Level.INFO);
        } catch (Exception e) {
            // skip it
            Log.warn("log level setting error", e);
        }
    }

    /**
     * 初始化和注册异构链
     */
    private void initHeterogeneousChainInfo() throws Exception {
        heterogeneousChainManager.initHeterogeneousChainInfo();
    }

    private void initDB() throws Exception {
        // 数据文件存储地址
        RocksDBService.init(converterConfig.getTxDataRoot());
        RocksDBService.createTable(ConverterDBConstant.DB_MODULE_CONGIF);
        // 异构链基本信息表
        RocksDBService.createTable(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO);
    }

    /**
     * 初始化配置项上下文
     */
    private void initConverterContext() {
        // 触发初始化虚拟银行的区块高度
        ConverterContext.INIT_VIRTUAL_BANK_HEIGHT = converterConfig.getInitVirtualBankHeight();
        // 手续费汇集分发公钥
        ConverterContext.FEE_PUBKEY = HexUtil.decode(converterConfig.getFeePubkey());
        // 触发执行虚拟银行变更交易的高度周期
        ConverterContext.EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT = converterConfig.getExecuteChangeVirtualBankPeriodicHeight();
        // 提现黑洞公钥
        ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY = HexUtil.decode(converterConfig.getBlackHolePublicKey());
        // 虚拟银行共识节点总数（包含种子节点成员）
        ConverterContext.VIRTUAL_BANK_AGENT_TOTAL = converterConfig.getVirtualBankAgentTotal();
        // 提案花费
        ConverterContext.PROPOSAL_PRICE = converterConfig.getProposalPrice();
        // 签名拜占庭比例
        ConverterContext.BYZANTINERATIO = converterConfig.getByzantineRatio();
        // 提案可投票持续区块数
        ConverterContext.PROPOSAL_VOTE_TIME_BLOCKS = DAY_BLOCKS * converterConfig.getProposalVotingDays();
        // 异构链交易手续费补贴 第一次协议升级高度
        ConverterContext.FEE_EFFECTIVE_HEIGHT_FIRST = converterConfig.getFeeEffectiveHeightFirst();
        // 第二次协议升级高度 异构链交易手续费补贴
        ConverterContext.FEE_EFFECTIVE_HEIGHT_SECOND = converterConfig.getFeeEffectiveHeightSecond();
        // 设置NVT价格key, 通过喂价模块获取价格
        ConverterContext.priceKeyMap.put(AssetName.NVT.name(), ORACLE_KEY_NVT_PRICE);
        // 初始化虚拟银行公钥(异构链版本2开始)
        List<String> seedPubKeyList = List.of(converterConfig.getInitVirtualBankPubKeyList().split(ConverterConstant.SEED_PUBKEY_SEPARATOR));
        for(String pubKey : seedPubKeyList) {
            ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST.add(pubKey.toLowerCase());
        }
    }
}
