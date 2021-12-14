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

package network.nerve.quotation.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.rpc.callback.NewBlockHeightInvoke;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.LoggerUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public class QuotationCall {


    public static Map<String, Object> getSwapPairByTokenLP(Chain chain, int lpChainId, int lpAssetId) {
        try {
            Map<String, Object> params = new HashMap(QuotationConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("tokenLPStr", lpChainId + "-" + lpAssetId);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.SW.abbr, "sw_swap_pair_info_by_lp", params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Packing state failed to send!");
                return null;
            }
            return (HashMap) ((HashMap) cmdResp.getResponseData()).get("sw_swap_pair_info_by_lp");
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    public static Map<String, Object> getSwapPrice(Chain chain,String amountIn,String[] tokenPath){

        try {
            Map<String, Object> params = new HashMap(QuotationConstant.INIT_CAPACITY_4);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("amountIn", amountIn);
            params.put("tokenPath", tokenPath);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.SW.abbr, "sw_swap_min_amount_token_trade", params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Packing state failed to send!");
                return null;
            }
            return (HashMap) ((HashMap) cmdResp.getResponseData()).get("sw_swap_min_amount_token_trade");
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    /**
     * 区块最新高度
     */
    public static boolean subscriptionNewBlockHeight(Chain chain) {
        try {
            Map<String, Object> params = new HashMap<>(QuotationConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            String messageId = ResponseMessageProcessor.requestAndInvoke(ModuleE.BL.abbr, "latestHeight",
                    params, "0", "1", new NewBlockHeightInvoke(chain));
            if (null != messageId) {
                return true;
            }
            return false;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }


    public static P2PHKSignature signDigest(String address, String password, byte[] data) throws NulsException {
        try {
            int chainId = AddressTool.getChainIdByAddress(address);
            Map<String, Object> params = new HashMap<>(QuotationConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("address", address);
            params.put("password", password);
            params.put("data", RPCUtil.encode(data));
            HashMap result = (HashMap) requestAndResponse(ModuleE.AC.abbr, "ac_signDigest", params);
            String signatureStr = (String) result.get("signature");
            return CommonUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
        } catch (NulsException e) {
            LoggerUtil.LOG.error("QuotationCall signDigest fail - address:{}", address);
            throw e;
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error("QuotationCall signDigest fail - address:{}", address);
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
        }
    }

    /**
     * 查询本节点是不是共识节点，如果是则返回，共识账户和密码
     * Query whether the node is a consensus node, if so, return, consensus account and password
     */
    public static Map<String, String> getPackerInfo(Chain chain) {
        try {
            Map<String, Object> params = new HashMap(QuotationConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_getPackerInfo", params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Packing state failed to send!");
                return null;
            }
            return (HashMap) ((HashMap) cmdResp.getResponseData()).get("cs_getPackerInfo");
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    public static boolean isConfirmed(Chain chain, String txhash) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(QuotationConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("txHash", txhash);
            HashMap result = (HashMap) requestAndResponse(ModuleE.TX.abbr, "tx_isConfirmed", params);
            return (boolean) result.get("value");
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
        }
    }

    /**
     * 发起新交易
     */
    public static void newTx(Chain chain, Transaction tx) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(QuotationConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("tx", RPCUtil.encode(tx.serialize()));
            requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
        } catch (IOException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.SERIALIZE_ERROR);
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
        }
    }

    public static boolean isConsensusNode(Chain chain, BlockHeader blockHeader, String address) throws NulsException {
        try {
            Map<String, Object> params = new HashMap(QuotationConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("blockHeader", null == blockHeader ? null : RPCUtil.encode(blockHeader.serialize()));
            params.put("address", address);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_isConsensusAgent", params);
            if (!cmdResp.isSuccess()) {
                String errorCode = cmdResp.getResponseErrorCode();
                chain.getLogger().error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{}", "cs_isConsensusAgent", errorCode, cmdResp.getResponseComment());
                throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
            }
            HashMap map = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cs_isConsensusAgent");
            if (null == map) {
                throw new NulsException(QuotationErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND);
            }
            Boolean rs = (Boolean) map.get("value");
            if (null == rs) {
                throw new NulsException(QuotationErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND);
            }
            return rs;
        } catch (IOException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.SERIALIZE_ERROR);
        } catch (Exception e) {
            chain.getLogger().error("[isConsensusNode] Failed to obtain consensus data!");
            chain.getLogger().error(e);
            throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
        }
    }

    public static Object requestAndResponse(String moduleCode, String cmd, Map params) throws NulsException {
        return requestAndResponse(moduleCode, cmd, params, null);
    }

    /**
     * 调用其他模块接口
     * Call other module interfaces
     */
    public static Object requestAndResponse(String moduleCode, String cmd, Map params, Long timeout) throws NulsException {
        try {
            params.put(Constants.VERSION_KEY_STR, QuotationConstant.RPC_VERSION);
            Response response = null;
            try {
                if (null == timeout) {
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params);
                } else {
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params, timeout);
                }
            } catch (Exception e) {
                LoggerUtil.LOG.error(e);
                throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
            }
            if (!response.isSuccess()) {
                String errorCode = response.getResponseErrorCode();
                LoggerUtil.LOG.error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{}", cmd, errorCode, response.getResponseComment());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            Map data = (Map) response.getResponseData();
            return data.get(cmd);
        } catch (RuntimeException e) {
            LoggerUtil.LOG.error(e);
            throw new NulsException(QuotationErrorCode.RPC_REQUEST_FAILD);
        }
    }
}
