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
package network.nerve.converter.storage.impl;


import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.po.IntegerSetPo;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;


/**
 * @desription:
 * @author: Mimi
 * @date: 2018/5/24
 */
@Component
public class HeterogeneousChainInfoStorageServiceImpl implements HeterogeneousChainInfoStorageService {

    private final String baseArea = ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO;
    private final String KEY_PREFIX = "HETEROGENEOUS_CHAIN_INFO-";
    private final String CHAIN_MERGE_DB_PREFIX = "CHAIN_MERGE_DB-";
    private final String CHAIN_CLOSE_DB_PREFIX = "CHAIN_CLOSE_DB-";
    private final byte[] ALL_KEY = stringToBytes("HETEROGENEOUS_CHAIN_INFO-ALL");
    private final byte[] HAD_INIT_2_LEDGER_ASSET_KEY = stringToBytes("HETEROGENEOUS-HAD_INIT_2_LEDGER_ASSET");
    /*private final String WITHDRAWL_UTXO_PREFIX = "WITHDRAWL_UTXO_PREFIX-";
    private final String WITHDRAWL_UTXO_REBUILD_PREFIX = "WITHDRAWL_UTXO_REBUILD_PREFIX-";
    private final String WITHDRAWL_UTXO_LOCKED_PREFIX = "WITHDRAWL_UTXO_LOCKED_PREFIX-";*/
    //private final String CHAIN_WITHDRAWL_FEE_PREFIX = "CHAIN_WITHDRAWL_FEE_PREFIX-";

