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

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.btc.txdata.*;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgMultiSignAddressHistoryStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.po.StringSetPo;
import network.nerve.converter.utils.ConverterDBUtil;
import org.rocksdb.RocksDBException;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Mimi
 * @date: 2020-03-17
 */
public class HtgMultiSignAddressHistoryStorageServiceImpl implements HtgMultiSignAddressHistoryStorageService {

    private String baseArea;
    private final String KEY_PREFIX = "MSADDRESS-";
    private final String PUB_KEY_PREFIX = "MSADDRESS_PUB-";
    private final String VERSION_KEY_PREFIX = "VMSADDRESS";
    private final byte[] ALL_KEY = stringToBytes("MSADDRESS-ALL");
    private final String MERGE_KEY_PREFIX;
    private final String MERGE_AVAILABLE_PUB_KEY_PREFIX;
    private final String MERGE_PUB_KEY_PREFIX;
    private final String MERGE_VERSION_KEY_PREFIX;
    private final String MERGE_CHAIN_WITHDRAWAL_FEE;
    private final String MERGE_CHAIN_WITHDRAWAL_FEE_LOG_PREFIX;
    private final byte[] MERGE_ALL_KEY;
    private final String WITHDRAWL_UTXO_PREFIX;
    private final String WITHDRAWL_UTXO_REBUILD_PREFIX;
    private final String WITHDRAWL_UTXO_LOCKED_PREFIX;
    private final String FTDATA_PREFIX;
    private final byte[] SPLIT_GRANULARITY;

