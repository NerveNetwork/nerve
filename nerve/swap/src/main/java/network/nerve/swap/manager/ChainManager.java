/*
 * MIT License
 *
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.swap.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.protocol.ProtocolLoader;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.cache.impl.FarmCacheImpl;
import network.nerve.swap.config.ConfigBean;
import network.nerve.swap.config.SwapConfig;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.dto.PairsP17Info;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.storage.SwapStablePairStorageService;
import network.nerve.swap.utils.ChainLoggerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 链管理类,负责各条链的初始化,运行,启动,参数维护等
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author qinyifeng
 * @date 2018/12/11
 */
@Component
public class ChainManager {

    @Autowired
    private SwapConfig swapConfig;
    @Autowired
    private SwapPairStorageService swapPairStorageService;
    @Autowired
    private SwapStablePairStorageService swapStablePairStorageService;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapHelper swapHelper;

    private Map<Integer, Chain> chainMap = new ConcurrentHashMap<>();

    /**
     * 初始化并启动链
     * Initialize and start the chain
     */
    public void initChain() throws Exception {
        Map<Integer, ConfigBean> configMap = configChain();
        if (configMap == null || configMap.size() == 0) {
            return;
        }
        for (Map.Entry<Integer, ConfigBean> entry : configMap.entrySet()) {
            Chain chain = new Chain();
            int chainId = entry.getKey();
            chain.setConfig(entry.getValue());
            initLogger(chain);
            initTable(chain);
            loadPairsCache(chain);
            initCache(chain);
            chainMap.put(chainId, chain);
            chain.getLogger().debug("Chain:{} init success..", chainId);
            ProtocolLoader.load(chainId);
            if(chainId == swapConfig.getChainId()) {
                swapHelper.setNerveChain(chain);
            }
        }
    }

    private void initCache(Chain chain) {
        FarmCacheImpl.init(chain);
    }

    private void initTable(Chain chain) {
        int chainId = chain.getConfig().getChainId();
        try {
            RocksDBService.createTable(SwapDBConstant.DB_NAME_SWAP + chainId);
            RocksDBService.createTable(SwapDBConstant.DB_NAME_FARM + chainId);
            RocksDBService.createTable(SwapDBConstant.DB_NAME_FARM_USER + chainId);

        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                chain.getLogger().error(e);
            }
        }
    }

    private void loadPairsCache(Chain chain) throws Exception {
        String configJson;
        if (swapConfig.isAllPairRelationMainNet()) {
            configJson = IoUtils.read(SwapConstant.PAIRS_MAINNET);
        } else {
            configJson = IoUtils.read(SwapConstant.PAIRS_TESTNET);
        }
        PairsP17Info info = JSONUtils.json2pojo(configJson, PairsP17Info.class);
        Collection<String> pairs = swapPairStorageService.findAllPairs(chain.getChainId());
        Collection<String> stablePairs = swapStablePairStorageService.findAllPairs(chain.getChainId());
        Set<String> pairsSet = new HashSet<>();
        pairsSet.addAll(pairs);
        pairsSet.addAll(info.getSwap());
        for (String pair : pairsSet) {
            swapPairCache.get(pair);
        }

        Set<String> stablePairsSet = new HashSet<>();
        stablePairsSet.addAll(stablePairs);
        stablePairsSet.addAll(info.getStable());
        for (String stablePair : stablePairsSet) {
            stableSwapPairCache.get(stablePair);
        }

        try {
            chain.getLogger().info("PairsP17Info : {}", JSONUtils.obj2json(info));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("PairsP17Info log print error ");
        }
    }


    /**
     * 停止一条链
     * Delete a chain
     *
     * @param chainId 链ID/chain id
     */
    public void stopChain(int chainId) {

    }

    /**
     * 读取配置文件创建并初始化链
     * Read the configuration file to create and initialize the chain
     */
    private Map<Integer, ConfigBean> configChain() {
        try {
            Map<Integer, ConfigBean> configMap = new HashMap<>();
            ConfigBean configBean = swapConfig;
            if (configBean == null) {
                return null;
            }
            configMap.put(configBean.getChainId(), configBean);
            return configMap;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    private void initLogger(Chain chain) {
        ChainLoggerUtil.init(chain);
        SwapContext.logger = chain.getLogger();
    }

    public static void chainHandle(int chainId, int blockType) {
        // 设置交易模块请求区块处理模式, 打包区块 - 0, 验证区块 - 1
        Chain.putCurrentThreadBlockType(blockType);
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
