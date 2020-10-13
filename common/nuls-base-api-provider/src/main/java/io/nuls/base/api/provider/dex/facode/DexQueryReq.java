package io.nuls.base.api.provider.dex.facode;

public class DexQueryReq {

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
