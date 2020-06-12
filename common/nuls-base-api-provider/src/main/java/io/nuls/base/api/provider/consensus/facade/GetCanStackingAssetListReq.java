package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-05-25 11:45
 * @Description: 功能描述
 */
public class GetCanStackingAssetListReq extends BaseReq {


    public GetCanStackingAssetListReq(int chainId) {
        this.setChainId(chainId);
    }

    public GetCanStackingAssetListReq() {
    }
}

