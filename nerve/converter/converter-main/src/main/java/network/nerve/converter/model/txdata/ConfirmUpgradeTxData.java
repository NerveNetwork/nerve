/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 确认合约升级交易txdata
 */
public class ConfirmUpgradeTxData extends BaseNulsData {

    private int heterogeneousChainId;
    /**
     * 异构链中对应的升级交易hash
     */
    private String heterogeneousTxHash;
    /**
     * 升级前异构链多签地址
     */
    private byte[] oldAddress;
    /**
     * 升级后的异构链多签地址
     */
    private byte[] address;
    /**
     * Nerve网络中对应的提案交易hash
     */
    private NulsHash nerveTxHash;
    /**
     * 需要分发提现手续费的节点异构链地址
     */
    private List<HeterogeneousAddress> listDistributionFee;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", heterogeneousTxHash)).append(lineSeparator);
        builder.append(String.format("\toldAddress: %s", null == oldAddress ? "" : HexUtil.encode(oldAddress))).append(lineSeparator);
        builder.append(String.format("\taddress: %s", null == address ? "" : HexUtil.encode(address))).append(lineSeparator);
        builder.append(String.format("\tnerveTxHash: %s", null == nerveTxHash ? "" : nerveTxHash.toHex())).append(lineSeparator);

        if (listDistributionFee == null) {
            builder.append("\tlistDistributionFee: null").append(lineSeparator);
        } else if (listDistributionFee.size() == 0) {
            builder.append("\tlistDistributionFee: size 0").append(lineSeparator);
        } else {
            builder.append("\tlistDistributionFee:").append(lineSeparator);
            for (int i = 0; i < listDistributionFee.size(); i++) {
                HeterogeneousAddress addr = listDistributionFee.get(i);
                builder.append(String.format("\t\theterogeneousAddress chainId:%s - address: %s", addr.getChainId(), addr.getAddress())).append(lineSeparator);
                builder.append(lineSeparator);
            }
        }

        return builder.toString();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(heterogeneousChainId);
        stream.writeString(heterogeneousTxHash);
        stream.writeBytesWithLength(oldAddress);
        stream.writeBytesWithLength(address);
        stream.write(nerveTxHash.getBytes());
        int listSize = listDistributionFee == null ? 0 : listDistributionFee.size();
        stream.writeUint16(listSize);
        if(null != listDistributionFee){
            for(HeterogeneousAddress address : listDistributionFee){
                stream.writeNulsData(address);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousChainId = byteBuffer.readUint16();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.oldAddress = byteBuffer.readByLengthByte();
        this.address = byteBuffer.readByLengthByte();
        this.nerveTxHash = byteBuffer.readHash();
        int listSize = byteBuffer.readUint16();
        if(0 < listSize){
            List<HeterogeneousAddress> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(byteBuffer.readNulsData(new HeterogeneousAddress()));
            }
            this.listDistributionFee = list;
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += SerializeUtils.sizeOfBytes(oldAddress);
        size += SerializeUtils.sizeOfBytes(address);
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint16();
        if (null != listDistributionFee) {
            for(HeterogeneousAddress address : listDistributionFee){
                size += SerializeUtils.sizeOfNulsData(address);
            }
        }
        return size;
    }

    public int getHeterogeneousChainId() {
        return heterogeneousChainId;
    }

    public void setHeterogeneousChainId(int heterogeneousChainId) {
        this.heterogeneousChainId = heterogeneousChainId;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public byte[] getOldAddress() {
        return oldAddress;
    }

    public void setOldAddress(byte[] oldAddress) {
        this.oldAddress = oldAddress;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public NulsHash getNerveTxHash() {
        return nerveTxHash;
    }

    public void setNerveTxHash(NulsHash nerveTxHash) {
        this.nerveTxHash = nerveTxHash;
    }

    public List<HeterogeneousAddress> getListDistributionFee() {
        return listDistributionFee;
    }

    public void setListDistributionFee(List<HeterogeneousAddress> listDistributionFee) {
        this.listDistributionFee = listDistributionFee;
    }
}
