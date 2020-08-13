package io.nuls.api.sup;

import io.nuls.api.constant.config.ApiConfig;
import io.nuls.api.db.SymbolQuotationPriceService;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-17 14:18
 * @Description: 功能描述
 */
@Deprecated
public class NvtPriceProvider implements PriceProvider, InitializingBean {

    @Autowired
    ApiConfig apiConfig;

    SymbolQuotationPriceService  symbolQuotationPriceService;



    @Override
    public void setURL(String url) {}

    @Override
    public BigDecimal queryPrice(String symbol) {
        return symbolQuotationPriceService.getFreshUsdtPrice(apiConfig.getSymbol()).getPrice();
    }

    @Override
    public void afterPropertiesSet() throws NulsException {
        symbolQuotationPriceService = SpringLiteContext.getBean(SymbolQuotationPriceService.class);
    }
}
