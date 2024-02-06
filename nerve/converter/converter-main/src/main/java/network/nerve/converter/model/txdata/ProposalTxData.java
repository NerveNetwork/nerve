/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import org.web3j.utils.Numeric;

import java.io.IOException;

/**
 * Initiate proposal transactionstxdata
 * @author: Loki
 * @date: 2020-02-18
 */
public class ProposalTxData extends BaseNulsData {

    /**
     * Proposal type（For example, heterogeneous chain transactions for recharginghash）
     */
    private byte type;

    /**
     * Proposal content
     */
    private String content;

    /**
     * Heterogeneous chainchainId
     */
    private int heterogeneousChainId;
    /**
     * Heterogeneous chain original transactionhash
     */
    private String heterogeneousTxHash;

    /**
     * address（account、Node address, etc）
     */
    private byte[] address;

    /**
     * On chain transactionshash(for example Proposal after failed withdrawal)
     */
    private byte[] hash;

    /**
     * Voting scope type
     */
    private byte voteRangeType;



    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\ttype: %s", type)).append(lineSeparator);
        builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", null == heterogeneousTxHash ? "" : heterogeneousTxHash)).append(lineSeparator);
        String addressStr = "";
        if(null != address){
            addressStr = AddressTool.getStringAddressByBytes(address);
            if(null == addressStr) {
                addressStr = Numeric.toHexString(address);
            }
        }
        builder.append(String.format("\taddress: %s", addressStr)).append(lineSeparator);
        builder.append(String.format("\thash: %s", null == hash ? "" : HexUtil.encode(hash))).append(lineSeparator);
        builder.append(String.format("\tvoteRangeType: %s", voteRangeType)).append(lineSeparator);
        builder.append(String.format("\tcontent: %s", content)).append(lineSeparator);
        return builder.toString();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeByte(type);
        stream.writeString(content);
        stream.writeUint16(heterogeneousChainId);
        stream.writeString(heterogeneousTxHash);
        stream.writeBytesWithLength(address);
        stream.writeBytesWithLength(hash);
        stream.writeByte(voteRangeType);

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.type = byteBuffer.readByte();
        this.content = byteBuffer.readString();
        this.heterogeneousChainId = byteBuffer.readUint16();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.address = byteBuffer.readByLengthByte();
        this.hash = byteBuffer.readByLengthByte();
        this.voteRangeType = byteBuffer.readByte();


    }

    @Override
    public int size() {
        int size = 0;
        size += 2;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.content);
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += SerializeUtils.sizeOfBytes(this.address);
        size += SerializeUtils.sizeOfBytes(this.hash);
        return size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
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

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public byte getVoteRangeType() {
        return voteRangeType;
    }

    public void setVoteRangeType(byte voteRangeType) {
        this.voteRangeType = voteRangeType;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }
}
