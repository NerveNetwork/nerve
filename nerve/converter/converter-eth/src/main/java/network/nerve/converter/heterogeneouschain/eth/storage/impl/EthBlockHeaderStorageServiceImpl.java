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

package network.nerve.converter.heterogeneouschain.eth.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.eth.storage.EthBlockHeaderStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class EthBlockHeaderStorageServiceImpl implements EthBlockHeaderStorageService {

    private String baseArea = EthDBConstant.DB_ETH;
    private final String KEY_PREFIX = "HEADER-";
    private final byte[] LOCAL_LATEST_HEADER_KEY = stringToBytes("HEADER-LOCAL_LATEST_HEADER");
    private final String MERGE_KEY_PREFIX;
    private final byte[] MERGE_LOCAL_LATEST_HEADER_KEY;

    public EthBlockHeaderStorageServiceImpl() {
        int htgChainId = 101;
        this.MERGE_KEY_PREFIX = htgChainId + "_" + KEY_PREFIX;
        this.MERGE_LOCAL_LATEST_HEADER_KEY = stringToBytes(htgChainId + "_HEADER-LOCAL_LATEST_HEADER");
    }

    private boolean merged = false;

    private void checkMerged() {
        if (merged) {
            return;
        }
        merged = EthContext.getConverterCoreApi().isDbMerged(101);
        if (merged) {
            this.baseArea = EthContext.getConverterCoreApi().mergedDBName();
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
    public int save(EthSimpleBlockHeader blockHeader) throws Exception {
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
    public EthSimpleBlockHeader findLatest() {
        return ConverterDBUtil.getModel(baseArea(), LOCAL_LATEST_HEADER_KEY(), EthSimpleBlockHeader.class);
    }

    @Override
    public EthSimpleBlockHeader findByHeight(Long height) {
        return ConverterDBUtil.getModel(baseArea(), stringToBytes(KEY_PREFIX() + height), EthSimpleBlockHeader.class);
    }

    @Override
    public void deleteByHeight(Long height) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX() + height));
        EthSimpleBlockHeader latest = this.findLatest();
        if (latest.getHeight().longValue() == height.longValue()) {
            EthSimpleBlockHeader header = this.findByHeight(height - 1);
            if (header != null) {
                ConverterDBUtil.putModel(baseArea(), LOCAL_LATEST_HEADER_KEY(), header);
            } else {
                RocksDBService.delete(baseArea(), LOCAL_LATEST_HEADER_KEY());
            }
        }
    }

}
