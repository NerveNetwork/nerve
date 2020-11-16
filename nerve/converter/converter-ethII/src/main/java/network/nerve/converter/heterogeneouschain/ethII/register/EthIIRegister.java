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
package network.nerve.converter.heterogeneouschain.ethII.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.docking.EthDocking;
import network.nerve.converter.heterogeneouschain.eth.helper.*;
import network.nerve.converter.heterogeneouschain.eth.helper.interfaces.IEthUpgrade;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.register.EthRegister;
import network.nerve.converter.heterogeneouschain.eth.storage.*;
import network.nerve.converter.heterogeneouschain.ethII.constant.EthIIConstant;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.ethII.docking.EthIIDocking;
import network.nerve.converter.heterogeneouschain.ethII.helper.*;
import network.nerve.converter.heterogeneouschain.ethII.model.EthWaitingTxPo;
import network.nerve.converter.heterogeneouschain.ethII.schedules.EthIIBlockScheduled;
import network.nerve.converter.heterogeneouschain.ethII.schedules.EthIIConfirmTxScheduled;
import network.nerve.converter.heterogeneouschain.ethII.schedules.EthIIWaitingTxInvokeDataScheduled;
import network.nerve.converter.heterogeneouschain.ethII.storage.EthTxInvokeInfoStorageService;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * Eth组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("ethIIRegister")
public class EthIIRegister implements IHeterogeneousChainRegister {
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthListener ethListener;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthAccountStorageService ethAccountStorageService;
    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private EthMultiSignAddressHistoryStorageService ethMultiSignAddressHistoryStorageService;
    @Autowired
    private EthTxStorageService ethTxStorageService;
    @Autowired
    private EthParseTxHelper ethParseTxHelper;
    @Autowired
    private EthAnalysisTxHelper ethAnalysisTxHelper;
    @Autowired
    private EthCommonHelper ethCommonHelper;
    @Autowired
    private EthIIInvokeTxHelper ethIIInvokeTxHelper;
    @Autowired
    private EthIIParseTxHelper ethIIParseTxHelper;
    @Autowired
    private EthIIAnalysisTxHelper ethIIAnalysisTxHelper;
    @Autowired
    private EthIIResendHelper ethIIResendHelper;
    @Autowired
    private EthIIBlockScheduled ethIIBlockScheduled;
    @Autowired
    private EthIIConfirmTxScheduled ethIIConfirmTxScheduled;
    @Autowired
    private EthIIWaitingTxInvokeDataScheduled ethIIWaitingTxInvokeDataScheduled;
    @Autowired
    private EthIIPendingTxHelper ethIIPendingTxHelper;
    @Autowired
    private EthTxInvokeInfoStorageService ethTxInvokeInfoStorageService;
    @Autowired
    private EthUpgradeContractSwitchHelper ethUpgradeContractSwitchHelper;
    @Autowired
    private EthRegister ethRegister;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    private ScheduledThreadPoolExecutor confirmTxExecutor;
    private ScheduledThreadPoolExecutor waitingTxExecutor;

    private boolean isInitial = false;

    private boolean newProcessActivated = false;

    @Override
    public int order() {
        return 2;
    }

    @Override
    public int getChainId() {
        return EthConstant.ETH_CHAIN_ID;
    }

