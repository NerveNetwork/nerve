package nerve.network.pocbft.utils.enumeration;

public enum DepositType {
    /**
     * 定期
     * */
    REGULAR((byte)1),

    /**
     * 活期
     * */
    CURRENT((byte)0);

    private final byte code;

    DepositType(byte code){
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
