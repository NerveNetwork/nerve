/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.rpc.querier.Querier;
import network.nerve.quotation.util.HttpRequestUtil;
import network.nerve.quotation.util.TimeUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Poloniex Quotes Latest
 *
 * @author: Loki
 * @date: 2019/12/9
 */
@Component
public class PoloniexQuerier implements Querier {
    /**
     * 获取交易对价格接口
     */
    private final String CMD = "/public?command=returnChartData&currencyPair=%s&start=%s&period=86400";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        String[] split = anchorToken.split("-");
        anchorToken = split[1].trim() + "_" + split[0].trim();
        String symbol = anchorToken.toUpperCase();
        long start = TimeUtil.getUTCZeroTimeMillisOfTheDay(NulsDateUtils.getCurrentTimeMillis()) / 1000;
        String url = baseurl + String.format(CMD, symbol, start);
        try {
            List<Map> data = HttpRequestUtil.httpRequestList(chain, url);
            if (null == data || data.isEmpty()) {
                return null;
            }
            Map resultMap = (Map) data.get(0);
            if (resultMap == null) {
                return null;
            }
            BigDecimal res = new BigDecimal(resultMap.get("close").toString());
            chain.getLogger().info("Poloniex 获取到交易对[{}]价格:{}", symbol.toUpperCase(), res);
            return res;
        } catch (Throwable e) {
            chain.getLogger().error("Poloniex, 调用接口 {}, anchorToken:{} 获取价格失败", url, anchorToken);
            return null;
        }
    }

}
