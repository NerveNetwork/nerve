/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgAccountStorageService;

import java.nio.charset.StandardCharsets;

/**
 * @author: Mimi
 * @date: 2020-03-26
 */
public class HtgAccountHelper implements BeanInitial {

    private HtgAccountStorageService htgAccountStorageService;
    private HtgContext htgContext;

    //public HtgAccountHelper(BeanMap beanMap) {
    //    this.htgAccountStorageService = (HtgAccountStorageService) beanMap.get("htgAccountStorageService");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    public String sign(String data, HtgContext htgContext) throws NulsException {
        HtgAccount account = (HtgAccount) htgAccountStorageService.findByAddress(htgContext.ADMIN_ADDRESS());
        if (account == null) {
            return null;
        }
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPrivate(account.getPriKey());
        byte[] signBytes = nEckey.sign(data.getBytes(StandardCharsets.UTF_8));
        String sign = HexUtil.encode(signBytes);
        return sign;
    }
}
