package io.nuls.consensus.v1.entity;

import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;

/**
 * @author Eva
 */
public class PackingData {

    private MeetingRound round;

    private long packStartTime;

    private MeetingMember member;

    public MeetingRound getRound() {
        return round;
    }

    public void setRound(MeetingRound round) {
        this.round = round;
    }

    public long getPackStartTime() {
        return packStartTime;
    }

    public void setPackStartTime(long packStartTime) {
        this.packStartTime = packStartTime;
    }

    public MeetingMember getMember() {
        return member;
    }

    public void setMember(MeetingMember member) {
        this.member = member;
    }
}
