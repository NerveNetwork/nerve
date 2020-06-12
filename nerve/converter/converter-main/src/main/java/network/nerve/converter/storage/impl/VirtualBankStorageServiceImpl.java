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

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020-03-03
 */
@Component
public class VirtualBankStorageServiceImpl implements VirtualBankStorageService {

    @Override
    public boolean save(Chain chain, VirtualBankDirector po) {
        if(null == po){
            return false;
        }
        try {
            return ConverterDBUtil.putModel(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(po.getSignAddress()), po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }

    }

    @Override
    public VirtualBankDirector findBySignAddress(Chain chain, String address) {
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(address), VirtualBankDirector.class);
    }

    @Override
    public boolean deleteBySignAddress(Chain chain, String address){
        if(StringUtils.isBlank(address)){
            chain.getLogger().error("deleteBySignAddress key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(address));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public Map<String, VirtualBankDirector> findAll(Chain chain) {
        List<Entry<byte[], byte[]>> listEntry = RocksDBService.entryList(ConverterDBConstant.DB_VIRTUAL_BANK_PREFIX + chain.getChainId());
        if(null == listEntry){
            return null;
        }
        Map<String, VirtualBankDirector> map = new HashMap<>();
        for(Entry<byte[], byte[]> entry : listEntry){
            VirtualBankDirector vbd = ConverterDBUtil.getModel(entry.getValue(), VirtualBankDirector.class);
            map.put(vbd.getSignAddress(), vbd);
        }
        return map;
    }
}
