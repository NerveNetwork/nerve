/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.swap.model.dto;

import java.math.BigInteger;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021/3/31
 */
public class LedgerAssetDTO {
    private int chainId;
    private int assetId;
    private String assetSymbol;
    private String assetName;
    private int assetType;
    private BigInteger initNumber;
    private int decimalPlace;

    public LedgerAssetDTO(int chainId, Map<String, Object> map) {
        this.chainId = chainId;
        this.assetId = Integer.parseInt(map.get("assetId").toString());
        this.assetSymbol = map.get("assetSymbol").toString();
        this.assetName = map.get("assetName").toString();
        this.assetType = Integer.parseInt(map.get("assetType").toString());
        this.initNumber = new BigInteger(map.get("initNumber").toString());
        this.decimalPlace = Integer.parseInt(map.get("decimalPlace").toString());
    }

    public LedgerAssetDTO(int chainId, int assetId, String assetSymbol, String assetName, int decimalPlace) {
        this.chainId = chainId;
        this.assetId = assetId;
        this.assetSymbol = assetSymbol;
        this.assetName = assetName;
        this.decimalPlace = decimalPlace;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
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

    public int getAssetType() {
        return assetType;
    }

    public void setAssetType(int assetType) {
        this.assetType = assetType;
    }

    public BigInteger getInitNumber() {
        return initNumber;
    }

    public void setInitNumber(BigInteger initNumber) {
        this.initNumber = initNumber;
    }

    public int getDecimalPlace() {
        return decimalPlace;
    }

    public void setDecimalPlace(int decimalPlace) {
        this.decimalPlace = decimalPlace;
    }
}
