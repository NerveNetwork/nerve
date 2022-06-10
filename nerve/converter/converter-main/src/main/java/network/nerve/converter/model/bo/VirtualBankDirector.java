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

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 虚拟银行成员
 * @author: Loki
 * @date: 2020-02-28
 */
public class VirtualBankDirector implements Serializable {
    /**
     * 节点hash
     */
    private String agentHash;
    /**
     * 签名地址（节点打包地址）
     */
    private String signAddress;
    /**
     * 签名地址 公钥
     */
    private String signAddrPubKey;

    /**
     * 节点地址
     */
    private String agentAddress;

    /**
     * 奖励地址
     */
    private String rewardAddress;

    /**
     * 是否是种子节点
     */
    private boolean seedNode;

    /**
     * 节点提交的所持有的异构链地址
     * K:异构链在NERVE中的chainId, V:异构链地址
     */
    private Map<Integer, HeterogeneousAddress> heterogeneousAddrMap;

    /**
     * 节点加入虚拟银行时的顺序
     */
    private int order;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }

    public Map<Integer, HeterogeneousAddress> getHeterogeneousAddrMap() {
        return heterogeneousAddrMap;
    }

    public void setHeterogeneousAddrMap(Map<Integer, HeterogeneousAddress> heterogeneousAddrMap) {
        this.heterogeneousAddrMap = heterogeneousAddrMap;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public boolean getSeedNode() {
        return seedNode;
    }

    public void setSeedNode(boolean seedNode) {
        this.seedNode = seedNode;
    }

    public String getSignAddrPubKey() {
        return signAddrPubKey;
    }

    public void setSignAddrPubKey(String signAddrPubKey) {
        this.signAddrPubKey = signAddrPubKey;
    }

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VirtualBankDirector that = (VirtualBankDirector) o;
        return Objects.equals(agentAddress, that.agentAddress);
    }

    @Override
    public int hashCode() {
        return agentAddress != null ? agentAddress.hashCode() : 0;
    }
}
