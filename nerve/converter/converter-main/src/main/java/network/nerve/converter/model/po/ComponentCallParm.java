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

package network.nerve.converter.model.po;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Heterogeneous Chain Components version2 Call parameters for
 * @author: Loki
 * @date: 2020/9/21
 */
public class ComponentCallParm implements Serializable {
    private int heterogeneousId;
    private int txType;
    private String txHash;
    private String signAddress;
    private String[] inAddress;
    private String[] outAddress;
    private int orginTxCount;
    private String signed;

    private String toAddress;
    private BigInteger value;
    private Integer assetId;
    private byte proposalType;

    private String upgradeContract;

    public ComponentCallParm() {
    }

    public ComponentCallParm(int heterogeneousId, int txType, String txHash, String[] inAddress, String[] outAddress, int orginTxCount, String signed) {
        this.heterogeneousId = heterogeneousId;
        this.txType = txType;
        this.txHash = txHash;
        this.inAddress = inAddress;
        this.outAddress = outAddress;
        this.orginTxCount = orginTxCount;
        this.signed = signed;
    }

    public ComponentCallParm(int heterogeneousId, int txType, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) {
        this.heterogeneousId = heterogeneousId;
        this.txType = txType;
        this.txHash = txHash;
        this.signed = signed;
        this.toAddress = toAddress;
        this.value = value;
        this.assetId = assetId;
    }

    public ComponentCallParm(int heterogeneousId, int txType, byte proposalType, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) {
        this.heterogeneousId = heterogeneousId;
        this.txType = txType;
        this.proposalType = proposalType;
        this.txHash = txHash;
        this.signed = signed;
        this.toAddress = toAddress;
        this.value = value;
        this.assetId = assetId;
    }

    public ComponentCallParm(int heterogeneousId, int txType, byte proposalType, String txHash, String upgradeContract, String signed) {
        this.heterogeneousId = heterogeneousId;
        this.txType = txType;
        this.proposalType = proposalType;
        this.txHash = txHash;
        this.signed = signed;
        this.upgradeContract = upgradeContract;
    }

    public int getHeterogeneousId() {
        return heterogeneousId;
    }

    public void setHeterogeneousId(int heterogeneousId) {
        this.heterogeneousId = heterogeneousId;
    }

    public int getTxType() {
        return txType;
    }

    public void setTxType(int txType) {
        this.txType = txType;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }

    public String[] getInAddress() {
        return inAddress;
    }

    public void setInAddress(String[] inAddress) {
        this.inAddress = inAddress;
    }

    public String[] getOutAddress() {
        return outAddress;
    }

    public void setOutAddress(String[] outAddress) {
        this.outAddress = outAddress;
    }

    public int getOrginTxCount() {
        return orginTxCount;
    }

    public void setOrginTxCount(int orginTxCount) {
        this.orginTxCount = orginTxCount;
    }

    public String getSigned() {
        return signed;
    }

    public void setSigned(String signed) {
        this.signed = signed;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public String getUpgradeContract() {
        return upgradeContract;
    }

    public void setUpgradeContract(String upgradeContract) {
        this.upgradeContract = upgradeContract;
    }

    public byte getProposalType() {
        return proposalType;
    }

    public void setProposalType(byte proposalType) {
        this.proposalType = proposalType;
    }
}
