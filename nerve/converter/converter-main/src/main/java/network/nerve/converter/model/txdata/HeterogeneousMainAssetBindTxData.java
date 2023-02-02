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
package network.nerve.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
public class HeterogeneousMainAssetBindTxData extends BaseNulsData {

    private int chainId;
    private int nerveAssetChainId;
    private int nerveAssetId;
    private byte[] data;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(chainId);
        stream.writeUint16(nerveAssetChainId);
        stream.writeUint16(nerveAssetId);
        stream.writeBytesWithLength(data);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readUint16();
        this.nerveAssetChainId = byteBuffer.readUint16();
        this.nerveAssetId = byteBuffer.readUint16();
        this.data = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += 2;
        size += 2;
        size += 2;
        size += SerializeUtils.sizeOfBytes(data);
        return size;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getNerveAssetChainId() {
        return nerveAssetChainId;
    }

    public void setNerveAssetChainId(int nerveAssetChainId) {
        this.nerveAssetChainId = nerveAssetChainId;
    }

    public int getNerveAssetId() {
        return nerveAssetId;
    }

    public void setNerveAssetId(int nerveAssetId) {
        this.nerveAssetId = nerveAssetId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\tchainId: %s", chainId)).append(lineSeparator);
        builder.append(String.format("\tnerveAssetChainId: %s", nerveAssetChainId)).append(lineSeparator);
        builder.append(String.format("\tnerveAssetId: %s", nerveAssetId)).append(lineSeparator);
        return builder.toString();
    }
}
