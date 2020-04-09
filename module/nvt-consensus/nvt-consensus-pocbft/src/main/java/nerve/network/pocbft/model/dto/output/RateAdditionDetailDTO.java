package nerve.network.pocbft.model.dto.output;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import nerve.network.pocbft.utils.enumeration.DepositTimeType;

@ApiModel(name = "委托类型对应的利率加成")
public class RateAdditionDetailDTO {
    @ApiModelProperty(description = "抵押类型0：活期，1：定期")
    private byte depositType;
    @ApiModelProperty(description = "定期委托类型")
    private byte timeType;
    @ApiModelProperty(description = "委托类型描述")
    private String describe;
    @ApiModelProperty(description = "定期委托利率加成")
    private double rateAddition;
    @ApiModelProperty(description = "总利率加成，rateAddition*basicRate")
    private double totalAddition;

    public RateAdditionDetailDTO(byte depositType, double basicRate, DepositTimeType depositTimeType){
        this.depositType = depositType;
        if(depositType == 0){
            this.describe = "活期";
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
