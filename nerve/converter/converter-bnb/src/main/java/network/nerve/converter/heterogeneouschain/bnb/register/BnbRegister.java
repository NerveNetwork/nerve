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
package network.nerve.converter.heterogeneouschain.bnb.register;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.register.HtgRegister;


/**
 * BNBComponent orientedNerveCore registration
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component("bnbRegister")
public class BnbRegister extends HtgRegister {

    @Autowired
    private ConverterConfig converterConfig;
    private BnbContext context = new BnbContext();

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
        return 3;
    }

    @Override
    public String DBName() {
        return "cv_table_bnb";
    }

    @Override
    public String blockSyncThreadName() {
        return "bnb-block-sync";
    }
}
