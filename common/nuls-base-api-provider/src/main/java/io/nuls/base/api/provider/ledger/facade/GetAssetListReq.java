package io.nuls.base.api.provider.ledger.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-25 10:11
 * @Description: 功能描述
 */
public class GetAssetListReq extends BaseReq {

    private int assetType = 0;

    public int getAssetType() {
        return assetType;
    }

    public void setAssetType(int assetType) {
        this.assetType = assetType;
    }

    public GetAssetListReq(int assetType) {
        this.assetType = assetType;
    }

    public GetAssetListReq(int chainId,int assetType) {
        setChainId(chainId);
        setAssetType(assetType);
    }
}
