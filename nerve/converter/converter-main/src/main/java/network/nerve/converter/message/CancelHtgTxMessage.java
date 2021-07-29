/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

package network.nerve.converter.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author: Mimi
 * @date: 2021/5/30
 */
public class CancelHtgTxMessage extends BaseBusinessMessage {

    private int heterogeneousChainId;
    private String heterogeneousAddress;
    private String nonce;
    private String priceGwei;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(this.heterogeneousChainId);
        stream.writeString(this.heterogeneousAddress);
        stream.writeString(this.nonce);
        stream.writeString(this.priceGwei);
    }

    @Override
    public void parse(NulsByteBuffer buffer) throws NulsException {
        this.heterogeneousChainId = buffer.readUint16();
        this.heterogeneousAddress = buffer.readString();
        this.nonce = buffer.readString();
        this.priceGwei = buffer.readString();
    }
    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousAddress);
        size += SerializeUtils.sizeOfString(this.nonce);
        size += SerializeUtils.sizeOfString(this.priceGwei);
        return size;
    }

    public CancelHtgTxMessage() {
    }

    public CancelHtgTxMessage(int heterogeneousChainId, String heterogeneousAddress, String nonce, String priceGwei) {
        this.heterogeneousChainId = heterogeneousChainId;
        this.heterogeneousAddress = heterogeneousAddress;
        this.nonce = nonce;
        this.priceGwei = priceGwei;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getPriceGwei() {
        return priceGwei;
    }

    public void setPriceGwei(String priceGwei) {
        this.priceGwei = priceGwei;
    }

    public String toKey() {
        final StringBuilder sb = new StringBuilder();
        sb.append(heterogeneousChainId).append(',')
                .append(heterogeneousAddress).append(',')
                .append(nonce).append(',')
                .append(priceGwei);
        return sb.toString();
    }
}
