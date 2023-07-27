package io.nuls.block.message.processor;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.BlockMessage;
import io.nuls.block.message.HeightMessage;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.service.BlockService;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.ByteUtils;

import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.constant.CommandConstant.BLOCK_MESSAGE;

/**
 * @author Niels
 */
public class DownloadBlockByHeightProcessor implements Runnable {

    private LinkedBlockingQueue<HeightMessage> queue = new LinkedBlockingQueue<>(1024);

    private BlockService service;

    public DownloadBlockByHeightProcessor(BlockService blockService) {
        this.service = blockService;
    }

    private void sendBlock(int chainId, Block block, String nodeId, NulsHash requestHash) {
        BlockMessage message = new BlockMessage();
        message.setRequestHash(requestHash);
        if (block != null) {
            message.setBlock(block);
        }
        message.setSyn(false);
        NetworkCall.sendToNode(chainId, message, nodeId, BLOCK_MESSAGE);
    }

    @Override
    public void run() {
        while (true) {
            try {
                process();
            } catch (Throwable e) {
                Log.error(e);
            }
        }
    }

    private void process() throws InterruptedException {
        HeightMessage message = queue.take();
        int chainId = message.getChainId();
        String nodeId = message.getNodeId();
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        long height = message.getHeight();
        logger.info("recieve " + message + " from node-" + nodeId + ", height:" + height);
        sendBlock(chainId, service.getBlock(chainId, height), nodeId, NulsHash.calcHash(ByteUtils.longToBytes(height)));
    }

    public void offer(HeightMessage message) {
        queue.offer(message);
    }
}
