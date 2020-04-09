package io.nuls.api.service.impl;

import io.nuls.api.constant.config.SymbolPriceProviderSourceConfig;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.ActualSymbolUsdtPriceDTO;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:41
 * @Description: 币种USDT价格指数提供器的一部分，负责缓存和获取最新的价格
 */
@Component
public class SymbolUsdtPriceProviderServiceImpl implements SymbolUsdtPriceProviderService, InitializingBean {

    private boolean inited = false;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    SymbolPriceProviderSourceConfig config;

    private Map<String,ActualSymbolUsdtPriceDTO> symbolList = new HashMap<>();

    private Set<String> supportAssetList = new HashSet<>();

    public void doQuery(){
        List<SymbolPriceProviderSourceConfig.Source> list = config.getSymbolPriceProviderSource();
        symbolList.entrySet().forEach(entry->{
            String symbol = entry.getKey();
            ActualSymbolUsdtPriceDTO symbolPrice = entry.getValue();
            List<BigDecimal> collect = new ArrayList<>();
            for (SymbolPriceProviderSourceConfig.Source provider: list ) {
                if((!provider.getMatchSet().isEmpty()) && !provider.getMatchSet().contains(symbol)){
                    continue;
                }
                if( !provider.getExcludeSet().isEmpty() && provider.getExcludeSet().contains(symbol)){
                    continue;
                }
                BigDecimal price = provider.getProvider().queryPrice(symbol);
                if(!price.equals(BigDecimal.ZERO)){
                    collect.add(price);
                }
            }
            BigDecimal total = collect.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            if(total.equals(BigDecimal.ZERO)){
                symbolPrice.setPrice(BigDecimal.ZERO);
            }else{
                symbolPrice.setPrice(collect.size() == 1 ? total : total.divide(new BigDecimal(collect.size()),SymbolPrice.DEFAULT_SCALE, RoundingMode.HALF_DOWN));
            }
            LoggerUtil.PRICE_PROVIDER_LOG.info("更新{}价格{}USDT",symbolPrice.getSymbol(),symbolPrice.getPrice());
        });
    }

    @Override
    public void regSymbol(SymbolRegInfo symbolRegInfo) {
        symbolRegInfo.buildId();
        symbolRegService.save(symbolRegInfo);
        this.symbolList.put(symbolRegInfo.getSymbol(),new ActualSymbolUsdtPriceDTO(symbolRegInfo.getSymbol()));
    }

    @Override
    public SymbolPrice getSymbolPriceForUsdt(String symbol) {
        return symbolList.containsKey(symbol) ? symbolList.get(symbol) : new ActualSymbolUsdtPriceDTO(symbol);
    }

    public void init() {
        symbolRegService.getAll().forEach(d->{
            symbolList.put(d.getSymbol(),new ActualSymbolUsdtPriceDTO(d.getSymbol()));
        });
        this.setInited(true);
    }

    public boolean isInited() {
        return inited;
    }

    public void setInited(boolean inited) {
        this.inited = inited;
    }

    @Override
    public void afterPropertiesSet() throws NulsException {

    }
}
