/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * 发起提案交易txdata
 * @author: Chino
 * @date: 2020-02-18
 */
public class ProposalTxData extends BaseNulsData {

    /**
     * 提案类型（比如充值的异构链交易hash）
     */
    private byte type;

    /**
     * 提案内容
     */
    private String content;
    /**
     * 原始交易hash
     */
    private String heterogeneousTxHash;

    /**
     * NERVE地址（账户、节点地址等）
     */
    private byte[] address;

    /**
     * 投票范围类型
     */
    private byte voteRangeType;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\ttype: %s", type)).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", heterogeneousTxHash)).append(lineSeparator);
        builder.append(String.format("\taddress: %s", AddressTool.getStringAddressByBytes(address))).append(lineSeparator);
        builder.append(String.format("\tvoteRangeType: %s", voteRangeType)).append(lineSeparator);
        builder.append(String.format("\tcontent: %s", content)).append(lineSeparator);
        return builder.toString();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeByte(type);
        stream.writeString(content);
        stream.writeString(heterogeneousTxHash);
        stream.writeBytesWithLength(address);
        stream.writeByte(voteRangeType);

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.type = byteBuffer.readByte();
        this.content = byteBuffer.readString();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.address = byteBuffer.readByLengthByte();
        this.voteRangeType = byteBuffer.readByte();


    }

    @Override
    public int size() {
        int size = 0;
        size += 2;
        size += SerializeUtils.sizeOfString(this.content);
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += SerializeUtils.sizeOfBytes(this.address);
        return size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
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
}
