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

package network.nerve.converter.model.dto;

import java.math.BigInteger;

/**
 * Additional withdrawal handling fee parameter
 * @author: Loki
 * @date: 2020/9/27
 */
public class WithdrawalAdditionalFeeTxDTO {

    /**
     * Additional handling fees are requirednerveWithdrawal transactionshash
     */
    private String txHash;

    /**
     * To be added The chain of assetsID(5/9,101,102,103....)
     */
    private int feeChainId;
    /**
     * To be added Asset handling fees
     */
    private BigInteger amount;

    /**
     * Transaction notes
     */
    private String remark;

    /**
     * Withdrawal initiation(autograph)Address information
     */
    private SignAccountDTO signAccount;

    private boolean rebuild;
    private Integer htgChainId;

    public Integer getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(Integer htgChainId) {
        this.htgChainId = htgChainId;
    }

    public boolean isRebuild() {
        return rebuild;
    }

    public void setRebuild(boolean rebuild) {
        this.rebuild = rebuild;
    }

    public int getFeeChainId() {
        return feeChainId;
    }

    public void setFeeChainId(int feeChainId) {
        this.feeChainId = feeChainId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
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

    public SignAccountDTO getSignAccount() {
        return signAccount;
    }

    public void setSignAccount(SignAccountDTO signAccount) {
        this.signAccount = signAccount;
    }
}
