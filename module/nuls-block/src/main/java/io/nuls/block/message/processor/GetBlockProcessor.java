package io.nuls.block.message.processor;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.BlockMessage;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.HashMessage;
import io.nuls.block.message.TxGroupMessage;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.rpc.call.TransactionCall;
import io.nuls.block.service.BlockService;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.constant.CommandConstant.BLOCK_MESSAGE;
import static io.nuls.block.constant.CommandConstant.TXGROUP_MESSAGE;

/**
 * @author Niels
 */
public class GetBlockProcessor implements Runnable {

    private final BlockService service;
    private LinkedBlockingQueue<HashMessage> queue = new LinkedBlockingQueue<>(1024);

    public GetBlockProcessor(BlockService service){
        this.service = service;
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
        HashMessage message = queue.take();
        int chainId = message.getChainId();
        String nodeId = message.getNodeId();
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        NulsHash requestHash = message.getRequestHash();
        logger.debug("recieve " + message + " from node-" + nodeId + ", hash:" + requestHash);
        Block block = service.getBlock(chainId, requestHash);
        if (block == null) {
            logger.debug("recieve invalid " + message + " from node-" + nodeId + ", hash:" + requestHash);
        }
        sendBlock(chainId, block, nodeId, requestHash);
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

    public void offer(HashMessage message) {
        queue.offer(message);
    }
}
