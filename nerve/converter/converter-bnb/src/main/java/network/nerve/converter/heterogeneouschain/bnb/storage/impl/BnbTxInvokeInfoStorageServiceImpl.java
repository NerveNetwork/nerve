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
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbDBConstant;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbWaitingTxPo;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbTxInvokeInfoStorageService;
import network.nerve.converter.model.po.StringListPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-02-21
 */
@Component
public class BnbTxInvokeInfoStorageServiceImpl implements BnbTxInvokeInfoStorageService {

    private final String baseArea = BnbDBConstant.DB_BNB;
    private final String KEY_PREFIX_NERVE_COMPLETED = "TXINVOKE-NC-";
    private final String KEY_PREFIX_NERVE_SENT = "TXINVOKE-NS-";
    private final String KEY_PREFIX_ETH_PO = "TXINVOKE-P-";
    private final byte[] WAITING_TX_ALL_KEY = stringToBytes("TXINVOKE-P-ALL");

    @Override
    public int save(String nerveTxHash, BnbWaitingTxPo po) throws Exception {
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_ETH_PO + nerveTxHash), po);
        if (result) {
            StringListPo setPo = ConverterDBUtil.getModel(baseArea, WAITING_TX_ALL_KEY, StringListPo.class);
            if (setPo == null) {
                setPo = new StringListPo();
                List<String> list = new ArrayList<>();
                list.add(nerveTxHash);
                setPo.setCollection(list);
                result = ConverterDBUtil.putModel(baseArea, WAITING_TX_ALL_KEY, setPo);
            } else {
                List<String> list = setPo.getCollection();
                Set<String> set = new HashSet<>(list);
                if (!set.contains(nerveTxHash)) {
                    list.add(nerveTxHash);
                    result = ConverterDBUtil.putModel(baseArea, WAITING_TX_ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public BnbWaitingTxPo findEthWaitingTxPo(String nerveTxHash) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_ETH_PO + nerveTxHash), BnbWaitingTxPo.class);
    }

    @Override
    public void deleteByTxHash(String nerveTxHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_ETH_PO + nerveTxHash));
        StringListPo setPo = ConverterDBUtil.getModel(baseArea, WAITING_TX_ALL_KEY, StringListPo.class);
        if (setPo != null) {
            setPo.getCollection().remove(nerveTxHash);
            ConverterDBUtil.putModel(baseArea, WAITING_TX_ALL_KEY, setPo);
        }
    }

    @Override
    public boolean existNerveTxHash(String nerveTxHash) {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_ETH_PO + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<BnbWaitingTxPo> findAllWaitingTxPo() {
        StringListPo setPo = ConverterDBUtil.getModel(baseArea, WAITING_TX_ALL_KEY, StringListPo.class);
        if (setPo == null) {
            return null;
        }
        List<String> list = setPo.getCollection();
        List<BnbWaitingTxPo> resultList = new ArrayList<>();
        for (String txHash : list) {
            resultList.add(this.findEthWaitingTxPo(txHash));
        }
        return resultList;
    }

    @Override
    public int saveSentEthTx(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_NERVE_SENT + nerveTxHash), BnbConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean ifSentEthTx(String nerveTxHash) throws Exception {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_NERVE_SENT + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteSentEthTx(String nerveTxHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_NERVE_SENT + nerveTxHash));
    }

    @Override
    public int saveCompletedNerveTx(String nerveTxHash) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_NERVE_COMPLETED + nerveTxHash), BnbConstant.EMPTY_BYTE);
        return 0;
    }

    @Override
    public boolean ifCompletedNerveTx(String nerveTxHash) throws Exception {
        if (StringUtils.isBlank(nerveTxHash)) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_NERVE_COMPLETED + nerveTxHash));
        if (bytes == null) {
            return false;
        }
        return true;
    }
}
