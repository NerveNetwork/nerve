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
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgBlockHeaderStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgBlockHeaderStorageServiceImpl implements HtgBlockHeaderStorageService {

    private String baseArea;
    private final String KEY_PREFIX = "HEADER-";
    private final String SYNCED_KEY_PREFIX = "SYNCED_HEADER-";
    private final byte[] LOCAL_LATEST_HEADER_KEY = stringToBytes("HEADER-LOCAL_LATEST_HEADER");
    private final String MERGE_KEY_PREFIX;
    private final String MERGE_SYNCED_KEY_PREFIX;
    private final byte[] MERGE_LOCAL_LATEST_HEADER_KEY;

    private final HtgContext htgContext;
    public HtgBlockHeaderStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
        int htgChainId = htgContext.HTG_CHAIN_ID();
        this.MERGE_KEY_PREFIX = htgChainId + "_" + KEY_PREFIX;
        this.MERGE_SYNCED_KEY_PREFIX = htgChainId + "_" + SYNCED_KEY_PREFIX;
        this.MERGE_LOCAL_LATEST_HEADER_KEY = stringToBytes(htgChainId + "_HEADER-LOCAL_LATEST_HEADER");
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
    private String SYNCED_KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_SYNCED_KEY_PREFIX;
        } else {
            return SYNCED_KEY_PREFIX;
        }
    }
    private byte[] LOCAL_LATEST_HEADER_KEY() {
        checkMerged();
        if (merged) {
            return MERGE_LOCAL_LATEST_HEADER_KEY;
        } else {
            return LOCAL_LATEST_HEADER_KEY;
        }
    }

    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int saveSynced(long height) throws Exception {
        boolean result = RocksDBService.put(baseArea(), stringToBytes(SYNCED_KEY_PREFIX() + height), HtgConstant.EMPTY_BYTE);
        return result ? 1 : 0;
    }

    @Override
    public boolean isSynced(long height) {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(SYNCED_KEY_PREFIX() + height));
        return bytes != null;
    }

    @Override
    public int save(HtgSimpleBlockHeader blockHeader) throws Exception {
        if (blockHeader == null) {
            return 0;
        }
        boolean result = ConverterDBUtil.putModel(baseArea(), stringToBytes(KEY_PREFIX() + blockHeader.getHeight()), blockHeader);
        if (result) {
            result = ConverterDBUtil.putModel(baseArea(), LOCAL_LATEST_HEADER_KEY(), blockHeader);
            if (!result) {
                this.deleteByHeight(blockHeader.getHeight());
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HtgSimpleBlockHeader findLatest() {
        return ConverterDBUtil.getModel(baseArea(), LOCAL_LATEST_HEADER_KEY(), HtgSimpleBlockHeader.class);
    }

    @Override
    public HtgSimpleBlockHeader findByHeight(Long height) {
        return ConverterDBUtil.getModel(baseArea(), stringToBytes(KEY_PREFIX() + height), HtgSimpleBlockHeader.class);
    }

    @Override
    public void deleteByHeight(Long height) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX() + height));
        HtgSimpleBlockHeader latest = this.findLatest();
        if (latest.getHeight().longValue() == height.longValue()) {
            HtgSimpleBlockHeader header = this.findByHeight(height - 1);
            if(header != null) {
                ConverterDBUtil.putModel(baseArea(), LOCAL_LATEST_HEADER_KEY(), header);
            } else {
                RocksDBService.delete(baseArea(), LOCAL_LATEST_HEADER_KEY());
            }
        }
    }

}
