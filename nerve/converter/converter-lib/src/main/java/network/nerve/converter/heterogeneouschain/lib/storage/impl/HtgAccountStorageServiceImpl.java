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

package network.nerve.converter.heterogeneouschain.lib.storage.impl;

import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgAccountStorageService;
import network.nerve.converter.model.bo.HeterogeneousAccount;
import network.nerve.converter.model.po.StringSetPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgAccountStorageServiceImpl implements HtgAccountStorageService {

    private final String baseArea;
    private final String KEY_PREFIX = "ACCOUNT-";
    private final byte[] ACCOUNT_ALL_KEY = stringToBytes("ACCOUNT-ALL");

    private final HtgContext htgContext;
    public HtgAccountStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
    }

    @Override
    public int save(HtgAccount po) throws Exception {
        if (po == null) {
            return 0;
        }
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + po.getAddress()), po);
        if (result) {
            StringSetPo accountSetPo = ConverterDBUtil.getModel(baseArea, ACCOUNT_ALL_KEY, StringSetPo.class);
            if (accountSetPo == null) {
                accountSetPo = new StringSetPo();
                Set<String> set = new HashSet<>();
                set.add(po.getAddress());
                accountSetPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea, ACCOUNT_ALL_KEY, accountSetPo);
            } else {
                accountSetPo.getCollection().add(po.getAddress());
                result = ConverterDBUtil.putModel(baseArea, ACCOUNT_ALL_KEY, accountSetPo);
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HtgAccount findByAddress(String address) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + address), HtgAccount.class);
    }

    @Override
    public void deleteByAddress(String address) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + address));
        StringSetPo accountSetPo = ConverterDBUtil.getModel(baseArea, ACCOUNT_ALL_KEY, StringSetPo.class);
        if(accountSetPo != null) {
            accountSetPo.getCollection().remove(address);
            ConverterDBUtil.putModel(baseArea, ACCOUNT_ALL_KEY, accountSetPo);
        }
    }

    @Override
    public List<HeterogeneousAccount> findAll() {
        StringSetPo accountSetPo = ConverterDBUtil.getModel(baseArea, ACCOUNT_ALL_KEY, StringSetPo.class);
        if (accountSetPo == null) {
            return null;
        }
        Set<String> ethAccountSet = accountSetPo.getCollection();
        List<HeterogeneousAccount> resultList = new ArrayList<>();
        for (String address : ethAccountSet) {
            resultList.add(this.findByAddress(address));
        }
        return resultList;
    }
}
