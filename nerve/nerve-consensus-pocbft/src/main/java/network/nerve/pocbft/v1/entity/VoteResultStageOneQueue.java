package network.nerve.pocbft.v1.entity;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class VoteResultStageOneQueue {

    /**
     * 所有第一阶段的结果都提交到这里
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
