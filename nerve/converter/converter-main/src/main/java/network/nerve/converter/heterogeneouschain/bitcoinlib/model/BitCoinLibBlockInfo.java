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

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2024/7/10
 */
public class BitCoinLibBlockInfo {
    private BitCoinLibBlockHeader header;
    private List txList;

    public BitCoinLibBlockInfo() {
    }

    public BitCoinLibBlockInfo(BitCoinLibBlockHeader header, List txList) {
        this.header = header;
        this.txList = txList;
    }

    public BitCoinLibBlockHeader getHeader() {
        return header;
    }

    public void setHeader(BitCoinLibBlockHeader header) {
        this.header = header;
    }

    public List getTxList() {
        return txList;
    }

    public void setTxList(List txList) {
        this.txList = txList;
    }
}
