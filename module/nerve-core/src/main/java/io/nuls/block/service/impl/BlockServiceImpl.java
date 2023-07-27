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

package io.nuls.block.service.impl;

import io.nuls.common.ConfigBean;
import io.nuls.base.RPCUtil;
import io.nuls.base.data.*;
import io.nuls.base.data.po.BlockHeaderPo;
import io.nuls.block.constant.BlockErrorCode;
import io.nuls.block.manager.BlockChainManager;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.manager.RunnableManager;
import io.nuls.block.message.HashMessage;
import io.nuls.block.message.SmallBlockMessage;
import io.nuls.block.model.*;
import io.nuls.block.rpc.call.*;
import io.nuls.block.service.BlockService;
import io.nuls.block.storage.BlockStorageService;
import io.nuls.block.storage.ChainStorageService;
import io.nuls.block.thread.BlockSaver;
import io.nuls.block.thread.CacheBlockProcessor;
import io.nuls.block.utils.BlockUtil;
import io.nuls.block.utils.ChainGenerator;
import io.nuls.block.utils.LoggerUtil;
import io.nuls.block.utils.SmallBlockCacher;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.channel.manager.ConnectManager;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.StampedLock;

import static io.nuls.base.data.BlockHeader.BLOCK_HEADER_COMPARATOR;
import static io.nuls.block.constant.BlockForwardEnum.COMPLETE;
import static io.nuls.block.constant.BlockForwardEnum.CONSENSUS_COMPLETE;
import static io.nuls.block.constant.CommandConstant.*;
import static io.nuls.block.constant.Constant.BLOCK_HEADER_INDEX;

/**
 * 区块服务实现类
 *
 * @author captain
 * @version 1.0
 * @date 18-11-20 上午11:09
 */
@Component
public class BlockServiceImpl implements BlockService {
    @Autowired
    private ConfigurationLoader configurationLoader;
    @Autowired
    private BlockStorageService blockStorageService;
    @Autowired
    private ChainStorageService chainStorageService;

