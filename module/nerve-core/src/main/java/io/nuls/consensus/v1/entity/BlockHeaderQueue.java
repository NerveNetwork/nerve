package io.nuls.consensus.v1.entity;

import io.nuls.base.data.BlockHeader;
import io.nuls.core.log.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Eva
 */
public class BlockHeaderQueue {

    /**
     * 接收到的投票都统一放在这里
     */
    private LinkedBlockingQueue<BlockHeader> voteMessageQueue = new LinkedBlockingQueue<>(100);

    public void clear() {
        this.voteMessageQueue.clear();
    }

    public boolean offer(BlockHeader msg) {
        try {
            return this.voteMessageQueue.offer(msg);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    public BlockHeader take() throws InterruptedException {
        return this.voteMessageQueue.take();
    }

}
