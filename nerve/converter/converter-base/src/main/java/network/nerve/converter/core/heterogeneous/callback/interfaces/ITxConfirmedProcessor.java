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
package network.nerve.converter.core.heterogeneous.callback.interfaces;

import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.model.bo.HeterogeneousAddress;

import java.util.List;

/**
 * After sending out heterogeneous on chain transactions, heterogeneous chain components listen to whether the transaction is confirmed to be packaged. After packaging
 *
 * @author: Mimi
 * @date: 2020-02-17
 */
public interface ITxConfirmedProcessor {
    /**
     * @param txType           Transaction type - WITHDRAW/CHANGE/UPGRADE Withdrawal/Administrator Change/upgrade
     * @param nerveTxHash      This chain transactionhash
     * @param txHash           Heterogeneous Chain Tradinghash
     * @param blockHeight      Heterogeneous chain transaction confirmation height
     * @param txTime           Heterogeneous chain transaction time
     * @param multiSignAddress Current multiple signed addresses
     * @param signers          Transaction signature address list
     */
    void txConfirmed(HeterogeneousChainTxType txType, String nerveTxHash,
                     String txHash, Long blockHeight, Long txTime, String multiSignAddress, List<HeterogeneousAddress> signers, byte[] remark) throws Exception;

    void txRecordWithdrawFee(HeterogeneousChainTxType txType, String txHash,
                     String blockHash, Long blockHeight, Long txTime, long fee, byte[] remark) throws Exception;

    /**
     * @param nerveTxHash         This chain transactionhash
     * @param heterogeneousTxHash Heterogeneous Chain Tradinghash
     */
    void pendingTxOfWithdraw(String nerveTxHash, String heterogeneousTxHash) throws Exception;
}
