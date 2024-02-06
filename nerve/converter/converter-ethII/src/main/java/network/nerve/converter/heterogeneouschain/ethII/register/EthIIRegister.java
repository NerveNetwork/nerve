/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.docking.EthDocking;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.register.EthRegister;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgBlockHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgRpcAvailableHandler;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgWaitingTxInvokeDataHandler;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.helper.interfaces.IHtgUpgrade;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * ETH_IIComponent orientedNerveCore registration
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("ethIIRegister")
public class EthIIRegister implements IHeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthRegister ethRegister;
    @Autowired
    private EthListener ethListener;
    @Autowired
    private ETHWalletApi ethWalletApi;

    private EthIIContext ethIIContext;
    private HtgListener htgListener;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    private HtgTxInvokeInfoStorageService htgTxInvokeInfoStorageService;
    private HtgUpgradeContractSwitchHelper htgUpgradeContractSwitchHelper;

    private ScheduledThreadPoolExecutor blockSyncExecutor;
    //private ScheduledThreadPoolExecutor confirmTxExecutor;
    //private ScheduledThreadPoolExecutor waitingTxExecutor;
    //private ScheduledThreadPoolExecutor rpcAvailableExecutor;
    private boolean isInitial = false;
    private boolean newProcessActivated = false;
    private BeanMap beanMap = new BeanMap();

    @Override
    public int order() {
        return 2;
    }

    @Override
    public int getChainId() {
        return EthIIContext.HTG_CHAIN_ID;
    }

    @Override
    public String init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            isInitial = true;
            // Initialize instance
            initBean();
            // Initialize address filtering set
            initFilterAddresses();
            // The function that initializes the upgrade switch operation will call the set anonymous function when dynamically upgrading contract transaction confirmation
            initUpgradeSwitchFunction();
        }
        return EthDBConstant.DB_ETH;
    }

    private void initBean() {
        try {
            beanMap.add(HtgDocking.class, (EthIIContext.DOCKING = new HtgDocking()));
            beanMap.add(HtgContext.class, (ethIIContext = new EthIIContext()));
            beanMap.add(HtgListener.class, (htgListener = new HtgListener()));
            beanMap.add(HtgUpgradeContractSwitchHelper.class, (htgUpgradeContractSwitchHelper = new HtgUpgradeContractSwitchHelper()));
            ethRegister.setEthUpgradeContractSwitchHelper(htgUpgradeContractSwitchHelper);

            beanMap.add(ConverterConfig.class, converterConfig);
            beanMap.add(HtgCallBackManager.class, ethCallBackManager);

            beanMap.add(HtgWalletApi.class);
            beanMap.add(HtgBlockHandler.class);
            beanMap.add(HtgConfirmTxHandler.class);
            beanMap.add(HtgWaitingTxInvokeDataHandler.class);
            beanMap.add(HtgRpcAvailableHandler.class);
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

            beanMap.add(HtgAccountStorageService.class, new HtgAccountStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH));
            beanMap.add(HtgBlockHeaderStorageService.class, new HtgBlockHeaderStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH));
            beanMap.add(HtgERC20StorageService.class, new HtgERC20StorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH));
            beanMap.add(HtgMultiSignAddressHistoryStorageService.class, (htgMultiSignAddressHistoryStorageService = new HtgMultiSignAddressHistoryStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH)));
            beanMap.add(HtgTxInvokeInfoStorageService.class, (htgTxInvokeInfoStorageService = new HtgTxInvokeInfoStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH)));
            beanMap.add(HtgTxRelationStorageService.class, new HtgTxRelationStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH));
            beanMap.add(HtgTxStorageService.class, new HtgTxStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH));
            beanMap.add(HtgUnconfirmedTxStorageService.class, (htgUnconfirmedTxStorageService = new HtgUnconfirmedTxStorageServiceImpl(ethIIContext, EthDBConstant.DB_ETH)));

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

    private void initUpgradeSwitchFunction() {
        htgUpgradeContractSwitchHelper.registerUpgrade(new IHtgUpgrade() {
            @Override
            public int version() {
                return EthIIContext.VERSION;
            }

            @Override
            public void newSwitch(String newContract) throws Exception {
                if (EthIIRegister.this.newProcessActivated) {
                    EthIIContext.getLogger().info("ETH II Registered, no more duplicate registrations.");
                    return;
                }
                // Stop the old processing flow
                ethRegister.shutDownScheduled();
                // Stop oldRPCRequest service
                if (ethWalletApi.getWeb3j() != null) {
                    ethWalletApi.getWeb3j().shutdown();
                }
                // Clear old address listeners
                ethListener.clear();
                // Listening to multiple signed addresses
                htgListener.addListeningAddress(newContract);
                if (EthContext.UNCONFIRMED_TX_QUEUE != null) {
                    EthContext.UNCONFIRMED_TX_QUEUE.stream().forEach(q -> {
                        EthIIContext.UNCONFIRMED_TX_QUEUE.add(q);
                    });
                    EthContext.UNCONFIRMED_TX_QUEUE.clear();
                }
                // Initialize defaultAPI
                initDefualtAPI();
                // Initialize transaction waiting task queue
                EthIIRegister.this.initWaitingTxQueue();
                // Start a new processing flow
                EthIIRegister.this.initScheduled();
                // switchdockingIIInstance toCOREin
                ethCallBackManager.getHeterogeneousUpgrade().switchDocking(EthIIContext.DOCKING);
                // Set new process switching flag
                EthIIRegister.this.newProcessActivated = true;
            }
        });
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(EthIIContext.config().getChainId());
        info.setChainName(EthIIContext.config().getSymbol());
        info.setMultySignAddress(EthIIContext.config().getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        // Contract not upgraded, returning old instance
        if (!isUpgradeContract() && !converterConfig.isNewProcessorMode()) {
            return EthDocking.getInstance();
        }
        return EthIIContext.DOCKING;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (converterConfig.isNewProcessorMode()) {
            EthIIContext.getLogger().info("ETHWill sign multiple contracts with the current one[{}]Enable new process.", EthContext.MULTY_SIGN_ADDRESS);
            htgUpgradeContractSwitchHelper.switchProcessor(EthContext.MULTY_SIGN_ADDRESS);
        }
        // Contract has been upgraded
        if (isUpgradeContract() && !this.newProcessActivated) {
            htgUpgradeContractSwitchHelper.switchProcessor(EthContext.MULTY_SIGN_ADDRESS);
        }
        // Initialize the pending confirmation task queue
        initUnconfirmedTxQueue();
        EthContext.getConverterCoreApi().addChainDBName(getChainId(), EthDBConstant.DB_ETH);
        EthIIContext.getLogger().info("ETH II Registration completed.");
    }

    private void initDefualtAPI() throws Exception {
        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        htgWalletApi.init(ethWalletRpcProcessing(EthIIContext.config().getCommonRpcAddress()));
    }

    private String ethWalletRpcProcessing(String rpc) {
        if (StringUtils.isBlank(rpc)) {
            return rpc;
        }
        rpc = rpc.trim();
        return rpc;
    }

    private void initFilterAddresses() {
        String filterAddresses = EthIIContext.config().getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                EthIIContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    EthIIContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // Add pending transactions to listening transactionshashIn the list
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        EthIIContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    /**
     * When the configured contract address is different from the currently valid contract address, it indicates that the contract has been upgraded
     */
    private boolean isUpgradeContract() {
        String multySignAddressOfSetting = EthContext.getConfig().getMultySignAddress().toLowerCase();
        String currentMultySignAddress = EthContext.MULTY_SIGN_ADDRESS;
        return !multySignAddressOfSetting.equals(currentMultySignAddress);
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ethII-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgBlockHandler.class), 60, 20, TimeUnit.SECONDS);

        ethIIContext.getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(HtgConfirmTxHandler.class));
        ethIIContext.getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class));
        ethIIContext.getConverterCoreApi().addHtgRpcAvailableHandler((Runnable) beanMap.get(HtgRpcAvailableHandler.class));
        //confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ethII-confirm-tx"));
        //confirmTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgConfirmTxHandler.class), 60, 20, TimeUnit.SECONDS);
        //
        //waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ethII-waiting-tx"));
        //waitingTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class), 60, 20, TimeUnit.SECONDS);
        //
        //rpcAvailableExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("ethII-rpcavailable-tx"));
        //rpcAvailableExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgRpcAvailableHandler.class), 60, 20, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    EthIIContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        EthIIContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
