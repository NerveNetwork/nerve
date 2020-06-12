/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟银行变更交易
 * @author: Loki
 * @date: 2020-02-17
 */
public class ChangeVirtualBankTxData extends BaseNulsData {

    /**
     * 加入虚拟银行节点地址
     */
    private List<byte[]> inAgents;

    /**
     * 退出虚拟银行节点地址
     */
    private List<byte[]> outAgents;

    /**
     * 退出节点stopAgent交易确认高度
     * 退出节点时,本交易会立即触发,
     * 所以一个交易中ouAgents退出列表中的节点,都是在同一高度退出的节点
     */
    private long outHeight;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        int inCount = inAgents == null ? 0 : inAgents.size();
        stream.writeUint16(inCount);
        if(null != inAgents){
            for(byte[] addressBytes : inAgents){
                stream.write(addressBytes);
            }
        }
        int outCount = outAgents == null ? 0 : outAgents.size();
        stream.writeUint16(outCount);
        if(null != outAgents){
            for(byte[] addressBytes : outAgents){
                stream.write(addressBytes);
            }
        }
        stream.writeInt64(outHeight);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int inCount = byteBuffer.readUint16();
        if(0 < inCount){
            List<byte[]> inAgents = new ArrayList<>();
            for(int i = 0; i< inCount; i++){
                inAgents.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
            }
            this.inAgents = inAgents;
        }
        int outCount = byteBuffer.readUint16();
        if(0 < outCount){
            List<byte[]> outAgents = new ArrayList<>();
            for(int i = 0; i< outCount; i++){
                outAgents.add(byteBuffer.readBytes(Address.ADDRESS_LENGTH));
            }
            this.outAgents = outAgents;
        }
        this.outHeight = byteBuffer.readInt64();
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
        if (null != inAgents) {
            size += Address.ADDRESS_LENGTH * inAgents.size();
        }
        size += SerializeUtils.sizeOfUint16();
        if (null != outAgents) {
            size += Address.ADDRESS_LENGTH * outAgents.size();
        }
        size += SerializeUtils.sizeOfInt64();
        return size;
    }

    public List<byte[]> getInAgents() {
        return inAgents;
    }

    public void setInAgents(List<byte[]> inAgents) {
        this.inAgents = inAgents;
    }

    public List<byte[]> getOutAgents() {
        return outAgents;
    }

    public void setOutAgents(List<byte[]> outAgents) {
        this.outAgents = outAgents;
    }

    public long getOutHeight() {
        return outHeight;
    }

    public void setOutHeight(long outHeight) {
        this.outHeight = outHeight;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        if (inAgents == null) {
            builder.append("\tinAgents: null").append(lineSeparator);
        } else if (inAgents.size() == 0) {
            builder.append("\tinAgents: size 0").append(lineSeparator);
        } else {
            builder.append("\tinAgents:").append(lineSeparator);
            for(byte[] bytes : inAgents){
                builder.append(String.format("\t\t%s", AddressTool.getStringAddressByBytes(bytes))).append(lineSeparator);
            }
        }

        if (outAgents == null) {
            builder.append("\toutAgents: null").append(lineSeparator);
        } else if (outAgents.size() == 0) {
            builder.append("\toutAgents: size 0").append(lineSeparator);
        } else {
            builder.append("\toutAgents:").append(lineSeparator);
            for(byte[] bytes : outAgents){
                builder.append(String.format("\t\t%s", AddressTool.getStringAddressByBytes(bytes))).append(lineSeparator);
            }
        }

        builder.append("\toutHeight: ").append(outHeight).append(lineSeparator);;
        return builder.toString();
    }
}
