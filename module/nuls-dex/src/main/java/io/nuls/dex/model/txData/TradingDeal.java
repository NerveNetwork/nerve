package io.nuls.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

public class TradingDeal extends BaseNulsData {

    //交易对hash
    private byte[] tradingHash;
    //买单交易hash
    private byte[] buyHash;
    //卖单交易hash
    private byte[] sellHash;
    //交易币种成交量
    private BigInteger quoteAmount;
    //计价币种成交量
    private BigInteger baseAmount;
    //成交价
    private BigInteger price;
    //成交状态: 1 买单已完全成交，2 卖单已完全成交，3 买卖双方都完全成交
    private byte type;

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH * 3;
        size += SerializeUtils.sizeOfBigInteger() * 3;
        size += 1;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash);
        stream.write(buyHash);
        stream.write(sellHash);
        stream.writeBigInteger(quoteAmount);
        stream.writeBigInteger(baseAmount);
        stream.writeBigInteger(price);
        stream.writeByte(type);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        buyHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        sellHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        quoteAmount = byteBuffer.readBigInteger();
        baseAmount = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
        type = byteBuffer.readByte();
    }

    public byte[] getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(byte[] tradingHash) {
        this.tradingHash = tradingHash;
    }

    public byte[] getSellHash() {
        return sellHash;
    }

    public void setSellHash(byte[] sellHash) {
        this.sellHash = sellHash;
    }

    public byte[] getBuyHash() {
        return buyHash;
    }

    public void setBuyHash(byte[] buyHash) {
        this.buyHash = buyHash;
    }

    public BigInteger getPrice() {
        return price;
    }

    public void setPrice(BigInteger price) {
        this.price = price;
    }

    public BigInteger getQuoteAmount() {
        return quoteAmount;
    }

    public void setQuoteAmount(BigInteger quoteAmount) {
        this.quoteAmount = quoteAmount;
    }

    public BigInteger getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigInteger baseAmount) {
        this.baseAmount = baseAmount;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }
}
