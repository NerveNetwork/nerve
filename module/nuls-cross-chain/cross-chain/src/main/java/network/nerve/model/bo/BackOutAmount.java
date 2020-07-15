package network.nerve.model.bo;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class BackOutAmount {
    private int chainId;
    private int assetId;
    private BigInteger out = BigInteger.ZERO;
    private BigInteger back = BigInteger.ZERO;

    public BackOutAmount(int chainId, int assetId) {
        this.chainId = chainId;
        this.assetId = assetId;
    }

    public void addOut(BigInteger amount) {
        out = out.add(amount);
    }

    public void addBack(BigInteger amount) {
        back = back.add(amount);
    }

    public int getChainId() {
        return chainId;
    }

    public int getAssetId() {
        return assetId;
    }

    public BigInteger getOut() {
        return out;
    }

    public BigInteger getBack() {
        return back;
    }
}
