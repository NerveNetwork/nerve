package io.nuls.api.constant.config;

import io.nuls.api.constant.ApiConstant;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.model.po.SymbolRegInfo;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.core.annotation.Order;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.util.*;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-18 17:02
 * @Description: 功能描述
 */
@Component
@Configuration(domain = ModuleE.Constant.PUBLIC_SERVICE)
@Order(1)
public class SymbolBaseInfoConfig implements InitializingBean {

    List<SymbolRegInfo> symbolBaseInfoList = new ArrayList<>();

    @Autowired
    ConfigurationLoader configurationLoader;

    @Autowired
    ApiConfig apiConfig;

    @Override
    public void afterPropertiesSet() throws NulsException {
        //查询nuls的stacking权重
        String nulsStackWeightConfig = configurationLoader.getConfigItem(ModuleE.Constant.CONSENSUS,"mainAssertBase").getValue();
        double nulsStackWeight = 0;
        if(StringUtils.isNotBlank(nulsStackWeightConfig)){
            nulsStackWeight = Double.parseDouble(nulsStackWeightConfig);
        }
        Optional<SymbolRegInfo> nulsRegInfoOt = this.getSymbolRegInfoForConfig(apiConfig.getMainChainId(),apiConfig.getMainAssetId());
        if(nulsRegInfoOt.isPresent()){
            nulsRegInfoOt.get().setStackWeight(nulsStackWeight);
        }else{
            SymbolRegInfo symbolRegInfo = new SymbolRegInfo();
            symbolRegInfo.setStackWeight(nulsStackWeight);
            symbolRegInfo.setChainId(apiConfig.getMainChainId());
            symbolRegInfo.setAssetId(apiConfig.getMainAssetId());
            symbolRegInfo.setSymbol(apiConfig.getMainSymbol());
            symbolRegInfo.setDecimals(ApiConstant.NULS_DECIMAL);
            symbolRegInfo.setSource(ApiConstant.SYMBOL_REG_SOURCE_CC);
            symbolRegInfo.setFullName("NULS");
            symbolBaseInfoList.add(symbolRegInfo);
        }
    }

    public Optional<SymbolRegInfo> getSymbolRegInfoForConfig(int chainId, int assetId){
        return this.symbolBaseInfoList.stream().filter(d->d.getChainId() == chainId && d.getAssetId() == assetId).findFirst();
    }


    public List<SymbolRegInfo> getSymbolBaseInfoList() {
        return symbolBaseInfoList;
    }
}
