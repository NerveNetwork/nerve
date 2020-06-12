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

package network.nerve.converter.model.po;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.txdata.ConfirmWithdrawalTxData;

import java.io.Serializable;
import java.util.List;

/**
 * NERVE网络中确认提现交易状态的交易 确认的业务数据
 * @author: Loki
 * @date: 2020-03-06
 */
public class ConfirmWithdrawalPO implements Serializable {

    private int heterogeneousChainId;
    /**
     * 异构链中对应的提现交易确认高度
     */
    private long heterogeneousHeight;

    /**
     * 异构链中对应的提现交易hash
     */
    private String heterogeneousTxHash;

    /**
     * NERVE网络中对应的提现交易hash
     */
    private NulsHash withdrawalTxHash;

    /**
     * NERVE网络中对应的确认提现交易状态的交易hash
     */
    private NulsHash confirmWithdrawalTxHash;

    /**
     * 需要分发提现手续费的节点异构链地址
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
