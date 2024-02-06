package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:53
 * @Description:
 * Commission consensus
 */
public class AgentDepositChangeReq extends BaseReq {

    String address;

    BigInteger amount;

    String password;

    public AgentDepositChangeReq(String address, BigInteger deposit, String password) {
        this.address = address;
        this.amount = deposit;
        this.password = password;
    }

    public AgentDepositChangeReq(String address, BigInteger deposit) {
        this.address = address;
        this.amount = deposit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
