package io.nuls.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.api.ApiContext;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Value;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/3 20:32
 * @Description: 功能描述
 */
@Component
public class NulsApi {

    @Value("nulsApiUrl")
    String nulsApiUrl;

    static HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public NulsApi(){};

    public RpcResult post(String method, Object... param) {
        try {
            URI uri = URI.create(nulsApiUrl);
            String requestBody = JSONUtils.obj2json(Map.of("jsonrpc","2.0","id",1,"params",param,"method",method));
            Log.info("call dex api url :{}, request boy : {}", uri, requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(5000))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            Log.debug("result : {} ", body);
            RpcResult res = JSONUtils.json2pojo(body, RpcResult.class);
            return res;
        } catch (JsonProcessingException e) {
            Log.error("序列化错误", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询NULS网络指定地址的余额
     * @param address
     * @param assetChainId
     * @param assetId
     * @return
     */
    public BigInteger getBalanceByNulsNetwork(String address,int assetChainId,int assetId){
        Map<String,Object> res = (Map<String, Object>) post("getAccountBalance", ApiContext.mainChainId,assetChainId,assetId,address).getResult();
        if(res != null){
            return new BigInteger(res.get("totalBalance").toString());
        }
        return BigInteger.ZERO;
    }

    public static void main(String[] args) {
        ApiContext.mainChainId = 1;
        NulsApi nulsApi = new NulsApi();
        nulsApi.nulsApiUrl = "https://api.nuls.io/jsonrpc";
        BigInteger balance = nulsApi.getBalanceByNulsNetwork("NULSd6HgjNHAs4W6RL6wy9XRaLtNNd3SyJ7Er",9,1);
        Log.info("{}",balance);
    }

}
