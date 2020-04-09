package io.nuls.api.constant.config;

import io.nuls.api.sup.PriceProvider;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.util.List;
import java.util.Set;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 19:49
 * @Description: 功能描述
 */
@Component
@Configuration(domain = ModuleE.Constant.PUBLIC_SERVICE)
public class SymbolPriceProviderSourceConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws NulsException {
        this.symbolPriceProviderSource.forEach(d->{
            Class<?> clsszs = null;
            try {
                clsszs = Class.forName(d.getClazs());
            } catch (ClassNotFoundException e) {
                LoggerUtil.commonLog.error("配置错误：symbolPriceProviderSource.clazs,必须是完整的class路径");
                System.exit(0);
            }
            PriceProvider priceProvider = (PriceProvider) SpringLiteContext.getBean(clsszs);
            if(!(priceProvider instanceof PriceProvider)){
                LoggerUtil.commonLog.error("配置错误：symbolPriceProviderSource.clazs,必须实现PriceProvider接口");
                System.exit(0);
            }
            priceProvider.setURL(d.getUrl());
            d.setProvider(priceProvider);
            if(StringUtils.isNotBlank(d.getExclude())){
                d.setExcludeSet(Set.of(d.getExclude().split(",")));
            }else{
                d.setExcludeSet(Set.of());
            }
            if(StringUtils.isNotBlank(d.getMatch())){
                d.setMatchSet(Set.of(d.getMatch().split(",")));
            }else{
                d.setMatchSet(Set.of());
            }
            LoggerUtil.commonLog.info("加载价格提供器:{}",d);
        });
    }

    public static class Source {

        String name;

        String clazs;

        PriceProvider provider;

        String exclude;

        Set<String> excludeSet;

        String match;

        Set<String> matchSet;

        String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClazs() {
            return clazs;
        }

        public void setClazs(String clazs) {
            this.clazs = clazs;
        }

        public PriceProvider getProvider() {
            return provider;
        }

        public void setProvider(PriceProvider provider) {
            this.provider = provider;
        }

        public String getExclude() {
            return exclude;
        }

        public void setExclude(String exclude) {
            this.exclude = exclude;
        }

        public Set<String> getExcludeSet() {
            return excludeSet;
        }

        public void setExcludeSet(Set<String> excludeSet) {
            this.excludeSet = excludeSet;
        }

        public String getMatch() {
            return match;
        }

        public void setMatch(String match) {
            this.match = match;
        }

        public Set<String> getMatchSet() {
            return matchSet;
        }

        public void setMatchSet(Set<String> matchSet) {
            this.matchSet = matchSet;
        }
    }

    List<Source> symbolPriceProviderSource;

    public List<Source> getSymbolPriceProviderSource() {
        return symbolPriceProviderSource;
    }

    public void setSymbolPriceProviderSource(List<Source> symbolPriceProviderSource) {
        this.symbolPriceProviderSource = symbolPriceProviderSource;
    }
}
