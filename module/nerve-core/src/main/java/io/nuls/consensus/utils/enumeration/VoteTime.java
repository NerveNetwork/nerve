package io.nuls.consensus.utils.enumeration;

/**
 * Voting time type
 * Voting time type
 *
 * @author tag
 * 2019/10/29
 */
public enum VoteTime {
    /**
     * Voting data before the current voting round
     * */
    PREVIOUS(0),
    /**
     * Current first stage voting
     * */
    CURRENT_STAGE_ONE(1),
    /**
     * Current second stage voting
     * */
    CURRENT_STAGE_TWO(2),
    /**
     * Voting data after the current voting round
     * */
    FUTURE(4);
    private final byte code;
    VoteTime(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
