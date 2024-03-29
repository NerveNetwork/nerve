/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * Send ahash
 *
 * @author captain
 * @version 1.0
 * @date 18-11-9 afternoon2:37
 */
public class HashMessage extends BaseBusinessMessage {

    private NulsHash requestHash;

    private long height;
    private int chainId;
    private String nodeId;

    public HashMessage() {
    }

    public NulsHash getRequestHash() {
        return requestHash;
    }

    public long getHeight() {
        return height;
    }

    public HashMessage(NulsHash hash, long height) {
        this.requestHash = hash;
        this.height = height;
    }

    @Override
    public int size() {
        int size = 4;
        size += NulsHash.HASH_LENGTH;
        return size;
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(height);
        stream.write(requestHash.getBytes());
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.height = byteBuffer.readUint32();
        this.requestHash = byteBuffer.readHash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HashMessage that = (HashMessage) o;

        return requestHash != null ? requestHash.equals(that.requestHash) : that.requestHash == null;
    }

    @Override
    public int hashCode() {
        return requestHash != null ? requestHash.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HashMessage{" +
                "requestHash=" + requestHash +
                '}';
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
