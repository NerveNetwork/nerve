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
package network.nerve.swap.model.bo;

import io.nuls.base.data.Transaction;
import network.nerve.swap.model.business.SwapTradeBus;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class SwapResult {

    private boolean success;
    private String errorMessage;
    private String hash;
    private int txType;
    private long txTime;
    private long blockHeight;
    private String business;
    private String subTxStr;
    private transient Transaction subTx;
    private transient SwapTradeBus swapTradeBus;

    public SwapTradeBus getSwapTradeBus() {
        return swapTradeBus;
    }

    public void setSwapTradeBus(SwapTradeBus swapTradeBus) {
        this.swapTradeBus = swapTradeBus;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getTxType() {
        return txType;
    }

    public void setTxType(int txType) {
        this.txType = txType;
    }

    public long getTxTime() {
        return txTime;
    }

    public void setTxTime(long txTime) {
        this.txTime = txTime;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getSubTxStr() {
        return subTxStr;
    }

    public void setSubTxStr(String subTxStr) {
        this.subTxStr = subTxStr;
    }

    public Transaction getSubTx() {
        return subTx;
    }

    public void setSubTx(Transaction subTx) {
        this.subTx = subTx;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":")
                .append(success);
        sb.append(",\"errorMessage\":")
                .append('\"').append(errorMessage).append('\"');
        sb.append(",\"hash\":")
                .append('\"').append(hash).append('\"');
        sb.append(",\"txType\":")
                .append(txType);
        sb.append(",\"txTime\":")
                .append(txTime);
        sb.append(",\"blockHeight\":")
                .append(blockHeight);
        sb.append(",\"business\":")
                .append('\"').append(business).append('\"');
        sb.append(",\"subTxStr\":")
                .append('\"').append(subTxStr).append('\"');
        sb.append(",\"swapTradeBus\":")
                .append('\"').append(swapTradeBus).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
