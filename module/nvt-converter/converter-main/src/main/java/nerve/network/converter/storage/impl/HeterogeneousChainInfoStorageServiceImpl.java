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
package nerve.network.converter.storage.impl;


import nerve.network.converter.constant.ConverterDBConstant;
import nerve.network.converter.model.bo.HeterogeneousChainInfo;
import nerve.network.converter.model.po.IntegerSetPo;
import nerve.network.converter.storage.HeterogeneousChainInfoStorageService;
import nerve.network.converter.utils.ConverterDBUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nerve.network.converter.utils.ConverterDBUtil.stringToBytes;


/**
 * @desription:
 * @author: Chino
 * @date: 2018/5/24
 */
@Component
public class HeterogeneousChainInfoStorageServiceImpl implements HeterogeneousChainInfoStorageService {

    private final String baseArea = ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO;
    private final String KEY_PREFIX = "HETEROGENEOUS_CHAIN_INFO-";
    private final byte[] ALL_KEY = stringToBytes("HETEROGENEOUS_CHAIN_INFO-ALL");

    @Override
    public int saveHeterogeneousChainInfo(int heterogeneousChainId, HeterogeneousChainInfo info) throws Exception {
        if (info == null) {
            return 0;
        }
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId), info);
        if(result) {
            IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
            if(setPo == null) {
                setPo = new IntegerSetPo();
                Set<Integer> set = new HashSet<>();
                set.add(heterogeneousChainId);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
            } else {
                Set<Integer> set = setPo.getCollection();
                if(!set.contains(heterogeneousChainId)) {
                    set.add(heterogeneousChainId);
                    result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HeterogeneousChainInfo getHeterogeneousChainInfo(int heterogeneousChainId) {
        HeterogeneousChainInfo info = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId), HeterogeneousChainInfo.class);
        return info;
    }

    @Override
    public void deleteHeterogeneousChainInfo(int heterogeneousChainId) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId));
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
        setPo.getCollection().remove(heterogeneousChainId);
        ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
    }

    @Override
    public boolean isExistHeterogeneousChainInfo(int heterogeneousChainId) {
        byte[] contract = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId));
        if (contract == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<HeterogeneousChainInfo> getAllHeterogeneousChainInfoList() {
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
        if (setPo == null) {
            return null;
        }
        Set<Integer> set = setPo.getCollection();
        List<HeterogeneousChainInfo> resultList = new ArrayList<>();
        for (Integer id : set) {
            resultList.add(this.getHeterogeneousChainInfo(id));
        }
        return resultList;
    }

}
