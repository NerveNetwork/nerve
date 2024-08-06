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
package network.nerve.converter.heterogeneouschain.btc.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibBlockHandler;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.register.BitCoinLibRegister;
import network.nerve.converter.heterogeneouschain.btc.context.BtcContext;
import network.nerve.converter.heterogeneouschain.btc.core.BitCoinApi;
import network.nerve.converter.heterogeneouschain.btc.docking.BtcDocking;
import network.nerve.converter.heterogeneouschain.btc.handler.BtcConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.btc.helper.BtcParseTxHelper;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
@Component("btcRegister")
public class BtcRegister extends BitCoinLibRegister {

    @Autowired
    private ConverterConfig converterConfig;
    private BtcContext context = new BtcContext();

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
        return "cv_table_btc";
    }

    @Override
    public String blockSyncThreadName() {
        return "btc-block-sync";
    }

    @Override
    protected BitCoinLibDocking newInstanceDocking() {
        return new BtcDocking();
    }

    @Override
    protected String getInitialMultiSignPubKeyList(IConverterCoreApi coreApi) {
        return coreApi.getInitialBtcPubKeyList();
    }
    @Override
    protected void injectionBeanMap(BeanMap beanMap) throws Exception {
        beanMap.add(BitCoinApi.class);
        beanMap.add(BitCoinLibWalletApi.class);
        beanMap.add(BitCoinLibBlockHandler.class);
        beanMap.add(BtcConfirmTxHandler.class);
        beanMap.add(BtcAnalysisTxHelper.class);
        beanMap.add(BitCoinLibBlockAnalysisHelper.class);
        beanMap.add(BtcParseTxHelper.class);
    }

}
