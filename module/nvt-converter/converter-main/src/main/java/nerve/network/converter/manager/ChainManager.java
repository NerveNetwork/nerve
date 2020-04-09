/*
 * MIT License
 *
 * Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.protocol.ProtocolLoader;
import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterDBConstant;
import nerve.network.converter.core.task.CfmTxSubsequentProcessTask;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.ConfigBean;
import nerve.network.converter.model.bo.HeterogeneousCfg;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.po.SubsequentProcessedTxPO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.storage.ConfigStorageService;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.storage.TxSubsequentProcessedTxStorageService;
import nerve.network.converter.storage.VirtualBankStorageService;
import nerve.network.converter.utils.ChainLoggerUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.io.IoUtils;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


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
    private ConfigStorageService configStorageService;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private TxSubsequentProcessedTxStorageService txSubsequentProcessedTxStorageService;
    @Autowired
    private ConverterConfig converterConfig;
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
            loadChainCacheData(chain);
            loadHeterogeneousCfgJson(chain);
            createScheduler(chain);
            chainMap.put(chainId, chain);
            chain.getLogger().debug("Chain:{} init success..", chainId);
            ProtocolLoader.load(chainId);
        }
    }

    private void initTable(Chain chain) {
        int chainId = chain.getConfig().getChainId();
        try {
            /*if(RocksDBService.existTable(ConverterDBConstant. + chainId)){
                RocksDBService.destroyTable(ConverterDBConstant. + chainId);
            }*/

            // 异构链基本信息表
            RocksDBService.createTable(ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO + chainId);

            // 虚拟银行成员表、虚拟银行变化记录对象
            RocksDBService.createTable(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chainId);
            // 确认虚拟银行变更交易 业务存储表
            RocksDBService.createTable(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chainId);
            // 确认提现交易状态业务数据表
            RocksDBService.createTable(ConverterDBConstant.DB_CONFIRM_WITHDRAWAL_PREFIX + chainId);
            // 已调用成功异构链组件的交易 持久化表 防止2次调用
            RocksDBService.createTable(ConverterDBConstant.DB_TX_SUBSEQUENT_PROCESS_PREFIX + chainId);
            // 等待调用组件的数据表
            RocksDBService.createTable(ConverterDBConstant.DB_PENDING_PREFIX + chainId);
            // 确认补贴手续费交易业务 数据表
            RocksDBService.createTable(ConverterDBConstant.DB_DISTRIBUTION_FEE_PREFIX + chainId);
            // 提案存储表
            RocksDBService.createTable(ConverterDBConstant.DB_PROPOSAL_PREFIX + chainId);
            // 提案功能投票信息表
            RocksDBService.createTable(ConverterDBConstant.DB_PROPOSAL_VOTE_PREFIX + chainId);
        } catch (Exception e) {
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                chain.getLogger().error(e);
            }
        }
    }

    /**
     * 获取加载数据库数据到缓存
     *
     * @param chain
     */
    private void loadChainCacheData(Chain chain) {
        // 加载虚拟银行成员
        Map<String, VirtualBankDirector> mapVirtualBank = virtualBankStorageService.findAll(chain);
        if (null != mapVirtualBank && !mapVirtualBank.isEmpty()) {
            chain.getMapVirtualBank().putAll(mapVirtualBank);
        }
        try {
            chain.getLogger().info("MapVirtualBank : {}", JSONUtils.obj2json(chain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("MapVirtualBank log print error ");
        }

        // 加载待调用异构组件的交易
        List<TxSubsequentProcessPO> listPending = txSubsequentProcessStorageService.findAll(chain);
        if (null != listPending && !listPending.isEmpty()) {
            chain.getPendingTxQueue().addAll(listPending);
        }
        try {
            chain.getLogger().info("PendingTxQueue : {}", JSONUtils.obj2json(chain.getPendingTxQueue()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("PendingTxQueue log print error ");
        }

        // 已调用成功异构链组件的交易
        Map<String, SubsequentProcessedTxPO> mapComponentCalledTx = txSubsequentProcessedTxStorageService.findAll(chain);
        if (null != mapComponentCalledTx && !mapComponentCalledTx.isEmpty()) {
            for (SubsequentProcessedTxPO po : mapComponentCalledTx.values()) {
                chain.getMapComponentCalledTx().put(po.getHash(), po.getHeight());
            }
        }
        try {
            chain.getLogger().info("MapComponentCalledTx : {}", JSONUtils.obj2json(chain.getMapComponentCalledTx()));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("MapComponentCalledTx log print error ");
        }
    }


    private void loadHeterogeneousCfgJson(Chain chain) throws Exception {
        String configJson = IoUtils.read(ConverterConstant.HETEROGENEOUS_CONFIG);
        List<HeterogeneousCfg> list = JSONUtils.json2list(configJson, HeterogeneousCfg.class);
        chain.setListHeterogeneous(list);
        try {
            chain.getLogger().info("HeterogeneousCfg : {}", JSONUtils.obj2json(list));
        } catch (JsonProcessingException e) {
            chain.getLogger().warn("HeterogeneousCfg log print error ");
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
            /*
            读取数据库链信息配置/Read database chain information configuration
             */
            Map<Integer, ConfigBean> configMap = configStorageService.getList();
            /*
            如果系统是第一次运行，则本地数据库没有存储链信息，此时需要从配置文件读取主链配置信息
            If the system is running for the first time, the local database does not have chain information,
            and the main chain configuration information needs to be read from the configuration file at this time.
            */
            if (configMap == null || configMap.size() == 0) {
                ConfigBean configBean = converterConfig;
                if (configBean == null) {
                    return null;
                }
                configStorageService.save(configBean, configBean.getChainId());
                configMap.put(configBean.getChainId(), configBean);
            }
            return configMap;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }


    public void createScheduler(Chain chain) {
        ScheduledThreadPoolExecutor collectorExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(ConverterConstant.CV_PENDING_THREAD));
        collectorExecutor.scheduleWithFixedDelay(new CfmTxSubsequentProcessTask(chain),
                ConverterConstant.CV_TASK_INITIALDELAY, ConverterConstant.QUTASK_PERIOD, TimeUnit.SECONDS);

        /*ScheduledThreadPoolExecutor calculatorExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(QuotationConstant.QU_CALCULATOR_THREAD));
        calculatorExecutor.scheduleAtFixedRate(new CalculatorTask(chain),
                QuotationConstant.QU_TASK_INITIALDELAY, QuotationConstant.QUTASK_PERIOD, TimeUnit.MINUTES);*/
    }


    private void initLogger(Chain chain) {
        ChainLoggerUtil.init(chain);
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
