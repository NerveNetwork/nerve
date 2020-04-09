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

package nerve.network.converter.model.dto;

import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.model.bo.VirtualBankDirector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Chino
 * @date: 2020/3/17
 */
public class VirtualBankDirectorDTO {

    /**
     * 签名地址（节点打包地址）
     */
    private String signAddress;
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
     * 异构链地址集合
     */
    List<HeterogeneousAddress> heterogeneousAddresses;

    public VirtualBankDirectorDTO() {
    }

    public VirtualBankDirectorDTO(VirtualBankDirector director) {
        this.signAddress = director.getSignAddress();
        this.agentAddress = director.getAgentAddress();
        this.seedNode = director.getSeedNode();
        this.rewardAddress = director.getRewardAddress();
        this.heterogeneousAddresses = new ArrayList<>(director.getHeterogeneousAddrMap().values());
    }

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public boolean isSeedNode() {
        return seedNode;
    }

    public void setSeedNode(boolean seedNode) {
        this.seedNode = seedNode;
    }

    public List<HeterogeneousAddress> getHeterogeneousAddresses() {
        return heterogeneousAddresses;
    }

    public void setHeterogeneousAddresses(List<HeterogeneousAddress> heterogeneousAddresses) {
        this.heterogeneousAddresses = heterogeneousAddresses;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

}
