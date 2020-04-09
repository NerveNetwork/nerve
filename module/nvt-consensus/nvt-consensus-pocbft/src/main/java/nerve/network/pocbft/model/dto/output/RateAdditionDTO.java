package nerve.network.pocbft.model.dto.output;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import nerve.network.pocbft.model.bo.StackingAsset;
import java.util.ArrayList;
import java.util.List;

@ApiModel(name = "节点利率加成类")
public class RateAdditionDTO {
    @ApiModelProperty(description = "抵押资产ID")
    private int assertChainId;
    @ApiModelProperty(description = "资产ID")
    private int assetId;
    @ApiModelProperty(description = "oracleKey")
    private String oracleKey;
    @ApiModelProperty(description = "simple")
    private String simple;
    @ApiModelProperty(description = "委托基础利率")
    private double basicRate;
    @ApiModelProperty(description = "委托类型利率加成明细")
    private List<RateAdditionDetailDTO> detailList;

    public RateAdditionDTO(StackingAsset stackingAsset, double basicRate){
        this.assertChainId = stackingAsset.getChainId();
        this.assetId = stackingAsset.getAssetId();
        this.oracleKey = stackingAsset.getOracleKey();
        this.simple = stackingAsset.getSimple();
        this.basicRate = basicRate;
        detailList = new ArrayList<>();
    }

    public int getAssertChainId() {
        return assertChainId;
    }

    public void setAssertChainId(int assertChainId) {
        this.assertChainId = assertChainId;
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

    public double getBasicRate() {
        return basicRate;
    }

    public void setBasicRate(double basicRate) {
        this.basicRate = basicRate;
    }

    public List<RateAdditionDetailDTO> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<RateAdditionDetailDTO> detailList) {
        this.detailList = detailList;
    }
}
