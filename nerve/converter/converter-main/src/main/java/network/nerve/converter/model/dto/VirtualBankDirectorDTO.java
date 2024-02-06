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

package network.nerve.converter.model.dto;

import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.VirtualBankDirector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020/3/17
 */
public class VirtualBankDirectorDTO {

    /**
     * Signature address（Node packaging address）
     */
    private String signAddress;
    /**
     * Node address
     */
    private String agentAddress;
    /**
     * Reward Address
     */
    private String rewardAddress;

    /**
     * Is it a seed node
     */
    private boolean seedNode;

    /**
     * Heterogeneous chain address set
     */
    List<HeterogeneousAddressDTO> heterogeneousAddresses;

    /**
     * Order of nodes joining virtual banks
     */
    private int order;

    public VirtualBankDirectorDTO() {
    }

    public VirtualBankDirectorDTO(VirtualBankDirector director) {
        this.signAddress = director.getSignAddress();
        this.agentAddress = director.getAgentAddress();
        this.seedNode = director.getSeedNode();
        this.rewardAddress = director.getRewardAddress();
        this.heterogeneousAddresses = new ArrayList<>();
        this.order = director.getOrder();
        for(HeterogeneousAddress heterogeneousAddress :  director.getHeterogeneousAddrMap().values()){
            heterogeneousAddresses.add(new HeterogeneousAddressDTO(heterogeneousAddress));
        }
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

    public List<HeterogeneousAddressDTO> getHeterogeneousAddresses() {
        return heterogeneousAddresses;
    }

    public void setHeterogeneousAddresses(List<HeterogeneousAddressDTO> heterogeneousAddresses) {
        this.heterogeneousAddresses = heterogeneousAddresses;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
