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
public class StableSwapTradeBus extends BaseBus {

    private byte[] pairAddress;
    /**
     * 交易后的余额变化量
     */
    private BigInteger[] changeBalances;
    /**
     * 交易前的余额
     */
    private BigInteger[] balances;
    /**
     * 进入池子的token数量
     */
    private BigInteger[] amountsIn;
    /**
     * `非`流动性提供者可奖励的交易手续费
     */
    private BigInteger[] unLiquidityAwardFees;
    /**
     * 流出池子的token的位置
     */
    private byte tokenOutIndex;
    /**
     * 流出池子的token数量
     */
    private BigInteger amountOut;
    /**
     * 流出的token接收地址
     */
    private byte[] to;

    public StableSwapTradeBus(byte[] pairAddress, BigInteger[] changeBalances, BigInteger[] balances, BigInteger[] amountsIn, BigInteger[] unLiquidityAwardFees, byte tokenOutIndex, BigInteger amountOut, byte[] to) {
        this.pairAddress = pairAddress;
        this.changeBalances = changeBalances;
        this.balances = balances;
        this.amountsIn = amountsIn;
        this.unLiquidityAwardFees = unLiquidityAwardFees;
        this.tokenOutIndex = tokenOutIndex;
        this.amountOut = amountOut;
        this.to = to;
    }

    public byte[] getPairAddress() {
        return pairAddress;
    }

    public void setPairAddress(byte[] pairAddress) {
        this.pairAddress = pairAddress;
    }

    public BigInteger[] getChangeBalances() {
        return changeBalances;
    }

    public void setChangeBalances(BigInteger[] changeBalances) {
        this.changeBalances = changeBalances;
    }

    public BigInteger[] getBalances() {
        return balances;
    }

    public void setBalances(BigInteger[] balances) {
        this.balances = balances;
    }

    public BigInteger[] getAmountsIn() {
        return amountsIn;
    }

    public void setAmountsIn(BigInteger[] amountsIn) {
        this.amountsIn = amountsIn;
    }

    public BigInteger[] getUnLiquidityAwardFees() {
        return unLiquidityAwardFees;
    }

    public void setUnLiquidityAwardFees(BigInteger[] unLiquidityAwardFees) {
        this.unLiquidityAwardFees = unLiquidityAwardFees;
    }

    public byte getTokenOutIndex() {
        return tokenOutIndex;
    }

    public void setTokenOutIndex(byte tokenOutIndex) {
        this.tokenOutIndex = tokenOutIndex;
    }

    public BigInteger getAmountOut() {
        return amountOut;
    }

    public void setAmountOut(BigInteger amountOut) {
        this.amountOut = amountOut;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }
}
