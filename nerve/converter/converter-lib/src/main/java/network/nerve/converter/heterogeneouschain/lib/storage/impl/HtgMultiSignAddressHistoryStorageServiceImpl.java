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

import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.model.po.StringSetPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-03-17
 */
public class HtgMultiSignAddressHistoryStorageServiceImpl implements HtgMultiSignAddressHistoryStorageService {

    private final String baseArea;
    private final String KEY_PREFIX = "MSADDRESS-";
    private final byte[] ALL_KEY = ConverterDBUtil.stringToBytes("MSADDRESS-ALL");

    private final HtgContext htgContext;
    public HtgMultiSignAddressHistoryStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
    }

    @Override
    public int save(String address) throws Exception {
        if (StringUtils.isBlank(address)) {
            return 0;
        }
        boolean result = RocksDBService.put(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address), HtgConstant.EMPTY_BYTE);
        if (result) {
            StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, StringSetPo.class);
            if(setPo == null) {
                setPo = new StringSetPo();
                Set<String> set = new HashSet<>();
                set.add(address);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
            } else {
                Set<String> set = setPo.getCollection();
                if(!set.contains(address)) {
                    set.add(address);
                    result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public boolean isExist(String address) {
        byte[] bytes = RocksDBService.get(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address));
        if(bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteByAddress(String address) throws Exception {
        RocksDBService.delete(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address));
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, StringSetPo.class);
        if(setPo != null) {
            setPo.getCollection().remove(address);
            ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
        }
    }

    @Override
    public Set<String> findAll() {
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, StringSetPo.class);
        if (setPo == null) {
            return null;
        }
        Set<String> set = setPo.getCollection();
        return set;
    }
}
