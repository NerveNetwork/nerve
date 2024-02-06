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

package network.nerve.quotation.rpc.querier.impl;

import io.nuls.core.core.annotation.Component;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.rpc.querier.Querier;
import network.nerve.quotation.util.HttpRequestUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020/7/31
 */
@Component
public class MxcQuerier implements Querier {

    private final String CMD = "/api/v3/avgPrice?symbol=";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        String symbol = (anchorToken.replace("-", "")).toUpperCase();
        String url = baseurl + CMD + symbol;
        try {
            Map<String, Object> map = HttpRequestUtil.httpRequest(chain, url);
            if (null == map) {
                return null;
            }
            String price = (String) map.get("price");

            chain.getLogger().info("MXC Obtaining transaction pairs[{}]price:{}", symbol.toUpperCase(), price);
            return new BigDecimal(price);
        } catch (Throwable e) {
            chain.getLogger().error("MXC, Calling interfaces {}, anchorToken:{} Failed to obtain price", url, anchorToken);
            return null;
        }
    }
}
