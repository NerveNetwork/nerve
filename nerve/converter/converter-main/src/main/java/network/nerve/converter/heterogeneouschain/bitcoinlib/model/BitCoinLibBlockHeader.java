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

/**
 * @author: PierreLuo
 * @date: 2024/7/10
 */
public class BitCoinLibBlockHeader {
    private long blockHeight;
    private long txTime;
    private String blockHash;
    private String preBlockHash;

    public BitCoinLibBlockHeader(long blockHeight, long txTime, String blockHash, String preBlockHash) {
        this.blockHeight = blockHeight;
        this.txTime = txTime;
        this.blockHash = blockHash;
        this.preBlockHash = preBlockHash;
    }

    public BitCoinLibBlockHeader() {
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getTxTime() {
        return txTime;
    }

    public void setTxTime(long txTime) {
        this.txTime = txTime;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getPreBlockHash() {
        return preBlockHash;
    }

    public void setPreBlockHash(String preBlockHash) {
        this.preBlockHash = preBlockHash;
    }
}
