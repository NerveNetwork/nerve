/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.lib.storage.impl;

import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxRelationStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
public class HtgTxRelationStorageServiceImpl implements HtgTxRelationStorageService {

    private final String baseArea;
    private final String KEY_PREFIX_NERVE = "TXRELATION-N-";
    private final String KEY_PREFIX_HT = "TXRELATION-E-";
    private final String KEY_PREFIX_HT_PO = "TXRELATION-P-";

    private final HtgContext htgContext;
    public HtgTxRelationStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
    }

    @Override
    public int save(String htTxHash, String nerveTxHash, HtgSendTransactionPo ethTxPo) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_HT + htTxHash), stringToBytes(nerveTxHash));
        ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_HT_PO + htTxHash), ethTxPo);
        return 1;
    }

    @Override
    public String findNerveTxHash(String htTxHash) {
        if (StringUtils.isBlank(htTxHash)) {
            return null;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_HT + htTxHash));
        if (bytes == null) {
            return null;
        }
        return bytesToString(bytes);
    }

    @Override
    public HtgSendTransactionPo findEthSendTxPo(String htTxHash) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_HT_PO + htTxHash), HtgSendTransactionPo.class);
    }

    @Override
    public void deleteByTxHash(String htTxHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_HT + htTxHash));
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_HT_PO + htTxHash));
    }

    @Override
    public int saveNerveTxHash(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveTxHash), HtgConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean existNerveTxHash(String nerveTxHash) {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }
}
