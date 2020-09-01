/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.storage.MergeComponentStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.constant.ConverterDBConstant.DB_MERGE_COMPONENT_PREFIX;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/7/30
 */
@Component
public class MergeComponentStorageServiceImpl implements MergeComponentStorageService {

    private final String MERGED_TX_KEY = "MERGED_TX_KEY_";

    @Override
    public boolean saveMergeComponentCall(Chain chain, String hash, MergedComponentCallPO po) {
        if (po == null) {
            return false;
        }
        int chainId = chain.getChainId();
        try {
            byte[] hashBytes = NulsHash.fromHex(hash).getBytes();
            ConverterDBUtil.putModel(DB_MERGE_COMPONENT_PREFIX + chainId, hashBytes, po);
            for (String txHash : po.getListTxHash()) {
                RocksDBService.put(DB_MERGE_COMPONENT_PREFIX + chainId, stringToBytes(MERGED_TX_KEY + txHash), hashBytes);
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public MergedComponentCallPO findMergedTx(Chain chain, String hash) {
        return ConverterDBUtil.getModel(DB_MERGE_COMPONENT_PREFIX + chain.getChainId(),
                NulsHash.fromHex(hash).getBytes(),
                MergedComponentCallPO.class);
    }

    @Override
    public String getMergedTxKeyByMember(Chain chain, String hash) {
        byte[] bytes = RocksDBService.get(DB_MERGE_COMPONENT_PREFIX + chain.getChainId(), stringToBytes(MERGED_TX_KEY + hash));
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        NulsHash keyHash = new NulsHash(bytes);
        return keyHash.toHex();
    }

    @Override
    public boolean removeMergedTx(Chain chain, String hash) throws Exception {
        MergedComponentCallPO mergedTxPO = findMergedTx(chain, hash);
        for (String txHash : mergedTxPO.getListTxHash()) {
            RocksDBService.delete(DB_MERGE_COMPONENT_PREFIX + chain.getChainId(), stringToBytes(MERGED_TX_KEY + txHash));
        }
        RocksDBService.delete(DB_MERGE_COMPONENT_PREFIX + chain.getChainId(), NulsHash.fromHex(hash).getBytes());
        return true;
    }
}
