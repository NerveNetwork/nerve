package io.nuls.base.api.provider.dex.facode;

import io.nuls.base.api.provider.BaseReq;

import java.math.BigInteger;

public class CoinTradingReq extends BaseReq {

    private String address;

    private String password;

    private int quoteAssetChainId;

    private int quoteAssetId;

    private int baseAssetChainId;

    private int baseAssetId;

    private int scaleQuoteDecimal;

    private int scaleBaseDecimal;

    private BigInteger minQuoteAmount;

    private BigInteger minBaseAmount;

    public CoinTradingReq() {

    }

    public CoinTradingReq(String address, String password, int quoteAssetChainId, int quoteAssetId, int baseAssetChainId, int baseAssetId, int scaleQuoteDecimal, int scaleBaseDecimal, BigInteger minQuoteAmount, BigInteger minBaseAmount) {
        this.address = address;
        this.password = password;
        this.quoteAssetChainId = quoteAssetChainId;
        this.quoteAssetId = quoteAssetId;
        this.baseAssetChainId = baseAssetChainId;
        this.baseAssetId = baseAssetId;
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

    public int getQuoteAssetChainId() {
        return quoteAssetChainId;
    }

    public void setQuoteAssetChainId(int quoteAssetChainId) {
        this.quoteAssetChainId = quoteAssetChainId;
    }

    public int getQuoteAssetId() {
        return quoteAssetId;
    }

    public void setQuoteAssetId(int quoteAssetId) {
        this.quoteAssetId = quoteAssetId;
    }

    public int getBaseAssetChainId() {
        return baseAssetChainId;
    }

    public void setBaseAssetChainId(int baseAssetChainId) {
        this.baseAssetChainId = baseAssetChainId;
    }

    public int getBaseAssetId() {
        return baseAssetId;
    }

    public void setBaseAssetId(int baseAssetId) {
        this.baseAssetId = baseAssetId;
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
