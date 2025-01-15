/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.heterogeneouschain.bitcoinlib.register;

import com.neemre.btcdcli4j.core.client.BtcdClient;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibBlockHandler;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibWaitingTxInvokeDataHandler;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibResendHelper;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManagerNew;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.register.HtgRegister;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public abstract class BitCoinLibRegister extends HtgRegister {

    protected abstract BitCoinLibDocking newInstanceDocking();
    protected abstract void injectionBeanMap(BeanMap beanMap) throws Exception;

    protected abstract String getInitialMultiSignPubKeyList(IConverterCoreApi coreApi);

    @Override
    protected void initDefualtAPI() throws Exception {
        IBitCoinLibWalletApi btcWalletApi = (IBitCoinLibWalletApi) beanMap.get(IBitCoinLibWalletApi.class);
        btcWalletApi.init(getHtgContext().getConfig().getCommonRpcAddress());
    }

    @Override
    public HeterogeneousChainInfo getChainInfo() {
        HeterogeneousChainInfo info = new HeterogeneousChainInfo();
        info.setChainId(getHtgContext().getConfig().getChainId());
        info.setChainName(getHtgContext().getConfig().getSymbol());
        info.setMultySignAddress(getHtgContext().getConfig().getMultySignAddress());
        return info;
    }

    @Override
    protected void initBean() {
        beanMap = initBeans(getHtgContext(), getConverterConfig(), null, DBName());
        htgListener = (HtgListener) beanMap.get(HtgListener.class);
        htgMultiSignAddressHistoryStorageService = (HtgMultiSignAddressHistoryStorageService) beanMap.get(HtgMultiSignAddressHistoryStorageService.class);
        htgTxInvokeInfoStorageService = (HtgTxInvokeInfoStorageService) beanMap.get(HtgTxInvokeInfoStorageService.class);
        htgUnconfirmedTxStorageService = (HtgUnconfirmedTxStorageService) beanMap.get(HtgUnconfirmedTxStorageService.class);
        htgCallBackManager = (HtgCallBackManager) beanMap.get(HtgCallBackManager.class);
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        super.registerCallBack(registerInfo);
        String multiSigAddress = registerInfo.getMultiSigAddress();
        boolean hasPubs = htgMultiSignAddressHistoryStorageService.hasMultiSignAddressPubs(multiSigAddress);
        if (!hasPubs) {
            String initialBtcPubKeyList = this.getInitialMultiSignPubKeyList(registerInfo.getConverterCoreApi());
            htgMultiSignAddressHistoryStorageService.saveMultiSignAddressPubs(multiSigAddress, initialBtcPubKeyList.split(","));
        }

    }

    protected void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(blockSyncThreadName()));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(BitCoinLibBlockHandler.class), 60, getHtgContext().getConfig().getBlockQueuePeriod(), TimeUnit.SECONDS);
        getHtgContext().getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(BitCoinLibConfirmTxHandler.class));
        getHtgContext().getConverterCoreApi().addHtgWaitingTxInvokeDataHandler((Runnable) beanMap.get(BitCoinLibWaitingTxInvokeDataHandler.class));
    }

    private BeanMap initBeans(HtgContext htgContext, ConverterConfig converterConfig,
                              HtgCallBackManager htgCallBackManager, String dbName) {
        try {
            BeanMap beanMap = new BeanMap();
            BitCoinLibDocking docking = this.newInstanceDocking();
            htgContext.SET_DOCKING(docking);
            beanMap.add(IHeterogeneousChainDocking.class, docking);
            beanMap.add(HtgContext.class, htgContext);
            beanMap.add(HtgListener.class, new HtgListener());

            beanMap.add(ConverterConfig.class, converterConfig);
            if (htgCallBackManager == null) {
                beanMap.add(HtgCallBackManager.class, HtgCallBackManagerNew.class.getDeclaredConstructor().newInstance());
            } else {
                beanMap.add(HtgCallBackManager.class, htgCallBackManager);
            }

            this.injectionBeanMap(beanMap);
            beanMap.add(HtgPendingTxHelper.class);
            beanMap.add(HtgCommonHelper.class);
            beanMap.add(HtgInvokeTxHelper.class);
            beanMap.add(HtgLocalBlockHelper.class);
            beanMap.add(HtgStorageHelper.class);

            beanMap.add(BitCoinLibResendHelper.class);
            beanMap.add(BitCoinLibWaitingTxInvokeDataHandler.class);

            beanMap.add(HtgAccountStorageService.class, new HtgAccountStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgBlockHeaderStorageService.class, new HtgBlockHeaderStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgMultiSignAddressHistoryStorageService.class, new HtgMultiSignAddressHistoryStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgTxInvokeInfoStorageService.class, new HtgTxInvokeInfoStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgTxRelationStorageService.class, new HtgTxRelationStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgTxStorageService.class, new HtgTxStorageServiceImpl(htgContext, dbName));
            beanMap.add(HtgUnconfirmedTxStorageService.class, new HtgUnconfirmedTxStorageServiceImpl(htgContext, dbName));

            Collection<Object> values = beanMap.values();
            for (Object value : values) {
                if (value instanceof BeanInitial) {
                    BeanInitial beanInitial = (BeanInitial) value;
                    beanInitial.init(beanMap);
                }
            }
            return beanMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdownConfirm() {
        if (!this.shutdownPending) {
            throw new RuntimeException("Error steps to close the chain.");
        }
        blockSyncExecutor.shutdown();

        IBitCoinLibWalletApi btcWalletApi = (IBitCoinLibWalletApi) beanMap.get(IBitCoinLibWalletApi.class);
        BtcdClient client = btcWalletApi.getClient();
        if (client != null) {
            client.close();
        }

        IConverterCoreApi coreApi = getHtgContext().getConverterCoreApi();
        List<Runnable> confirmTxHandlers = coreApi.getHtgConfirmTxHandlers();
        List<Runnable> waitingTxInvokeDataHandlers = coreApi.getHtgWaitingTxInvokeDataHandlers();
        boolean has1 = false, has3 = false;
        int index1 = 0, index3 = 0;
        for (Runnable runnable : confirmTxHandlers) {
            if (runnable.equals((Runnable) beanMap.get(BitCoinLibConfirmTxHandler.class))) {
                has1 = true;
                break;
            }
            index1++;
        }
        if (has1) confirmTxHandlers.remove(index1);

        for (Runnable runnable : waitingTxInvokeDataHandlers) {
            if (runnable.equals((Runnable) beanMap.get(BitCoinLibWaitingTxInvokeDataHandler.class))) {
                has3 = true;
                break;
            }
            index3++;
        }
        if (has3) waitingTxInvokeDataHandlers.remove(index3);
    }
}
