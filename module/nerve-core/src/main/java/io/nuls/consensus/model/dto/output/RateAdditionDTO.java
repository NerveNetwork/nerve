package io.nuls.consensus.model.dto.output;

import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(name = "Node interest rate bonus category")
public class RateAdditionDTO {
    @ApiModelProperty(description = "Mortgage assetsID")
    private int assetChainId;
    @ApiModelProperty(description = "assetID")
    private int assetId;
    @ApiModelProperty(description = "oracleKey")
    private String oracleKey;
    @ApiModelProperty(description = "symbol")
    private String symbol;
    @ApiModelProperty(description = "Entrusted basic interest rate")
    private double basicRate;
    @ApiModelProperty(description = "Does it support regular reporting")
    private boolean canBePeriodically;
    @ApiModelProperty(description = "Details of commission type interest rate markup")
    private List<RateAdditionDetailDTO> detailList;

    public RateAdditionDTO(StackingAsset stackingAsset, double basicRate, boolean canBePeriodically){
        this.assetChainId = stackingAsset.getChainId();
        this.assetId = stackingAsset.getAssetId();
        this.oracleKey = stackingAsset.getOracleKey();
        this.symbol = stackingAsset.getSimple();
        this.basicRate = basicRate;
        this.canBePeriodically = canBePeriodically;
        detailList = new ArrayList<>();
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assertChainId) {
        this.assetChainId = assertChainId;
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getBasicRate() {
        return basicRate;
    }

    public void setBasicRate(double basicRate) {
        this.basicRate = basicRate;
    }

    public boolean isCanBePeriodically() {
        return canBePeriodically;
    }

    public void setCanBePeriodically(boolean canBePeriodically) {
        this.canBePeriodically = canBePeriodically;
    }

    public List<RateAdditionDetailDTO> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<RateAdditionDetailDTO> detailList) {
        this.detailList = detailList;
    }
}
