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
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthSendTransactionPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
@Component
public class EthTxRelationStorageServiceImpl implements EthTxRelationStorageService {

    private String baseArea = EthDBConstant.DB_ETH;
    private final String KEY_PREFIX_NERVE = "TXRELATION-N-";
    private final String KEY_PREFIX_ETH = "TXRELATION-E-";
    private final String KEY_PREFIX_ETH_PO = "TXRELATION-P-";
    private final String MERGE_KEY_PREFIX_NERVE;
    private final String MERGE_KEY_PREFIX_ETH;
    private final String MERGE_KEY_PREFIX_ETH_PO;

    public EthTxRelationStorageServiceImpl() {
        int htgChainId = 101;
        this.MERGE_KEY_PREFIX_NERVE = htgChainId + "_TXRELATION-N-";
        this.MERGE_KEY_PREFIX_ETH = htgChainId + "_TXRELATION-E-";
        this.MERGE_KEY_PREFIX_ETH_PO = htgChainId + "_TXRELATION-P-";
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
    private String KEY_PREFIX_NERVE() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX_NERVE;
        } else {
            return KEY_PREFIX_NERVE;
        }
    }
    private String KEY_PREFIX_ETH() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX_ETH;
        } else {
            return KEY_PREFIX_ETH;
        }
    }
    private String KEY_PREFIX_ETH_PO() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX_ETH_PO;
        } else {
            return KEY_PREFIX_ETH_PO;
        }
    }
    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int save(String ethTxHash, String nerveTxHash, EthSendTransactionPo ethTxPo) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX_ETH() + ethTxHash), stringToBytes(nerveTxHash));
        ConverterDBUtil.putModel(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + ethTxHash), ethTxPo);
        return 1;
    }

    @Override
    public String findNerveTxHash(String ethTxHash) {
        if (StringUtils.isBlank(ethTxHash)) {
            return null;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX_ETH() + ethTxHash));
        if (bytes == null) {
            return null;
        }
        return bytesToString(bytes);
    }

    @Override
    public EthSendTransactionPo findEthSendTxPo(String ethTxHash) {
        return ConverterDBUtil.getModel(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + ethTxHash), EthSendTransactionPo.class);
    }

    @Override
    public void deleteByTxHash(String ethTxHash) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX_ETH() + ethTxHash));
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + ethTxHash));
    }

    @Override
    public int saveNerveTxHash(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX_NERVE() + nerveTxHash), EthConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean existNerveTxHash(String nerveTxHash) {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX_NERVE() + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }
}
