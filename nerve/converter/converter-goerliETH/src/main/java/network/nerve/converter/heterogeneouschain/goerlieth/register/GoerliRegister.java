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
package network.nerve.converter.heterogeneouschain.goerlieth.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.goerlieth.context.GoerliContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.register.HtgRegister;
import network.nerve.converter.model.bo.HeterogeneousCfg;
import network.nerve.converter.model.bo.HeterogeneousChainRegisterInfo;


/**
 * Goerli组件向Nerve核心注册
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("goerliRegister")
public class GoerliRegister extends HtgRegister {

    @Autowired
    private ConverterConfig converterConfig;
    private GoerliContext context = new GoerliContext();

    @Override
    public void registerCallBack(HeterogeneousChainRegisterInfo registerInfo) throws Exception {
        super.registerCallBack(registerInfo);
        context.getConverterCoreApi().setCurrentHeterogeneousVersionII();
    }

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
        return 19;
    }

    @Override
    public String DBName() {
        return "cv_table_goerli";
    }

    @Override
    public String blockSyncThreadName() {
        return "goerli-block-sync";
    }
}