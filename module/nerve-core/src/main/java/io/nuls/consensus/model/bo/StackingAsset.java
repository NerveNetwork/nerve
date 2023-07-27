package io.nuls.consensus.model.bo;

public class StackingAsset {
    private Integer chainId;
    private Integer assetId;
    private String oracleKey;
    private boolean canBePeriodically;
    private int decimal;
    private Integer weight;
    private String simple;
    private Long stopHeight;

    public StackingAsset() {
    }

    public StackingAsset(Integer chainId,
                         Integer assetId,
                         String oracleKey,
                         boolean canBePeriodically,
                         int decimal,
                         String simple, Integer weight,Long stopHeight
    ) {
        this.chainId = chainId;
        this.assetId = assetId;
        this.oracleKey = oracleKey;
        this.canBePeriodically = canBePeriodically;
        this.decimal = decimal;
        this.simple = simple;
        this.weight = weight;
        this.stopHeight = stopHeight;
    }

    public boolean isCanBePeriodically() {
        return canBePeriodically;
    }

    public void setCanBePeriodically(boolean canBePeriodically) {
        this.canBePeriodically = canBePeriodically;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public Integer getChainId() {
        return chainId;
    }

    public void setChainId(Integer chainId) {
        this.chainId = chainId;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
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

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Long getStopHeight() {
        return stopHeight;
    }

    public void setStopHeight(Long stopHeight) {
        this.stopHeight = stopHeight;
    }
}
