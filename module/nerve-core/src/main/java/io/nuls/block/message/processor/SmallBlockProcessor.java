package io.nuls.block.message.processor;

import io.nuls.common.ConfigBean;
import io.nuls.base.data.*;
import io.nuls.block.constant.BlockForwardEnum;
import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.SmallBlockMessage;
import io.nuls.block.model.*;
import io.nuls.block.rpc.call.ConsensusCall;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.rpc.call.TransactionCall;
import io.nuls.block.service.BlockService;
import io.nuls.block.thread.monitor.TxGroupRequestor;
import io.nuls.block.utils.BlockUtil;
import io.nuls.block.utils.SmallBlockCacher;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.CollectionUtils;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.BlockBootstrap.blockConfig;
import static io.nuls.block.constant.BlockForwardEnum.*;
import static io.nuls.block.constant.BlockForwardEnum.ERROR;
import static io.nuls.block.constant.CommandConstant.GET_TXGROUP_MESSAGE;

/**
 * @author Niels
 */
public class SmallBlockProcessor implements Runnable {

    private LinkedBlockingQueue<SmallBlockMessage> queue;
    private final BlockService blockService;

    public SmallBlockProcessor(BlockService blockService,LinkedBlockingQueue<SmallBlockMessage> queue ){
        this.blockService = blockService;
        this.queue = queue;
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
        SmallBlockMessage message = queue.take();
        int chainId = message.getChainId();
        String nodeId = message.getNodeId();
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        SmallBlock smallBlock = message.getSmallBlock();
        if (null == smallBlock) {
            logger.warn("recieved a null smallBlock!");
            return;
        }

        BlockHeader header = smallBlock.getHeader();
        NulsHash blockHash = header.getHash();
        if (header.getHeight() <= context.getLatestHeight()) {
//            logger.debug("The block has been confirmed locally,height:{},hash:{}", header.getHeight(), header.getHash());
            return;
        }
        //Prevent malicious nodes from prematurely exiting blocks,Refuse to receive blocks beyond a certain period of time in the future
        ConfigBean parameters = context.getParameters();
        int validBlockInterval = parameters.getValidBlockInterval();
        long currentTime = NulsDateUtils.getCurrentTimeMillis();
        if (header.getTime() * 1000 > (currentTime + validBlockInterval)) {
            logger.error("header.getTime()-" + header.getTime() + ", currentTime-" + currentTime + ", validBlockInterval-" + validBlockInterval);
            return;
        }

//        logger.info("recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
        context.getCachedHashHeightMap().put(blockHash, header.getHeight());
        NetworkCall.setHashAndHeight(chainId, blockHash, header.getHeight(), nodeId);

        if (message.getVoteResult() != null) {
            ConsensusCall.noticeVoteResult(chainId, message.getVoteResult());
            context.getVoteResultCache().cache(blockHash, message.getVoteResult());
        } else {
            System.out.println();
        }

        //Prevent nodes from receiving new cell blocks during the process of calculating the latest height of the network, resulting in message loss
        if (context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
//            logger.info("Node status is in synchronization,  recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
            try {
                context.getSynCompleteLock().lock();
                if (context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
                    if (context.getLatestHeight() <= header.getHeight()) {
                        return;
                    }
                }
            } finally {
                context.getSynCompleteLock().unlock();
            }
        }
        SmallBlockCacher.cacheNode(blockHash, nodeId, false);
        filterMessage(chainId, blockHash, nodeId, header, smallBlock, message.getVoteResult());
    }

    private void filterMessage(int chainId, NulsHash blockHash, String nodeId, BlockHeader header, SmallBlock smallBlock, byte[] voteResult) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        if (header.getHeight() <= context.getLatestHeight()) {
//            logger.info("The block has been confirmed locally,height:{},hash:{}", header.getHeight(), header.getHash());
            return;
        }
        //If the current block is being processed, cache the current message
        if (!SmallBlockCacher.processBlockList.addIfAbsent(blockHash)) {
            MessageInfo messageInfo = new MessageInfo(chainId, nodeId, blockHash, header, smallBlock);
            List<MessageInfo> messageInfoList = SmallBlockCacher.pendMessageMap.computeIfAbsent(blockHash, k -> new ArrayList<>());
            messageInfoList.add(messageInfo);
//            logger.debug("Chunk in process,  recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
            return;
        }
        handleMessage(chainId, blockHash, nodeId, header, smallBlock, voteResult);
        SmallBlockCacher.processBlockList.remove(blockHash);
    }

    private void handleMessage(int chainId, NulsHash blockHash, String nodeId, BlockHeader header, SmallBlock smallBlock, byte[] voteResult) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        BlockForwardEnum status = SmallBlockCacher.getStatus(chainId, blockHash);
