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
package network.nerve.converter.heterogeneouschain.ht.register;

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
import network.nerve.converter.heterogeneouschain.ht.callback.HtCallBackManager;
import network.nerve.converter.heterogeneouschain.ht.constant.HtDBConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
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
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * HT组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("htRegister")
public class HtRegister implements IHeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HtCallBackManager htCallBackManager;

    private HtContext htContext = new HtContext();
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
        return 4;
    }

    @Override
    public int getChainId() {
        return htContext.HTG_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            htContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            htContext.setConfig(config);
            // 初始化实例
            initBean();
            // 初始化默认API
            initDefualtAPI();
            // 解析HT API URL
            initEthWalletRPC();
            // 存放nerveChainId
            htContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(HtDBConstant.DB_HT);
            // 初始化待确认任务队列
            initUnconfirmedTxQueue();
            // 初始化地址过滤集合
            initFilterAddresses();
        }
    }

    private void initBean() {
        beanMap = HtgUtil.initBeanV2(htContext, converterConfig, htCallBackManager, HtDBConstant.DB_HT);
        htgListener = (HtgListener) beanMap.get(HtgListener.class);
        htgMultiSignAddressHistoryStorageService = (HtgMultiSignAddressHistoryStorageService) beanMap.get(HtgMultiSignAddressHistoryStorageService.class);
        htgTxInvokeInfoStorageService = (HtgTxInvokeInfoStorageService) beanMap.get(HtgTxInvokeInfoStorageService.class);
        htgUnconfirmedTxStorageService = (HtgUnconfirmedTxStorageService) beanMap.get(HtgUnconfirmedTxStorageService.class);
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(htContext.config.getChainId());
        info.setChainName(htContext.config.getSymbol());
        info.setMultySignAddress(htContext.config.getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        return htContext.DOCKING;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
            // 监听多签地址交易
            htgListener.addListeningAddress(multiSigAddress);
            // 管理回调函数实例
            htCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            htCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            htCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // 存放CORE查询API实例
            htContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            // 更新多签地址
            htContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // 保存当前多签地址到多签地址历史列表中
            htgMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        htContext.logger.info("{} 注册完成.", htContext.config.getSymbol());
    }

    private void initDefualtAPI() throws Exception {
        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        htgWalletApi.init(ethWalletRpcProcessing(htContext.config.getCommonRpcAddress()));
    }

    private void initEthWalletRPC() {
        String orderRpcAddresses = htContext.config.getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                htContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = htContext.config.getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                htContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
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
        String filterAddresses = htContext.config.getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                htContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    htContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // 把待确认的交易加入到监听交易hash列表中
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        htContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgBlockHandler.class), 60, 5, TimeUnit.SECONDS);

        htContext.getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(HtgConfirmTxHandler.class));
        htContext.getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class));
        htContext.getConverterCoreApi().addHtgRpcAvailableHandler((Runnable) beanMap.get(HtgRpcAvailableHandler.class));

        //confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-confirm-tx"));
        //confirmTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgConfirmTxHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-waiting-tx"));
        //waitingTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //rpcAvailableExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-rpcavailable-tx"));
        //rpcAvailableExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgRpcAvailableHandler.class), 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    htContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        htContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
