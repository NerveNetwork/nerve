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
import io.nuls.core.parse.JSONUtils;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.rpc.querier.Querier;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Binance Quotes Latest
 *
 * @author: Loki
 * @date: 2019/12/9
 */
@Component
public class BinanceQuerier implements Querier {
    /** 获取交易对价格接口 */
    private final String CMD = "/api/v3/ticker/price?symbol=";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        chain.getLogger().debug("BinanceQuerier, 取价anchorToken:{}", anchorToken);
        String symbol = (anchorToken.replace("-","")).toUpperCase();
        String url = baseurl + CMD + symbol;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(5000))
                .build();
            HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200){
                chain.getLogger().error("调用{}接口, Binance获取价格失败, statusCode:{}", url, response.statusCode());
                return null;
            }
            Map<String,Object> data = JSONUtils.json2map(response.body());
            BigDecimal res = new BigDecimal((String)data.get("price"));
            chain.getLogger().debug("Binance获取到交易对[{}]价格:{}", symbol, res);
            return res;
        } catch (Throwable e) {
            chain.getLogger().error("调用{}接口, Binance获取价格失败", url);
            chain.getLogger().error(e);
            return null;
        }
    }
}
