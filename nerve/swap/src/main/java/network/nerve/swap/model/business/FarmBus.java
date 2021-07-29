package network.nerve.swap.model.business;

import io.nuls.base.data.NulsHash;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class FarmBus extends BaseBus {

    private NulsHash farmHash;
    private byte[] userAddress;

    private long lastRewardBlockNew;
    private long lastRewardBlockOld;
    private BigInteger accSyrupPerShareNew;
    private BigInteger accSyrupPerShareOld;
    private BigInteger syrupBalanceNew ;
    private BigInteger syrupBalanceOld ;
    private BigInteger stakingBalanceNew ;
    private BigInteger stakingBalanceOld ;

    private BigInteger userAmountNew;
    private BigInteger userAmountOld;
    private BigInteger userRewardDebtNew;
    private BigInteger userRewardDebtOld;

    public long getLastRewardBlockNew() {
        return lastRewardBlockNew;
    }

    public void setLastRewardBlockNew(long lastRewardBlockNew) {
        this.lastRewardBlockNew = lastRewardBlockNew;
    }

    public long getLastRewardBlockOld() {
        return lastRewardBlockOld;
    }

    public void setLastRewardBlockOld(long lastRewardBlockOld) {
        this.lastRewardBlockOld = lastRewardBlockOld;
    }

    public BigInteger getAccSyrupPerShareNew() {
        return accSyrupPerShareNew;
    }

    public void setAccSyrupPerShareNew(BigInteger accSyrupPerShareNew) {
        this.accSyrupPerShareNew = accSyrupPerShareNew;
    }

    public BigInteger getAccSyrupPerShareOld() {
        return accSyrupPerShareOld;
    }

    public void setAccSyrupPerShareOld(BigInteger accSyrupPerShareOld) {
        this.accSyrupPerShareOld = accSyrupPerShareOld;
    }

    public BigInteger getSyrupBalanceNew() {
        return syrupBalanceNew;
    }

    public void setSyrupBalanceNew(BigInteger syrupBalanceNew) {
        this.syrupBalanceNew = syrupBalanceNew;
    }

    public BigInteger getSyrupBalanceOld() {
        return syrupBalanceOld;
    }

    public void setSyrupBalanceOld(BigInteger syrupBalanceOld) {
        this.syrupBalanceOld = syrupBalanceOld;
    }

    public BigInteger getStakingBalanceNew() {
        return stakingBalanceNew;
    }

    public void setStakingBalanceNew(BigInteger stakingBalanceNew) {
        this.stakingBalanceNew = stakingBalanceNew;
    }

    public BigInteger getStakingBalanceOld() {
        return stakingBalanceOld;
    }

    public void setStakingBalanceOld(BigInteger stakingBalanceOld) {
        this.stakingBalanceOld = stakingBalanceOld;
    }

    public BigInteger getUserAmountNew() {
        return userAmountNew;
    }

    public void setUserAmountNew(BigInteger userAmountNew) {
        this.userAmountNew = userAmountNew;
    }

    public BigInteger getUserAmountOld() {
        return userAmountOld;
    }

    public void setUserAmountOld(BigInteger userAmountOld) {
        this.userAmountOld = userAmountOld;
    }

    public BigInteger getUserRewardDebtNew() {
        return userRewardDebtNew;
    }

    public void setUserRewardDebtNew(BigInteger userRewardDebtNew) {
        this.userRewardDebtNew = userRewardDebtNew;
    }

    public BigInteger getUserRewardDebtOld() {
        return userRewardDebtOld;
    }

    public void setUserRewardDebtOld(BigInteger userRewardDebtOld) {
        this.userRewardDebtOld = userRewardDebtOld;
    }

    public NulsHash getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(NulsHash farmHash) {
        this.farmHash = farmHash;
    }

    public byte[] getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(byte[] userAddress) {
        this.userAddress = userAddress;
    }
}
