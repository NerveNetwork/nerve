package io.nuls.block.model;

import io.nuls.base.data.Block;
import io.nuls.core.basic.Result;
import io.nuls.core.rpc.util.NulsDateUtils;

public class BlockSaveTemp {
    Result blockVerifyResult;
    Block block;
    long time = 0;
    public BlockSaveTemp(Result blockVerifyResult,Block block){
        this.blockVerifyResult = blockVerifyResult;
        this.block=block;
        this.time = NulsDateUtils.getCurrentTimeSeconds();
    }
    public Result getBlockVerifyResult() {
        return blockVerifyResult;
    }

    public void setBlockVerifyResult(Result blockVerifyResult) {
        this.blockVerifyResult = blockVerifyResult;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
