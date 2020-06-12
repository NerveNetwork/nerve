package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-26 14:34
 * @Description: 功能描述
 */
public class GetStackingAssetBySymbolReq extends BaseReq {

    private String symbol;

    public GetStackingAssetBySymbolReq(String symbol){
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

}
