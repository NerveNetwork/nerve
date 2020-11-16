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

package network.nerve.converter.core.business;

import network.nerve.converter.model.bo.Chain;

import java.math.BigDecimal;

/**
 * 获取/计算价格
 * @author: Loki
 * @date: 2020/10/10
 */
public interface PriceService {

    /**
     * 计算两个token报价的兑换价格
     * 例如通过两个token各自对USDT的价格 来计算这两个token相互兑换的价格
     * @param priceFront
     * @param priceBehind
     * @return
     */
    BigDecimal getTickerPrice(BigDecimal priceFront, BigDecimal priceBehind);

    /**
     * 通过两个oracleKey 来计算这个两个token的兑换价格
     * @param oracleKeyFront
     * @param oracleKeyBehind
     * @return
     */
    BigDecimal getTickerPrice(Chain chain, String oracleKeyFront, String oracleKeyBehind);
}
