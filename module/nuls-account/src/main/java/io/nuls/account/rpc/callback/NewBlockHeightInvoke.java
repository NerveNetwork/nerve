package io.nuls.account.rpc.callback;

import io.nuls.account.model.bo.Chain;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.rpc.invoke.BaseInvoke;
import io.nuls.core.rpc.model.message.Response;

import java.util.HashMap;


/**
 * 订阅最新高度的回调
 *
 * @author: Loki
 * @date: 2020-02-28
 */
public class NewBlockHeightInvoke extends BaseInvoke {

    private Chain chain;

    public NewBlockHeightInvoke(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void callBack(Response response) {
        HashMap hashMap = (HashMap) ((HashMap) response.getResponseData()).get("latestHeight");
        if (null == hashMap.get("value")) {
            chain.getLogger().error("[订阅事件]最新区块高度为null");
            return;
        }
        if (null == hashMap.get("time")) {
            chain.getLogger().error("[订阅事件]最新区块高度时间为null");
            return;
        }
        if (null == hashMap.get("syncStatusEnum")) {
            chain.getLogger().error("[订阅事件]当前区块同步模式");
            return;
        }
        int syncStatus = Integer.valueOf(hashMap.get("syncStatusEnum").toString());
        SyncStatusEnum syncStatusEnum = SyncStatusEnum.getEnum(syncStatus);
        if (null == syncStatusEnum) {
            chain.getLogger().error("[订阅事件]当前区块同步模式状态为null");
            return;
        }
        long height = Long.valueOf(hashMap.get("value").toString());
        long time = Long.valueOf(hashMap.get("time").toString());
        chain.getLatestBasicBlock().setHeight(height);
        chain.getLatestBasicBlock().setTime(time);
        chain.getLatestBasicBlock().setSyncStatusEnum(syncStatusEnum);
        chain.getLogger().debug("[订阅事件]最新区块高度:{} blockTime:{} syncStatus:{}", height, time, syncStatusEnum.name());
    }
}