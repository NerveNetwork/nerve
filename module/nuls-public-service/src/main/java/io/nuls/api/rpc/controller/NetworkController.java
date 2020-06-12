package io.nuls.api.rpc.controller;

import io.nuls.api.model.rpc.RpcResult;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.core.annotation.Value;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-04 19:04
 * @Description: 功能描述
 */
@Controller
public class NetworkController {

    ThreadPoolExecutor executor = ThreadUtils.createThreadPool(5,100,new NulsThreadFactory("network_query_pool"));

    HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(100))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();


    @Value("network")
    String network;

    @RpcMethod("networkIps")
    public RpcResult getNetworkIps(List<Object> param){
        return RpcResult.success(network);
    }

    @RpcMethod("networkMonitor")
    public RpcResult networkMonitor(List<Object> param) throws InterruptedException {
        List<Map> nodes = new CopyOnWriteArrayList<>();
        Set<String> links = new CopyOnWriteArraySet();
        if(param.size() == 1){
            String ip = (String) param.get(0);
            Map<String,Object> info = getNodeInfo(ip);
            if(info.get("value") != null){
                nodes.add(info);
            }
            getNodes(ip).forEach(node->{
                links.add(info.get("name") + "-" +node.split(":")[0]);
            });

        }else {
            String[] ips = network.split(",");
            CountDownLatch latch = new CountDownLatch(ips.length);
            Arrays.stream(ips).forEach(ip->{
                executor.execute(()->{
                    String nodeIp = getIp(ip);
                    if(nodeIp == null){
                        nodes.add(Map.of("name",ip.split(":")[0],"value",-1));
                        latch.countDown();
                        return ;
                    }
                    Map<String,Object> info = getNodeInfo(ip);
                    if(info.get("value") != null){
                        nodes.add(info);
                        latch.countDown();
                        return ;
                    }
                    getNodes(ip).forEach(node->{
                        links.add(nodeIp.split(":")[0] + "-" +node.split(":")[0]);
                    });
                    nodes.add(info);
                    latch.countDown();
                });
            });
            latch.await(30, TimeUnit.SECONDS);
        }
        return RpcResult.success(Map.of("nodes",nodes,"links",links.stream().map(d->Map.of("source",d.split("-")[0],"target",d.split("-")[1])).collect(Collectors.toList())));
    }

    private Map<String,Object> getNodeInfo(String ip){
        String nodeIp = getIp(ip);
        if(nodeIp == null){
            return Map.of("name",ip.split(":")[0],"value",null);
        }
        Map<String,Object> info = getInfo(ip);
        info.put("name",nodeIp.split(":")[0]);
        info.put("value",info.get("localBestHeight"));
        info.put("version",getVersion(ip));
        return info;
    }

    private List<String> getNodes(String ip){
        Object res = post(ip + "/api/network/nodes");
        return (List<String>)res;
    }

    private Map<String,Object> getInfo(String ip){
        return (Map<String, Object>) post(ip + "/api/network/info");
    }

    private String getIp(String ip){
        return (String) post(ip + "/api/network/ip");
    }

    private String getVersion(String ip){
        Map<String,Object> res = (Map<String, Object>) post(ip + "/version");
        return (String) res.get("clientVersion");
    }

    private Object post(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + url))
                .timeout(Duration.ofMillis(100))
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String,Object> data = JSONUtils.json2map(response.body());
            return data.get("result");
        } catch (Exception e) {
            Log.error("连接{}失败",url);
            return null;
        }
    }

    public static void main(String[] args) {
        NetworkController networkController = new NetworkController();
        String ip = "http://localhost:17004";
        List<String> nodes = networkController.getNodes(ip);
        nodes.forEach(System.out::println);
        System.out.println(networkController.getInfo(ip));
        System.out.println(networkController.getIp(ip));
    }

}
