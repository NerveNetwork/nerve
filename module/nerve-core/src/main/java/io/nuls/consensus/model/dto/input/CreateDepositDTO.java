package io.nuls.consensus.model.dto.input;
/**
 * 创建委托交易参数类
 * Create delegate transaction parameter class
 *
 * @author tag
 * 2018/11/12
 * */
public class CreateDepositDTO {
    /**
     * 链ID
     * */
    private int chainId;
    /**
     * 账户
     * */
    private String address;
    /**
     * 委托金额
     * */
    private String deposit;
    /**
     * 账户密码
     * */
    private String password;
    /**
     * 委托资产链ID
     * */
    private int assetChainId;
    /**
     * 委托资产ID
     * */
    private int assetId;
    /**
     * 委托类型 0：活期  1：定期
     * */
    private byte depositType;
    /**
     * 委托定期的类型（半年，一年等）
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
