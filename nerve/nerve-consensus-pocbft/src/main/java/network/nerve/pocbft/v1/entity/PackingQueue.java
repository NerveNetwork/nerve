package network.nerve.pocbft.v1.entity;

import network.nerve.pocbft.v1.message.VoteMessage;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class PackingQueue {

    /**
     * 出块请求放在这里
     */
    private LinkedBlockingQueue<PackingData> voteMessageQueue = new LinkedBlockingQueue<>();

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(PackingData msg) {
        return this.voteMessageQueue.offer(msg);
    }

    public PackingData take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

}
