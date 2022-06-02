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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2022/2/9
 */
public class SwapPairDataTest {

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

    protected Object request(String requestURL, String method, List<Object> params) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", method);
        paramMap.put("params", params);
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(requestURL, paramMap);
        if (StringUtils.isBlank(response)) {
            System.err.println("未能得到返回数据");
            return null;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Object result = map.get("result");
        if (null == result) {
            System.err.println(map.get("error"));
            return null;
        }
        return result;
    }

    @Test
    public void swapPairTokensInfo() throws Exception {
        setTest();
        Map<String, BigDecimal> tokens = new HashMap<>();
        List<String> allSwapPairs = (List<String>) this.request(rpcAddress, "getAllSwapPairsInfo", List.of(chainId));
        for (String pair : allSwapPairs) {
            // getSwapPairInfoByPairAddress
            Map info = (Map)this.request(rpcAddress, "getSwapPairInfoByPairAddress", List.of(chainId, pair));
            Map baseInfo = (Map) info.get("po");
            String reserve0 = (String) info.get("reserve0");
            String reserve1 = (String) info.get("reserve1");
            String token0Str = (String) baseInfo.get("token0");
            String token1Str = (String) baseInfo.get("token1");
            BigDecimal value0 = tokens.getOrDefault(token0Str, BigDecimal.ZERO);
            value0 = value0.add(new BigDecimal(reserve0));
            tokens.put(token0Str, value0);
            BigDecimal value1 = tokens.getOrDefault(token1Str, BigDecimal.ZERO);
            value1 = value1.add(new BigDecimal(reserve1));
            tokens.put(token1Str, value1);
        }
        for (String tokenStr : tokens.keySet()) {
            String[] split = tokenStr.split("-");
            int assetChainId = Integer.parseInt(split[0]);
            int assetId = Integer.parseInt(split[1]);
            Map assetInfo = (Map) this.request(rpcAddress, "getAssetInfo", List.of(chainId, assetChainId, assetId));
            int decimals = (Integer) assetInfo.get("decimalPlace");
            String symbol = (String) assetInfo.get("assetSymbol");
            System.out.println(String.format("%s-%s, decimals: %s, symbol: %s, reserve: %s",
                    assetChainId, assetId, decimals, symbol, tokens.get(tokenStr).movePointLeft(decimals).stripTrailingZeros().toPlainString()));
        }

    }

    @Test
    public void stableSwapPairTokensInfo() throws Exception {
        setTest();
        Map<String, BigDecimal> tokens = new HashMap<>();
        List<String> allSwapPairs = (List<String>) this.request(rpcAddress, "getAllStableSwapPairsInfo", List.of(chainId));
        for (String pair : allSwapPairs) {
            Map info = (Map)this.request(rpcAddress, "getStableSwapPairInfo", List.of(chainId, pair));
            Map baseInfo = (Map) info.get("po");
            String tokenLPStr = (String) baseInfo.get("tokenLP");
            BigDecimal value0 = tokens.getOrDefault(tokenLPStr, BigDecimal.ZERO);
            tokens.put(tokenLPStr, value0);
        }
        for (String tokenStr : tokens.keySet()) {
            String[] split = tokenStr.split("-");
            int assetChainId = Integer.parseInt(split[0]);
            int assetId = Integer.parseInt(split[1]);
            Map assetInfo = (Map) this.request(rpcAddress, "getAssetInfo", List.of(chainId, assetChainId, assetId));
            int decimals = (Integer) assetInfo.get("decimalPlace");
            String symbol = (String) assetInfo.get("assetSymbol");
            System.out.println(String.format("%s-%s, decimals: %s, symbol: %s",
                    assetChainId, assetId, decimals, symbol));
        }

    }
}
