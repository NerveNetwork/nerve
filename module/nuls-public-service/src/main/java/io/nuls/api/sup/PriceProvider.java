package io.nuls.api.sup;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:56
 * @Description: 功能描述
 */
public interface PriceProvider {

    void setURL(String url);

    BigDecimal queryPrice(String symbol);

}
