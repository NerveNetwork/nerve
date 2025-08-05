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

import static network.nerve.quotation.constant.QuotationConstant.ANCHOR_TOKEN_AKC;

/**
 * @author: Loki
 * @date: 2019/12/10
 */
@Component
public class Aiby2Querier implements Querier {

    private String CMD_FORMAT = "/v1/quotation/allspotticker";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        if (!ANCHOR_TOKEN_AKC.equals(anchorToken)) {
            return null;
        }
        String[] split = anchorToken.split("-");
        anchorToken = split[0].trim() + "_" + split[1].trim();
        String symbol = anchorToken.toLowerCase();
        String url = baseurl + CMD_FORMAT;

        try {
            Map<String, Object> data = HttpRequestUtil.httpRequest(chain, url);
            if (null == data) {
                return null;
            }
            List<Map> list = (List<Map>) data.get("ticker");
            if (list == null) {
                return null;
            }
            String last_price = null;
            for (Map map : list) {
                String _symbol = (String) map.get("symbol");
                if (_symbol == null) {
                    continue;
                }
                if (_symbol.equalsIgnoreCase(symbol)) {
                    last_price = (String) map.get("last");
                    break;
                }
            }

            if (null == last_price) {
                return null;
            }
            BigDecimal res = new BigDecimal(last_price);
            chain.getLogger().info("Aiby2 Obtaining transaction pairs [{}] price: {}", symbol, res);
            return res;
        } catch (Throwable e) {
            chain.getLogger().error("Aiby2, Calling interfaces {}, anchorToken: {} Failed to obtain price", url, anchorToken);
            return null;
        }
    }
}
