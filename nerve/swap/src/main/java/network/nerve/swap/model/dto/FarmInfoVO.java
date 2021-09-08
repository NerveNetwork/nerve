package network.nerve.swap.model.dto;

import io.nuls.core.rpc.model.ApiModel;

/**
 * @author Niels
 */
@ApiModel(name = "Farm信息详情")
public class FarmInfoVO {
    private String farmHash;
    private int stakeTokenChainId;
    private int stakeTokenAssetId;
    private int syrupTokenChainId;
    private int syrupTokenAssetId;
    private String syrupPerBlock;//每个区块奖励的糖果数量
    private long startBlockHeight;//开始计算奖励的高度
    private long lockedTime;//锁定时间，在此时间之前不解锁
    private String creatorAddress;
    private String accSyrupPerShare;//累计每股可分到的奖励数量
    private String syrupTokenBalance;
    private String stakeTokenBalance;
    private String totalSyrupAmount;
    private boolean modifiable; //0不可以修改，1可以修改
    private long withdrawLockTime;

    public String getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(String farmHash) {
        this.farmHash = farmHash;
    }

    public int getStakeTokenChainId() {
        return stakeTokenChainId;
    }

    public void setStakeTokenChainId(int stakeTokenChainId) {
        this.stakeTokenChainId = stakeTokenChainId;
    }

    public int getStakeTokenAssetId() {
        return stakeTokenAssetId;
    }

    public void setStakeTokenAssetId(int stakeTokenAssetId) {
        this.stakeTokenAssetId = stakeTokenAssetId;
    }

    public int getSyrupTokenChainId() {
        return syrupTokenChainId;
    }

    public void setSyrupTokenChainId(int syrupTokenChainId) {
        this.syrupTokenChainId = syrupTokenChainId;
    }

    public int getSyrupTokenAssetId() {
        return syrupTokenAssetId;
    }

    public void setSyrupTokenAssetId(int syrupTokenAssetId) {
        this.syrupTokenAssetId = syrupTokenAssetId;
    }

    public String getSyrupPerBlock() {
        return syrupPerBlock;
    }

    public void setSyrupPerBlock(String syrupPerBlock) {
        this.syrupPerBlock = syrupPerBlock;
    }

    public long getStartBlockHeight() {
        return startBlockHeight;
    }

    public void setStartBlockHeight(long startBlockHeight) {
        this.startBlockHeight = startBlockHeight;
    }

    public long getLockedTime() {
        return lockedTime;
    }

    public void setLockedTime(long lockedTime) {
        this.lockedTime = lockedTime;
    }

    public String getCreatorAddress() {
        return creatorAddress;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public String getAccSyrupPerShare() {
        return accSyrupPerShare;
    }

    public void setAccSyrupPerShare(String accSyrupPerShare) {
        this.accSyrupPerShare = accSyrupPerShare;
    }

    public String getSyrupTokenBalance() {
        return syrupTokenBalance;
    }

    public void setSyrupTokenBalance(String syrupTokenBalance) {
        this.syrupTokenBalance = syrupTokenBalance;
    }

    public String getStakeTokenBalance() {
        return stakeTokenBalance;
    }

    public void setStakeTokenBalance(String stakeTokenBalance) {
        this.stakeTokenBalance = stakeTokenBalance;
    }

    public String getTotalSyrupAmount() {
        return totalSyrupAmount;
    }

    public void setTotalSyrupAmount(String totalSyrupAmount) {
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
