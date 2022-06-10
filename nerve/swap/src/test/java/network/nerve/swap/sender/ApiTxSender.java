/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.swap.sender;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.model.bo.LedgerBalance;
import network.nerve.swap.utils.HttpClientUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
public abstract class ApiTxSender {

    protected boolean broadcastTx(int chainId,Transaction tx) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", "broadcastTx");
        paramMap.put("params", List.of(chainId, HexUtil.encode(tx.serialize())));
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(getApiUrl(), paramMap);
        if (StringUtils.isBlank(response)) {
            System.out.println("未能得到返回数据");
            return false;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Map<String, Object> resultMap = (Map<String, Object>) map.get("result");
        if (null == resultMap) {
            System.out.println(map.get("error"));
            return false;
        }
        return true;
    }

    protected LedgerBalance getLedgerBalance(int chainId, String address, int syrupAssetChainId, int syrupAssetId) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("jsonrpc", "2.0");
        paramMap.put("method", "getAccountBalance");
        paramMap.put("params", List.of(chainId, syrupAssetChainId, syrupAssetId, address));
        paramMap.put("id", "1234");
        String response = HttpClientUtil.post(getApiUrl(), paramMap);
        if (StringUtils.isBlank(response)) {
            return null;
        }
        Map<String, Object> map = JSONUtils.json2map(response);
        Map<String, Object> resultMap = (Map<String, Object>) map.get("result");
        if (null == resultMap) {
            return null;
        }
        LedgerBalance balance = LedgerBalance.newInstance();
        balance.setAssetsId(syrupAssetId);
        balance.setNonce(HexUtil.decode(resultMap.get("nonce") + ""));
        balance.setBalance(new BigInteger(resultMap.get("balance") + ""));
        balance.setFreeze(new BigInteger(resultMap.get("freeze") + ""));
        balance.setAssetsChainId(syrupAssetChainId);
        balance.setAddress(AddressTool.getAddress(address));
        return balance;
    }


    public abstract String getApiUrl();
}
