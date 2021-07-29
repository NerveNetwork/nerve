package network.nerve.swap.model.po;

import java.math.BigInteger;

/**
 * @author Niels
 */
public class SwapPairReservesPO {

    private byte[] address;
    private BigInteger reserve0;
    private BigInteger reserve1;
    private BigInteger totalLP;
    private long blockTimeLast;
    private long blockHeightLast;

    public SwapPairReservesPO() {
    }

    public SwapPairReservesPO(byte[] address) {
        this.address = address;
        this.reserve0 = BigInteger.ZERO;
        this.reserve1 = BigInteger.ZERO;
        this.totalLP = BigInteger.ZERO;
        this.blockTimeLast = 0L;
        this.blockHeightLast = 0L;
    }

    public SwapPairReservesPO(byte[] address, BigInteger reserve0, BigInteger reserve1, BigInteger totalLP, long blockTimeLast, long blockHeightLast) {
        this.address = address;
        this.reserve0 = reserve0;
        this.reserve1 = reserve1;
        this.totalLP = totalLP;
        this.blockTimeLast = blockTimeLast;
        this.blockHeightLast = blockHeightLast;
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

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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

    public BigInteger getTotalLP() {
        return totalLP;
    }

    public void setTotalLP(BigInteger totalLP) {
        this.totalLP = totalLP;
    }
}
