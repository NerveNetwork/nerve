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

package nerve.network.converter.model.dto;

import java.math.BigInteger;

/**
 * @author: Chino
 * @date: 2020-03-02
 */
public class WithdrawalTxDTO {
    /**
     * 提现异构链id
     */
    int heterogeneousChainId;
    /**
     * 提现资产id
     */
    int heterogeneousAssetId;
    /**
     * 提现异构链地址
     */
    String heterogeneousAddress;
    /**
     * 提现金额
     */
    BigInteger amount;
    /**
     * 交易备注
     */
    String remark;

    /**
     * 提现发起(签名)地址信息
     */
    SignAccountDTO signAccount;



    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public int getHeterogeneousAssetId() {
        return heterogeneousAssetId;
    }

    public void setHeterogeneousAssetId(int heterogeneousAssetId) {
        this.heterogeneousAssetId = heterogeneousAssetId;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public SignAccountDTO getSignAccount() {
        return signAccount;
    }

    public void setSignAccount(SignAccountDTO signAccount) {
        this.signAccount = signAccount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
