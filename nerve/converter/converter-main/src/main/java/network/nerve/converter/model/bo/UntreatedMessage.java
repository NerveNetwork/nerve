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

package network.nerve.converter.model.bo;

import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;

/**
 * 还未处理的交易消息
 *
 * @author: Loki
 * @date: 2020/4/13
 */
public class UntreatedMessage {
    private int chainId;
    private String nodeId;
    private BaseBusinessMessage message;
    private NulsHash cacheHash;

    public UntreatedMessage() {

    }

    public UntreatedMessage(int chainId, String nodeId, BaseBusinessMessage baseMessage, NulsHash cacheHash) {
        this.chainId = chainId;
        this.nodeId = nodeId;
        this.message = baseMessage;
        this.cacheHash = cacheHash;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public BaseBusinessMessage getMessage() {
        return message;
    }

    public void setMessage(BaseBusinessMessage message) {
        this.message = message;
    }

    public NulsHash getCacheHash() {
        return cacheHash;
    }

    public void setCacheHash(NulsHash cacheHash) {
        this.cacheHash = cacheHash;
    }
}
