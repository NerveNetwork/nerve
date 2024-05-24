/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter.btc.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import network.nerve.converter.utils.ConverterUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Confirm contract upgrade transactiontxdata
 */
public class WithdrawalUTXOTxData extends BaseNulsData {

    private String nerveTxHash;
    private int htgChainId;
    private String currentMultiSignAddress;
    private int currentVirtualBankTotal;
    private long feeRate;
    private List<byte[]> pubs;
    private List<UTXOData> utxoDataList;

    public WithdrawalUTXOTxData() {
    }

    public WithdrawalUTXOTxData(String nerveTxHash, int htgChainId, String currentMultiSignAddress, int currentVirtualBankTotal, long feeRate, List<byte[]> pubs, List<UTXOData> utxoDataList) {
        this.nerveTxHash = nerveTxHash;
        this.htgChainId = htgChainId;
        this.currentMultiSignAddress = currentMultiSignAddress;
        this.currentVirtualBankTotal = currentVirtualBankTotal;
        this.feeRate = feeRate;
        this.pubs = pubs;
        this.utxoDataList = utxoDataList;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(HexUtil.decode(nerveTxHash));
        stream.writeUint16(htgChainId);
        stream.writeString(currentMultiSignAddress);
        stream.writeUint16(currentVirtualBankTotal);
        stream.writeUint32(feeRate);
        int pubSize = pubs == null ? 0 : pubs.size();
        stream.writeUint16(pubSize);
        if(null != pubs){
            for(byte[] pub : pubs){
                stream.write(pub);
            }
        }

        int listSize = utxoDataList == null ? 0 : utxoDataList.size();
        stream.writeUint16(listSize);
        if(null != utxoDataList){
            for(UTXOData utxoData : utxoDataList){
                stream.writeNulsData(utxoData);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.nerveTxHash = HexUtil.encode(byteBuffer.readBytes(NulsHash.HASH_LENGTH));
        this.htgChainId = byteBuffer.readUint16();
        this.currentMultiSignAddress = byteBuffer.readString();
        this.currentVirtualBankTotal = byteBuffer.readUint16();
        this.feeRate = byteBuffer.readUint32();
        int pubSize = byteBuffer.readUint16();
        if(0 < pubSize){
            List<byte[]> pubList = new ArrayList<>();
            for(int i = 0; i< pubSize; i++){
                pubList.add(byteBuffer.readBytes(ConverterUtil.PUB_LENGTH));
            }
            this.pubs = pubList;
        }

        int listSize = byteBuffer.readUint16();
        if(0 < listSize){
            List<UTXOData> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(byteBuffer.readNulsData(new UTXOData()));
            }
            this.utxoDataList = list;
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.currentMultiSignAddress);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint32();

        size += SerializeUtils.sizeOfUint16();
        if (pubs != null) {
            size += ConverterUtil.PUB_LENGTH * pubs.size();
        }

        size += SerializeUtils.sizeOfUint16();
        if (null != utxoDataList) {
            for(UTXOData utxoData : utxoDataList){
                size += SerializeUtils.sizeOfNulsData(utxoData);
            }
        }
        return size;
    }

    public int getHtgChainId() {
        return htgChainId;
    }

    public void setHtgChainId(int htgChainId) {
        this.htgChainId = htgChainId;
    }

    public String getCurrentMultiSignAddress() {
        return currentMultiSignAddress;
    }

    public void setCurrentMultiSignAddress(String currentMultiSignAddress) {
        this.currentMultiSignAddress = currentMultiSignAddress;
    }

    public long getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(long feeRate) {
        this.feeRate = feeRate;
    }

    public int getCurrentVirtualBankTotal() {
        return currentVirtualBankTotal;
    }

    public void setCurrentVirtualBankTotal(int currentVirtualBankTotal) {
        this.currentVirtualBankTotal = currentVirtualBankTotal;
    }

    public String getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(String nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public List<byte[]> getPubs() {
        return pubs;
    }

    public void setPubs(List<byte[]> pubs) {
        this.pubs = pubs;
    }

    public List<UTXOData> getUtxoDataList() {
        return utxoDataList;
    }

    public void setUtxoDataList(List<UTXOData> utxoDataList) {
        this.utxoDataList = utxoDataList;
    }

    public WithdrawalUTXO toWithdrawalUTXO() {
        return new WithdrawalUTXO(
                this.nerveTxHash,
                this.htgChainId,
                this.currentMultiSignAddress,
                this.currentVirtualBankTotal,
                this.feeRate,
                this.pubs,
                this.utxoDataList
        );
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"nerveTxHash\":")
                .append('\"').append(nerveTxHash).append('\"');
        sb.append(",\"htgChainId\":")
                .append(htgChainId);
        sb.append(",\"currentMultiSignAddress\":")
                .append('\"').append(currentMultiSignAddress).append('\"');
        sb.append(",\"currenVirtualBankTotal\":")
                .append(currentVirtualBankTotal);
        sb.append(",\"feeRate\":")
                .append(feeRate);
        sb.append(",\"pubs\":[");
        if (pubs != null) {
            for (byte[] pub : pubs) {
                sb.append('\"').append(HexUtil.encode(pub)).append('\"').append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        sb.append(",\"utxoDataList\":[");
        if (utxoDataList != null) {
            for (UTXOData utxo : utxoDataList) {
                sb.append(utxo.toString()).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]}");
        return sb.toString();
    }
}
