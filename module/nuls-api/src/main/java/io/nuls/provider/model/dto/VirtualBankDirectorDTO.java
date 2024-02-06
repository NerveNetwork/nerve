package io.nuls.provider.model.dto;

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
