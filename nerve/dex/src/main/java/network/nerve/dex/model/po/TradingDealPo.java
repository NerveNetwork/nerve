package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.dex.model.txData.TradingDeal;

import java.io.IOException;
import java.math.BigInteger;

public class TradingDealPo extends BaseNulsData {

    private transient NulsHash dealHash;
    //Transaction pairshash
    private NulsHash tradingHash;
    //Buy order transactionhash
    private NulsHash buyHash;

    private byte[] buyNonce;
    //Sell order transactionhash
    private NulsHash sellHash;

    private byte[] sellNonce;
    //Pricing currency transaction volume
    private BigInteger quoteAmount;
    //Trading Currency Trading Volume
    private BigInteger baseAmount;
    //Transaction price
    private BigInteger price;

    private byte type;


    public TradingDealPo() {

    }

    public TradingDealPo(NulsHash dealHash, TradingDeal deal) {
        this.dealHash = dealHash;
        this.tradingHash = new NulsHash(deal.getTradingHash());
        this.buyHash = new NulsHash(deal.getBuyHash());
        this.sellHash = new NulsHash(deal.getSellHash());
        this.quoteAmount = deal.getQuoteAmount();
        this.baseAmount = deal.getBaseAmount();
        this.buyNonce = deal.getBuyNonce();
        this.sellNonce = deal.getSellNonce();
        this.price = deal.getPrice();
        this.type = deal.getType();
    }

    @Override
    public int size() {
        int size = NulsHash.HASH_LENGTH * 3;
        size += SerializeUtils.sizeOfBigInteger() * 3;
        size += 1;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash.getBytes());
        stream.write(buyHash.getBytes());
        stream.write(sellHash.getBytes());
        stream.writeBigInteger(quoteAmount);
        stream.writeBigInteger(baseAmount);
        stream.writeBigInteger(price);
        stream.writeByte(type);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readHash();
        buyHash = byteBuffer.readHash();
        sellHash = byteBuffer.readHash();
        quoteAmount = byteBuffer.readBigInteger();
        baseAmount = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
        type = byteBuffer.readByte();
    }

    public NulsHash getDealHash() {
        return dealHash;
    }

    public void setDealHash(NulsHash dealHash) {
        this.dealHash = dealHash;
    }

    public NulsHash getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(NulsHash tradingHash) {
        this.tradingHash = tradingHash;
    }

    public NulsHash getBuyHash() {
        return buyHash;
    }

    public void setBuyHash(NulsHash buyHash) {
        this.buyHash = buyHash;
    }

    public NulsHash getSellHash() {
        return sellHash;
    }

    public void setSellHash(NulsHash sellHash) {
        this.sellHash = sellHash;
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
}
