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

    public ActualSymbolUsdtPriceDTO(String symbol){
        this.symbol = symbol;
        this.price = BigDecimal.ZERO;
    }

    public ActualSymbolUsdtPriceDTO(String symbol,BigDecimal price){
        this.symbol = symbol;
        this.price = price;
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
}
