/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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


import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import static network.nerve.swap.utils.SwapUtils.minus;


/**
 * @author: PierreLuo
 * @date: 2018/6/7
 */
public class LedgerBalance implements Serializable {

    private byte[] address;
    private int assetsChainId;
    private int assetsId;
    private BigInteger balance;
    private BigInteger freeze;
    private byte[] nonce;
    /**
     * Store the first transaction of a continuous transactionnonce, used to roll back continuous transactions
     */
    private byte[] preNonce;

    public static LedgerBalance newInstance() {
        return new LedgerBalance();
    }

    private LedgerBalance() {
        this.balance = BigInteger.ZERO;
        this.freeze = BigInteger.ZERO;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public int getAssetsChainId() {
        return assetsChainId;
    }

    public void setAssetsChainId(int assetsChainId) {
        this.assetsChainId = assetsChainId;
    }

    public int getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(int assetsId) {
        this.assetsId = assetsId;
    }

    public BigInteger getTotal() {
        return balance.add(freeze);
    }

    public void minusTemp(BigInteger amount) {
        this.balance = minus(balance, amount);
    }

    public void addTemp(BigInteger amount) {
        this.balance = balance.add(amount);
    }

    public void minusLockedTemp(BigInteger amount) {
        this.freeze = minus(freeze, amount);
    }

    public void addLockedTemp(BigInteger amount) {
        this.freeze = freeze.add(amount);
    }

    public BigInteger getBalance() {
        return balance;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public BigInteger getFreeze() {
        return freeze;
    }

    public void setFreeze(BigInteger freeze) {
        this.freeze = freeze;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public byte[] getPreNonce() {
        return preNonce;
    }

    public void setPreNonce(byte[] preNonce) {
        this.preNonce = preNonce;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"address\":")
                .append("'").append(AddressTool.getStringAddressByBytes(address)).append("'");
        sb.append(",\"assetsChainId\":")
                .append(assetsChainId);
        sb.append(",\"assetsId\":")
                .append(assetsId);
        sb.append(",\"balance\":")
                .append("'").append(balance).append("'");
        sb.append(",\"freeze\":")
                .append("'").append(freeze).append("'");
        sb.append(",\"nonce\":")
                .append("'").append(nonce == null ? "" : HexUtil.encode(nonce)).append("'");
        sb.append(",\"preNonce\":")
                .append("'").append(preNonce == null ? "" : HexUtil.encode(preNonce)).append("'");
        sb.append('}');
        return sb.toString();
    }
}
