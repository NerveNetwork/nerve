/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.block.manager;

import io.nuls.block.rpc.call.TransactionCall;
import io.nuls.block.service.BlockService;
import io.nuls.block.utils.ConfigLoader;
import io.nuls.block.utils.LoggerUtil;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rockdb.service.RocksDBService;

import java.util.List;

import static io.nuls.block.constant.Constant.*;

/**
 * Chain management,Responsible for initializing each chain,working,start-up,Parameter maintenance, etc
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author: PierreLuo
 * @date: 2019-02-26
 */
@Component
public class ChainManager {

    @Autowired
    private BlockService service;

    public void initChain() throws Exception {
        //load configuration
        ConfigLoader.load();
        List<Integer> chainIds = ContextManager.CHAIN_ID_LIST;
        for (Integer chainId : chainIds) {
            initTable(chainId);
            //ProtocolLoader.load(chainId);
        }
    }

    /**
     * BlockSynchronizer
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void runChain() throws InterruptedException {
        List<Integer> chainIds = ContextManager.CHAIN_ID_LIST;
        for (Integer chainId : chainIds) {
            List<Integer> systemTypes = TransactionCall.getSystemTypes(chainId);
            while (systemTypes == null || systemTypes.isEmpty() || !systemTypes.contains(TxType.COIN_BASE)) {
                Thread.sleep(1000);
                LoggerUtil.COMMON_LOG.warn("systemTypes doesn't contains coin_base");
                systemTypes = TransactionCall.getSystemTypes(chainId);
            }
            RunnableManager.startSaver(chainId, service);
            //Service initialization
            service.init(chainId);

        }
    }

    /**
     * Stop a chain
     * Delete a chain
     *
     * @param chainId chainID/chain id
     */
    public void stopChain(int chainId) {

    }

    /**
     * Initialize Chain Related Tables
     * Initialization chain correlation table
     *
     * @param chainId
     */
    private void initTable(int chainId) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            RocksDBService.createTable(BLOCK_HEADER + chainId);
            RocksDBService.createTable(BLOCK_HEADER_INDEX + chainId);
            if (RocksDBService.existTable(CACHED_BLOCK + chainId)) {
                RocksDBService.destroyTable(CACHED_BLOCK + chainId);
            }
            RocksDBService.createTable(CACHED_BLOCK + chainId);
        } catch (Exception e) {
            logger.error(e);
        }
    }

}
