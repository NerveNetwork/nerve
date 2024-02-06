package io.nuls.consensus.v1.entity;

import io.nuls.core.log.Log;
import io.nuls.consensus.network.model.message.ConsensusShareMsg;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class ShareMessageQueue {

    /**
     * All received votes are placed here uniformly
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
