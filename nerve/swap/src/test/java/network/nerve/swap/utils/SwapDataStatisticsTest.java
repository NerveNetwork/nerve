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
package network.nerve.swap.utils;

import io.nuls.core.io.IoUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2022/2/9
 */
public class SwapDataStatisticsTest {

    static int chainId = 5;
    static int assetChainId = 5;
    static int assetId = 1;
    static String rpcAddress = "";

    private void setTest() {
        chainId = 5;
        assetChainId = 5;
        assetId = 1;
        rpcAddress = "http://beta.api.nerve.network/jsonrpc";
    }

    private void setMain() {
        chainId = 9;
        assetChainId = 9;
        assetId = 1;
        rpcAddress = "https://api.nerve.network/jsonrpc";
    }

    protected Map request(String requestURL, String method, List<Object> params) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", method);
        paramMap.put("params", params);
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(requestURL, paramMap);
        if (StringUtils.isBlank(response)) {
            System.err.println("Failed to obtain return data");
            return null;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Map<String, Object> resultMap = (Map<String, Object>) map.get("result");
        if (null == resultMap) {
            System.err.println(map.get("error"));
            return null;
        }
        return resultMap;
    }

    protected BigDecimal getPrice(int assetChainId, int assetId, String symbol) throws Exception {
        if ("OxSGD".equals(symbol)) {
            return new BigDecimal("1.2391");
        }
        if ("OxUSD".equals(symbol)) {
            return new BigDecimal("1");
        }
        if ("VIBK".equals(symbol)) {
            return new BigDecimal("0.00146835");
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("language", "EN");
        paramMap.put("chain", "NERVE");
        paramMap.put("searchKey", symbol);
        String response = HttpClientUtil.post("https://api.v2.nabox.io/nabox-api/asset/price", paramMap);
        if (StringUtils.isBlank(response)) {
            System.err.println("Failed to obtain return data");
            return BigDecimal.ZERO;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        List<Map> list = (List<Map>) map.get("data");
        if (null == list) {
            System.err.println(JSONUtils.obj2PrettyJson(map));
            return BigDecimal.ZERO;
        }
        for (Map p : list) {
            int _chainId = (Integer) p.get("chainId");
            int _assetId = (Integer) p.get("assetId");
            if (_chainId == assetChainId && _assetId == assetId) {
                String usdPrice = p.get("usdPrice").toString();
                return new BigDecimal(usdPrice);
            }
        }
        System.err.println(JSONUtils.obj2PrettyJson(list));
        return BigDecimal.ZERO;
    }

    @Test
    public void tradingVolumeTest() throws Exception {
        setMain();
        BigDecimal total = BigDecimal.ZERO;
        List<String> list = IoUtils.readLines(new FileInputStream(new File("/Users/pierreluo/Nuls/user_swap_trade_record.json")), StandardCharsets.UTF_8.name());
        List<Map<String, Object>> maps = list.stream().map(s -> {
            try {
                return JSONUtils.json2map(s);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

        Map<String, BigDecimal> assetMap = new HashMap<>();
        for (Map map : maps) {
            String tokenOut = (String) map.get("tokenOut");
            String amountOut = (String) map.get("amountOut");
            BigDecimal amount0 = assetMap.getOrDefault(tokenOut, BigDecimal.ZERO);
            amount0 = amount0.add(new BigDecimal(amountOut));
            assetMap.put(tokenOut, amount0);
        }
        Set<Map.Entry<String, BigDecimal>> entries = assetMap.entrySet();
        for(Map.Entry<String, BigDecimal> entry: entries) {
            String asset = entry.getKey();
            BigDecimal value = entry.getValue();
            String[] split = asset.split("-");
            int assetChainId = Integer.parseInt(split[0]);
            int assetId = Integer.parseInt(split[1]);
            Map result = this.request(rpcAddress, "getAssetInfo", List.of(chainId, assetChainId, assetId));
            int decimals = (Integer) result.get("decimalPlace");
            String symbol = (String) result.get("assetSymbol");
            BigDecimal dValue = value.movePointLeft(decimals);
            System.out.println(String.format("Asset inquiry: %s-%s, symbol: %s, Transaction quantity: %s", assetChainId, assetId, symbol, dValue.stripTrailingZeros().toPlainString()));
            BigDecimal price = this.getPrice(assetChainId, assetId, symbol);
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                System.out.println(String.format("Wrong asset, unable to obtain price"));
                System.out.println("-----");
                continue;
            }
            BigDecimal uValue = dValue.multiply(price);
            total = total.add(uValue);
            System.out.println(String.format("Asset results: %s-%s, symbol: %s, Transaction quantity: %s, Uprice: %s, transactionUvalue: %s", assetChainId, assetId, symbol, dValue.stripTrailingZeros().toPlainString(), price.toPlainString(), uValue.stripTrailingZeros().toPlainString()));
            System.out.println("-----");
        }
        System.out.println("Total transactionsUvalue: " + total.stripTrailingZeros().toPlainString());
    }

    @Test
    public void liquidityTest() throws Exception {
        setMain();
        List<String> pairList = new ArrayList<>();
        pairList.add("NERVEepb72TGVkNEz2UUUUMLM44WBBXQvRnxZN");
        pairList.add("NERVEepb6zfdxohYHU8mUjg97pQ8Zse3wpRTdh");
        pairList.add("NERVEepb72kHYRSrqoTtG9ZZw4TFUArawduy1s");
        pairList.add("NERVEepb6qmtCmo4hjgySikQRWWVnHN3GtVTHZ");
        pairList.add("NERVEepb75bDuojUPPLsuukUJ3Usb19dv9y1Gh");
        pairList.add("NERVEepb6v9PBcD2B32kMBVtQfq98p84taRmWW");
        pairList.add("NERVEepb6srG3EW7sXbN9JJxX5bAgf91svGQkk");
        pairList.add("NERVEepb6qPMLk8ZG5b8KukJpaH2Q4FgWTfZJa");
        pairList.add("NERVEepb6uKw11E5YcF7zkFcfTEKvHpZ2R7zdu");
        pairList.add("NERVEepb73NHWbCtGttMTzXMCwCYgrABsn5W1U");
        Map<String, BigDecimal> assetMap = new HashMap<>();
        for (String pairAddress : pairList) {
            Map result = this.request(rpcAddress, "getSwapPairInfoByPairAddress", List.of(chainId, pairAddress));
            String reserve0 = (String) result.get("reserve0");
            String reserve1 = (String) result.get("reserve1");
            Map po = (Map) result.get("po");
            String token0 = (String) po.get("token0");
            String token1 = (String) po.get("token1");
            BigDecimal amount0 = assetMap.getOrDefault(token0, BigDecimal.ZERO);
            BigDecimal amount1 = assetMap.getOrDefault(token1, BigDecimal.ZERO);
            amount0 = amount0.add(new BigDecimal(reserve0));
            amount1 = amount1.add(new BigDecimal(reserve1));
            assetMap.put(token0, amount0);
            assetMap.put(token1, amount1);
        }
        Set<Map.Entry<String, BigDecimal>> entries = assetMap.entrySet();
        for(Map.Entry<String, BigDecimal> entry: entries) {
            String asset = entry.getKey();
            BigDecimal value = entry.getValue();
            String[] split = asset.split("-");
            int assetChainId = Integer.parseInt(split[0]);
            int assetId = Integer.parseInt(split[1]);
            Map result = this.request(rpcAddress, "getAssetInfo", List.of(chainId, assetChainId, assetId));
            int decimals = (Integer) result.get("decimalPlace");
            String symbol = (String) result.get("assetSymbol");
            BigDecimal dValue = value.movePointLeft(decimals);
            System.out.println(String.format("Asset inquiry: %s-%s, symbol: %s, Liquidity quantity: %s", assetChainId, assetId, symbol, dValue.stripTrailingZeros().toPlainString()));
            BigDecimal price = this.getPrice(assetChainId, assetId, symbol);
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                System.out.println(String.format("Wrong asset, unable to obtain price"));
                System.out.println("-----");
                continue;
            }
            BigDecimal uValue = dValue.multiply(price);
            System.out.println(String.format("Asset results: %s-%s, symbol: %s, Liquidity quantity: %s, Uprice: %s, LiquidityUvalue: %s", assetChainId, assetId, symbol, dValue.stripTrailingZeros().toPlainString(), price.toPlainString(), uValue.stripTrailingZeros().toPlainString()));
            System.out.println("-----");
        }
    }
}
