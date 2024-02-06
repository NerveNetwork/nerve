package network.nerve.converter.rpc.callback;

import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rpc.invoke.BaseInvoke;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.model.bo.Chain;

import java.util.HashMap;

import static network.nerve.converter.config.ConverterContext.LATEST_BLOCK_HEIGHT;
import static network.nerve.converter.utils.LoggerUtil.LOG;

/**
 * Subscription to the latest height callback
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
            chain.getLogger().error("[Subscription events]The latest block height isnull");
            return;
        }
        if (null == hashMap.get("time")) {
            chain.getLogger().error("[Subscription events]The latest block height time isnull");
            return;
        }
        if (null == hashMap.get("syncStatusEnum")) {
            chain.getLogger().error("[Subscription events]Current block synchronization mode");
            return;
        }
        int syncStatus = Integer.valueOf(hashMap.get("syncStatusEnum").toString());
        SyncStatusEnum syncStatusEnum = SyncStatusEnum.getEnum(syncStatus);
        if (null == syncStatusEnum) {
            chain.getLogger().error("[Subscription events]The current block synchronization mode status isnull");
            return;
        }
        long height = Long.valueOf(hashMap.get("value").toString());
        long time = Long.valueOf(hashMap.get("time").toString());

        LOG.debug("[Subscription events]Latest block height:{} blockTime:{} syncStatus:{}", height, time, syncStatusEnum.name());
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
