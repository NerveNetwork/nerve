package io.nuls.api.model.po;

import io.nuls.api.constant.ApiConstant;

import java.util.Objects;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 11:22
 * @Description: 币种注册表，所有通过跨链进入网络的其他币种需要注册在这个对象中
 */
public class SymbolRegInfo {

    private String _id;

    private String symbol;

    private Integer assetId;

    private String contractAddress;

    private Integer chainId;

    private String fullName;

    private Integer decimals;

    private String icon;

    private double stackWeight = 0D;

    private boolean queryPrice;

    /**
     * 资产类型 [1-链内普通资产 2-链内合约资产 3-平行链资产 4-异构链资产]
     */
    private Integer source;

    private Integer level = 0;

    public static String buildId(int assetChainId,int assetId){
        return assetChainId + ApiConstant.SPACE + assetId;
    }

    public void buildId(){
        Objects.requireNonNull(this.getChainId(),"chainId can't be null");
        Objects.requireNonNull(this.getAssetId(),"assetId can't be null");
        this._id = buildId(chainId,assetId);
    }

    public Integer getChainId() {
        return chainId;
    }

    public void setChainId(Integer chainId) {
        this.chainId = chainId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimal) {
        this.decimals = decimal;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public double getStackWeight() {
        return stackWeight;
    }

    public void setStackWeight(double stackWeight) {
        this.stackWeight = stackWeight;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public boolean isQueryPrice() {
        return queryPrice;
    }

    public void setQueryPrice(boolean queryPrice) {
        this.queryPrice = queryPrice;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }




}
