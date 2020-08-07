package io.nuls.api.model.po;

import java.math.BigDecimal;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 11:54
 * @Description: 可参与stack的币种汇率
 */
public class StackSymbolPriceInfo extends TxDataInfo implements SymbolPrice{

    private String _id;

    /**
     * 币种
     */
    private String symbol;

    /**
     * 计价货币
     * .e.g USDT
     */
    private String currency;

    /**
     * 单价
     */
    private BigDecimal price;

    private long blockHeight;

    private String txHash;

    private long createTime;

    private Integer assetChainId;

    private Integer assetId;

    /**
     * 较上一次喂价的涨幅
     */
    private double change;

    public StackSymbolPriceInfo() {}

    public static StackSymbolPriceInfo empty(String symbol, String currency){
        StackSymbolPriceInfo info = new StackSymbolPriceInfo();
        info.setSymbol(symbol);
        info.setCurrency(currency);
        info.setPrice(BigDecimal.ZERO);
        return info;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Integer getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(Integer assetChainId) {
        this.assetChainId = assetChainId;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }
}