//        logger.debug("status:{}, context status:{}", status, context.getStatus());
        //1.Received complete block,discard
        if (COMPLETE.equals(status) || ERROR.equals(status)) {
            logger.debug("Received complete block,discard,  recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
            return;
        }

        logger.info("recieve smallBlock from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash() + ",result:" + (voteResult == null ? null : voteResult.length));
        //2.Received partial blocks,Transaction information is still missing,sendHashListMessageTo source node
        if (INCOMPLETE.equals(status) && !context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
            logger.debug("receivesmallBlock,But some transactions are missing");
            CachedSmallBlock block = SmallBlockCacher.getCachedSmallBlock(chainId, blockHash);
            if (block == null) {
                logger.info("not foundsmallBlock");
                SmallBlockCacher.processBlockList.remove(blockHash);
                return;
            }
            List<NulsHash> missingTransactions = block.getMissingTransactions();
            if (missingTransactions == null) {
                logger.info("Lost transaction not found");
                return;
            }
            HashListMessage request = new HashListMessage();
            request.setBlockHash(blockHash);
            request.setTxHashList(missingTransactions);
            TxGroupTask task = new TxGroupTask();
            task.setId(System.nanoTime());
            task.setNodeId(nodeId);
            task.setRequest(request);
            task.setExcuteTime(blockConfig.getTxGroupTaskDelay());
            TxGroupRequestor.addTask(chainId, blockHash.toString(), task);
            handlePendMessage(blockHash);
            logger.debug("Send a receipt requesting missing transaction information");
            return;
        }

        //3.Block not received
        if ((EMPTY.equals(status) || CONSENSUS_COMPLETE.equals(status) || CONSENSUS_ERROR.equals(status)) && !context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
            if (!BlockUtil.headerVerify(chainId, header)) {
                logger.info("recieve error SmallBlockMessage from " + nodeId);
                SmallBlockCacher.setStatus(chainId, blockHash, ERROR);
                return;
            }
            //The transactions packaged by consensus nodes include two types of transactions,One type is ordinary transactions that have already been broadcasted on the internet,One type is special transactions generated by consensus nodes(Like consensus rewards、bookings),The latter type of transaction cannot exist in the unconfirmed transaction pool of other nodes,So it's all placed insystemTxListin
            //There is another scenario where you receivesmallBlockTime,Some regular transactions have not yet been cached in the unconfirmed transaction pool,At this point, we need to request from the source node again
            //txMapUsed to assemble blocks
            Map<NulsHash, Transaction> txMap = new HashMap<>(header.getTxCount());
            List<Transaction> systemTxList = smallBlock.getSystemTxList();
            List<NulsHash> systemTxHashList = new ArrayList<>();
            //First, put the system transaction into thetxMap
            for (Transaction tx : systemTxList) {
                txMap.put(tx.getHash(), tx);
                systemTxHashList.add(tx.getHash());
            }
            ArrayList<NulsHash> txHashList = smallBlock.getTxHashList();
            List<NulsHash> missTxHashList = (List<NulsHash>) txHashList.clone();
            //Remove system transactionshashPost request transaction management module,Batch acquisition of transactions in blocks
            missTxHashList = CollectionUtils.removeAll(missTxHashList, systemTxHashList);

            List<Transaction> existTransactions = TransactionCall.getTransactions(chainId, missTxHashList, false);
            if (!existTransactions.isEmpty()) {
                //Put regular transactions intotxMap
                List<NulsHash> existTransactionHashs = new ArrayList<>();
                existTransactions.forEach(e -> existTransactionHashs.add(e.getHash()));
                for (Transaction existTransaction : existTransactions) {
                    txMap.put(existTransaction.getHash(), existTransaction);
                }
                missTxHashList = CollectionUtils.removeAll(missTxHashList, existTransactionHashs);
            }

            //Obtain transactions that are not available
            if (!missTxHashList.isEmpty()) {
                logger.debug("send HashListMessage block height:" + header.getHeight() + ", total tx count:" + header.getTxCount() + " , get group tx of " + missTxHashList.size());
                //HeresmallBlockofsubTxListIt contains some non system transactions,Used to communicate withTxGroupCombine into complete blocks
                CachedSmallBlock cachedSmallBlock = new CachedSmallBlock(missTxHashList, smallBlock, txMap, nodeId, false);
                SmallBlockCacher.cacheSmallBlock(chainId, cachedSmallBlock);
                SmallBlockCacher.setStatus(chainId, blockHash, INCOMPLETE);
                HashListMessage request = new HashListMessage();
                request.setBlockHash(blockHash);
                request.setTxHashList(missTxHashList);
                NetworkCall.sendToNode(chainId, request, nodeId, GET_TXGROUP_MESSAGE);
                handlePendMessage(blockHash);
                return;
            }

            CachedSmallBlock cachedSmallBlock = new CachedSmallBlock(null, smallBlock, txMap, nodeId, false);
            SmallBlockCacher.cacheSmallBlock(chainId, cachedSmallBlock);
            SmallBlockCacher.setStatus(chainId, blockHash, COMPLETE);
            TxGroupRequestor.removeTask(chainId, blockHash);
            Block block = BlockUtil.assemblyBlock(header, txMap, txHashList);
            block.setNodeId(nodeId);
//            logger.debug("record recv block, block create time-" + DateUtils.timeStamp2DateStr(block.getHeader().getTime() * 1000) + ", hash-" + block.getHeader().getHash());
            boolean b = blockService.saveBlock(chainId, block, 1, true, false, true, cachedSmallBlock.getNodeId());
            if (!b) {
                SmallBlockCacher.setStatus(chainId, blockHash, ERROR);
                SmallBlockCacher.nodeMap.remove(blockHash);
                logger.debug("block save error hash-" + block.getHeader().getHash());
            }
            SmallBlockCacher.pendMessageMap.remove(blockHash);
        }
    }

    private void handlePendMessage(NulsHash blockHash) {
        List<MessageInfo> messageInfoList = SmallBlockCacher.pendMessageMap.get(blockHash);
        if (messageInfoList != null && !messageInfoList.isEmpty()) {
            MessageInfo messageInfo = messageInfoList.remove(0);
            handleMessage(messageInfo.getChainId(), messageInfo.getBlockHash(), messageInfo.getNodeId(), messageInfo.getHeader(), messageInfo.getSmallBlock(), null);
        }
    }
}
