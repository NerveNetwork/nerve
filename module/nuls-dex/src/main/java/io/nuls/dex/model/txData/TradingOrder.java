package io.nuls.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 币对交易委托挂单协议
 */
public class TradingOrder extends BaseNulsData {
    //币对hash
    private byte[] tradingHash;
    //1买入 2卖出
    private byte type;
    //委托交易币种数量
    private BigInteger amount;
    //委托价格
    private BigInteger price;

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += 1;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash);
        stream.write(type);
        stream.writeBigInteger(amount);
        stream.writeBigInteger(price);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        type = byteBuffer.readByte();
        amount = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
    }

    public byte[] getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(byte[] tradingHash) {
        this.tradingHash = tradingHash;
    }


    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public BigInteger getPrice() {
        return price;
    }

    public void setPrice(BigInteger price) {
        this.price = price;
    }
}
