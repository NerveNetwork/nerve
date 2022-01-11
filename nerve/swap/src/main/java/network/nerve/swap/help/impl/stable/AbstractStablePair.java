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
package network.nerve.swap.help.impl.stable;

import network.nerve.swap.help.IStablePair;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2021/4/12
 */
public abstract class AbstractStablePair implements IStablePair {

    protected abstract StableSwapPairDTO getStableSwapPairDTO();

    @Override
    public StableSwapPairPo getPair() {
        return getStableSwapPairDTO().getPo();
    }

    @Override
    public BigInteger totalSupply() {
        return getStableSwapPairDTO().getTotalLP();
    }

    @Override
    public long getBlockTimeLast() {
        return getStableSwapPairDTO().getBlockTimeLast();
    }

    @Override
    public long getBlockHeightLast() {
        return getStableSwapPairDTO().getBlockHeightLast();
    }

    @Override
    public BigInteger[] getBalances() {
        return getStableSwapPairDTO().getBalances();
    }

    @Override
    public void update(BigInteger liquidityChange, BigInteger[] changeBalances, BigInteger[] balances, long blockHeight, long blockTime) throws Exception {
        int length = balances.length;
        BigInteger[] realAmounts = changeBalances;
        BigInteger[] newBalances = new BigInteger[length];
        for (int i = 0; i < length; i++) {
            newBalances[i] = balances[i].add(realAmounts[i]);
        }
        _update(liquidityChange, newBalances, blockHeight, blockTime);
    }

    public abstract void _update(BigInteger liquidityChange, BigInteger[] newBalances, long blockHeight, long blockTime) throws Exception;

    @Override
    public void rollback(BigInteger liquidityChange, BigInteger[] balances, long blockHeight, long blockTime) throws Exception {
        _rollback(liquidityChange, balances, blockHeight, blockTime);
    }

    public abstract void _rollback(BigInteger liquidityChange, BigInteger[] balances, long blockHeight, long blockTime) throws Exception;
}
