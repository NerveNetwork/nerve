package network.nerve.swap.model.po.stable;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class StableSwapUserLiquidityPo {
    private byte[] address;
    private BigInteger liquidity;
    private BigInteger[] amounts;

    public StableSwapUserLiquidityPo() {
    }

    public StableSwapUserLiquidityPo(byte[] address, BigInteger liquidity, BigInteger[] amounts) {
        this.address = address;
        this.liquidity = liquidity;
        this.amounts = amounts;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public BigInteger getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(BigInteger liquidity) {
        this.liquidity = liquidity;
    }

    public BigInteger[] getAmounts() {
        return amounts;
    }

    public void setAmounts(BigInteger[] amounts) {
        this.amounts = amounts;
    }

    public void addLiquidity(BigInteger liquidity) {
        if (this.liquidity == null) {
            this.liquidity = liquidity;
        } else {
            this.liquidity = this.liquidity.add(liquidity);
        }
    }

    public void addAmounts(BigInteger[] amounts) {
        if (this.amounts == null) {
            this.amounts = amounts;
        } else {
            int length = this.amounts.length;
            for (int i = 0; i < length; i++) {
                BigInteger a = this.amounts[i];
                if (a == null) {
                    this.amounts[i] = amounts[i];
                } else {
                    if (amounts[i] != null) {
                        this.amounts[i] = a.add(amounts[i]);
                    }
                }
            }
        }
    }
}
