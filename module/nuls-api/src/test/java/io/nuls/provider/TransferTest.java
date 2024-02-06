package io.nuls.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.v2.model.dto.CoinFromDto;
import io.nuls.v2.model.dto.CoinToDto;
import io.nuls.v2.model.dto.TransferDto;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-11 18:30
 * @Description: dex Tool class
 */
public class TransferTest {

    String dexApiUrl;

    ECKey ecKey;

    static HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();


    private TransferTest(String dexApiUrl, String priKey) {
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
    public static TransferTest getInstance(String dexApiUrl, String priKey) {
        TransferTest dexUtils = new TransferTest(dexApiUrl, priKey);
        return dexUtils;
    }

    /**
     * Build a tool class instance
     * Can only assemble transactions, cannot sign or broadcast
     *
     * @param dexApiUrl dex apiInterface Access Address
     * @return
     */
    public static TransferTest getInstance(String dexApiUrl) {
        TransferTest dexUtils = new TransferTest(dexApiUrl, null);
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
        return post(dexApiUrl,Map.of("jsonrpc","2.0",
                "method" ,"broadcastTx",
                "id",1000,
                "params",
                List.of(1,
                        txHex)));
    }

//    private String createOrderOffline(int type, String address, String symbol, BigDecimal price, BigDecimal quantity) {
//        Map<String, Object> param = Map.of("address", address, "symbol", symbol, "price", price, "quantity", quantity, "type", type);
//        Result res = post(dexApiUrl + "api/order", param);
//        return res.getData().toString();
//    }

    private Object post(String url, Map<String, Object> param) {
        try {
            URI uri = URI.create(url);
            String requestBody = JSONUtils.obj2json(param);
            Log.debug("call dex api url :{}, request boy : {}", uri, requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(5000))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            Log.debug("result : {} ", body);
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


    public static void main(String[] args) throws InterruptedException, NulsException, JsonProcessingException {
        String url = "https://api.nuls.io/jsonrpc";
        String address = "NULSd6HgWcfKLw78Fspq4Ni7cKXieJiM213M3";
        String toAddress = "NULSd6HgaWymKrQ7NdtWossLFzunasJzdwave";
        int assetChainId = 59999;
        int assetId = 1;
        int chainId = 1;
        BigInteger amount = BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100L));
//        //example
//        //Build a tool class
//
        TransferTest dexUtils = TransferTest.getInstance(url, "");
//        //Offline assembly of pending transactions, generating transactionshex
        Map balance = (Map)dexUtils.post(url + "",
                Map.of("jsonrpc","2.0",
                "method" ,"getAccountBalance",
                        "id",1000,
                        "params",
                        List.of(chainId,
                                assetChainId,
                                assetId,
                        address)));
        Log.info("{}",balance);
        TransferDto transferDto = new TransferDto();
        CoinFromDto coinFromDto = new CoinFromDto();
        coinFromDto.setAddress(address);
        coinFromDto.setAmount(amount);
        coinFromDto.setNonce(balance.get("nonce").toString());
        coinFromDto.setAssetChainId(assetChainId);
        coinFromDto.setAssetId(assetId);
        balance = (Map)dexUtils.post(url + "",
                Map.of("jsonrpc","2.0",
                        "method" ,"getAccountBalance",
                        "id",1000,
                        "params",
                        List.of(1,
                                chainId,
                                1,
                                address)));
        Log.info("{}",balance);
        CoinFromDto coinFromDto1 = new CoinFromDto();
        coinFromDto1.setAddress(address);
        coinFromDto1.setAmount(TransactionFeeCalculator.NORMAL_PRICE_PRE_1024_BYTES);
        coinFromDto1.setNonce(balance.get("nonce").toString());
        coinFromDto1.setAssetChainId(chainId);
        coinFromDto1.setAssetId(assetId);
        CoinToDto coinToDto = new CoinToDto();
        coinToDto.setAddress(toAddress);
        coinToDto.setAmount(amount);
        coinToDto.setAssetChainId(assetChainId);
        coinToDto.setAssetId(assetId);
        Map tx = (Map) dexUtils.post(url ,Map.of("jsonrpc","2.0",
                "method" ,"createTransferTxOffline",
                "id",1000,
                "params",
                List.of(
                        List.of(coinFromDto,coinFromDto1),
                        List.of(coinToDto),
                        "remark"
                )));
        Log.info("{}",tx);
        //        //Sign and broadcast the transaction
        Object hash = dexUtils.signAndBroadcast(tx.get("txHex").toString());
        Log.info("{}",hash);
    }
}
