/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.heterogeneouschain.zk.context;

import network.nerve.converter.heterogeneouschain.lib.context.HtgContextNew;

import java.io.Serializable;
import java.math.BigInteger;

import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.GWEI_DOT_3;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public class ZkContext extends HtgContextNew implements Serializable {
    private final BigInteger GAS_LIMIT_OF_WITHDRAW = BigInteger.valueOf(200_0000L);
    private final BigInteger GAS_LIMIT_OF_CHANGE = BigInteger.valueOf(300_0000L);
    private final BigInteger GAS_LIMIT_OF_MAIN_ASSET = BigInteger.valueOf(100_0000L);
    private final BigInteger GAS_LIMIT_OF_ERC20 = BigInteger.valueOf(150_0000L);

    private final BigInteger HTG_ESTIMATE_GAS = BigInteger.valueOf(5000_0000L);
    private final BigInteger BASE_GAS_LIMIT = BigInteger.valueOf(50_0000L);

    public ZkContext() {
        super.SET_VERSION((byte) 3);
    }

    @Override
    public int HTG_CHAIN_ID() {
        return 123;
    }

    @Override
    public BigInteger initialGas() {
        return GWEI_DOT_3;
    }

    @Override
    public BigInteger GET_GAS_LIMIT_OF_WITHDRAW() {
        return GAS_LIMIT_OF_WITHDRAW;
    }

    @Override
    public BigInteger GET_GAS_LIMIT_OF_CHANGE() {
        return GAS_LIMIT_OF_CHANGE;
    }

    @Override
    public BigInteger GET_GAS_LIMIT_OF_MAIN_ASSET() {
        return GAS_LIMIT_OF_MAIN_ASSET;
    }

    @Override
    public BigInteger GET_GAS_LIMIT_OF_ERC20() {
        return GAS_LIMIT_OF_ERC20;
    }

    @Override
    public BigInteger GET_HTG_ESTIMATE_GAS() {
        return HTG_ESTIMATE_GAS;
    }

    @Override
    public BigInteger GET_BASE_GAS_LIMIT() {
        return BASE_GAS_LIMIT;
    }
}
