package network.nerve.dex.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.dex.model.txData.TradingOrder;
import network.nerve.dex.util.DexUtil;

import java.io.IOException;
import java.math.BigInteger;

public class TradingOrderPo extends BaseNulsData implements Comparable {

    private transient NulsHash orderHash;

    private NulsHash tradingHash;

    private byte[] address;

    private byte type;

    private long blockHeight;

    private int index;

    private BigInteger amount;

    private BigInteger dealAmount;

    private BigInteger price;

    private byte[] feeAddress;

    private byte feeScale;

    private BigInteger leftQuoteAmount;
    //记录当前订单更新时的nonce值
    private byte[] nonce;

    //冗余字段，记录剩余未成交的数量，不做底层存储
    private BigInteger leftAmount;
    //是否已经完全成交
    private boolean over;

    public TradingOrderPo() {

    }

    public TradingOrderPo copy() {
        TradingOrderPo po = new TradingOrderPo();
        po.orderHash = new NulsHash(this.orderHash.getBytes());
        po.tradingHash = new NulsHash(this.tradingHash.getBytes());
        po.address = this.address;
        po.type = this.type;
        po.blockHeight = this.blockHeight;
        po.index = this.index;
        po.amount = new BigInteger(this.amount.toString());
        po.dealAmount = new BigInteger(this.dealAmount.toString());
        po.leftQuoteAmount = new BigInteger(this.leftQuoteAmount.toString());
        po.price = new BigInteger(this.price.toString());
        po.nonce = this.nonce;
        po.feeScale = this.feeScale;
        po.feeAddress = this.feeAddress;
        return po;
    }

    public void copyFrom(TradingOrderPo po) {
        this.dealAmount = po.dealAmount;
        this.leftQuoteAmount = po.leftQuoteAmount;
        this.nonce = po.nonce;
    }

    public TradingOrderPo(Transaction tx, int index, byte[] address, TradingOrder order) {
        this.orderHash = tx.getHash();
        this.tradingHash = new NulsHash(order.getTradingHash());
        this.address = address;
        this.type = order.getType();
        this.blockHeight = tx.getBlockHeight();
        this.index = index;
        this.amount = order.getAmount();
        this.dealAmount = BigInteger.ZERO;
        this.leftQuoteAmount = BigInteger.ZERO;
        this.price = order.getPrice();
        this.nonce = DexUtil.getNonceByHash(orderHash);
        this.feeAddress = order.getFeeAddress();
        this.feeScale = order.getFeeScale();
    }

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += Address.ADDRESS_LENGTH;
        size += 1;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfBigInteger() * 4;
        size += SerializeUtils.sizeOfBytes(nonce);
        size += SerializeUtils.sizeOfBytes(feeAddress);
        size += 1;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash.getBytes());
        stream.write(address);
        stream.write(type);
        stream.writeUint32(blockHeight);
        stream.writeUint16(index);
        stream.writeBigInteger(amount);
        stream.writeBigInteger(dealAmount);
        stream.writeBigInteger(leftQuoteAmount);
        stream.writeBigInteger(price);
        stream.writeBytesWithLength(nonce);
        stream.writeBytesWithLength(feeAddress);
        stream.write(feeScale);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readHash();
        address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        type = byteBuffer.readByte();
        blockHeight = byteBuffer.readUint32();
        index = byteBuffer.readUint16();
        amount = byteBuffer.readBigInteger();
        dealAmount = byteBuffer.readBigInteger();
        leftQuoteAmount = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
        nonce = byteBuffer.readByLengthByte();
        feeAddress = byteBuffer.readByLengthByte();
        feeScale = byteBuffer.readByte();
    }

    public NulsHash getOrderHash() {
        return orderHash;
    }

    public void setOrderHash(NulsHash orderHash) {
        this.orderHash = orderHash;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
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

    public BigInteger getDealAmount() {
        return dealAmount;
    }

    public void setDealAmount(BigInteger dealAmount) {
        this.dealAmount = dealAmount;
    }

    public BigInteger getPrice() {
        return price;
    }

    public void setPrice(BigInteger price) {
        this.price = price;
    }

    public NulsHash getTradingHash() {
        return tradingHash;
    }

    public void setTradingHash(NulsHash tradingHash) {
        this.tradingHash = tradingHash;
    }

    @Override
    public int compareTo(Object o) {
        TradingOrderPo po = (TradingOrderPo) o;
        if (this.blockHeight < po.getBlockHeight()) {
            return -1;
        } else if (this.blockHeight > po.getBlockHeight()) {
            return 1;
        } else {
            if (this.getIndex() < po.getIndex()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public BigInteger getLeftAmount() {
        leftAmount = amount.subtract(dealAmount);
        return leftAmount;
    }

    public void setLeftAmount(BigInteger leftAmount) {
        this.leftAmount = leftAmount;
    }

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    @Override
    public String toString() {
//        return "tradingHash:" + tradingHash.toHex().substring(0, 6) + ", orderHash:" + orderHash.toHex() + ", type: " + type + ", amount:" + amount.toString() + ", dealAmount:" + dealAmount.toString() + ", nonce:" + HexUtil.encode(nonce == null ? "".getBytes() : nonce) + ", price:" + price.toString();
        return "orderHash:" + orderHash.toHex() + ", type: " + type + ", price:" + price.toString() + ", height:" + blockHeight + ",index:" + index + ", amount:" + amount.toString() + ", dealAmount:" + dealAmount.toString() + ", leftQuoteAmount:" + leftQuoteAmount;
    }

    public BigInteger getLeftQuoteAmount() {
        return leftQuoteAmount;
    }

    public void setLeftQuoteAmount(BigInteger leftQuoteAmount) {
        this.leftQuoteAmount = leftQuoteAmount;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getFeeAddress() {
        return feeAddress;
    }

    public void setFeeAddress(byte[] feeAddress) {
        this.feeAddress = feeAddress;
    }

    public byte getFeeScale() {
        return feeScale;
    }

    public void setFeeScale(byte feeScale) {
        this.feeScale = feeScale;
    }
}
