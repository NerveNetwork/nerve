package io.nuls.api.constant;


/**
 * 定期stacking 的定期类型
 */
public enum DepositFixedType {

    /**
     * 活期
     */
    NONE((byte)-1,0),

    /**
     * 定期委托3个月
     * */
    THREE_MONTHS((byte)0, 7776000),

    /**
     * 半年
     * */
    HALF_YEAR((byte)1, 15552000),

    /**
     * 一年
     * */
    ONE_YEAR((byte)2,  31104000),

    /**
     * 两年
     * */
    TOW_YEARS((byte)3, 62208000),

    /**
     * 三年
     * */
    THREE_YEARS((byte)4, 93312000),

    /**
     * 五年
     * */
    FIVE_YEARS((byte)5, 155520000),

//    TEST((byte)7,  300),

    /**
     * 十年
     * */
    TEN_YEARS((byte)6, 311040000);

    private final byte type;

    private final long time;

    DepositFixedType(byte type, long time){
        this.type = type;
        this.time = time;
    }

    public byte getType() {
        return type;
    }


    public long getTime() {
        return time;
    }

    /**
     * 根据委托时间类型获取信息
     * @param type  委托时间
     * @return      DepositTimeType
     */
    public static DepositFixedType getValue(byte type){
        for (DepositFixedType depositTimeType : values()) {
            if(depositTimeType.getType()== type){
                return depositTimeType;
            }
        }
        return null;
    }
}
