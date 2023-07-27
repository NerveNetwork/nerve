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

package io.nuls.transaction.storage.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.transaction.constant.TxDBConstant;
import io.nuls.transaction.storage.LockedAddressStorageService;

import java.util.ArrayList;
import java.util.List;

import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * 记录被冻结的账户地址
 * k:v 都是冻结地址
 * @author: Charlie
 * @date: 2020/5/19
 */
@Component
public class LockedAddressStorageServiceImpl implements LockedAddressStorageService {

    @Override
    public boolean save(int chainId, String address) {
        if(StringUtils.isBlank(address)){
            return false;
        }
        try {
            byte[] addressByte = AddressTool.getAddress(address);
            return RocksDBService.put(TxDBConstant.DB_LOCKED_ADDRESS + chainId, addressByte, addressByte);
        } catch (Exception e) {
            LOG.error(e);
        }
        return false;
    }

    @Override
    public String find(int chainId, String address) {
        if(StringUtils.isBlank(address)){
            return null;
        }
        byte[] key = AddressTool.getAddress(address);
        byte[] addressBytes = RocksDBService.get(TxDBConstant.DB_LOCKED_ADDRESS + chainId, key);
        if(null == addressBytes || addressBytes.length == 0){
            return null;
        }
        return address;
    }

    @Override
    public boolean delete(int chainId, String address) {
        if(StringUtils.isBlank(address)){
            return false;
        }
        try {
            byte[] key = AddressTool.getAddress(address);
            return RocksDBService.delete(TxDBConstant.DB_LOCKED_ADDRESS + chainId, key);
        } catch (Exception e) {
            LOG.error(e);
        }
        return false;
    }

    @Override
    public List<String> findAll(int chainId) {
        List<byte[]> keys = RocksDBService.keyList(TxDBConstant.DB_LOCKED_ADDRESS + chainId);
        List<String> list = new ArrayList<>();
        if(null != keys){
            for(byte[] key : keys){
                list.add(AddressTool.getStringAddressByBytes(key));
            }
        }
        return list;
    }
}
