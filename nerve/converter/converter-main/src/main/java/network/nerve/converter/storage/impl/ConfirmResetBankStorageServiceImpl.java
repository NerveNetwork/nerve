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

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.ConfirmResetBankStorageService;

/**
 * @author: Loki
 * @date: 2020/6/26
 */
@Component
public class ConfirmResetBankStorageServiceImpl implements ConfirmResetBankStorageService {

    @Override
    public boolean save(Chain chain, NulsHash resetTxhash, NulsHash confirmTxHash) {
        if(null == resetTxhash || null == confirmTxHash){
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_RESET_BANK_PREFIX + chain.getChainId(),
                    resetTxhash.getBytes(), confirmTxHash.getBytes());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public NulsHash get(Chain chain, NulsHash resetTxhash) {
        if(null == resetTxhash){
            return null;
        }
        byte[] hashBytes = RocksDBService.get(ConverterDBConstant.DB_RESET_BANK_PREFIX + chain.getChainId(),
                resetTxhash.getBytes());
        if(null == hashBytes || hashBytes.length <= 0){
            return null;
        }
        return new NulsHash(hashBytes);
    }

    @Override
    public boolean remove(Chain chain, NulsHash resetTxhash) {
        if(null == resetTxhash){
            return false;
        }
        try{
            return RocksDBService.delete(ConverterDBConstant.DB_RESET_BANK_PREFIX + chain.getChainId(), resetTxhash.getBytes());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}
