package io.nuls.consensus.v1.entity;

import io.nuls.consensus.v1.message.VoteResultMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class VoteResultQueue {

    /**
     * All received votes are placed here uniformly
     */
    private LinkedBlockingQueue<VoteResultMessage> voteMessageQueue = new LinkedBlockingQueue<>();

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(VoteResultMessage msg) {
        return this.voteMessageQueue.offer(msg);
    }

    public VoteResultMessage take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

}
