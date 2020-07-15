package network.nerve.pocbft.v1.entity;

import network.nerve.pocbft.v1.message.VoteResultMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class VoteResultQueue {

    /**
     * 接收到的投票都统一放在这里
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
