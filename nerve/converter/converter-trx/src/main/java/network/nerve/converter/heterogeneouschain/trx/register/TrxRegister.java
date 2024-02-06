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
package network.nerve.converter.heterogeneouschain.trx.register;

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
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.handler.HtgRpcAvailableHandler;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import network.nerve.converter.heterogeneouschain.trx.callback.TrxCallBackManager;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxDBConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.docking.TrxDocking;
import network.nerve.converter.heterogeneouschain.trx.handler.TrxBlockHandler;
import network.nerve.converter.heterogeneouschain.trx.handler.TrxConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.trx.handler.TrxWaitingTxInvokeDataHandler;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxERC20Helper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxParseTxHelper;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * TRXComponent orientedNerveCore registration
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("trxRegister")
public class TrxRegister implements IHeterogeneousChainRegister {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private TrxCallBackManager trxCallBackManager;

    private TrxContext trxContext = new TrxContext();
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
        return 9;
    }

    @Override
    public int getChainId() {
        return trxContext.HTG_CHAIN_ID;
    }

    @Override
    public String init(HeterogeneousCfg config, NulsLogger logger) throws Exception {
        if (!isInitial) {
            // Storing log instances
            trxContext.setLogger(logger);
            isInitial = true;
            // Storing configuration instances
            trxContext.setConfig(config);
            // Initialize instance
            initBean();
            // Initialize defaultAPI
            initDefualtAPI();
            // analysisTRX API URL
            initTrxWalletRPC();
            // depositnerveChainId
            trxContext.NERVE_CHAINID = converterConfig.getChainId();
            //RocksDBService.createTable(TrxDBConstant.DB_TRX);
            // Initialize address filtering set
            initFilterAddresses();
        }
        return TrxDBConstant.DB_TRX;
    }

    private void initBean() {
        try {
            beanMap.add(HtgDocking.class, (trxContext.DOCKING = new TrxDocking()));
            beanMap.add(HtgContext.class, trxContext);
            beanMap.add(HtgListener.class, (htgListener = new HtgListener()));

            beanMap.add(ConverterConfig.class, converterConfig);
            beanMap.add(HtgCallBackManager.class, trxCallBackManager);

            beanMap.add(TrxWalletApi.class);
            beanMap.add(TrxBlockHandler.class);
            beanMap.add(TrxConfirmTxHandler.class);
            beanMap.add(TrxWaitingTxInvokeDataHandler.class);
            beanMap.add(TrxAnalysisTxHelper.class);
            beanMap.add(TrxBlockAnalysisHelper.class);
            beanMap.add(TrxERC20Helper.class);
            beanMap.add(TrxParseTxHelper.class);

            beanMap.add(HtgRpcAvailableHandler.class);
            beanMap.add(HtgAccountHelper.class);
            beanMap.add(HtgCommonHelper.class);
            beanMap.add(HtgInvokeTxHelper.class);
            beanMap.add(HtgLocalBlockHelper.class);
            beanMap.add(HtgPendingTxHelper.class);
            beanMap.add(HtgResendHelper.class);
            beanMap.add(HtgStorageHelper.class);
            beanMap.add(HtgUpgradeContractSwitchHelper.class);

            beanMap.add(HtgAccountStorageService.class, new HtgAccountStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX));
            beanMap.add(HtgBlockHeaderStorageService.class, new HtgBlockHeaderStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX));
            beanMap.add(HtgERC20StorageService.class, new HtgERC20StorageServiceImpl(trxContext, TrxDBConstant.DB_TRX));
            beanMap.add(HtgMultiSignAddressHistoryStorageService.class, (htgMultiSignAddressHistoryStorageService = new HtgMultiSignAddressHistoryStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX)));
            beanMap.add(HtgTxInvokeInfoStorageService.class, (htgTxInvokeInfoStorageService = new HtgTxInvokeInfoStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX)));
            beanMap.add(HtgTxRelationStorageService.class, new HtgTxRelationStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX));
            beanMap.add(HtgTxStorageService.class, new HtgTxStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX));
            beanMap.add(HtgUnconfirmedTxStorageService.class, (htgUnconfirmedTxStorageService = new HtgUnconfirmedTxStorageServiceImpl(trxContext, TrxDBConstant.DB_TRX)));

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
        info.setChainId(trxContext.config.getChainId());
        info.setChainName(trxContext.config.getSymbol());
        info.setMultySignAddress(trxContext.config.getMultySignAddress());
        return info;
    }

    @Override
    public IHeterogeneousChainDocking getDockingImpl() {
        return trxContext.DOCKING;
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        if (!this.newProcessActivated) {
            String multiSigAddress = registerInfo.getMultiSigAddress();
            // Listening for multi signature address transactions
            htgListener.addListeningAddress(multiSigAddress);
            // Manage callback function instances
            trxCallBackManager.setDepositTxSubmitter(registerInfo.getDepositTxSubmitter());
            trxCallBackManager.setTxConfirmedProcessor(registerInfo.getTxConfirmedProcessor());
            trxCallBackManager.setHeterogeneousUpgrade(registerInfo.getHeterogeneousUpgrade());
            // depositCOREqueryAPIexample
            trxContext.setConverterCoreApi(registerInfo.getConverterCoreApi());
            trxContext.getConverterCoreApi().addChainDBName(getChainId(), TrxDBConstant.DB_TRX);
            // Update multiple signed addresses
            trxContext.MULTY_SIGN_ADDRESS = multiSigAddress;
            // Save the current multi signature address to the multi signature address history list
            htgMultiSignAddressHistoryStorageService.save(multiSigAddress);
            // Initialize the pending confirmation task queue
            initUnconfirmedTxQueue();
            // Initialize transaction waiting task queue
            initWaitingTxQueue();
            // Start a new process's work task pool
            initScheduled();
            // Set new process switching flag
            this.newProcessActivated = true;
        }
        trxContext.logger.info("{} Registration completed.", trxContext.config.getSymbol());
    }

    private void initDefualtAPI() throws Exception {
        TrxWalletApi trxWalletApi = (TrxWalletApi) beanMap.get(TrxWalletApi.class);
        trxWalletApi.init(ethWalletRpcProcessing(trxContext.config.getCommonRpcAddress()));
    }

    private void initTrxWalletRPC() {
        String orderRpcAddresses = trxContext.config.getOrderRpcAddresses();
        if(StringUtils.isNotBlank(orderRpcAddresses)) {
            String[] rpcArray = orderRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                trxContext.RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
            }
        }
        String standbyRpcAddresses = trxContext.config.getStandbyRpcAddresses();
        if(StringUtils.isNotBlank(standbyRpcAddresses)) {
            String[] rpcArray = standbyRpcAddresses.split(",");
            for(String rpc : rpcArray) {
                trxContext.STANDBY_RPC_ADDRESS_LIST.add(ethWalletRpcProcessing(rpc));
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
        String filterAddresses = trxContext.config.getFilterAddresses();
        if(StringUtils.isNotBlank(filterAddresses)) {
            String[] filterArray = filterAddresses.split(",");
            for(String address : filterArray) {
                address = address.trim();
                trxContext.FILTER_ACCOUNT_SET.add(address);
            }
        }
    }

    private void initUnconfirmedTxQueue() {
        List<HtgUnconfirmedTxPo> list = htgUnconfirmedTxStorageService.findAll();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    trxContext.UNCONFIRMED_TX_QUEUE.offer(po);
                    // Add pending transactions to listening transactionshashIn the list
                    htgListener.addListeningTx(po.getTxHash());
                }
            });
        }
        trxContext.INIT_UNCONFIRMEDTX_QUEUE_LATCH.countDown();
    }

    private void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("trx-block-sync"));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(TrxBlockHandler.class), 60, 5, TimeUnit.SECONDS);

        trxContext.getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(TrxConfirmTxHandler.class));
        trxContext.getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(TrxWaitingTxInvokeDataHandler.class));
        trxContext.getConverterCoreApi().addHtgRpcAvailableHandler((Runnable) beanMap.get(HtgRpcAvailableHandler.class));

        //confirmTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("trx-confirm-tx"));
        //confirmTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(TrxConfirmTxHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //waitingTxExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("trx-waiting-tx"));
        //waitingTxExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(TrxWaitingTxInvokeDataHandler.class), 60, 10, TimeUnit.SECONDS);
        //
        //rpcAvailableExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("trx-rpcavailable-tx"));
        //rpcAvailableExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(HtgRpcAvailableHandler.class), 60, 10, TimeUnit.SECONDS);
    }

    private void initWaitingTxQueue() {
        List<HtgWaitingTxPo> list = htgTxInvokeInfoStorageService.findAllWaitingTxPo();
        if (list != null && !list.isEmpty()) {
            list.stream().forEach(po -> {
                if(po != null) {
                    // Initialize cache list
                    trxContext.WAITING_TX_QUEUE.offer(po);
                }
            });
        }
        trxContext.INIT_WAITING_TX_QUEUE_LATCH.countDown();
    }

}
