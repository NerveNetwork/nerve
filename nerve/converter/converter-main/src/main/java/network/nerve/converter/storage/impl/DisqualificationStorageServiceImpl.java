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

package network.nerve.converter.storage.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.DisqualificationStorageService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020/5/19
 */
@Component
public class DisqualificationStorageServiceImpl implements DisqualificationStorageService {

    @Override
    public boolean save(Chain chain, byte[] address) {
        try {
            return RocksDBService.put(ConverterDBConstant.DB_DISQUALIFICATION_PREFIX + chain.getChainId(), address, address);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }

    @Override
    public String find(Chain chain, byte[] address) {
        byte[] addressBytes = RocksDBService.get(ConverterDBConstant.DB_DISQUALIFICATION_PREFIX + chain.getChainId(), address);
        if(null == addressBytes || addressBytes.length == 0){
            return null;
        }
        return AddressTool.getStringAddressByBytes(address);
    }

    @Override
    public boolean delete(Chain chain, byte[] address) {
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_DISQUALIFICATION_PREFIX + chain.getChainId(), address);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }

    @Override
    public List<String> findAll(Chain chain) {
        List<byte[]> keys = RocksDBService.keyList(ConverterDBConstant.DB_DISQUALIFICATION_PREFIX + chain.getChainId());
        List<String> list = new ArrayList<>();
        if(null != keys){
            for(byte[] key : keys){
                list.add(AddressTool.getStringAddressByBytes(key));
            }
        }
        return list;
    }
}
