package io.nuls.provider.api.service.entity;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

/**
 * @author Niels
 */
@ApiModel(name = "Farm信息详情")
public class FarmInfoDTO {

    @ApiModelProperty(description = "Farm-HASH")
    private String farmHash;

    @ApiModelProperty(description = "当前质押资产总量")
    private double stakeBalance;

    @ApiModelProperty(description = "当前剩余糖果资产总量")
    private double syrupBalance;

    @ApiModelProperty(description = "每块奖励糖果资产数量")
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
