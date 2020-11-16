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

package network.nerve.converter.heterogeneouschain.bnb.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbBlockHeaderStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class BnbBlockHeaderStorageServiceImpl implements BnbBlockHeaderStorageService {

    private final String baseArea = BnbDBConstant.DB_BNB;
    private final String KEY_PREFIX = "HEADER-";
    private final byte[] LOCAL_LATEST_HEADER_KEY = stringToBytes("HEADER-LOCAL_LATEST_HEADER");

    @Override
    public int save(BnbSimpleBlockHeader blockHeader) throws Exception {
        if (blockHeader == null) {
            return 0;
        }
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + blockHeader.getHeight()), blockHeader);
        if (result) {
            result = ConverterDBUtil.putModel(baseArea, LOCAL_LATEST_HEADER_KEY, blockHeader);
            if (!result) {
                this.deleteByHeight(blockHeader.getHeight());
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public BnbSimpleBlockHeader findLatest() {
        return ConverterDBUtil.getModel(baseArea, LOCAL_LATEST_HEADER_KEY, BnbSimpleBlockHeader.class);
    }

    @Override
    public BnbSimpleBlockHeader findByHeight(Long height) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + height), BnbSimpleBlockHeader.class);
    }

    @Override
    public void deleteByHeight(Long height) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + height));
        BnbSimpleBlockHeader latest = this.findLatest();
        if (latest.getHeight().longValue() == height.longValue()) {
            BnbSimpleBlockHeader header = this.findByHeight(height - 1);
            if(header != null) {
                ConverterDBUtil.putModel(baseArea, LOCAL_LATEST_HEADER_KEY, header);
            } else {
                RocksDBService.delete(baseArea, LOCAL_LATEST_HEADER_KEY);
            }
        }
    }

}
