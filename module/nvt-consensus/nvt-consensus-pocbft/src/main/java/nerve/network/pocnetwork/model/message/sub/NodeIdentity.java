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
package nerve.network.pocnetwork.model.message.sub;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description 节点身份消息
 */
public class NodeIdentity extends BaseNulsData {
    /**
     * 密文
     */
    private byte[] identity;
    /**
     * 公钥
     */
    private byte[] pubKey;

    public NodeIdentity(byte[] identity, byte[] pubKey) {
        this.identity = identity;
        this.pubKey = pubKey;
    }

    public NodeIdentity() {
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(identity);
        stream.writeBytesWithLength(pubKey);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.identity = byteBuffer.readByLengthByte();
        this.pubKey = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfBytes(this.identity);
        size += SerializeUtils.sizeOfBytes(this.pubKey);
        return size;
    }

    public byte[] getIdentity() {
        return identity;
    }

    public void setIdentity(byte[] identity) {
        this.identity = identity;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }
}