    private final HtgContext htgContext;
    public HtgMultiSignAddressHistoryStorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
        int htgChainId = htgContext.HTG_CHAIN_ID();
        this.MERGE_KEY_PREFIX = htgChainId + "_MSADDRESS-";
        this.MERGE_PUB_KEY_PREFIX = htgChainId + "_MSADDRESS_PUB-";
        this.MERGE_AVAILABLE_PUB_KEY_PREFIX = htgChainId + "_AVLB_MSADDRESS_PUB-";
        this.MERGE_VERSION_KEY_PREFIX = htgChainId + "_VMSADDRESS";
        this.MERGE_CHAIN_WITHDRAWAL_FEE = htgChainId + "_CHAIN_WITHDRAWAL_FEE";
        this.MERGE_CHAIN_WITHDRAWAL_FEE_LOG_PREFIX = htgChainId + "_CHAIN_WITHDRAWAL_FEE_LOG-";
        this.MERGE_ALL_KEY = stringToBytes(htgChainId + "_MSADDRESS-ALL");
        this.WITHDRAWL_UTXO_PREFIX = htgChainId + "_WITHDRAWL_UTXO_PREFIX-";
        this.WITHDRAWL_UTXO_REBUILD_PREFIX = htgChainId + "_WITHDRAWL_UTXO_REBUILD_PREFIX-";
        this.WITHDRAWL_UTXO_LOCKED_PREFIX = htgChainId + "_WITHDRAWL_UTXO_LOCKED_PREFIX-";
        this.SPLIT_GRANULARITY = stringToBytes(htgChainId + "_SPLIT_GRANULARITY");
        this.FTDATA_PREFIX = htgChainId + "_FTDATA_PREFIX-";
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
    private String KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_KEY_PREFIX;
        } else {
            return KEY_PREFIX;
        }
    }

    private String PUB_KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_PUB_KEY_PREFIX;
        } else {
            return PUB_KEY_PREFIX;
        }
    }
    private String VERSION_KEY_PREFIX() {
        checkMerged();
        if (merged) {
            return MERGE_VERSION_KEY_PREFIX;
        } else {
            return VERSION_KEY_PREFIX;
        }
    }
    private byte[] ALL_KEY() {
        checkMerged();
        if (merged) {
            return MERGE_ALL_KEY;
        } else {
            return ALL_KEY;
        }
    }

    public void saveVersion(byte version) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(VERSION_KEY_PREFIX()), new byte[]{version});
    }

    public byte getVersion() {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(VERSION_KEY_PREFIX()));
        if (bytes == null || bytes.length == 0) {
            return 0;
        } else {
            return bytes[0];
        }
    }

    private String baseArea() {
        checkMerged();
        return this.baseArea;
    }

    @Override
    public int save(String address) throws Exception {
        if (StringUtils.isBlank(address)) {
            return 0;
        }
        boolean result = RocksDBService.put(baseArea(), stringToBytes(KEY_PREFIX() + address), HtgConstant.EMPTY_BYTE);
        if (result) {
            StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
            if(setPo == null) {
                setPo = new StringSetPo();
                Set<String> set = new HashSet<>();
                set.add(address);
                setPo.setCollection(set);
                result = ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
            } else {
                Set<String> set = setPo.getCollection();
                if(!set.contains(address)) {
                    set.add(address);
                    result = ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public boolean isExist(String address) {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(KEY_PREFIX() + address));
        if(bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteByAddress(String address) throws Exception {
        RocksDBService.delete(baseArea(), stringToBytes(KEY_PREFIX() + address));
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
        if(setPo != null) {
            setPo.getCollection().remove(address);
            ConverterDBUtil.putModel(baseArea(), ALL_KEY(), setPo);
        }
    }

    @Override
    public Set<String> findAll() {
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea(), ALL_KEY(), StringSetPo.class);
        if (setPo == null) {
            return null;
        }
        Set<String> set = setPo.getCollection();
        return set;
    }


    @Override
    public boolean hasMultiSignAddressPubs(String address) {
        if (htgContext.HTG_CHAIN_ID() < 200) {
            return false;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(PUB_KEY_PREFIX() + address));
        return bytes != null;
    }

    @Override
    public List<String> getMultiSignAddressPubs(String address) {
        if (htgContext.HTG_CHAIN_ID() < 200) {
            return Collections.EMPTY_LIST;
        }
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(PUB_KEY_PREFIX() + address));
        if (bytes != null) {
            String pubsHex = HexUtil.encode(bytes);
            List<String> pubs = new ArrayList<>();
            int length = pubsHex.length();
            int i = 0;
            while (length > 0) {
                pubs.add(pubsHex.substring(0 + (i * 66), 66 + (i * 66)));
                i++;
                length -= 66;
            }
            return pubs;
        }
        return null;
    }

    @Override
    public void saveMultiSignAddressPubs(String address, String[] pubs) throws Exception {
        if (htgContext.HTG_CHAIN_ID() < 200) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String pub : pubs) {
            sb.append(pub.trim());
        }
        RocksDBService.put(baseArea(), stringToBytes(PUB_KEY_PREFIX() + address), HexUtil.decode(sb.toString()));
    }

    @Override
    public void saveChainWithdrawalFee(WithdrawalFeeLog feeLog) throws Exception {
        feeLog.setHtgChainId(htgContext.HTG_CHAIN_ID());
        if (feeLog.isRecharge()) {
            this.addChainWithdrawalFee(feeLog);
        } else {
            this.subtractChainWithdrawalFee(feeLog);
        }
    }

    private void addChainWithdrawalFee(WithdrawalFeeLog feeLog) throws Exception {
        BigInteger amount = BigInteger.valueOf(feeLog.getFee());
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new RuntimeException("error amount");
        }
        BigInteger fee = this.getChainWithdrawalFee();
        RocksDBService.put(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE), fee.add(amount).toByteArray());
        RocksDBService.put(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE_LOG_PREFIX + feeLog.getHtgTxHash()), feeLog.serialize());
    }

    private void subtractChainWithdrawalFee(WithdrawalFeeLog feeLog) throws Exception {
        BigInteger amount = BigInteger.valueOf(feeLog.getFee());
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new RuntimeException("error amount");
        }
        BigInteger fee = this.getChainWithdrawalFee();
        RocksDBService.put(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE), fee.subtract(amount).toByteArray());
        RocksDBService.put(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE_LOG_PREFIX + feeLog.getHtgTxHash()), feeLog.serialize());
    }

    @Override
    public BigInteger getChainWithdrawalFee() {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE));
        if (bytes == null) {
            return BigInteger.ZERO;
        }
        return new BigInteger(bytes);
    }

    @Override
    public WithdrawalFeeLog queryChainWithdrawalFeeLog(String htgTxHash) throws NulsException {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(MERGE_CHAIN_WITHDRAWAL_FEE_LOG_PREFIX + htgTxHash));
        if (bytes == null) {
            return null;
        }
        WithdrawalFeeLog feeLog = new WithdrawalFeeLog();
        feeLog.parse(bytes, 0);
        return feeLog;
    }

    @Override
    public void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception {
        ConverterDBUtil.putModel(baseArea(), stringToBytes(WITHDRAWL_UTXO_PREFIX + txData.getNerveTxHash()), txData);
        List<UTXOData> utxoDataList = txData.getUtxoDataList();
        if (!HtgUtil.isEmptyList(utxoDataList)) {
            for (UTXOData utxo : utxoDataList) {
                RocksDBService.put(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + utxo.getTxid() + "-" + utxo.getVout()), HexUtil.decode(txData.getNerveTxHash()));
            }
        }
        List<FtUTXOData> ftUtxoDataList = txData.getFtUtxoDataList();
        if (!HtgUtil.isEmptyList(ftUtxoDataList)) {
            for (FtUTXOData utxo : ftUtxoDataList) {
                RocksDBService.put(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + utxo.getTxId() + "-" + utxo.getOutputIndex()), HexUtil.decode(txData.getNerveTxHash()));
            }
        }
    }

    @Override
    public boolean isLockedUTXO(String txid, int vout) {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + txid + "-" + vout));
        return bytes != null;
    }

    @Override
    public String getNerveHashByLockedUTXO(String txid, int vout) {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + txid + "-" + vout));
        if (bytes == null) {
            return null;
        }
        return HexUtil.encode(bytes);
    }

    @Override
    public List<byte[]> getNerveHashListByLockedUTXO(List<UTXOData> utxoList) throws Exception {
        return RocksDBManager.getTable(baseArea()).multiGetAsList(utxoList.stream().map(u -> stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + u.getTxid() + "-" + u.getVout())).collect(Collectors.toList()));
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
            RocksDBService.put(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key), HexUtil.decode(nerveTxHash));
        }
        byte[] nerveTxHashBytes = HexUtil.decode(nerveTxHash);
        List<UTXOData> utxoDataList = withdrawalUTXOTxData.getUtxoDataList();
        if (HtgUtil.isEmptyList(utxoDataList)) {
            return withdrawalUTXOTxData;
        }
        for (UTXOData utxoData : utxoDataList) {
            String key = utxoData.getTxid() + "-" + utxoData.getVout();
            byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key));
            if (bytes == null) {
                continue;
            }
            // unclock unused UTXO
            if (Arrays.equals(bytes, nerveTxHashBytes) && !usedSet.contains(key)) {
                RocksDBService.delete(baseArea(), stringToBytes(WITHDRAWL_UTXO_LOCKED_PREFIX + key));
            }
        }
        return withdrawalUTXOTxData;
    }

    @Override
    public WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash) {
        WithdrawalUTXOTxData model = ConverterDBUtil.getModel(baseArea(), stringToBytes(WITHDRAWL_UTXO_PREFIX + nerveTxhash), WithdrawalUTXOTxData.class);
        return model;
    }

    @Override
    public void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(WITHDRAWL_UTXO_REBUILD_PREFIX + nerveTxHash), po.serialize());
    }

    @Override
    public WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(WITHDRAWL_UTXO_REBUILD_PREFIX + nerveTxHash));
        if (bytes == null) {
            return null;
        }
        WithdrawalUTXORebuildPO po = new WithdrawalUTXORebuildPO();
        po.parse(bytes, 0);
        return po;
    }

    @Override
    public void saveSplitGranularity(long splitGranularity) throws Exception {
        RocksDBService.put(baseArea(), SPLIT_GRANULARITY, ByteUtils.longToBytes(splitGranularity));
    }

    @Override
    public long getCurrentSplitGranularity() {
        byte[] bytes = RocksDBService.get(baseArea(), SPLIT_GRANULARITY);
        if (bytes == null) {
            return 0;
        }
        return ByteUtils.byteToLong(bytes);
    }

    @Override
    public void saveFtData(FtData ftData) throws Exception {
        RocksDBService.put(baseArea(), stringToBytes(FTDATA_PREFIX + ftData.getContractTxid()), ftData.serialize());

    }

    @Override
    public FtData getFtData(String contractId) throws Exception {
        byte[] bytes = RocksDBService.get(baseArea(), stringToBytes(FTDATA_PREFIX + contractId));
        if (bytes == null) {
            return null;
        }
        FtData po = new FtData();
        po.parse(bytes, 0);
        return po;
    }
}
