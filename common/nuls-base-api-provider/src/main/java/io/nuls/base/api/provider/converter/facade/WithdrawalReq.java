/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

package io.nuls.base.api.provider.converter.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

/**
 * Heterogeneous chain withdrawal
 * @author: Charlie
 * @date: 2020/4/28
 */
public class WithdrawalReq extends BaseReq {

    /**
     * Withdrawal of heterogeneous chainsid
     */
    private int heterogeneousChainId;

    private int assetChainId;

    private int assetId;

    /**
     * Withdrawal to heterogeneous account addresses
     */
    private String heterogeneousAddress;

    /**
     * Custom withdrawal handling fee
     */
    private BigInteger distributionFee;

    /**
     * Withdrawal amount
     */
    private BigInteger amount;


    /**
     * Withdrawal remarks
     */
    private String remark;

    /**
     * Withdrawal and redemption address(Transfer out)
     */
    private String address;

    /**
     * Redemption address password
     */
    private String password;

    public WithdrawalReq(int assetChainId, int assetId, int heterogeneousChainId, String heterogeneousAddress, BigInteger distributionFee, BigInteger amount, String address) {
        this.assetChainId = assetChainId;
        this.assetId = assetId;
        this.heterogeneousChainId = heterogeneousChainId;
        this.heterogeneousAddress = heterogeneousAddress;
        this.distributionFee = distributionFee;
        this.amount = amount;
        this.address = address;
    }

    public WithdrawalReq() {
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public BigInteger getDistributionFee() {
        return distributionFee;
    }

    public void setDistributionFee(BigInteger distributionFee) {
        this.distributionFee = distributionFee;
    }
}
