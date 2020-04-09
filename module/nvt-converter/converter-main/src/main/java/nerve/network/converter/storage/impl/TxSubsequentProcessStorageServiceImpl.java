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

import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.TxSubsequentProcessKeyListPO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.utils.ConverterDBUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;

import java.util.ArrayList;
import java.util.List;

import static nerve.network.converter.constant.ConverterDBConstant.DB_PENDING_PREFIX;
import static nerve.network.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Chino
 * @date: 2020-03-09
 */
@Component
public class TxSubsequentProcessStorageServiceImpl implements TxSubsequentProcessStorageService {

    private final byte[] PENDING_TX_ALL_KEY = stringToBytes("PENDING_TX-ALL");

    @Override
    public boolean save(Chain chain, TxSubsequentProcessPO po) {
        if (po == null) {
            return false;
        }
        boolean result = false;
        int chainId = chain.getChainId();
        try {
            String txHash = po.getTx().getHash().toHex();
            result = ConverterDBUtil.putModel(DB_PENDING_PREFIX + chainId, stringToBytes(txHash), po);
            if (result) {
                TxSubsequentProcessKeyListPO listPO = ConverterDBUtil.getModel(DB_PENDING_PREFIX,
                        PENDING_TX_ALL_KEY, TxSubsequentProcessKeyListPO.class);
                if (listPO == null) {
                    listPO = new TxSubsequentProcessKeyListPO();
                    List<String> list = new ArrayList<>();
                    list.add(txHash);
                    listPO.setListTxHash(list);
                    result = ConverterDBUtil.putModel(DB_PENDING_PREFIX + chainId, PENDING_TX_ALL_KEY, listPO);
                } else {
                    listPO.getListTxHash().add(txHash);
                    result = ConverterDBUtil.putModel(DB_PENDING_PREFIX + chainId, PENDING_TX_ALL_KEY, listPO);
                }
            }
            return result;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }

    }

    @Override
    public TxSubsequentProcessPO get(Chain chain, String txHash) {
        return ConverterDBUtil.getModel(DB_PENDING_PREFIX + chain.getChainId(), stringToBytes(txHash), TxSubsequentProcessPO.class);
    }

    @Override
    public void delete(Chain chain, String txHash) {
        try {
            int chainId = chain.getChainId();
            RocksDBService.delete(DB_PENDING_PREFIX + chainId, stringToBytes(txHash));
            TxSubsequentProcessKeyListPO listPO = ConverterDBUtil.getModel(DB_PENDING_PREFIX + chainId,
                    PENDING_TX_ALL_KEY, TxSubsequentProcessKeyListPO.class);
            listPO.getListTxHash().remove(txHash);

            ConverterDBUtil.putModel(DB_PENDING_PREFIX + chainId, PENDING_TX_ALL_KEY, listPO);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    @Override
    public List<TxSubsequentProcessPO> findAll(Chain chain) {
        TxSubsequentProcessKeyListPO listPO = ConverterDBUtil.getModel(DB_PENDING_PREFIX + chain.getChainId(),
                PENDING_TX_ALL_KEY, TxSubsequentProcessKeyListPO.class);
        if(null == listPO){
            return null;
        }
        List<TxSubsequentProcessPO> list = new ArrayList<>();
        for (String txHash : listPO.getListTxHash()) {
            list.add(this.get(chain, txHash));
        }
        return list;
    }
}
