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
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Request;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.Chain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020-02-27
 */
public class NetWorkCall extends BaseCall {

    /**
     * Send messages to specified nodes
     *
     * @param chain
     * @param message
     * @param nodeId
     * @return
     */
    public static boolean sendToNode(Chain chain, BaseBusinessMessage message, String nodeId, String cmd) {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("nodes", nodeId);
            params.put("messageBody", RPCUtil.encode(message.serialize()));
            params.put("command", cmd);
            Request request = MessageUtil.newRequest("nw_sendPeersMsg", params, Constants.BOOLEAN_FALSE, Constants.ZERO, Constants.ZERO);
            String messageId = ResponseMessageProcessor.requestOnly(ModuleE.NW.abbr, request);
            return messageId.equals("0") ? false : true;
        } catch (IOException e) {
            chain.getLogger().error("message:" + cmd + " failed", e);
            return false;
        } catch (Exception e) {
            chain.getLogger().error("message:" + cmd + " failed", e);
            return false;
        }
    }

    /**
     * Broadcast messages to nodes on the network
     * 1.Forwarding transactionshash
     * 2.Broadcast complete transactions
     *
     * @param chain
     * @param message
     * @param excludeNodes Excluded nodes
     * @return
     */
    public static boolean broadcast(Chain chain, BaseBusinessMessage message, String excludeNodes, String cmd) {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
            params.put(Constants.VERSION_KEY_STR, ConverterConstant.RPC_VERSION);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("excludeNodes", excludeNodes);
            params.put("messageBody", RPCUtil.encode(message.serialize()));
            params.put("command", cmd);
            Request request = MessageUtil.newRequest("nw_broadcast", params, Constants.BOOLEAN_FALSE, Constants.ZERO, Constants.ZERO);
            String messageId = ResponseMessageProcessor.requestOnly(ModuleE.NW.abbr, request);
            return messageId.equals("0") ? false : true;
        } catch (IOException e) {
            chain.getLogger().error("message:" + cmd + " failed", e);
            return false;
        } catch (Exception e) {
            chain.getLogger().error("message:" + cmd + " failed", e);
            return false;
        }
    }

    public static boolean broadcast(Chain chain, BaseBusinessMessage message, String cmd) {
        return broadcast(chain, message, null, cmd);
    }
}
