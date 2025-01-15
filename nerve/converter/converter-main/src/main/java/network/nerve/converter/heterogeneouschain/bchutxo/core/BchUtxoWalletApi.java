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
package network.nerve.converter.heterogeneouschain.bchutxo.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.utils.jsonrpc.JsonRpcUtil;
import network.nerve.converter.utils.jsonrpc.RpcResult;
import org.apache.http.message.BasicHeader;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2024/7/16
 */
public class BchUtxoWalletApi extends BitCoinLibWalletApi {

    private LoadingCache<String, List<UTXOData>> MAINNET_ADDRESS_UTXO_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(50)
            .maximumSize(200)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(new CacheLoader<String, List<UTXOData>>() {
                @Override
                public List<UTXOData> load(String address) {
                    return takeUTXOFromOKX(address);
                }
            });

    /*
       028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7 - NERVEepb6ED2QAwfBdXdL7ufZ4LNmbRupyxvgb
       02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d - NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB
       02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03 - NERVEepb6Dvi5xRK5rwByAPCgF2d6bsDPuJKJ9
       02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049 - NERVEepb66GmaKLaqiFyRqsEuLNM1i1qRwTQ64
       02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0 - NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC
       03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21 - NERVEepb653BT5FFveGSPdMZzkb3iDk4ybVi63
       02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd - NERVEepb65ZajSasYsVphzZCWXZi1MDfDa9J49
       029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4 - NERVEepb69pdDv3gZEZtJEmahzsHiQE6CK4xRi
       02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9 - NERVEepb67bXCQ4XJxH4q2GyG9WmA5NUFuHZQx
       03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9 - NERVEepb698N2GmQkd8LqC6WnSN3k7gimAtzxE
       0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b - NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA
       03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4 - NERVEepb67XwfW4pHf33U1DuM4o4nyACTohooD
       035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803 - NERVEepb69vD3ZaZLgeUSwSonjndMTPmBGc8n1
       039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980 - NERVEepb61YGfhhFwpTJVt9bj2scnSsVWZGXtt
       02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292 - NERVEepb6B3jKbVM8SKHsb92j22yEKwxa19akB
     */
    List<String> defaultOkxApiKeys = new ArrayList<>();
    Map<String, String> keyMap = new HashMap<>();
    Map<String, String> keyMapFromThirdParty = new HashMap<>();
    String commonKey;