    @Override
    public Block getGenesisBlock(int chainId) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            long l = System.nanoTime();
            Block block = new Block();
            BlockHeaderPo blockHeaderPo = blockStorageService.query(chainId, 0);
            if (blockHeaderPo == null) {
                return null;
            }
            block.setHeader(BlockUtil.fromBlockHeaderPo(blockHeaderPo));
            List<Transaction> transactions = TransactionCall.getConfirmedTransactions(chainId, blockHeaderPo.getTxHashList(), 60 * 1000);
            if (transactions.isEmpty()) {
                return null;
            }
            block.setTxs(transactions);
            logger.debug("get block time-" + (System.nanoTime() - l) + ", height-0");
            return block;
        } catch (Exception e) {
            logger.error("error when getBlock by height", e);
            return null;
        }
    }

    @Override
    public Block getLatestBlock(int chainId) {
        return ContextManager.getContext(chainId).getLatestBlock();
    }

    @Override
    public BlockHeader getLatestBlockHeader(int chainId) {
        return ContextManager.getContext(chainId).getLatestBlock().getHeader();
    }

    @Override
    public BlockHeaderPo getLatestBlockHeaderPo(int chainId) {
        ChainContext context = ContextManager.getContext(chainId);
        return getBlockHeaderPo(chainId, context.getLatestHeight());
    }

    @Override
    public BlockHeader getBlockHeader(int chainId, long height) {
        return BlockUtil.fromBlockHeaderPo(getBlockHeaderPo(chainId, height));
    }

    @Override
    public BlockHeaderPo getBlockHeaderPo(int chainId, long height) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            return blockStorageService.query(chainId, height);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    @Override
    public List<BlockHeader> getBlockHeader(int chainId, long startHeight, long endHeight) {
        if (startHeight < 0 || endHeight < 0 || startHeight > endHeight) {
            return Collections.emptyList();
        }
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            int size = (int) (endHeight - startHeight + 1);
            List<BlockHeader> list = new ArrayList<>(size);
            for (long i = startHeight; i <= endHeight; i++) {
                BlockHeaderPo blockHeaderPo = blockStorageService.query(chainId, i);
                if (blockHeaderPo.getHeight() == endHeight && !blockHeaderPo.isComplete()) {
                    continue;
                }
                BlockHeader blockHeader = BlockUtil.fromBlockHeaderPo(blockHeaderPo);
                list.add(blockHeader);
            }
            return list;
        } catch (Exception e) {
            logger.error("", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BlockHeader> getBlockHeaderByRound(int chainId, long height, int round) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        try {
            int count = 0;
            BlockHeaderPo startHeaderPo = getBlockHeaderPo(chainId, height);
            byte[] extend = startHeaderPo.getExtend();
            BlockExtendsData data = new BlockExtendsData(extend);
            long roundIndex = data.getRoundIndex();
            List<BlockHeader> blockHeaders = new ArrayList<>();
            if (startHeaderPo.isComplete()) {
                blockHeaders.add(BlockUtil.fromBlockHeaderPo(startHeaderPo));
            }
            while (true) {
                height--;
                if ((height < 0)) {
                    break;
                }
                BlockHeader blockHeader = getBlockHeader(chainId, height);
                BlockExtendsData newData = blockHeader.getExtendsData();
                long newRoundIndex = newData.getRoundIndex();
                if (newRoundIndex != roundIndex) {
                    count++;
                    roundIndex = newRoundIndex;
                }
                if (count >= round) {
                    break;
                }
                blockHeaders.add(blockHeader);
            }
            blockHeaders.sort(BLOCK_HEADER_COMPARATOR);
            return blockHeaders;
        } catch (Exception e) {
            logger.error("", e);
            return Collections.emptyList();
        }
    }

    @Override
    public BlockHeader getBlockHeader(int chainId, NulsHash hash) {
        return BlockUtil.fromBlockHeaderPo(getBlockHeaderPo(chainId, hash));
    }

    @Override
    public BlockHeaderPo getBlockHeaderPo(int chainId, NulsHash hash) {
        return blockStorageService.query(chainId, hash);
    }

    @Override
    public Block getBlock(int chainId, NulsHash hash) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            Block block = new Block();
            BlockHeaderPo blockHeaderPo = blockStorageService.query(chainId, hash);
            if (blockHeaderPo == null) {
                logger.warn("hash-" + hash + " block not exists");
                return null;
            }
            block.setHeader(BlockUtil.fromBlockHeaderPo(blockHeaderPo));
            List<Transaction> transactions = TransactionCall.getConfirmedTransactions(chainId, blockHeaderPo.getTxHashList(), 10 * 1000);
            block.setTxs(transactions);
            return block;
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    @Override
    public Block getBlock(int chainId, long height) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            long l = System.nanoTime();
            Block block = new Block();
            BlockHeaderPo blockHeaderPo = blockStorageService.query(chainId, height);
            if (blockHeaderPo == null) {
                return null;
            }
            block.setHeader(BlockUtil.fromBlockHeaderPo(blockHeaderPo));
            List<Transaction> transactions = TransactionCall.getConfirmedTransactions(chainId, blockHeaderPo.getTxHashList(), 10 * 1000);
            if (transactions.isEmpty()) {
                return null;
            }
            block.setTxs(transactions);
//            logger.debug("get block time-" + (System.nanoTime() - l) + ", height-" + height);
            return block;
        } catch (Exception e) {
            logger.error("error when getBlock by height", e);
            return null;
        }
    }

    @Override
    public List<Block> getBlock(int chainId, long startHeight, long endHeight) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            List<Block> list = new ArrayList<>();
            for (long i = startHeight; i <= endHeight; i++) {
                Block block = getBlock(chainId, i);
                if (block == null) {
                    return Collections.emptyList();
                }
                list.add(block);
            }
            return list;
        } catch (Exception e) {
            logger.error("", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean saveBlock(int chainId, Block block, boolean needLock) {
        return saveBlock(chainId, block, false, 0, needLock, false, false, false, null);
    }

    @Override
    public boolean saveConsensusBlock(int chainId, Block block, int download, boolean needLock, boolean broadcast, boolean forward, boolean isRecPocNet, String nodeId) {
        return saveBlock(chainId, block, false, download, needLock, broadcast, forward, isRecPocNet, nodeId);
    }

    @Override
    public boolean saveBlock(int chainId, Block block, int download, boolean needLock, boolean broadcast, boolean forward, String nodeId) {
        return saveBlock(chainId, block, false, download, needLock, broadcast, forward, false, nodeId);
    }


    /**
     * 拜占庭与区块校验标识处理=================================begin
     **/
    private final static int BZT_FLAG = 0xF0;
    private final static int BLOCK_FLAG = 0x0F;
    private final static int BZT_BLOCK_FLAG = 0xFF;

    @Override
    public void handleEvidence(int chainId, String firstHash, String secondHash) {
        NulsHash firstHashData = NulsHash.fromHex(firstHash);
        putTempBlock(chainId, firstHashData, null, (byte) BZT_FLAG);
        if (isVerifyBlock(chainId, firstHashData)) {
            BlockSaveTemp firstBlock = getBlockBasicVerifyResult(chainId, firstHashData);
            boolean b = saveBlock(chainId, firstBlock.getBlock(), 1, true, true, false, null);
            if (b) {
                ConsensusCall.evidence(chainId, firstHash, secondHash);
            }
        }
    }

    @Override
    public boolean putBlockBZT(int chainId, NulsHash hash, boolean bztResult) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        if (bztResult) {
            putTempBlock(chainId, hash, null, (byte) BZT_FLAG);
            if (isVerifyBlock(chainId, hash)) {
                BlockSaveTemp blockSaveTemp = getBlockBasicVerifyResult(chainId, hash);
                if (null != blockSaveTemp) {
                    logger.info("bzt存储数据height={}", blockSaveTemp.getBlock().getHeader().getHeight());
                    // 这里改成异步
                    RunnableManager.offer(new BlockSaver.Saver(chainId, blockSaveTemp.getBlock(), 1, true, true, false, null));
                    return true;
                } else {
                    logger.warn("基础验证还没做，已经收到投票结果了");
                    //异常数据
                    return false;
                }
            }
//            logger.warn("isVerifyBlock： false,");
        } else {
            //拜占庭失败，进行信息清理
            logger.warn("hash={},bzt失败了...", hash);
            clearConsensusFlag(chainId, hash);
        }
        return true;

    }

    private synchronized void putTempBlock(int chainId, NulsHash hash, Block block, byte bztAndBaseVerify) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
//        logger.debug("put varify flag:{}-{}", hash.toHex(), bztAndBaseVerify);
        Map<NulsHash, BlockSaveTemp> mapBasicVerify = ContextManager.getContext(chainId).getBlockVerifyResult();
        BlockSaveTemp blockSaveTemp = mapBasicVerify.get(hash);
        if (null == blockSaveTemp) {
            blockSaveTemp = new BlockSaveTemp(block, bztAndBaseVerify);
            mapBasicVerify.put(hash, blockSaveTemp);
        } else {
            if (block != null) {
                mapBasicVerify.get(hash).setBlock(block);
            }
            if (bztAndBaseVerify != 0) {
//                logger.info("do it.");
                mapBasicVerify.get(hash).setBztAndBaseVerify((byte) (blockSaveTemp.getBztAndBaseVerify() | bztAndBaseVerify));
            }
        }
    }

    private BlockSaveTemp getBlockBasicVerifyResult(int chainId, NulsHash hash) {
        Map<NulsHash, BlockSaveTemp> mapBasicVerify = ContextManager.getContext(chainId).getBlockVerifyResult();
        return mapBasicVerify.get(hash);
    }

    private boolean isVerifyBlock(int chainId, NulsHash hash) {
        BlockSaveTemp blockSaveTemp = getBlockBasicVerifyResult(chainId, hash);
        if (null == blockSaveTemp) {
            return false;
        }
        byte verifyHex = blockSaveTemp.getBztAndBaseVerify();
        return ((verifyHex & BLOCK_FLAG) > 0);
    }

    private boolean isVerifyBZT(int chainId, NulsHash hash) {
        BlockSaveTemp blockSaveTemp = getBlockBasicVerifyResult(chainId, hash);
        if (null == blockSaveTemp) {
            return false;
        }
        byte verifyHex = blockSaveTemp.getBztAndBaseVerify();
        return ((verifyHex & BZT_FLAG) > 0);
    }

    private boolean isVerifyBZTAndBlock(int chainId, NulsHash hash) {
        BlockSaveTemp blockSaveTemp = getBlockBasicVerifyResult(chainId, hash);
        if (null == blockSaveTemp) {
            return false;
        }
        byte verifyHex = blockSaveTemp.getBztAndBaseVerify();
        return ((verifyHex & BZT_BLOCK_FLAG) == BZT_BLOCK_FLAG);
    }

    private void clearConsensusFlag(int chainId, NulsHash hash) {
        Map<NulsHash, BlockSaveTemp> mapBasicVerify = ContextManager.getContext(chainId).getBlockVerifyResult();
        mapBasicVerify.remove(hash);
        SmallBlockCacher.consensusNodeMap.remove(hash);
        SmallBlockCacher.nodeMap.remove(hash);
    }

    /**
     * 拜占庭与区块校验标识处理=================================end
     **/
    private boolean saveBlock(int chainId, Block block, boolean localInit, int download, boolean needLock,
                              boolean broadcast, boolean forward, boolean isRecPocNet, String nodeId) {
        long startTime = System.nanoTime();
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        if (context.isStoping()) {
            logger.warn("The system is about to stop.......");
            return false;
        }

        BlockHeader header = block.getHeader();
        long height = header.getHeight();
        NulsHash hash = header.getHash();
        StampedLock lock = context.getLock();
        long l = 0;
        if (needLock) {
            l = lock.writeLock();
        }
        logger.info("====startSaveBlock height={},hash={},isRecPocNet={},download={},nodeId={}", height, hash.toHex(), isRecPocNet, download, nodeId);
        try {
            if (block.getHeader().getHeight() > 0 && block.getHeader().getHeight() <= context.getLatestHeight()) {
                logger.info("=====Block has been saved height={},hash={},isRecPocNet={},download={},nodeId={}", height, hash.toHex(), isRecPocNet, download, nodeId);
                return true;
            }
            //默认不进行拜占庭校验
            boolean needByzantine;
            //默认必须基础校验
            boolean needBasicVerify = true;
            boolean didBZTFlag = isVerifyBZT(chainId, hash);
            //还未bzt过,并且不是共识传送，并且不是同步区块情况下byzantine=true
            if (didBZTFlag || isRecPocNet || download == 0) {
                needByzantine = false;
            } else {
                needByzantine = true;
            }

            if (isVerifyBlock(chainId, hash)) {
                //已经基础校验过
                needBasicVerify = false;
            }
            //基础与bzt校验存在一个
            if (needBasicVerify || needByzantine) {
                //1.验证区块
                Result result = verifyBlock(chainId, block, localInit, download, needBasicVerify, needByzantine, nodeId, isRecPocNet);
                if (result.isFailed()) {
                    logger.error("verifyBlock fail! height-" + height + "," + result.getErrorCode().getMsg());
                    return false;
                } else {
                    //成功进行基础校验置位
//                    logger.debug("=======verifyBlock success hash={} basicVerify={} ", hash, needBasicVerify);
                    if (needBasicVerify) {
                        putTempBlock(chainId, hash, block, (byte) BLOCK_FLAG);
                    }
                    if (null != result.getData()) {
                        Boolean bztValue = (Boolean) ((Map<String, Object>) result.getData()).get("bzt_value");
                        if (null != bztValue && bztValue) {
                            didBZTFlag = true;
                            //把拜占庭结果放集合里
                            putTempBlock(chainId, hash, null, (byte) BZT_FLAG);
//                            logger.debug("验证的同时，得到了拜占庭结果");
                        }
                    }
                }
            }
            //同步\链切换\孤儿链对接过程中不进行区块广播 download=0 同步中，1新块
            if (download == 1) {
                if (isRecPocNet && broadcast) {
                    logger.debug("=======broadcastPocNetBlock hash={}", hash);
                    if (didBZTFlag) {
                        CachedSmallBlock cachedSmallBlock = SmallBlockCacher.getCachedSmallBlock(chainId, hash);
                        if (cachedSmallBlock != null) {
                            cachedSmallBlock.setPocNet(false);
                        }
                        //bzt已过,可以全网转发
                        broadcastBlock(chainId, block);
                        SmallBlockCacher.setStatus(chainId, hash, COMPLETE);
                    } else {
                        broadcastPocNetBlock(chainId, block);
                        SmallBlockCacher.setStatus(chainId, hash, CONSENSUS_COMPLETE);
                    }
                } else if (broadcast && didBZTFlag) {
                    broadcastBlock(chainId, block);
                }
                if (forward && didBZTFlag) {
                    forwardBlock(chainId, hash, height, null);
                }
            }
            /**
             * 拜占庭没完成，先返回等待
             */
            if (download == 1 && !isVerifyBZTAndBlock(chainId, hash)) {
                logger.info("wait for BZTAndBlock  bzt={} verify={},height-{}", isVerifyBZT(chainId, hash), isVerifyBlock(chainId, hash), height);
                return true;
            }

            //区块持久化
            if (!persistBlock(chainId, block, localInit, download)) {
                logger.warn("presist ", height);
                return false;
            }

            //6.如果不是第一次启动,则更新主链属性
            if (!localInit) {
                context.setLatestBlock(block);
                Chain masterChain = BlockChainManager.getMasterChain(chainId);
                masterChain.setEndHeight(masterChain.getEndHeight() + 1);
                int heightRange = context.getParameters().getHeightRange();
                Deque<NulsHash> hashList = masterChain.getHashList();
                if (hashList.size() >= heightRange) {
                    hashList.removeFirst();
                }
                hashList.addLast(hash);
            }
            Response response = MessageUtil.newSuccessResponse("");
            Map<String, Object> responseData = new HashMap<>(2);
            responseData.put("value", height);
            responseData.put("time", header.getTime());
            responseData.put("syncStatusEnum", context.getSimpleStatus());

            Map<String, Object> sss = new HashMap<>(2);
            sss.put(LATEST_HEIGHT, responseData);
            response.setResponseData(sss);
            ConnectManager.eventTrigger(LATEST_HEIGHT, response);
            context.setNetworkHeight(height);
            long elapsedNanos = System.nanoTime() - startTime;
            //清除共识标识
            clearConsensusFlag(chainId, hash);
            context.getFutureBlockCache().remove(height);

            logger.info("save block,height-" + height + ",time-" + (elapsedNanos / 1000000) + "ms,blocktime: " + NulsDateUtils.timeStamp2Str(block.getHeader().getTime() * 1000) + ",txCount-" + block.getHeader().getTxCount() + ",hash-" + hash + ", size-" + block.size());
            //区块处理完成之后，查看本地是否缓存有下一个区块，如果存在下一个区块则直接拿出下一个区块保存
            if (height > 0 && download == 1) {
                handleCacheBlock(chainId, height + 1);
            }
            return true;
        } finally {
            if (needLock) {
                lock.unlockWrite(l);
            }
            if (context.isStoping()) {
                logger.warn("The system is about to stop.......");
            }

        }
    }

    /**
     * 区块保存成功之后，验证本地是否存在下一个高度的区块，如果存在则开启线程处理下一个区块
     */
    private void handleCacheBlock(int chainId, long height) {
        ChainContext context = ContextManager.getContext(chainId);
        if (context.getFutureBlockCache().containsKey(height)) {
            for (FutureBlockData blockData : context.getFutureBlockCache().get(height).values()) {
                context.getLogger().debug("====已收到下一个区块，处理下一个区块：height:{},hash:{}", height, blockData.getBlock().getHeader().getHash());
                context.getThreadPool().execute(new CacheBlockProcessor(chainId, height, blockData));
            }
        }
    }

    private boolean persistBlock(int chainId, Block block, boolean localInit, int download) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        BlockHeader header = block.getHeader();
        long height = header.getHeight();
        NulsHash hash = header.getHash();
        //2.设置最新高度,如果失败则恢复上一个高度
        while (!blockStorageService.setLatestHeight(chainId, height)) {
            logger.error("setHeight false, height-" + height);
        }
        //3.保存区块头, 保存交易
        BlockHeaderPo blockHeaderPo = BlockUtil.toBlockHeaderPo(block);
        while (!blockStorageService.save(chainId, blockHeaderPo)) {
            logger.error("headerSave error, height-" + height + ", hash-" + hash);
        }
        //4.通知交易模块保存交易
        while (!TransactionCall.save(chainId, blockHeaderPo, block.getTxs(), localInit, null)) {
            logger.error("Transaction save error, height-" + height + ", hash-" + hash);
        }
        //5.通知共识模块保存区块
        while (!ConsensusCall.saveNotice(chainId, header, localInit, download)) {
            logger.error("consensus notice fail! height-" + height);
        }
        //6.通知协议升级模块保存
        blockHeaderPo.setComplete(true);
        while (!ProtocolCall.saveNotice(chainId, header)) {
            logger.error("ProtocolCall saveNotice fail! height-" + height);
        }
        while (!blockStorageService.save(chainId, blockHeaderPo)) {
            logger.error("Block save fail, height-" + height);
        }
        //7.高度变更通知
        while (!TransactionCall.heightNotice(chainId, height)) {
            logger.error("Transaction height notice fail, height-" + height);
        }
        try {
            CrossChainCall.heightNotice(chainId, height, RPCUtil.encode(block.getHeader().serialize()));
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.error(e);
        }
        return true;
    }

    @Override
    public boolean rollbackBlock(int chainId, long height, boolean needLock) {
        BlockHeaderPo blockHeaderPo = getBlockHeaderPo(chainId, height);
        return rollbackBlock(chainId, blockHeaderPo, needLock);
    }

    @Override
    public boolean rollbackBlock(int chainId, BlockHeaderPo blockHeaderPo, boolean needLock) {
        long startTime = System.nanoTime();
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        long height = blockHeaderPo.getHeight();
        if (height == 0) {
            logger.warn("can't rollback GenesisBlock!");
            return true;
        }
        StampedLock lock = context.getLock();
        long l = 0;
        if (needLock) {
            l = lock.writeLock();
        }
        try {
            BlockHeader blockHeader = BlockUtil.fromBlockHeaderPo(blockHeaderPo);
            blockHeaderPo.setComplete(false);
            if (!blockStorageService.save(chainId, blockHeaderPo) || !ProtocolCall.rollbackNotice(chainId, blockHeader)) {
                logger.error("ProtocolCall rollbackNotice fail! height-" + height);
                return false;
            }

            if (!ConsensusCall.rollbackNotice(chainId, height)) {
                if (!ProtocolCall.saveNotice(chainId, blockHeader)) {
                    throw new NulsRuntimeException(BlockErrorCode.PU_SAVE_ERROR);
                }
                logger.error("ConsensusCall rollbackNotice fail! height-" + height);
                return false;
            }

            if (!TransactionCall.rollback(chainId, blockHeaderPo)) {
                if (!ConsensusCall.saveNotice(chainId, blockHeader, false)) {
                    throw new NulsRuntimeException(BlockErrorCode.CS_SAVE_ERROR);
                }
                if (!ProtocolCall.saveNotice(chainId, blockHeader)) {
                    throw new NulsRuntimeException(BlockErrorCode.PU_SAVE_ERROR);
                }
                logger.error("TransactionCall rollback fail! height-" + height);
                return false;
            }

            if (!blockStorageService.remove(chainId, height)) {
                blockHeaderPo.setComplete(true);
                if (!blockStorageService.save(chainId, blockHeaderPo)) {
                    throw new NulsRuntimeException(BlockErrorCode.HEADER_SAVE_ERROR);
                }
                if (!TransactionCall.saveNormal(chainId, blockHeaderPo, TransactionCall.getTransactions(chainId, blockHeaderPo.getTxHashList(), true), null)) {
                    throw new NulsRuntimeException(BlockErrorCode.TX_SAVE_ERROR);
                }
                if (!ConsensusCall.saveNotice(chainId, blockHeader, false)) {
                    throw new NulsRuntimeException(BlockErrorCode.CS_SAVE_ERROR);
                }
                if (!ProtocolCall.saveNotice(chainId, blockHeader)) {
                    throw new NulsRuntimeException(BlockErrorCode.PU_SAVE_ERROR);
                }
                logger.error("blockStorageService remove fail! height-" + height);
                return false;
            }
            if (!blockStorageService.setLatestHeight(chainId, height - 1)) {
                if (!blockStorageService.setLatestHeight(chainId, height)) {
                    throw new NulsRuntimeException(BlockErrorCode.UPDATE_HEIGHT_ERROR);
                }
                blockHeaderPo.setComplete(true);
                if (!blockStorageService.save(chainId, blockHeaderPo)) {
                    throw new NulsRuntimeException(BlockErrorCode.HEADER_SAVE_ERROR);
                }
                if (!TransactionCall.saveNormal(chainId, blockHeaderPo, TransactionCall.getTransactions(chainId, blockHeaderPo.getTxHashList(), true), null)) {
                    throw new NulsRuntimeException(BlockErrorCode.TX_SAVE_ERROR);
                }
                if (!ConsensusCall.saveNotice(chainId, blockHeader, false)) {
                    throw new NulsRuntimeException(BlockErrorCode.CS_SAVE_ERROR);
                }
                if (!ProtocolCall.saveNotice(chainId, blockHeader)) {
                    throw new NulsRuntimeException(BlockErrorCode.PU_SAVE_ERROR);
                }
                logger.error("rollback setLatestHeight fail! height-" + height);
                return false;
            }
            try {
                TransactionCall.heightNotice(chainId, height - 1);
                CrossChainCall.heightNotice(chainId, height - 1, RPCUtil.encode(blockHeader.serialize()));
            } catch (Exception e) {
                LoggerUtil.COMMON_LOG.error(e);
            }
            context.setLatestBlock(getBlock(chainId, height - 1));
            Chain masterChain = BlockChainManager.getMasterChain(chainId);
            masterChain.setEndHeight(height - 1);
            Deque<NulsHash> hashList = masterChain.getHashList();
            hashList.removeLast();
            int heightRange = context.getParameters().getHeightRange();
            if (height - heightRange >= 0) {
                hashList.addFirst(getBlockHash(chainId, height - heightRange));
            }
            long elapsedNanos = System.nanoTime() - startTime;
            logger.info("rollback block success, time-" + (elapsedNanos / 1000000) + "ms, height-" + height + ", txCount-" + blockHeaderPo.getTxCount() + ", hash-" + blockHeaderPo.getHash());
            Response response = MessageUtil.newSuccessResponse("");
            Map<String, Object> responseData = new HashMap<>(2);
            responseData.put("value", height - 1);
            responseData.put("time", context.getLatestBlock().getHeader().getTime());
            responseData.put("syncStatusEnum", context.getSimpleStatus());
            Map<String, Object> sss = new HashMap<>(2);
            sss.put(LATEST_HEIGHT, responseData);
            response.setResponseData(sss);
            ConnectManager.eventTrigger(LATEST_HEIGHT, response);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (needLock) {
                lock.unlockWrite(l);
            }
        }
    }

    @Override
    public boolean forwardBlock(int chainId, NulsHash hash, long height, String excludeNode) {
        HashMessage message = new HashMessage(hash, height);
        return NetworkCall.broadcast(chainId, message, excludeNode, FORWARD_SMALL_BLOCK_MESSAGE);
    }

    @Override
    public boolean forwardPocNetBlock(int chainId, NulsHash hash, long height, String excludeNode) {
        HashMessage message = new HashMessage(hash, height);
        return NetworkCall.broadcastPocNet(chainId, message, excludeNode, FORWARD_SMALL_BLOCK_MESSAGE);
    }

    @Override
    public boolean broadcastBlock(int chainId, Block block) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        SmallBlockMessage message = new SmallBlockMessage();
        message.setSmallBlock(BlockUtil.getSmallBlock(chainId, block));
        String excludeNodes = null;
        if (SmallBlockCacher.nodeMap.containsKey(block.getHeader().getHash())) {
            excludeNodes = String.join(",", SmallBlockCacher.nodeMap.get(block.getHeader().getHash()));
        }
        byte[] voteResult = context.getVoteResultCache().get(block.getHeader().getHash());
        if (null == voteResult) {
            voteResult = ConsensusCall.getVoteResult(chainId, block.getHeader().getHash());
            if (null != voteResult) {
                context.getVoteResultCache().cache(block.getHeader().getHash(), voteResult);
            }
        }
        message.setVoteResult(voteResult);
        if (null == message.getVoteResult()) {
            logger.info("没有投票结果 -" + block.getHeader().getHash());
        }
        boolean broadcast = NetworkCall.broadcast(chainId, message, excludeNodes, SMALL_BLOCK_MESSAGE);
        logger.debug("hash-" + block.getHeader().getHash() + ", broadcast-" + broadcast);
        return broadcast;
    }

    @Override
    public boolean broadcastPocNetBlock(int chainId, Block block) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        SmallBlockMessage message = new SmallBlockMessage();
        message.setSmallBlock(BlockUtil.getSmallBlock(chainId, block));
        String excludeNodes = null;
        if (SmallBlockCacher.consensusNodeMap.containsKey(block.getHeader().getHash())) {
            excludeNodes = String.join(",", SmallBlockCacher.consensusNodeMap.get(block.getHeader().getHash()));
        }
        boolean broadcast = NetworkCall.broadcastPocNet(chainId, message, excludeNodes, SMALL_BLOCK_BZT_MESSAGE);
        logger.debug("hash-" + block.getHeader().getHash() + ", broadcast-" + broadcast);
        return broadcast;
    }

    private Result verifyBlock(int chainId, Block block, boolean localInit, int download,
                               boolean needBasicVerify, boolean needByzantineVerify, String nodeId, boolean isPocNet) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        BlockHeader header = block.getHeader();
        //0.版本验证：通过获取block中extends字段的版本号
        if (header.getHeight() > 0 && !ProtocolCall.checkBlockVersion(chainId, header)) {
            logger.error("checkBlockVersion failed! height-" + header.getHeight());
            return Result.getFailed(BlockErrorCode.BLOCK_VERIFY_ERROR);
        }

        //1.验证一些基本信息如区块大小限制、字段非空验证
        boolean basicVerify = BlockUtil.basicVerify(chainId, block);
        if (localInit) {
            logger.debug("basicVerify-" + basicVerify);
            if (basicVerify) {
                return Result.getSuccess(BlockErrorCode.SUCCESS);
            } else {
                return Result.getFailed(BlockErrorCode.BLOCK_VERIFY_ERROR);
            }
        }
        //分叉验证
        boolean forkVerify = BlockUtil.forkVerify(chainId, block, isPocNet, nodeId);
        if (!forkVerify) {
            logger.debug("forkVerify-" + forkVerify);
            return Result.getFailed(BlockErrorCode.BLOCK_VERIFY_ERROR);
        }
        //共识验证
        Result consensusVerify = ConsensusCall.verifyCs(chainId, block, nodeId, download, needBasicVerify, needByzantineVerify);
        if (consensusVerify.isFailed()) {
            logger.error("consensusVerify-" + consensusVerify);
            return Result.getFailed(BlockErrorCode.BLOCK_VERIFY_ERROR);
        }
        return consensusVerify;
    }

    private boolean initLocalBlocks(int chainId) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger logger = context.getLogger();
        Block block;
        Block genesisBlock;
        try {
            genesisBlock = getGenesisBlock(chainId);
            //1.判断有没有创世块,如果没有就初始化创世块并保存
            if (null == genesisBlock) {
                ConfigBean chainParameters = context.getParameters();
                String genesisBlockPath = chainParameters.getGenesisBlockPath();
                if (StringUtils.isBlank(genesisBlockPath)) {
                    genesisBlock = GenesisBlock.getInstance(chainId, chainParameters.getAssetId());
                } else {
                    ConfigurationLoader.ConfigItem item = configurationLoader.getConfigItem("genesisBlockPath");
                    String configFile = item.getConfigFile();
                    String value = item.getValue();
                    File file = new File(value);
                    if (file.isAbsolute()) {
                        genesisBlock = GenesisBlock.getInstance(chainId, chainParameters.getAssetId(), Files.readString(file.toPath()));
                    } else {
                        configFile = configFile.substring(0, configFile.lastIndexOf(File.separator));
                        genesisBlock = GenesisBlock.getInstance(chainId, chainParameters.getAssetId(), Files.readString(Path.of(configFile, value)));
                    }
                }
                putBlockBZT(chainId, genesisBlock.getHeader().getHash(), true);
                boolean b = saveBlock(chainId, genesisBlock, true, 0, false, false, false, false, null);
                if (!b) {
                    throw new NulsRuntimeException(BlockErrorCode.SAVE_GENESIS_ERROR);
                }
            }

            //2.获取缓存的最新区块高度（缓存的最新高度与实际的最新高度最多相差1,理论上不会有相差多个高度的情况,所以异常场景也只考虑了高度相差1）
            long latestHeight = blockStorageService.queryLatestHeight(chainId);

            //3.查询有没有这个高度的区块头
            BlockHeaderPo blockHeader = blockStorageService.query(chainId, latestHeight);
            //如果没有对应高度的header,说明缓存的本地高度错误,更新高度
            if (blockHeader == null) {
                latestHeight = latestHeight - 1;
                blockStorageService.setLatestHeight(chainId, latestHeight);
            }
            //4.latestHeight已经维护成功,上面的步骤保证了latestHeight这个高度的区块数据在本地是完整的,但是区块数据的内容并不一定是正确的,区块同步之前会继续验证latestBlock
            block = getBlock(chainId, latestHeight);
            //5.本地区块维护成功
            context.setLatestBlock(block);
            context.setGenesisBlock(genesisBlock);
            BlockChainManager.setMasterChain(chainId, ChainGenerator.generateMasterChain(chainId, block, this));
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
        return true;
    }

    @Override
    public void init(int chainId) {
        boolean initLocalBlocks = initLocalBlocks(chainId);
        if (!initLocalBlocks) {
            throw new NulsRuntimeException(BlockErrorCode.INIT_ERROR);
        }
    }

    @Override
    public NulsHash getBlockHash(int chainId, long height) {
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        try {
            byte[] key = SerializeUtils.uint64ToByteArray(height);
            byte[] value = RocksDBService.get(BLOCK_HEADER_INDEX + chainId, key);
            if (value == null) {
                return null;
            }
            return new NulsHash(value);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }
}
