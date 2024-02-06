package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:44
 * @Description:
 *  Create consensus nodes
 *  create consensus agent node
 */
public class CreateAgentReq extends BaseReq {

    private String agentAddress;

    private String packingAddress;

    private String rewardAddress;

    private BigInteger deposit;

    private String password;

    public CreateAgentReq(String agentAddress, String packingAddress, String rewardAddress, BigInteger deposit, String password) {
        this.agentAddress = agentAddress;
        this.packingAddress = packingAddress;
        this.rewardAddress = rewardAddress;
        this.deposit = deposit;
        this.password = password;
    }

    public CreateAgentReq(String agentAddress, String packingAddress, String rewardAddress, BigInteger deposit) {
        this.agentAddress = agentAddress;
        this.packingAddress = packingAddress;
        this.rewardAddress = rewardAddress;
        this.deposit = deposit;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(String packingAddress) {
        this.packingAddress = packingAddress;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public BigInteger getDeposit() {
        return deposit;
    }

    public void setDeposit(BigInteger deposit) {
        this.deposit = deposit;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
