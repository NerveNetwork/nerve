/*
 * MIT License
 *
 * Copyright (c) 2019-2022 nerve.network
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package network.nerve.converter.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.ConfigBean;
import network.nerve.converter.storage.ConfigStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration Information Storage Management Class
 * Configuration Information Storage Management Class
 *
 * @author qinyifeng
 * @date 2018/12/11
 */
@Component
public class ConfigStorageServiceImpl implements ConfigStorageService {

    @Override
    public boolean save(ConfigBean bean, int chainID) throws Exception {
        if (bean == null) {
            return false;
        }
        return RocksDBService.put(ConverterDBConstant.DB_MODULE_CONGIF, ByteUtils.intToBytes(chainID), ObjectUtils.objectToBytes(bean));
    }

    @Override
    public ConfigBean get(int chainID) {
        try {
            byte[] value = RocksDBService.get(ConverterDBConstant.DB_MODULE_CONGIF, ByteUtils.intToBytes(chainID));
            return ObjectUtils.bytesToObject(value);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public boolean delete(int chainID) {
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_MODULE_CONGIF, ByteUtils.intToBytes(chainID));
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    @Override
    public Map<Integer, ConfigBean> getList() {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConverterDBConstant.DB_MODULE_CONGIF);
            Map<Integer, ConfigBean> configBeanMap = new HashMap<>(8);
            for (Entry<byte[], byte[]> entry : list) {
                int key = ByteUtils.bytesToInt(entry.getKey());
                ConfigBean value = ObjectUtils.bytesToObject(entry.getValue());
                if(value != null) {
                    configBeanMap.put(key, value);
                }
            }
            return configBeanMap;
        } catch (Exception e) {
            Log.warn(e.getMessage());
            return null;
        }
    }
}
