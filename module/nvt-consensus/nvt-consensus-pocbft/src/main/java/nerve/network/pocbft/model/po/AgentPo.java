/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package nerve.network.pocbft.model.po;


import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 存入数据库的节点信息类
 * Node information class stored in database
 *
 * @author: Jason
 * 2018/11/14
 */
public class AgentPo extends BaseNulsData {

    private transient NulsHash hash;

    private byte[] agentAddress;

    private byte[] packingAddress;

    private byte[] rewardAddress;

    private BigInteger deposit;

    private long time;

    private long blockHeight = -1L;

    private long delHeight = -1L;

    private byte[] pubKey;

    public AgentPo(){}

    public AgentPo(Agent agent){
        this.agentAddress = agent.getAgentAddress();
        this.packingAddress = agent.getPackingAddress();
        this.rewardAddress = agent.getRewardAddress();
        this.deposit = agent.getDeposit();
        this.blockHeight = agent.getBlockHeight();
        this.delHeight = agent.getDelHeight();
        this.hash = agent.getTxHash();
        this.time = agent.getTime();
        this.pubKey = agent.getPubKey();
    }

    /**
     * serialize important field
     */
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(hash.getBytes());
        stream.write(agentAddress);
        stream.write(packingAddress);
        stream.write(rewardAddress);
        stream.writeBigInteger(deposit);
        stream.writeUint48(time);
        stream.writeVarInt(blockHeight);
        stream.writeVarInt(delHeight);
        stream.writeBytesWithLength(pubKey);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.hash = byteBuffer.readHash();
        this.agentAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.packingAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.rewardAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deposit = byteBuffer.readBigInteger();
        this.time = byteBuffer.readUint48();
        this.blockHeight = byteBuffer.readVarInt();
        this.delHeight = byteBuffer.readVarInt();
        this.pubKey = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH;
        size += Address.ADDRESS_LENGTH * 3;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfVarInt(blockHeight);
        size += SerializeUtils.sizeOfVarInt(delHeight);
        size += SerializeUtils.sizeOfBytes(pubKey);
        return size;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public byte[] getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(byte[] agentAddress) {
        this.agentAddress = agentAddress;
    }

    public byte[] getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(byte[] packingAddress) {
        this.packingAddress = packingAddress;
    }

    public byte[] getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(byte[] rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getDelHeight() {
        return delHeight;
    }

    public void setDelHeight(long delHeight) {
        this.delHeight = delHeight;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }
}