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
package network.nerve.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2022/11/29
 */
public class TempTest {
    int rpcVersion = -1;
    String apiUrl = null;

    @Test
    public void listFilterTest() throws Exception {
        List<HeterogeneousChainInfo> list = new ArrayList<>();
        list.add(new HeterogeneousChainInfo(111, "aaa", "0x2804a4296211ab079aed4e12120808f1703841b3"));
        list.add(new HeterogeneousChainInfo(222, "bbb", "0x1EA3FfD41c3ed3e3f788830aAef553F8F691aD8C"));
        list.add(new HeterogeneousChainInfo(333, "ccc", "0x5e7E2AbAa58e108f5B9D5D30A76253Fa8Cb81f9d"));
        list.add(new HeterogeneousChainInfo(444, "ddd", "0x3c2ff003fF996836d39601cA22394A58ca9c473b"));
        list.add(new HeterogeneousChainInfo(201, "btc", "39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV"));
        list.add(new HeterogeneousChainInfo(201, "btc", "2NDu3vcpjyiMgvRjDpQfbyh9uF2McfDJ3NF"));
        list.add(new HeterogeneousChainInfo(201, "btc", "tb1qtskq8773jlhjqm7ad6a8kxhxleznp0nech0wpk0nxt45khuy0vmqwzeumf"));
        list.add(new HeterogeneousChainInfo(555, "eee", "0x6c2039B5fDaE068baD4931E8Cc0b8E3a542937ac"));
        list.stream().filter(
                // Bitcoin chain’s multi-signature address hard upgrade
                info -> !(info.getChainId() == 201
                        && (info.getMultySignAddress().equals("39xsUsh4h1FBPiUTYqaGBi9nJKP4PgFrjV")
                        || info.getMultySignAddress().equals("2NDu3vcpjyiMgvRjDpQfbyh9uF2McfDJ3NF")))
        ).forEach(info -> {
            try {
                System.out.println(JSONUtils.obj2json(info));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void pubTest() {
        String pub = "222222222222222222222222222222222222222222222222222222222222222222";
        System.out.println(AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(pub, 5)));
    }

    @Test
    public void rpcFromAssetSystemTest() {
        do {
            try {
                this._rpcFromAssetSystem();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (true);
    }

    @Test
    public void strTest() {
        Integer a = 1;
        Integer b = 8;
        System.out.println(a + 1);
        System.out.println(b + 1);
    }

    private void _rpcFromAssetSystem() throws Exception {
        do {
            String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi"));
            if (StringUtils.isNotBlank(result)) {
                List<Map> list = JSONUtils.json2list(result, Map.class);
                Map<Long, Map> map = list.stream().collect(Collectors.toMap(m -> Long.valueOf(m.get("nativeId").toString()), Function.identity()));
                ConverterContext.HTG_RPC_CHECK_MAP = map;
            }

            // Force updates from third-party systemsrpc
            Map<Long, Map> rpcCheckMap = ConverterContext.HTG_RPC_CHECK_MAP;
            Map<String, Object> resultMap = rpcCheckMap.get(10000);
            if (resultMap == null) {
                break;
            }
            Integer _version = (Integer) resultMap.get("rpcVersion");
            if (_version == null) {
                break;
            }
            if (rpcVersion == -1) {
                rpcVersion = _version.intValue();
                System.out.println("flag1");
                break;
            }
            if (rpcVersion == _version.intValue()){
                System.out.println("flag2");
                break;
            }
            if (_version.intValue() > rpcVersion){
                // findversionChange, switchrpc
                Integer _index = (Integer) resultMap.get("index");
                if (_index == null) {
                    break;
                }
                apiUrl = (String) resultMap.get("extend" + (_index + 1));
                if (StringUtils.isBlank(apiUrl)) {
                    break;
                }
                rpcVersion = _version.intValue();
                System.out.println("flag3");
                break;
            }
        } while (false);
        System.out.println(rpcVersion);
        System.out.println(apiUrl);
        System.out.println("------------------");
    }
}
