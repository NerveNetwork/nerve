package network.nerve.quotation.util;

import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.quotation.model.bo.Chain;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static network.nerve.quotation.constant.QuotationConstant.TIMEOUT_MILLIS;

/**
 * @author: Loki
 * @date: 2020/08/11
 */
public class HttpRequestUtil {

    public static Map<String, Object> httpRequest(Chain chain, String url) {
        try {
            String dataStr = _httpRequest(chain, url);
            if (StringUtils.isBlank(dataStr)) {
                return null;
            }
            Map<String, Object> data = JSONUtils.jsonToMap(dataStr);
            return data;
        } catch (Exception e){
            chain.getLogger().error("Calling interfaces:{} abnormal, {}", url, e.getMessage());
            return null;
        }
    }

    public static List<Map> httpRequestList(Chain chain, String url) {
        try {
            String dataStr = _httpRequest(chain, url);
            if (StringUtils.isBlank(dataStr)) {
                return null;
            }
            List<Map> dataList = JSONUtils.json2pojo(dataStr, List.class);
            return dataList;
        } catch (Exception e){
            chain.getLogger().error("Calling interfaces:{} abnormal, {}", url, e.getMessage());
            return null;
        }
    }

    public static String _httpRequest(Chain chain, String url) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        HttpGet httpGet = new HttpGet(url);
//        HttpHost proxy = new HttpHost("127.0.0.1", 1080);
//        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT_MILLIS).setProxy(proxy)
//                .setSocketTimeout(TIMEOUT_MILLIS).setConnectTimeout(TIMEOUT_MILLIS).build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT_MILLIS)
                .setSocketTimeout(TIMEOUT_MILLIS).setConnectTimeout(TIMEOUT_MILLIS).build();
        httpGet.setConfig(requestConfig);

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String dataStr = EntityUtils.toString(entity);
                return dataStr;
            }
            chain.getLogger().error("Calling interfaces:{} abnormal, StatusCode:{}", url, response.getStatusLine().getStatusCode());
            return null;
        } catch (IOException e) {
            chain.getLogger().error("Calling interfaces:{} abnormal, {}", url, e.getMessage());
            return null;
        } catch (Exception e){
            chain.getLogger().error("Calling interfaces:{} abnormal, {}", url, e.getMessage());
            return null;
        }
    }

}
