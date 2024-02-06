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

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class AddLiquidityBus extends BaseBus {

    /**
     * Actual added assets
     */
    private BigInteger realAddAmount0;
    private BigInteger realAddAmount1;
    /**
     * ObtainedLPasset
     */
    private BigInteger liquidity;
    /**
     * Current pool balance（Before adding）
     */
    private BigInteger reserve0;
    private BigInteger reserve1;
    /**
     * Assets ultimately returned to users
     */
    private BigInteger refundAmount0;
    private BigInteger refundAmount1;

    public AddLiquidityBus() {
    }

    public AddLiquidityBus(BigInteger realAddAmount0, BigInteger realAddAmount1, BigInteger liquidity, BigInteger reserve0, BigInteger reserve1, BigInteger refundAmount0, BigInteger refundAmount1) {
        this.realAddAmount0 = realAddAmount0;
        this.realAddAmount1 = realAddAmount1;
        this.liquidity = liquidity;
        this.reserve0 = reserve0;
        this.reserve1 = reserve1;
        this.refundAmount0 = refundAmount0;
        this.refundAmount1 = refundAmount1;
    }

    public BigInteger getRealAddAmount0() {
        return realAddAmount0;
    }

    public void setRealAddAmount0(BigInteger realAddAmount0) {
        this.realAddAmount0 = realAddAmount0;
    }

    public BigInteger getRealAddAmount1() {
        return realAddAmount1;
    }

    public void setRealAddAmount1(BigInteger realAddAmount1) {
        this.realAddAmount1 = realAddAmount1;
    }

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
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

    public BigInteger getRefundAmount0() {
        return refundAmount0;
    }

    public void setRefundAmount0(BigInteger refundAmount0) {
        this.refundAmount0 = refundAmount0;
    }

    public BigInteger getRefundAmount1() {
        return refundAmount1;
    }

    public void setRefundAmount1(BigInteger refundAmount1) {
        this.refundAmount1 = refundAmount1;
    }
}
