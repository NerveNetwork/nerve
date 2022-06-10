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
package network.nerve.converter.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.po.HeterogeneousConfirmedChangeVBPo;
import network.nerve.converter.model.po.StringSetPo;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.HashSet;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * 确认虚拟银行管理员变更
 * @author: Mimi
 * @date: 2020-03-12
 */
@Component
public class HeterogeneousConfirmedChangeVBStorageServiceImpl implements HeterogeneousConfirmedChangeVBStorageService {

    private final String baseArea = ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO;
    private final String KEY_PREFIX = "CONFIRMED_CHANGE_VB-";
    private final byte[] ALL_KEY = stringToBytes("CONFIRMED_CHANGE_VB-ALL");

    @Override
    public int save(HeterogeneousConfirmedChangeVBPo po) throws Exception {
        if (po == null) {
            return 0;
        }
        String txHash = po.getNerveTxHash();
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + txHash), po);
        if(result) {
            StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, StringSetPo.class);
            if(setPo == null) {
                setPo = new StringSetPo();
                Set<String> set = new HashSet<>();
                set.add(txHash);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
            } else {
                Set<String> set = setPo.getCollection();
                if(!set.contains(txHash)) {
                    set.add(txHash);
                    result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HeterogeneousConfirmedChangeVBPo findByTxHash(String txHash) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + txHash), HeterogeneousConfirmedChangeVBPo.class);
    }

    @Override
    public void deleteByTxHash(String txHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + txHash));
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, StringSetPo.class);
        if(setPo != null) {
            setPo.getCollection().remove(txHash);
            ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
        }
    }
}
