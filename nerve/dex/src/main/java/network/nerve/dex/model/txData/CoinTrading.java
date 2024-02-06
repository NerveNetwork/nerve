package network.nerve.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.TxData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/***
 * Coin to coin transaction agreement
 */
public class CoinTrading extends TxData {
    //Transaction pair creator
    private byte[] address;
    //Transaction currencychainId
    private int baseAssetChainId;
    //Transaction currencyassetId
    private int baseAssetId;
    //Transaction currency allows for minimum transaction decimal places
    private byte scaleBaseDecimal;
    //Pricing currencychainId
    private int quoteAssetChainId;
    //Pricing currencyassetId
    private int quoteAssetId;
    //Pricing currency allows for minimum transaction decimal places
    private byte scaleQuoteDecimal;
    //Minimum order amount
    private BigInteger minBaseAmount;
    //Minimum purchase amount
    private BigInteger minQuoteAmount;

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint16();
        size += 1;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfInt16();
        size += 1;

        size += SerializeUtils.sizeOfBigInteger() * 2;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(address);
        stream.writeUint16(baseAssetChainId);
        stream.writeUint16(baseAssetId);
        stream.write(scaleBaseDecimal);

        stream.writeUint16(quoteAssetChainId);
        stream.writeUint16(quoteAssetId);
        stream.write(scaleQuoteDecimal);

        stream.writeBigInteger(minBaseAmount);
        stream.writeBigInteger(minQuoteAmount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.baseAssetChainId = byteBuffer.readUint16();
        this.baseAssetId = byteBuffer.readUint16();
        this.scaleBaseDecimal = byteBuffer.readByte();

        this.quoteAssetChainId = byteBuffer.readUint16();
        this.quoteAssetId = byteBuffer.readUint16();
        this.scaleQuoteDecimal = byteBuffer.readByte();

        this.minBaseAmount = byteBuffer.readBigInteger();
        this.minQuoteAmount = byteBuffer.readBigInteger();
    }

    public int getBaseAssetChainId() {
        return baseAssetChainId;
    }

    public void setBaseAssetChainId(int baseAssetChainId) {
        this.baseAssetChainId = baseAssetChainId;
    }

    public int getBaseAssetId() {
        return baseAssetId;
    }

    public void setBaseAssetId(int baseAssetId) {
        this.baseAssetId = baseAssetId;
    }

    public int getQuoteAssetChainId() {
        return quoteAssetChainId;
    }

    public void setQuoteAssetChainId(int quoteAssetChainId) {
        this.quoteAssetChainId = quoteAssetChainId;
    }

    public int getQuoteAssetId() {
        return quoteAssetId;
    }

    public void setQuoteAssetId(int quoteAssetId) {
        this.quoteAssetId = quoteAssetId;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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
