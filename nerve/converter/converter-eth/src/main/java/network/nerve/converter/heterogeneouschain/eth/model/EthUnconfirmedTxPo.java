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

package network.nerve.converter.heterogeneouschain.eth.model;

import network.nerve.converter.heterogeneouschain.eth.enums.MultiSignatureStatus;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

import java.io.Serializable;

/**
 * @author: Mimi
 * @date: 2020-03-02
 */
public class EthUnconfirmedTxPo extends HeterogeneousTransactionInfo implements Serializable {

    /**
     * 提现或管理员变更的ETH交易多签状态
     */
    private MultiSignatureStatus status;
    private boolean validateTx;
    private boolean delete;
    private Long deletedHeight;
    private int blockHeightTimes;
    private int skipTimes;
    private transient int checkFailedTimes;
    private int resendTimes;
    private long createDate;
    private int dbVersion;
    private EthRecoveryDto recoveryDto;

    public EthUnconfirmedTxPo() {
        status = MultiSignatureStatus.INITIAL;
        createDate = System.currentTimeMillis();
        dbVersion = 0;
    }

    public boolean checkFailedTimeOut() {
        if (this.checkFailedTimes >= 3) {
            this.checkFailedTimes = 0;
            return true;
        }
        this.checkFailedTimes++;
        return false;
    }

    public EthRecoveryDto getRecoveryDto() {
        return recoveryDto;
    }

    public void setRecoveryDto(EthRecoveryDto recoveryDto) {
        this.recoveryDto = recoveryDto;
    }

    public int getBlockHeightTimes() {
        return blockHeightTimes;
    }

    public void setBlockHeightTimes(int blockHeightTimes) {
        this.blockHeightTimes = blockHeightTimes;
    }

    public void increaseBlockHeightTimes() {
        this.blockHeightTimes++;
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public int getResendTimes() {
        return resendTimes;
    }

    public void setResendTimes(int resendTimes) {
        this.resendTimes = resendTimes;
    }

    public boolean isValidateTx() {
        return validateTx;
    }

    public void setValidateTx(boolean validateTx) {
        this.validateTx = validateTx;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public int getSkipTimes() {
        return skipTimes;
    }

    public void setSkipTimes(int skipTimes) {
        this.skipTimes = skipTimes;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public Long getDeletedHeight() {
        return deletedHeight;
    }

    public void setDeletedHeight(Long deletedHeight) {
        this.deletedHeight = deletedHeight;
    }


    public MultiSignatureStatus getStatus() {
        return status;
    }

    public void setStatus(MultiSignatureStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"status\":")
                .append('\"').append(status).append('\"');
        sb.append(",\"ethTxHash\":")
                .append('\"').append(getTxHash()).append('\"');
        sb.append(",\"nerveTxHash\":")
                .append('\"').append(getNerveTxHash()).append('\"');
        sb.append(",\"validateTx\":")
                .append(validateTx);
        sb.append(",\"delete\":")
                .append(delete);
        sb.append(",\"deletedHeight\":")
                .append(deletedHeight);
        sb.append(",\"skipTimes\":")
                .append(skipTimes);
        sb.append(",\"createDate\":")
                .append(createDate);
        sb.append(",\"dbVersion\":")
                .append(dbVersion);
        sb.append(",\"recoveryDto\":")
                .append(recoveryDto);
        sb.append(",\"baseInfo\":")
                .append(super.toString());
        sb.append('}');
        return sb.toString();
    }

    public String superString() {
        return super.superString();
    }
}
