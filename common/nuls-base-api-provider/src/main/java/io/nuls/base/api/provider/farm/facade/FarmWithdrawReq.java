package io.nuls.base.api.provider.farm.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @author Niels
 */
public class FarmWithdrawReq  extends BaseReq {
    private double amount;
    private String address;
    private String farmHash;
    private String password;

    public FarmWithdrawReq(String address, String farmHash,double amount,String password) {
        this.address = address;
        this.farmHash = farmHash;
        this.password = password;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(String farmHash) {
        this.farmHash = farmHash;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
