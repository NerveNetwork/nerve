package network.nerve.swap.model.po;

import io.nuls.base.basic.AddressTool;
import network.nerve.swap.model.NerveToken;

import java.util.Arrays;

import static network.nerve.swap.constant.SwapConstant.BI_3;

/**
 * @author Niels
 */
public class SwapPairPO {

    private byte[] address;

    private NerveToken token0;
    private NerveToken token1;
    private NerveToken tokenLP;
    private Integer feeRate;// add by pierre at 2023/3/8 p24

    public SwapPairPO(byte[] address) {
        this.address = address;
    }


    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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

    public NerveToken getTokenLP() {
        return tokenLP;
    }

    public void setTokenLP(NerveToken tokenLP) {
        this.tokenLP = tokenLP;
    }

    public Integer getFeeRate() {
        if (feeRate == null) {
            feeRate = BI_3.intValue();
        }
        return feeRate ;
    }

    public void setFeeRate(Integer feeRate) {
        this.feeRate = feeRate;
    }

    @Override
    public SwapPairPO clone() {
        SwapPairPO po = new SwapPairPO(address);
        po.setToken0(token0.clone());
        po.setToken1(token1.clone());
        po.setTokenLP(tokenLP.clone());
        po.setFeeRate(feeRate);
        return po;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"address\":")
                .append('\"').append(AddressTool.getStringAddressByBytes(address)).append('\"');
        sb.append(",\"token0\":")
                .append('\"').append(token0.str()).append('\"');
        sb.append(",\"token1\":")
                .append('\"').append(token1.str()).append('\"');
        sb.append(",\"tokenLP\":")
                .append('\"').append(tokenLP.str()).append('\"');
        sb.append(",\"feeRate\":").append(feeRate);
        sb.append('}');
        return sb.toString();
    }
}
