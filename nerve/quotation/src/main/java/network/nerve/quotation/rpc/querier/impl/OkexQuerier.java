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
 * @date: 2019/12/10
 */
@Component
public class OkexQuerier implements Querier {

    private String CMD_FORMAT = "/api/v5/market/ticker?instId=";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        String symbol = anchorToken.toUpperCase();
        String url = baseurl + CMD_FORMAT + symbol;

        try {
//            HttpClient client = HttpClient.newBuilder()
//                    .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
//                    .followRedirects(HttpClient.Redirect.NORMAL)
//                    .build();
//            HttpClient client = HttpClient.newHttpClient();
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
//                    .build();
//            HttpResponse<String> response =
//                    client.send(request, HttpResponse.BodyHandlers.ofString());
//            Map<String, Object> responseData = JSONUtils.json2map(response.body());
            Map<String, Object> data = HttpRequestUtil.httpRequest(chain, url);
            if (null == data) {
                return null;
            }
            List realData = (List) data.get("data");
            if(null==realData||realData.isEmpty()){
                return null;
            }
            Map<String, Object> obj = (Map<String, Object>) realData.get(0);
            BigDecimal res = new BigDecimal((String) obj.get("last"));
            chain.getLogger().info("Okex Obtaining transaction pairs[{}]price:{}", symbol, res);
            return res;
        } catch (Throwable e) {
            chain.getLogger().error("Okex, Calling interfaces {}, anchorToken:{} Failed to obtain price", url, anchorToken);
            return null;
        }
    }
}
