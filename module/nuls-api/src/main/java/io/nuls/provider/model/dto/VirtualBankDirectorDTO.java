package io.nuls.provider.model.dto;

import java.util.List;

/**
 * @author: Loki
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
    List<HeterogeneousAddressDTO> heterogeneousAddresses;

    /**
     * 节点加入虚拟银行时的顺序
     */
    private int order;

    public VirtualBankDirectorDTO() {
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