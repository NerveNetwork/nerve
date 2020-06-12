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

package network.nerve.converter.model.bo;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020-03-11
 */
public class HeterogeneousConfirmedVirtualBank extends BaseNulsData {

    /**
     * 异构链id
     */
    private int heterogeneousChainId;

    /**
     * 异构链多签地址/或合约地址 由异构链决定
     */
    private String heterogeneousAddress;

    /**
     * 异构链交易hash
     */
    private String heterogeneousTxHash;

    /**
     * 生效时间
     * 秒级时间戳
     */
    private long effectiveTime;

    /**
     * 调用异构链的签名地址列表
     */
    private List<HeterogeneousAddress> signedHeterogeneousAddress;


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint16(heterogeneousChainId);
        stream.writeString(heterogeneousAddress);
        stream.writeString(heterogeneousTxHash);
        stream.writeUint48(effectiveTime);
        int listSize = signedHeterogeneousAddress == null ? 0 : signedHeterogeneousAddress.size();
        stream.writeUint16(listSize);
        if(null != signedHeterogeneousAddress){
            for(HeterogeneousAddress address : signedHeterogeneousAddress){
                stream.writeNulsData(address);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.heterogeneousChainId = byteBuffer.readUint16();
        this.heterogeneousAddress = byteBuffer.readString();
        this.heterogeneousTxHash = byteBuffer.readString();
        this.effectiveTime = byteBuffer.readUint48();
        int listSize = byteBuffer.readUint16();
        if(0 < listSize){
            List<HeterogeneousAddress> list = new ArrayList<>();
            for(int i = 0; i< listSize; i++){
                list.add(byteBuffer.readNulsData(new HeterogeneousAddress()));
            }
            this.signedHeterogeneousAddress = list;
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfString(this.heterogeneousAddress);
        size += SerializeUtils.sizeOfString(this.heterogeneousTxHash);
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfUint16();
        if (null != signedHeterogeneousAddress) {
            for(HeterogeneousAddress address : signedHeterogeneousAddress){
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

    public String getHeterogeneousAddress() {
        return heterogeneousAddress;
    }

    public void setHeterogeneousAddress(String heterogeneousAddress) {
        this.heterogeneousAddress = heterogeneousAddress;
    }

    public String getHeterogeneousTxHash() {
        return heterogeneousTxHash;
    }

    public void setHeterogeneousTxHash(String heterogeneousTxHash) {
        this.heterogeneousTxHash = heterogeneousTxHash;
    }

    public long getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public List<HeterogeneousAddress> getSignedHeterogeneousAddress() {
        return signedHeterogeneousAddress;
    }

    public void setSignedHeterogeneousAddress(List<HeterogeneousAddress> signedHeterogeneousAddress) {
        this.signedHeterogeneousAddress = signedHeterogeneousAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeterogeneousConfirmedVirtualBank bank = (HeterogeneousConfirmedVirtualBank) o;

        if (heterogeneousTxHash != null ? !heterogeneousTxHash.equals(bank.heterogeneousTxHash) : bank.heterogeneousTxHash != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return heterogeneousTxHash != null ? heterogeneousTxHash.hashCode() : 0;
    }
}
