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

package network.nerve.converter.heterogeneouschain.lib.storage;

import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.txdata.*;
import org.rocksdb.RocksDBException;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-03-17
 */
public interface HtgMultiSignAddressHistoryStorageService {

    int save(String address) throws Exception;

    boolean isExist(String address);

    void deleteByAddress(String address) throws Exception;

    Set<String> findAll();

    void saveVersion(byte version) throws Exception;

    byte getVersion();

    boolean hasMultiSignAddressPubs(String address);

    List<String> getMultiSignAddressPubs(String address);

    void saveMultiSignAddressPubs(String address, String[] pubs) throws Exception;

    void saveChainWithdrawalFee(WithdrawalFeeLog feeLog) throws Exception;
    BigInteger getChainWithdrawalFee();
    WithdrawalFeeLog queryChainWithdrawalFeeLog(String htgTxHash) throws NulsException;

    void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception;

    boolean isLockedUTXO(String txid, int vout);

    String getNerveHashByLockedUTXO(String txid, int vout);
    List<byte[]> getNerveHashListByLockedUTXO(List<UTXOData> utxoList) throws Exception;

    WithdrawalUTXOTxData checkLockedUTXO(String nerveTxHash, List<UsedUTXOData> usedUTXOs) throws Exception;

    WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash);

    void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception;
    WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception;

    void saveSplitGranularity(long splitGranularity) throws Exception;
    long getCurrentSplitGranularity();
}
