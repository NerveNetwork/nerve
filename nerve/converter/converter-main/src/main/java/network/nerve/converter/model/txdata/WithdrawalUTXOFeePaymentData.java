///**
// * MIT License
// * <p>
// * Copyright (c) 2019-2022 nerve.network
// * <p>
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * <p>
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// * <p>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package network.nerve.converter.model.txdata;
//
//import io.nuls.base.basic.NulsByteBuffer;
//import io.nuls.base.basic.NulsOutputStreamBuffer;
//import io.nuls.base.data.BaseNulsData;
//import io.nuls.base.data.NulsHash;
//import io.nuls.core.crypto.HexUtil;
//import io.nuls.core.exception.NulsException;
//import io.nuls.core.parse.SerializeUtils;
//import network.nerve.converter.btc.txdata.UTXOData;
//import network.nerve.converter.constant.ConverterConstant;
//import network.nerve.converter.model.bo.WithdrawalUTXO;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Confirm contract upgrade transactiontxdata
// */
//public class WithdrawalUTXOFeePaymentData extends BaseNulsData {
//    private long blockHeight;
//    private String blockHash;
//    private String htgTxHash;
//    private int htgChainId;
//    private long fee;
//
//    @Override
//    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
//        stream.writeUint32(blockHeight);
//        stream.writeString(blockHash);
//        stream.writeString(htgTxHash);
//        stream.writeUint16(htgChainId);
//        stream.writeUint32(fee);
//    }
//
//    @Override
//    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
//        this.blockHeight = byteBuffer.readUint32();
//        this.blockHash = byteBuffer.readString();
//        this.htgTxHash = byteBuffer.readString();
//        this.htgChainId = byteBuffer.readUint16();
//        this.fee = byteBuffer.readUint32();
//    }
//
//    @Override
//    public int size() {
//        int size = 0;
//        size += SerializeUtils.sizeOfUint32();
//        size += SerializeUtils.sizeOfString(this.blockHash);
//        size += SerializeUtils.sizeOfString(this.htgTxHash);
//        size += SerializeUtils.sizeOfUint16();
//        size += SerializeUtils.sizeOfUint32();
//        return size;
//    }
//
//    public long getBlockHeight() {
//        return blockHeight;
//    }
//
//    public void setBlockHeight(long blockHeight) {
//        this.blockHeight = blockHeight;
//    }
//
//    public String getBlockHash() {
//        return blockHash;
//    }
//
//    public void setBlockHash(String blockHash) {
//        this.blockHash = blockHash;
//    }
//
//    public long getFee() {
//        return fee;
//    }
//
//    public void setFee(long fee) {
//        this.fee = fee;
//    }
//
//    public int getHtgChainId() {
//        return htgChainId;
//    }
//
//    public void setHtgChainId(int htgChainId) {
//        this.htgChainId = htgChainId;
//    }
//
//    public String getHtgTxHash() {
//        return htgTxHash;
//    }
//
//    public void setHtgTxHash(String htgTxHash) {
//        this.htgTxHash = htgTxHash;
//    }
//
//    @Override
//    public String toString() {
//        final StringBuilder sb = new StringBuilder("{");
//        sb.append("\"blockHeight\":")
//                .append(blockHeight);
//
//        sb.append(",\"blockHash\":")
//                .append('\"').append(blockHash).append('\"');
//        sb.append(",\"htgTxHash\":")
//                .append('\"').append(htgTxHash).append('\"');
//        sb.append(",\"htgChainId\":")
//                .append(htgChainId);
//        sb.append(",\"fee\":")
//                .append(fee);
//        sb.append("}");
//        return sb.toString();
//    }
//}
