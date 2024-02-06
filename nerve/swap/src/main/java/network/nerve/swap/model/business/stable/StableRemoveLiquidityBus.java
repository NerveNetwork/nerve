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

import io.nuls.base.basic.AddressTool;
import network.nerve.swap.model.business.BaseBus;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: PierreLuo
 * @date: 2021/4/1
 */
public class StableRemoveLiquidityBus extends BaseBus {

    /**
     * Redemption of assets
     */
    private BigInteger[] amounts;
    /**
     * Current Pool（Before redemption）
     */
    private BigInteger[] balances;
    /**
     * DestroyedLPasset
     */
    private BigInteger liquidity;
    /**
     * Transaction to address
     */
    private byte[] pairAddress;
    /**
     * Asset receiving address
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"amounts\":")
                .append(Arrays.toString(amounts));
        sb.append(",\"balances\":")
                .append(Arrays.toString(balances));
        sb.append(",\"liquidity\":")
                .append(liquidity);
        sb.append(",\"pairAddress\":")
                .append("\"").append(pairAddress == null ? "" : AddressTool.getStringAddressByBytes(pairAddress)).append("\"");
        sb.append(",\"to\":")
                .append("\"").append(to == null ? "" : AddressTool.getStringAddressByBytes(to)).append("\"");
        sb.append('}');
        return sb.toString();
    }
}
