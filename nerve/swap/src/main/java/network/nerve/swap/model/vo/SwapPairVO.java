package network.nerve.swap.model.vo;

import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.dto.SwapPairDTO;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class SwapPairVO {

    private NerveToken token0;
    private NerveToken token1;
    private BigInteger reserve0;
    private BigInteger reserve1;

    public SwapPairVO(NerveToken token0, NerveToken token1) {
        this.token0 = token0;
        this.token1 = token1;
        this.reserve0 = BigInteger.ZERO;
        this.reserve1 = BigInteger.ZERO;
    }

    public SwapPairVO(SwapPairDTO dto) {
        this.token0 = dto.getPo().getToken0();
        this.token1 = dto.getPo().getToken1();
        this.reserve0 = dto.getReserve0();
        this.reserve1 = dto.getReserve1();
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

    public BigInteger getReserve0() {
        return reserve0;
    }

    public void setReserve0(BigInteger reserve0) {
        this.reserve0 = reserve0;
    }

    public BigInteger getReserve1() {
        return reserve1;
    }

    public void setReserve1(BigInteger reserve1) {
        this.reserve1 = reserve1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"token0\":")
                .append(token0);
        sb.append(",\"token1\":")
                .append(token1);
        sb.append(",\"reserve0\":")
                .append('\"').append(reserve0).append('\"');
        sb.append(",\"reserve1\":")
                .append('\"').append(reserve1).append('\"');
        sb.append('}');
        return sb.toString();
    }

    public boolean hasToken(NerveToken token) {
        return token0.equals(token) || token1.equals(token);
    }
}
