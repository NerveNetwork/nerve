package nerve.network.converter.model.bo;

import java.math.BigInteger;

/**
 * @author: Chino
 * @date: 2019/4/12
 */
public class NonceBalance {
    private byte[] nonce;
    private BigInteger available;

    public NonceBalance() {
    }

    public NonceBalance(byte[] nonce, BigInteger available) {
        this.nonce = nonce;
        this.available = available;
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
}
