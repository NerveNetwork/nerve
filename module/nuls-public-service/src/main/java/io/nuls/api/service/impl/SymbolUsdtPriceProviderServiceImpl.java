package io.nuls.api.service.impl;

import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.config.SymbolPriceProviderSourceConfig;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.dto.ActualSymbolUsdtPriceDTO;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.SymbolPrice;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.sup.BasePriceProvider;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rockdb.model.Entry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
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

    public void doQuery(){
        symbolList.entrySet().forEach(this::updateSymbolPrice);
    }

    private void updateSymbolPrice(Map.Entry<String,ActualSymbolUsdtPriceDTO> entry){
        String symbol = entry.getKey();
        ActualSymbolUsdtPriceDTO symbolPrice = entry.getValue();
        List<BigDecimal> collect = new ArrayList<>();
        List<SymbolPriceProviderSourceConfig.Source> list = config.getSymbolPriceProviderSource();
        for (SymbolPriceProviderSourceConfig.Source provider: list ) {
            if((!provider.getMatchSet().isEmpty()) && !provider.getMatchSet().contains(symbol)){
                continue;
            }
            if( !provider.getExcludeSet().isEmpty() && provider.getExcludeSet().contains(symbol)){
                continue;
            }
            BigDecimal price = provider.getProvider().queryPrice(BasePriceProvider.ALIAS.getOrDefault(symbol,symbol));
            if(!price.equals(BigDecimal.ZERO)){
                collect.add(price);
            }
        }
        BigDecimal total = collect.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if(total.equals(BigDecimal.ZERO)){
            symbolPrice.setPrice(BigDecimal.ZERO);
        }else{
            symbolPrice.setPrice(collect.size() == 1 ? total : total.divide(new BigDecimal(collect.size()),symbolPrice.getDecimal(), RoundingMode.HALF_DOWN));
        }
        if(symbolPrice.getPrice().compareTo(BigDecimal.ZERO) > 0 ){
            LoggerUtil.PRICE_PROVIDER_LOG.info("更新{}价格{}USDT",symbolPrice.getSymbol(),symbolPrice.getPrice());
        }
    }
    @Override
    public SymbolPrice getSymbolPriceForUsdt(String symbol) {
        if(symbolList.containsKey(symbol)){
            return symbolList.get(symbol);
        }else{
            symbolList.entrySet().stream().filter(entry->entry.getKey().equals(symbol)).forEach(this::updateSymbolPrice);
            return symbolList.containsKey(symbol) ? symbolList.get(symbol) : new ActualSymbolUsdtPriceDTO(symbol, SymbolPrice.DEFAULT_SCALE);
        }
    }

    @Override
    public BigDecimal toUsdValue(String symbol, BigDecimal amount) {
        SymbolPrice sourceSymbolPrice = getSymbolPriceForUsdt(symbol);
        return getUsd().transfer(sourceSymbolPrice,amount);
    }

    public SymbolPrice getUsd(){
        return this.getSymbolPriceForUsdt(ApiConstant.USD);
    }

    @Override
    public SymbolUsdPercentDTO calcRate(String symbol, Map<String, BigInteger> list){
        Map<String,BigDecimal> symbolUsdTxTotalMap = new HashMap<>(list.size());
        SymbolPrice usdPrice = this.getUsd();
        BigDecimal allSymbolTxTotalUsdValue =list.entrySet().stream().map(d->{
            SymbolPrice symbolPrice = this.getSymbolPriceForUsdt(d.getKey());
            if(symbolPrice.getPrice().equals(BigDecimal.ZERO)){
                return BigDecimal.ZERO;
            }
            SymbolRegInfo symbolRegInfo = symbolRegService.getFirst(d.getKey()).get();
            //计算当前币种交易对应的的USD总量
            BigDecimal total = new BigDecimal(d.getValue()).movePointLeft(symbolRegInfo.getDecimals());
            BigDecimal usdTotal = usdPrice.transfer(symbolPrice,BigDecimal.ONE).setScale(ApiConstant.USD_DECIMAL,RoundingMode.HALF_DOWN);
            usdTotal = usdTotal.multiply(total, MathContext.DECIMAL64).setScale(ApiConstant.USD_DECIMAL,RoundingMode.HALF_DOWN);
            symbolUsdTxTotalMap.put(d.getKey(),usdTotal);
            return usdTotal;
        }).reduce(BigDecimal.ZERO,(v1,v2)->v1.add(v2));
        BigDecimal rate;
        BigDecimal usdValue = symbolUsdTxTotalMap.getOrDefault(symbol,BigDecimal.ZERO);
        if(allSymbolTxTotalUsdValue.compareTo(BigDecimal.ZERO) == 0){
            rate = new BigDecimal(1);
        }else if (usdValue.compareTo(BigDecimal.ZERO) == 0){
            rate = BigDecimal.ZERO;
        }else {
            rate = symbolUsdTxTotalMap.get(symbol).divide(allSymbolTxTotalUsdValue,4, RoundingMode.HALF_DOWN);
        }
        SymbolUsdPercentDTO res = new SymbolUsdPercentDTO();
        res.setPer(rate);
        res.setTotalUsdVal(allSymbolTxTotalUsdValue);
        res.setAmount(list.getOrDefault(symbol,BigInteger.ZERO));
        res.setUsdVal(usdValue.setScale(ApiConstant.USD_DECIMAL, RoundingMode.HALF_DOWN));
        return res;
    }

    public void init() {
        symbolRegService.getAll().forEach(d->{
            symbolList.put(d.getSymbol(),new ActualSymbolUsdtPriceDTO(d.getSymbol(),d.getDecimals()));
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
