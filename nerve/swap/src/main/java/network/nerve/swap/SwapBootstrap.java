/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.swap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.base.protocol.cmd.TransactionDispatcher;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
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
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.rpc.call.BlockCall;
import network.nerve.swap.rpc.call.TransactionCall;
import network.nerve.swap.storage.SwapStablePairStorageService;
import network.nerve.swap.tx.common.TransactionCommitAdvice;
import network.nerve.swap.tx.common.TransactionRollbackAdvice;
import network.nerve.swap.tx.common.TransactionValidatorAdvice;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: PierreLuo
 * @date: 2021-03-11
 */
@Component
public class SwapBootstrap extends RpcModule {

    @Autowired
    private SwapConfig swapConfig;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        initSys();
        NulsRpcModuleBootstrap.run("network.nerve.swap,io.nuls", args);
    }

    @Override
    public void init() {
        try {
            super.init();
            //Add address tool class initialization
            AddressTool.init(addressPrefixDatas);
            initDB();
            initProtocolUpdate();
            chainManager.initChain();
            initContext();
            ModuleHelper.init(this);
        } catch (Exception e) {
            Log.error("module init error!", e);
            throw new RuntimeException(e);
        }
    }

    private void initProtocolUpdate() {
        ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
        try {
            long heightVersion1_13_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_13_0"));
            SwapContext.PROTOCOL_UPGRADE_HEIGHT = heightVersion1_13_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_13_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_15_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_15_0"));
            SwapContext.PROTOCOL_1_15_0 = heightVersion1_15_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_15_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_16_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_16_0"));
            SwapContext.PROTOCOL_1_16_0 = heightVersion1_16_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_16_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_17_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_17_0"));
            SwapContext.PROTOCOL_1_17_0 = heightVersion1_17_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_17_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_21_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_21_0"));
            SwapContext.PROTOCOL_1_21_0 = heightVersion1_21_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_21_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_22_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_22_0"));
            SwapContext.PROTOCOL_1_22_0 = heightVersion1_22_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_22_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_24_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_24_0"));
            SwapContext.PROTOCOL_1_24_0 = heightVersion1_24_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_24_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_26_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_26_0"));
            SwapContext.PROTOCOL_1_26_0 = heightVersion1_26_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_26_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_28_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_28_0"));
            SwapContext.PROTOCOL_1_28_0 = heightVersion1_28_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_28_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_29_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_29_0"));
            SwapContext.PROTOCOL_1_29_0 = heightVersion1_29_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_29_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_31_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_31_0"));
            SwapContext.PROTOCOL_1_31_0 = heightVersion1_31_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_31_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_35_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_35_0"));
            SwapContext.PROTOCOL_1_35_0 = heightVersion1_35_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_35_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_36_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_36_0"));
            SwapContext.PROTOCOL_1_36_0 = heightVersion1_36_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_36_0", e);
            throw new RuntimeException(e);
        }
        try {
            String password = configurationLoader.getValue(ModuleE.Constant.CONSENSUS, "password");
            SwapContext.PASSWORD = password;
        } catch (Exception e) {
            Log.error("Failed to get password", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean doStart() {
        try {
            while (!isDependencieReady(ModuleE.NC.abbr)){
                Log.info("wait nerve-core modules ready");
                Thread.sleep(2000L);
            }
            TransactionDispatcher dispatcher = SpringLiteContext.getBean(TransactionDispatcher.class);
            TransactionCommitAdvice commitAdvice = SpringLiteContext.getBean(TransactionCommitAdvice.class);
            TransactionRollbackAdvice rollbackAdvice = SpringLiteContext.getBean(TransactionRollbackAdvice.class);
            TransactionValidatorAdvice validatorAdvice = SpringLiteContext.getBean(TransactionValidatorAdvice.class);
            dispatcher.register(commitAdvice, rollbackAdvice, validatorAdvice);
            Log.info("module chain do start");
            return true;
        } catch (Exception e) {
            Log.error("module start error!");
            Log.error(e);
            return false;
        }
    }


    @Override
    public void onDependenciesReady(Module module) {
        Log.info("dependencies [{}] ready", module.getName());
        if (module.getName().equals(ModuleE.NC.abbr)) {
            Map<Integer, Chain> chainMap = chainManager.getChainMap();
            for (Chain chain : chainMap.values()) {
                int chainId = chain.getConfig().getChainId();
                boolean registerTx = RegisterHelper.registerTx(chainId, ProtocolGroupManager.getCurrentProtocol(chainId));
                // Notify the trading module,SWAPSystem transactions of modules
                setSwapGenerateTxTypes(chainId);
                Log.info("register tx type to tx module, chain id is {}, result is {}", chainId, registerTx);
            }
            RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
            chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
            Log.info("register to protocol-update module");
            chainManager.getChainMap().values().forEach(BlockCall::subscriptionNewBlockHeight);
            Log.info("subscription new block height");
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
                Module.build(ModuleE.NC),
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.SW.abbr, SwapConstant.RPC_VERSION);
    }

    @Override
    public Set<String> getRpcCmdPackage() {
        return Set.of(SwapConstant.SWAP_CMD_PATH);
    }

    /**
     * Initialize system encoding
     */
    private static void initSys() throws NoSuchFieldException, IllegalAccessException {
        try {
            System.setProperty(SwapConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(SwapConstant.SYS_FILE_ENCODING, UTF_8.name());
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


    private void initDB() throws Exception {
        // Data file storage address
        RocksDBService.init(swapConfig.getPathRoot());

    }

    /**
     * Initialize configuration item context
     */
    private void initContext() {
        // Withdrawal of black hole public key
        SwapContext.BLACKHOLE_PUBKEY = HexUtil.decode(swapConfig.getBlackHolePublicKey());
        SwapContext.BLACKHOLE_ADDRESS = AddressTool.getAddressByPubKeyStr(swapConfig.getBlackHolePublicKey(), swapConfig.getChainId());
        // System receiving address for handling fee rewards
        SwapContext.AWARD_FEE_SYSTEM_ADDRESS = AddressTool.getAddressByPubKeyStr(swapConfig.getAwardFeeSystemAddressPublicKey(), swapConfig.getChainId());
        SwapContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0 = AddressTool.getAddressByPubKeyStr(swapConfig.getAwardFeeSystemAddressPublicKeyProtocol17(), swapConfig.getChainId());
        SwapContext.AWARD_FEE_DESTRUCTION_ADDRESS = AddressTool.getAddressByPubKeyStr(swapConfig.getAwardFeeDestructionAddressPublicKey(), swapConfig.getChainId());
        // Initialize AggregationstableCombining
        String stablePairAddressSetStr = swapConfig.getStablePairAddressInitialSet();
        if (StringUtils.isNotBlank(stablePairAddressSetStr)) {
            String[] array = stablePairAddressSetStr.split(",");
            if (array.length == 0) {
                return;
            }
            // Delayed caching: Managing stablecoin transactions-Used forSwaptransaction
            for (String stable : array) {
                stable = stable.trim();
                SwapContext.stableCoinGroup.addAddress(stable, stableSwapPairCache, swapStablePairStorageService, swapConfig);
            }
        }
    }

    private void setSwapGenerateTxTypes(int currentChainId) {
        List<Integer> list = List.of(
                TxType.SWAP_SYSTEM_DEAL,
                TxType.SWAP_SYSTEM_REFUND);
        List<Integer> resultList = new ArrayList<>();
        resultList.addAll(list);
        try {
            TransactionCall.setSwapGenerateTxTypes(currentChainId, resultList);
        } catch (NulsException e) {
            Log.warn("Exception in obtaining transaction type for smart contract generation", e);
        }
    }
}
