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

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.model.po.WithdrawalAdditionalFeePO;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.ConverterDBUtil;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: Loki
 * @date: 2020-02-27
 */
@Component
public class TxStorageServiceImpl implements TxStorageService  {

    /**
     * 异构链 交易key前缀
     */
    private static final String HETEROGENEOUS_TX_PREFIX = "HETEROGENEOUS_TX_PREFIX_";
    private static final String WITHDRAWAL_ADDITIONAL_FEE_PREFIX = "WITHDRAWAL_ADDITIONAL_FEE_PREFIX_";

    @Override
    public boolean saveHeterogeneousHash(Chain chain, String heterogeneousHash) {
        if (StringUtils.isBlank(heterogeneousHash)) {
            return false;
        }
        try {
            byte[] key = ConverterDBUtil.stringToBytes(HETEROGENEOUS_TX_PREFIX + heterogeneousHash);
            byte[] value = ConverterDBUtil.stringToBytes(heterogeneousHash);
            return RocksDBService.put(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), key, value);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public String getHeterogeneousHash(Chain chain, String heterogeneousHash) {
        if (StringUtils.isBlank(heterogeneousHash)) {
            return null;
        }
        try {
            byte[] key = ConverterDBUtil.stringToBytes(HETEROGENEOUS_TX_PREFIX + heterogeneousHash);
            byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), key);
            return null == bytes ? null : ConverterDBUtil.bytesToString(bytes);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    @Override
    public boolean save(Chain chain, TransactionPO tx) {
        if (tx == null) {
            return false;
        }
        byte[] txHashBytes = tx.getTx().getHash().getBytes();
        try {
           return RocksDBService.put(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), txHashBytes, tx.serialize());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
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


    @Override
    public boolean saveWithdrawalAdditionalFee(Chain chain, WithdrawalAdditionalFeePO po) {
        if (null == po) {
            return false;
        }
        try {
            byte[] key = ConverterDBUtil.stringToBytes(HETEROGENEOUS_TX_PREFIX + po.getBasicTxHash());
            return ConverterDBUtil.putModel(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), key, po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public WithdrawalAdditionalFeePO getWithdrawalAdditionalFeePO(Chain chain, String withdrawalTxHash) {
        if(StringUtils.isBlank(withdrawalTxHash)){
            return null;
        }
        byte[] key = ConverterDBUtil.stringToBytes(HETEROGENEOUS_TX_PREFIX + withdrawalTxHash);
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_TX_PREFIX + chain.getChainId(), key, WithdrawalAdditionalFeePO.class);
    }
}
