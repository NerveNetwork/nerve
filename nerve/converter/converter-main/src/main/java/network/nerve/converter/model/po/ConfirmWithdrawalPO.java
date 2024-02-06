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

package network.nerve.converter.model.po;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;

import java.io.Serializable;
import java.util.List;

/**
 * NERVETransactions that confirm the status of withdrawal transactions in the network Confirmed business data
 * @author: Loki
 * @date: 2020-03-06
 */
public class ConfirmWithdrawalPO implements Serializable {

    private int heterogeneousChainId;
    /**
     * Confirmation height of corresponding withdrawal transactions in heterogeneous chains
     */
    private long heterogeneousHeight;

    /**
     * Corresponding withdrawal transactions in heterogeneous chainshash
     */
    private String heterogeneousTxHash;

    /**
     * NERVECorresponding withdrawal transactions in the networkhash
     */
    private NulsHash withdrawalTxHash;

    /**
     * NERVETransactions with corresponding confirmed withdrawal transaction status in the networkhash
     */
    private NulsHash confirmWithdrawalTxHash;

    /**
     * Node heterogeneous chain addresses that require distribution of withdrawal fees
     */
    private List<HeterogeneousAddress> listDistributionFee;

    public ConfirmWithdrawalPO() {
    }

    public ConfirmWithdrawalPO(ConfirmWithdrawalTxData txData, NulsHash confirmWithdrawalTxHash) {
        this.heterogeneousChainId = txData.getHeterogeneousChainId();
        this.heterogeneousHeight = txData.getHeterogeneousHeight();
        this.heterogeneousTxHash = txData.getHeterogeneousTxHash();
        this.withdrawalTxHash = txData.getWithdrawalTxHash();
        this.listDistributionFee = txData.getListDistributionFee();
        this.confirmWithdrawalTxHash = confirmWithdrawalTxHash;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public NulsHash getConfirmWithdrawalTxHash() {
        return confirmWithdrawalTxHash;
    }

    public void setConfirmWithdrawalTxHash(NulsHash confirmWithdrawalTxHash) {
        this.confirmWithdrawalTxHash = confirmWithdrawalTxHash;
    }

    public long getHeterogeneousHeight() {
        return heterogeneousHeight;
    }

    public void setHeterogeneousHeight(long heterogeneousHeight) {
        this.heterogeneousHeight = heterogeneousHeight;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public NulsHash getWithdrawalTxHash() {
        return withdrawalTxHash;
    }

    public void setWithdrawalTxHash(NulsHash withdrawalTxHash) {
        this.withdrawalTxHash = withdrawalTxHash;
    }

    public List<HeterogeneousAddress> getListDistributionFee() {
        return listDistributionFee;
    }

    public void setListDistributionFee(List<HeterogeneousAddress> listDistributionFee) {
        this.listDistributionFee = listDistributionFee;
    }
}
