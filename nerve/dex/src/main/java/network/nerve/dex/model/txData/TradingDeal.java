package network.nerve.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.dex.context.DexConstant;

import java.io.IOException;
import java.math.BigInteger;

public class TradingDeal extends BaseNulsData {
    //交易对hash
    private byte[] tradingHash;
    //买单交易hash
    private byte[] buyHash;

    private byte[] buyNonce;
    //卖单交易hash
    private byte[] sellHash;

    private byte[] sellNonce;
    //交易币种成交量
    private BigInteger quoteAmount;
    //计价币种成交量
    private BigInteger baseAmount;
    //买家支付手续费
    private BigInteger buyFee;
    //卖家支付手续费
    private BigInteger sellFee;
    //成交价
    private BigInteger price;
    //成交状态: 1 买单已完全成交，2 卖单已完全成交，3 买卖双方都完全成交
    private byte type;
    //主动成交方：1买单主动成交， 2卖单主动成交
    private byte taker;

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH * 3;
        size += DexConstant.NONCE_LENGTH * 2;
        size += SerializeUtils.sizeOfBigInteger() * 5;
        size += 2;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash);
        stream.write(buyHash);
        stream.write(buyNonce);
        stream.write(sellHash);
        stream.write(sellNonce);
        stream.writeBigInteger(quoteAmount);
        stream.writeBigInteger(baseAmount);
        stream.writeBigInteger(buyFee);
        stream.writeBigInteger(sellFee);
        stream.writeBigInteger(price);
        stream.writeByte(type);
        stream.writeByte(taker);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        buyHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        buyNonce = byteBuffer.readBytes(DexConstant.NONCE_LENGTH);
        sellHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        sellNonce = byteBuffer.readBytes(DexConstant.NONCE_LENGTH);
        quoteAmount = byteBuffer.readBigInteger();
        baseAmount = byteBuffer.readBigInteger();
        buyFee = byteBuffer.readBigInteger();
        sellFee = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
        type = byteBuffer.readByte();
        taker = byteBuffer.readByte();
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

    public byte getTaker() {
        return taker;
    }

    public void setTaker(byte taker) {
        this.taker = taker;
    }

    public byte[] getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(byte[] tradingHash) {
        this.tradingHash = tradingHash;
    }

    public byte[] getBuyNonce() {
        return buyNonce;
    }

    public void setBuyNonce(byte[] buyNonce) {
        this.buyNonce = buyNonce;
    }

    public byte[] getSellNonce() {
        return sellNonce;
    }

    public void setSellNonce(byte[] sellNonce) {
        this.sellNonce = sellNonce;
    }

    public BigInteger getBuyFee() {
        return buyFee;
    }

    public void setBuyFee(BigInteger buyFee) {
        this.buyFee = buyFee;
    }

    public BigInteger getSellFee() {
        return sellFee;
    }

    public void setSellFee(BigInteger sellFee) {
        this.sellFee = sellFee;
    }
}
