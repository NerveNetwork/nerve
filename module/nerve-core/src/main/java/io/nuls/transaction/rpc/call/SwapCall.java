/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.transaction.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021/4/16
 */
public class SwapCall {

    /**
     * packSWAPnotice
     * @param chain
     * @param blockHeight
     * @param blockTime
     * @param packingAddress
     * @param preStateRoot
     * @param blockType The processing mode of this call, pack:0, Verify Block:1
     * @return
     * @throws NulsException
     */
    public static boolean swapBatchBegin(Chain chain, long blockHeight, long blockTime, String preStateRoot, int blockType) {
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("blockHeight", blockHeight);
        params.put("blockTime", blockTime);
        params.put("blockType", blockType);
        params.put("preStateRoot", preStateRoot);
        try {
            TransactionCall.requestAndResponse(ModuleE.SW.abbr, "sw_batch_begin", params);
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }

    }
    /**
     * callSWAP, Whether the transaction execution was successful or not,Does not affect the packaging of transactions
     * @param chain
     * @param tx
     * @param blockType The processing mode of this call, pack:0, Verify Block:1
     * @return
     * @throws NulsException
     */
    public static Map<String, Object> invoke(Chain chain, String tx, long blockHeight, long blockTime, int blockType) throws NulsException {
        try {
            Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("tx", tx);
            params.put("blockHeight", blockHeight);
            params.put("blockTime", blockTime);
            params.put("blockType", blockType);
            HashMap result = (HashMap) TransactionCall.requestAndResponse(ModuleE.SW.abbr, "sw_invoke", params);
            if(null == result){
                chain.getLogger().error("call sw_invoke response result is null, error:{}",
                        TxErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND.getCode());
                throw new NulsException(TxErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND);
            }
            return result;
        } catch (RuntimeException e) {
            chain.getLogger().error(e);
            throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
        }
    }

    /**
     * Terminate callSWAPTransaction Execution
     * @param chain
     * @param blockHeight
     * @return
     * @throws NulsException
     */
    public static Map<String, Object> swapBatchEnd(Chain chain, long blockHeight, int blockType) throws NulsException {

        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_4);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("blockHeight", blockHeight);
        params.put("blockType", blockType);
        try {
            Map result = (Map) TransactionCall.requestAndResponse(ModuleE.SW.abbr, "sw_batch_end", params);
            return result;
        }catch (NulsException e) {
            throw e;
        }catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
        }
    }

}
