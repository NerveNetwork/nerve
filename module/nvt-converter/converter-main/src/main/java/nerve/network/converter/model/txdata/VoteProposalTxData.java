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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.io.IOException;

/**
 * 对提案进行投票交易txdata
 * @author: Chino
 * @date: 2020-02-18
 */
public class VoteProposalTxData extends BaseNulsData {

    /**
     * 提案交易hash
     */
    private NulsHash proposalTxHash;

    /**
     * 表决提案 0:against反对，1:favor赞成
     */
    private byte choice;

    public VoteProposalTxData() {
    }

    public VoteProposalTxData(NulsHash proposalTxHash, byte choice) {
        this.proposalTxHash = proposalTxHash;
        this.choice = choice;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(proposalTxHash.getBytes());
        stream.writeByte(choice);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.proposalTxHash = byteBuffer.readHash();
        this.choice = byteBuffer.readByte();
    }

    @Override
    public int size() {
        return NulsHash.HASH_LENGTH + 1;
    }

    public NulsHash getProposalTxHash() {
        return proposalTxHash;
    }

    public void setProposalTxHash(NulsHash proposalTxHash) {
        this.proposalTxHash = proposalTxHash;
    }

    public byte getChoice() {
        return choice;
    }

    public void setChoice(byte choice) {
        this.choice = choice;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\tproposalTxHash: %s", proposalTxHash.toHex())).append(lineSeparator);
        builder.append(String.format("\tchoice: %s", choice)).append(lineSeparator);
        return builder.toString();
    }
}
