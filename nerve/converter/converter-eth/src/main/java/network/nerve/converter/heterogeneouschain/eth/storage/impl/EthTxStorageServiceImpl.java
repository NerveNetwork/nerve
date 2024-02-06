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
import network.nerve.converter.heterogeneouschain.eth.model.EthRecoveryDto;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.utils.ConverterDBUtil;

/**
 * Save transaction information - Recharge、Withdrawal completed with signature、change
 * @author: Mimi
 * @date: 2020-03-12
 */
@Component
public class EthTxStorageServiceImpl implements EthTxStorageService {

    private String baseArea = EthDBConstant.DB_ETH;
    private final String KEY_PREFIX = "BROADCAST-";
    private final String RECOVERY_KEY_PREFIX = "RECOVERY-";
    private final String MERGE_KEY_PREFIX;
    private final String MERGE_RECOVERY_KEY_PREFIX;

    public EthTxStorageServiceImpl() {
        int htgChainId = 101;
        this.MERGE_KEY_PREFIX = htgChainId + "_BROADCAST-";
        this.MERGE_RECOVERY_KEY_PREFIX = htgChainId + "_RECOVERY-";
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
    private String RECOVERY_KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_RECOVERY_KEY_PREFIX;
        } else {
            return RECOVERY_KEY_PREFIX;
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

    @Override
    public int saveRecovery(String nerveTxKey, EthRecoveryDto recovery) throws Exception {
        if (recovery == null) {
            return 0;
        }
        ConverterDBUtil.putModel(baseArea(), ConverterDBUtil.stringToBytes(RECOVERY_KEY_PREFIX() + nerveTxKey), recovery);
        return 1;
    }

    @Override
    public EthRecoveryDto findRecoveryByNerveTxKey(String nerveTxKey) {
        return ConverterDBUtil.getModel(baseArea(), ConverterDBUtil.stringToBytes(RECOVERY_KEY_PREFIX() + nerveTxKey), EthRecoveryDto.class);
    }
}
