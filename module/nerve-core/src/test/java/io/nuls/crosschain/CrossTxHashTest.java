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
package io.nuls.crosschain;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.crosschain.base.model.bo.txdata.CrossTransferData;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2022/4/18
 */
public class CrossTxHashTest {

    static int nulsChainId = 5;
    static int nerveChainId = 5;
    static String rpcAddress = "";

    private void setTest() {
        nulsChainId = 2;
        nerveChainId = 5;
        rpcAddress = "http://beta.api.nerve.network/jsonrpc";
    }

    private void setMain() {
        nulsChainId = 1;
        nerveChainId = 9;
        rpcAddress = "https://api.nerve.network/jsonrpc";
    }

    @Test
    public void test() throws Exception {
        setTest();
        String hash = "ae2aec0316f0e7ad497eb9b1a768de1cb1a8f3fe1cf9a9e51a920883044210a3";
        Map resultMap = this.request(rpcAddress, "getTx", List.of(nerveChainId, hash));
        Long timestamp = Long.valueOf(resultMap.get("timestamp").toString());
        String remark = (String) resultMap.get("remark");
        List<Map> froms = (List<Map>) resultMap.get("from");
        List<Map> tos = (List<Map>) resultMap.get("to");
        if (tos.size() > 1) {
            throw new RuntimeException("Unsupported transactions! reason:CoinToThere are multiple.");
        }
        Transaction tx = new Transaction();
        // assembleNULSBasic information of cross chain transactions on the internet
        tx.setType(10);
        tx.setTime(timestamp);
        if (StringUtils.isNotBlank(remark)) {
            tx.setRemark(remark.getBytes(StandardCharsets.UTF_8));
        }
        // assembleNULSCross chain transactions on the internettxData
        CrossTransferData txData = new CrossTransferData();
        txData.setSourceType(10);
        txData.setSourceHash(HexUtil.decode(hash));
        byte[] txDataBytes = txData.serialize();
        tx.setTxData(txDataBytes);
        // assembleNULSCross chain transactions on the internetCoinData
        CoinData coinData = new CoinData();
        boolean isTransferNVT = false;
        BigInteger transferAmount = BigInteger.ZERO;
        for (Map to : tos) {
            int assetsChainId = Integer.parseInt(to.get("assetsChainId").toString());
            int assetsId = Integer.parseInt(to.get("assetsId").toString());
            if (assetsChainId == nerveChainId && assetsId == 1) {
                isTransferNVT = true;
            }
            String address = to.get("address").toString();
            transferAmount = new BigInteger(to.get("amount").toString());
            long lockTime = Long.parseLong(to.get("lockTime").toString());
            CoinTo _to = new CoinTo();
            _to.setAddress(AddressTool.getAddress(address));
            _to.setAssetsChainId(assetsChainId);
            _to.setAssetsId(assetsId);
            _to.setAmount(transferAmount);
            _to.setLockTime(lockTime);
            coinData.getTo().add(_to);
        }
        for (Map from : froms) {
            int assetsChainId = Integer.parseInt(from.get("assetsChainId").toString());
            int assetsId = Integer.parseInt(from.get("assetsId").toString());
            BigInteger amount = new BigInteger(from.get("amount").toString());
            // eliminate NERVEChain basedNVTHandling fees
            if (assetsChainId == nerveChainId && assetsId == 1) {
                if (!isTransferNVT) {
                    continue;
                } else {
                    amount = transferAmount;
                }
            }
            String address = from.get("address").toString();
            String nonce = from.get("nonce").toString();
            int locked = Integer.parseInt(from.get("locked").toString());
            CoinFrom _from = new CoinFrom();
            _from.setAddress(AddressTool.getAddress(address));
            _from.setAssetsChainId(assetsChainId);
            _from.setAssetsId(assetsId);
            _from.setAmount(amount);
            _from.setNonce(HexUtil.decode(nonce));
            _from.setLocked((byte) locked);
            coinData.getFrom().add(_from);
        }
        tx.setCoinData(coinData.serialize());
        System.out.println(String.format("NULSOnline transactionshash: %s", tx.getHash().toString()));

    }

    private Map request(String requestURL, String method, List<Object> params) throws Exception {
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

}
