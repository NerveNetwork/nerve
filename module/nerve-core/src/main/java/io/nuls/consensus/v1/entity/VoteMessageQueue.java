package io.nuls.consensus.v1.entity;

import io.nuls.core.log.Log;
import io.nuls.consensus.v1.message.VoteMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class VoteMessageQueue {

    /**
     * All received votes are placed here uniformly
     */
    private LinkedBlockingQueue<VoteMessage> voteMessageQueue = new LinkedBlockingQueue<>(350000);

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(VoteMessage msg) {
        try {
            return this.voteMessageQueue.offer(msg);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    public VoteMessage take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

    public int size() {
        return voteMessageQueue.size();
    }
}
