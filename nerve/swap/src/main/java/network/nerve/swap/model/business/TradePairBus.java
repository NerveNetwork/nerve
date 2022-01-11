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
package network.nerve.swap.model.business;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/9
 */
public class TradePairBus extends BaseBus {
    private byte[] pairAddress;
    /**
     * 交易后的余额
     */
    private BigInteger balance0;
    private BigInteger balance1;
    /**
     * 交易前的余额
     */
    private BigInteger reserve0;
    private BigInteger reserve1;
    /**
     * 进入池子的token
     */
    private NerveToken tokenIn;
    private BigInteger amountIn;
    /**
     * `非`流动性提供者可奖励的交易手续费
     */
    private BigInteger unLiquidityAwardFee;
    /**
     * 流出池子的token
     */
    private NerveToken tokenOut;
    private BigInteger amountOut;
    private byte[] to;
    private transient StableSwapTradeBus stableSwapTradeBus;// 用于普通swap结合稳定币swap的缓存更新

    public TradePairBus(byte[] pairAddress, BigInteger balance0, BigInteger balance1, BigInteger reserve0, BigInteger reserve1, NerveToken tokenIn, BigInteger amountIn, BigInteger unLiquidityAwardFee, NerveToken tokenOut, BigInteger amountOut, byte[] to) {
        this.pairAddress = pairAddress;
        this.balance0 = balance0;
        this.balance1 = balance1;
        this.reserve0 = reserve0;
        this.reserve1 = reserve1;
        this.tokenIn = tokenIn;
        this.amountIn = amountIn;
        this.unLiquidityAwardFee = unLiquidityAwardFee;
        this.tokenOut = tokenOut;
        this.amountOut = amountOut;
        this.to = to;
    }

    public StableSwapTradeBus getStableSwapTradeBus() {
        return stableSwapTradeBus;
    }

    public void setStableSwapTradeBus(StableSwapTradeBus stableSwapTradeBus) {
        this.stableSwapTradeBus = stableSwapTradeBus;
    }

    public byte[] getPairAddress() {
        return pairAddress;
    }

    public void setPairAddress(byte[] pairAddress) {
        this.pairAddress = pairAddress;
    }

    public BigInteger getBalance0() {
        return balance0;
    }

    public void setBalance0(BigInteger balance0) {
        this.balance0 = balance0;
    }

    public BigInteger getBalance1() {
        return balance1;
    }

    public void setBalance1(BigInteger balance1) {
        this.balance1 = balance1;
    }

    public BigInteger getReserve0() {
        return reserve0;
    }

    public void setReserve0(BigInteger reserve0) {
        this.reserve0 = reserve0;
    }

    public BigInteger getReserve1() {
        return reserve1;
    }

    public void setReserve1(BigInteger reserve1) {
        this.reserve1 = reserve1;
    }

    public NerveToken getTokenIn() {
        return tokenIn;
    }

    public void setTokenIn(NerveToken tokenIn) {
        this.tokenIn = tokenIn;
    }

    public BigInteger getAmountIn() {
        return amountIn;
    }

    public void setAmountIn(BigInteger amountIn) {
        this.amountIn = amountIn;
    }

    public BigInteger getUnLiquidityAwardFee() {
        return unLiquidityAwardFee;
    }

    public void setUnLiquidityAwardFee(BigInteger unLiquidityAwardFee) {
        this.unLiquidityAwardFee = unLiquidityAwardFee;
    }

    public NerveToken getTokenOut() {
        return tokenOut;
    }

    public void setTokenOut(NerveToken tokenOut) {
        this.tokenOut = tokenOut;
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
