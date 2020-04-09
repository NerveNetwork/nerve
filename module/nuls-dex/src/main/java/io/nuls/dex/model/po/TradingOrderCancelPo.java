package io.nuls.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

public class TradingOrderCancelPo extends BaseNulsData {

    private transient NulsHash hash;

    private NulsHash orderHash;

    private BigInteger cancelAmount;

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(orderHash.getBytes());
        stream.writeBigInteger(cancelAmount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.orderHash = byteBuffer.readHash();
        this.cancelAmount = byteBuffer.readBigInteger();
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public NulsHash getOrderHash() {
        return orderHash;
    }

    public void setOrderHash(NulsHash orderHash) {
        this.orderHash = orderHash;
    }

    public BigInteger getCancelAmount() {
        return cancelAmount;
    }

    public void setCancelAmount(BigInteger cancelAmount) {
        this.cancelAmount = cancelAmount;
    }
}
