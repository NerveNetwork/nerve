package io.nuls.api.manager;

import io.nuls.core.core.annotation.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-23 14:56
 * @Description: 异构链资产余额查询管理器
 * 缓存异构链资产余额
 */
@Component
public class HeterogeneousChainAssetBalanceManager {

    private Map<Integer, Map<String, BigDecimal>> balanceList = new HashMap<>();

    public BigDecimal getBalance(Integer chainId, String address){
        Map<String,BigDecimal> chain = balanceList.get(chainId);
        if(chain == null){
            return BigDecimal.ZERO;
        }
        return chain.getOrDefault(address,BigDecimal.ZERO);
    }

    public Map<Integer, Map<String, BigDecimal>> getBalanceList() {
        return balanceList;
    }

}
