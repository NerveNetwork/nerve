package io.nuls.api.model.dto;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020/8/10 12:04
 * @Description:
 */
public class DepositGroupBySymbolSumDTO {

    private int assetChainId;

    private int assetId;

    private String symbol;

    private int decimal;

    private BigInteger amount;

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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }
}
