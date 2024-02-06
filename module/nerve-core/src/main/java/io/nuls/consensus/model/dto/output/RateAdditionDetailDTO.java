package io.nuls.consensus.model.dto.output;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import io.nuls.consensus.utils.enumeration.DepositTimeType;

@ApiModel(name = "Interest rate markup corresponding to commission type")
public class RateAdditionDetailDTO {
    @ApiModelProperty(description = "Mortgage type0：Current,1：regular")
    private byte depositType;
    @ApiModelProperty(description = "Regular commission type")
    private byte timeType;
    @ApiModelProperty(description = "Delegate Type Description")
    private String describe;
    @ApiModelProperty(description = "Regular commission interest rate markup")
    private double rateAddition;
    @ApiModelProperty(description = "Total interest rate markup,rateAddition*basicRate")
    private double totalAddition;

    public RateAdditionDetailDTO(byte depositType, double basicRate, DepositTimeType depositTimeType){
        this.depositType = depositType;
        if(depositType == 0){
            this.describe = "current";
            this.rateAddition = 1;
            this.totalAddition = basicRate;
        }else{
            this.timeType = depositTimeType.getType();
            this.describe = depositTimeType.getDescribe();
            this.rateAddition = depositTimeType.getWeight();
            this.totalAddition = basicRate * depositTimeType.getWeight();
        }
    }

    public byte getDepositType() {
        return depositType;
    }

    public void setDepositType(byte depositType) {
        this.depositType = depositType;
    }

    public byte getTimeType() {
        return timeType;
    }

    public void setTimeType(byte timeType) {
        this.timeType = timeType;
    }

    public double getRateAddition() {
        return rateAddition;
    }

    public void setRateAddition(double rateAddition) {
        this.rateAddition = rateAddition;
    }

    public double getTotalAddition() {
        return totalAddition;
    }

    public void setTotalAddition(double totalAddition) {
        this.totalAddition = totalAddition;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}
