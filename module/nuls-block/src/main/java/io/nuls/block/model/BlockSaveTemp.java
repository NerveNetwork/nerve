package io.nuls.block.model;

import io.nuls.base.data.Block;
import io.nuls.core.basic.Result;
import io.nuls.core.rpc.util.NulsDateUtils;

public class BlockSaveTemp {
    private Block block;
    private byte bztAndBaseVerify = 0;
    private long time;

    public BlockSaveTemp(byte bztAndBaseVerify){
        this.bztAndBaseVerify = bztAndBaseVerify;
        this.time = NulsDateUtils.getCurrentTimeSeconds();
    }

    public BlockSaveTemp(Block block){
        this.block=block;
        this.time = NulsDateUtils.getCurrentTimeSeconds();
    }

    public BlockSaveTemp(Block block,byte bztAndBaseVerify){
        this.block=block;
        this.bztAndBaseVerify = bztAndBaseVerify;
        this.time = NulsDateUtils.getCurrentTimeSeconds();
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

    public byte getBztAndBaseVerify() {
        return bztAndBaseVerify;
    }

    public void setBztAndBaseVerify(byte bztAndBaseVerify) {
        this.bztAndBaseVerify = bztAndBaseVerify;
    }
}
