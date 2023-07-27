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

package io.nuls.consensus.model.bo.tx.txdata;


import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.basic.VarInt;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.model.ApiModelProperty;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 活期staking合并交易的txData
 */
public class BatchStakingMerge extends BaseNulsData {

    private byte[] address;

    private List<NulsHash> joinTxHashList;


    private BigInteger deposit;

    private int assetChainId;

    private int assetId;

    private byte depositType;

    private byte timeType;

    public Set<byte[]> getAddresses() {
        Set<byte[]> addressSet = new HashSet<>();
        if (null != address) {
            addressSet.add(this.address);
        }
        return addressSet;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public List<NulsHash> getJoinTxHashList() {
        return joinTxHashList;
    }

    public void setJoinTxHashList(List<NulsHash> joinTxHashList) {
        this.joinTxHashList = joinTxHashList;
    }

    public byte getDepositType() {
        return depositType;
    }

    public void setDepositType(byte depositType) {
        this.depositType = depositType;
    }

    public byte getTimeType() {
        return timeType;
    }

    public void setTimeType(byte timeType) {
        this.timeType = timeType;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    /**
     * serialize important field
     */
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(deposit);
        stream.write(address);
        stream.writeUint16(assetChainId);
        stream.writeUint16(assetId);
        stream.writeByte(depositType);
        stream.writeByte(timeType);
        stream.writeVarInt(joinTxHashList.size());
        for (NulsHash hash : this.joinTxHashList) {
            stream.write(hash.getBytes());
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.deposit = byteBuffer.readBigInteger();
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.assetChainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
        this.depositType = byteBuffer.readByte();
        this.timeType = byteBuffer.readByte();
        this.joinTxHashList = new ArrayList<>();
        long count = byteBuffer.readVarInt();
        for (int i = 0; i < count; i++) {
            joinTxHashList.add(byteBuffer.readHash());
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfBigInteger();
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint16() * 2;
        size += 2;
        size += VarInt.sizeOf(joinTxHashList.size());
        size += joinTxHashList.size() * NulsHash.HASH_LENGTH;
        return size;
    }
}
