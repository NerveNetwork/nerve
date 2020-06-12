package io.nuls.economic.nuls.model.bo;
/**
 * 轮次相关信息
 * Round related information
 *
 * @author tag
 * 2019/7/23
 * */
public class RoundInfo {
    private long roundStartTime;
    private int memberCount;

    public RoundInfo(){}

    public RoundInfo(long roundStartTime,int memberCount){
        this.roundStartTime = roundStartTime;
        this.memberCount = memberCount;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }


    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
}
