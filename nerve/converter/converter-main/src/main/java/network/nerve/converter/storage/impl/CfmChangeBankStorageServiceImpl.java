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
import network.nerve.converter.model.po.ConfirmedChangeVirtualBankPO;
import network.nerve.converter.storage.CfmChangeBankStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020-03-11
 */
@Component
public class CfmChangeBankStorageServiceImpl implements CfmChangeBankStorageService {


    @Override
    public boolean save(Chain chain, ConfirmedChangeVirtualBankPO po) {
        if(null == po){
            return false;
        }
        try {
            return ConverterDBUtil.putModel(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chain.getChainId(),
                    stringToBytes(po.getChangeVirtualBankTxHash().toHex()), po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public ConfirmedChangeVirtualBankPO find(Chain chain, String hash) {
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(hash), ConfirmedChangeVirtualBankPO.class);
    }

    @Override
    public boolean delete(Chain chain, String hash) {
        if(StringUtils.isBlank(hash)){
            chain.getLogger().error("delete key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chain.getChainId(), stringToBytes(hash));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public Map<String, ConfirmedChangeVirtualBankPO> findAll(Chain chain) {
        List<Entry<byte[], byte[]>> listEntry = RocksDBService.entryList(ConverterDBConstant.DB_CFM_VIRTUAL_BANK_PREFIX + chain.getChainId());
        if(null == listEntry){
            return null;
        }
        Map<String, ConfirmedChangeVirtualBankPO> map = new HashMap<>();
        for(Entry<byte[], byte[]> entry : listEntry){
            ConfirmedChangeVirtualBankPO po = ConverterDBUtil.getModel(entry.getValue(), ConfirmedChangeVirtualBankPO.class);
            map.put(po.getChangeVirtualBankTxHash().toHex(), po);
        }
        return map;
    }
}
