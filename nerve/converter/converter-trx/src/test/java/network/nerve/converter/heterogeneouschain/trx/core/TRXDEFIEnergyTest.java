/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter.heterogeneouschain.trx.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.core.model.DateUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.trx.base.Base;
import network.nerve.converter.heterogeneouschain.trx.model.OrderData;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import okhttp3.*;
import org.junit.Before;
import org.junit.Test;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.TRX_20;

/**
 * @author: PierreLuo
 * @date: 2025/7/15
 */
public class TRXDEFIEnergyTest extends Base {

    private OkHttpClient client;
    private ObjectMapper objectMapper;

    @Before
    public void before() {
        this.client = new OkHttpClient();
        this.objectMapper = JSONUtils.getInstance();
        setMain();
    }

    /*
    {
       "apiKey":"dcb698eb-1dbd-4f40-a27e-af59448080d1",
       "payNums":"33333",
       "rentTime":"1",
       "receiveAddress":"TJ9ys5ojXMBoyaqg4kESX9LZoKN2vz69p2",
       "orderNotes":"1nnnnnn"
    }
     */
    @Test
    public void orderEnergy() throws Exception {
        System.out.println("开始组装参数");
        System.out.println(DateUtils.timeStamp2DateStr(System.currentTimeMillis()));
        String apiKey = "xxx";
        String energy = "65000";
        String rentTime = "1";
        String receiveAddress = "TMZBDFxu5WE8VwYSj2p3vVuBxxKMSqZDc8";
        String notes = "1nnnnnn";
        System.out.println("开始下单");
        System.out.println(DateUtils.timeStamp2DateStr(System.currentTimeMillis()));
        Map order = this.order(apiKey, energy, rentTime, receiveAddress, notes);
        if (order == null) {
            return;
        }
        OrderData orderData = this.makeOrder(order);
        String orderId = orderData.getOrderId();
        System.out.println(String.format(
                """
                orderId: %s
                balance: %s
                orderMoney: %s
                activationHash: %s
                hash: %s
                """,
                orderId,
                orderData.getBalance(),
                orderData.getOrderMoney(),
                orderData.getActivationHash(),
                Arrays.toString(orderData.getHash().toArray())
                ));
        boolean enough = false;
        int count = 0;
        while (true) {
            long _energy = Long.parseLong(energy);
            long availableEnergy = this.availableEnergy(receiveAddress);
            System.out.println("可用能量: " + availableEnergy);
            System.out.println(DateUtils.timeStamp2DateStr(System.currentTimeMillis()));
            if (availableEnergy >= _energy) {
                enough = true;
                break;
            }
            count++;
            if (count > 9) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(300);
        }
        if (!enough) {
            System.err.println("没有足够的能量去交易");
        }
        System.out.println("能量足够，可发交易");
    }

    private long availableEnergy(String addr) {
        try {
            // 查询账户资源信息
            org.tron.trident.proto.Response.AccountResourceMessage resourceInfo = walletApi.getWrapper().getAccountResource(addr);

            // 提取能量相关信息
            long energyUsed = resourceInfo.getEnergyUsed(); // 已使用的能量
            long energyLimit = resourceInfo.getEnergyLimit(); // 能量总量

            return energyLimit - energyUsed;

        } catch (Exception e) {
            System.err.println("查询能量失败: " + e.getMessage());
            return 0;
        }
    }

    @Test
    public void accountInfoTest() {
        String addr = "TJ9ys5ojXMBoyaqg4kESX9LZoKN2vz69p2";
        try {
            // 查询账户资源信息
            org.tron.trident.proto.Response.AccountResourceMessage resourceInfo = walletApi.getWrapper().getAccountResource(addr);

            // 提取能量相关信息
            long freeNetUsed = resourceInfo.getFreeNetUsed(); // 已使用的免费带宽
            long freeNetLimit = resourceInfo.getFreeNetLimit(); // 免费带宽总量
            long energyUsed = resourceInfo.getEnergyUsed(); // 已使用的能量
            long energyLimit = resourceInfo.getEnergyLimit(); // 能量总量

            // 打印能量信息
            System.out.println("账户地址: " + addr);
            System.out.println("已使用能量: " + energyUsed);
            System.out.println("能量总量: " + energyLimit);
            System.out.println("可用能量: " + (energyLimit - energyUsed));
            System.out.println("已使用免费带宽: " + freeNetUsed);
            System.out.println("免费带宽总量: " + freeNetLimit);

        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
        } finally {
            // 关闭客户端连接
            walletApi.getWrapper().close();
        }
    }

    private OrderData makeOrder(Map map) {
        Object activationHash = map.get("activationHash");
        Object hash = map.get("hash");
        OrderData orderData = new OrderData(
                map.get("orderId").toString(),
                map.get("balance").toString(),
                map.get("orderMoney").toString(),
                activationHash == null ? null : activationHash.toString(),
                hash == null ? Collections.EMPTY_LIST : (List<String>) hash
        );
        return orderData;
    }


