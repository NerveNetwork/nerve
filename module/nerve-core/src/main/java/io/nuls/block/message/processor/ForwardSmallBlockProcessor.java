package io.nuls.block.message.processor;

import io.nuls.base.data.NulsHash;
import io.nuls.block.constant.BlockForwardEnum;
import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.HashMessage;
import io.nuls.block.model.CachedSmallBlock;
import io.nuls.block.model.ChainContext;
import io.nuls.block.model.TxGroupTask;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.thread.monitor.TxGroupRequestor;
import io.nuls.block.utils.SmallBlockCacher;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.BlockBootstrap.blockConfig;
import static io.nuls.block.constant.CommandConstant.GET_SMALL_BLOCK_MESSAGE;

/**
 * @author Niels
 */
public class ForwardSmallBlockProcessor implements Runnable {

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
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        NulsHash blockHash = message.getRequestHash();
        long height = message.getHeight();
        context.getCachedHashHeightMap().put(blockHash, height);
        NetworkCall.setHashAndHeight(chainId, blockHash, height, nodeId);
        BlockForwardEnum status = SmallBlockCacher.getStatus(chainId, blockHash);
//        logger.debug("recieve " + message + " from node-" + nodeId + ", hash:" + blockHash);
        List<String> nodes = context.getOrphanBlockRelatedNodes().get(blockHash);
        if (nodes != null && !nodes.contains(nodeId)) {
            nodes.add(nodeId);
            logger.debug("add OrphanBlockRelatedNodes, blockHash-{}, nodeId-{}", blockHash, nodeId);
        }
        //1.已收到完整区块,丢弃
        if (BlockForwardEnum.COMPLETE.equals(status)) {
            return;
        }
        //2.已收到部分区块,还缺失交易信息,发送HashListMessage到源节点
        if (BlockForwardEnum.INCOMPLETE.equals(status) && !context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
            CachedSmallBlock block = SmallBlockCacher.getCachedSmallBlock(chainId, blockHash);
            if (block == null) {
                return;
            }
            HashListMessage request = new HashListMessage();
            request.setBlockHash(blockHash);
            request.setTxHashList(block.getMissingTransactions());
            TxGroupTask task = new TxGroupTask();
            task.setId(System.nanoTime());
            task.setNodeId(nodeId);
            task.setRequest(request);
            task.setExcuteTime(blockConfig.getTxGroupTaskDelay());
            TxGroupRequestor.addTask(chainId, blockHash.toString(), task);
            return;
        }
        //3.未收到区块
        if (BlockForwardEnum.EMPTY.equals(status)) {
            HashMessage request = new HashMessage(blockHash, height);
            NetworkCall.sendToNode(chainId, request, nodeId, GET_SMALL_BLOCK_MESSAGE);
        }
    }


    public void offer(HashMessage message) {
        queue.offer(message);
    }
}
