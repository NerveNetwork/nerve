package network.nerve.swap.model.dto;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

import java.math.BigInteger;

/**
 * @author Niels
 */
@ApiModel(name = "质押信息详情")
public class FarmUserInfoDTO {
    @ApiModelProperty(description = "Farm-HASH")
    private String farmHash;
    @ApiModelProperty(description = "user address")
    private String userAddress;
    @ApiModelProperty(description = "已质押金额")
    private double amount;
    @ApiModelProperty(description = "待领取奖励金额")
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
