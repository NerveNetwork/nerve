package io.nuls.consensus.model.dto.input;
/**
 * Create entrusted transaction parameter class
 * Create delegate transaction parameter class
 *
 * @author tag
 * 2018/11/12
 * */
public class CreateDepositDTO {
    /**
     * chainID
     * */
    private int chainId;
    /**
     * account
     * */
    private String address;
    /**
     * Entrusted amount
     * */
    private String deposit;
    /**
     * Account password
     * */
    private String password;
    /**
     * Entrusted asset chainID
     * */
    private int assetChainId;
    /**
     * Entrusted assetsID
     * */
    private int assetId;
    /**
     * Entrustment type 0：current  1：regular
     * */
    private byte depositType;
    /**
     * The type of commission period（Half a year, one year, etc）
     * */
    private byte timeType;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public byte getDepositType() {
        return depositType;
    }

    public void setDepositType(byte depositType) {
        this.depositType = depositType;
    }

    public byte getTimeType() {
        return timeType;
    }

    public void setTimeType(byte timeType) {
        this.timeType = timeType;
    }
}
