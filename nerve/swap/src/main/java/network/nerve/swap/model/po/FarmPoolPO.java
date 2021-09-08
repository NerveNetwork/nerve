package network.nerve.swap.model.po;

import io.nuls.base.data.NulsHash;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.NerveToken;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class FarmPoolPO {

    private NulsHash farmHash;
    private NerveToken stakeToken;
    private NerveToken syrupToken;
    private BigInteger syrupPerBlock;//每个区块奖励的糖果数量
    private long startBlockHeight;//开始计算奖励的高度
    private long lockedTime;//锁定时间，在此时间之前不解锁
    private long lastRewardBlock; // 最近计算过激励的区块高度
    private byte[] creatorAddress;
    private BigInteger accSyrupPerShare;//累计每股可分到的奖励数量
    private BigInteger syrupTokenBalance = BigInteger.ZERO;
    private BigInteger stakeTokenBalance = BigInteger.ZERO;
    private BigInteger totalSyrupAmount = BigInteger.ZERO;
    private boolean modifiable; //0不可以修改，1可以修改
    private long withdrawLockTime;

    public BigInteger getTotalSyrupAmount() {
        return totalSyrupAmount;
    }

    public void setTotalSyrupAmount(BigInteger totalSyrupAmount) {
        this.totalSyrupAmount = totalSyrupAmount;
    }

    public BigInteger getSyrupTokenBalance() {
        return syrupTokenBalance;
    }

    public void setSyrupTokenBalance(BigInteger syrupTokenBalance) {
        this.syrupTokenBalance = syrupTokenBalance;
    }

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
    }

    public NerveToken getStakeToken() {
        return stakeToken;
    }

    public void setStakeToken(NerveToken stakeToken) {
        this.stakeToken = stakeToken;
    }

    public NerveToken getSyrupToken() {
        return syrupToken;
    }

    public void setSyrupToken(NerveToken syrupToken) {
        this.syrupToken = syrupToken;
    }

    public BigInteger getSyrupPerBlock() {
        return syrupPerBlock;
    }

    public void setSyrupPerBlock(BigInteger syrupPerBlock) {
        this.syrupPerBlock = syrupPerBlock;
    }

    public long getStartBlockHeight() {
        return startBlockHeight;
    }

    public void setStartBlockHeight(long startBlockHeight) {
        this.startBlockHeight = startBlockHeight;
    }

    public byte[] getCreatorAddress() {
        return creatorAddress;
    }

    public void setCreatorAddress(byte[] creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public long getLockedTime() {
        return lockedTime;
    }

    public void setLockedTime(long lockedTime) {
        this.lockedTime = lockedTime;
    }

    public long getLastRewardBlock() {
        return lastRewardBlock;
    }

    public void setLastRewardBlock(long lastRewardBlock) {
        this.lastRewardBlock = lastRewardBlock;
    }

    public BigInteger getAccSyrupPerShare() {
        return accSyrupPerShare;
    }

    public void setAccSyrupPerShare(BigInteger accSyrupPerShare) {
        this.accSyrupPerShare = accSyrupPerShare;
    }

    public BigInteger getStakeTokenBalance() {
        return stakeTokenBalance;
    }

    public void setStakeTokenBalance(BigInteger stakeTokenBalance) {
        this.stakeTokenBalance = stakeTokenBalance;
    }

    public long getWithdrawLockTime() {
        return withdrawLockTime;
    }

    public void setWithdrawLockTime(long withdrawLockTime) {
        this.withdrawLockTime = withdrawLockTime;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    public FarmPoolPO copy() {
        FarmPoolPO po = new FarmPoolPO();
        po.setLockedTime(this.getLockedTime());
        po.setSyrupToken(this.syrupToken);
        po.setSyrupPerBlock(this.syrupPerBlock);
        po.setStartBlockHeight(this.startBlockHeight);
        po.setStakeToken(this.stakeToken);
        po.setLastRewardBlock(this.lastRewardBlock);
        po.setFarmHash(this.farmHash);
        po.setCreatorAddress(this.creatorAddress);
        po.setAccSyrupPerShare(this.accSyrupPerShare);
        po.setSyrupTokenBalance(this.syrupTokenBalance);
        po.setStakeTokenBalance(this.stakeTokenBalance);
        po.setTotalSyrupAmount(this.totalSyrupAmount);
        po.setWithdrawLockTime(this.withdrawLockTime);
        po.setModifiable(this.modifiable);
        return po;
    }

}
