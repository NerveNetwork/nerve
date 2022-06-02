/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.heterogeneouschain.lib.core;

import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.heterogeneouschain.lib.utils.HttpClientUtil;

import java.util.Map;

/**
 * FileName: WalletApi
 * Description:
 * Author: ZhangYX
 * Date:  2018/4/13
 */
public interface WalletApi {

    default String getAvailableRpcFromThirdParty(long htgChainId) {
        try {
            String result = HttpClientUtil.get(String.format("https://assets.nabox.io/api/chainapi/%s", htgChainId));
            if (StringUtils.isNotBlank(result)) {
                Map<String, Object> map = JSONUtils.json2map(result);
                String apiUrl = (String) map.get("apiUrl");
                if (StringUtils.isNotBlank(apiUrl)) {
                    apiUrl = apiUrl.trim();
                }
                return apiUrl;
            }
            return null;
        } catch (Exception e) {
            return null;
        }

    }
}
