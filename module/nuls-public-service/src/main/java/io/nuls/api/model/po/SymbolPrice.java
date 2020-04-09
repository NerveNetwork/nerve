package io.nuls.api.model.po;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:18
 * @Description: 币种价格
 */
public interface SymbolPrice {

    int DEFAULT_SCALE = 8;

    String getCurrency();

    BigDecimal getPrice();

    String getSymbol();

    /**
     * 将目标symbol的数量转换成当前symbol的数量
     * @param source
     * @param amount
     * @return
     */
    default BigDecimal transfer(SymbolPrice source, BigDecimal amount){
        Objects.requireNonNull(source,"source price info can't be null");
        Objects.requireNonNull(amount);
        if(!source.getCurrency().equals(this.getCurrency())){
            throw new IllegalArgumentException("source和target计价货币必须一致");
        }
        if(source.getPrice() == null){
            return BigDecimal.ZERO;
        }
        //计算源symbol的usdt价值
        BigDecimal sourceUsdtAmount = source.getPrice().multiply(amount);
        if(sourceUsdtAmount.equals(BigDecimal.ZERO)){
            return BigDecimal.ZERO;
        }
        if(getPrice() == null || getPrice().equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        //计算目标symbol的数量
        BigDecimal targetUsdtAmount = sourceUsdtAmount.divide(getPrice(), MathContext.DECIMAL64);
        return targetUsdtAmount.setScale(DEFAULT_SCALE, RoundingMode.HALF_DOWN);
    }

}
