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

package network.nerve.converter.rpc.call;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.LoggerUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * @author: Loki
 * @date: 2020-03-02
 */
public class AccountCall extends BaseCall {

    /**
     * 签名
     * @param address
     * @param password
     * @param hash
     * @return
     * @throws NulsException
     */
    public static P2PHKSignature signDigest(String address, String password, NulsHash hash) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", hash.toHex());
            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
            String signatureStr = (String)result.get("signature");
            return ConverterUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        } catch (NulsException e) {
            LoggerUtil.LOG.error("SignDigest fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error("SignDigest fail - address:{}", address);
            LoggerUtil.LOG.error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }


    public static String getPriKey(String address, String password) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_getPriKeyByAddress", params);
            String priKey = (String) result.get("priKey");
            return priKey;
        } catch (NulsException e) {
            LoggerUtil.LOG.error("getPriKey fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error("getPriKey fail - address:{}", address);
            LoggerUtil.LOG.error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }
}
