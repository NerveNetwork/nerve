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

package network.nerve.pocbft.model.po;

import network.nerve.pocbft.model.bo.tx.txdata.Deposit;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 存入数据库的委托信息类
 * Delegation information class stored in database
 *
 * @author tag
 * 2018/11/14
 */
public class DepositPo extends BaseNulsData {
    private NulsHash txHash;
    private BigInteger deposit;
    private byte[] address;
    private long time;
    private long blockHeight = -1L;
    private long delHeight = -1L;
    private int assetChainId;
    private int assetId;
    private byte depositType;
    private byte timeType;

    public DepositPo(){}

    public DepositPo(Deposit deposit){
        this.deposit = deposit.getDeposit();
        this.address = deposit.getAddress();
        this.assetChainId = deposit.getAssetChainId();
        this.assetId = deposit.getAssetId();
        this.depositType = deposit.getDepositType();
        this.time = deposit.getTime();
        this.timeType = deposit.getTimeType();
        this.delHeight = deposit.getDelHeight();
        this.blockHeight = deposit.getBlockHeight();
        this.txHash = deposit.getTxHash();
    }

    /**
     * serialize important field
     */
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBigInteger(deposit);
        stream.write(address);
        stream.writeUint48(time);
        stream.write(txHash.getBytes());
        stream.writeVarInt(blockHeight);
        stream.writeVarInt(delHeight);
        stream.writeUint16(assetChainId);
        stream.writeUint16(assetId);
        stream.writeByte(depositType);
        stream.writeByte(timeType);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.deposit = byteBuffer.readBigInteger();
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.time = byteBuffer.readUint48();
        this.txHash = byteBuffer.readHash();
        this.blockHeight = byteBuffer.readVarInt();
        this.delHeight = byteBuffer.readVarInt();
        this.assetChainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
        this.depositType = byteBuffer.readByte();
        this.timeType = byteBuffer.readByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += address.length;
        size += SerializeUtils.sizeOfUint48();
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfVarInt(blockHeight);
        size += SerializeUtils.sizeOfVarInt(delHeight);
        size += SerializeUtils.sizeOfUint16() * 2;
        size += 2;
        return size;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public NulsHash getTxHash() {
        return txHash;
    }

    public void setTxHash(NulsHash txHash) {
        this.txHash = txHash;
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
}
