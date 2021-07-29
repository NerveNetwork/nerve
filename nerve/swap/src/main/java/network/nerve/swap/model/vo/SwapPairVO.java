package network.nerve.swap.model.vo;

import network.nerve.swap.model.NerveToken;

/**
 * @author Niels
 */
public class SwapPairVO {

    private NerveToken token0;
    private NerveToken token1;

    public SwapPairVO(NerveToken token0, NerveToken token1) {
        this.token0 = token0;
        this.token1 = token1;
    }

    public NerveToken getToken0() {
        return token0;
    }

    public void setToken0(NerveToken token0) {
        this.token0 = token0;
    }

    public NerveToken getToken1() {
        return token1;
    }

    public void setToken1(NerveToken token1) {
        this.token1 = token1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"token0\":")
                .append(token0);
        sb.append(",\"token1\":")
                .append(token1);
        sb.append('}');
        return sb.toString();
    }

    public boolean hasToken(NerveToken token) {
        return token0.equals(token) || token1.equals(token);
    }
}
