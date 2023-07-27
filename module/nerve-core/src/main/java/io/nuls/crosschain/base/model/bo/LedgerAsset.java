/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.crosschain.base.model.bo;

import java.math.BigInteger;

/**
 * @author tangyi
 * @date 2018/11/6
 * @description
 */
public class LedgerAsset{
    /**
     * 资产是在哪条链上注册的
     * Which chain is the asset registered on
     */
    private int assetChainId;
    private int assetId;
    private String assetSymbol;
    private String assetName;
    private BigInteger initNumber = BigInteger.ZERO;
    private short decimalPlace;
    private boolean usable;
    private String address = "";

    public LedgerAsset(){
        super();
    }

    public LedgerAsset(AssetInfo assetInfo, int assertChainId) {
        this.assetChainId = assertChainId;
        this.assetId = assetInfo.getAssetId();
        this.assetSymbol = assetInfo.getSymbol();
        if(assetInfo.getAssetName() == null || assetInfo.getAssetName().isBlank()){
            this.assetName = assetInfo.getSymbol();
        }else{
            this.assetName = assetInfo.getAssetName();
        }
        this.decimalPlace = (short) assetInfo.getDecimalPlaces();
        this.usable = assetInfo.isUsable();
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

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
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

    public void setInitNumber(BigInteger initNumber) {
        this.initNumber = initNumber;
    }

    public short getDecimalPlace() {
        return decimalPlace;
    }

    public void setDecimalPlace(short decimalPlace) {
        this.decimalPlace = decimalPlace;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
