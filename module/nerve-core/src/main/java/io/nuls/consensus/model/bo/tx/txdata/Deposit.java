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
package io.nuls.consensus.model.bo.tx.txdata;


import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.consensus.model.dto.input.CreateDepositDTO;
import io.nuls.consensus.model.po.DepositPo;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Entrusted Information
 * Delegated information class
 *
 * @author tag
 * 2018/11/28
 */
@ApiModel(name = "Entrustment information")
public class Deposit extends BaseNulsData {
    @ApiModelProperty(description = "Entrusted amount")
    private BigInteger deposit;
    @ApiModelProperty(description = "Entrusted account")
    private byte[] address;
    @ApiModelProperty(description = "Asset ChainID")
    private int assetChainId;
    @ApiModelProperty(description = "assetID")
    private int assetId;
    @ApiModelProperty(description = "Entrustment type")
    private byte depositType;
    @ApiModelProperty(description = "Entrustment duration")
    private byte timeType;
    @ApiModelProperty(description = "Entrustment time")
    private transient long time;
    @ApiModelProperty(description = "Entrusted transactionHASH")
    private transient NulsHash txHash;
    @ApiModelProperty(description = "The height at which the entrusted transaction is packaged")
    private transient long blockHeight = -1L;
    @ApiModelProperty(description = "Exit commission height")
    private transient long delHeight = -1L;


    public Deposit(){}

    public Deposit(CreateDepositDTO dto){
        this.deposit = BigIntegerUtils.stringToBigInteger(dto.getDeposit());
        this.address = AddressTool.getAddress(dto.getAddress());
        this.assetChainId = dto.getAssetChainId();
        this.assetId = dto.getAssetId();
        this.depositType = dto.getDepositType();
        this.timeType = dto.getTimeType();
    }

    public Deposit(DepositPo po){
        this.deposit = po.getDeposit();
        this.address = po.getAddress();
        this.assetChainId = po.getAssetChainId();
        this.assetId = po.getAssetId();
        this.depositType = po.getDepositType();
        this.time = po.getTime();
        this.timeType = po.getTimeType();
        this.delHeight = po.getDelHeight();
        this.blockHeight = po.getBlockHeight();
        this.txHash = po.getTxHash();
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
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.deposit = byteBuffer.readBigInteger();
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.assetChainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
        this.depositType = byteBuffer.readByte();
        this.timeType = byteBuffer.readByte();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBigInteger();
        size += Address.ADDRESS_LENGTH;
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

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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

    @Override
    public Deposit clone() throws CloneNotSupportedException {
        return (Deposit) super.clone();
    }

}
