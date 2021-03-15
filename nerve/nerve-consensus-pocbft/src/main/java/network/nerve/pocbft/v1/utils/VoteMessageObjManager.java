package network.nerve.pocbft.v1.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import network.nerve.pocbft.v1.message.VoteMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Niels
 */
public class VoteMessageObjManager {
    private static final LinkedBlockingQueue<VoteMessage> QUEUE = new LinkedBlockingQueue<>(2000);

    public static VoteMessage getInstance(String msg) {
        if (StringUtils.isBlank(msg)) {
            Log.error("error code-" + CommonCodeConstanst.DESERIALIZE_ERROR);
            return null;
        }
        VoteMessage vote = QUEUE.poll();
        if (null == vote) {
            vote = new VoteMessage();
        }
        try {
            vote.parse(new NulsByteBuffer(RPCUtil.decode(msg)));
        } catch (Exception e) {
            Log.error(e);
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
