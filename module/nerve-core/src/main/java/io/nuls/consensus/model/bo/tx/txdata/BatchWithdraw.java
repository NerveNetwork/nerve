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
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.basic.VarInt;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Batch exitstakingTransactionaltxData
 */
public class BatchWithdraw extends BaseNulsData {

    private byte[] address;

    private List<NulsHash> joinTxHashList;

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

    /**
     * serialize important field
     */
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(address);
        stream.writeVarInt(joinTxHashList.size());
        for (NulsHash hash : this.joinTxHashList) {
            stream.write(hash.getBytes());
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readByLengthByte();
        this.joinTxHashList = new ArrayList<>();
        long count = byteBuffer.readVarInt();
        for (int i = 0; i < count; i++) {
            joinTxHashList.add(byteBuffer.readHash());
        }
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfBytes(this.address);
        size += VarInt.sizeOf(joinTxHashList.size());
        size += joinTxHashList.size() * NulsHash.HASH_LENGTH;
        return size;
    }
}
