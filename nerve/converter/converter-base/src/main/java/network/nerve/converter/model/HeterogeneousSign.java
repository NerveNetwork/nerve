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

package network.nerve.converter.model;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousAddress;

import java.io.IOException;

/**
 * @author: Loki
 * @date: 2020/9/3
 */
public class HeterogeneousSign extends BaseNulsData {

    /**
     * Heterogeneous chain signature address(Contains heterogeneous chainschainId)
     */
    private HeterogeneousAddress heterogeneousAddress;

    private byte[] signature;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(heterogeneousAddress);
        stream.writeBytesWithLength(signature);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousAddress = byteBuffer.readNulsData(new HeterogeneousAddress());
        this.signature = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(heterogeneousAddress);
        size += SerializeUtils.sizeOfBytes(signature);
        return size;
    }

    public HeterogeneousSign() {
    }

    public HeterogeneousSign(HeterogeneousAddress heterogeneousAddress, byte[] signature) {
        this.heterogeneousAddress = heterogeneousAddress;
        this.signature = signature;
    }

    public HeterogeneousAddress getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(HeterogeneousAddress heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeterogeneousSign that = (HeterogeneousSign) o;

        if (!heterogeneousAddress.equals(that.heterogeneousAddress)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return heterogeneousAddress.hashCode();
    }
}
