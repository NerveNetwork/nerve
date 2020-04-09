package io.nuls.api.model.po;

import io.nuls.api.constant.ConverterTxType;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-23 17:41
 * @Description: 异构跨链相关交易
 */
public class ConverterTxInfo extends TxDataInfo {

    /**
     * 本链交易hash
     */
    private String txHash;

    /**
     * 异构链交易hash
     */
    private String outerTxHash;

    private String address;

    /**
     * 跨链类型
     * @see ConverterTxType
     */
    private String converterType;

    private Long createTime;

    private Long blockHeight;

    private BigInteger amount;

    private int assetChainId;

    private int assetId;

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getOuterTxHash() {
        return outerTxHash;
    }

    public void setOuterTxHash(String outerTxHash) {
        this.outerTxHash = outerTxHash;
    }

    public String getConverterType() {
        return converterType;
    }

    public void setConverterType(String converterType) {
        this.converterType = converterType;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
