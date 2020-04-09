package io.nuls.api.model.dto;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-24 11:56
 * @Description: 功能描述
 */
public class Asset {

    private int chainId;

    private int assetId;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;

        Asset asset = (Asset) o;

        if (chainId != asset.chainId) return false;
        return assetId == asset.assetId;
    }

    @Override
    public int hashCode() {
        int result = chainId;
        result = 31 * result + assetId;
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
                .append("\"chainId\":")
                .append(chainId)
                .append(",\"assetId\":")
                .append(assetId)
                .append('}').toString();
    }

}
