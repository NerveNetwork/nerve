package io.nuls.api.model.po;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 16:22
 * @Description: stack 快照数据
 */
public class StackSnapshootInfo {

    /**
     * 快照日期 时间戳
     */
    private Long day;

    /**
     * 基础利息
     */
    private BigInteger baseInterest;


    /**
     * 参与stack的总量
     */
    private BigInteger stackTotal;


    /**
     * 当日发放收益
     */
    private BigInteger rewardTotal;


    /**
     * 区块高度
     */
    private Long blockHeight;


    /**
     * 创建时间
     */
    private long createTime;

    public long getDay() {
        return day;
    }

    public void setDay(long day) {
        this.day = day;
    }

    public BigInteger getBaseInterest() {
        return baseInterest;
    }

    public void setBaseInterest(BigInteger baseInterest) {
        this.baseInterest = baseInterest;
    }

    public BigInteger getStackTotal() {
        return stackTotal;
    }

    public void setStackTotal(BigInteger stackTotal) {
        this.stackTotal = stackTotal;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }


    public BigInteger getRewardTotal() {
        return rewardTotal;
    }

    public void setRewardTotal(BigInteger rewardTotal) {
        this.rewardTotal = rewardTotal;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }
}
