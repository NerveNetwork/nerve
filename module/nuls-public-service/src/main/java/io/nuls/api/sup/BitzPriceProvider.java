package io.nuls.api.sup;

import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/10 17:49
 * @Description: 功能描述
 */
@Component
public class BitzPriceProvider extends BasePriceProvider{

    @Override
    public BigDecimal queryPrice(String symbol) {
        String url = this.url+"Market/ticker?symbol=" + symbol.toLowerCase()+ "_usdt";
        try {
            Map<String,Object> data = httpRequest(url);
            String statusStr = data.get("status").toString();
            if(statusStr == null){
                LoggerUtil.PRICE_PROVIDER_LOG.error("biz获取"+symbol+"价格失败");
                return BigDecimal.ZERO;
            }
            int status = Integer.parseInt(statusStr);
            if(status != 200){
                LoggerUtil.PRICE_PROVIDER_LOG.error("biz获取"+symbol+"价格失败,status:" + status);
                return BigDecimal.ZERO;
            }
            Map<String,Object> resData = (Map<String, Object>) data.get("data");
            BigDecimal res = new BigDecimal((String)resData.get("now"));
            LoggerUtil.PRICE_PROVIDER_LOG.debug("获取到当前{}兑USDT的价格:{}",symbol,res);
            return res;
        } catch (Exception e) {
            LoggerUtil.PRICE_PROVIDER_LOG.error("调用{}接口获取{}价格失败",url,symbol,e);
            return BigDecimal.ZERO;
        }
    }

}
