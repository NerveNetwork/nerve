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
package network.nerve.swap.model.dto;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class RealAddLiquidityOrderDTO {

    private BigInteger realAmountA;
    private BigInteger realAmountB;
    private BigInteger reservesA;
    private BigInteger reservesB;
    private BigInteger refundA;
    private BigInteger refundB;
    private BigInteger liquidity;

    public RealAddLiquidityOrderDTO(BigInteger[] realAmount, BigInteger[] reserves, BigInteger[] refund, BigInteger liquidity) {
        this.realAmountA = realAmount[0];
        this.realAmountB = realAmount[1];
        this.reservesA = reserves[0];
        this.reservesB = reserves[1];
        this.refundA = refund[0];
        this.refundB = refund[1];
        this.liquidity = liquidity;
    }

    public BigInteger getRealAmountA() {
        return realAmountA;
    }

    public void setRealAmountA(BigInteger realAmountA) {
        this.realAmountA = realAmountA;
    }

    public BigInteger getRealAmountB() {
        return realAmountB;
    }

    public void setRealAmountB(BigInteger realAmountB) {
        this.realAmountB = realAmountB;
    }

    public BigInteger getReservesA() {
        return reservesA;
    }

    public void setReservesA(BigInteger reservesA) {
        this.reservesA = reservesA;
    }

    public BigInteger getReservesB() {
        return reservesB;
    }

    public void setReservesB(BigInteger reservesB) {
        this.reservesB = reservesB;
    }

    public BigInteger getRefundA() {
        return refundA;
    }

    public void setRefundA(BigInteger refundA) {
        this.refundA = refundA;
    }

    public BigInteger getRefundB() {
        return refundB;
    }

    public void setRefundB(BigInteger refundB) {
        this.refundB = refundB;
    }

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
    }
}
