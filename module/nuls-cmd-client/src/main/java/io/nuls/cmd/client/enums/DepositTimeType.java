package io.nuls.cmd.client.enums;
public enum DepositTimeType {
    /**
     * 定期委托3个月
     * */
    THREE_MONTHS((byte)0, 1.2, 7776000),

    /**
     * 半年
     * */
    HALF_YEAR((byte)1, 1.5, 15552000),

    /**
     * 一年
     * */
    ONE_YEAR((byte)2, 2, 31104000),

    /**
     * 两年
     * */
    TOW_YEARS((byte)3, 2.5, 62208000),

    /**
     * 三年
     * */
    THREE_YEARS((byte)4, 3, 93312000),

    /**
     * 五年
     * */
    FIVE_YEARS((byte)5, 4, 155520000),

    /**
     * 十年
     * */
    TEN_YEARS((byte)6, 5, 311040000);
    private final byte type;

    private final double weight;

    private final long time;

    DepositTimeType(byte type, double weight, long time){
        this.type = type;
        this.weight = weight;
        this.time = time;
    }

    public byte getType() {
        return type;
    }

    public double getWeight() {
        return weight;
    }

    public long getTime() {
        return time;
    }

    /**
     * 根据委托时间类型获取信息
     * @param type  委托时间
     * @return      DepositTimeType
     */
    public static DepositTimeType getValue(byte type){
        for (DepositTimeType depositTimeType : values()) {
            if(depositTimeType.getType()== type){
                return depositTimeType;
            }
        }
        return null;
    }
}
