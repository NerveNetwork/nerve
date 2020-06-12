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

package network.nerve.converter.core.thread.handler;

import network.nerve.converter.core.processor.TransactionMsgProcessor;
import network.nerve.converter.message.BroadcastHashSignMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.UntreatedMessage;

/**
 * @author: Loki
 * @date: 2020/4/14
 */
public class SignMessageByzantineHandler implements Runnable {

    private Chain chain;

    public SignMessageByzantineHandler(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (chain.getSignMessageByzantineQueue() != null) {
            try {
                UntreatedMessage untreatedMessage = chain.getSignMessageByzantineQueue().take();
                String nativeHex = untreatedMessage.getCacheHash().toHex();
                chain.getLogger().debug("[处理线程]开始对节点[{}]广播的交易签名消息拜占庭验证, Hash：{}", untreatedMessage.getNodeId(), nativeHex);
                TransactionMsgProcessor.handleSignMessageByzantine(
                        chain,
                        untreatedMessage.getCacheHash(),
                        untreatedMessage.getNodeId(),
                        (BroadcastHashSignMessage)untreatedMessage.getMessage(),
                        nativeHex);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }
}
