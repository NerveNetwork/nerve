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
package network.nerve.converter.heterogeneouschain.bitcoinlib.model;

import network.nerve.converter.enums.HeterogeneousChainTxType;

/**
 * @author: PierreLuo
 * @date: 2024/7/10
 */
public class AnalysisTxInfo {
    private HeterogeneousChainTxType txType;
    private Object tx;

    public AnalysisTxInfo() {
    }

    public AnalysisTxInfo(HeterogeneousChainTxType txType, Object tx) {
        this.txType = txType;
        this.tx = tx;
    }

    public HeterogeneousChainTxType getTxType() {
        return txType;
    }

    public void setTxType(HeterogeneousChainTxType txType) {
        this.txType = txType;
    }

    public Object getTx() {
        return tx;
    }

    public void setTx(Object tx) {
        this.tx = tx;
    }
}
