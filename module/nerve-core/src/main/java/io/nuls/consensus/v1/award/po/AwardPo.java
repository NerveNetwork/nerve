package io.nuls.consensus.v1.award.po;

/**
 * @author Niels
 */
public class AwardPo {

    private long awardedHeight = 0;

    private long endKey;

    private long endHeight;

    private long endTime;

    public long getEndHeight() {
        return endHeight;
    }

    public void setEndHeight(long endHeight) {
        this.endHeight = endHeight;
    }

    public long getAwardedHeight() {
        return awardedHeight;
    }

    public void setAwardedHeight(long awardedHeight) {
        this.awardedHeight = awardedHeight;
    }

    public long getEndKey() {
        return endKey;
    }

    public void setEndKey(long endKey) {
        this.endKey = endKey;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
