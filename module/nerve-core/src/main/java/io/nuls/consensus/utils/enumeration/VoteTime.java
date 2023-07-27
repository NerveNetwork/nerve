package io.nuls.consensus.utils.enumeration;

/**
 * 投票时间类型
 * Voting time type
 *
 * @author tag
 * 2019/10/29
 */
public enum VoteTime {
    /**
     * 当前投票轮次之前的投票数据
     * */
    PREVIOUS(0),
    /**
     * 当前第一阶段投票
     * */
    CURRENT_STAGE_ONE(1),
    /**
     * 当前第二阶段投票
     * */
    CURRENT_STAGE_TWO(2),
    /**
     * 当前投票轮次之后的投票数据
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
