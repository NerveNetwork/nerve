package io.nuls.consensus.utils.enumeration;
public enum DepositTimeType {

//    TEST((byte)7, 5, 300, "5minute"),

    /**
     * Regular commission3Months
     * */
    THREE_MONTHS((byte)0, 1.2, 7776000, "Three months"),

    /**
     * Half a year
     * */
    HALF_YEAR((byte)1, 1.5, 15552000, "Half a year"),

    /**
     * One year
     * */
    ONE_YEAR((byte)2, 2, 31104000, "One year"),

    /**
     * two years
     * */
    TOW_YEARS((byte)3, 2.5, 62208000, "two years"),

    /**
     * Three years
     * */
    THREE_YEARS((byte)4, 3, 93312000, "Three years"),

    /**
     * five years
     * */
    FIVE_YEARS((byte)5, 4, 155520000, "five years"),

    /**
     * Ten years
     * */
    TEN_YEARS((byte)6, 5, 311040000, "Ten years") ;
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
     * Obtain information based on the type of delegation time
     * @param type  Entrustment time
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
