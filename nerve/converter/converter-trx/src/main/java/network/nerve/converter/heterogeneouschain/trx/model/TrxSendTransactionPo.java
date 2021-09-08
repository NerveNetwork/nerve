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
package network.nerve.converter.heterogeneouschain.trx.model;

import network.nerve.converter.heterogeneouschain.lib.model.HtgSendTransactionPo;

import java.math.BigInteger;

/**
 * @author: Mimi
 * @date: 2020-06-29
 */
public class TrxSendTransactionPo {

    private String txHash;
    private String from;
    private String to;
    private BigInteger value;
    private String data;
    private BigInteger feeLimit;

    public TrxSendTransactionPo(String txHash, String from, String to, BigInteger value, String data, BigInteger feeLimit) {
        this.txHash = txHash;
        this.from = from;
        this.to = to;
        this.value = value;
        this.data = data;
        this.feeLimit = feeLimit;
    }

    public TrxSendTransactionPo(HtgSendTransactionPo htgPo) {
        this.txHash = htgPo.getTxHash();
        this.from = htgPo.getFrom();
        this.to = htgPo.getTo();
        this.value = htgPo.getValue();
        this.data = htgPo.getData();
        this.feeLimit = htgPo.getGasLimit();
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BigInteger getFeeLimit() {
        return feeLimit;
    }

    public void setFeeLimit(BigInteger feeLimit) {
        this.feeLimit = feeLimit;
    }

    public HtgSendTransactionPo toHtgPo() {
        return new HtgSendTransactionPo(txHash, from, null, null, feeLimit, to, value, data);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"txHash\":")
                .append('\"').append(txHash).append('\"');
        sb.append(",\"from\":")
                .append('\"').append(from).append('\"');
        sb.append(",\"to\":")
                .append('\"').append(to).append('\"');
        sb.append(",\"value\":")
                .append(value);
        sb.append(",\"data\":")
                .append('\"').append(data).append('\"');
        sb.append(",\"feeLimit\":")
                .append(feeLimit);
        sb.append('}');
        return sb.toString();
    }
}
