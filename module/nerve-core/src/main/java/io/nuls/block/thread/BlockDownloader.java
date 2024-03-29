/*
 *
 *  * MIT License
 *  * Copyright (c) 2017-2019 nuls.io
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package io.nuls.block.thread;

import io.nuls.block.constant.BlockErrorCode;
import io.nuls.block.constant.NodeEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HeightRangeMessage;
import io.nuls.block.model.BlockDownloaderParams;
import io.nuls.block.model.ChainContext;
import io.nuls.common.ConfigBean;
import io.nuls.block.model.Node;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static io.nuls.block.constant.CommandConstant.GET_BLOCKS_BY_HEIGHT_MESSAGE;

/**
 * Block Download Manager
 *
 * @author captain
 * @version 1.0
 * @date 18-11-9 afternoon4:25
 */
public class BlockDownloader implements Callable<Boolean> {

    /**
     * chainID
     */
    private int chainId;

    BlockDownloader(int chainId) {
        this.chainId = chainId;
    }

    @Override
    public Boolean call() {

        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        if (context.isStoping()) {
            logger.warn("The system is about to stop.......");
            return false;
        }
        BlockDownloaderParams downloaderParams = context.getDownloaderParams();
        List<Node> nodes = downloaderParams.getNodes();
        long netLatestHeight = downloaderParams.getNetLatestHeight();
        long startHeight = downloaderParams.getLocalLatestHeight() + 1;
        try {
//            logger.info("BlockDownloader start work from " + startHeight + " to " + netLatestHeight + ", nodes-" + nodes);
            ConfigBean chainParameters = context.getParameters();
            long cachedBlockSizeLimit = chainParameters.getCachedBlockSizeLimit();
            int downloadNumber = chainParameters.getDownloadNumber();
            AtomicInteger cachedBlockSize = context.getCachedBlockSize();
            long limit = context.getParameters().getCachedBlockSizeLimit() * 80 / 100;
            while (startHeight <= netLatestHeight && context.isNeedSyn()) {
                if (startHeight > context.getLatestHeight() + 2000) {
                    Thread.sleep(1000L);
                    continue;
                }
                int cachedSize = cachedBlockSize.get();
                while (cachedSize > cachedBlockSizeLimit) {
                    logger.info("BlockDownloader wait! cached block:" + context.getBlockMap().size() + ", total block size:" + cachedSize);
                    nodes.forEach(e -> e.setCredit(20));
                    Thread.sleep(3000L);
                    cachedSize = cachedBlockSize.get();
                    if (!context.isNeedSyn()) {
                        return false;
                    }
                }
                //The number of downloaded block bytes has reached the cache threshold80%When downloading, slow down the download speed
                if (cachedSize > limit) {
                    nodes.forEach(e -> e.setCredit(e.getCredit() / 2));
                }
                Node node = getNode(nodes);
                if (node == null) {
                    Thread.sleep(100L);
                    continue;
                }
                int credit = node.getCredit();
                int size = downloadNumber * credit / 100;
                size = size <= 0 ? 1 : size;
                if (startHeight + size > netLatestHeight) {
                    size = (int) (netLatestHeight - startHeight + 1);
                }
                long endHeight = startHeight + size - 1;
                //Batch assembly to obtain block messages
                HeightRangeMessage message = new HeightRangeMessage(startHeight, endHeight);
//                logger.info("Request to download block:{}-{}", startHeight, endHeight);
                //Send a message to the target node
                boolean b = NetworkCall.sendToNode(chainId, message, node.getId(), GET_BLOCKS_BY_HEIGHT_MESSAGE);
                if (b) {
                    node.setNodeEnum(NodeEnum.WORKING);
                    node.setStartTime(System.currentTimeMillis());
                    node.setStartHeight(startHeight);
                    node.setEndHeight(endHeight);
                    startHeight += size;
                } else {
                    logger.error("BlockDownloader sendToNode failed!");
                }
            }
            logger.info("BlockDownloader stop work, flag-" + context.isNeedSyn());
        } catch (Exception e) {
            logger.error("", e);
            context.setNeedSyn(false);
        }
        return context.isNeedSyn();
    }

    private Node getNode(List<Node> nodes) {
        int count = 0;
        for (Node node : nodes) {
            if (node.getNodeEnum().equals(NodeEnum.IDLE)) {
                return node;
            }
            if (node.getNodeEnum().equals(NodeEnum.TIMEOUT)) {
                count++;
            }
        }
        if (count == nodes.size()) {
            throw new NulsRuntimeException(BlockErrorCode.BLOCK_SYN_ERROR);
        }
        return null;
    }

}
