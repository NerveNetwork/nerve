package io.nuls.base.api.provider.farm.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @author Niels
 */
public class FarmStakeReq  extends BaseReq {
    private String address;
    private String farmHash;
    private double amount;
    private String password;

    public FarmStakeReq(String address, String farmHash, double amount,String password) {
        this.address = address;
        this.farmHash = farmHash;
        this.amount = amount;
        this.password = password;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
