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

package network.nerve.converter.core.business.impl;

import network.nerve.converter.core.business.PriceService;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.rpc.call.QuotationCall;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author: Loki
 * @date: 2020/10/10
 */
public class PriceServiceImpl implements PriceService {


    @Override
    public BigDecimal getTickerPrice(BigDecimal priceFront, BigDecimal priceBehind) {
        if (null == priceFront || null == priceBehind) {
            return null;
        }
        return priceFront.divide(priceBehind, 8, RoundingMode.HALF_DOWN);
    }

    @Override
    public BigDecimal getTickerPrice(Chain chain, String oracleKeyFront, String oracleKeyBehind) {
        BigDecimal tokenFront = QuotationCall.getPriceByOracleKey(chain, oracleKeyFront);
        if (null == tokenFront) {
            return null;
        }
        BigDecimal tokenBehind = QuotationCall.getPriceByOracleKey(chain, oracleKeyBehind);
        if (null == tokenBehind) {
            return null;
        }
        return tokenFront.divide(tokenBehind, 8, RoundingMode.HALF_DOWN);
    }
}
