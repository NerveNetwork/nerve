package network.nerve.pocbft.v1.entity;

import io.nuls.core.log.Log;
import network.nerve.pocbft.network.model.message.ConsensusShareMsg;
import network.nerve.pocbft.v1.message.VoteMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class ShareMessageQueue {

    /**
     * 接收到的投票都统一放在这里
     */
    private LinkedBlockingQueue<ConsensusShareMsg> voteMessageQueue = new LinkedBlockingQueue<>(350000);

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(ConsensusShareMsg msg) {
        try {
            return this.voteMessageQueue.offer(msg);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    public ConsensusShareMsg take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

}
