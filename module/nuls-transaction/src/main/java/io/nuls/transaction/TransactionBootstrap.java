/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.transaction;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.base.protocol.ProtocolGroupManager;
import io.nuls.base.protocol.RegisterHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
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
import io.nuls.transaction.constant.TxConfig;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxContext;
import io.nuls.transaction.constant.TxDBConstant;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.utils.TxUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

import static io.nuls.transaction.constant.TxConstant.TX_PROTOCOL_FILE;
import static io.nuls.transaction.utils.LoggerUtil.LOG;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: Charlie
 * @date: 2019/3/4
 */
@Component
public class TransactionBootstrap extends RpcModule {

    @Autowired
    private TxConfig txConfig;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ChainManager chainManager;

    public static void main(String[] args) {
        NulsRpcModuleBootstrap.run("io.nuls", args);
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
            initTransactionContext();
            initProtocolUpdate();
            chainManager.initChain();
            TxUtil.blackHolePublicKey = HexUtil.decode(txConfig.getBlackHolePublicKey());
            ModuleHelper.init(this);
        } catch (Exception e) {
            LOG.error("Transaction init error!");
            LOG.error(e);
        }
    }

    @Override
    public boolean doStart() {
        try {
            chainManager.runChain();
            while (!isDependencieReady(ModuleE.NW.abbr)) {
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
    public void onDependenciesReady(Module module) {
        if (ModuleE.NW.abbr.equals(module.getName())) {
            RegisterHelper.registerMsg(ProtocolGroupManager.getOneProtocol());
        }
        if (ModuleE.PU.abbr.equals(module.getName())) {
            chainManager.getChainMap().keySet().forEach(RegisterHelper::registerProtocol);
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        LOG.info("Transaction onDependenciesReady");
        NulsDateUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module module) {
        if (ModuleE.BL.abbr.equals(module.getName())) {
            for (Chain chain : chainManager.getChainMap().values()) {
                chain.getProcessTxStatus().set(false);
            }
        }
        if (ModuleE.CS.abbr.equals(module.getName())) {
            for (Chain chain : chainManager.getChainMap().values()) {
                chain.getPackaging().set(false);
            }
        }
        return RpcModuleState.Ready;
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.NW),
                Module.build(ModuleE.LG),
                Module.build(ModuleE.BL),
                Module.build(ModuleE.AC),
                new Module(ModuleE.PU.abbr, ROLE)
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.TX.abbr, TxConstant.RPC_VERSION);
    }

    @Override
    public Set<String> getRpcCmdPackage() {
        return Set.of(TxConstant.TX_CMD_PATH);
    }

    /**
     * 初始化系统编码
     */
    private void initSys() {
        try {
            System.setProperty(TxConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(TxConstant.SYS_FILE_ENCODING, UTF_8.name());
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    /**
     * 根据chainId 加载特殊的协议配置
     */
    private void initModuleProtocolCfg() {
        try {
            Map map = JSONUtils.json2map(IoUtils.read(TX_PROTOCOL_FILE + txConfig.getChainId() + ".json"));
            long first = Long.parseLong(map.get("coinToPtlHeightFirst").toString());
            txConfig.setCoinToPtlHeightFirst(first);
            long second = Long.parseLong(map.get("coinToPtlHeightSecond").toString());
            txConfig.setCoinToPtlHeightSecond(second);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public void initDB() {
        try {
            //数据文件存储地址
            RocksDBService.init(txConfig.getTxDataRoot());
            RocksDBService.createTable(TxDBConstant.DB_MODULE_CONGIF);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private void initTransactionContext() {
        TxContext.TX_MAX_SIZE = txConfig.getTxMaxSize();
        TxContext.ORPHAN_LIFE_TIME_SEC = txConfig.getOrphanLifeTimeSec();
        TxContext.BLOCK_TX_TIME_RANGE_SEC = txConfig.getBlockTxTimeRangeSec();
        TxContext.UNCONFIRMED_TX_EXPIRE_SEC = txConfig.getUnconfirmedTxExpireSec();
        TxContext.COINTO_PTL_HEIGHT_FIRST = txConfig.getCoinToPtlHeightFirst();
        TxContext.COINTO_PTL_HEIGHT_SECOND = txConfig.getCoinToPtlHeightSecond();
        String accountBlockManagerPublicKeys = txConfig.getAccountBlockManagerPublicKeys();
        if (StringUtils.isNotBlank(accountBlockManagerPublicKeys)) {
            String[] split = accountBlockManagerPublicKeys.split(",");
            for (String pubkey : split) {
                TxContext.ACCOUNT_BLOCK_MANAGER_ADDRESS_SET.add(AddressTool.getAddressString(HexUtil.decode(pubkey.trim()), txConfig.getChainId()));
            }
            int size = TxContext.ACCOUNT_BLOCK_MANAGER_ADDRESS_SET.size();
            TxContext.ACCOUNT_BLOCK_MIN_SIGN_COUNT = BigDecimal.valueOf(size).multiply(BigDecimal.valueOf(6)).divide(BigDecimal.TEN, 0, RoundingMode.UP).intValue();
        }

    }

    private void initProtocolUpdate() {
        ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
        try {
            long heightVersion1_18_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_18_0"));
            TxContext.PROTOCOL_1_18_0 = heightVersion1_18_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_18_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_19_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_19_0"));
            TxContext.PROTOCOL_1_19_0 = heightVersion1_19_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_19_0", e);
            throw new RuntimeException(e);
        }
        try {
            long heightVersion1_20_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_20_0"));
            TxContext.PROTOCOL_1_20_0 = heightVersion1_20_0;
        } catch (Exception e) {
            Log.error("Failed to get height_1_20_0", e);
            throw new RuntimeException(e);
        }
    }

}
