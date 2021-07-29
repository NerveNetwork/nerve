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
package network.nerve.swap.help.impl;

import network.nerve.swap.help.IPair;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.model.po.SwapPairPO;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/12
 */
public abstract class AbstractPair implements IPair {

    private BigInteger[] reserves;

    protected abstract SwapPairDTO getSwapPairDTO();

    @Override
    public SwapPairPO getPair() {
        return getSwapPairDTO().getPo();
    }

    @Override
    public BigInteger[] getReserves() {
        if (reserves != null) {
            return reserves;
        }
        return (reserves = new BigInteger[]{getSwapPairDTO().getReserve0(), getSwapPairDTO().getReserve1()});
    }

    @Override
    public BigInteger totalSupply() {
        return getSwapPairDTO().getTotalLP();
    }

    @Override
    public long getBlockTimeLast() {
        return getSwapPairDTO().getBlockTimeLast();
    }

    @Override
    public long getBlockHeightLast() {
        return getSwapPairDTO().getBlockHeightLast();
    }

    @Override
    public void update(BigInteger liquidityChange, BigInteger balance0, BigInteger balance1,
                       BigInteger reserve0, BigInteger reserve1, long blockHeight, long blockTime) throws Exception {
        reserves = null;
        _update(liquidityChange, balance0, balance1, reserve0, reserve1, blockHeight, blockTime);
    }

    public abstract void _update(BigInteger liquidityChange, BigInteger balance0, BigInteger balance1,
                       BigInteger reserve0, BigInteger reserve1, long blockHeight, long blockTime) throws Exception;

    @Override
    public void rollback(BigInteger liquidityChange, BigInteger reserve0, BigInteger reserve1,
                         long blockHeight, long blockTime) throws Exception {
        reserves = null;
        _rollback(liquidityChange, reserve0, reserve1, blockHeight, blockTime);
    }

    public abstract void _rollback(BigInteger liquidityChange, BigInteger reserve0, BigInteger reserve1,
                         long blockHeight, long blockTime) throws Exception;
}
