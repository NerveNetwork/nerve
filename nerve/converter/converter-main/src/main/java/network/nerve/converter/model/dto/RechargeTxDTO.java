package network.nerve.converter.model.dto;

import java.math.BigInteger;

/**
 * 组装充值交易的参数
 * @author: Loki
 * @date: 2020-03-02
 */
public class RechargeTxDTO {

    /**
     * 异构链充值交易hash / 提案交易hash
     */
    String originalTxHash;
    /**
     * 充值到账NERVE地址
     */
    String toAddress;

    /**
     * 资产链id（异构链id）
     */
    int heterogeneousChainId;
    /**
     * 充值的资产Id
     */
    int heterogeneousAssetId;
    /**
     * 充值的金额
     */
    BigInteger amount;

    long txtime;

    public String getOriginalTxHash() {
        return originalTxHash;
    }

    public void setOriginalTxHash(String originalTxHash) {
        this.originalTxHash = originalTxHash;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public int getHeterogeneousAssetId() {
        return heterogeneousAssetId;
    }

    public void setHeterogeneousAssetId(int heterogeneousAssetId) {
        this.heterogeneousAssetId = heterogeneousAssetId;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public long getTxtime() {
        return txtime;
    }

    public void setTxtime(long txtime) {
        this.txtime = txtime;
    }
}