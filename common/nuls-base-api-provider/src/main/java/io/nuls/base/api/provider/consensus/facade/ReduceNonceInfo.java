package io.nuls.base.api.provider.consensus.facade;

public class ReduceNonceInfo {
    private String deposit;
    private String nonce;

    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}
