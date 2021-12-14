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

    private final String CMD = "/open/api/v2/market/ticker?symbol=";

    @Override
    public BigDecimal tickerPrice(Chain chain, String baseurl, String anchorToken) {
        String symbol = (anchorToken.replace("-", "_")).toUpperCase();
        String url = baseurl + CMD + symbol;
        try {
            Map<String, Object> map = HttpRequestUtil.httpRequest(chain, url);
            if (null == map) {
                return null;
            }
            List<Object> list = (List<Object>) map.get("data");
            if(list.isEmpty()){
                return null;
            }
            Map<String,Object> data = (Map<String, Object>) list.get(0);
            chain.getLogger().info("MXC 获取到交易对[{}]价格:{}", symbol.toUpperCase(), data.get("last"));
            return new BigDecimal(data.get("last").toString());
        } catch (Throwable e) {
            chain.getLogger().error("MXC, 调用接口 {}, anchorToken:{} 获取价格失败", url, anchorToken);
            return null;
        }
    }
}
