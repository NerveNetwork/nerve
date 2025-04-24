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
package network.nerve.converter.heterogeneouschain.tbc.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.bitcoinlib.handler.BitCoinLibBlockHandler;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibBlockAnalysisHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.register.BitCoinLibRegister;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.management.BeanMap;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgERC20StorageService;
import network.nerve.converter.heterogeneouschain.lib.storage.impl.HtgERC20StorageServiceImpl;
import network.nerve.converter.heterogeneouschain.tbc.context.TbcContext;
import network.nerve.converter.heterogeneouschain.tbc.core.TbcBitCoinApi;
import network.nerve.converter.heterogeneouschain.tbc.core.TbcWalletApi;
import network.nerve.converter.heterogeneouschain.tbc.docking.TbcDocking;
import network.nerve.converter.heterogeneouschain.tbc.handler.TbcConfirmTxHandler;
import network.nerve.converter.heterogeneouschain.tbc.helper.TbcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.tbc.helper.TbcParseTxHelper;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
@Component("tbcRegister")
public class TbcRegister extends BitCoinLibRegister {

    @Autowired
    private ConverterConfig converterConfig;
    private TbcContext context = new TbcContext();

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
        return "cv_table_tbc";
    }

    @Override
    public String blockSyncThreadName() {
        return "tbc-block-sync";
    }

    @Override
    protected BitCoinLibDocking newInstanceDocking() {
        return new TbcDocking();
    }

    @Override
    protected String getInitialMultiSignPubKeyList(IConverterCoreApi coreApi) {
        return coreApi.getInitialTbcPubKeyList();
    }

    @Override
    protected void injectionBeanMap(BeanMap beanMap) throws Exception {
        beanMap.add(TbcBitCoinApi.class);
        beanMap.add(TbcWalletApi.class);
        beanMap.add(BitCoinLibBlockHandler.class);
        beanMap.add(TbcConfirmTxHandler.class);
        beanMap.add(TbcAnalysisTxHelper.class);
        beanMap.add(BitCoinLibBlockAnalysisHelper.class);
        beanMap.add(TbcParseTxHelper.class);
        beanMap.add(HtgERC20Helper.class);
        beanMap.add(HtgERC20StorageService.class, new HtgERC20StorageServiceImpl(context, DBName()));
    }

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        super.registerCallBack(registerInfo);
        String combineHash = TbcUtil.getCombineHash(registerInfo.getMultiSigAddress());
        context.dynamicCache().put("combineHash", combineHash);
        String pubKeyList = registerInfo.getConverterCoreApi().getInitialTbcPubKeyList();
        String[] split = pubKeyList.split(",");
        context.dynamicCache().put("pubSet", Set.copyOf(Arrays.asList(split)));
    }
}
