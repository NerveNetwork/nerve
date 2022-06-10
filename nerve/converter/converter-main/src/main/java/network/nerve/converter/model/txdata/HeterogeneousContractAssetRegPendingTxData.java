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

import java.io.IOException;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
public class HeterogeneousContractAssetRegPendingTxData extends BaseNulsData {

    private int chainId;
    private byte decimals;
    private String symbol;
    private String contractAddress;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(chainId);
        stream.writeByte(decimals);
        stream.writeString(symbol);
        stream.writeString(contractAddress);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readUint16();
        this.decimals = byteBuffer.readByte();
        this.symbol = byteBuffer.readString();
        this.contractAddress = byteBuffer.readString();
    }

    @Override
    public int size() {
        int size = 0;
        size += 3;
        size += SerializeUtils.sizeOfString(this.symbol);
        size += SerializeUtils.sizeOfString(this.contractAddress);
        return size;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public byte getDecimals() {
        return decimals;
    }

    public void setDecimals(byte decimals) {
        this.decimals = decimals;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String toString1() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\tchainId: %s", chainId)).append(lineSeparator);
        builder.append(String.format("\tdecimals: %s", decimals)).append(lineSeparator);
        builder.append(String.format("\tsymbol: %s", symbol)).append(lineSeparator);
        builder.append(String.format("\tcontractAddress: %s",contractAddress)).append(lineSeparator);
        return builder.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"chainId\":")
                .append(chainId);
        sb.append(",\"decimals\":")
                .append(decimals);
        sb.append(",\"symbol\":")
                .append('\"').append(symbol).append('\"');
        sb.append(",\"contractAddress\":")
                .append('\"').append(contractAddress).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
