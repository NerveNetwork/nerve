package io.nuls.block.thread;
import io.nuls.base.data.NulsHash;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.FutureBlockData;
import io.nuls.block.service.BlockService;
import io.nuls.block.utils.SmallBlockCacher;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.DateUtils;

import static io.nuls.block.constant.BlockForwardEnum.*;

public class CacheBlockProcessor implements Runnable{
    private int chainId;
    private long height;
    private FutureBlockData blockData;
    public CacheBlockProcessor(int chainId, long height, FutureBlockData blockData){
        this.chainId = chainId;
        this.height = height;
        this.blockData = blockData;
    }
    @Override
    public void run() {
        BlockService blockService = SpringLiteContext.getBean(BlockService.class);
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        NulsHash hash = blockData.getBlock().getHeader().getHash();
        logger.debug("record cache block, block create time-" + DateUtils.timeStamp2DateStr(blockData.getBlock().getHeader().getTime() * 1000) + ", hash-" + hash);
        boolean b;
        boolean isPocNet = blockData.isPocNet();
        if(isPocNet){
            SmallBlockCacher.setStatus(chainId, hash, CONSENSUS_COMPLETE);
        }else {
            SmallBlockCacher.setStatus(chainId, hash, COMPLETE);
        }
        if(isPocNet){
            b = blockService.saveConsensusBlock(chainId, blockData.getBlock(), 1, true, true, false,true,blockData.getNodeId());
            if (!b) {
                SmallBlockCacher.setStatus(chainId, hash, CONSENSUS_ERROR);
            }
        }else{
            b = blockService.saveBlock(chainId, blockData.getBlock(), 1, true, false, true, blockData.getNodeId());
            if (!b) {
                SmallBlockCacher.setStatus(chainId, hash, ERROR);
            }
        }
        SmallBlockCacher.pendMessageMap.remove(hash);
    }
}
