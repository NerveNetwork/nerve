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
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.storage.VirtualBankAllHistoryStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static io.nuls.core.model.ByteUtils.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/7/9
 */
@Component
public class VirtualBankAllHistoryStorageServiceImpl implements VirtualBankAllHistoryStorageService {

    private static final String AGENT_ADDRESS_PREFIX = "AGENT_ADDRESS_PREFIX_";
    @Override
    public boolean save(Chain chain, VirtualBankDirector po) {
        if(null == po){
            return false;
        }
        try {
            byte[] signAddress = stringToBytes(po.getSignAddress());
            boolean rs = ConverterDBUtil.putModel(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), signAddress, po);
            if(!rs){
                return false;
            }
            byte[] key = stringToBytes(AGENT_ADDRESS_PREFIX + po.getAgentAddress());
            rs = RocksDBService.put(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), key, signAddress);
            if(!rs){
                return false;
            }
            for(HeterogeneousAddress hAddress : po.getHeterogeneousAddrMap().values()) {
                rs = RocksDBService.put(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(hAddress.getAddress()), signAddress);
                if(!rs){
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public String findSignAddressByAgentAddress(Chain chain, String address) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(AGENT_ADDRESS_PREFIX + address));
        if(null != bytes && bytes.length > 0){
            return bytesToString(bytes);
        }
        return null;
    }

    @Override
    public VirtualBankDirector findBySignAddress(Chain chain, String address) {
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(address), VirtualBankDirector.class);
    }

    @Override
    public String findByHeterogeneousAddress(Chain chain, String heterogeneousAddress) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(heterogeneousAddress));
        if(null != bytes && bytes.length > 0){
            return bytesToString(bytes);
        }
        return null;
    }
}
