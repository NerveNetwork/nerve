/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

package io.nuls.api.model.entity;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 确认提现成功状态交易txdata
 * @author: Charlie
 * @date: 2020-02-17
 */
public class ConfirmWithdrawalTxData extends BaseNulsData {

    public static class HeterogeneousAddress extends BaseNulsData {

        private int chainId;

        private String address;

        @Override
        protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
            stream.writeUint16(chainId);
            stream.writeString(address);
        }

        @Override
        public void parse(NulsByteBuffer byteBuffer) throws NulsException {
            this.chainId = byteBuffer.readUint16();
            this.address = byteBuffer.readString();
        }

        @Override
        public int size() {
            int size = 0;
            size += SerializeUtils.sizeOfUint16();
            size += SerializeUtils.sizeOfString(this.address);
            return size;
        }

        public int getChainId() {
            return chainId;
        }

        public void setChainId(int chainId) {
            this.chainId = chainId;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

        private int heterogeneousChainId;
    /**
     * 异构链中对应的提现交易确认高度
     */
    private long heterogeneousHeight;

    /**
     * 异构链中对应的提现交易hash
     */
    private String heterogeneousTxHash;

    /**
     * 网络中对应的提现交易hash
     */
    private NulsHash withdrawalTxHash;

    /**
     * 需要分发提现手续费的节点异构链地址
     */
    private List<HeterogeneousAddress> listDistributionFee;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(heterogeneousChainId);
        stream.writeInt64(heterogeneousHeight);
        stream.writeString(heterogeneousTxHash);
        stream.write(withdrawalTxHash.getBytes());
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
        this.heterogeneousHeight = byteBuffer.readInt64();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.withdrawalTxHash = byteBuffer.readHash();
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
        size += SerializeUtils.sizeOfInt64();
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
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

    public long getHeterogeneousHeight() {
        return heterogeneousHeight;
    }

    public void setHeterogeneousHeight(long heterogeneousHeight) {
        this.heterogeneousHeight = heterogeneousHeight;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public NulsHash getWithdrawalTxHash() {
        return withdrawalTxHash;
    }

    public void setWithdrawalTxHash(NulsHash withdrawalTxHash) {
        this.withdrawalTxHash = withdrawalTxHash;
    }

    public List<HeterogeneousAddress> getListDistributionFee() {
        return listDistributionFee;
    }

    public void setListDistributionFee(List<HeterogeneousAddress> listDistributionFee) {
        this.listDistributionFee = listDistributionFee;
    }

}
