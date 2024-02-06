package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.TxData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Modify Currency Transaction Agreement
 */
public class EditCoinTradingPo extends TxData {

    private NulsHash txHash;

    //Transaction currency allows for minimum transaction decimal places
    private byte scaleBaseDecimal;

    //Pricing currency allows for minimum transaction decimal places
    private byte scaleQuoteDecimal;
    //Minimum trading volume
    private BigInteger minBaseAmount;

    private BigInteger minQuoteAmount;

    @Override
    public int size() {
        int size = 0;
        size += txHash.getBytes().length;
        size += 2;
        size += SerializeUtils.sizeOfBigInteger() * 2;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(txHash.getBytes());
        stream.write(scaleBaseDecimal);
        stream.write(scaleQuoteDecimal);
        stream.writeBigInteger(minBaseAmount);
        stream.writeBigInteger(minQuoteAmount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        txHash = byteBuffer.readHash();
        scaleBaseDecimal = byteBuffer.readByte();
        scaleQuoteDecimal = byteBuffer.readByte();
        minBaseAmount = byteBuffer.readBigInteger();
        minQuoteAmount = byteBuffer.readBigInteger();
    }

    public NulsHash getTxHash() {
        return txHash;
    }

    public void setTxHash(NulsHash txHash) {
        this.txHash = txHash;
    }



    public byte getScaleBaseDecimal() {
        return scaleBaseDecimal;
    }

    public void setScaleBaseDecimal(byte scaleBaseDecimal) {
        this.scaleBaseDecimal = scaleBaseDecimal;
    }

    public byte getScaleQuoteDecimal() {
        return scaleQuoteDecimal;
    }

    public void setScaleQuoteDecimal(byte scaleQuoteDecimal) {
        this.scaleQuoteDecimal = scaleQuoteDecimal;
    }

    public BigInteger getMinBaseAmount() {
        return minBaseAmount;
    }

    public void setMinBaseAmount(BigInteger minBaseAmount) {
        this.minBaseAmount = minBaseAmount;
    }

    public BigInteger getMinQuoteAmount() {
        return minQuoteAmount;
    }

    public void setMinQuoteAmount(BigInteger minQuoteAmount) {
        this.minQuoteAmount = minQuoteAmount;
    }
}
