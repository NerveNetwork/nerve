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
package network.nerve.converter.heterogeneouschain.one.register;

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
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgBlockHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgRpcAvailableHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgWaitingTxInvokeDataHandler;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxInvokeInfoStorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.heterogeneouschain.one.callback.OneCallBackManager;
import network.nerve.converter.heterogeneouschain.one.constant.OneDBConstant;
import network.nerve.converter.heterogeneouschain.one.context.OneContext;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * ONE组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("oneRegister")
public class OneRegister implements IHeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private OneCallBackManager oneCallBackManager;

    private OneContext oneContext = new OneContext();
    private HtgListener htgListener;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    private HtgTxInvokeInfoStorageService htgTxInvokeInfoStorageService;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    //private ScheduledThreadPoolExecutor confirmTxExecutor;
    //private ScheduledThreadPoolExecutor waitingTxExecutor;
    //private ScheduledThreadPoolExecutor rpcAvailableExecutor;
    private boolean isInitial = false;
    private boolean newProcessActivated = false;
    private BeanMap beanMap = new BeanMap();

    @Override
    public int order() {
        return 6;
    }

    @Override
    public int getChainId() {
        return oneContext.HTG_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            oneContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            oneContext.setConfig(config);
            // 初始化实例
            initBean();
            // 初始化默认API
            initDefualtAPI();
            // 解析HT API URL
            initEthWalletRPC();
            // 存放nerveChainId
            oneContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(OneDBConstant.DB_ONE);
            // 初始化待确认任务队列
            initUnconfirmedTxQueue();
            // 初始化地址过滤集合
            initFilterAddresses();
        }
    }

    private void initBean() {
        beanMap = HtgUtil.initBeanV2(oneContext, converterConfig, oneCallBackManager, OneDBConstant.DB_ONE);
        htgListener = (HtgListener) beanMap.get(HtgListener.class);
        htgMultiSignAddressHistoryStorageService = (HtgMultiSignAddressHistoryStorageService) beanMap.get(HtgMultiSignAddressHistoryStorageService.class);
        htgTxInvokeInfoStorageService = (HtgTxInvokeInfoStorageService) beanMap.get(HtgTxInvokeInfoStorageService.class);
        htgUnconfirmedTxStorageService = (HtgUnconfirmedTxStorageService) beanMap.get(HtgUnconfirmedTxStorageService.class);
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(oneContext.config.getChainId());
        info.setChainName(oneContext.config.getSymbol());
        info.setMultySignAddress(oneContext.config.getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        return oneContext.DOCKING;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
            // 监听多签地址交易
            htgListener.addListeningAddress(multiSigAddress);
            // 管理回调函数实例
            oneCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            oneCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            oneCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // 存放CORE查询API实例
            oneContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            // 更新多签地址
            oneContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // 保存当前多签地址到多签地址历史列表中
            htgMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        oneContext.logger.info("{} 注册完成.", oneContext.config.getSymbol());
    }

    private void initDefualtAPI() throws Exception {
        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        htgWalletApi.init(ethWalletRpcProcessing(oneContext.config.getCommonRpcAddress()));
    }

    private void initEthWalletRPC() {
        String orderRpcAddresses = oneContext.config.getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                oneContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = oneContext.config.getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                oneContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
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
        String filterAddresses = oneContext.config.getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                oneContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    oneContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // 把待确认的交易加入到监听交易hash列表中
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        oneContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("one-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgBlockHandler.class), 60, 5, TimeUnit.SECONDS);

        oneContext.getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(HtgConfirmTxHandler.class));
        oneContext.getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class));
        oneContext.getConverterCoreApi().addHtgRpcAvailableHandler((Runnable) beanMap.get(HtgRpcAvailableHandler.class));

        //confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("one-confirm-tx"));
        //confirmTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgConfirmTxHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("one-waiting-tx"));
        //waitingTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //rpcAvailableExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("one-rpcavailable-tx"));
        //rpcAvailableExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgRpcAvailableHandler.class), 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    oneContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        oneContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}