package io.nuls.api.sup;

import io.nuls.api.constant.config.ApiConfig;
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
@Component
public class NvtPriceProvider implements PriceProvider, InitializingBean {

    @Autowired
    ApiConfig apiConfig;

    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;

    @Override
    public void setURL(String url) {}

    @Override
    public BigDecimal queryPrice(String symbol) {
        return symbolUsdtPriceProviderService.getSymbolPriceForUsdt(apiConfig.getMainSymbol()).getPrice();
    }

    @Override
    public void afterPropertiesSet() throws NulsException {
        symbolUsdtPriceProviderService = SpringLiteContext.getBean(SymbolUsdtPriceProviderService.class);
    }
}
