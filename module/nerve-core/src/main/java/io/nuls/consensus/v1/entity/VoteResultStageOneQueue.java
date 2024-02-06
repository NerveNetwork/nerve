package io.nuls.consensus.v1.entity;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class VoteResultStageOneQueue {

    /**
     * All the results of the first stage are submitted here
     */
    private LinkedBlockingQueue<VoteStageResult> voteMessageQueue = new LinkedBlockingQueue<>();

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(VoteStageResult msg) {
        return this.voteMessageQueue.offer(msg);
    }

    public VoteStageResult take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

}
