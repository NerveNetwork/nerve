package io.nuls.api.model.po;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-18 16:38
 * @Description: symbol 基础信息
 * symbol基础信息由多个来源获取
 * 1.通过配置文件加载
 * 2.通过异构跨链交易获取
 * 3.通过生态内跨链交易获取
 * id由 chainId - assetId 组合而成
 */
public class SymbolBaseInfo {

    private String _id;

    private Integer chainId;

    private Integer assetId;

    private String symbol;

    private String fullName;

    private int decimal;

    private String icon;

    private int stackWeight = 0;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getStackWeight() {
        return stackWeight;
    }

    public void setStackWeight(int stackWeight) {
        this.stackWeight = stackWeight;
    }
}
