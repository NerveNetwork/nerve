package io.nuls.api.sup;

import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/10 17:49
 * @Description: 功能描述
 */
@Component
public class AexPriceProvider extends BasePriceProvider {

    @Override
    public BigDecimal queryPrice(String symbol) {
        String param = "?mk_type=USDT&coinname=" + symbol.toUpperCase();
        String url = this.url + param;
        try {
            Map<String, Object> res = httpRequest(url);
            if (null == res) {
                return null;
            }
            Long code = Long.parseLong(res.get("code").toString());
            if (code != 20000L) {
                LoggerUtil.PRICE_PROVIDER_LOG.error("aex获取"+symbol+"价格失败,code:" + code);
                return null;
            }
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            Map<String, Object> ticker = (Map<String, Object>) data.get("ticker");
            return new BigDecimal(ticker.get("last").toString());

        } catch (Throwable e) {
            LoggerUtil.PRICE_PROVIDER_LOG.error("调用{}接口获取{}价格失败",url,symbol,e);
            return null;
        }

    }

}
