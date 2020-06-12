package io.nuls.economic.nuls.model.bo;

import java.math.BigInteger;
import java.util.List;

/**
 * 节点信息
 * agent info
 *
 * @author tag
 * 2019/7/23
 * */
public class AgentInfo {
    private BigInteger deposit;
    private byte[] rewardAddress;
    private double creditVal;
    public AgentInfo(){}

    public  AgentInfo(BigInteger deposit,byte[] rewardAddress,double creditVal){
        this.deposit = deposit;
        this.rewardAddress = rewardAddress;
        this.creditVal = creditVal;
    }


    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public byte[] getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(byte[] rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public double getCreditVal() {
        return creditVal;
    }

    public void setCreditVal(double creditVal) {
        this.creditVal = creditVal;
    }
}
