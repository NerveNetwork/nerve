/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger;

import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.manager.LedgerChainManager;
import io.nuls.ledger.utils.LedgerUtil;
import io.nuls.ledger.utils.LoggerUtil;

/**
 * @author: Niels Wang
 * @date: 2018/10/15
 */
@Component
public class LedgerBootstrap implements INerveCoreBootstrap {
    @Autowired
    NerveCoreConfig ledgerConfig;

    @Override
    public int order() {
        return 5;
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.LG.abbr, "1.0");
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();
    }

    private void init() {
        try {
            LedgerUtil.initChainCfg(ledgerConfig.getChainId());
            LedgerConstant.UNCONFIRM_NONCE_EXPIRED_TIME = ledgerConfig.getUnconfirmedTxExpired();
            LedgerConstant.DEFAULT_ENCODING = ledgerConfig.getEncoding();
            LedgerConstant.blackHolePublicKey = HexUtil.decode(ledgerConfig.getBlackHolePublicKey());
            initProtocolUpdate();
            LedgerChainManager ledgerChainManager = SpringLiteContext.getBean(LedgerChainManager.class);
            ledgerChainManager.initChains();
            LoggerUtil.COMMON_LOG.info("Ledger data init complete!");
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            LoggerUtil.COMMON_LOG.error("start fail...");
            System.exit(-1);
        }

    }

    private void initProtocolUpdate() {
        ConfigurationLoader configurationLoader = SpringLiteContext.getBean(ConfigurationLoader.class);
        try {
            long heightVersion1_17_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_17_0"));
            // v1.17.0 Protocol upgrade height
            LedgerConstant.PROTOCOL_1_17_0 = heightVersion1_17_0;
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.warn("Failed to get height_1_17_0", e);
        }
        try {
            long heightVersion1_32_0 = Long.parseLong(configurationLoader.getValue(ModuleE.Constant.PROTOCOL_UPDATE, "height_1_32_0"));
            // v1.32.0 Protocol upgrade height
            LedgerConstant.PROTOCOL_1_32_0 = heightVersion1_32_0;
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.warn("Failed to get height_1_32_0", e);
        }
    }

    @Override
    public void onDependenciesReady() {
        try {
            LoggerUtil.COMMON_LOG.info("Ledger onDependenciesReady");
            //ProtocolLoader.load(ledgerConfig.getChainId());
            /*Processing block information*/
            LedgerChainManager ledgerChainManager = SpringLiteContext.getBean(LedgerChainManager.class);
            ledgerChainManager.syncBlockHeight();
            NulsDateUtils.getInstance().start(5 * 60 * 1000);
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
            System.exit(-1);

        }
    }
}
