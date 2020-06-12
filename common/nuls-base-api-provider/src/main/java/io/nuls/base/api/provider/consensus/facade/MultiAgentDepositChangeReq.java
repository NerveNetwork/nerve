package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

public class MultiAgentDepositChangeReq extends BaseReq {
    String address;

    BigInteger amount;

    String password;

    private String signAddress;

    public  MultiAgentDepositChangeReq(String address, BigInteger deposit){
        this.address = address;
        this.amount = deposit;
    }

    public  MultiAgentDepositChangeReq(String address, BigInteger deposit,String signAddress, String password){
        this.address = address;
        this.amount = deposit;
        this.signAddress = signAddress;
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }
}
