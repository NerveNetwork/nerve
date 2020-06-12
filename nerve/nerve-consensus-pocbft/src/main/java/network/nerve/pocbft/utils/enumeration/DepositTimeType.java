package network.nerve.pocbft.utils.enumeration;
public enum DepositTimeType {
    /**
     * 定期委托3个月
     * */
    THREE_MONTHS((byte)0, 1.2, 7776000, "三个月"),

    /**
     * 半年
     * */
    HALF_YEAR((byte)1, 1.5, 15552000, "半年"),

    /**
     * 一年
     * */
    ONE_YEAR((byte)2, 2, 31104000, "一年"),

    /**
     * 两年
     * */
    TOW_YEARS((byte)3, 2.5, 62208000, "两年"),

    /**
     * 三年
     * */
    THREE_YEARS((byte)4, 3, 93312000, "三年"),

    /**
     * 五年
     * */
    FIVE_YEARS((byte)5, 4, 155520000, "五年"),

    /**
     * 十年
     * */
    TEN_YEARS((byte)6, 5, 311040000, "十年"),

    /**
     * 测试
     * todo 用于测试，减少等待时间，在正式上线前干掉
     * */
    TEST((byte)7, 5, 300, "五分钟");
    private final byte type;

    private final double weight;

    private final long time;

    private final String describe;

    DepositTimeType(byte type, double weight, long time, String describe){
        this.type = type;
        this.weight = weight;
        this.time = time;
        this.describe = describe;
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

    public String getDescribe() {
        return describe;
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
