package network.nerve.distribute;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * @Author: zhoulijun
 * @Time: 2020/7/2 14:10
 * @Description: Function Description
 */
public class Base {

    /**
     * Query asset address
     */
    static String QUERY_PS_URL = "http://seede.nuls.io:8003/jsonrpc";

    /**
     * queryPOCMEntrusted address
     */
    static String POCM_URL = "http://seede.nuls.io:8008/pocm/total/75320664-9638-47c9-af62-e334665d46f8";

    static File NRC20 = new File(System.getProperty("user.dir") + File.separator + "nrc20");

    static File NULS = new File(System.getProperty("user.dir") + File.separator + "nuls");

    static File POCM = new File(System.getProperty("user.dir") + File.separator + "pocm");

    /**
     * Excluded Address Directory
     */
    static Set<String> EXCLUSION = new HashSet<>();

    static Set<String> NRC20_EXCLUSION = new HashSet<>();

    /**
     * Airdrop threshold
     */
    static BigInteger MIN = BigInteger.valueOf(0L);

    static int CHAIN_ID = 1;

    static int NVT_CHAIN_ID = 9;

    static int NVT_ASSET_ID = 1;

    /**
     * nrc20 Map out the gold address
     */
    static String NVT_FROM_ADDRESS_FOR_NRC20 = "NULSd6HgeiNNZY6r3Rq66Gvrg69Bf1tbVrnVV";

    /**
     * nulsAirdrop withdrawal address
     */
    static String NVT_FROM_ADDRESS_FOR_NULS = "NULSd6HgeiNNZY6r3Rq66Gvrg69Bf1tbVrnVV";

    /**
     * NVT NRC20Contract address
     */
    static String NVT_NRC20 = "NULSd6Hgk44pRaP9EuBXsEyQy15yv6pfGGVUz";

