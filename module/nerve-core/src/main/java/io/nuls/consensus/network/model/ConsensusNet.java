/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.consensus.network.model;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description
 */
public class ConsensusNet extends BaseNulsData {
    private byte[] pubKey;
    private String nodeId;
    private transient boolean hadConnect = false;
    private transient String address = "";
    //获取有效连接的失败次数
    private transient int failTimes=0;

    public ConsensusNet(byte[] publicKey, String nodeId) {
        this.pubKey = ByteUtils.copyOf(publicKey, publicKey.length);
        this.nodeId = nodeId;
    }

    public ConsensusNet(byte[] publicKey, String nodeId, int chainId) {
        this(publicKey,nodeId);
        this.address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(publicKey, chainId));
    }

    public ConsensusNet(String address, String nodeId) {
        this.address = address;
        this.nodeId = nodeId;
    }

    public ConsensusNet() {
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(pubKey);
        stream.writeString(nodeId);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.pubKey = byteBuffer.readByLengthByte();
        this.nodeId = byteBuffer.readString();
    }


    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBytes(pubKey);
        size += SerializeUtils.sizeOfString(nodeId);
        return size;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = ByteUtils.copyOf(pubKey, pubKey.length);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isHadConnect() {
        return hadConnect;
    }

    public void setHadConnect(boolean hadConnect) {
        this.hadConnect = hadConnect;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getFailTimes() {
        return failTimes;
    }

    public void setFailTimes(int failTimes) {
        this.failTimes = failTimes;
    }
}
