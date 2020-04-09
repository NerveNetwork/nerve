package io.nuls.api.model.po;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-06 11:27
 * @Description: 资产统计快照
 */
public class AssetSnapshotInfo {

    private String symbol;

    private int assetChainId;

    private int assetId;

    /**
     * 交易总量
     */
    private BigInteger txTotal = BigInteger.ZERO;

    /**
     * usdt价格
     */
    private BigInteger usdtPrice = BigInteger.ZERO;

    /**
     * 异构跨链流入数量
     */
    private BigInteger converterInTotal = BigInteger.ZERO;

    /**
     * 异构跨链流出数量
     */
    private BigInteger converterOutTotal = BigInteger.ZERO;

    /**
     * 地址总数
     */
    private long addressCount = 0;

    /**
     * 资产总量
     */
    private BigInteger total = BigInteger.ZERO;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getAssetChainId() {
        return assetChainId;
    }

    public void setAssetChainId(int assetChainId) {
        this.assetChainId = assetChainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public BigInteger getTotal() {
        return total;
    }

    public void setTotal(BigInteger total) {
        this.total = total;
    }

    public BigInteger getUsdtPrice() {
        return usdtPrice;
    }

    public void setUsdtPrice(BigInteger usdtPrice) {
        this.usdtPrice = usdtPrice;
    }

    public BigInteger getTxTotal() {
        return txTotal;
    }

    public void setTxTotal(BigInteger txTotal) {
        this.txTotal = txTotal;
    }

    public BigInteger getConverterInTotal() {
        return converterInTotal;
    }

    public void setConverterInTotal(BigInteger converterInTotal) {
        this.converterInTotal = converterInTotal;
    }

    public BigInteger getConverterOutTotal() {
        return converterOutTotal;
    }

    public void setConverterOutTotal(BigInteger converterOutTotal) {
        this.converterOutTotal = converterOutTotal;
    }

    public long getAddressCount() {
        return addressCount;
    }

    public void setAddressCount(long addressCount) {
        this.addressCount = addressCount;
    }
}