    static {
        File conf = new File(System.getProperty("user.dir") + File.separator + "conf");
        if(conf.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(conf));
                String line = reader.readLine();
                while(line != null){
                    String key = line.split("=")[0];
                    String value = line.split("=")[1];
                    switch (key){
                        case "QUERY_PS_URL": QUERY_PS_URL=value;break;
                        case "POCM_URL" : POCM_URL=value;break;
                        case "CHAIN_ID" : CHAIN_ID=Integer.parseInt(value);break;
                        case "NVT_CHAIN_ID" : NVT_CHAIN_ID=Integer.parseInt(value);break;
                        case "NVT_ASSET_ID" : NVT_ASSET_ID=Integer.parseInt(value);break;
                        case "NVT_FROM_ADDRESS_FOR_NRC20" : NVT_FROM_ADDRESS_FOR_NRC20=value;break;
                        case "NVT_FROM_ADDRESS_FOR_NULS" : NVT_FROM_ADDRESS_FOR_NULS=value;break;
                        case "NVT_NRC20" : NVT_NRC20=value;break;
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //Lock address
        EXCLUSION.add("NULSd6HgWAwX7MbvcFSLYqMoyn88d5x3AcUww");
        EXCLUSION.add("NULSd6HgaXkFL7uYEhvC8zqjkYDNY5GQwrHos");
        EXCLUSION.add("NULSd6HgdZh1GWTN7a6P92zThPC77EuDPt3N2");
        EXCLUSION.add("NULSd6HgjVAEJi5ZZZs7hyrPt4xMv3uGTucGj");
        EXCLUSION.add("NULSd6HgfSqGPCh97oXmGDD9rQqodNZbLivzc");
        EXCLUSION.add("NULSd6HgiVWU4LMyQts6YXPFYsuQz8vDZss7J");
        EXCLUSION.add("NULSd6HgfHVsVx1DYv9RouqKUyKnx6qwDtQMr");
        EXCLUSION.add("NULSd6HgU1DEYKJMwMnvxQmwY9C4CAMFsEGov");
        EXCLUSION.add("NULSd6HgX6CRCL8PnDU2rWCDWAo45Bv3nnHJa");
        EXCLUSION.add("NULSd6HghrwiGsdkvPLk6h3AmgiEayK9XgPcD");
        EXCLUSION.add("NULSd6HgexB2yTuB3UEvo1z62V2XeHeksWnNf");
        EXCLUSION.add("NULSd6HgTwwcmqb1AAxXFMjHQtwx5r96bjarV");
        EXCLUSION.add("NULSd6HgZn6h34EdiPZW3uftAwktqaxEP7Jr1");
        EXCLUSION.add("NULSd6HgYq9bZFPPYAT2AGfskNd2xBocE9DDG");
        EXCLUSION.add("NULSd6HggLHaWAgr57BzAujWujK7cnkdfUpDv");
        EXCLUSION.add("NULSd6HgY2RHpL5qUzSZY3e5dDXTS7kKTwwkE");
        EXCLUSION.add("NULSd6HgZn9PCmNMYEVLktW12NojNymk6JoFD");
        EXCLUSION.add("NULSd6HgcjP6h7xbVgBzTXEWVKSZrAW5JMgEe");
        EXCLUSION.add("NULSd6HghNkGyb8XHcFrsLj9mdxyHB3dwqLYh");
        EXCLUSION.add("NULSd6HgXjnzZjPQUqhxdsDnGQxinDPJ1wyUU");
        EXCLUSION.add("NULSd6HghLcS3B6kAc9929wSSX6F2gxTsCfjF");
        EXCLUSION.add("NULSd6HgfUkXZduCFcWieYA1t9sieEnz7jjxL");
        EXCLUSION.add("NULSd6HgeLcgDeAU3fJpwwf196kQdtJ6WhAyP");
        EXCLUSION.add("NULSd6HgfTFrdqGCiB1SXZY5WZBjSUtgPLVCJ");
        EXCLUSION.add("NULSd6Hggmxe4LDEcuYkfUVxJboJFdMA9vS2m");
        EXCLUSION.add("NULSd6HgZfkCMz4oVMm2Dp9qPu53zL9XpMDGo");
        EXCLUSION.add("NULSd6HgiW5AGRUqshNb6TqEZraRg1QUMaQ44");
        EXCLUSION.add("NULSd6HgaFggHtSTyBA9H8uCJo14FEcivvXzf");
        EXCLUSION.add("NULSd6HgiUvUtuyo2AK8xaXKBR7byo5AuK34T");
        EXCLUSION.add("NULSd6HgiCnpKWd22i3FKqy4EZSRcvyDGKWNb");
        EXCLUSION.add("NULSd6HgbvthvopoJjwcs8Y9xQbMTTgwLFjoz");
        EXCLUSION.add("NULSd6HgeLxxtebLWDhTuULMHe1NFTPDo6swg");
        EXCLUSION.add("NULSd6Hgd9RipQqzW19cpymhbLW1Pjeq4WkJA");
        EXCLUSION.add("NULSd6HgaAqUVTRWGGsdz3AhzGKAshUbRQiKX");
        EXCLUSION.add("NULSd6Hgeujp7b2Eox8TZzBHTfQYERzaMmCot");
        EXCLUSION.add("NULSd6HgWQJbdYVjA3NUsrVP9g8hbvimJsKLr");
        EXCLUSION.add("NULSd6HgjYpHJXbZ5mpzvRgG923nssNPRP188");
        EXCLUSION.add("NULSd6HgWCZHGDuAHgRnJSjoLhXoDmPFv5Sbm");
        EXCLUSION.add("NULSd6HgaYBps2XoqtGTJBp95Q3ET6M87oD36");
        EXCLUSION.add("NULSd6HgX6tbavRbQBzYdiUAWG1BW8gzSmFq9");
        EXCLUSION.add("NULSd6HgVtCN6CpceEKhRexa1zCJw6sXdBtqM");
        EXCLUSION.add("NULSd6HghSFFmFHPECZXrqv1Jo1ZmUZBbwuFM");
        EXCLUSION.add("NULSd6HgdeVgSdz6653mWv6VdQeoUMkEuYGgi");
        EXCLUSION.add("NULSd6HggqMzNMQ1khS6sHxmPss79pCYPPEgS");
        EXCLUSION.add("NULSd6HgfqCPU5AdwCbxQK62hd4q4USP27nAY");
        EXCLUSION.add("NULSd6HgVwvoGs69TfvUNcPm5W4x9UYcW1QdR");
        EXCLUSION.add("NULSd6HgXjfFD5HLYLm7vcNxjCM9x3iVZSvWR");
        EXCLUSION.add("NULSd6HgfY5oVvjkCLz7vyBRdPQXppCcRxkCR");
        EXCLUSION.add("NULSd6Hgd9mLzsEjGHFQ8igyzUEiGxCzG7mwg");
        EXCLUSION.add("NULSd6Hgh3NYZAnsEqb83iYzzHrKxpZQNpbyS");
        EXCLUSION.add("NULSd6Hgib3NyJWaGcNeMUyrAwBRhNp2zXYzi");
        EXCLUSION.add("NULSd6HgaHjr4Z7GdnGc3vNLgFvXWP55hzNNX");
        EXCLUSION.add("NULSd6HgZdGvSnxFPLHrLzpVzda4gtWvHKibT");
        EXCLUSION.add("NULSd6HgZSxNKeFG7AwEL8MVDGZa1KwE39fPP");
        EXCLUSION.add("NULSd6HgYLpfpPuZV9umGvYjJJX7EkKsUVukS");
        EXCLUSION.add("NULSd6HgcCf8NfvCBjmdvDWbanWL5cFYiB8DM");
        EXCLUSION.add("NULSd6HgZwFFYvEKppidUp7irjQFKADspQXaX");
        EXCLUSION.add("NULSd6HgfNf8oRpKL8gsiFsR9ZwXVebHrcDAz");
        EXCLUSION.add("NULSd6HgfuE7eRNu4wJrQV6fXKupuioL9cKri");
        EXCLUSION.add("NULSd6HgWZmG6MSNighAjziatrvuhM1LpCF9o");
        EXCLUSION.add("NULSd6HgbjaWQZrq3CoDZEed9RwAU3zyTeUAP");
        EXCLUSION.add("NULSd6HgawRRELcuKfvf4TeyidGLqFsHo9hgM");
        EXCLUSION.add("NULSd6HgfYpp9kfgJU5z5q5D4BRHw4rrbGVH3");
        EXCLUSION.add("NULSd6HgjYXoMgRa89jDeHcEV7B5JCEhhWqm2");

        //Black hole address
        byte[] blackHolePublicKey= HexUtil.decode("000000000000000000000000000000000000000000000000000000000000000000");
        Address address = new Address(CHAIN_ID, "NULS", BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(blackHolePublicKey));
        String addressStr = AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix());
        EXCLUSION.add(addressStr);

        NRC20_EXCLUSION.add("NULSd6Hgih8M6TPcvkU9S6pB3JESifdiTCwiZ");
    }

    static HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(10000))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    static Map<String,Object> toMap(String method, List<Object> params){
        return Map.of("jsonrpc","2.0",
                "method" ,method,
                "id",1000,
                "params",
                params);
    }

    static Object post(String url, Map<String, Object> param) {
        try {
            URI uri = URI.create(url);
            String requestBody = JSONUtils.obj2json(param);
            Log.debug("url :{}, request boy : {}", uri, requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(10000))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            Map res = JSONUtils.json2pojo(body, Map.class);
            return res.get("result");
        } catch (JsonProcessingException e) {
            Log.error("Serialization error", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static Object get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(5000))
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String,Object> data = JSONUtils.json2map(response.body());
            return data.get("data");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
    }
}
