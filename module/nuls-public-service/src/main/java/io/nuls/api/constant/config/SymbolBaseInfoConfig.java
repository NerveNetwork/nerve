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

import java.util.ArrayList;
import java.util.List;

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
    SymbolRegService symbolRegService;

    @Autowired
    ConfigurationLoader configurationLoader;

    @Autowired
    ApiConfig apiConfig;

    @Override
    public void afterPropertiesSet() throws NulsException {
        SymbolRegInfo nulsRegInfo = symbolRegService.get(apiConfig.getMainChainId(),apiConfig.getMainAssetId());
        if(nulsRegInfo != null){
            return;
        }
        nulsRegInfo = new SymbolRegInfo();
        nulsRegInfo.setSymbol(apiConfig.getSymbol());
        nulsRegInfo.setAssetId(apiConfig.getMainAssetId());
        nulsRegInfo.setChainId(apiConfig.getMainChainId());
        nulsRegInfo.setDecimals(apiConfig.getDecimals());
        nulsRegInfo.setQueryPrice(false);
        nulsRegInfo.setFullName(apiConfig.getSymbol());
        nulsRegInfo.setSource(ApiConstant.SYMBOL_REG_SOURCE_NATIVE);
        symbolBaseInfoList.add(nulsRegInfo);
        String nulsStackWeightConfig = configurationLoader.getConfigItem(ModuleE.Constant.CONSENSUS,"mainAssertBase").getValue();
        if(StringUtils.isNotBlank(nulsStackWeightConfig)){
            double nulsStackWeight = Double.parseDouble(nulsStackWeightConfig);
            nulsRegInfo.setStackWeight(nulsStackWeight);
        }
//        symbolBaseInfoList.forEach(d->{
//            symbolRegService.save(d);
//        });
    }

    public List<SymbolRegInfo> getSymbolBaseInfoList() {
        return symbolBaseInfoList;
    }
}
