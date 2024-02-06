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
import io.nuls.core.constant.SyncStatusEnum;

import java.io.Serializable;

/**
 * Handling approved proposals
 * @author: Loki
 * @date: 2020/5/15
 */
public class ExeProposalPO implements Serializable {

    NulsHash proposalTxHash;

    private long time;

    private long height;
    /**
     * Verify the number of times the transaction has been confirmed during processing（Discard transactions that have reached the threshold but have not been confirmed）
     */
    private int isConfirmedVerifyCount;

    /**
     * Node block synchronization mode
     */
    private SyncStatusEnum syncStatusEnum;

    /**
     * The total number of virtual bank members in the current block
     * (Not including current joining, To calculate the current exit)
     */
    private int currenVirtualBankTotal;

    public NulsHash getProposalTxHash() {
        return proposalTxHash;
    }

    public void setProposalTxHash(NulsHash proposalTxHash) {
        this.proposalTxHash = proposalTxHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public int getIsConfirmedVerifyCount() {
        return isConfirmedVerifyCount;
    }

    public void setIsConfirmedVerifyCount(int isConfirmedVerifyCount) {
        this.isConfirmedVerifyCount = isConfirmedVerifyCount;
    }

    public SyncStatusEnum getSyncStatusEnum() {
        return syncStatusEnum;
    }

    public void setSyncStatusEnum(SyncStatusEnum syncStatusEnum) {
        this.syncStatusEnum = syncStatusEnum;
    }

    public int getCurrenVirtualBankTotal() {
        return currenVirtualBankTotal;
    }

    public void setCurrenVirtualBankTotal(int currenVirtualBankTotal) {
        this.currenVirtualBankTotal = currenVirtualBankTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExeProposalPO that = (ExeProposalPO) o;
        return proposalTxHash.equals(that.proposalTxHash);
    }

    @Override
    public int hashCode() {
        return proposalTxHash.hashCode();
    }
}
