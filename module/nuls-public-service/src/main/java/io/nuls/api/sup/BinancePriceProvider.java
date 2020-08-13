package io.nuls.api.sup;

import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import org.bson.types.Symbol;
import org.glassfish.grizzly.utils.ArraySet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 20:17
 * @Description: 功能描述
 */
@Component
public class BinancePriceProvider extends BasePriceProvider implements PriceProvider, InitializingBean {

    private int initTryCount = 0 ;

    Set<String> supportAssetList = new HashSet<>();

    @Override
    public BigDecimal queryPrice(String symbol) {
        if(!supportAssetList.contains(symbol)){
            return BigDecimal.ZERO;
        }
        try {
            String url  = this.url+"api/v3/ticker/price?symbol=" + symbol+ "USDT";
            Map<String, Object> data = httpRequest(url);
            BigDecimal res = new BigDecimal((String)data.get("price"));
            LoggerUtil.PRICE_PROVIDER_LOG.debug("获取到当前{}兑USDT的价格:{}",symbol,res);
            return res;
        } catch (Exception e) {
            LoggerUtil.PRICE_PROVIDER_LOG.error("调用{}接口获取{}价格失败",url,symbol,e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void afterPropertiesSet() throws NulsException {
        String url = this.url+"api/v3/exchangeInfo";
        try {
            Map<String, Object> data = httpRequest(url);
            List<Map<String, Object>> res = (List<Map<String, Object>>) data.get("symbols");
            res.forEach(d -> {
                Map<String, Object> item = d;
                if (!"USDT".equals(d.get("quoteAsset"))) {
                    return;
                }
                supportAssetList.add(d.get("baseAsset").toString());
            });
        } catch (Exception e) {
            if(initTryCount > 3){
                Log.error("获取binance的交易对基础信息失败",e);
                System.exit(0);
            }
            initTryCount++;
            afterPropertiesSet();
        }
    }




}
