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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousHash;
import org.web3j.utils.Numeric;

import java.io.IOException;

/**
 * One click cross chain
 * @author: PierreLuo
 * @date: 2022/3/21
 */
public class OneClickCrossChainTxData extends BaseNulsData {

    /**
     * Heterogeneous chain recharge transactionshash
     */
    private HeterogeneousHash originalTxHash;
    /**
     * Heterogeneous chain exchanges at block height
     */
    private long heterogeneousHeight;
    /**
     * In heterogeneous chain networks Rechargeablefromaddress
     */
    private String heterogeneousFromAddress;
    // Cross chain information of the target chain
    private int desChainId;
    private String desToAddress;
    private String desExtend;
    // Reserved fields
    private byte[] extend;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(this.originalTxHash);
        stream.writeInt64(this.heterogeneousHeight);
        stream.writeString(this.heterogeneousFromAddress);
        stream.writeUint16(this.desChainId);
        stream.writeString(this.desToAddress);
        stream.writeString(this.desExtend);
        stream.writeBytesWithLength(this.extend);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.originalTxHash = byteBuffer.readNulsData(new HeterogeneousHash());
        this.heterogeneousHeight = byteBuffer.readInt64();
        this.heterogeneousFromAddress = byteBuffer.readString();
        this.desChainId = byteBuffer.readUint16();
        this.desToAddress = byteBuffer.readString();
        this.desExtend = byteBuffer.readString();
        this.extend = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(this.originalTxHash);
        size += SerializeUtils.sizeOfInt64();
        size += SerializeUtils.sizeOfString(this.heterogeneousFromAddress);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.desToAddress);
        size += SerializeUtils.sizeOfString(this.desExtend);
        size += SerializeUtils.sizeOfBytes(this.extend);
        return size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousChaiId: %s", this.originalTxHash.getHeterogeneousChainId())).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", this.originalTxHash.getHeterogeneousHash())).append(lineSeparator);
        builder.append(String.format("\theterogeneousHeight: %s", this.heterogeneousHeight)).append(lineSeparator);
        builder.append(String.format("\theterogeneousFromAddress: %s", this.heterogeneousFromAddress)).append(lineSeparator);
        builder.append(String.format("\tdesChainId: %s", this.desChainId)).append(lineSeparator);
        builder.append(String.format("\tdesToAddress: %s", this.desToAddress)).append(lineSeparator);
        builder.append(String.format("\textend: %s", this.extend == null ? "NULL" : Numeric.toHexString(this.extend))).append(lineSeparator);
        return builder.toString();
    }

    public HeterogeneousHash getOriginalTxHash() {
        return originalTxHash;
    }

    public void setOriginalTxHash(HeterogeneousHash originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public long getHeterogeneousHeight() {
        return heterogeneousHeight;
    }

    public void setHeterogeneousHeight(long heterogeneousHeight) {
        this.heterogeneousHeight = heterogeneousHeight;
    }

    public String getHeterogeneousFromAddress() {
        return heterogeneousFromAddress;
    }

    public void setHeterogeneousFromAddress(String heterogeneousFromAddress) {
        this.heterogeneousFromAddress = heterogeneousFromAddress;
    }

    public int getDesChainId() {
        return desChainId;
    }

    public void setDesChainId(int desChainId) {
        this.desChainId = desChainId;
    }

    public String getDesToAddress() {
        return desToAddress;
    }

    public void setDesToAddress(String desToAddress) {
        this.desToAddress = desToAddress;
    }

    public String getDesExtend() {
        return desExtend;
    }

    public void setDesExtend(String desExtend) {
        this.desExtend = desExtend;
    }

    public byte[] getExtend() {
        return extend;
    }

    public void setExtend(byte[] extend) {
        this.extend = extend;
    }
}
