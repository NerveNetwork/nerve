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
package network.nerve.converter.heterogeneouschain.lib.helper;

import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.springframework.beans.BeanUtils;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
public class HtgStorageHelper implements BeanInitial {

    private HtgTxStorageService htTxStorageService;

    private HtgContext htgContext;

    //public HtgStorageHelper(BeanMap beanMap) {
    //    this.htTxStorageService = (HtgTxStorageService) beanMap.get("htTxStorageService");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    public void saveTxInfo(HtgUnconfirmedTxPo txPo) throws Exception {
        HeterogeneousTransactionInfo txInfo = new HeterogeneousTransactionInfo();
        BeanUtils.copyProperties(txPo, txInfo);
        htTxStorageService.save(txInfo);
    }

    public void saveTxInfo(HeterogeneousTransactionInfo txInfo) throws Exception {
        htTxStorageService.save(txInfo);
    }
}
