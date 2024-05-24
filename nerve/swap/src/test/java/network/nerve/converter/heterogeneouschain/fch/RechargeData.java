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

package network.nerve.converter.heterogeneouschain.fch;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 *
 * @author: PierreLuo
 * @date: 2022/3/21
 */
public class RechargeData extends BaseNulsData {

    private byte[] to;
    private long value;
    private byte[] feeTo;
    // Reserved fields
    private String extend0;
    private String extend1;
    private String extend2;
    private String extend3;
    private String extend4;
    private String extend5;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(this.to);
        stream.writeVarInt(this.value);
        if (this.feeTo != null) {
            stream.writeByte((byte) 1);
            stream.write(this.feeTo);
        } else {
            stream.writeByte((byte) 0);
        }
        stream.writeString(this.extend0);
        stream.writeString(this.extend1);
        stream.writeString(this.extend2);
        stream.writeString(this.extend3);
        stream.writeString(this.extend4);
        stream.writeString(this.extend5);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.value = byteBuffer.readVarInt();
        byte b = byteBuffer.readByte();
        if (b == 0x1) {
            this.feeTo = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        }
        this.extend0 = byteBuffer.readString();
        this.extend1 = byteBuffer.readString();
        this.extend2 = byteBuffer.readString();
        this.extend3 = byteBuffer.readString();
        this.extend4 = byteBuffer.readString();
        this.extend5 = byteBuffer.readString();
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfVarInt(this.value);
        size += 1;
        if (this.feeTo != null) {
            size += Address.ADDRESS_LENGTH;
        }
        size += SerializeUtils.sizeOfString(this.extend0);
        size += SerializeUtils.sizeOfString(this.extend1);
        size += SerializeUtils.sizeOfString(this.extend2);
        size += SerializeUtils.sizeOfString(this.extend3);
        size += SerializeUtils.sizeOfString(this.extend4);
        size += SerializeUtils.sizeOfString(this.extend5);
        return size;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"to\":")
                .append('\"').append(to == null ? "" : AddressTool.getStringAddressByBytes(to)).append('\"');
        sb.append(",\"value\":")
                .append(value);
        sb.append(",\"feeTo\":")
                .append('\"').append(feeTo == null ? "" : AddressTool.getStringAddressByBytes(feeTo)).append('\"');
        sb.append(",\"extend0\":")
                .append('\"').append(extend0).append('\"');
        sb.append(",\"extend1\":")
                .append('\"').append(extend1).append('\"');
        sb.append(",\"extend2\":")
                .append('\"').append(extend2).append('\"');
        sb.append(",\"extend3\":")
                .append('\"').append(extend3).append('\"');
        sb.append(",\"extend4\":")
                .append('\"').append(extend4).append('\"');
        sb.append(",\"extend5\":")
                .append('\"').append(extend5).append('\"');
        sb.append('}');
        return sb.toString();
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public byte[] getFeeTo() {
        return feeTo;
    }

    public void setFeeTo(byte[] feeTo) {
        this.feeTo = feeTo;
    }

    public String getExtend0() {
        return extend0;
    }

    public void setExtend0(String extend0) {
        this.extend0 = extend0;
    }

    public String getExtend1() {
        return extend1;
    }

    public void setExtend1(String extend1) {
        this.extend1 = extend1;
    }

    public String getExtend2() {
        return extend2;
    }

    public void setExtend2(String extend2) {
        this.extend2 = extend2;
    }

    public String getExtend3() {
        return extend3;
    }

    public void setExtend3(String extend3) {
        this.extend3 = extend3;
    }

    public String getExtend4() {
        return extend4;
    }

    public void setExtend4(String extend4) {
        this.extend4 = extend4;
    }

    public String getExtend5() {
        return extend5;
    }

    public void setExtend5(String extend5) {
        this.extend5 = extend5;
    }
}
