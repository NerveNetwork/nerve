/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.P2PHKSignature;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;

import java.util.HashMap;
import java.util.Map;

import static nerve.network.converter.utils.LoggerUtil.LOG;


/**
 * @author: Chino
 * @date: 2020-03-02
 */
public class AccountCall extends BaseCall {

    /**
     * 签名
     * @param address
     * @param password
     * @param data
     * @return
     * @throws NulsException
     */
    public static P2PHKSignature signDigest(String address, String password, byte[] data) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", RPCUtil.encode(data));
            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
            String signatureStr = (String)result.get("signature");
            return ConverterUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        } catch (NulsException e) {
            LOG.error("SignDigest fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LOG.error("SignDigest fail - address:{}", address);
            LOG.error(e);
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
            LOG.error("getPriKey fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LOG.error("getPriKey fail - address:{}", address);
            LOG.error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }
}
