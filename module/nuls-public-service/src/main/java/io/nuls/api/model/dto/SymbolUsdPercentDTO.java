package io.nuls.api.model.dto;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-17 14:55
 * @Description: 功能描述
 */
public class SymbolUsdPercentDTO {

    /**
     * 列表总的usd价值
     */
    BigDecimal totalUsdVal;

    /**
     * 目标项的USD价值
     */
    BigDecimal usdVal;

    /**
     * 目标项在列表中的占比
     * 1 为 100%
     */
    BigDecimal per;

    public BigDecimal getTotalUsdVal() {
        return totalUsdVal;
    }

    public void setTotalUsdVal(BigDecimal totalUsdVal) {
        this.totalUsdVal = totalUsdVal;
    }

    public BigDecimal getUsdVal() {
        return usdVal;
    }

    public void setUsdVal(BigDecimal usdVal) {
        this.usdVal = usdVal;
    }

    public BigDecimal getPer() {
        return per;
    }

    public void setPer(BigDecimal per) {
        this.per = per;
    }
}
