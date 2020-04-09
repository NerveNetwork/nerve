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

package nerve.network.quotation.rpc.querier.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.parse.JSONUtils;
import nerve.network.quotation.model.bo.Chain;
import nerve.network.quotation.rpc.querier.Querier;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Huobi Quotes Latest
 *
 * @author: Chino
 * @date: 2020/03/10
 */
@Component
public class HuobiQuerier implements Querier {

    /**
     * 获取交易对价格接口
     */
    private final String CMD = "/market/tickers";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        chain.getLogger().debug("HuobiQuerier, 取价anchorToken:{}", anchorToken);
        String symbol = (anchorToken.replace("-","")).toLowerCase();
        String url = baseurl + CMD;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(5000))
                    .build();
            HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200){
                chain.getLogger().error("调用{}接口, Huobi获取价格失败, statusCode:{}", url, response.statusCode());
                return null;
            }
            Map<String, Object> responseDate = JSONUtils.json2map(response.body());
            List<Map<String, Object>> dataList = (List) responseDate.get("data");
            for (Map<String, Object> map : dataList) {
                if (symbol.equals(map.get("symbol").toString())) {
                    BigDecimal res = new BigDecimal(String.valueOf(map.get("close")));
                    chain.getLogger().debug("Huobi获取到交易对[{}]价格:{}", symbol.toUpperCase(), res);
                    return res;
                }
            }
            chain.getLogger().error("调用{}接口Huobi获取价格失败, 没有找到交易对[{}]价格", url, symbol.toUpperCase());
            return null;
        } catch (Throwable e) {
            chain.getLogger().error("调用{}接口Huobi获取价格失败", url);
            chain.getLogger().error(e);
            return null;
        }
    }
}
