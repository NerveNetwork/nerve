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
package network.nerve.converter.heterogeneouschain.bnb.register;

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
import network.nerve.converter.heterogeneouschain.bnb.callback.BnbCallBackManager;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.docking.BnbDocking;
import network.nerve.converter.heterogeneouschain.bnb.helper.*;
import network.nerve.converter.heterogeneouschain.bnb.listener.BnbListener;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbWaitingTxPo;
import network.nerve.converter.heterogeneouschain.bnb.schedules.BnbBlockScheduled;
import network.nerve.converter.heterogeneouschain.bnb.schedules.BnbConfirmTxScheduled;
import network.nerve.converter.heterogeneouschain.bnb.schedules.BnbWaitingTxInvokeDataScheduled;
import network.nerve.converter.heterogeneouschain.bnb.storage.*;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.bnb.context.BnbContext.logger;


/**
 * BNB组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("bnbRegister")
public class BnbRegister implements IHeterogeneousChainRegister {
    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private BnbListener bnbListener;
    @Autowired
    private BnbCallBackManager bnbCallBackManager;
    @Autowired
    private BnbTxRelationStorageService bnbTxRelationStorageService;
    @Autowired
    private BnbUnconfirmedTxStorageService bnbUnconfirmedTxStorageService;
    @Autowired
    private BnbAccountStorageService bnbAccountStorageService;
    @Autowired
    private BnbERC20Helper bnbERC20Helper;
    @Autowired
    private BnbMultiSignAddressHistoryStorageService bnbMultiSignAddressHistoryStorageService;
    @Autowired
    private BnbTxStorageService bnbTxStorageService;
    @Autowired
    private BnbCommonHelper bnbCommonHelper;
    @Autowired
    private BnbInvokeTxHelper bnbInvokeTxHelper;
    @Autowired
    private BnbParseTxHelper bnbParseTxHelper;
    @Autowired
    private BnbAnalysisTxHelper bnbAnalysisTxHelper;
    @Autowired
    private BnbResendHelper bnbResendHelper;
    @Autowired
    private BnbBlockScheduled bnbBlockScheduled;
    @Autowired
    private BnbConfirmTxScheduled bnbConfirmTxScheduled;
    @Autowired
    private BnbWaitingTxInvokeDataScheduled bnbWaitingTxInvokeDataScheduled;
    @Autowired
    private BnbPendingTxHelper bnbPendingTxHelper;
    @Autowired
    private BnbTxInvokeInfoStorageService bnbTxInvokeInfoStorageService;
    @Autowired
    private BnbUpgradeContractSwitchHelper bnbUpgradeContractSwitchHelper;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    private ScheduledThreadPoolExecutor confirmTxExecutor;
    private ScheduledThreadPoolExecutor waitingTxExecutor;

    private boolean isInitial = false;

    private boolean newProcessActivated = false;

    @Override
    public int order() {
        return 3;
    }

    @Override
    public int getChainId() {
        return BnbConstant.BNB_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // 存放日志实例
            BnbContext.setLogger(logger);
            isInitial = true;
            // 存放配置实例
            BnbContext.setConfig(config);
            // 初始化默认API
            initDefualtAPI();
            // 解析BNB API URL
            initEthWalletRPC();
            // 存放nerveChainId
            BnbContext.NERVE_CHAINID = converterConfig.getChainId();
            RocksDBService.createTable(BnbDBConstant.DB_BNB);
            // 初始化待确认任务队列
            initUnconfirmedTxQueue();
            // 初始化地址过滤集合
            initFilterAddresses();
        }
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(BnbConstant.BNB_CHAIN_ID);
        info.setChainName(BnbConstant.BNB_SYMBOL);
        info.setMultySignAddress(BnbContext.getConfig().getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        BnbDocking docking = BnbDocking.getInstance();
        docking.setBnbWalletApi(bnbWalletApi);
        docking.setBnbListener(bnbListener);
        docking.setConverterConfig(converterConfig);
        docking.setBnbTxRelationStorageService(bnbTxRelationStorageService);
        docking.setBnbUnconfirmedTxStorageService(bnbUnconfirmedTxStorageService);
        docking.setBnbAccountStorageService(bnbAccountStorageService);
        docking.setBnbMultiSignAddressHistoryStorageService(bnbMultiSignAddressHistoryStorageService);
        docking.setBnbERC20Helper(bnbERC20Helper);
        docking.setBnbTxStorageService(bnbTxStorageService);
        docking.setBnbCallBackManager(bnbCallBackManager);
        docking.setBnbCommonHelper(bnbCommonHelper);
        docking.setBnbUpgradeContractSwitchHelper(bnbUpgradeContractSwitchHelper);
        docking.setBnbInvokeTxHelper(bnbInvokeTxHelper);
        docking.setBnbParseTxHelper(bnbParseTxHelper);
        docking.setBnbAnalysisTxHelper(bnbAnalysisTxHelper);
        docking.setBnbResendHelper(bnbResendHelper);
        docking.setBnbPendingTxHelper(bnbPendingTxHelper);
        return docking;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress().toLowerCase();
            // 监听多签地址交易
            bnbListener.addListeningAddress(multiSigAddress);
            // 管理回调函数实例
            bnbCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            bnbCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            bnbCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // 存放CORE查询API实例
            BnbContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            // 更新多签地址
            BnbContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // 保存当前多签地址到多签地址历史列表中
            bnbMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        logger().info("BNB 注册完成.");
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
        bnbWalletApi.init(ethWalletRpcProcessing(BnbContext.getConfig().getCommonRpcAddress()));
    }

    private void initEthWalletRPC() {
        String orderRpcAddresses = BnbContext.getConfig().getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                BnbContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = BnbContext.getConfig().getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                BnbContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
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
        String filterAddresses = BnbContext.getConfig().getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                BnbContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<BnbUnconfirmedTxPo> list = bnbUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    BnbContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // 把待确认的交易加入到监听交易hash列表中
                    bnbListener.addListeningTx(po.getTxHash());
                }
            });
        }
        BnbContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("bnb-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay(bnbBlockScheduled, 60, 5, TimeUnit.SECONDS);

        confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("bnb-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay(bnbConfirmTxScheduled, 60, 10, TimeUnit.SECONDS);

        waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("bnb-waiting-tx"));
        waitingTxExecutor.scheduleWithFixedDelay(bnbWaitingTxInvokeDataScheduled, 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<BnbWaitingTxPo> list = bnbTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    BnbContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        BnbContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
