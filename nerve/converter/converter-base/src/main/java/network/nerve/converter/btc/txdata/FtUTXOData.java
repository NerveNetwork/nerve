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

/**
 * On chain recharge transactionstxdata
 *
 * @author: Loki
 * @date: 2020-02-17
 */
public class FtUTXOData extends BaseNulsData {

    private String txId;

    private int outputIndex;

    private long satoshis;
    private String ftContractId;
    private BigInteger ftBalance;

    public FtUTXOData() {
    }

    public FtUTXOData(String txId) {
        this.txId = txId;
    }

    public FtUTXOData(String txId, int outputIndex, long satoshis, String ftContractId, BigInteger ftBalance) {
        this.txId = txId;
        this.outputIndex = outputIndex;
        this.satoshis = satoshis;
        this.ftContractId = ftContractId;
        this.ftBalance = ftBalance;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.txId);
        stream.writeUint16(this.outputIndex);
        stream.writeUint32(this.satoshis);
        stream.writeString(this.ftContractId);
        stream.writeBigInteger(this.ftBalance);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.txId = byteBuffer.readString();
        this.outputIndex = byteBuffer.readUint16();
        this.satoshis = byteBuffer.readUint32();
        this.ftContractId = byteBuffer.readString();
        this.ftBalance = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.txId);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfString(this.ftContractId);
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public int getOutputIndex() {
        return outputIndex;
    }

    public void setOutputIndex(int outputIndex) {
        this.outputIndex = outputIndex;
    }

    public long getSatoshis() {
        return satoshis;
    }

    public void setSatoshis(long satoshis) {
        this.satoshis = satoshis;
    }

    public String getFtContractId() {
        return ftContractId;
    }

    public void setFtContractId(String ftContractId) {
        this.ftContractId = ftContractId;
    }

    public BigInteger getFtBalance() {
        return ftBalance;
    }

    public void setFtBalance(BigInteger ftBalance) {
        this.ftBalance = ftBalance;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"txId\":")
                .append('\"').append(txId).append('\"');

        sb.append(",\"outputIndex\":")
                .append(outputIndex);
        sb.append(",\"satoshis\":")
                .append(satoshis);

        sb.append(",\"ftContractId\":")
                .append('\"').append(ftContractId).append('\"');
        sb.append(",\"ftBalance\":")
                .append('\"').append(ftBalance).append('\"');

        sb.append('}');
        return sb.toString();
    }
}
