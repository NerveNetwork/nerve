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
package network.nerve.converter.heterogeneouschain.tbc.context;

import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContextNew;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.utils.LoggerUtil;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class TbcContext extends HtgContextNew implements Serializable {

    public TbcContext() {
        super.SET_VERSION((byte) 1);
    }

    @Override
    public int HTG_CHAIN_ID() {
        return 204;
    }

    @Override
    public BigInteger initialGas() {
        return BigInteger.ONE;
    }

    @Override
    public int getByzantineCount(Integer total) {
        int directorCount = total;
        int ByzantineRateCount = directorCount * TbcUtil.BYZANTINERATIO;
        int minPassCount = ByzantineRateCount / ConverterConstant.MAGIC_NUM_100;
        if (ByzantineRateCount % ConverterConstant.MAGIC_NUM_100 > 0) {
            minPassCount++;
        }
        return minPassCount;
    }
}
