package network.nerve.swap.model.bo;

import java.math.BigInteger;

/**
 * @author: Loki
 * @date: 2019/4/12
 */
public class NonceBalance {
    private byte[] nonce;
    private BigInteger available;
    private BigInteger freeze;

    public NonceBalance() {
    }

    public NonceBalance(byte[] nonce, BigInteger available, BigInteger freeze) {
        this.nonce = nonce;
        this.available = available;
        this.freeze = freeze;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public BigInteger getAvailable() {
        return available;
    }

    public void setAvailable(BigInteger available) {
        this.available = available;
    }

    public BigInteger getFreeze() {
        return freeze;
    }

    public void setFreeze(BigInteger freeze) {
        this.freeze = freeze;
    }
}
