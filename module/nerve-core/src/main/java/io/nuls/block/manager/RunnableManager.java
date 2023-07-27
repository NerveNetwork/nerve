package io.nuls.block.manager;

import io.nuls.block.message.*;
import io.nuls.block.message.processor.*;
import io.nuls.block.service.BlockService;
import io.nuls.block.thread.BlockSaver;
import io.nuls.core.thread.ThreadUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Niels
 */
public class RunnableManager {

    private static Map<Integer, BlockSaver> saverMap = new HashMap<>();
    private static Map<Integer, DownloadBlocksProcessor> downloadBlocksProcessorMap = new HashMap<>();
    private static Map<Integer, DownloadBlockByHeightProcessor> downloadBlockProcessorMap = new HashMap<>();
    private static Map<Integer, GetTxGroupProcessor> getTxGroupProcessorMap = new HashMap<>();
    private static Map<Integer, GetBlockProcessor> getBlockByHashProcessorMap = new HashMap<>();
    private static Map<Integer, GetSmallBlockProcessor> getSmallBlockProcessorHashMap = new HashMap<>();
    private static Map<Integer, ForwardSmallBlockProcessor> forwardSmallBlockProcessorHashMap = new HashMap<>();
    private static Map<Integer, LinkedBlockingQueue<SmallBlockMessage>> smallBlockQueueMap = new HashMap<>();

    public static void startSaver(int chainId, BlockService blockService) {
        BlockSaver saver = new BlockSaver(blockService);
        saverMap.put(chainId, saver);
        ThreadUtils.createAndRunThread("saver" + chainId, saver);

        DownloadBlocksProcessor processor = new DownloadBlocksProcessor(blockService);
        downloadBlocksProcessorMap.put(chainId, processor);
        ThreadUtils.createAndRunThread("getblocks" + chainId, processor);

        GetBlockProcessor getBlockProcessor = new GetBlockProcessor(blockService);
        getBlockByHashProcessorMap.put(chainId, getBlockProcessor);
        ThreadUtils.createAndRunThread("getblock-hash-" + chainId, getBlockProcessor);

        DownloadBlockByHeightProcessor processor1 = new DownloadBlockByHeightProcessor(blockService);
        downloadBlockProcessorMap.put(chainId, processor1);
        ThreadUtils.createAndRunThread("getblock" + chainId, processor1);

        GetTxGroupProcessor txGroupProcessor = new GetTxGroupProcessor();
        getTxGroupProcessorMap.put(chainId, txGroupProcessor);
        ThreadUtils.createAndRunThread("getTxGroup" + chainId, txGroupProcessor);

        LinkedBlockingQueue<SmallBlockMessage> queue = new LinkedBlockingQueue<>(1024);
        smallBlockQueueMap.put(chainId, queue);
        SmallBlockProcessor smallBlockProcessor1 = new SmallBlockProcessor(blockService, queue);
        SmallBlockProcessor smallBlockProcessor2 = new SmallBlockProcessor(blockService, queue);
        ThreadUtils.createAndRunThread("smBlock-a-" + chainId, smallBlockProcessor1);
        ThreadUtils.createAndRunThread("smBlock-b-" + chainId, smallBlockProcessor2);


        GetSmallBlockProcessor getSmallBlockProcessor = new GetSmallBlockProcessor();
        getSmallBlockProcessorHashMap.put(chainId, getSmallBlockProcessor);
        ThreadUtils.createAndRunThread("getSMBlock" + chainId, getSmallBlockProcessor);

        ForwardSmallBlockProcessor forwardSmallBlockProcessor = new ForwardSmallBlockProcessor();
        forwardSmallBlockProcessorHashMap.put(chainId, forwardSmallBlockProcessor);
        ThreadUtils.createAndRunThread("forwardSMBlock" + chainId, forwardSmallBlockProcessor);

    }

    public static void offer(BlockSaver.Saver saver) {
        if (null == saver || saver.getChainId() <= 0) {
            return;
        }
        BlockSaver blockSaver = saverMap.get(saver.getChainId());
        blockSaver.offer(saver);
    }

    public static void offerGetBlocksMsg(HeightRangeMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        DownloadBlocksProcessor processor = downloadBlocksProcessorMap.get(message.getChainId());
        processor.offer(message);
    }

    public static void offerGetBlockMsg(HeightMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        DownloadBlockByHeightProcessor processor = downloadBlockProcessorMap.get(message.getChainId());
        processor.offer(message);
    }

    public static void offerHashListMsg(HashListMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        GetTxGroupProcessor processor = getTxGroupProcessorMap.get(message.getChainId());
        processor.offer(message);
    }

    public static void offerSmallBlockMsg(SmallBlockMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        LinkedBlockingQueue<SmallBlockMessage> queue = smallBlockQueueMap.get(message.getChainId());
        queue.offer(message);
    }

    public static void offerHashMsg(HashMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        GetBlockProcessor processor = getBlockByHashProcessorMap.get(message.getChainId());
        processor.offer(message);
    }

    public static void offerGetSmallBlockMsg(HashMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        GetSmallBlockProcessor processor = getSmallBlockProcessorHashMap.get(message.getChainId());
        processor.offer(message);
    }

    public static void offerForwardSmallBlockMsg(HashMessage message) {
        if (null == message || message.getChainId() <= 0) {
            return;
        }
        ForwardSmallBlockProcessor processor = forwardSmallBlockProcessorHashMap.get(message.getChainId());
        processor.offer(message);
    }
}
