/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
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
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.rpc.call.BlockCall;
import network.nerve.converter.rpc.call.ConsensusCall;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
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

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        initSys();
        NulsRpcModuleBootstrap.run("io.nuls,network.nerve.converter", args);
    }

    @Override
    public void init() {
        try {
            //增加地址工具类初始化
            AddressTool.init(addressPrefixDatas);
            initDB();
            initModuleProtocolCfg();
            initConverterContext();
            chainManager.initChain();
            initHeterogeneousChainInfo();
            ModuleHelper.init(this);
        } catch (Exception e) {
            Log.error("Converter init error!", e);
            throw new RuntimeException(e);
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
            /*
             * 注册交易到交易管理模块
             */
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
            Result<Map> result = ConsensusCall.getConsensusConfig(converterConfig.getChainId());
            if(result.isSuccess()){
                Map<String, Object> configMap = result.getData();
                ConverterContext.INITIAL_VIRTUAL_BANK_COUNT = (int)configMap.get("seed_count");
            }
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
                new Module(ModuleE.CC.abbr, ROLE)
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
    private static void initSys() throws NoSuchFieldException, IllegalAccessException {
        try {
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
    }

    /**
     * 根据chainId 加载特殊的协议配置
     */
    private void initModuleProtocolCfg() {
        try {
            Map map = JSONUtils.json2map(IoUtils.read(CV_PROTOCOL_FILE + converterConfig.getChainId() + ".json"));
            long feeEffectiveHeightFirst = Long.parseLong(map.get("feeEffectiveHeightFirst").toString());
            converterConfig.setFeeEffectiveHeightFirst(feeEffectiveHeightFirst);
            long feeEffectiveHeightSecond = Long.parseLong(map.get("feeEffectiveHeightSecond").toString());
            converterConfig.setFeeEffectiveHeightSecond(feeEffectiveHeightSecond);
        } catch (Exception e) {
            Log.error(e);
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
        Log.debug("FEE_PUBKEY - chainId2 地址:{}",
                AddressTool.getStringAddressByBytes(AddressTool.getAddress(ConverterContext.FEE_PUBKEY, 2)));
        // 触发执行虚拟银行变更交易的高度周期
        ConverterContext.EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT = converterConfig.getExecuteChangeVirtualBankPeriodicHeight();
        // 提现黑洞公钥
        ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY = HexUtil.decode(converterConfig.getBlackHolePublicKey());
        Log.debug("WITHDRAWAL_BLACKHOLE_PUBKEY - chainId2 地址:{}",
                AddressTool.getStringAddressByBytes(AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, 2)));

        // 虚拟银行共识节点总数（非种子节点）
        ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED = converterConfig.getVirtualBankAgentCountWithoutSeed();
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
    }


}
