/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.model.bo;

import nerve.network.converter.enums.HeterogeneousChainTxType;

/**
 * @author: Chino
 * @date: 2020-02-17
 */
public class HeterogeneousTransactionInfo extends HeterogeneousTransactionBaseInfo{
    /**
     * 交易类型: 充值、提现、变更
     */
    private HeterogeneousChainTxType txType;
    /**
     * 提现或管理员变更的nerve交易hash
     */
    private String nerveTxHash;
    /**
     * 管理员变更的参数
     */
    private String[] addAddresses;
    private String[] removeAddresses;
    private String[] currentAddresses;

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public String[] getAddAddresses() {
        return addAddresses;
    }

    public void setAddAddresses(String[] addAddresses) {
        this.addAddresses = addAddresses;
    }

    public String[] getRemoveAddresses() {
        return removeAddresses;
    }

    public void setRemoveAddresses(String[] removeAddresses) {
        this.removeAddresses = removeAddresses;
    }

    public String[] getCurrentAddresses() {
        return currentAddresses;
    }

    public void setCurrentAddresses(String[] currentAddresses) {
        this.currentAddresses = currentAddresses;
    }

    public HeterogeneousChainTxType getTxType() {
        return txType;
    }

    public void setTxType(HeterogeneousChainTxType txType) {
        this.txType = txType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"txType\":")
                .append('\"').append(txType).append('\"');
        sb.append(",\"nerveTxHash\":")
                .append('\"').append(nerveTxHash).append('\"');
        sb.append(",\"baseInfo\":")
                .append(super.toString());
        sb.append('}');
        return sb.toString();
    }

    public String superString() {
        return super.toString();
    }
}
