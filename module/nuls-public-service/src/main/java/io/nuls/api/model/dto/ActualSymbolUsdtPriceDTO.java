package io.nuls.api.model.dto;

import io.nuls.api.model.po.SymbolPrice;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 20:49
 * @Description: 功能描述
 */
public class ActualSymbolUsdtPriceDTO implements SymbolPrice {

    String USDT = "USDT";

    private String symbol;

    private BigDecimal price;

    private int decimal;

    public ActualSymbolUsdtPriceDTO(String symbol,int decimal){
        this.symbol = symbol;
        this.decimal = decimal;
        this.price = BigDecimal.ZERO;
    }

    public ActualSymbolUsdtPriceDTO(String symbol,BigDecimal price,int decimal){
        this.symbol = symbol;
        this.price = price;
        this.decimal = decimal;
    }

    @Override
    public String getCurrency() {
        return USDT;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getDecimal() {
        return decimal;
    }
}
