package io.nuls.consensus.model.bo;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Details of current inflation stage
 * Details of the current inflation stage
 *
 * @author tag
 * 2019/7/23
 * */
public class InflationInfo {
    private long startHeight;
    private long endHeight;
    private BigInteger inflationAmount ;
    private double awardUnit;

    public long getStartHeight() {
        return startHeight;
    }

    public void setStartHeight(long startHeight) {
        this.startHeight = startHeight;
    }

    public long getEndHeight() {
        return endHeight;
    }

    public void setEndHeight(long endHeight) {
        this.endHeight = endHeight;
    }

    public BigInteger getInflationAmount() {
        return inflationAmount;
    }

    public void setInflationAmount(BigInteger inflationAmount) {
        this.inflationAmount = inflationAmount;
    }

    public double getAwardUnit() {
        return awardUnit;
    }

    public void setAwardUnit(double awardUnit) {
        this.awardUnit = awardUnit;
    }
}
