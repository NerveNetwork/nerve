package io.nuls.cmd.client.enums;
public enum DepositTimeType {

//    TEST((byte)7, 5, 300),

    /**
     * Regular commission3Months
     * */
    THREE_MONTHS((byte)0, 1.2, 7776000),

    /**
     * Half a year
     * */
    HALF_YEAR((byte)1, 1.5, 15552000),
    /**
     * One year
     * */
    ONE_YEAR((byte)2, 2, 31104000),

    /**
     * two years
     * */
    TOW_YEARS((byte)3, 2.5, 62208000),

    /**
     * Three years
     * */
    THREE_YEARS((byte)4, 3, 93312000),

    /**
     * five years
     * */
    FIVE_YEARS((byte)5, 4, 155520000),

    /**
     * Ten years
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
