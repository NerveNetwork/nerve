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

/**
 * @author: Loki
 * @date: 2020/6/9
 */
public class VoteReq extends BaseReq {

    /**
     * proposalhash
     */
    private String proposalTxHash;


    private byte choice;

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

    public VoteReq(String proposalTxHash, byte choice, String address) {
        this.proposalTxHash = proposalTxHash;
        this.choice = choice;
        this.address = address;
    }

    public String getProposalTxHash() {
        return proposalTxHash;
    }

    public void setProposalTxHash(String proposalTxHash) {
        this.proposalTxHash = proposalTxHash;
    }

    public byte getChoice() {
        return choice;
    }

    public void setChoice(byte choice) {
        this.choice = choice;
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
}
