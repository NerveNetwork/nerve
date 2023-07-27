package io.nuls.block.model;

import io.nuls.base.data.Block;

public class FutureBlockData {
    private Block block;
    private boolean pocNet;
    private String nodeId;

    public FutureBlockData(Block block,String nodeId,boolean pocNet){
        this.block = block;
        this.nodeId = nodeId;
        this.pocNet = pocNet;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public boolean isPocNet() {
        return pocNet;
    }

    public void setPocNet(boolean pocNet) {
        this.pocNet = pocNet;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
