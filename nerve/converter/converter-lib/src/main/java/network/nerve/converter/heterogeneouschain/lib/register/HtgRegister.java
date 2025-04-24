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
package network.nerve.converter.heterogeneouschain.lib.register;


import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
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
import org.web3j.protocol.Web3j;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public abstract class HtgRegister implements IHeterogeneousChainRegister {

    protected HtgListener htgListener;
    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    protected HtgTxInvokeInfoStorageService htgTxInvokeInfoStorageService;
    protected HtgCallBackManager htgCallBackManager;

    protected ScheduledThreadPoolExecutor blockSyncExecutor;
    protected boolean isInitial = false;
    protected boolean newProcessActivated = false;
    protected boolean shutdownPending = false;
    protected BeanMap beanMap = new BeanMap();

    public abstract ConverterConfig getConverterConfig();
    public abstract HtgContext getHtgContext();
    public abstract int order();
    public abstract String DBName();
    public abstract String blockSyncThreadName();

    @Override
    public int getChainId() {
        return getHtgContext().HTG_CHAIN_ID();
    }

    @Override
    public String init(IConverterCoreApi coreApi, HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            isInitial = true;
            // Initialize instance
            initBean();
            getHtgContext().setConverterCoreApi(coreApi);
            // Storing log instances
            getHtgContext().setLogger(logger);
            // Storing configuration instances
            getHtgContext().setConfig(config);
            // Initialize defaultAPI
            initDefualtAPI();
            // analysisHT API URL
            initEthWalletRPC();
            // depositnerveChainId
            getHtgContext().setNERVE_CHAINID(getConverterConfig().getChainId());
            //RocksDBService.createTable(DBName());
            // Initialize address filtering set
            initFilterAddresses();
        }
        return DBName();
    }

    protected void initBean() {
        beanMap = HtgUtil.initBeanV2(getHtgContext(), getConverterConfig(), null, DBName());
        htgListener = (HtgListener) beanMap.get(HtgListener.class);
        htgMultiSignAddressHistoryStorageService = (HtgMultiSignAddressHistoryStorageService) beanMap.get(HtgMultiSignAddressHistoryStorageService.class);
        htgTxInvokeInfoStorageService = (HtgTxInvokeInfoStorageService) beanMap.get(HtgTxInvokeInfoStorageService.class);
        htgUnconfirmedTxStorageService = (HtgUnconfirmedTxStorageService) beanMap.get(HtgUnconfirmedTxStorageService.class);
        htgCallBackManager = (HtgCallBackManager) beanMap.get(HtgCallBackManager.class);
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() throws Exception {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(getHtgContext().getConfig().getChainId());
        info.setChainName(getHtgContext().getConfig().getSymbol());
        info.setMultySignAddress(getHtgContext().getConfig().getMultySignAddress().toLowerCase());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        return getHtgContext().DOCKING();
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress();
            if (getHtgContext().HTG_CHAIN_ID() < 200) {
                multiSigAddress = multiSigAddress.toLowerCase();
            }

            // Listening for multi signature address transactions
            htgListener.addListeningAddress(multiSigAddress);
            // Manage callback function instances
            htgCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            htgCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            htgCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // depositCOREqueryAPIexample
            getHtgContext().setConverterCoreApi(registerInfo.getConverterCoreApi());
            getHtgContext().getConverterCoreApi().addChainDBName(getChainId(), DBName());
            // Update multiple signed addresses
            getHtgContext().SET_MULTY_SIGN_ADDRESS(multiSigAddress);
            // Save the current multi signature address to the multi signature address history list
            htgMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // Initialize the pending confirmation task queue
            initUnconfirmedTxQueue();
            // Initialize transaction waiting task queue
            initWaitingTxQueue();
            // Start a new process's work task pool
            initScheduled();
            getDockingImpl().setRegister(this);
            // Set new process switching flag
            this.newProcessActivated = true;
        }
        getHtgContext().logger().info("{} Registration completed.", getHtgContext().getConfig().getSymbol());
    }

    protected void initDefualtAPI() throws Exception {
        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        htgWalletApi.init(ethWalletRpcProcessing(getHtgContext().getConfig().getCommonRpcAddress()));
    }

    protected void initEthWalletRPC() {
        String orderRpcAddresses = getHtgContext().getConfig().getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                getHtgContext().RPC_ADDRESS_LIST().add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = getHtgContext().getConfig().getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                getHtgContext().STANDBY_RPC_ADDRESS_LIST().add(ethWalletRpcProcessing(rpc));
            }
        }
    }

    protected String ethWalletRpcProcessing(String rpc) {
        if (StringUtils.isBlank(rpc)) {
            return rpc;
        }
        rpc = rpc.trim();
        return rpc;
    }

    protected void initFilterAddresses() {
        String filterAddresses = getHtgContext().getConfig().getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim().toLowerCase();
                getHtgContext().FILTER_ACCOUNT_SET().add(address);
            }
        }
    }

    protected void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    getHtgContext().UNCONFIRMED_TX_QUEUE().offer(po);
                    // Add pending transactions to listening transactionshashIn the list
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        getHtgContext().INIT_UNCONFIRMEDTX_QUEUE_LATCH().countDown();
    }

    protected void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(blockSyncThreadName()));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgBlockHandler.class), 60, getHtgContext().getConfig().getBlockQueuePeriod(), TimeUnit.SECONDS);

        getHtgContext().getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(HtgConfirmTxHandler.class));
        getHtgContext().getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class));
        getHtgContext().getConverterCoreApi().addHtgRpcAvailableHandler((Runnable) beanMap.get(HtgRpcAvailableHandler.class));
    }

    protected void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    getHtgContext().WAITING_TX_QUEUE().offer(po);
                }
            });
        }
        getHtgContext().INIT_WAITING_TX_QUEUE_LATCH().countDown();
    }

    @Override
    public void shutdownPending() {
        this.shutdownPending = true;
    }

    @Override
    public void shutdownConfirm() {
        if (!this.shutdownPending) {
            throw new RuntimeException("Error steps to close the chain.");
        }
        blockSyncExecutor.shutdown();

        HtgWalletApi htgWalletApi = (HtgWalletApi) beanMap.get(HtgWalletApi.class);
        Web3j web3j = htgWalletApi.getWeb3j();
        if (web3j != null) {
            web3j.shutdown();
        }

        IConverterCoreApi coreApi = getHtgContext().getConverterCoreApi();
        List<Runnable> confirmTxHandlers = coreApi.getHtgConfirmTxHandlers();
        List<Runnable> availableHandlers = coreApi.getHtgRpcAvailableHandlers();
        List<Runnable> waitingTxInvokeDataHandlers = coreApi.getHtgWaitingTxInvokeDataHandlers();
        boolean has1 = false, has2 = false, has3 = false;
        int index1 = 0, index2 = 0, index3 = 0;
        for (Runnable runnable : confirmTxHandlers) {
            if (runnable.equals((Runnable) beanMap.get(HtgConfirmTxHandler.class))) {
                has1 = true;
                break;
            }
            index1++;
        }
        if (has1) confirmTxHandlers.remove(index1);

        for (Runnable runnable : availableHandlers) {
            if (runnable.equals((Runnable) beanMap.get(HtgRpcAvailableHandler.class))) {
                has2 = true;
                break;
            }
            index2++;
        }
        if (has2) availableHandlers.remove(index2);

        for (Runnable runnable : waitingTxInvokeDataHandlers) {
            if (runnable.equals((Runnable) beanMap.get(HtgWaitingTxInvokeDataHandler.class))) {
                has3 = true;
                break;
            }
            index3++;
        }
        if (has3) waitingTxInvokeDataHandlers.remove(index3);
    }
}
