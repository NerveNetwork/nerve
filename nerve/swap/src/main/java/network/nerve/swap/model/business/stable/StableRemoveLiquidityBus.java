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
package network.nerve.swap.model.business.stable;

import network.nerve.swap.model.business.BaseBus;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class StableRemoveLiquidityBus extends BaseBus {

    /**
     * 赎回的资产
     */
    private BigInteger[] amounts;
    /**
     * 当前池子（赎回前）
     */
    private BigInteger[] balances;
    /**
     * 销毁的LP资产
     */
    private BigInteger liquidity;
    /**
     * 交易对地址
     */
    private byte[] pairAddress;
    /**
     * 资产接收地址
     */
    private byte[] to;

    public StableRemoveLiquidityBus() {
    }

    public StableRemoveLiquidityBus(BigInteger[] amounts, BigInteger[] balances, BigInteger liquidity, byte[] pairAddress, byte[] to) {
        this.amounts = amounts;
        this.balances = balances;
        this.liquidity = liquidity;
        this.pairAddress = pairAddress;
        this.to = to;
    }

    public byte[] getPairAddress() {
        return pairAddress;
    }

    public void setPairAddress(byte[] pairAddress) {
        this.pairAddress = pairAddress;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
    }

    public BigInteger[] getAmounts() {
        return amounts;
    }

    public void setAmounts(BigInteger[] amounts) {
        this.amounts = amounts;
    }

    public BigInteger[] getBalances() {
        return balances;
    }

    public void setBalances(BigInteger[] balances) {
        this.balances = balances;
    }
}
