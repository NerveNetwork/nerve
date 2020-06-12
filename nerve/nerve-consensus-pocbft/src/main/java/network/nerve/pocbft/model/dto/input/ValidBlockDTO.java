package network.nerve.pocbft.model.dto.input;

/**
 * 区块验证参数
 * Block validation parameter
 *
 * @author tag
 * 2018/11/12
 * */
public class ValidBlockDTO {
    private int chainId;
    private int download;
    private String block;
    private boolean basicVerify;
    private boolean byzantineVerify;
    private String nodeId;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getDownload() {
        return download;
    }

    public void setDownload(int download) {
        this.download = download;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public boolean isBasicVerify() {
        return basicVerify;
    }

    public void setBasicVerify(boolean basicVerify) {
        this.basicVerify = basicVerify;
    }

    public boolean isByzantineVerify() {
        return byzantineVerify;
    }

    public void setByzantineVerify(boolean byzantineVerify) {
        this.byzantineVerify = byzantineVerify;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
