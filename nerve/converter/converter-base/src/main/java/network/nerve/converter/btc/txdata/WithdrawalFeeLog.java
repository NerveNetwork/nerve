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
package network.nerve.converter.btc.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author: PierreLuo
 * @date: 2024/3/28
 */
public class WithdrawalFeeLog extends BaseNulsData {
    private long blockHeight;
    private String blockHash;
    private String htgTxHash;
    private int htgChainId;
    private long fee;
    private boolean recharge;
    private Boolean nerveInner;
    private transient long txTime;

    public WithdrawalFeeLog() {
    }

    public WithdrawalFeeLog(long blockHeight, String blockHash, String htgTxHash, int htgChainId, long fee, boolean recharge) {
        this.blockHeight = blockHeight;
        this.blockHash = blockHash;
        this.htgTxHash = htgTxHash;
        this.htgChainId = htgChainId;
        this.fee = fee;
        this.recharge = recharge;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(blockHeight);
        stream.writeString(blockHash);
        stream.writeString(htgTxHash);
        stream.writeUint16(htgChainId);
        stream.writeUint32(fee);
        stream.writeBoolean(recharge);
        if (nerveInner != null) {
            stream.writeBoolean(nerveInner);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.blockHeight = byteBuffer.readUint32();
        this.blockHash = byteBuffer.readString();
        this.htgTxHash = byteBuffer.readString();
        this.htgChainId = byteBuffer.readUint16();
        this.fee = byteBuffer.readUint32();
        this.recharge = byteBuffer.readBoolean();
        if (!byteBuffer.isFinished()) {
            this.nerveInner = byteBuffer.readBoolean();
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfString(this.blockHash);
        size += SerializeUtils.sizeOfString(this.htgTxHash);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfBoolean();
        if (nerveInner != null) {
            size += SerializeUtils.sizeOfBoolean();
        }
        return size;
    }

    public boolean isNerveInner() {
        return nerveInner == null ? false : nerveInner;
    }

    public void setNerveInner(Boolean nerveInner) {
        this.nerveInner = nerveInner;
    }

    public boolean isRecharge() {
        return recharge;
    }

    public void setRecharge(boolean recharge) {
        this.recharge = recharge;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getHtgTxHash() {
        return htgTxHash;
    }

    public void setHtgTxHash(String htgTxHash) {
        this.htgTxHash = htgTxHash;
    }

    public int getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(int htgChainId) {
        this.htgChainId = htgChainId;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getTxTime() {
        return txTime;
    }

    public void setTxTime(long txTime) {
        this.txTime = txTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"blockHeight\":")
                .append(blockHeight);

        sb.append(",\"blockHash\":")
                .append('\"').append(blockHash).append('\"');

        sb.append(",\"htgTxHash\":")
                .append('\"').append(htgTxHash).append('\"');

        sb.append(",\"htgChainId\":")
                .append(htgChainId);

        sb.append(",\"fee\":")
                .append(fee);

        sb.append(",\"recharge\":")
                .append(recharge);
        sb.append(",\"nerveInner\":")
                .append(nerveInner);

        sb.append('}');
        return sb.toString();
    }
}
