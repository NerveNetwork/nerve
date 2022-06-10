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

package network.nerve.quotation.model.txdata;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.quotation.util.CommonUtil;

import java.io.IOException;

/**
 * 报价交易业务数据
 * @author: Loki
 * @date: 2019/11/25
 */
public class Quotation extends BaseNulsData {

    /**
     * 发送交易的节点打包地址(防止出现不同节点组装出的交易，计算交易hash的数据一致, 导致出现相同交易hash)
     */
    private byte[] address;
    /**
     * 表示data的数据类型 1：price（后续可扩展）
     */
    private byte type;

    private byte[] data;

    private transient Prices prices;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(this.address);
        stream.writeByte(this.type);
        stream.writeBytesWithLength(this.data);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readByLengthByte();
        this.type = byteBuffer.readByte();
        this.data = byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 1;
        size += SerializeUtils.sizeOfBytes(address);
        size += SerializeUtils.sizeOfBytes(this.data);
        return size;
    }

    public Quotation() {
    }

    public Quotation(byte[] address, byte type, byte[] data) {
        this.address = address;
        this.type = type;
        this.data = data;
    }

    public Prices getPrices() throws NulsException {
        return CommonUtil.getInstance(data, Prices.class);
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    @Override
    public String toString() {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("\taddress: %s", AddressTool.getStringAddressByBytes(this.address))).append(lineSeparator);
        builder.append(String.format("\ttype: %s", this.type)).append(lineSeparator);
        builder.append("\tdata: ").append(lineSeparator);
        Prices prices = new Prices();
        String data = null;
        try {
            prices.parse(new NulsByteBuffer(this.data));
            data = prices.toString();
        } catch (NulsException e) {
            Log.error("format txData error", e);
        }
        if(null == data){
            builder.append("\t\t");
        }
        builder.append(data);
        return builder.toString();
    }
}
