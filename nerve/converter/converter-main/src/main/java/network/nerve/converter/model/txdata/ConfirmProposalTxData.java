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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.enums.ProposalTypeEnum;

import java.io.IOException;

/**
 * 确认提案交易txdata
 */
public class ConfirmProposalTxData extends BaseNulsData {

    /**
     * 提案类型（比如充值的异构链交易hash）
     */
    private byte type;

    /**
     * 相应提案类型下的数据结构
     */
    private byte[] businessData;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeByte(type);
        stream.writeBytesWithLength(businessData);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.type = byteBuffer.readByte();
        this.businessData = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 1;
        size += SerializeUtils.sizeOfBytes(businessData);
        return size;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\ttype: %s", this.type)).append(lineSeparator);
        if (ProposalTypeEnum.getEnum(type).equals(ProposalTypeEnum.UPGRADE)) {
            ConfirmUpgradeTxData txData = new ConfirmUpgradeTxData();
            try {
                txData.parse(new NulsByteBuffer(this.businessData));
            } catch (NulsException e) {
                e.printStackTrace();
            }
            builder.append(txData.toString()).append(lineSeparator);
        } else {
            ProposalExeBusinessData txData = new ProposalExeBusinessData();
            try {
                txData.parse(new NulsByteBuffer(this.businessData));
            } catch (NulsException e) {
                e.printStackTrace();
            }
            builder.append(txData.toString()).append(lineSeparator);

        }
        return builder.toString();
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getBusinessData() {
        return businessData;
    }

    public void setBusinessData(byte[] businessData) {
        this.businessData = businessData;
    }
}
