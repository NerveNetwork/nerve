package io.nuls.api.db;

import io.nuls.api.model.po.SymbolBaseInfo;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-18 17:07
 * @Description: symbol基础信息维护
 */
public interface SymbolBaseInfoService {

    void save(SymbolBaseInfo... list);

    SymbolBaseInfo get(int assetChainId,int assetId);

    SymbolBaseInfo get(String symbol);

}
