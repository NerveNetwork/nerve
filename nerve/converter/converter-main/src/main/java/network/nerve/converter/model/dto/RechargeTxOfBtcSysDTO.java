package network.nerve.converter.model.dto;

import java.math.BigInteger;

/**
 * Parameters for assembly recharge transactions
 * @author: Loki
 * @date: 2020-03-02
 */
public class RechargeTxOfBtcSysDTO {

    /**
     * Heterogeneous chain recharge transactionshash / Proposal transactionhash
     */
    String htgTxHash;
    /**
     * Heterogeneous chainfromaddress
     */
    String htgFrom;
    /**
     * Asset Chainid（Heterogeneous chainid）
     */
    int htgChainId;
    /**
     * Recharged assetsId
     */
    int htgAssetId;
    long htgTxTime;
    long htgBlockHeight;
    /**
     * Recharge to accountNERVEaddress
     */
    String to;
    /**
     * Recharged amount
     */
    BigInteger amount;
    BigInteger fee;
    String feeTo;

    // Simultaneously rechargetokenandmain,mainAmount placed in the new fieldmainAmountin
    boolean depositII;
    BigInteger mainAmount;
    String extend;

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

    public String getHtgTxHash() {
        return htgTxHash;
    }

    public void setHtgTxHash(String htgTxHash) {
        this.htgTxHash = htgTxHash;
    }

    public String getHtgFrom() {
        return htgFrom;
    }

    public void setHtgFrom(String htgFrom) {
        this.htgFrom = htgFrom;
    }

    public int getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(int htgChainId) {
        this.htgChainId = htgChainId;
    }

    public int getHtgAssetId() {
        return htgAssetId;
    }

    public void setHtgAssetId(int htgAssetId) {
        this.htgAssetId = htgAssetId;
    }

    public long getHtgTxTime() {
        return htgTxTime;
    }

    public void setHtgTxTime(long htgTxTime) {
        this.htgTxTime = htgTxTime;
    }

    public long getHtgBlockHeight() {
        return htgBlockHeight;
    }

    public void setHtgBlockHeight(long htgBlockHeight) {
        this.htgBlockHeight = htgBlockHeight;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public String getFeeTo() {
        return feeTo;
    }

    public void setFeeTo(String feeTo) {
        this.feeTo = feeTo;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }
}