    @Override
    public void init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            isInitial = true;
            initFilterAddresses();
            // 初始化升级切换操作的函数，当动态升级合约交易确认时，将调用设置的匿名函数
            initUpgradeSwitchFunction();
        }
    }

    private void initUpgradeSwitchFunction() {
        ethUpgradeContractSwitchHelper.registerUpgrade(new IEthUpgrade() {
            @Override
            public int version() {
                return EthIIConstant.VERSION;
            }

            @Override
            public void newSwitch(String newContract) throws Exception {
                if (EthIIRegister.this.newProcessActivated) {
                    logger().info("ETH II 已注册，不再重复注册。");
                    return;
                }
                // 停止旧的处理流程
                ethRegister.shutDownScheduled();
                // 初始化交易等待任务队列
                initWaitingTxQueue();
                // 开启新的处理流程
                EthIIRegister.this.initScheduled();
                // 切换dockingII实例到CORE中
                ethCallBackManager.getHeterogeneousUpgrade().switchDocking(EthIIDocking.getInstance());
                // 设置新流程切换标志
                EthIIRegister.this.newProcessActivated = true;
            }
        });
    }

    private void initFilterAddresses() {
        String filterAddresses = EthContext.getConfig().getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                EthIIContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(EthConstant.ETH_CHAIN_ID);
        info.setChainName(EthConstant.ETH_SYMBOL);
        info.setMultySignAddress(EthContext.getConfig().getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        EthIIDocking docking = EthIIDocking.getInstance();
        docking.setEthWalletApi(ethWalletApi);
        docking.setEthListener(ethListener);
        docking.setConverterConfig(converterConfig);
        docking.setEthTxRelationStorageService(ethTxRelationStorageService);
        docking.setEthUnconfirmedTxStorageService(ethUnconfirmedTxStorageService);
        docking.setEthAccountStorageService(ethAccountStorageService);
        docking.setEthMultiSignAddressHistoryStorageService(ethMultiSignAddressHistoryStorageService);
        docking.setEthERC20Helper(ethERC20Helper);
        docking.setEthTxStorageService(ethTxStorageService);
        docking.setEthParseTxHelper(ethParseTxHelper);
        docking.setEthCallBackManager(ethCallBackManager);
        docking.setEthAnalysisTxHelper(ethAnalysisTxHelper);
        docking.setEthCommonHelper(ethCommonHelper);
        docking.setEthUpgradeContractSwitchHelper(ethUpgradeContractSwitchHelper);
        docking.setEthIIInvokeTxHelper(ethIIInvokeTxHelper);
        docking.setEthIIParseTxHelper(ethIIParseTxHelper);
        docking.setEthIIAnalysisTxHelper(ethIIAnalysisTxHelper);
        docking.setEthIIResendHelper(ethIIResendHelper);
        docking.setEthIIPendingTxHelper(ethIIPendingTxHelper);
        // 合约未升级，返回旧的实例
        if (!isUpgradeContract()) {
            return EthDocking.getInstance();
        }
        return docking;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (converterConfig.isNewProcessorMode()) {
            logger().info("ETH将以当前多签合约[{}]启用新流程.", EthContext.MULTY_SIGN_ADDRESS);
            ethUpgradeContractSwitchHelper.switchProcessor(EthContext.MULTY_SIGN_ADDRESS);
        }
        // 合约已升级
        if (isUpgradeContract() && !this.newProcessActivated) {
            // 初始化交易等待任务队列
            initWaitingTxQueue();
            // 启动新流程的工作任务池
            initScheduled();
            // 切换升级后的docking实例
            registerInfo.getHeterogeneousUpgrade().switchDocking(EthIIDocking.getInstance());
            // 设置新流程切换标志
            this.newProcessActivated = true;
        }
        logger().info("ETH II 注册完成.");
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

    /**
     * 当配置的合约地址与当前有效的合约地址不同时，则说明合约已升级
     */
    private boolean isUpgradeContract() {
        String multySignAddressOfSetting = EthContext.getConfig().getMultySignAddress().toLowerCase();
        String currentMultySignAddress = EthContext.MULTY_SIGN_ADDRESS;
        return !multySignAddressOfSetting.equals(currentMultySignAddress);
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("eth-II-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay(ethIIBlockScheduled, 60, 20, TimeUnit.SECONDS);

        confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("eth-II-confirm-tx"));
        confirmTxExecutor.scheduleWithFixedDelay(ethIIConfirmTxScheduled, 60, 20, TimeUnit.SECONDS);

        waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("eth-II-waiting-tx"));
        waitingTxExecutor.scheduleWithFixedDelay(ethIIWaitingTxInvokeDataScheduled, 60, 20, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<EthWaitingTxPo> list = ethTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // 初始化缓存列表
                    EthIIContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        EthIIContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
