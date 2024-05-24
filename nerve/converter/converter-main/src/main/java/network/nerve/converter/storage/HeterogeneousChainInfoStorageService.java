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
package network.nerve.converter.storage;

import network.nerve.converter.model.bo.HeterogeneousChainInfo;

import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-02-18
 */
public interface HeterogeneousChainInfoStorageService {
    /**
     * Save basic information of heterogeneous chains
     */
    int saveHeterogeneousChainInfo(int heterogeneousChainId, HeterogeneousChainInfo info) throws Exception;

    /**
     * Obtain basic information of heterogeneous chains
     */
    HeterogeneousChainInfo getHeterogeneousChainInfo(int heterogeneousChainId);

    /**
     * Delete basic information of heterogeneous chains
     */
    void deleteHeterogeneousChainInfo(int heterogeneousChainId) throws Exception;

    /**
     * Based on heterogeneous chainschainIdCheck if there is basic information about this heterogeneous chain
     */
    boolean isExistHeterogeneousChainInfo(int heterogeneousChainId);

    /**
     * Obtain basic information of all heterogeneous chains
     *
     * @return
     */
    List<HeterogeneousChainInfo> getAllHeterogeneousChainInfoList();

    /**
     * Query whether the initialization of heterogeneous chain assets to the ledger has been completed
     */
    boolean hadInit2LedgerAsset();

    /**
     * Complete the initialization of heterogeneous chain assets to the ledger
     * @throws Exception
     */
    void init2LedgerAssetCompleted() throws Exception;

    /**
     * Has the heterogeneous chain been completedDBmerge
     */
    boolean hadDBMerged(int hChainId);

    /**
     * Marking heterogeneous chains completedDBmerge
     */
    void markMergedChainDB(int hChainId) throws Exception;

    void markChainClosed(int hChainId) throws Exception;

    boolean hadClosed(int hChainId);

    // Add an independent account ledger to record the income and expenditure of bitSys'chain fees
    //void increaseChainWithdrawalFee(int htgChainId, BigInteger value) throws Exception;
    //void decreaseChainWithdrawalFee(int htgChainId, BigInteger value) throws Exception;
    //BigInteger getChainWithdrawalFee(int htgChainId);

    /*void saveWithdrawalUTXOs(WithdrawalUTXOTxData txData) throws Exception;

    boolean isLockedUTXO(String txid, int vout);

    String getNerveHashByLockedUTXO(String txid, int vout);

    WithdrawalUTXOTxData checkLockedUTXO(String nerveTxHash, List<UsedUTXOData> usedUTXOs) throws Exception;

    WithdrawalUTXOTxData takeWithdrawalUTXOs(String nerveTxhash);

    void saveWithdrawalUTXORebuildPO(String nerveTxHash, WithdrawalUTXORebuildPO po) throws Exception;
    WithdrawalUTXORebuildPO getWithdrawalUTXORebuildPO(String nerveTxHash) throws Exception;*/

}
