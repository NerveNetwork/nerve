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
    //Transaction pairshash
    private byte[] tradingHash;
    //Buy order transactionhash
    private byte[] buyHash;

    private byte[] buyNonce;
    //Sell order transactionhash
    private byte[] sellHash;

    private byte[] sellNonce;
    //Trading Currency Trading Volume
    private BigInteger quoteAmount;
    //Pricing currency transaction volume
    private BigInteger baseAmount;
    //Buyer pays handling fees
    private BigInteger buyFee;
    //Seller pays handling fees
    private BigInteger sellFee;
    //Transaction price
    private BigInteger price;
    //Transaction status: 1 The purchase order has been fully completed,2 The sales order has been fully completed,3 Both buyers and sellers have a complete transaction
    private byte type;
    //Active trading party：1Proactively closing the purchase order, 2Active closing of sales orders
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
