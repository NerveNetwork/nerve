package network.nerve.converter.model.dto;

import java.math.BigInteger;

/**
 * Parameters for assembly recharge transactions
 * @author: Loki
 * @date: 2020-03-02
 */
public class RechargeTxDTO {

    /**
     * Heterogeneous chain recharge transactionshash / Proposal transactionhash
     */
    String originalTxHash;
    /**
     * Heterogeneous chainfromaddress
     */
    String heterogeneousFromAddress;
    /**
     * Recharge to accountNERVEaddress
     */
    String toAddress;

    /**
     * Asset Chainid（Heterogeneous chainid）
     */
    int heterogeneousChainId;
    /**
     * Recharged assetsId
     */
    int heterogeneousAssetId;
    /**
     * Recharged amount
     */
    BigInteger amount;

    long txtime;

    // Simultaneously rechargetokenandmain,mainAmount placed in the new fieldmainAmountin
    boolean depositII;
    BigInteger mainAmount;
    String extend;

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    public String getHeterogeneousFromAddress() {
        return heterogeneousFromAddress;
    }

    public void setHeterogeneousFromAddress(String heterogeneousFromAddress) {
        this.heterogeneousFromAddress = heterogeneousFromAddress;
    }

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

    public boolean isDepositII() {
        return depositII;
    }

    public void setDepositII(boolean depositII) {
        this.depositII = depositII;
    }

    public BigInteger getMainAmount() {
        return mainAmount;
    }

    public void setMainAmount(BigInteger mainAmount) {
        this.mainAmount = mainAmount;
    }
}
