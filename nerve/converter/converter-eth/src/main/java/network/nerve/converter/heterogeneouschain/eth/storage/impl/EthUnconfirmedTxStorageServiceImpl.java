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
import network.nerve.converter.heterogeneouschain.eth.constant.EthDBConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.model.po.StringListPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class EthUnconfirmedTxStorageServiceImpl implements EthUnconfirmedTxStorageService {

    private final String baseArea = EthDBConstant.DB_ETH;
    private final String KEY_PREFIX = "UNCONFIRMED_TX-";
    private final byte[] UNCONFIRMED_TX_ALL_KEY = ConverterDBUtil.stringToBytes("UNCONFIRMED_TX-ALL");
    private Object version = new Object();
    private Object delete = new Object();

    @Override
    public int save(EthUnconfirmedTxPo po) throws Exception {
        if (po == null || StringUtils.isBlank(po.getTxHash())) {
            return 0;
        }
        String ethTxHash = po.getTxHash();
        if (EthContext.logger().isDebugEnabled()) {
            EthContext.logger().debug("保存未确认交易[{}], 详情: {}", ethTxHash, po.toString());
        }
        boolean result = ConverterDBUtil.putModel(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + ethTxHash), po);
        if (result) {
            StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
            if (setPo == null) {
                setPo = new StringListPo();
                List<String> list = new ArrayList<>();
                list.add(po.getTxHash());
                setPo.setCollection(list);
                result = ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
            } else {
                List<String> list = setPo.getCollection();
                Set<String> set = new HashSet<>(list);
                if (!set.contains(po.getTxHash())) {
                    list.add(po.getTxHash());
                    result = ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public int update(EthUnconfirmedTxPo po, Consumer<EthUnconfirmedTxPo> update) throws Exception {
        synchronized (version) {
            EthUnconfirmedTxPo current = this.findByTxHash(po.getTxHash());
            if (current == null) {
                return this.save(po);
            }
            if (current.getDbVersion() == po.getDbVersion()) {
                po.setDbVersion(po.getDbVersion() + 1);
                return this.save(po);
            } else {
                update.accept(current);
                current.setDbVersion(current.getDbVersion() + 1);
                return this.save(current);
            }
        }
    }

    @Override
    public EthUnconfirmedTxPo findByTxHash(String ethTxHash) {
        return ConverterDBUtil.getModel(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + ethTxHash), EthUnconfirmedTxPo.class);
    }

    @Override
    public void deleteByTxHash(String ethTxHash) throws Exception {
        synchronized (delete) {
            RocksDBService.delete(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + ethTxHash));
            StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
            if (setPo != null) {
                setPo.getCollection().remove(ethTxHash);
                ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
            }
        }
    }

    @Override
    public List<EthUnconfirmedTxPo> findAll() {
        StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
        if (setPo == null) {
            return null;
        }
        List<String> list = setPo.getCollection();
        List<EthUnconfirmedTxPo> resultList = new ArrayList<>();
        for (String txHash : list) {
            resultList.add(this.findByTxHash(txHash));
        }
        return resultList;
    }
}
