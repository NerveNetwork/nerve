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

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-03-17
 */
public class HtgMultiSignAddressHistoryStorageServiceImpl implements HtgMultiSignAddressHistoryStorageService {

    private String baseArea;
    private final String KEY_PREFIX = "MSADDRESS-";
    private final String VERSION_KEY_PREFIX = "VMSADDRESS";
    private final byte[] ALL_KEY = stringToBytes("MSADDRESS-ALL");
    private final String MERGE_KEY_PREFIX;
    private final String MERGE_VERSION_KEY_PREFIX;
    private final byte[] MERGE_ALL_KEY;

    private final HtgContext htgContext;
    public HtgMultiSignAddressHistoryStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
        int htgChainId = htgContext.HTG_CHAIN_ID();
        this.MERGE_KEY_PREFIX = htgChainId + "_MSADDRESS-";
        this.MERGE_VERSION_KEY_PREFIX = htgChainId + "_VMSADDRESS";
        this.MERGE_ALL_KEY = stringToBytes(htgChainId + "_MSADDRESS-ALL");
    }

    private boolean merged = false;
    private void checkMerged() {
        if (merged) {
            return;
        }
        merged = htgContext.getConverterCoreApi().isDbMerged(htgContext.HTG_CHAIN_ID());
        if (merged) {
            this.baseArea = htgContext.getConverterCoreApi().mergedDBName();
        }
    }
    private String KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX;
        } else {
            return KEY_PREFIX;
        }
    }
    private String VERSION_KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_VERSION_KEY_PREFIX;
        } else {
            return VERSION_KEY_PREFIX;
        }
    }
    private byte[] ALL_KEY() {
        checkMerged();
        if (merged) {
            return MERGE_ALL_KEY;
        } else {
            return ALL_KEY;
        }
    }

    public void saveVersion(byte version) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(VERSION_KEY_PREFIX()), new byte[]{version});
    }

    public byte getVersion() {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(VERSION_KEY_PREFIX()));
        if (bytes == null || bytes.length == 0) {
            return 0;
        } else {
            return bytes[0];
        }
    }

    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int save(String address) throws Exception {
        if (StringUtils.isBlank(address)) {
            return 0;
        }
        boolean result = RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX() + address), HtgConstant.EMPTY_BYTE);
        if (result) {
            StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
            if(setPo == null) {
                setPo = new StringSetPo();
                Set<String> set = new HashSet<>();
                set.add(address);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
            } else {
                Set<String> set = setPo.getCollection();
                if(!set.contains(address)) {
                    set.add(address);
                    result = ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public boolean isExist(String address) {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX() + address));
        if(bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteByAddress(String address) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX() + address));
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
        if(setPo != null) {
            setPo.getCollection().remove(address);
            ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
        }
    }

    @Override
    public Set<String> findAll() {
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
        if (setPo == null) {
            return null;
        }
        Set<String> set = setPo.getCollection();
        return set;
    }
}
