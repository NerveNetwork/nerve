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

import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;
import org.junit.Test;

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

            // 强制从第三方系统更新rpc
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
                // 发现version改变，切换rpc
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
