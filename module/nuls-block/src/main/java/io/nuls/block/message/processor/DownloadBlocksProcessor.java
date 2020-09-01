package io.nuls.block.message.processor;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.BlockMessage;
import io.nuls.block.message.HeightRangeMessage;
import io.nuls.block.model.ChainContext;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.service.BlockService;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.constant.CommandConstant.BLOCK_MESSAGE;

/**
 * @author Niels
 */
public class DownloadBlocksProcessor implements Runnable {

    private LinkedBlockingQueue<HeightRangeMessage> queue = new LinkedBlockingQueue<>(1024);

    private BlockService service;

    public DownloadBlocksProcessor(BlockService blockService) {
        this.service = blockService;
    }

    private void sendBlock(int chainId, Block block, String nodeId, NulsHash requestHash) {
        BlockMessage blockMessage = new BlockMessage(requestHash, block, true);
        NetworkCall.sendToNode(chainId, blockMessage, nodeId, BLOCK_MESSAGE);
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
        Thread.sleep(10L);
        HeightRangeMessage message = queue.take();
        int chainId = message.getChainId();
        String nodeId = message.getNodeId();

        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        long startHeight = message.getStartHeight();
        long endHeight = message.getEndHeight();
        if (startHeight < 0L || startHeight > endHeight || endHeight - startHeight > context.getParameters().getDownloadNumber()) {
            logger.error("PARAMETER_ERROR");
            return;
        }
        logger.info("recieve HeightRangeMessage from node-" + nodeId + ", start:" + startHeight + ", end:" + endHeight);

        NulsHash requestHash;
        try {
            requestHash = NulsHash.calcHash(message.serialize());
            Block block;
            do {
                block = service.getBlock(chainId, startHeight++);
                if (block == null) {
                    NetworkCall.sendFail(chainId, requestHash, nodeId);
                    return;
                }
                sendBlock(chainId, block, nodeId, requestHash);
            } while (endHeight >= startHeight);
            NetworkCall.sendSuccess(chainId, requestHash, nodeId);
        } catch (Exception e) {
            logger.error("error occur when send block", e);
        }
    }

    public void offer(HeightRangeMessage message) {
        queue.offer(message);
    }
}
