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

package network.nerve.converter.core.business;

import network.nerve.converter.message.*;
import network.nerve.converter.model.bo.Chain;

/**
 * 消息处理核心业务
 * @author: Loki
 * @date: 2020-02-27
 */
public interface MessageService {

    /**
     * 处理收到新交易hash和签名的消息
     * @param chain   消息所属链Id
     * @param nodeId    发送此消息的节点Id
     * @param message   消息体
     */
    void newHashSign(Chain chain, String nodeId, BroadcastHashSignMessage message);

    /**
     * 处理收到索取完整交易的消息
     * @param chain   消息所属链Id
     * @param nodeId    发送此消息的节点Id
     * @param message   消息体
     */
    void getTx(Chain chain, String nodeId, GetTxMessage message);

    /**
     * 处理收到完整交易的消息
     * @param chain   消息所属链Id
     * @param nodeId    发送此消息的节点Id
     * @param message   消息体
     */
    void receiveTx(Chain chain, String nodeId, NewTxMessage message);

    /**
     * 重新解析(异构链)交易
     * @param chain
     * @param nodeId
     * @param message
     */
    void checkRetryParse(Chain chain, String nodeId, CheckRetryParseMessage message);

    /**
     * 收到异构链签名消息
     * @param chain
     * @param nodeId
     * @param message
     */
    void componentSign(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate);

    /**
     * 取消当前虚拟银行发出的异构链交易
     * @param chain
     * @param nodeId
     * @param cancelHtgTxMessage
     */
    void cancelHtgTx(Chain chain, String nodeId, CancelHtgTxMessage cancelHtgTxMessage);
    /**
     * 重发虚拟银行变更签名消息
     * @param chain
     * @param nodeId
     * @param message
     */
    void retryVirtualBankSign(Chain chain, String nodeId, VirtualBankSignMessage message, boolean isCreate);
}
