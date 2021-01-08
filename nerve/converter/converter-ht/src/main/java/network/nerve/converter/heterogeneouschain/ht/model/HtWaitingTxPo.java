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

package network.nerve.converter.heterogeneouschain.ht.model;

import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2020-08-26
 */
public class HtWaitingTxPo extends HeterogeneousTransactionInfo implements Serializable {

    private Long validateHeight;
    private Map<String, Integer> currentVirtualBanks;
    private int currentNodeSendOrder;
    private long waitingEndTime;
    private long maxWaitingEndTime;
    /**
     * 合约升级的参数
     */
    private String upgradeContract;
    /**
     * 拜占庭签名数据
     */
    private String signatures;


    public String getUpgradeContract() {
        return upgradeContract;
    }

    public void setUpgradeContract(String upgradeContract) {
        this.upgradeContract = upgradeContract;
    }

    public String getSignatures() {
        return signatures;
    }

    public void setSignatures(String signatures) {
        this.signatures = signatures;
    }

    public long getMaxWaitingEndTime() {
        return maxWaitingEndTime;
    }

    public void setMaxWaitingEndTime(long maxWaitingEndTime) {
        this.maxWaitingEndTime = maxWaitingEndTime;
    }

    public long getWaitingEndTime() {
        return waitingEndTime;
    }

    public void setWaitingEndTime(long waitingEndTime) {
        this.waitingEndTime = waitingEndTime;
    }

    public int getCurrentNodeSendOrder() {
        return currentNodeSendOrder;
    }

    public void setCurrentNodeSendOrder(int currentNodeSendOrder) {
        this.currentNodeSendOrder = currentNodeSendOrder;
    }

    public Map<String, Integer> getCurrentVirtualBanks() {
        return currentVirtualBanks;
    }

    public void setCurrentVirtualBanks(Map<String, Integer> currentVirtualBanks) {
        this.currentVirtualBanks = currentVirtualBanks;
    }

    public Long getValidateHeight() {
        return validateHeight;
    }

    public void setValidateHeight(Long validateHeight) {
        this.validateHeight = validateHeight;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"validateHeight\":")
                .append(validateHeight);
        sb.append(",\"currentVirtualBanks\":")
                .append('\"').append(currentVirtualBanks).append('\"');
        sb.append(",\"currentNodeSendOrder\":")
                .append(currentNodeSendOrder);
        sb.append(",\"waitingEndTime\":")
                .append(waitingEndTime);
        sb.append(",\"maxWaitingEndTime\":")
                .append(maxWaitingEndTime);
        sb.append(",\"upgradeContract\":")
                .append('\"').append(upgradeContract).append('\"');
        sb.append(",\"signatures\":")
                .append('\"').append(signatures).append('\"');
        sb.append(",\"baseInfo\":")
                .append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
