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
package network.nerve.converter.model.bo;

import network.nerve.converter.enums.HeterogeneousChainTxType;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: Mimi
 * @date: 2020-02-17
 */
public class HeterogeneousTransactionInfo extends HeterogeneousTransactionBaseInfo{
    /**
     * Transaction type: Recharge、Withdrawal、change
     */
    private HeterogeneousChainTxType txType;
    /**
     * Withdrawals or administrator changesnervetransactionhash
     */
    private String nerveTxHash;
    /**
     * Parameters changed by administrator
     */
    private String[] addAddresses;
    private String[] removeAddresses;
    private int orginTxCount;
    private transient int depositErrorTimes;

    // Due to the fact that the persistent structure of this object cannot be changed arbitrarily（Once changed, it takes a lot of time to test）Here, unused fields in the recharge type are utilized`addAddresses` As a`crossOutII`Fields for recharging main assets
    public void setDepositIIMainAsset(BigInteger value, Integer decimals, Integer assetId) {
        this.setAddAddresses(new String[]{value.toString(), decimals.toString(), assetId.toString()});
    }
    // Due to the fact that the persistent structure of this object cannot be changed arbitrarily（Once changed, it takes a lot of time to test）Here, unused fields in the recharge type are utilized`removeAddresses` As a`crossOutII`Extended fields in
    public void setDepositIIExtend(String extend) {
        this.setRemoveAddresses(new String[]{extend});
    }

    public String getDepositIIExtend() {
        return getTxType() == HeterogeneousChainTxType.DEPOSIT && getRemoveAddresses() != null && getRemoveAddresses().length > 0 ? getRemoveAddresses()[0] : null;
    }

    // trueRepresented simultaneous rechargetokenandmain asset
    public boolean isDepositIIMainAndToken() {
        return getTxType() == HeterogeneousChainTxType.DEPOSIT && getAddAddresses() != null && getAddAddresses().length == 3;
    }

    public BigInteger getDepositIIMainAssetValue() {
        return isDepositIIMainAndToken() ? new BigInteger(getAddAddresses()[0]) : null;
    }

    public Integer getDepositIIMainAssetDecimals() {
        return isDepositIIMainAndToken() ? Integer.parseInt(getAddAddresses()[1]) : null;
    }

    public Integer getDepositIIMainAssetAssetId() {
        return isDepositIIMainAndToken() ? Integer.parseInt(getAddAddresses()[2]) : null;
    }

    public void increaseDepositErrorTime() {
        this.depositErrorTimes++;
    }

    public boolean isDepositExceedErrorTime(int limit) {
        return this.depositErrorTimes >= limit;
    }

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

    public int getOrginTxCount() {
        return orginTxCount;
    }

    public void setOrginTxCount(int orginTxCount) {
        this.orginTxCount = orginTxCount;
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
        sb.append(",\"addAddresses\":")
                .append(Arrays.toString(addAddresses));
        sb.append(",\"removeAddresses\":")
                .append(Arrays.toString(removeAddresses));
        sb.append(",\"orginTxCount\":")
                .append(orginTxCount);
        sb.append(",\"baseInfo\":")
                .append(super.toString());
        sb.append('}');
        return sb.toString();
    }

    public String superString() {
        return super.toString();
    }
}
