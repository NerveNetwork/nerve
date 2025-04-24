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

package network.nerve.hetool.txdata;

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
public class UTXOData extends BaseNulsData {

    /**
     * Heterogeneous chain recharge transactionshash / Proposal transactionhash
     */
    private String txid;

    private int vout;

    private BigInteger amount;

    public UTXOData() {
    }

    public UTXOData(String txid) {
        this.txid = txid;
    }

    public UTXOData(String txid, int vout, BigInteger amount) {
        this.txid = txid;
        this.vout = vout;
        this.amount = amount;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(this.txid);
        stream.writeUint16(this.vout);
        stream.writeBigInteger(this.amount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.txid = byteBuffer.readString();
        this.vout = byteBuffer.readUint16();
        this.amount = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfString(this.txid);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public int getVout() {
        return vout;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"txid\":")
                .append('\"').append(txid).append('\"');

        sb.append(",\"vout\":")
                .append(vout);

        sb.append(",\"amount\":")
                .append('\"').append(amount).append('\"');

        sb.append('}');
        return sb.toString();
    }
}
