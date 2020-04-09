package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:53
 * @Description:
 * 委托共识
 */
public class DepositToAgentReq extends BaseReq {

    String address;

    String agentHash;

    BigInteger amount;

    String password;

    public DepositToAgentReq(String address, String agentHash, BigInteger deposit, String password) {
        this.address = address;
        this.agentHash = agentHash;
        this.amount = deposit;
        this.password = password;
    }

    public DepositToAgentReq(String address, String agentHash, BigInteger deposit) {
        this.address = address;
        this.agentHash = agentHash;
        this.amount = deposit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }
}
