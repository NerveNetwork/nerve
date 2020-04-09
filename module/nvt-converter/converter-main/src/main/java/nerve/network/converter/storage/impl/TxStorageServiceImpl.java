/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

import io.nuls.base.data.NulsHash;
import nerve.network.converter.constant.ConverterDBConstant;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.TransactionPO;
import nerve.network.converter.storage.TxStorageService;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;

/**
 * @author: Chino
 * @date: 2020-02-27
 */
@Component
public class TxStorageServiceImpl implements TxStorageService  {


    @Override
    public boolean save(Chain chain, TransactionPO tx) {
        if (tx == null) {
            return false;
        }
        byte[] txHashBytes = tx.getTx().getHash().getBytes();
        boolean result = false;
        try {
            result = RocksDBService.put(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), txHashBytes, tx.serialize());
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return result;
    }

    @Override
    public TransactionPO get(Chain chain, NulsHash hash) {
        if (hash == null) {
            return null;
        }
        return getTx(chain, hash.getBytes());
    }

    @Override
    public TransactionPO get(Chain chain, String hash) {
        if (StringUtils.isBlank(hash)) {
            return null;
        }
        return getTx(chain, HexUtil.decode(hash));
    }

    private TransactionPO getTx(Chain chain, byte[] hashSerialize) {
        byte[] txBytes = RocksDBService.get(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), hashSerialize);
        TransactionPO tx = null;
        if (null != txBytes) {
            try {
                tx = ConverterUtil.getInstance(txBytes, TransactionPO.class);
            } catch (Exception e) {
                chain.getLogger().error(e);
                return null;
            }
        }
        return tx;
    }

    @Override
    public boolean delete(Chain chain, NulsHash hash) {
        boolean result = false;
        try {
            result = RocksDBService.delete(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), hash.getBytes());
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return result;
    }

    @Override
    public boolean delete(Chain chain, String hash) {
        boolean result = false;
        try {
            result = RocksDBService.delete(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), HexUtil.decode(hash));
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return result;
    }

}
