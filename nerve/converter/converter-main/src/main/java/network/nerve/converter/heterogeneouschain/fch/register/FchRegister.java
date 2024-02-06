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
package network.nerve.converter.heterogeneouschain.fch.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.fch.docking.FchDocking;
import network.nerve.converter.heterogeneouschain.fch.handler.FchBlockHandler;
import network.nerve.converter.heterogeneouschain.fch.handler.FchConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.fch.helper.FchAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.fch.helper.FchBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.fch.helper.FchParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManagerNew;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgInvokeTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgLocalBlockHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgStorageHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.register.HtgRegister;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.*;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
@Component("fchRegister")
public class FchRegister extends HtgRegister {

    @Autowired
    private ConverterConfig converterConfig;
    private FchContext context = new FchContext();

    @Override
    public ConverterConfig getConverterConfig() {
        return converterConfig;
    }

    @Override
    public HtgContext getHtgContext() {
        return context;
    }

    @Override
    public int order() {
        return getChainId();
    }

    @Override
    public String DBName() {
        return "cv_table_fch";
    }

    @Override
    public String blockSyncThreadName() {
        return "fch-block-sync";
    }

    @Override
    protected void initDefualtAPI() throws Exception {
        FchWalletApi fchWalletApi = (FchWalletApi) beanMap.get(FchWalletApi.class);
        fchWalletApi.init(getHtgContext().getConfig().getCommonRpcAddress());
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

    protected void initScheduled() {
        blockSyncExecutor = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory(blockSyncThreadName()));
        blockSyncExecutor.scheduleWithFixedDelay((Runnable) beanMap.get(FchBlockHandler.class), 60, getHtgContext().getConfig().getBlockQueuePeriod(), TimeUnit.SECONDS);
        getHtgContext().getConverterCoreApi().addHtgConfirmTxHandler((Runnable) beanMap.get(FchConfirmTxHandler.class));
    }

    protected void initWaitingTxQueue() {

    }

    private BeanMap initBeans(HtgContext htgContext, ConverterConfig converterConfig,
                              HtgCallBackManager htgCallBackManager, String dbName) {
        try {
            BeanMap beanMap = new BeanMap();
            FchDocking docking = new FchDocking();
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

            beanMap.add(FchWalletApi.class);
            beanMap.add(FchBlockHandler.class);
            beanMap.add(FchConfirmTxHandler.class);
            beanMap.add(FchAnalysisTxHelper.class);
            beanMap.add(FchBlockAnalysisHelper.class);
            beanMap.add(FchParseTxHelper.class);
            beanMap.add(HtgCommonHelper.class);
            beanMap.add(HtgInvokeTxHelper.class);
            beanMap.add(HtgLocalBlockHelper.class);
            beanMap.add(HtgStorageHelper.class);

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
}
