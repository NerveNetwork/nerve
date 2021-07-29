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

import network.nerve.swap.help.IPair;
import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class RemoveLiquidityBus extends BaseBus {

    /**
     * 赎回的资产
     */
    private BigInteger amount0;
    private BigInteger amount1;
    /**
     * 当前池子（赎回前）
     */
    private BigInteger reserve0;
    private BigInteger reserve1;
    /**
     * 销毁的LP资产
     */
    private BigInteger liquidity;
    private transient IPair pair;
    private transient NerveToken token0;
    private transient NerveToken token1;



    public RemoveLiquidityBus() {
    }

    public RemoveLiquidityBus(BigInteger amount0, BigInteger amount1, BigInteger reserve0, BigInteger reserve1, BigInteger liquidity) {
        this.amount0 = amount0;
        this.amount1 = amount1;
        this.reserve0 = reserve0;
        this.reserve1 = reserve1;
        this.liquidity = liquidity;
    }

    public RemoveLiquidityBus(BigInteger amount0, BigInteger amount1, BigInteger reserve0, BigInteger reserve1, BigInteger liquidity, IPair pair, NerveToken token0, NerveToken token1) {
        this.amount0 = amount0;
        this.amount1 = amount1;
        this.reserve0 = reserve0;
        this.reserve1 = reserve1;
        this.liquidity = liquidity;
        this.pair = pair;
        this.token0 = token0;
        this.token1 = token1;
    }

    public BigInteger getAmount0() {
        return amount0;
    }

    public void setAmount0(BigInteger amount0) {
        this.amount0 = amount0;
    }

    public BigInteger getAmount1() {
        return amount1;
    }

    public void setAmount1(BigInteger amount1) {
        this.amount1 = amount1;
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

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
    }

    public IPair getPair() {
        return pair;
    }

    public void setPair(IPair pair) {
        this.pair = pair;
    }

    public NerveToken getToken0() {
        return token0;
    }

    public void setToken0(NerveToken token0) {
        this.token0 = token0;
    }

    public NerveToken getToken1() {
        return token1;
    }

    public void setToken1(NerveToken token1) {
        this.token1 = token1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"amount0\":")
                .append(amount0);
        sb.append(",\"amount1\":")
                .append(amount1);
        sb.append(",\"reserve0\":")
                .append(reserve0);
        sb.append(",\"reserve1\":")
                .append(reserve1);
        sb.append(",\"liquidity\":")
                .append(liquidity);
        sb.append('}');
        return sb.toString();
    }
}
