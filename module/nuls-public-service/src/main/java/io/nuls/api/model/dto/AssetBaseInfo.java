package io.nuls.api.model.dto;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 11:44
 * @Description: 资产基础信息
 */
public class AssetBaseInfo extends Asset {

    private String symbol;

    private int decimals;

    /**
     * 合约地址（如果有）
     *
     */
    private String address;

    /**
     * 是否官方认证
     */
    private boolean official;


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isOfficial() {
        return official;
    }

    public void setOfficial(boolean official) {
        this.official = official;
    }
}
