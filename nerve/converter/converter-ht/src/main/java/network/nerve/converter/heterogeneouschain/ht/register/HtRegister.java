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
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.heterogeneouschain.ht.callback.HtCallBackManager;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.heterogeneouschain.ht.constant.HtDBConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.core.HtWalletApi;
import network.nerve.converter.heterogeneouschain.ht.docking.HtDocking;
import network.nerve.converter.heterogeneouschain.ht.helper.*;
import network.nerve.converter.heterogeneouschain.ht.listener.HtListener;
import network.nerve.converter.heterogeneouschain.ht.model.HtUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.ht.model.HtWaitingTxPo;
import network.nerve.converter.heterogeneouschain.ht.schedules.HtBlockScheduled;
import network.nerve.converter.heterogeneouschain.ht.schedules.HtConfirmTxScheduled;
import network.nerve.converter.heterogeneouschain.ht.schedules.HtWaitingTxInvokeDataScheduled;
import network.nerve.converter.heterogeneouschain.ht.storage.*;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.ht.context.HtContext.logger;


/**
 * HT组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("htRegister")
public class HtRegister implements IHeterogeneousChainRegister {
    @Autowired
    private HtWalletApi htWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HtListener htListener;
    @Autowired
    private HtCallBackManager htCallBackManager;
    @Autowired
    private HtTxRelationStorageService htTxRelationStorageService;
    @Autowired
    private HtUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    @Autowired
    private HtAccountStorageService htAccountStorageService;
    @Autowired
    private HtERC20Helper htERC20Helper;
    @Autowired
    private HtMultiSignAddressHistoryStorageService htMultiSignAddressHistoryStorageService;
    @Autowired
    private HtTxStorageService htTxStorageService;
    @Autowired
    private HtCommonHelper htCommonHelper;
    @Autowired
    private HtInvokeTxHelper htInvokeTxHelper;
    @Autowired
    private HtParseTxHelper htParseTxHelper;
    @Autowired
    private HtAnalysisTxHelper htAnalysisTxHelper;
    @Autowired
    private HtResendHelper htResendHelper;
    @Autowired
    private HtBlockScheduled htBlockScheduled;
    @Autowired
    private HtConfirmTxScheduled htConfirmTxScheduled;
    @Autowired
    private HtWaitingTxInvokeDataScheduled htWaitingTxInvokeDataScheduled;
    @Autowired
    private HtPendingTxHelper htPendingTxHelper;
    @Autowired
    private HtTxInvokeInfoStorageService htTxInvokeInfoStorageService;
    @Autowired
    private HtUpgradeContractSwitchHelper htUpgradeContractSwitchHelper;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    private ScheduledThreadPoolExecutor confirmTxExecutor;
    private ScheduledThreadPoolExecutor waitingTxExecutor;

    private boolean isInitial = false;

    private boolean newProcessActivated = false;

    @Override
    public int order() {
        return 4;
    }

    @Override
    public int getChainId() {
        return HtConstant.HT_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            HtContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            HtContext.setConfig(config);
            // 初始化默认API
            initDefualtAPI();
            // 解析HT API URL
            initEthWalletRPC();
            // 存放nerveChainId
            HtContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(HtDBConstant.DB_HT);
            // 初始化待确认任务队列
            initUnconfirmedTxQueue();
            // 初始化地址过滤集合
            initFilterAddresses();
        }
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(HtConstant.HT_CHAIN_ID);
        info.setChainName(HtConstant.HT_SYMBOL);
        info.setMultySignAddress(HtContext.getConfig().getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        HtDocking docking = HtDocking.getInstance();
        docking.setHtWalletApi(htWalletApi);
        docking.setHtListener(htListener);
        docking.setConverterConfig(converterConfig);
        docking.setHtTxRelationStorageService(htTxRelationStorageService);
        docking.setHtUnconfirmedTxStorageService(htUnconfirmedTxStorageService);
        docking.setHtAccountStorageService(htAccountStorageService);
        docking.setHtMultiSignAddressHistoryStorageService(htMultiSignAddressHistoryStorageService);
        docking.setHtERC20Helper(htERC20Helper);
        docking.setHtTxStorageService(htTxStorageService);
        docking.setHtCallBackManager(htCallBackManager);
        docking.setHtCommonHelper(htCommonHelper);
        docking.setHtUpgradeContractSwitchHelper(htUpgradeContractSwitchHelper);
        docking.setHtInvokeTxHelper(htInvokeTxHelper);
        docking.setHtParseTxHelper(htParseTxHelper);
        docking.setHtAnalysisTxHelper(htAnalysisTxHelper);
        docking.setHtResendHelper(htResendHelper);
        docking.setHtPendingTxHelper(htPendingTxHelper);
        return docking;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
            // 监听多签地址交易
            htListener.addListeningAddress(multiSigAddress);
            // 管理回调函数实例
            htCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            htCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            htCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // 存放CORE查询API实例
            HtContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            // 更新多签地址
            HtContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // 保存当前多签地址到多签地址历史列表中
            htMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        logger().info("HT 注册完成.");
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

    private void initDefualtAPI() throws NulsException {
        htWalletApi.init(ethWalletRpcProcessing(HtContext.getConfig().getCommonRpcAddress()));
    }

    private void initEthWalletRPC() {
        String orderRpcAddresses = HtContext.getConfig().getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                HtContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = HtContext.getConfig().getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                HtContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
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
        String filterAddresses = HtContext.getConfig().getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                HtContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtUnconfirmedTxPo> list = htUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    HtContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // 把待确认的交易加入到监听交易hash列表中
                    htListener.addListeningTx(po.getTxHash());
                }
            });
        }
        HtContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay(htBlockScheduled, 60, 5, TimeUnit.SECONDS);

        confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay(htConfirmTxScheduled, 60, 10, TimeUnit.SECONDS);

        waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ht-waiting-tx"));
        waitingTxExecutor.scheduleWithFixedDelay(htWaitingTxInvokeDataScheduled, 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtWaitingTxPo> list = htTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    HtContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        HtContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
