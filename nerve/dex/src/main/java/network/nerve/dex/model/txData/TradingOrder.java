package network.nerve.dex.model.txData;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Coin to coin trading commission order agreement
 */
public class TradingOrder extends BaseNulsData {
    //Coin pairshash
    private byte[] tradingHash;
    //Principal
    private byte[] address;
    //1Buy 2Selling
    private byte type;
    //Number of entrusted transaction currencies
    private BigInteger amount;
    //Commission price
    private BigInteger price;
    //Participating dividend addresses
    private byte[] feeAddress;
    //Participating dividend ratio, value0-5
    private byte feeScale;

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += Address.ADDRESS_LENGTH;
        size += 1;
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBigInteger();
        size += SerializeUtils.sizeOfBytes(feeAddress);
        size += 1;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(tradingHash);
        stream.write(address);
        stream.write(type);
        stream.writeBigInteger(amount);
        stream.writeBigInteger(price);
        stream.writeBytesWithLength(feeAddress);
        stream.write(feeScale);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        tradingHash = byteBuffer.readBytes(NulsHash.HASH_LENGTH);
        address = byteBuffer.readBytes(Address.ADDRESS_LENGTH);
        type = byteBuffer.readByte();
        amount = byteBuffer.readBigInteger();
        price = byteBuffer.readBigInteger();
        feeAddress = byteBuffer.readByLengthByte();
        feeScale = byteBuffer.readByte();
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

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\ttradingHash: %s", HexUtil.encode(tradingHash))).append(lineSeparator);
        builder.append(String.format("\taddress: %s", AddressTool.getStringAddressByBytes(address))).append(lineSeparator);
        builder.append(String.format("\ttype: %s", type)).append(lineSeparator);
        builder.append(String.format("\tamount: %s", amount)).append(lineSeparator);
        builder.append(String.format("\tprice: %s", price)).append(lineSeparator);
        String addr = null;
        if(null != feeAddress && feeAddress.length > 0){
            addr = AddressTool.getStringAddressByBytes(feeAddress);
        }
        builder.append(String.format("\tfeeAddress: %s", addr)).append(lineSeparator);
        builder.append(String.format("\tfeeScale: %s", feeScale)).append(lineSeparator);
        return builder.toString();
    }

}
