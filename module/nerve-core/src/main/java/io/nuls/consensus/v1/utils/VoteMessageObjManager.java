package io.nuls.consensus.v1.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.v1.message.VoteMessage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Niels
 */
public class VoteMessageObjManager {
    private static final LinkedBlockingQueue<VoteMessage> QUEUE = new LinkedBlockingQueue<>(2000);
    private static final AtomicInteger count = new AtomicInteger(0);

    public static VoteMessage getInstance(Chain chain, String msg) {
        if (StringUtils.isBlank(msg)) {
            Log.error("error code-" + CommonCodeConstanst.DESERIALIZE_ERROR);
            return null;
        }
        VoteMessage vote = QUEUE.poll();
//        Log.warn(QUEUE.size() + "");
        if (null == vote) {
            vote = new VoteMessage();
            chain.getLogger().info("create:::" + count.addAndGet(1));
        }
        try {
            vote.parse(new NulsByteBuffer(RPCUtil.decode(msg)));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
        return vote;
    }

    public static void putBack(VoteMessage voteMessage) {
        try {
            QUEUE.offer(voteMessage);
        } catch (Throwable e) {
            //不处理
        }
    }
}
