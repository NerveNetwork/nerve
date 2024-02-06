/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.PersistentCacheStroageService;
import network.nerve.converter.utils.ConverterDBUtil;

/**
 * @author: Loki
 * @date: 2020/7/2
 */
@Component
public class PersistentCacheStroageServiceImpl implements PersistentCacheStroageService {

    @Override
    public boolean saveCacheState(Chain chain, String key, int value) {
        try {
            // Shared Table Name(In order to reduce the number of tables)
            return RocksDBService.put(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(), ConverterDBUtil.stringToBytes(key), ByteUtils.intToBytes(value));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
    @Override
    public Integer getCacheState(Chain chain, String key) {
        try {
            byte[] value = RocksDBService.get(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(), ConverterDBUtil.stringToBytes(key));
            if(null == value || value.length == 0){
                return null;
            }
            return ByteUtils.bytesToInt(value);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }
}
