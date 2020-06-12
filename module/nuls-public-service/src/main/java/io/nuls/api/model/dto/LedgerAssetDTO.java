package io.nuls.api.model.dto;

import io.nuls.core.rpc.model.Key;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-24 11:54
 * @Description: 功能描述
 */
public class LedgerAssetDTO {


    /**
     * assetSymbol : USDI
     * assetId : 2
     * assetOwnerAddress : TNVTdN9iJVX42PxxzvhnkC7vFmTuoPnRAgtyA
     * assetName : USDI
     * initNumber : 100000000
     * txHash : e6ade54fe64bd53eb1a943edc975d48a0d568d9b22abf2d06879fe03fc23d01d
     * assetType : 1
     * decimalPlace : 8
     */

    private String assetSymbol;
    private Integer assetId;
    private String assetOwnerAddress;
    private String assetName;
    private BigInteger initNumber;
    private String txHash;
    private Integer assetType;
    private Integer decimalPlace;

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public String getAssetOwnerAddress() {
        return assetOwnerAddress;
    }

    public void setAssetOwnerAddress(String assetOwnerAddress) {
        this.assetOwnerAddress = assetOwnerAddress;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public BigInteger getInitNumber() {
        return initNumber;
    }

    public void setInitNumber(Object initNumber) {
        this.initNumber = new BigInteger(String.valueOf(initNumber));
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public Integer getAssetType() {
        return assetType;
    }

    public void setAssetType(Integer assetType) {
        this.assetType = assetType;
    }

    public Integer getDecimalPlace() {
        return decimalPlace;
    }

    public void setDecimalPlace(Integer decimalPlace) {
        this.decimalPlace = decimalPlace;
    }
}
