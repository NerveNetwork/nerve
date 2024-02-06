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
import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxContext;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.utils.TxUtil;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static io.nuls.transaction.constant.TxConstant.TX_PROTOCOL_FILE;
import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * @author: Charlie
 * @date: 2019/3/4
 */
@Component
public class TransactionBootstrap implements INerveCoreBootstrap {

    @Autowired
    private NerveCoreConfig txConfig;
    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ChainManager chainManager;

    @Override
    public int order() {
        return 3;
    }

    private void init() {
        try {
            //Initialize database configuration file
            initModuleProtocolCfg();
            initTransactionContext();
            initProtocolUpdate();
            chainManager.initChain();
            TxUtil.blackHolePublicKey = HexUtil.decode(txConfig.getBlackHolePublicKey());
        } catch (Exception e) {
            LOG.error("Transaction init error!");
            LOG.error(e);
        }
    }

    @Override
    public void onDependenciesReady() {
        try {
            chainManager.runChain();
        } catch (Exception e) {
            LOG.error(e);
        }
        LOG.info("Transaction onDependenciesReady");
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.TX.abbr, TxConstant.RPC_VERSION);
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();
    }

    /**
     * according tochainId Load special protocol configurations
     */
    private void initModuleProtocolCfg() {
        try {
            Map map = JSONUtils.json2map(IoUtils.read("transaction" + File.separator + TX_PROTOCOL_FILE + txConfig.getChainId() + ".json"));
            long first = Long.parseLong(map.get("coinToPtlHeightFirst").toString());
            txConfig.setCoinToPtlHeightFirst(first);
            long second = Long.parseLong(map.get("coinToPtlHeightSecond").toString());
            txConfig.setCoinToPtlHeightSecond(second);
        } catch (Exception e) {
            Log.error(e);
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
