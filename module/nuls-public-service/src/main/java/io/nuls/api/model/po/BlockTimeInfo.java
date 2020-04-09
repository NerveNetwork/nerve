package io.nuls.api.model.po;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-04 16:09
 * @Description: 功能描述
 */
public class BlockTimeInfo {

    /**
     * chainId
     */
    private int chainId;

    /**
     * 平均出块消耗时间
     */
    private long avgConsumeTime;

    /**
     * 最后一次出块消耗时间
     */
    private long lastConsumeTime;

    /**
     * 当前高度
     */
    private long blockHeight;

    private long lastBlockTimeStamp;

    public long getLastBlockTimeStamp() {
        return lastBlockTimeStamp;
    }

    public void setLastBlockTimeStamp(long lastBlockTimeStamp) {
        this.lastBlockTimeStamp = lastBlockTimeStamp;
    }

    public long getAvgConsumeTime() {
        return avgConsumeTime;
    }

    public void setAvgConsumeTime(long avgConsumeTime) {
        this.avgConsumeTime = avgConsumeTime;
    }

    public long getLastConsumeTime() {
        return lastConsumeTime;
    }

    public void setLastConsumeTime(long lastConsumeTime) {
        this.lastConsumeTime = lastConsumeTime;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }


    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }


    @Override
    public int hashCode() {
        return this.chainId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockTimeInfo)) return false;
        BlockTimeInfo that = (BlockTimeInfo) o;
        return chainId == that.chainId;
    }
}
