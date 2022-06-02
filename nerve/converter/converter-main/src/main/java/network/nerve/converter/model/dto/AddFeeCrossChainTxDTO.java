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

package network.nerve.converter.model.dto;

import network.nerve.converter.model.bo.HeterogeneousHash;

import java.math.BigInteger;

/**
 * 跨链追加手续费
 *
 * @author: PierreLuo
 * @date: 2022/4/14
 */
public class AddFeeCrossChainTxDTO {

    /**
     * 异构链充值交易hash
     */
    private HeterogeneousHash originalTxHash;
    /**
     * 异构链交易所在区块高度
     */
    private long heterogeneousHeight;
    /**
     * 异构链网络中 充值的from地址
     */
    private String heterogeneousFromAddress;
    /**
     * nerve 网络中的到账地址
     */
    private byte[] nerveToAddress;
    // main资产
    private BigInteger mainAssetAmount;
    private int mainAssetChainId;
    private int mainAssetId;
    private String nerveTxHash;// 提现交易hash
    private String subExtend;

    public HeterogeneousHash getOriginalTxHash() {
        return originalTxHash;
    }

    public void setOriginalTxHash(HeterogeneousHash originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public long getHeterogeneousHeight() {
        return heterogeneousHeight;
    }

    public void setHeterogeneousHeight(long heterogeneousHeight) {
        this.heterogeneousHeight = heterogeneousHeight;
    }

    public String getHeterogeneousFromAddress() {
        return heterogeneousFromAddress;
    }

    public void setHeterogeneousFromAddress(String heterogeneousFromAddress) {
        this.heterogeneousFromAddress = heterogeneousFromAddress;
    }

    public byte[] getNerveToAddress() {
        return nerveToAddress;
    }

    public void setNerveToAddress(byte[] nerveToAddress) {
        this.nerveToAddress = nerveToAddress;
    }

    public BigInteger getMainAssetAmount() {
        return mainAssetAmount;
    }

    public void setMainAssetAmount(BigInteger mainAssetAmount) {
        this.mainAssetAmount = mainAssetAmount;
    }

    public int getMainAssetChainId() {
        return mainAssetChainId;
    }

    public void setMainAssetChainId(int mainAssetChainId) {
        this.mainAssetChainId = mainAssetChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public String getSubExtend() {
        return subExtend;
    }

    public void setSubExtend(String subExtend) {
        this.subExtend = subExtend;
    }
}
