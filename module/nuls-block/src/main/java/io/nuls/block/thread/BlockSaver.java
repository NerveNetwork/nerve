package io.nuls.block.thread;

import io.nuls.base.data.Block;
import io.nuls.block.service.BlockService;
import io.nuls.core.log.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Niels
 */
public class BlockSaver implements Runnable {

    private LinkedBlockingQueue<Saver> queue = new LinkedBlockingQueue<>();
    private final BlockService blockService;

    public BlockSaver(BlockService blockService) {
        this.blockService = blockService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Saver saver = queue.take();
                Log.debug("BLOCK-SAVE:" + saver.getBlock().getHeader().getHeight());
                blockService.saveBlock(saver.getChainId(), saver.getBlock(), saver.getDownload(), saver.isNeedLock(),
                        saver.isBroadcast(), saver.isForward(), saver.getNodeId());

            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    public void offer(Saver saver) {
        this.queue.offer(saver);
    }

    public static class Saver {
        public Saver(int chainId, Block block, int download, boolean needLock, boolean broadcast, boolean forward, String nodeId) {
            this.chainId = chainId;
            this.block = block;
            this.download = download;
            this.needLock = needLock;
            this.broadcast = broadcast;
            this.forward = forward;
            this.nodeId = nodeId;
        }

        private int chainId;
        private Block block;
        private int download;
        private boolean needLock;
        private boolean broadcast;
        private boolean forward;
        private String nodeId;

        public int getChainId() {
            return chainId;
        }

        public void setChainId(int chainId) {
            this.chainId = chainId;
        }

        public Block getBlock() {
            return block;
        }

        public void setBlock(Block block) {
            this.block = block;
        }

        public int getDownload() {
            return download;
        }

        public void setDownload(int download) {
            this.download = download;
        }

        public boolean isNeedLock() {
            return needLock;
        }

        public void setNeedLock(boolean needLock) {
            this.needLock = needLock;
        }

        public boolean isBroadcast() {
            return broadcast;
        }

        public void setBroadcast(boolean broadcast) {
            this.broadcast = broadcast;
        }

        public boolean isForward() {
            return forward;
        }

        public void setForward(boolean forward) {
            this.forward = forward;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
    }
}
