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

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020-03-02
 */
public class TransactionCall extends BaseCall {

    /**
     * Initiate new transactions
     */
    public static boolean newTx(Chain chain, Transaction tx) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            Response cmdResp = null;
            try {
                params.put("tx", RPCUtil.encode(tx.serialize()));
                cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            } catch (IOException e) {
                chain.getLogger().error(e);
                throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
            } catch (Exception e) {
                chain.getLogger().error(e);
                throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
            }
            if (!cmdResp.isSuccess()) {
                String errorCode = cmdResp.getResponseErrorCode();
                chain.getLogger().error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{} hash:{}",
                        "tx_newTx", errorCode, cmdResp.getResponseComment(), tx.getHash().toHex());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            return cmdResp.isSuccess();
        } catch (RuntimeException e) {
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }

    }

    /**
     * According to the transactionhash Obtain confirmed transactions
     * @param chain
     * @param hash
     * @return
     * @throws NulsException
     */
    public static Transaction getConfirmedTx(Chain chain, NulsHash hash) throws NulsException {
        return getConfirmedTx(chain, hash.toHex());
    }

    public static Transaction getConfirmedTx(Chain chain, String hash) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("txHash", hash);
            HashMap map = (HashMap) requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTx", params);
            String txStr = (String)map.get("tx");
            if(StringUtils.isBlank(txStr)){
                return null;
            }
            long blockHeight = Long.parseLong(map.get("blockHeight").toString());
            Transaction tx = Transaction.getInstance(RPCUtil.decode(txStr));
            tx.setBlockHeight(blockHeight);
            return tx;
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }

    /**
     * Check if the address has been frozen
     * @param chain
     * @param address
     * @return
     * @throws NulsException
     */
    public static boolean isLocked(Chain chain, String address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("address", address);
            HashMap map = (HashMap) requestAndResponse(ModuleE.TX.abbr, "tx_isLocked", params);
            return (boolean) map.get("value");
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }

    public static boolean lock(Chain chain, byte[] address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("address", AddressTool.getStringAddressByBytes(address));
            HashMap map = (HashMap) requestAndResponse(ModuleE.TX.abbr, "tx_lock", params);
            return (boolean) map.get("value");
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }

    public static boolean unlock(Chain chain, byte[] address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("address", AddressTool.getStringAddressByBytes(address));
            HashMap map = (HashMap) requestAndResponse(ModuleE.TX.abbr, "tx_unlock", params);
            return (boolean) map.get("value");
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(ConverterErrorCode.RPC_REQUEST_FAILD);
        }
    }



}