    @Override
    public void additionalCheck(Map<String, Object> resultMap) {
        try {
            String extend3 = (String) resultMap.get("extend3");
            if (StringUtils.isBlank(extend3)) {
                return;
            }
            String[] array = extend3.split(",");
            for (String str : array) {
                String[] info = str.split(":");
                keyMapFromThirdParty.put(info[0].trim(), info[1].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BchUtxoWalletApi() {
        // 33bed3e2-1605-467f-a6d0-53888df39b62
        commonKey = "8054293f-de13-4b16-90eb-0b09a8ac90d1";
        defaultOkxApiKeys.add("3e228670-3b14-4025-9640-76eba9e85903");
        defaultOkxApiKeys.add("4bbbf9cc-bf13-4d5d-817d-9d9688e4dd26");
        defaultOkxApiKeys.add("c4098e71-c068-4119-83e5-22d8c58e6e18");
        String keyI = defaultOkxApiKeys.get(0);
        String keyII = defaultOkxApiKeys.get(1);
        String keyIII = defaultOkxApiKeys.get(2);
        keyMap.put("0308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b", keyI);
        keyMap.put("02db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d", keyI);
        keyMap.put("02ae22c8f0f43081d82fcca1eae4488992cdb0caa9c902ba7cbfa0eacc1c6312f0", keyI);
        keyMap.put("02893771a18d17e10eabb08718f7da8e10a825ee19c33c8b36b13d95375f6f4a03", keyI);
        keyMap.put("028c232cfd2d3757e50cb6af2e010819a942ab231c92406170ece0846b23d323b7", keyI);

        keyMap.put("02a2edb535be21aa7fd4aa0748ae29e110e35783bc6a92fa7f417f3ffeeeec18cd", keyII);
        keyMap.put("03929732b37e41a5a37b35122002c068f596432f4b9438ba4ac2a85e7dd31c3df4", keyII);
        keyMap.put("03ac396ab4bc360610058d04940c879e0da57ea1b4a541b75df6989a6c3d5081c9", keyII);
        keyMap.put("035fe7599a7b39ad69fbd243aac7cfb93055f8f0827c6b08057874877cb890b803", keyII);
        keyMap.put("02ac31c213b1dc1d2fd55d7751326b4f07b4a5b4ecb2ce3f214cafb7832fd211b9", keyII);

        keyMap.put("02ac649c3eaf32886342b7d2ed83c01c24f297de2d006ad88e36017973644e3049", keyIII);
        keyMap.put("03c363f44196aa1a57ef7e14c19845acad721c9eefd837dacdf3fe3af1ba08ee21", keyIII);
        keyMap.put("029da5a7bf42be5c32bb06732160d033acefdbef6a537b717caea41a22a60706b4", keyIII);
        keyMap.put("039eefe5915a253db131c5a825f03ca048e5aad257edfcd295fea3fec78609d980", keyIII);
        keyMap.put("02a28a636088973c35ca0c28498b672cb8f61802164cf6904fc83463c4722c6292", keyIII);
    }
    @Override
    public BigDecimal getBalance(String address) {
        return BigDecimal.ZERO;
    }

    @Override
    public long getFeeRate() {
        return 1;
    }

    String fetchApiKey() {
        String pub = htgContext.ADMIN_ADDRESS_PUBLIC_KEY();
        String apiKey = keyMap.get(pub);
        if (StringUtils.isBlank(apiKey)) {
            apiKey = keyMapFromThirdParty.getOrDefault(pub, commonKey);
        }
        return apiKey;
    }

    List<UTXOData> takeUTXOFromOKX(String address) {
        try {
            String apiKey = this.fetchApiKey();

            String url = "https://www.oklink.com/api/v5/explorer/address/utxo?chainShortName=BCH&address=%s";
            List<BasicHeader> headers = new ArrayList<>();
            headers.add(new BasicHeader("Ok-Access-Key", apiKey));
            String s = HttpClientUtil.get(String.format(url, address), headers);
            getLog().info("[TakeUTXOFromOKX] key: {}, addr: {}, response: {}", apiKey, address, s);
            Map<String, Object> map = JSONUtils.json2map(s);
            List<Map> data = (List<Map>) map.get("data");
            if (data == null || data.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            Map dataMap = data.get(0);
            List<Map> utxoList = (List<Map>) dataMap.get("utxoList");
            if (utxoList == null || utxoList.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<UTXOData> resultList = utxoList.stream().map(utxo -> new UTXOData(
                    (String) utxo.get("txid"),
                    Integer.parseInt(utxo.get("index").toString()),
                    new BigDecimal(utxo.get("unspentAmount").toString()).movePointRight(8).toBigInteger()
            )).collect(Collectors.toList());
            return resultList;
        } catch (Exception e) {
            getLog().error("TakeUTXOFromOKX error, addr: " + address, e);
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public List<UTXOData> getAccountUTXOs(String address) {
        getLog().info("[BCH] GetAccountUTXOs, addr: {}, mainnet: {}", address, htgContext.getConverterCoreApi().isNerveMainnet());
        if (htgContext.getConverterCoreApi().isNerveMainnet()) {
            try {
                if (address.startsWith("bitcoincash:")) {
                    address = address.substring(12);
                }
                return MAINNET_ADDRESS_UTXO_CACHE.get(address);
            } catch (Exception e) {
                getLog().error("get utxo error: " + e.getMessage());
                return Collections.EMPTY_LIST;
            }
        } else {
            String url = "https://bchutxo.nerve.network/jsonrpc";
            try {
                RpcResult request = JsonRpcUtil.request(url, "getAddressUTXO", List.of(address));
                List<Map> list = (List<Map>) request.getResult();
                if (list == null || list.isEmpty()) {
                    return Collections.EMPTY_LIST;
                }
                List<UTXOData> resultList = new ArrayList<>();
                for (Map utxo : list) {
                    resultList.add(new UTXOData(
                            utxo.get("txid").toString(),
                            Integer.parseInt(utxo.get("vout").toString()),
                            new BigInteger(utxo.get("value").toString())
                    ));
                }
                return resultList;
            } catch (Exception e) {
                getLog().error("get utxo error: " + e.getMessage());
                return Collections.EMPTY_LIST;
            }
        }
    }
}
