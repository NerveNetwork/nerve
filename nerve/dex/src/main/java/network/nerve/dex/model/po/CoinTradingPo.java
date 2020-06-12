package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.dex.model.txData.CoinTrading;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 交易对实体对象
 */
public class CoinTradingPo extends BaseNulsData {
    //交易地址创建人
    private byte[] address;
    //交易hash
    private transient NulsHash hash;
    //交易币种的chainId
    private int baseAssetChainId;
    //交易币种的assetId
    private int baseAssetId;
    //交易币种默认小数位数
    private byte baseDecimal;
    //交易币种允许最小交易小数位
    private byte scaleBaseDecimal;

    //计价币种的chainId
    private int quoteAssetChainId;
    //计价币种的assetId
    private int quoteAssetId;
    //计价币种小位数
    private byte quoteDecimal;
    //计价币种允许最小交易小数位
    private byte scaleQuoteDecimal;
    //最小交易量
    private BigInteger minBaseAmount;

    private BigInteger minQuoteAmount;

    public CoinTradingPo() {

    }

    public CoinTradingPo copy() {
        CoinTradingPo po = new CoinTradingPo();
        po.address = this.address;
        po.hash = new NulsHash(this.hash.getBytes());
        po.baseAssetChainId = this.baseAssetChainId;
        po.baseAssetId = this.baseAssetId;
        po.baseDecimal = this.baseDecimal;
        po.scaleBaseDecimal = this.scaleBaseDecimal;

        po.quoteAssetChainId = this.quoteAssetChainId;
        po.quoteAssetId = this.quoteAssetId;
        po.quoteDecimal = this.quoteDecimal;
        po.scaleQuoteDecimal = this.scaleQuoteDecimal;

        po.minBaseAmount = new BigInteger(this.minBaseAmount.toString());
        po.minQuoteAmount = new BigInteger(this.minQuoteAmount.toString());
        return po;
    }

    public CoinTradingPo(NulsHash hash, CoinTrading trading) {
        this.hash = hash;
        this.address = trading.getAddress();
        this.baseAssetChainId = trading.getBaseAssetChainId();
        this.baseAssetId = trading.getBaseAssetId();
        this.scaleBaseDecimal = trading.getScaleBaseDecimal();

        this.quoteAssetChainId = trading.getQuoteAssetChainId();
        this.quoteAssetId = trading.getQuoteAssetId();
        this.scaleQuoteDecimal = trading.getScaleQuoteDecimal();

        this.minBaseAmount = trading.getMinBaseAmount();
        this.minQuoteAmount = trading.getMinQuoteAmount();
    }

    @Override
    public int size() {
        int size = 0;
        size += Address.ADDRESS_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint16();
        size += 2;

        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint16();
        size += 2;
        size += SerializeUtils.sizeOfBigInteger() * 2;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(address);
        stream.writeUint16(baseAssetChainId);
        stream.writeUint16(baseAssetId);
        stream.write(baseDecimal);
        stream.write(scaleBaseDecimal);

        stream.writeUint16(quoteAssetChainId);
        stream.writeUint16(quoteAssetId);
        stream.write(quoteDecimal);
        stream.write(scaleQuoteDecimal);

        stream.writeBigInteger(minBaseAmount);
        stream.writeBigInteger(minQuoteAmount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        this.baseAssetChainId = byteBuffer.readUint16();
        this.baseAssetId = byteBuffer.readUint16();
        this.baseDecimal = byteBuffer.readByte();
        this.scaleBaseDecimal = byteBuffer.readByte();

        this.quoteAssetChainId = byteBuffer.readUint16();
        this.quoteAssetId = byteBuffer.readUint16();
        this.quoteDecimal = byteBuffer.readByte();
        this.scaleQuoteDecimal = byteBuffer.readByte();

        this.minBaseAmount = byteBuffer.readBigInteger();
        this.minQuoteAmount = byteBuffer.readBigInteger();
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
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

    public byte getBaseDecimal() {
        return baseDecimal;
    }

    public void setBaseDecimal(byte baseDecimal) {
        this.baseDecimal = baseDecimal;
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

    public byte getQuoteDecimal() {
        return quoteDecimal;
    }

    public void setQuoteDecimal(byte quoteDecimal) {
        this.quoteDecimal = quoteDecimal;
    }


    @Override
    public String toString() {
        return "coinHash:" + hash.toHex() + ", baseChainId:" + baseAssetChainId + ", baseAssetId:" + baseAssetId + ", baseDecimal:" + baseDecimal + ",scaleBaseDecimal:" + scaleBaseDecimal +
                ",quoteChainId: " + quoteAssetChainId + ", quoteAssetId: " + quoteAssetId + ", quoteDecimal:" + quoteDecimal + ", scaleQuoteDecimal" + scaleQuoteDecimal + ",minBaseAmount:" + minBaseAmount + ",minQuoteAmount:" + minQuoteAmount;
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
