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
 * 币对交易协议
 */
public class CoinTrading extends TxData {
    //交易对创建人
    private byte[] address;
    //交易币种的chainId
    private int baseAssetChainId;
    //交易币种的assetId
    private int baseAssetId;
    //交易币种允许最小交易小数位
    private byte scaleBaseDecimal;
    //计价币种的chainId
    private int quoteAssetChainId;
    //计价币种的assetId
    private int quoteAssetId;
    //计价币种允许最小交易小数位
    private byte scaleQuoteDecimal;
    //最小卖单额
    private BigInteger minBaseAmount;
    //最小买单额
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
