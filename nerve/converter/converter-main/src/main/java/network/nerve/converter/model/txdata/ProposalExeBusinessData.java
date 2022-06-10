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

package network.nerve.converter.model.txdata;

import io.nuls.base.basic.AddressTool;
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
 * 提案确认交易的业务数据
 * 确认交易中的txData 的业务数据
 *
 * @author: Loki
 * @date: 2020/5/20
 */
public class ProposalExeBusinessData extends BaseNulsData {

    private int heterogeneousChainId;

    private String heterogeneousTxHash;

    /**
     * 提案交易hash
     */
    private NulsHash proposalTxHash;

    /**
     * NERVE地址（账户、节点地址等）
     */
    private byte[] address;

    private byte[] hash;

    /**
     * 需要分发提现手续费的节点异构链地址
     */
    private List<HeterogeneousAddress> listDistributionFee;

    /**
     * 执行提案时产生的交易hash(如果有)
     */
    private NulsHash proposalExeHash;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        builder.append(String.format("\theterogeneousChainId: %s", heterogeneousChainId)).append(lineSeparator);
        builder.append(String.format("\theterogeneousTxHash: %s", heterogeneousTxHash)).append(lineSeparator);
        builder.append(String.format("\tproposalTxHash: %s", proposalTxHash)).append(lineSeparator);
        builder.append(String.format("\taddress: %s", AddressTool.getStringAddressByBytes(address))).append(lineSeparator);
        builder.append(String.format("\tnerveHash: %s", null == hash ? null : HexUtil.encode(hash))).append(lineSeparator);
        if (listDistributionFee == null) {
            builder.append("\tlistDistributionFee: null").append(lineSeparator);
        } else if (listDistributionFee.size() == 0) {
            builder.append("\tlistDistributionFee: size 0").append(lineSeparator);
        } else {
            builder.append("\tlistDistributionFee:").append(lineSeparator);
            for(int i = 0; i < listDistributionFee.size(); i++){
                HeterogeneousAddress address = listDistributionFee.get(i);
                builder.append(String.format("\t\tbanks-%s:", i)).append(lineSeparator);
                builder.append(String.format("\t\t\theterogeneousChainId: %s", address.getChainId())).append(lineSeparator);
                builder.append(String.format("\t\t\theterogeneousAddress: %s", address.getAddress())).append(lineSeparator);
            }
        }
        builder.append(String.format("\tproposalExeHash: %s", proposalExeHash)).append(lineSeparator);
        return builder.toString();
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(heterogeneousChainId);
        stream.writeString(heterogeneousTxHash);
        stream.write(proposalTxHash.getBytes());
        stream.writeBytesWithLength(address);
        stream.writeBytesWithLength(hash);
        int listSize = listDistributionFee == null ? 0 : listDistributionFee.size();
        stream.writeUint16(listSize);
        if(null != listDistributionFee){
            for(HeterogeneousAddress address : listDistributionFee){
                stream.writeNulsData(address);
            }
        }
        if (null != proposalExeHash) {
            stream.write(proposalExeHash.getBytes());
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousChainId = byteBuffer.readUint16();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.proposalTxHash = byteBuffer.readHash();
        this.address = byteBuffer.readByLengthByte();
        this.hash = byteBuffer.readByLengthByte();
        int listSize = byteBuffer.readUint16();
        if(0 < listSize){
            List<HeterogeneousAddress> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(byteBuffer.readNulsData(new HeterogeneousAddress()));
            }
            this.listDistributionFee = list;
        }
        if (!byteBuffer.isFinished()) {
            this.proposalExeHash = byteBuffer.readHash();
        }

    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfBytes(this.address);
        size += SerializeUtils.sizeOfBytes(this.hash);
        size += SerializeUtils.sizeOfUint16();
        if (null != listDistributionFee) {
            for(HeterogeneousAddress address : listDistributionFee){
                size += SerializeUtils.sizeOfNulsData(address);
            }
        }
        if (null != proposalExeHash) {
            size += NulsHash.HASH_LENGTH;
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

    public NulsHash getProposalTxHash() {
        return proposalTxHash;
    }

    public void setProposalTxHash(NulsHash proposalTxHash) {
        this.proposalTxHash = proposalTxHash;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public NulsHash getProposalExeHash() {
        return proposalExeHash;
    }

    public void setProposalExeHash(NulsHash proposalExeHash) {
        this.proposalExeHash = proposalExeHash;
    }

    public List<HeterogeneousAddress> getListDistributionFee() {
        return listDistributionFee;
    }

    public void setListDistributionFee(List<HeterogeneousAddress> listDistributionFee) {
        this.listDistributionFee = listDistributionFee;
    }
}