    private Map order(String apiKey, String energy, String rentTime, String receiveAddress, String notes) {
        try {
            Map req = Map.of(
                    "apiKey", apiKey,
                    "payNums", energy,
                    "rentTime", rentTime,
                    "receiveAddress", receiveAddress,
                    "orderNotes", notes
            );
            // 构造请求体
            String requestBody = JSONUtils.obj2json(req);

            // 构建请求
            Request request = new Request.Builder()
                    .url("https://app-api.trxdefi.ai/openapi/tron/energy/order/batchPay")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println(String.format("Failed to order energy %s, receiveAddress %s", energy, receiveAddress));
                    return null;
                }
                String resp = response.body().string();
                Map<String, Object> map = JSONUtils.json2map(resp);
                Object codeObj = map.get("code");
                if (codeObj == null) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                int code = Integer.parseInt(codeObj.toString());
                if (code != 0) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                Object dataObj = map.get("data");
                if (dataObj == null) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                return (Map) dataObj;
            }
        } catch (Exception e) {
            System.err.println(String.format("Error to order energy %s, receiveAddress %s, error: %s", energy, receiveAddress, e.getMessage()));
        }
        return null;
    }

    private Map queryOrder(String apiKey, String orderId) {
        try {
            Map req = Map.of(
                    "apiKey", apiKey,
                    "orderId", orderId
            );
            // 构造请求体
            String requestBody = JSONUtils.obj2json(req);

            // 构建请求
            Request request = new Request.Builder()
                    .url("https://app-api.trxdefi.ai/openapi/tron/energy/orderData")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println(String.format("Failed to query orderId %s", orderId));
                    return null;
                }
                String resp = response.body().string();
                Map<String, Object> map = JSONUtils.json2map(resp);
                Object codeObj = map.get("code");
                if (codeObj == null) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                int code = Integer.parseInt(codeObj.toString());
                if (code != 0) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                Object dataObj = map.get("data");
                if (dataObj == null) {
                    System.err.println(String.format("error resp: %s", resp));
                    return null;
                }
                return (Map) dataObj;
            }
        } catch (Exception e) {
            System.err.println(String.format("Failed to query orderId %s", orderId));
        }
        return null;
    }

    @Test
    public void estimateEnergy() throws Exception {
        String from = "TMZBDFxu5WE8VwYSj2p3vVuBxxKMSqZDc8";
        String to = "TRJGmWwGVxswHmwQRepah75ouvDzMCwbv4";
        String erc20Address = "TVTgTk8ewBJh8nqhK6UGCpnujojXbRkVLz";
        int erc20Decimals = 18;
        String value = "0.5";
        BigInteger valueBig = new BigDecimal(value).movePointRight(erc20Decimals).toBigInteger();
        // estimatefeeLimit
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(valueBig)),
                Arrays.asList(new TypeReference<Type>() {}));
        TrxEstimateSun estimateSun = walletApi.estimateSunUsed(from, erc20Address, function);
        if (estimateSun.isReverted()) {
            System.err.println(String.format("Transaction verification failed, reason: %s", estimateSun.getRevertReason()));
            return;
        }
        System.out.println(String.format("estimateSun: %s", JSONUtils.obj2json(estimateSun)));
        if (estimateSun.getEnergyUsed() > 65000) {
            System.err.println("能量不足，交易拒绝🙅🏻");
            return;
        }
    }

    @Test
    public void transferTRC20() throws Exception {
        String fromPriKey = "110a3729d779846003d4cc07937fb6b172994d6ee2a54d1ab7301f2d73f48798";
        //String from = "TJ9ys5ojXMBoyaqg4kESX9LZoKN2vz69p2";
        //String to = "TRJGmWwGVxswHmwQRepah75ouvDzMCwbv4";
        //String erc20Address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
        String from = "TMZBDFxu5WE8VwYSj2p3vVuBxxKMSqZDc8";
        String to = "TMZBDFxu5WE8VwYSj2p3vVuBxxKMSqZDc8";
        String erc20Address = "TSMePsQvdDw1eyVHDbXfu9gqrGh4hfe1JM";
        int erc20Decimals = 6;
        String value = "0.0001";
        //String value = "11";
        BigInteger valueBig = new BigDecimal(value).movePointRight(erc20Decimals).toBigInteger();
        // estimatefeeLimit
        Function function = new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(valueBig)),
                Arrays.asList(new TypeReference<Type>() {}));
        TrxEstimateSun estimateSun = walletApi.estimateSunUsed(from, erc20Address, function);
        if (estimateSun.isReverted()) {
            System.err.println(String.format("Transaction verification failed, reason: %s", estimateSun.getRevertReason()));
            return;
        }
        System.out.println(String.format("estimateSun: %s", JSONUtils.obj2json(estimateSun)));
        if (estimateSun.getEnergyUsed() > 65000) {
            System.err.println("能量不足，交易拒绝🙅🏻");
            return;
        }

        BigInteger feeLimit = TRX_20;
        if (estimateSun.getSunUsed() > 0) {
            feeLimit = BigInteger.valueOf(estimateSun.getSunUsed());
        }
        System.out.println(feeLimit);
        //TrxSendTransactionPo trx = walletApi.transferTRC20Token(
        //        from, to, valueBig, fromPriKey, erc20Address, feeLimit);
        //System.out.println(trx.getTxHash());
    }
}
