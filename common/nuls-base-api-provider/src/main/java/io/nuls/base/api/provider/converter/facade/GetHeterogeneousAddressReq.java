package io.nuls.base.api.provider.converter.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-17 17:50
 * @Description: Function Description
 */
public class GetHeterogeneousAddressReq extends BaseReq {

    int heterogeneousChainId;

    String packingAddress;

    public GetHeterogeneousAddressReq(int heterogeneousChainId, String packingAddress) {
        this.heterogeneousChainId = heterogeneousChainId;
        this.packingAddress = packingAddress;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(String packingAddress) {
        this.packingAddress = packingAddress;
    }
}
