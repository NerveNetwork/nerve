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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2022/3/21
 */
public class FchSignData extends BaseNulsData {

    private String addr;
    private List<byte[]> signatures;

    public FchSignData() {
    }

    public FchSignData(String addr, List<byte[]> signatures) {
        this.addr = addr;
        this.signatures = signatures;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.addr);
        stream.writeUint8((short) signatures.size());
        for (byte[] sign : signatures) {
            stream.writeBytesWithLength(sign);
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.addr = byteBuffer.readString();
        int size = byteBuffer.readUint8();
        signatures = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            signatures.add(byteBuffer.readByLengthByte());
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(addr);
        size += SerializeUtils.sizeOfUint8();
        for (int i = 0; i < signatures.size(); i++) {
            size += SerializeUtils.sizeOfBytes(signatures.get(i));
        }
        return size;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public List<byte[]> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<byte[]> signatures) {
        this.signatures = signatures;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"addr\":")
                .append('\"').append(addr).append('\"');
        sb.append(",\"signatures\":[");
        if (signatures != null && signatures.size() > 0) {
            for (byte[] sign : signatures) {
                sb.append('\"').append(HexUtil.encode(sign)).append('\"').append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]}");
        return sb.toString();
    }

}
