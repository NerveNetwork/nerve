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

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.SyncStatusEnum;
import network.nerve.converter.model.bo.VirtualBankDirector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Confirmed transactions awaiting further processing
 * Maybe it's Calling components[Withdrawal transactions、Bank change transaction（Create heterogeneous chain addresses, create(modify)Multiple signed addresses、）], Or generate new transactions
 * @author: Loki
 * @date: 2020-03-09
 */
public class TxSubsequentProcessPO implements Serializable {

    /**
     * Pending transactions
     */
    private Transaction tx;

    /**
     * Join virtual bank member list (Need to create heterogeneous addresses)
     */
    private List<VirtualBankDirector> listInDirector = new ArrayList<>();

    /**
     * Exit virtual bank member list
     */
    private List<VirtualBankDirector> listOutDirector = new ArrayList<>();

    /**
     * Verify the number of times the transaction has been confirmed during processing（Discard transactions that have reached the threshold but have not been confirmed）
     */
    private int isConfirmedVerifyCount;

    /**
     * The exchange is located in the block head
     */
    private BlockHeader blockHeader;

    /**
     * Node block synchronization mode
     */
    private SyncStatusEnum syncStatusEnum;

    /**
     * Is the current node a virtual bank member
     */
    private boolean currentDirector;

    /**
     * Is the current node a virtual bank added to this transaction
     */
    private boolean currentJoin;

    /**
     * Is the current node a virtual bank that has exited in this transaction
     */
    private boolean currentQuit;

    /**
     * If the current administrator exits the virtual bank for this transaction, Need to temporarily store data(Use when signing)
     */
    private VirtualBankDirector currentQuitDirector;

    /**
     * The total number of virtual bank members in the current block
     * (Not including current joining, To calculate the current exit)
     */
    private int currenVirtualBankTotal;

    /**
     * Retry mechanism(message replay)
     */
    private boolean retry;
    private transient boolean retryVirtualBankInit;// Change retry data initialization
    private transient int prepare;// 1 - Preparation phase,2 - Unprepared, execution phase
    private transient int withdrawErrorTimes;
    private transient int withdrawErrorTotalTimes;
    private transient int feeChangeVersion;
    private transient long timeForMakeUTXO;
    private transient int withdrawAbnormalTimes;

    public long getTimeForMakeUTXO() {
        return timeForMakeUTXO;
    }

    public void setTimeForMakeUTXO(long timeForMakeUTXO) {
        this.timeForMakeUTXO = timeForMakeUTXO;
    }

    public boolean isRetryVirtualBankInit() {
        return retryVirtualBankInit;
    }

    public void setRetryVirtualBankInit(boolean retryVirtualBankInit) {
        this.retryVirtualBankInit = retryVirtualBankInit;
    }

    public int getPrepare() {
        return prepare;
    }

    public void setPrepare(int prepare) {
        this.prepare = prepare;
    }

    public void increaseWithdrawErrorTime() {
        this.withdrawErrorTimes++;
        this.withdrawErrorTotalTimes++;
    }
    public void increaseWithdrawAbnormalTime() {
        this.withdrawAbnormalTimes++;
    }

    public int getWithdrawAbnormalTimes() {
        return withdrawAbnormalTimes;
    }

    public boolean isWithdrawExceedErrorTime(int currentFeeChangeVersion, int limit) {
        // When the transaction fee provided by the user changes, resume the withdrawal process from the suspended withdrawal
        if (currentFeeChangeVersion != feeChangeVersion) {
            feeChangeVersion = currentFeeChangeVersion;
            this.clearWithdrawErrorTime();
            return false;
        }
        int multiplier = (this.withdrawErrorTotalTimes + limit - 1) / limit;
        multiplier = Math.min(multiplier, 5);
        if (this.withdrawErrorTimes >= (limit * multiplier)) {
            if (multiplier == 1 && this.withdrawErrorTimes >= (limit * 2)) {
                this.withdrawErrorTimes = 0;
                return false;
            } else if (multiplier > 1) {
                this.withdrawErrorTimes = 0;
                return false;
            }
        }
        boolean error = this.withdrawErrorTimes >= limit;
        if (error) {
            this.withdrawErrorTimes++;
        }
        return error;
    }

    private void clearWithdrawErrorTime() {
        this.withdrawErrorTimes = 0;
        this.withdrawErrorTotalTimes = 0;
    }

    public TxSubsequentProcessPO() {
    }

    public TxSubsequentProcessPO(Transaction tx) {
        this.tx = tx;
    }

    public int getIsConfirmedVerifyCount() {
        return isConfirmedVerifyCount;
    }

    public void setIsConfirmedVerifyCount(int isConfirmedVerifyCount) {
        this.isConfirmedVerifyCount = isConfirmedVerifyCount;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public List<VirtualBankDirector> getListInDirector() {
        return listInDirector;
    }

    public void setListInDirector(List<VirtualBankDirector> listInDirector) {
        this.listInDirector = listInDirector;
    }

    public List<VirtualBankDirector> getListOutDirector() {
        return listOutDirector;
    }

    public void setListOutDirector(List<VirtualBankDirector> listOutDirector) {
        this.listOutDirector = listOutDirector;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    public SyncStatusEnum getSyncStatusEnum() {
        return syncStatusEnum;
    }

    public void setSyncStatusEnum(SyncStatusEnum syncStatusEnum) {
        this.syncStatusEnum = syncStatusEnum;
    }

    public boolean getCurrentJoin() {
        return currentJoin;
    }

    public void setCurrentJoin(boolean currentJoin) {
        this.currentJoin = currentJoin;
    }

    public boolean getCurrentDirector() {
        return currentDirector;
    }

    public void setCurrentDirector(boolean currentDirector) {
        this.currentDirector = currentDirector;
    }

    public boolean getCurrentQuit() {
        return currentQuit;
    }

    public void setCurrentQuit(boolean currentQuit) {
        this.currentQuit = currentQuit;
    }

    public VirtualBankDirector getCurrentQuitDirector() {
        return currentQuitDirector;
    }

    public void setCurrentQuitDirector(VirtualBankDirector currentQuitDirector) {
        this.currentQuitDirector = currentQuitDirector;
    }

    public int getCurrenVirtualBankTotal() {
        return currenVirtualBankTotal;
    }

    public void setCurrenVirtualBankTotal(int currenVirtualBankTotal) {
        this.currenVirtualBankTotal = currenVirtualBankTotal;
    }

    public boolean getRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
