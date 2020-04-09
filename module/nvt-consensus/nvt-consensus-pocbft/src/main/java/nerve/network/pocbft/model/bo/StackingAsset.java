package nerve.network.pocbft.model.bo;

public class StackingAsset {
    private int chainId;
    private int assetId;
    private String oracleKey;
    private String simple;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getOracleKey() {
        return oracleKey;
    }

    public void setOracleKey(String oracleKey) {
        this.oracleKey = oracleKey;
    }

    public String getSimple() {
        return simple;
    }

    public void setSimple(String simple) {
        this.simple = simple;
    }
}
