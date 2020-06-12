package io.nuls.base.api.provider.converter.facade;

import io.nuls.core.rpc.model.Key;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-25 10:11
 * @Description: 功能描述
 */
public class HeterogeneousAssetInfo {

    String symbol;

    String heterogeneousChainSymbol;

    int decimals;

    boolean isToken;

    String contractAddress;

    int heterogeneousChainId;

    String heterogeneousChainMultySignAddress;

    public String getHeterogeneousChainSymbol() {
        return heterogeneousChainSymbol;
    }

    public void setHeterogeneousChainSymbol(String heterogeneousChainSymbol) {
        this.heterogeneousChainSymbol = heterogeneousChainSymbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public boolean isToken() {
        return isToken;
    }

    public void setToken(boolean token) {
        isToken = token;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousChainMultySignAddress() {
        return heterogeneousChainMultySignAddress;
    }

    public void setHeterogeneousChainMultySignAddress(String heterogeneousChainMultySignAddress) {
        this.heterogeneousChainMultySignAddress = heterogeneousChainMultySignAddress;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
