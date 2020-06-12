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
package network.nerve.converter.heterogeneouschain.eth.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;

import static network.nerve.converter.utils.ConverterDBUtil.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
@Component
public class EthTxRelationStorageServiceImpl implements EthTxRelationStorageService {

    private final String baseArea = EthDBConstant.DB_ETH;
    //private final String KEY_PREFIX_NERVE = "TXRELATION-N-";
    private final String KEY_PREFIX_ETH = "TXRELATION-E-";

    @Override
    public int save(String ethTxHash, String nerveTxHash) throws Exception {
        //RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveTxHash), stringToBytes(ethTxHash));
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_ETH + ethTxHash), stringToBytes(nerveTxHash));
        return 1;
    }

    @Override
    public String findNerveTxHash(String ethTxHash) {
        if (StringUtils.isBlank(ethTxHash)) {
            return null;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_ETH + ethTxHash));
        if (bytes == null) {
            return null;
        }
        return bytesToString(bytes);
    }

    @Override
    public void deleteByTxHash(String ethTxHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_ETH + ethTxHash));
    }

    /*@Override
    public String findEthTxHash(String nerveTxHash) {
        if (StringUtils.isBlank(nerveTxHash)) {
            return null;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveTxHash));
        if (bytes == null) {
            return null;
        }
        return bytesToString(bytes);
    }*/
}
