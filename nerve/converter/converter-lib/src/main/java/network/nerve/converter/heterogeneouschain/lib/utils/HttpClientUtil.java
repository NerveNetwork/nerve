package network.nerve.converter.heterogeneouschain.lib.utils;

import io.nuls.core.parse.JSONUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpClientTool class
 *
 * @author SHANHY
 * @return
 * @create 2015year12month18day
 */
public class HttpClientUtil {

    private static final int timeOut = 10 * 1000;

    //private static CloseableHttpClient httpClient = null;

    private static final Map<String, CloseableHttpClient> httpClientMap = new ConcurrentHashMap<>();

    private final static Object syncLock = new Object();

    private static void config(HttpRequestBase httpRequestBase) {
        // set upHeaderetc.
        // httpRequestBase.setHeader("User-Agent", "Mozilla/5.0");
        // httpRequestBase
        // .setHeader("Accept",
        // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        // httpRequestBase.setHeader("Accept-Language",
        // "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");// "en-US,en;q=0.5");
        // httpRequestBase.setHeader("Accept-Charset",
        // "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");

        // Configure timeout settings for requests
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeOut)
                .setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
        httpRequestBase.setConfig(requestConfig);
    }

    /**
     * obtainHttpClientobject
     *
     * @return
     * @author SHANHY
     * @create 2015year12month18day
     */
    public static CloseableHttpClient getHttpClient(String url) {
        String hostname = url.split("/")[2];
        int port = 80;
        if (hostname.contains(":")) {
            String[] arr = hostname.split(":");
            hostname = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        String address = hostname + ":" + port;
        CloseableHttpClient httpClient = httpClientMap.get(address);
        if (httpClient == null) {
            synchronized (syncLock) {
                httpClient = httpClientMap.get(address);
                if (httpClient == null) {
                    httpClient = createHttpClient(200, 40, 100, hostname, port);
                    httpClientMap.put(address, httpClient);
                }
            }
        }
        return httpClient;
    }

    /**
     * ResethttpClientobject
     * @param url
     */
    public static void resetHttpClient(String url) {
        String hostname = url.split("/")[2];
        int port = 80;
        if (hostname.contains(":")) {
            String[] arr = hostname.split(":");
            hostname = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        String address = hostname + ":" + port;
        CloseableHttpClient client = httpClientMap.remove(address);
        try {
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getHttpClient(url);
    }

    /**
     * establishHttpClientobject
     *
     * @return
     * @author SHANHY
     * @create 2015year12month18day
     */
    public static CloseableHttpClient createHttpClient(int maxTotal,
                                                       int maxPerRoute, int maxRoute, String hostname, int port) {
        return createHttpClient(maxTotal, maxPerRoute, maxRoute, hostname, port, null);
    }

    public static CloseableHttpClient createHttpClient(int maxTotal,
                                                       int maxPerRoute, int maxRoute, String hostname, int port, SSLConnectionSocketFactory socketFactory) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = socketFactory;
        if (sslsf == null) {
            sslsf = SSLConnectionSocketFactory.getSocketFactory();
        }
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory>create().register("http", plainsf)
                .register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
                registry);
        // Increase the maximum number of connections
        cm.setMaxTotal(maxTotal);
        // Add connections to each routing base
        cm.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost = new HttpHost(hostname, port);
        // Increase the maximum number of connections to the target host
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

        // Request retry processing
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,
                                        int executionCount, HttpContext context) {
                if (executionCount >= 3) {// If it has already been retried3Next time, give up
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// If the server loses the connection, then try again
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// Do not retrySSLHandshake abnormality
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// overtime
                    return false;
                }
                if (exception instanceof UnknownHostException) {// Target server unreachable
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// connection not permitted
                    return false;
                }
                if (exception instanceof SSLException) {// SSLHandshake abnormality
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext
                        .adapt(context);
                HttpRequest request = clientContext.getRequest();
                // If the request is idempotent, try again
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setRetryHandler(httpRequestRetryHandler).build();

        return httpClient;
    }

    private static void setPostParams(HttpPost httpPost, Map<String, Object> params) throws Exception {
        //Set request parameters
        String json = JSONUtils.obj2json(params);
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");//sendjsonNeed to set upcontentType
        httpPost.setEntity(entity);
    }

    private static void setPostParams(HttpPost httpPost, List<Map> params) throws Exception {
        //Set request parameters
        String json = JSONUtils.obj2json(params);
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");//sendjsonNeed to set upcontentType
        httpPost.setEntity(entity);
    }

    private static void setPutParams(HttpPut httpPut, Map<String, Object> params) throws Exception {
        //Set request parameters
        String json = JSONUtils.obj2json(params);
        StringEntity entity = new StringEntity(json, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");//sendjsonNeed to set upcontentType
        httpPut.setEntity(entity);
    }

    /**
     * POSTrequestURLGet content
     *
     * @param url
     * @return
     * @throws IOException
     * @author SHANHY
     * @create 2015year12month18day
     */
    public static String post(String url, List<Map> params) throws Exception {
        return post(url, params, null);
    }

    public static String post(String url, List<Map> params, List<BasicHeader> headers) throws Exception {
        return post_(url, params, headers);
    }

    public static String post(String url, Map<String, Object> params) throws Exception {
        return post(url, params, null);
    }

    public static String post(String url, Map<String, Object> params, List<BasicHeader> headers) throws Exception {
        return post_(url, params, headers);
    }

    public static String post_(String url, Object params, List<BasicHeader> headers) throws Exception {
        CloseableHttpResponse response = null;
        try {
            HttpPost httppost = new HttpPost(url);
            config(httppost);
            if(headers != null && !headers.isEmpty()) {
                headers.stream().forEach(header -> httppost.addHeader(header));
            }
            if (params instanceof Map) {
                setPostParams(httppost, (Map) params);
            } else if (params instanceof List) {
                setPostParams(httppost, (List) params);
            } else {
                throw new RuntimeException("unkown params type");
            }
            CloseableHttpClient httpClient = getHttpClient(url);
            response = httpClient.execute(httppost,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();

            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * PUTrequestURLGet content
     *
     * @param url
     * @return
     * @throws IOException
     * @author SHANHY
     * @create 2015year12month18day
     */
    public static String put(String url, Map<String, Object> params) throws Exception {
        CloseableHttpResponse response = null;
        try {
            HttpPut httpPut = new HttpPut(url);
            config(httpPut);
            setPutParams(httpPut, params);
            response = getHttpClient(url).execute(httpPut,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();

            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * GETrequestURLGet content
     *
     * @param url
     * @return
     * @author SHANHY
     * @create 2015year12month18day
     */
    public static String get(String url) throws Exception {
        return get(url, null, null);
    }

    public static String get(String url, List<BasicHeader> headers) throws Exception {
        return get(url, null, headers);
    }

    public static String get(String url, Map<String, Object> params) throws Exception {
        return get(url, params, null);
    }

    public static String get(String url, Map<String, Object> params, List<BasicHeader> headers) throws Exception {
        StringBuffer buffer;
        if (null != params && !params.isEmpty()) {
            //ergodicmap
            buffer = new StringBuffer("");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                buffer.append("&" + entry.getKey() + "=" + entry.getValue());
            }
            url = url + buffer.toString();
        }
        HttpGet httpGet = new HttpGet(url);
        config(httpGet);
        if(headers != null && !headers.isEmpty()) {
            headers.stream().forEach(header -> httpGet.addHeader(header));
        }
        CloseableHttpResponse response = null;
        try {
            response = getHttpClient(url).execute(httpGet,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
