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

package network.nerve.converter.btc.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

public class FtData extends BaseNulsData {

    private String name;
    private String symbol;
    private int decimal;
    private long totalSupply;
    private String codeScript;
    private String tapeScript;
    private String contractTxid;

    public FtData() {
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.name);
        stream.writeString(this.symbol);
        stream.writeUint16(this.decimal);
        stream.writeUint32(this.totalSupply);
        stream.writeString(this.codeScript);
        stream.writeString(this.tapeScript);
        stream.writeString(this.contractTxid);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.name = byteBuffer.readString();
        this.symbol = byteBuffer.readString();
        this.decimal = byteBuffer.readUint16();
        this.totalSupply = byteBuffer.readUint32();
        this.codeScript = byteBuffer.readString();
        this.tapeScript = byteBuffer.readString();
        this.contractTxid = byteBuffer.readString();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.name);
        size += SerializeUtils.sizeOfString(this.symbol);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfString(this.codeScript);
        size += SerializeUtils.sizeOfString(this.tapeScript);
        size += SerializeUtils.sizeOfString(this.contractTxid);
        return size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public long getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(long totalSupply) {
        this.totalSupply = totalSupply;
    }

    public String getCodeScript() {
        return codeScript;
    }

    public void setCodeScript(String codeScript) {
        this.codeScript = codeScript;
    }

    public String getTapeScript() {
        return tapeScript;
    }

    public void setTapeScript(String tapeScript) {
        this.tapeScript = tapeScript;
    }

    public String getContractTxid() {
        return contractTxid;
    }

    public void setContractTxid(String contractTxid) {
        this.contractTxid = contractTxid;
    }
}
