package io.nuls.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.Address;
import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.BlockSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.v2.model.dto.CoinFromDto;
import io.nuls.v2.model.dto.CoinToDto;
import io.nuls.v2.model.dto.TransferDto;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-11 18:30
 * @Description: dex Tool class
 */
public class BlockSignTest {

    String dexApiUrl;

    ECKey ecKey;

    static HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    static InetSocketAddress addr = new InetSocketAddress("localhost", 1087);
    static HttpClient proxyClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .proxy(ProxySelector.of(addr))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    static boolean proxy = false;
    static HttpClient client() {
        if (proxy) {
            return proxyClient;
        } else {
            return client;
        }
    }

    public BlockSignTest(String dexApiUrl, String priKey) {
        Objects.requireNonNull(dexApiUrl, "dexApiUrl can't be null");
        this.ecKey = ECKey.fromPrivate(RPCUtil.decode(priKey));
        this.dexApiUrl = dexApiUrl.lastIndexOf("/") == dexApiUrl.length() - 1 ? dexApiUrl : dexApiUrl + "/";
    }

    /**
     * Build a tool class instance
     * Can call transaction signing and broadcasting transaction interfaces
     *
     * @param dexApiUrl dex apiInterface Access Address
     * @param priKey    Private key
     */
    public static BlockSignTest getInstance(String dexApiUrl, String priKey) {
        BlockSignTest dexUtils = new BlockSignTest(dexApiUrl, priKey);
        return dexUtils;
    }

    /**
     * Build a tool class instance
     * Can only assemble transactions, cannot sign or broadcast
     *
     * @param dexApiUrl dex apiInterface Access Address
     * @return
     */
    public static BlockSignTest getInstance(String dexApiUrl) {
        BlockSignTest dexUtils = new BlockSignTest(dexApiUrl, null);
        return dexUtils;
    }

    public String signTx(String txHex) {
        if (ecKey == null) {
            throw new IllegalArgumentException("need private key for getInstance");
        }
        try {
            byte[] digest = RPCUtil.decode(txHex);
            Transaction tx = new Transaction();
            tx.parse(new NulsByteBuffer(digest));
            //Decoding data into byte arrays
            byte[] signBytes = SignatureUtil.signDigest(tx.getHash().getBytes(), ecKey).serialize();
            P2PHKSignature signature = new P2PHKSignature(signBytes, ecKey.getPubKey());
            tx.setTransactionSignature(signature.serialize());
            txHex = RPCUtil.encode(tx.serialize());
            return txHex;
        } catch (IOException e) {
            Log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (NulsException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    public Object signAndBroadcast(String txHex) {
        if (ecKey == null) {
            throw new IllegalArgumentException("need private key for getInstance");
        }
        txHex = signTx(txHex);
        return post(dexApiUrl, Map.of("jsonrpc", "2.0",
                "method", "broadcastTx",
                "id", 1000,
                "params",
                List.of(1,
                        txHex)));
    }

    public static Object post(String url, Map<String, Object> param) {
        try {
            URI uri = URI.create(url);
            String requestBody = JSONUtils.obj2json(param);
            //Log.debug("call dex api url :{}, request boy : {}", uri, requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(5000))
                    .build();

            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            //Log.debug("result : {} ", body);
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

    public static void blockSignerPubTest() throws NulsException {
        //        HttpHost proxy = new HttpHost("127.0.0.1", 1080);
        //        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT_MILLIS).setProxy(proxy)
        //                .setSocketTimeout(TIMEOUT_MILLIS).setConnectTimeout(TIMEOUT_MILLIS).build();
        String url = "https://api.nerve.network/jsonrpc";
        //long start = 55029898;
        //long end = 55029915;
        long start = 59047530;
        long end = 59047548;
        for (long i = start; i <= end; i++) {
            String post = (String) post(url + "",
                    Map.of("jsonrpc", "2.0",
                            "method", "getBlockSerializationByHeight",
                            "id", 1000,
                            "params",
                            List.of(9, i)));
            byte[] blockBytes = HexUtil.decode(post);
            Block block = new Block();
            block.parse(blockBytes, 0);
            BlockHeader header = block.getHeader();
            BlockSignature signature = header.getBlockSignature();
            io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPublicOnly(signature.getPublicKey());
            Address nerveAddress = new Address(9, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
            System.out.println(String.format("%s - %s", HexUtil.encode(signature.getPublicKey()), nerveAddress));
        }
    }

    public static void takePubFromSignTest() throws Exception {
        byte[] blockBytes = HexUtil.decode("a6c77db4b03835e2e1432163624e53f97b718e152d69e335395a12578b583185336da6e76956fec294651a9626410950e6118c57bcf4757bc326bfedcd9da99b13f7be658ab04703010000003448302c00120013f7be650100010001003c6400208100a1e7fcc7428fb70e006a97aa84d1b5afafc44cecf70b49ac175ebdda09aa210308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b47304502210090e973557f8be9f50a76bc3bd237894a8e0ff7bd686105ffd0b29ab881c5ffe30220411c7881fae27a311aaa240d9318ec04196018c737f07a1a416f423b31c0af3a010013f7be65000002000000");
        Block block = new Block();
        block.parse(blockBytes, 0);
        BlockHeader header = block.getHeader();
        BlockSignature signature = header.getBlockSignature();
        System.out.println(HexUtil.encode(signature.getPublicKey()));
        io.nuls.core.crypto.ECKey nEckey = io.nuls.core.crypto.ECKey.fromPublicOnly(signature.getPublicKey());
        Address nerveAddress = new Address(9, BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(nEckey.getPubKey()));
        System.out.println(String.format("Nerve address: %s", nerveAddress.toString()));
    }

    public static void main(String[] args) throws Exception {
        proxy = true;
        blockSignerPubTest();
    }
}
