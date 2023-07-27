package network.nerve.converter.rpc.callback;

import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.invoke.BaseInvoke;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.model.bo.Chain;

import java.util.Arrays;
import java.util.HashMap;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.utils.LoggerUtil.LOG;

/**
 * 订阅最新高度的回调
 *
 * @author: Loki
 * @date: 2020-02-28
 */
public class NewBlockHeightInvoke extends BaseInvoke {

    private VirtualBankService virtualBankService = SpringLiteContext.getBean(VirtualBankService.class);
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

        LOG.debug("[订阅事件]最新区块高度:{} blockTime:{} syncStatus:{}", height, time, syncStatusEnum.name());
        LATEST_BLOCK_HEIGHT = height;
        chain.getLatestBasicBlock().setHeight(height);
        chain.getLatestBasicBlock().setTime(time);
        chain.getLatestBasicBlock().setSyncStatusEnum(syncStatusEnum);

        if(height == 0){
            return;
        }
        virtualBankService.recordVirtualBankChanges(chain);
        //try {
        //    chain.getLogger().warn("pierre test===chain htg version: {}", chain.getCurrentHeterogeneousVersion());
        //    chain.getMapVirtualBank().values().forEach(e -> {
        //        chain.getLogger().warn("pierre test===bank member: {}", e.getSignAddress());
        //    });
        //} catch (Exception e) {
        //    chain.getLogger().warn("MapVirtualBank log print error ");
        //}
    }
}