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
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.utils.LoggerUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * @author: Loki
 * @date: 2020-03-02
 */
public class AccountCall extends BaseCall {

    public static String signature(int chainId, String address, String password, String data, Map<String, Object> extend) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", data);
            params.put("extend", extend);

            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_signature", params);
            String signatureStr = (String)result.get("signature");
            return signatureStr;
        } catch (NulsException e) {
            LoggerUtil.LOG.error("SignDigest fail - data:{}", data);
            throw e;
        }
    }

    public static Boolean saveWhitelist(int chainId, String data) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("data", data);

            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_savewhitelist", params);
            Boolean value = (Boolean)result.get("value");
            return value;
        } catch (NulsException e) {
            LoggerUtil.LOG.error("SaveWhitelist fail - data:{}", data);
            throw e;
        }
    }


    public static String getPriKey(String address, String password) throws NulsException {
        if (ConverterContext.SIG_MODE == ConverterConstant.SIG_MODE_LOCAL) {
            return _getPriKey(address, password);
        } else {
            return _getPubKey(address, password);
        }
    }

    private static String _getPriKey(String address, String password) throws NulsException {
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

    private static String _getPubKey(String address, String password) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_getPubKeyByAddress", params);
            String pubKey = (String) result.get("pubKey");
            return pubKey;
        } catch (NulsException e) {
            LoggerUtil.LOG.error("getPubKey fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error("getPubKey fail - address:{}", address);
            LoggerUtil.LOG.error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }
}
