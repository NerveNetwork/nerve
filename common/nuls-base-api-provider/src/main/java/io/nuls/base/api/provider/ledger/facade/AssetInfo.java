package io.nuls.base.api.provider.ledger.facade;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-25 10:11
 * @Description: 功能描述
 */
public class AssetInfo {

    int assetChainId;

    int assetId;

    String symbol;

    int decimals;

    int assetType;

    boolean canBePeriodically;

    public boolean isCanBePeriodically() {
        return canBePeriodically;
    }

    public void setCanBePeriodically(boolean canBePeriodically) {
        this.canBePeriodically = canBePeriodically;
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public int getAssetType() {
        return assetType;
    }

    public void setAssetType(int assetType) {
        this.assetType = assetType;
    }
}
