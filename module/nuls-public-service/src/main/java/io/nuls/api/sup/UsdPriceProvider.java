package io.nuls.api.sup;

import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.parse.JSONUtils;
import org.checkerframework.checker.units.qual.A;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 17:16
 * @Description: 获取美元兑USDT的汇率
 */
@Component
public class UsdPriceProvider implements PriceProvider {

    public static final int USD_SCALE = 4;

    HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static final String BTC ="BTC";

    @Autowired
    BinancePriceProvider binancePriceProvider;

    private String bitfinexUrl;

    @Override
    public void setURL(String url) {
        this.bitfinexUrl = url + "/v1/pubticker/btcusd";
    }

    @Override
    public BigDecimal queryPrice(String symbol) {
        BigDecimal btcUsdtPrice = binancePriceProvider.queryPrice(BTC);
        if(btcUsdtPrice == null || btcUsdtPrice.equals(BigDecimal.ZERO)){
            return BigDecimal.ONE;
        }
        BigDecimal btcUsdPrice;
        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(bitfinexUrl))
        .timeout(Duration.ofMillis(5000))
        .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String,Object> data = JSONUtils.json2map(response.body());
            btcUsdPrice = new BigDecimal((String)data.get("last_price"));
            LoggerUtil.PRICE_PROVIDER_LOG.debug("获取到当前BTC兑USD的价格:{}",btcUsdPrice);
            return btcUsdPrice.divide(btcUsdtPrice,USD_SCALE, RoundingMode.HALF_DOWN);
        } catch (Exception e) {
            LoggerUtil.PRICE_PROVIDER_LOG.error("调用{}接口获取价格失败",bitfinexUrl,e);
            return BigDecimal.ONE;
        }
    }
}
