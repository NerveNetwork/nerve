package io.nuls.consensus.utils.enumeration;

public enum DepositType {
    /**
     * regular
     * */
    REGULAR((byte)1),

    /**
     * current
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
