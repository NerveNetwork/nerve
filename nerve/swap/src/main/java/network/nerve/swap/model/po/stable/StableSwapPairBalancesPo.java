package network.nerve.swap.model.po.stable;

import io.nuls.base.basic.AddressTool;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author Niels
 */
public class StableSwapPairBalancesPo {
    private byte[] address;
    private BigInteger totalLP;
    private BigInteger[] balances;
    private long blockTimeLast;
    private long blockHeightLast;

    public StableSwapPairBalancesPo() {
    }

    public StableSwapPairBalancesPo(byte[] address, BigInteger totalLP, BigInteger[] balances, long blockTimeLast, long blockHeightLast) {
        this.address = address;
        this.totalLP = totalLP;
        this.balances = balances;
        this.blockTimeLast = blockTimeLast;
        this.blockHeightLast = blockHeightLast;
    }

    public StableSwapPairBalancesPo(byte[] address, int length) {
        this.address = address;
        this.totalLP = BigInteger.ZERO;
        this.balances = SwapUtils.emptyFillZero(new BigInteger[length]);
        this.blockTimeLast = 0L;
        this.blockHeightLast = 0L;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public BigInteger[] getBalances() {
        return balances;
    }

    public void setBalances(BigInteger[] balances) {
        this.balances = balances;
    }

    public long getBlockTimeLast() {
        return blockTimeLast;
    }

    public void setBlockTimeLast(long blockTimeLast) {
        this.blockTimeLast = blockTimeLast;
    }

    public long getBlockHeightLast() {
        return blockHeightLast;
    }

    public void setBlockHeightLast(long blockHeightLast) {
        this.blockHeightLast = blockHeightLast;
    }

    public BigInteger getTotalLP() {
        return totalLP;
    }

    public void setTotalLP(BigInteger totalLP) {
        this.totalLP = totalLP;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"address\":")
                .append(AddressTool.getStringAddressByBytes(address));
        sb.append(",\"totalLP\":")
                .append(totalLP);
        sb.append(",\"balances\":")
                .append(Arrays.toString(balances));
        sb.append(",\"blockTimeLast\":")
                .append(blockTimeLast);
        sb.append(",\"blockHeightLast\":")
                .append(blockHeightLast);
        sb.append('}');
        return sb.toString();
    }
}
