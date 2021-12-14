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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousHash;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 异构链充值待确认交易txData
 * 系统交易
 * @author: Loki
 * @date: 2020/9/25
 */
public class RechargeUnconfirmedTxData extends BaseNulsData {

    /**
     * 异构链充值交易hash
     */
    private HeterogeneousHash originalTxHash;

    /**
     * 异构链交易所在区块高度
     */
    private long heterogeneousHeight;

    /**
     * 异构链网络中 充值的from地址
     */
    private String heterogeneousFromAddress;

    /**
     * nerve 网络中的到账地址
     */
    private byte[] nerveToAddress;

    /**
     * nerve 网络中充值资产链Id
     */
    private int assetChainId;

    /**
     * nerve 网络中充值资产Id
     */
    private int assetId;
    /**
     * 充值金额
     */
    private BigInteger amount;
    // 同时充值token和main，记录main
    private BigInteger mainAssetAmount;
    private int mainAssetChainId;
    private int mainAssetId;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(this.originalTxHash);
        stream.writeInt64(this.heterogeneousHeight);
        stream.writeString(this.heterogeneousFromAddress);
        stream.write(this.nerveToAddress);
        stream.writeUint16(this.assetChainId);
        stream.writeUint16(this.assetId);
        stream.writeBigInteger(this.amount);
        if (this.mainAssetAmount != null) {
            stream.writeBigInteger(this.mainAssetAmount);
            stream.writeUint16(this.mainAssetChainId);
            stream.writeUint16(this.mainAssetId);
        }

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.originalTxHash = byteBuffer.readNulsData(new HeterogeneousHash());
        this.heterogeneousHeight = byteBuffer.readInt64();
        this.heterogeneousFromAddress = byteBuffer.readString();
        this.nerveToAddress = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.assetChainId = byteBuffer.readUint16();
        this.assetId = byteBuffer.readUint16();
        this.amount = byteBuffer.readBigInteger();
        if (!byteBuffer.isFinished()) {
            this.mainAssetAmount = byteBuffer.readBigInteger();
            this.mainAssetChainId = byteBuffer.readUint16();
            this.mainAssetId = byteBuffer.readUint16();
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(this.originalTxHash);
        size += SerializeUtils.sizeOfInt64();
        size += SerializeUtils.sizeOfString(this.heterogeneousFromAddress);
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfBigInteger();
        if (this.mainAssetAmount != null) {
            size += SerializeUtils.sizeOfBigInteger();
            size += SerializeUtils.sizeOfInt16();
            size += SerializeUtils.sizeOfInt16();
        }
        return size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousChaiId: %s", this.originalTxHash.getHeterogeneousChainId())).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", this.originalTxHash.getHeterogeneousHash())).append(lineSeparator);
        builder.append(String.format("\theterogeneousHeight: %s", this.heterogeneousHeight)).append(lineSeparator);
        builder.append(String.format("\theterogeneousFromAddress: %s", this.heterogeneousFromAddress)).append(lineSeparator);
        builder.append(String.format("\tnerveToAddress: %s", AddressTool.getStringAddressByBytes(this.nerveToAddress))).append(lineSeparator);
        builder.append(String.format("\tassetChainId: %s", this.assetChainId)).append(lineSeparator);
        builder.append(String.format("\tassetId: %s", this.assetId)).append(lineSeparator);
        builder.append(String.format("\tamount: %s", this.amount.toString())).append(lineSeparator);
        if (this.mainAssetAmount != null) {
            builder.append(String.format("\tmainAssetAmount: %s", this.mainAssetAmount.toString())).append(lineSeparator);
            builder.append(String.format("\tmainAssetChainId: %s", this.mainAssetChainId)).append(lineSeparator);
            builder.append(String.format("\tmainAssetId: %s", this.mainAssetId)).append(lineSeparator);
        }
        return builder.toString();
    }

    public HeterogeneousHash getOriginalTxHash() {
        return originalTxHash;
    }

    public void setOriginalTxHash(HeterogeneousHash originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public long getHeterogeneousHeight() {
        return heterogeneousHeight;
    }

    public void setHeterogeneousHeight(long heterogeneousHeight) {
        this.heterogeneousHeight = heterogeneousHeight;
    }

    public String getHeterogeneousFromAddress() {
        return heterogeneousFromAddress;
    }

    public void setHeterogeneousFromAddress(String heterogeneousFromAddress) {
        this.heterogeneousFromAddress = heterogeneousFromAddress;
    }

    public byte[] getNerveToAddress() {
        return nerveToAddress;
    }

    public void setNerveToAddress(byte[] nerveToAddress) {
        this.nerveToAddress = nerveToAddress;
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

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public BigInteger getMainAssetAmount() {
        return mainAssetAmount;
    }

    public void setMainAssetAmount(BigInteger mainAssetAmount) {
        this.mainAssetAmount = mainAssetAmount;
    }

    public int getMainAssetChainId() {
        return mainAssetChainId;
    }

    public void setMainAssetChainId(int mainAssetChainId) {
        this.mainAssetChainId = mainAssetChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }
}
