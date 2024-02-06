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
package io.nuls.transaction.manager;

import io.nuls.common.CommonContext;
import io.nuls.common.ConfigBean;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.constant.TxDBConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.po.TransactionNetPO;
import io.nuls.transaction.utils.LoggerUtil;

import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import static io.nuls.transaction.constant.TxConstant.TX_UNVERIFIED_QUEUE_SIZE;

/**
 * Chain management,Responsible for initializing each chain,working,start-up,Parameter maintenance, etc
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author qinyifeng
 * @date 2018/12/11
 */
@Component
public class ChainManager {
    @Autowired
    private NerveCoreConfig txConfig;
    @Autowired
    private SchedulerManager schedulerManager;

    private Map<Integer, Chain> chainMap = new ConcurrentHashMap<>();

    /**
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void initChain() throws Exception {
        Map<Integer, ConfigBean> configMap = CommonContext.CONFIG_BEAN_MAP;
        if (configMap == null || configMap.size() == 0) {
            return;
        }
        for (Map.Entry<Integer, ConfigBean> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            chain.setConfig(entry.getValue());
            initLogger(chain);
            initTable(chain);
            chainMap.put(chainId, chain);
            chain.getLogger().debug("Chain:{} init success..", chainId);
        }
    }

    /**
     * Initialize and start the chain
     * Initialize and start the chain
     */
    public void runChain() throws Exception {

        for (Chain chain: chainMap.values()) {
            initCache(chain);
            schedulerManager.createTransactionScheduler(chain);
            chainMap.put(chain.getChainId(), chain);
            chain.getLogger().debug("Chain:{} runChain success..", chain.getChainId());
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
     * @param chain
     */
    private void initTable(Chain chain) {
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getConfig().getChainId();
        try {
            RocksDBService.init(txConfig.getDataPath() + File.separator + ModuleE.TX.name);
            //Unconfirmed table
            if(RocksDBService.existTable(TxDBConstant.DB_TRANSACTION_UNCONFIRMED_PREFIX + chainId)){
                RocksDBService.destroyTable(TxDBConstant.DB_TRANSACTION_UNCONFIRMED_PREFIX + chainId);
            }

            /*
            Create confirmed transaction table
            Create confirmed transaction table
            */
            RocksDBService.createTable(TxDBConstant.DB_TRANSACTION_CONFIRMED_PREFIX + chainId);


            /*
            Verified Unpackaged Transactions Unconfirmed
            Verified transaction
            */
            RocksDBService.createTable(TxDBConstant.DB_TRANSACTION_UNCONFIRMED_PREFIX + chainId);

            /*
            Lock account table
             */
            RocksDBService.createTable(TxDBConstant.DB_LOCKED_ADDRESS + chainId);


        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                logger.error(e);
            }
        }
    }

    /**
     * Initialize chain cache data
     * Initialize chain caching entity
     *
     * @param chain chain info
     */
    private void initCache(Chain chain) {
        BlockingDeque<TransactionNetPO> unverifiedQueue = new LinkedBlockingDeque<>(TX_UNVERIFIED_QUEUE_SIZE);
        chain.setUnverifiedQueue(unverifiedQueue);
    }

    private void initLogger(Chain chain) {
        LoggerUtil.init(chain);
    }

    public Map<Integer, Chain> getChainMap() {
        return chainMap;
    }

    public void setChainMap(Map<Integer, Chain> chainMap) {
        this.chainMap = chainMap;
    }

    public boolean containsKey(int key) {
        return this.chainMap.containsKey(key);
    }

    public Chain getChain(int key) {
        return this.chainMap.get(key);
    }
}
