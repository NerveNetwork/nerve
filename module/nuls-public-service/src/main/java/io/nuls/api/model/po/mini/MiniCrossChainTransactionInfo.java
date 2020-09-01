package io.nuls.api.model.po.mini;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-27 19:00
 * @Description: 功能描述
 */
public class MiniCrossChainTransactionInfo extends MiniTransactionInfo {

    private String outerTxHash;

    private String crossChainType;

    private String converterType;

    private String symbol;

    private String network;

    private int decimals;

    private String icon;

    private BigInteger usdValue;

    public BigInteger getUsdValue() {
        return usdValue;
    }

    public void setUsdValue(BigInteger usdValue) {
        this.usdValue = usdValue;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getOuterTxHash() {
        return outerTxHash;
    }

    public void setOuterTxHash(String outerTxHash) {
        this.outerTxHash = outerTxHash;
    }

    public String getCrossChainType() {
        return crossChainType;
    }

    public void setCrossChainType(String crossChainType) {
        this.crossChainType = crossChainType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getConverterType() {
        return converterType;
    }

    public void setConverterType(String converterType) {
        this.converterType = converterType;
    }
}