    @Override
    public int saveHeterogeneousChainInfo(int heterogeneousChainId, HeterogeneousChainInfo info) throws Exception {
        if (info == null) {
            return 0;
        }
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId), info);
        if(result) {
            IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
            if(setPo == null) {
                setPo = new IntegerSetPo();
                Set<Integer> set = new HashSet<>();
                set.add(heterogeneousChainId);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
            } else {
                Set<Integer> set = setPo.getCollection();
                if(!set.contains(heterogeneousChainId)) {
                    set.add(heterogeneousChainId);
                    result = ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public HeterogeneousChainInfo getHeterogeneousChainInfo(int heterogeneousChainId) {
        HeterogeneousChainInfo info = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId), HeterogeneousChainInfo.class);
        return info;
    }

    @Override
    public void deleteHeterogeneousChainInfo(int heterogeneousChainId) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId));
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
        setPo.getCollection().remove(heterogeneousChainId);
        ConverterDBUtil.putModel(baseArea, ALL_KEY, setPo);
    }

    @Override
    public boolean isExistHeterogeneousChainInfo(int heterogeneousChainId) {
        byte[] contract = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX + heterogeneousChainId));
        if (contract == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<HeterogeneousChainInfo> getAllHeterogeneousChainInfoList() {
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, ALL_KEY, IntegerSetPo.class);
        if (setPo == null) {
            return null;
        }
        Set<Integer> set = setPo.getCollection();
        List<HeterogeneousChainInfo> resultList = new ArrayList<>();
        for (Integer id : set) {
            resultList.add(this.getHeterogeneousChainInfo(id));
        }
        return resultList;
    }

    @Override
    public boolean hadInit2LedgerAsset() {
        byte[] bytes = RocksDBService.get(baseArea, HAD_INIT_2_LEDGER_ASSET_KEY);
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void init2LedgerAssetCompleted() throws Exception {
        RocksDBService.put(baseArea, HAD_INIT_2_LEDGER_ASSET_KEY, EthConstant.EMPTY_BYTE);
    }

    @Override
    public boolean hadDBMerged(int hChainId) {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(CHAIN_MERGE_DB_PREFIX + hChainId));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void markMergedChainDB(int hChainId) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(CHAIN_MERGE_DB_PREFIX + hChainId), EthConstant.EMPTY_BYTE);
    }

    @Override
    public void markChainClosed(int hChainId) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(CHAIN_CLOSE_DB_PREFIX + hChainId), EthConstant.EMPTY_BYTE);
    }

    @Override
    public boolean hadClosed(int hChainId) {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(CHAIN_CLOSE_DB_PREFIX + hChainId));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    //@Override
    //public void increaseChainWithdrawalFee(int htgChainId, BigInteger value) throws Exception {
    //    BigInteger fee = this.getChainWithdrawalFee(htgChainId);
    //    RocksDBService.put(baseArea, stringToBytes(CHAIN_WITHDRAWL_FEE_PREFIX + htgChainId), fee.add(value).toByteArray());
    //}
    //
    //@Override
    //public void decreaseChainWithdrawalFee(int htgChainId, BigInteger value) throws Exception {
    //    BigInteger fee = this.getChainWithdrawalFee(htgChainId);
    //    RocksDBService.put(baseArea, stringToBytes(CHAIN_WITHDRAWL_FEE_PREFIX + htgChainId), fee.subtract(value).toByteArray());
    //}
    //
    //@Override
    //public BigInteger getChainWithdrawalFee(int htgChainId) {
    //    byte[] bytes = RocksDBService.get(baseArea, stringToBytes(CHAIN_WITHDRAWL_FEE_PREFIX + htgChainId));
    //    if (bytes == null) {
    //        return BigInteger.ZERO;
    //    }
    //    return new BigInteger(bytes);
    //}

    /*@Override
    public void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception {
        ConverterDBUtil.putModel(baseArea, stringToBytes(WITHDRAWL_UTXO_PREFIX + txData.getNerveTxHash()), txData);
        List<UTXOData> utxoDataList = txData.getUtxoDataList();
        for (UTXOData utxo : utxoDataList) {
            RocksDBService.put(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + utxo.getTxid() + "-" + utxo.getVout()), HexUtil.decode(txData.getNerveTxHash()));
        }
    }

    @Override
    public boolean isLockedUTXO(String txid, int vout) {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + txid + "-" + vout));
        return bytes != null;
    }

    @Override
    public String getNerveHashByLockedUTXO(String txid, int vout) {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + txid + "-" + vout));
        if (bytes == null) {
            return null;
        }
        return HexUtil.encode(bytes);
    }

    @Override
    public WithdrawalUTXOTxData checkLockedUTXO(String nerveTxHash, List<UsedUTXOData> usedUTXOs) throws Exception {
        WithdrawalUTXOTxData withdrawalUTXOTxData = this.takeWithdrawalUTXOs(nerveTxHash);
        if (withdrawalUTXOTxData == null) {
            return null;
        }
        Set<String> usedSet = new HashSet<>();
        for (UsedUTXOData used : usedUTXOs) {
            String key = used.getTxid() + "-" + used.getVout();
            usedSet.add(key);
            RocksDBService.put(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key), HexUtil.decode(nerveTxHash));
        }
        byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
        List<UTXOData> utxoDataList = withdrawalUTXOTxData.getUtxoDataList();
        for (UTXOData utxoData : utxoDataList) {
            String key = utxoData.getTxid() + "-" + utxoData.getVout();
            byte[] bytes = RocksDBService.get(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key));
            if (bytes == null) {
                continue;
            }
            // unclock unused UTXO
            if (Arrays.equals(bytes, nerveTxHashBytes) && !usedSet.contains(key)) {
                RocksDBService.delete(baseArea, stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key));
            }
        }
        return withdrawalUTXOTxData;
    }

    @Override
    public WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash) {
        WithdrawalUTXOTxData model = ConverterDBUtil.getModel(baseArea, stringToBytes(WITHDRAWL_UTXO_PREFIX + nerveTxhash), WithdrawalUTXOTxData.class);
        return model;
    }

    @Override
    public void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception {
        RocksDBService.put(baseArea, stringToBytes(WITHDRAWL_UTXO_REBUILD_PREFIX + nerveTxHash), po.serialize());
    }

    @Override
    public WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(WITHDRAWL_UTXO_REBUILD_PREFIX + nerveTxHash));
        if (bytes == null) {
            return null;
        }
        WithdrawalUTXORebuildPO po = new WithdrawalUTXORebuildPO();
        po.parse(bytes, 0);
        return po;
    }*/
}
