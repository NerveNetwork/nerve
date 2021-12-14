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
package network.nerve.converter.heterogeneouschain.lib.model;

import network.nerve.converter.enums.HeterogeneousChainTxType;

/**
 * @author: Mimi
 * @date: 2020-05-21
 */
public class HtgInput {
    private static final HtgInput EMPTY = new HtgInput(false, null, null);
    private boolean isBroadcastTx;
    private boolean isDepositTx;
    private HeterogeneousChainTxType txType;
    private String nerveTxHash;
    private transient boolean isDepositIITx;


    public HtgInput(boolean isBroadcastTx, HeterogeneousChainTxType txType, String nerveTxHash) {
        this.isBroadcastTx = isBroadcastTx;
        this.txType = txType;
        this.nerveTxHash = nerveTxHash;
    }

    public HtgInput(boolean isDepositTx, HeterogeneousChainTxType txType) {
        this.isDepositTx = isDepositTx;
        this.txType = txType;
    }

    public HtgInput(boolean isDepositIITx) {
        this.isDepositIITx = isDepositIITx;
        this.txType = HeterogeneousChainTxType.DEPOSIT;
    }

    public static HtgInput empty() {
        return EMPTY;
    }

    public boolean isBroadcastTx() {
        return isBroadcastTx;
    }

    public void setBroadcastTx(boolean broadcastTx) {
        isBroadcastTx = broadcastTx;
    }

    public boolean isDepositTx() {
        return isDepositTx;
    }

    public void setDepositTx(boolean depositTx) {
        isDepositTx = depositTx;
    }

    public HeterogeneousChainTxType getTxType() {
        return txType;
    }

    public void setTxType(HeterogeneousChainTxType txType) {
        this.txType = txType;
    }

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public boolean isDepositIITx() {
        return isDepositIITx;
    }

    public void setDepositIITx(boolean depositIITx) {
        isDepositIITx = depositIITx;
    }
}
