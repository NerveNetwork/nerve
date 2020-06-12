package io.nuls.api.sup;

import io.nuls.core.core.annotation.Component;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-02 16:20
 * @Description: 功能描述
 */
@Component
public class UsdtPriceProvider implements PriceProvider {

    @Override
    public void setURL(String url) {

    }

    @Override
    public BigDecimal queryPrice(String symbol) {
        return BigDecimal.ONE;
    }
}
