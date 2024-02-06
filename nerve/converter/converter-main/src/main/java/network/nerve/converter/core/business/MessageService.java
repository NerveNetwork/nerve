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
 * Message processing core business
 * @author: Loki
 * @date: 2020-02-27
 */
public interface MessageService {

    /**
     * Processing new transactions receivedhashMessage with signature
     * @param chain   Chain to which the message belongsId
     * @param nodeId    The node that sent this messageId
     * @param message   Message Body
     */
    void newHashSign(Chain chain, String nodeId, BroadcastHashSignMessage message);

    /**
     * Processing messages requesting complete transactions
     * @param chain   Chain to which the message belongsId
     * @param nodeId    The node that sent this messageId
     * @param message   Message Body
     */
    void getTx(Chain chain, String nodeId, GetTxMessage message);

    /**
     * Processing messages that receive complete transactions
     * @param chain   Chain to which the message belongsId
     * @param nodeId    The node that sent this messageId
     * @param message   Message Body
     */
    void receiveTx(Chain chain, String nodeId, NewTxMessage message);

    /**
     * Re parsing(Heterogeneous chain)transaction
     * @param chain
     * @param nodeId
     * @param message
     */
    void checkRetryParse(Chain chain, String nodeId, CheckRetryParseMessage message);

    /**
     * Received heterogeneous chain signature message
     * @param chain
     * @param nodeId
     * @param message
     */
    void componentSign(Chain chain, String nodeId, ComponentSignMessage message, boolean isCreate);

    /**
     * Cancel heterogeneous chain transactions issued by the current virtual bank
     * @param chain
     * @param nodeId
     * @param cancelHtgTxMessage
     */
    void cancelHtgTx(Chain chain, String nodeId, CancelHtgTxMessage cancelHtgTxMessage);
    /**
     * Resend virtual bank signature change message
     * @param chain
     * @param nodeId
     * @param message
     */
    void retryVirtualBankSign(Chain chain, String nodeId, VirtualBankSignMessage message, boolean isCreate);
}
