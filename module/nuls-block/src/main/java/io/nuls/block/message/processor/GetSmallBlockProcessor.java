package io.nuls.block.message.processor;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.SmallBlock;
import io.nuls.block.constant.BlockForwardEnum;
import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.HashMessage;
import io.nuls.block.message.SmallBlockMessage;
import io.nuls.block.model.CachedSmallBlock;
import io.nuls.block.model.ChainContext;
import io.nuls.block.model.TxGroupTask;
import io.nuls.block.rpc.call.ConsensusCall;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.service.BlockService;
import io.nuls.block.thread.monitor.TxGroupRequestor;
import io.nuls.block.utils.SmallBlockCacher;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.BlockBootstrap.blockConfig;
import static io.nuls.block.constant.CommandConstant.*;

/**
 * @author Niels
 */
public class GetSmallBlockProcessor implements Runnable {

    private LinkedBlockingQueue<HashMessage> queue = new LinkedBlockingQueue<>(1024);

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
        NulsHash blockHash = message.getRequestHash();
//        logger.debug("recieve sm-hash" + message + " from node-" + nodeId + ", hash:" + blockHash);
        CachedSmallBlock cachedSmallBlock = SmallBlockCacher.getCachedSmallBlock(chainId, blockHash);
        if (cachedSmallBlock == null) {
            return;
        }
        SmallBlock smallBlock = SmallBlockCacher.getSmallBlock(chainId, blockHash);
        if (smallBlock != null) {
            SmallBlockMessage smallBlockMessage = new SmallBlockMessage();
            smallBlockMessage.setSmallBlock(smallBlock);

            byte[] voteResult = ContextManager.getContext(chainId).getVoteResultCache().get(blockHash);
            if (null == voteResult) {
                voteResult = ConsensusCall.getVoteResult(chainId, blockHash);
                if (null != voteResult) {
                    ContextManager.getContext(chainId).getVoteResultCache().cache(blockHash, voteResult);
                }
            }

            smallBlockMessage.setVoteResult(voteResult);

            if (cachedSmallBlock.isPocNet()) {
                NetworkCall.sendToNode(chainId, smallBlockMessage, nodeId, SMALL_BLOCK_BZT_MESSAGE);
                NetworkCall.sendToNode(chainId, smallBlockMessage, nodeId, SMALL_BLOCK_MESSAGE);
            } else {
                NetworkCall.sendToNode(chainId, smallBlockMessage, nodeId, SMALL_BLOCK_MESSAGE);
                NetworkCall.sendToNode(chainId, smallBlockMessage, nodeId, SMALL_BLOCK_BZT_MESSAGE);
            }
        }
    }


    public void offer(HashMessage message) {
        queue.offer(message);
    }
}
