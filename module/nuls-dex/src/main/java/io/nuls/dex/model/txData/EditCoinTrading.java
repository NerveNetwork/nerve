package io.nuls.dex.model.txData;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.TxData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 修改币种交易协议
 */
public class EditCoinTrading extends TxData {

    private NulsHash tradingHash;

    //交易币种允许最小交易小数位
    private byte scaleBaseDecimal;

    //计价币种允许最小交易小数位
    private byte scaleQuoteDecimal;
    //最小交易量
    private BigInteger minTradingAmount;


    @Override
    public int size() {
        int size = 0;
        size += tradingHash.getBytes().length;
        size += 2;
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash.getBytes());
        stream.write(scaleBaseDecimal);
        stream.write(scaleQuoteDecimal);
        stream.writeBigInteger(minTradingAmount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readHash();
        scaleBaseDecimal = byteBuffer.readByte();
        scaleQuoteDecimal = byteBuffer.readByte();
        minTradingAmount = byteBuffer.readBigInteger();
    }


    public NulsHash getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(NulsHash tradingHash) {
        this.tradingHash = tradingHash;
    }

    public BigInteger getMinTradingAmount() {
        return minTradingAmount;
    }

    public void setMinTradingAmount(BigInteger minTradingAmount) {
        this.minTradingAmount = minTradingAmount;
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
}
