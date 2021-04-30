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
package network.nerve.converter.heterogeneouschain.okt.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgBlockHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgWaitingTxInvokeDataHandler;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import network.nerve.converter.heterogeneouschain.okt.callback.OktCallBackManager;
import network.nerve.converter.heterogeneouschain.okt.constant.OktDBConstant;
import network.nerve.converter.heterogeneouschain.okt.context.OktContext;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



/**
 * OKT组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("oktRegister")
public class OktRegister implements IHeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private OktCallBackManager oktCallBackManager;

    private OktContext oktContext;
    private HtgListener htgListener;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    private HtgTxInvokeInfoStorageService htgTxInvokeInfoStorageService;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    private ScheduledThreadPoolExecutor confirmTxExecutor;
    private ScheduledThreadPoolExecutor waitingTxExecutor;
    private boolean isInitial = false;
    private boolean newProcessActivated = false;
    private BeanMap beanMap = new BeanMap();

    @Override
    public int order() {
        return 5;
    }

    @Override
    public int getChainId() {
        return OktContext.HTG_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            OktContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            OktContext.setConfig(config);
            // 初始化实例
            initBean();
            // 初始化默认API
            initDefualtAPI();
            // 解析HT API URL
            initEthWalletRPC();
            // 存放nerveChainId
            OktContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(OktDBConstant.DB_OKT);
            // 初始化待确认任务队列
            initUnconfirmedTxQueue();
            // 初始化地址过滤集合
            initFilterAddresses();
        }
    }

    private void initBean() {
        try {
            beanMap.add(HtgDocking.class, (OktContext.DOCKING = new HtgDocking()));
            beanMap.add(HtgContext.class, (oktContext = new OktContext()));
            beanMap.add(HtgListener.class, (htgListener = new HtgListener()));

            beanMap.add(ConverterConfig.class, converterConfig);
            beanMap.add(HtgCallBackManager.class, oktCallBackManager);

            beanMap.add(HtgWalletApi.class);
            beanMap.add(HtgBlockHandler.class);
            beanMap.add(HtgConfirmTxHandler.class);
            beanMap.add(HtgWaitingTxInvokeDataHandler.class);
            beanMap.add(HtgAccountHelper.class);
            beanMap.add(HtgAnalysisTxHelper.class);
            beanMap.add(HtgBlockAnalysisHelper.class);
            beanMap.add(HtgCommonHelper.class);
            beanMap.add(HtgERC20Helper.class);
            beanMap.add(HtgInvokeTxHelper.class);
            beanMap.add(HtgLocalBlockHelper.class);
            beanMap.add(HtgParseTxHelper.class);
            beanMap.add(HtgPendingTxHelper.class);
            beanMap.add(HtgResendHelper.class);
            beanMap.add(HtgStorageHelper.class);
            beanMap.add(HtgUpgradeContractSwitchHelper.class);

            beanMap.add(HtgAccountStorageService.class, new HtgAccountStorageServiceImpl(oktContext, OktDBConstant.DB_OKT));
            beanMap.add(HtgBlockHeaderStorageService.class, new HtgBlockHeaderStorageServiceImpl(oktContext, OktDBConstant.DB_OKT));
            beanMap.add(HtgERC20StorageService.class, new HtgERC20StorageServiceImpl(oktContext, OktDBConstant.DB_OKT));
            beanMap.add(HtgMultiSignAddressHistoryStorageService.class, (htgMultiSignAddressHistoryStorageService = new HtgMultiSignAddressHistoryStorageServiceImpl(oktContext, OktDBConstant.DB_OKT)));
            beanMap.add(HtgTxInvokeInfoStorageService.class, (htgTxInvokeInfoStorageService = new HtgTxInvokeInfoStorageServiceImpl(oktContext, OktDBConstant.DB_OKT)));
            beanMap.add(HtgTxRelationStorageService.class, new HtgTxRelationStorageServiceImpl(oktContext, OktDBConstant.DB_OKT));
            beanMap.add(HtgTxStorageService.class, new HtgTxStorageServiceImpl(oktContext, OktDBConstant.DB_OKT));
            beanMap.add(HtgUnconfirmedTxStorageService.class, (htgUnconfirmedTxStorageService = new HtgUnconfirmedTxStorageServiceImpl(oktContext, OktDBConstant.DB_OKT)));

            Collection<Object> values = beanMap.values();
            for (Object value : values) {
                if (value instanceof BeanInitial) {
                    BeanInitial beanInitial = (BeanInitial) value;
                    beanInitial.init(beanMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(OktContext.config.getChainId());
        info.setChainName(OktContext.config.getSymbol());
        info.setMultySignAddress(OktContext.config.getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        return OktContext.DOCKING;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
            // 监听多签地址交易
            htgListener.addListeningAddress(multiSigAddress);
            // 管理回调函数实例
            oktCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            oktCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            oktCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // 存放CORE查询API实例
            OktContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            // 更新多签地址
            OktContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // 保存当前多签地址到多签地址历史列表中
            htgMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        OktContext.logger.info("{} 注册完成.", OktContext.config.getSymbol());
    }

    /**
     * 停止当前区块解析任务与待确认交易任务
     */
    public void shutDownScheduled() {
        if (blockSyncExecutor != null && !blockSyncExecutor.isShutdown()) {
            blockSyncExecutor.shutdown();
        }
        if (confirmTxExecutor != null && !confirmTxExecutor.isShutdown()) {
            confirmTxExecutor.shutdown();
        }
        if (waitingTxExecutor != null && !waitingTxExecutor.isShutdown()) {
            waitingTxExecutor.shutdown();
        }
    }

    private void initDefualtAPI() throws Exception {
        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        htgWalletApi.init(ethWalletRpcProcessing(OktContext.config.getCommonRpcAddress()));
    }

    private void initEthWalletRPC() {
        String orderRpcAddresses = OktContext.config.getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                OktContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = OktContext.config.getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                OktContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
    }

    private String ethWalletRpcProcessing(String rpc) {
        if (StringUtils.isBlank(rpc)) {
            return rpc;
        }
        rpc = rpc.trim();
        return rpc;
    }

    private void initFilterAddresses() {
        String filterAddresses = OktContext.config.getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                OktContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    OktContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // 把待确认的交易加入到监听交易hash列表中
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        OktContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("okt-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgBlockHandler.class), 60, 5, TimeUnit.SECONDS);

        confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("okt-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgConfirmTxHandler.class), 60, 10, TimeUnit.SECONDS);

        waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("okt-waiting-tx"));
        waitingTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class), 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    OktContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        OktContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
