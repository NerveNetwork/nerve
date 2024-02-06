package network.nerve.swap.model.dto;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

import java.math.BigInteger;

/**
 * @author Niels
 */
@ApiModel(name = "FarmInformation details")
public class FarmInfoDTO {

    @ApiModelProperty(description = "Farm-HASH")
    private String farmHash;

    @ApiModelProperty(description = "Current total amount of pledged assets")
    private double stakeBalance;

    @ApiModelProperty(description = "Current total remaining candy assets")
    private double syrupBalance;

    @ApiModelProperty(description = "Number of reward candy assets per piece")
    private double syrupPerBlock;

    public String getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(String farmHash) {
        this.farmHash = farmHash;
    }

    public double getStakeBalance() {
        return stakeBalance;
    }

    public void setStakeBalance(double stakeBalance) {
        this.stakeBalance = stakeBalance;
    }

    public double getSyrupBalance() {
        return syrupBalance;
    }

    public void setSyrupBalance(double syrupBalance) {
        this.syrupBalance = syrupBalance;
    }

    public double getSyrupPerBlock() {
        return syrupPerBlock;
    }

    public void setSyrupPerBlock(double syrupPerBlock) {
        this.syrupPerBlock = syrupPerBlock;
    }
}
