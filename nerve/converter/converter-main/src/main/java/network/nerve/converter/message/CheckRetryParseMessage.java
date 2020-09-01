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

package network.nerve.converter.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author: Loki
 * @date: 2020/8/12
 */
public class CheckRetryParseMessage extends BaseBusinessMessage {

    private int heterogeneousChainId;

    private String heterogeneousTxHash;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(this.heterogeneousChainId);
        stream.writeString(this.heterogeneousTxHash);
    }

    @Override
    public void parse(NulsByteBuffer buffer) throws NulsException {
        this.heterogeneousChainId = buffer.readUint16();
        this.heterogeneousTxHash = buffer.readString();
    }
    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        return size;
    }

    public CheckRetryParseMessage() {
    }

    public CheckRetryParseMessage(int heterogeneousChainId, String heterogeneousTxHash) {
        this.heterogeneousChainId = heterogeneousChainId;
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }
}
