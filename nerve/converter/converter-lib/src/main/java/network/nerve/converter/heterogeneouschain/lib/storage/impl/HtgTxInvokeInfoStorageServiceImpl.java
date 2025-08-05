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
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxInvokeInfoStorageService;
import network.nerve.converter.model.po.StringListPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
public class HtgTxInvokeInfoStorageServiceImpl implements HtgTxInvokeInfoStorageService {

    private String baseArea;
    private final String KEY_PREFIX_NERVE_COMPLETED = "TXINVOKE-NC-";
    private final String KEY_PREFIX_NERVE_SENT = "TXINVOKE-NS-";
    private final String KEY_PREFIX_ETH_PO = "TXINVOKE-P-";
    private final byte[] WAITING_TX_ALL_KEY = stringToBytes("TXINVOKE-P-ALL");
    private final String MERGE_KEY_PREFIX_NERVE_COMPLETED;
    private final String MERGE_KEY_PREFIX_NERVE_SENT;
    private final String MERGE_KEY_PREFIX_ETH_PO;
    private final byte[] MERGE_WAITING_TX_ALL_KEY;
    private final byte[] LAST_NERVE_TXHASH_WITH_TRON_KEY;

    private final HtgContext htgContext;
    public HtgTxInvokeInfoStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
        int htgChainId = htgContext.HTG_CHAIN_ID();
        this.MERGE_KEY_PREFIX_NERVE_COMPLETED = htgChainId + "_TXINVOKE-NC-";
        this.MERGE_KEY_PREFIX_NERVE_SENT = htgChainId + "_TXINVOKE-NS-";
        this.MERGE_KEY_PREFIX_ETH_PO = htgChainId + "_TXINVOKE-P-";
        this.MERGE_WAITING_TX_ALL_KEY = stringToBytes(htgChainId + "_TXINVOKE-P-ALL");
        this.LAST_NERVE_TXHASH_WITH_TRON_KEY = stringToBytes(htgChainId + "_LAST_NERVE_TXHASH_WITH_TRON");
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
    private String KEY_PREFIX_NERVE_COMPLETED() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX_NERVE_COMPLETED;
        } else {
            return KEY_PREFIX_NERVE_COMPLETED;
        }
    }
    private String KEY_PREFIX_NERVE_SENT() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX_NERVE_SENT;
        } else {
            return KEY_PREFIX_NERVE_SENT;
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
    private byte[] WAITING_TX_ALL_KEY() {
        checkMerged();
        if (merged) {
            return MERGE_WAITING_TX_ALL_KEY;
        } else {
            return WAITING_TX_ALL_KEY;
        }
    }

    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int save(String nerveTxHash, HtgWaitingTxPo po) throws Exception {
        boolean result = ConverterDBUtil.putModel(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + nerveTxHash), po);
        if (result) {
            StringListPo setPo = ConverterDBUtil.getModel(baseArea(), WAITING_TX_ALL_KEY(), StringListPo.class);
            if (setPo == null) {
                setPo = new StringListPo();
                List<String> list = new ArrayList<>();
                list.add(nerveTxHash);
                setPo.setCollection(list);
                result = ConverterDBUtil.putModel(baseArea(), WAITING_TX_ALL_KEY(), setPo);
            } else {
                List<String> list = setPo.getCollection();
                Set<String> set = new HashSet<>(list);
                if (!set.contains(nerveTxHash)) {
                    list.add(nerveTxHash);
                    result = ConverterDBUtil.putModel(baseArea(), WAITING_TX_ALL_KEY(), setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HtgWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return ConverterDBUtil.getModel(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + nerveTxHash), HtgWaitingTxPo.class);
    }

    @Override
    public void deleteByTxHash(String nerveTxHash) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + nerveTxHash));
        StringListPo setPo = ConverterDBUtil.getModel(baseArea(), WAITING_TX_ALL_KEY(), StringListPo.class);
        if (setPo != null) {
            setPo.getCollection().remove(nerveTxHash);
            ConverterDBUtil.putModel(baseArea(), WAITING_TX_ALL_KEY(), setPo);
        }
    }

    @Override
    public boolean existNerveTxHash(String nerveTxHash) {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX_ETH_PO() + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<HtgWaitingTxPo> findAllWaitingTxPo() {
        StringListPo setPo = ConverterDBUtil.getModel(baseArea(), WAITING_TX_ALL_KEY(), StringListPo.class);
        if (setPo == null) {
            return null;
        }
        List<String> list = setPo.getCollection();
        List<HtgWaitingTxPo> resultList = new ArrayList<>();
        for (String txHash : list) {
            resultList.add(this.findEthWaitingTxPo(txHash));
        }
        return resultList;
    }

    @Override
    public int saveSentEthTx(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX_NERVE_SENT() + nerveTxHash), HtgConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean ifSentEthTx(String nerveTxHash) throws Exception {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX_NERVE_SENT() + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteSentEthTx(String nerveTxHash) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX_NERVE_SENT() + nerveTxHash));
    }

    @Override
    public int saveCompletedNerveTx(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX_NERVE_COMPLETED() + nerveTxHash), HtgConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean ifCompletedNerveTx(String nerveTxHash) throws Exception {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX_NERVE_COMPLETED() + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public int saveLastNerveTxHashWithTron(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea(), LAST_NERVE_TXHASH_WITH_TRON_KEY, nerveTxHash.getBytes(StandardCharsets.UTF_8));
        return 0;
    }

    @Override
    public String getLastNerveTxHashWithTron() throws Exception {
        byte[] bytes = RocksDBService.get(baseArea(), LAST_NERVE_TXHASH_WITH_TRON_KEY);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
