package io.nuls.block.model;

import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.SmallBlock;

public class MessageInfo {
    private int chainId;
    private String nodeId;
    private NulsHash blockHash;
    private BlockHeader header;
    private SmallBlock smallBlock;

    public MessageInfo(int chainId, String nodeId, NulsHash blockHash, BlockHeader header, SmallBlock smallBlock){
        this.chainId = chainId;
        this.nodeId = nodeId;
        this.blockHash = blockHash;
        this.header = header;
        this.smallBlock = smallBlock;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NulsHash getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(NulsHash blockHash) {
        this.blockHash = blockHash;
    }

    public BlockHeader getHeader() {
        return header;
    }

    public void setHeader(BlockHeader header) {
        this.header = header;
    }

    public SmallBlock getSmallBlock() {
        return smallBlock;
    }

    public void setSmallBlock(SmallBlock smallBlock) {
        this.smallBlock = smallBlock;
    }
}
