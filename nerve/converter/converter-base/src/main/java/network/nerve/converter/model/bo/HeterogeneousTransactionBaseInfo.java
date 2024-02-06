/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.model.bo;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author: Mimi
 * @date: 2020-03-10
 */
public class HeterogeneousTransactionBaseInfo {

    private String txHash;
    private Long blockHeight;
    /**
     * Heterogeneous chain network outgoing address
     */
    private String from;
    /**
     * Heterogeneous chain network receiving address
     */
    private String to;
    /**
     * Transfer amount
     */
    private BigInteger value;
    /**
     * Transaction time
     */
    private Long txTime;

    /**
     * Decimal places of assets
     */
    private Integer decimals;
    /**
     * Is it a contractual asset
     */
    private boolean ifContractAsset;
    /**
     * Contract address
     */
    private String contractAddress;
    /**
     * NerveThe assets of this asset under the networkid
     */
    private Integer assetId;
    /**
     * NerveUnder the networkfromCorresponding to addressnerveFormat Address(The recharge address for the recharge transaction)
     */
    private String nerveAddress;
    /**
     * Signature list for multi signature transactions(Withdrawal、Administrator Change)
     */
    private List<HeterogeneousAddress> signers;

    public List<HeterogeneousAddress> getSigners() {
        return signers;
    }

    public void setSigners(List<HeterogeneousAddress> signers) {
        this.signers = signers;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Long getTxTime() {
        return txTime;
    }

    public void setTxTime(Long txTime) {
        this.txTime = txTime;
    }

    public Integer getAssetId() {
        return assetId;
    }

    public void setAssetId(Integer assetId) {
        this.assetId = assetId;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public boolean isIfContractAsset() {
        return ifContractAsset;
    }

    public void setIfContractAsset(boolean ifContractAsset) {
        this.ifContractAsset = ifContractAsset;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getNerveAddress() {
        return nerveAddress;
    }

    public void setNerveAddress(String nerveAddress) {
        this.nerveAddress = nerveAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeterogeneousTransactionBaseInfo that = (HeterogeneousTransactionBaseInfo) o;

        if (txHash != null ? !txHash.equals(that.txHash) : that.txHash != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return txHash != null ? txHash.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"txHash\":")
                .append('\"').append(txHash).append('\"');
        sb.append(",\"blockHeight\":")
                .append(blockHeight);
        sb.append(",\"from\":")
                .append('\"').append(from).append('\"');
        sb.append(",\"to\":")
                .append('\"').append(to).append('\"');
        sb.append(",\"value\":")
                .append(value);
        sb.append(",\"txTime\":")
                .append(txTime);
        sb.append(",\"decimals\":")
                .append(decimals);
        sb.append(",\"ifContractAsset\":")
                .append(ifContractAsset);
        sb.append(",\"contractAddress\":")
                .append('\"').append(contractAddress).append('\"');
        sb.append(",\"assetId\":")
                .append(assetId);
        sb.append(",\"nerveAddress\":")
                .append('\"').append(nerveAddress).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
