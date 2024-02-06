package io.nuls.provider.api.service.entity;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

/**
 * @author Niels
 */
@ApiModel(name = "Pledge information details")
public class FarmUserInfoDTO {
    @ApiModelProperty(description = "Farm-HASH")
    private String farmHash;
    @ApiModelProperty(description = "user address")
    private String userAddress;
    @ApiModelProperty(description = "Pledged amount")
    private double amount;
    @ApiModelProperty(description = "Reward amount to be claimed")
    private double reward;

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public String getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(String farmHash) {
        this.farmHash = farmHash;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
