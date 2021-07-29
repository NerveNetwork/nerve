package network.nerve.swap.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.swap.model.NerveToken;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author Niels
 */
public class RemoveLiquidityData extends BaseNulsData {

    private NerveToken tokenA;
    private NerveToken tokenB;
    private byte[] to;
    private long deadline;
    private BigInteger amountAMin;
    private BigInteger amountBMin;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(tokenA);
        stream.writeNulsData(tokenB);
        stream.write(to);
        stream.writeUint32(deadline);
        stream.writeBigInteger(amountAMin);
        stream.writeBigInteger(amountBMin);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.tokenA = byteBuffer.readNulsData(new NerveToken());
        this.tokenB = byteBuffer.readNulsData(new NerveToken());
        this.to = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.deadline = byteBuffer.readUint32();
        this.amountAMin = byteBuffer.readBigInteger();
        this.amountBMin = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 0;
        size += tokenA.size();
        size += tokenB.size();
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    public NerveToken getTokenA() {
        return tokenA;
    }

    public void setTokenA(NerveToken tokenA) {
        this.tokenA = tokenA;
    }

    public NerveToken getTokenB() {
        return tokenB;
    }

    public void setTokenB(NerveToken tokenB) {
        this.tokenB = tokenB;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public BigInteger getAmountAMin() {
        return amountAMin;
    }

    public void setAmountAMin(BigInteger amountAMin) {
        this.amountAMin = amountAMin;
    }

    public BigInteger getAmountBMin() {
        return amountBMin;
    }

    public void setAmountBMin(BigInteger amountBMin) {
        this.amountBMin = amountBMin;
    }
}
