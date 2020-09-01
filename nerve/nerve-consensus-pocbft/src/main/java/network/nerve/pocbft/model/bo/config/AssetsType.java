package network.nerve.pocbft.model.bo.config;

/**
 * @author Niels
 */
public class AssetsType {
    private int chainId;
    private int assetsId;

    public AssetsType() {
    }

    public AssetsType(int chainId, int assetsId) {
        this.chainId = chainId;
        this.assetsId = assetsId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(int assetsId) {
        this.assetsId = assetsId;
    }
}
