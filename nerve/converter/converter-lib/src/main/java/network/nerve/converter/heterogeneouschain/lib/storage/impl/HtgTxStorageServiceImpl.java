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

import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.utils.ConverterDBUtil;

/**
 * 保存交易信息 - 充值、签名完成的提现、变更
 * @author: Mimi
 * @date: 2020-03-12
 */
public class HtgTxStorageServiceImpl implements HtgTxStorageService {

    private String baseArea;
    private final String KEY_PREFIX = "BROADCAST-";
    private final String MERGE_KEY_PREFIX;

    private final HtgContext htgContext;
    public HtgTxStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
        int htgChainId = htgContext.HTG_CHAIN_ID();
        this.MERGE_KEY_PREFIX = htgChainId + "_BROADCAST-";
    }

    private boolean merged = false;
    private void checkMerged() {
        if (merged) {
            return;
        }
        merged = htgContext.getConverterCoreApi().isDbMerged(htgContext.HTG_CHAIN_ID());
        if (merged) {
            this.baseArea = htgContext.getConverterCoreApi().mergedDBName();
        }
    }
    private String KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX;
        } else {
            return KEY_PREFIX;
        }
    }
    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int save(HeterogeneousTransactionInfo po) throws Exception {
        if (po == null) {
            return 0;
        }
        String txHash = po.getTxHash();
        boolean result = ConverterDBUtil.putModel(baseArea(), ConverterDBUtil.stringToBytes(KEY_PREFIX() + txHash), po);
        return result ? 1 : 0;
    }

    @Override
    public HeterogeneousTransactionInfo findByTxHash(String txHash) {
        return ConverterDBUtil.getModel(baseArea(), ConverterDBUtil.stringToBytes(KEY_PREFIX() + txHash), HeterogeneousTransactionInfo.class);
    }

    @Override
    public void deleteByTxHash(String txHash) throws Exception {
        RocksDBService.delete(baseArea(), ConverterDBUtil.stringToBytes(KEY_PREFIX() + txHash));
    }

}
