package io.nuls.base.api.provider.dex.facode;

import io.nuls.base.api.provider.BaseReq;

public class DexQueryReq extends BaseReq {

    private String tradingHash;

    public DexQueryReq() {

    }

    public DexQueryReq(String tradingHash) {
        this.tradingHash = tradingHash;
    }

    public String getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(String tradingHash) {
        this.tradingHash = tradingHash;
    }

}
