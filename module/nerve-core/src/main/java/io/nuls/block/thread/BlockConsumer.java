/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.thread;

import io.nuls.base.data.Block;
import io.nuls.block.constant.BlockErrorCode;
import io.nuls.block.constant.NodeEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.BlockDownloaderParams;
import io.nuls.block.model.ChainContext;
import io.nuls.block.model.Node;
import io.nuls.block.service.BlockService;
import io.nuls.block.utils.BlockUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Blocks in the consumption sharing queue
 *
 * @author captain
 * @version 1.0
 * @date 18-11-8 afternoon5:45
 */
public class BlockConsumer implements Callable<Boolean> {

    private int chainId;
    private BlockService blockService;
    private Map<Long, Integer> failedTimesMap = new HashMap<>();

    BlockConsumer(int chainId) {
        this.chainId = chainId;
        this.blockService = SpringLiteContext.getBean(BlockService.class);
    }

    @Override
    public Boolean call() {
        ChainContext context = ContextManager.getContext(chainId);
        BlockDownloaderParams params = context.getDownloaderParams();
        long netLatestHeight = params.getNetLatestHeight();
        long pendingHeight = params.getLocalLatestHeight() + 1;
        NulsLogger logger = context.getLogger();
        Block block;
        logger.info("BlockConsumer start work");
        try {
            long begin = System.nanoTime();
            while (pendingHeight <= netLatestHeight && context.isNeedSyn()) {
                block = context.getBlockMap().remove(pendingHeight);
                if (block != null) {
                    begin = System.nanoTime();
                    boolean saveBlock = blockService.saveBlock(chainId, block, true);
                    if (!saveBlock) {
                        int value = failedTimesMap.compute(block.getHeader().getHeight(), (k, v) -> {
                            if (v == null) {
                                return 1;
                            }
                            return ++v;
                        });
                        if (value > 3) {
                            logger.info("continuity3Secondary synchronization block failed, starting to stop node operation");
                            context.stopBlock();
                        }
                        logger.error("saving block exception, height-" + pendingHeight + ", hash-" + block.getHeader().getHash());
                        context.setNeedSyn(false);
                        Thread.sleep(180000L);

                        return false;
                    }
                    pendingHeight++;
                    context.getCachedBlockSize().addAndGet(-block.size());
                    continue;
                }
                Thread.sleep(10);
                long end = System.nanoTime();
                //exceed1No height update in seconds
                if ((end - begin) / 1000000 > 1000) {
                    updateNodeStatus(context);
                    punishNode(pendingHeight, params.getNodes(), context);
                    retryDownload(pendingHeight, context);
                    begin = System.nanoTime();
                }
            }
            logger.info("BlockConsumer stop work normally");
            return context.isNeedSyn();
        } catch (Exception e) {
            logger.error("BlockConsumer stop work abnormally", e);
            context.setNeedSyn(false);
            return false;
        }
    }

    private void punishNode(long pendingHeight, List<Node> nodes, ChainContext context) {
        for (Node node : nodes) {
            if (node.getStartHeight() <= pendingHeight && pendingHeight <= node.getEndHeight()) {
                context.getLogger().error("download block from {} failed! failed height {}", node.getId(), pendingHeight);
                node.adjustCredit(false);
                return;
            }
        }
    }

    private void updateNodeStatus(ChainContext context) {
        List<Node> nodes = context.getDownloaderParams().getNodes();
        for (Node node : nodes) {
            if (node.getNodeEnum().equals(NodeEnum.WORKING) && (System.currentTimeMillis() - node.getStartTime() > 60000)) {
                node.adjustCredit(false);
                if (!node.getNodeEnum().equals(NodeEnum.TIMEOUT)) {
                    node.setNodeEnum(NodeEnum.IDLE);
                }
            }
        }
    }

    /**
     * Download failed and retry,Until successful(Batch download failed,Try again and download one by one)
     *
     * @param height Downloaded blocks
     * @return
     */
    private void retryDownload(long height, ChainContext context) throws NulsException {
        boolean download = false;
        BlockDownloaderParams downloaderParams = context.getDownloaderParams();
        List<Node> nodeList = downloaderParams.getNodes();
        nodeList.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                int val = o1.getCredit() - o2.getCredit();
                if (val < 0) {
                    return 1;
                } else if(val > 0) {
                    return -1;
                }
                return 0;
            }
        });
        for (Node node : nodeList) {
            if (node.getNodeEnum().equals(NodeEnum.TIMEOUT)) {
                continue;
            }
            context.getLogger().info("retryDownload, get block from " + node.getId() + " begin, height-" + height);
            Block block = BlockUtil.downloadBlockByHeight(chainId, node.getId(), height);
            if (block != null) {
                context.getLogger().info("retryDownload, get block from " + node.getId() + " success, height-" + height);
                download = true;
                context.getBlockMap().put(height, block);
                context.getCachedBlockSize().addAndGet(block.size());
                break;
            } else {
                node.adjustCredit(false);
            }
        }
        if (!download) {
            //If downloading this height block from all nodes fails,Stop the synchronization process,Waiting for next synchronization
            throw new NulsException(BlockErrorCode.BLOCK_SYN_ERROR);
        }
    }

}
