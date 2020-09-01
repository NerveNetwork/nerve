package io.nuls.block.message.processor;

import io.nuls.base.data.*;
import io.nuls.block.constant.BlockForwardEnum;
import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.SmallBlockMessage;
import io.nuls.block.message.TxGroupMessage;
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
import io.nuls.core.model.DateUtils;
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
import static io.nuls.block.constant.CommandConstant.TXGROUP_MESSAGE;

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
        //阻止恶意节点提前出块,拒绝接收未来一定时间外的区块
        ChainParameters parameters = context.getParameters();
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

        //防止节点统计网络最新高度过程中收到新的小区块，造成消息丢失
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
        //如果当前区块正在处理则缓存当前消息
        if (!SmallBlockCacher.processBlockList.addIfAbsent(blockHash)) {
            MessageInfo messageInfo = new MessageInfo(chainId, nodeId, blockHash, header, smallBlock);
            List<MessageInfo> messageInfoList = SmallBlockCacher.pendMessageMap.computeIfAbsent(blockHash, k -> new ArrayList<>());
            messageInfoList.add(messageInfo);
            logger.debug("Chunk in process,  recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
            return;
        }
        handleMessage(chainId, blockHash, nodeId, header, smallBlock, voteResult);
        SmallBlockCacher.processBlockList.remove(blockHash);
    }

    private void handleMessage(int chainId, NulsHash blockHash, String nodeId, BlockHeader header, SmallBlock smallBlock, byte[] voteResult) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        BlockForwardEnum status = SmallBlockCacher.getStatus(chainId, blockHash);
        logger.debug("status:{}, context status:{}", status, context.getStatus());
        //1.已收到完整区块,丢弃
        if (COMPLETE.equals(status) || ERROR.equals(status)) {
            logger.debug("已收到完整区块,丢弃,  recieve smallBlockMessage from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash());
            return;
        }

        logger.info("recieve smallBlock from node-" + nodeId + ", height:" + header.getHeight() + ", hash:" + header.getHash() + ",result:" + (voteResult == null ? null : voteResult.length));
        //2.已收到部分区块,还缺失交易信息,发送HashListMessage到源节点
        if (INCOMPLETE.equals(status) && !context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
            logger.debug("收到smallBlock,但缺少部分交易");
            CachedSmallBlock block = SmallBlockCacher.getCachedSmallBlock(chainId, blockHash);
            if (block == null) {
                logger.info("未找到smallBlock");
                SmallBlockCacher.processBlockList.remove(blockHash);
                return;
            }
            List<NulsHash> missingTransactions = block.getMissingTransactions();
            if (missingTransactions == null) {
                logger.info("未找到丢失交易");
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
            logger.debug("发送回执索要缺失交易信息");
            return;
        }

        //3.未收到区块
        if ((EMPTY.equals(status) || CONSENSUS_COMPLETE.equals(status) || CONSENSUS_ERROR.equals(status)) && !context.getStatus().equals(StatusEnum.SYNCHRONIZING)) {
            if (!BlockUtil.headerVerify(chainId, header)) {
                logger.info("recieve error SmallBlockMessage from " + nodeId);
                SmallBlockCacher.setStatus(chainId, blockHash, ERROR);
                return;
            }
            //共识节点打包的交易包括两种交易,一种是在网络上已经广播的普通交易,一种是共识节点生成的特殊交易(如共识奖励、红黄牌),后面一种交易其他节点的未确认交易池中不可能有,所以都放在systemTxList中
            //还有一种场景时收到smallBlock时,有一些普通交易还没有缓存在未确认交易池中,此时要再从源节点请求
            //txMap用来组装区块
            Map<NulsHash, Transaction> txMap = new HashMap<>(header.getTxCount());
            List<Transaction> systemTxList = smallBlock.getSystemTxList();
            List<NulsHash> systemTxHashList = new ArrayList<>();
            //先把系统交易放入txMap
            for (Transaction tx : systemTxList) {
                txMap.put(tx.getHash(), tx);
                systemTxHashList.add(tx.getHash());
            }
            ArrayList<NulsHash> txHashList = smallBlock.getTxHashList();
            List<NulsHash> missTxHashList = (List<NulsHash>) txHashList.clone();
            //移除系统交易hash后请求交易管理模块,批量获取区块中交易
            missTxHashList = CollectionUtils.removeAll(missTxHashList, systemTxHashList);

            List<Transaction> existTransactions = TransactionCall.getTransactions(chainId, missTxHashList, false);
            if (!existTransactions.isEmpty()) {
                //把普通交易放入txMap
                List<NulsHash> existTransactionHashs = new ArrayList<>();
                existTransactions.forEach(e -> existTransactionHashs.add(e.getHash()));
                for (Transaction existTransaction : existTransactions) {
                    txMap.put(existTransaction.getHash(), existTransaction);
                }
                missTxHashList = CollectionUtils.removeAll(missTxHashList, existTransactionHashs);
            }

            //获取没有的交易
            if (!missTxHashList.isEmpty()) {
                logger.debug("send HashListMessage block height:" + header.getHeight() + ", total tx count:" + header.getTxCount() + " , get group tx of " + missTxHashList.size());
                //这里的smallBlock的subTxList中包含一些非系统交易,用于跟TxGroup组合成完整区块
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
            logger.debug("record recv block, block create time-" + DateUtils.timeStamp2DateStr(block.getHeader().getTime() * 1000) + ", hash-" + block.getHeader().getHash());
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
