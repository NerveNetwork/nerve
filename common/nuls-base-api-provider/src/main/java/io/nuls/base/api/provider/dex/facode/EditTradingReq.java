package io.nuls.base.api.provider.dex.facode;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

public class EditTradingReq extends BaseReq {

    private String address;

    private String password;

    private String tradingHash;

    private int scaleQuoteDecimal;

    private int scaleBaseDecimal;

    private BigInteger minQuoteAmount;

    private BigInteger minBaseAmount;

    public EditTradingReq() {

    }

    public EditTradingReq(String address, String password, String tradingHash, int scaleQuoteDecimal, int scaleBaseDecimal, BigInteger minQuoteAmount, BigInteger minBaseAmount) {
        this.address = address;
        this.password = password;
        this.tradingHash = tradingHash;
        this.scaleQuoteDecimal = scaleQuoteDecimal;
        this.scaleBaseDecimal = scaleBaseDecimal;
        this.minQuoteAmount = minQuoteAmount;
        this.minBaseAmount = minBaseAmount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(String tradingHash) {
        this.tradingHash = tradingHash;
    }

    public int getScaleQuoteDecimal() {
        return scaleQuoteDecimal;
    }

    public void setScaleQuoteDecimal(int scaleQuoteDecimal) {
        this.scaleQuoteDecimal = scaleQuoteDecimal;
    }

    public int getScaleBaseDecimal() {
        return scaleBaseDecimal;
    }

    public void setScaleBaseDecimal(int scaleBaseDecimal) {
        this.scaleBaseDecimal = scaleBaseDecimal;
    }

    public BigInteger getMinQuoteAmount() {
        return minQuoteAmount;
    }

    public void setMinQuoteAmount(BigInteger minQuoteAmount) {
        this.minQuoteAmount = minQuoteAmount;
    }

    public BigInteger getMinBaseAmount() {
        return minBaseAmount;
    }

    public void setMinBaseAmount(BigInteger minBaseAmount) {
        this.minBaseAmount = minBaseAmount;
    }
}
