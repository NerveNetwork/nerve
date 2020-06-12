package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-09 14:29
 * @Description: 获取一个指定高度区块的总奖励数
 */
public class GetTotalRewardForBlockHeightReq extends BaseReq {

    long height;

    public GetTotalRewardForBlockHeightReq(long height) {
        this.height = height;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }
}
