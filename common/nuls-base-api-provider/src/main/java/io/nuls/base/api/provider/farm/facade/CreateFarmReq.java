package io.nuls.base.api.provider.farm.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @author Niels
 */
public class CreateFarmReq extends BaseReq {
    private String address;
    private String stakeTokenStr;
    private String syrupTokenStr;
    private double syrupPerBlock;
    private long startHeight;
    private long lockedHeight;
    private String password;
    private double totalSyrupAmount;
    private boolean modifiable;
    private long withdrawLockTime;

    public CreateFarmReq(String address, String stakeTokenStr, String syrupTokenStr, double totalSyrupAmount, double syrupPerBlock, long startHeight, long lockedHeight, boolean modifiable, long withdrawLockTime, String password) {
        this.address = address;
        this.stakeTokenStr = stakeTokenStr;
        this.syrupPerBlock = syrupPerBlock;
        this.syrupTokenStr = syrupTokenStr;
        this.startHeight = startHeight;
        this.lockedHeight = lockedHeight;
        this.password = password;
        this.totalSyrupAmount = totalSyrupAmount;
        this.modifiable = modifiable;
        this.withdrawLockTime = withdrawLockTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStakeTokenStr() {
        return stakeTokenStr;
    }

    public void setStakeTokenStr(String stakeTokenStr) {
        this.stakeTokenStr = stakeTokenStr;
    }

    public String getSyrupTokenStr() {
        return syrupTokenStr;
    }

    public void setSyrupTokenStr(String syrupTokenStr) {
        this.syrupTokenStr = syrupTokenStr;
    }

    public double getSyrupPerBlock() {
        return syrupPerBlock;
    }

    public void setSyrupPerBlock(double syrupPerBlock) {
        this.syrupPerBlock = syrupPerBlock;
    }

    public long getStartHeight() {
        return startHeight;
    }

    public void setStartHeight(long startHeight) {
        this.startHeight = startHeight;
    }

    public long getLockedHeight() {
        return lockedHeight;
    }

    public void setLockedHeight(long lockedHeight) {
        this.lockedHeight = lockedHeight;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getTotalSyrupAmount() {
        return totalSyrupAmount;
    }

    public void setTotalSyrupAmount(double totalSyrupAmount) {
        this.totalSyrupAmount = totalSyrupAmount;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    public long getWithdrawLockTime() {
        return withdrawLockTime;
    }

    public void setWithdrawLockTime(long withdrawLockTime) {
        this.withdrawLockTime = withdrawLockTime;
    }
}
