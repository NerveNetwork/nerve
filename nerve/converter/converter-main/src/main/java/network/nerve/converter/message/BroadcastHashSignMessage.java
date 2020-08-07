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

package network.nerve.converter.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.model.bo.HeterogeneousHash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易hash和交易签名的消息
 * @author: Loki
 * @date: 2020-02-27
 */
public class BroadcastHashSignMessage  extends BaseBusinessMessage {

    private NulsHash hash;

    private P2PHKSignature p2PHKSignature;

    /**
     * 原始交易hash(例如确认变更交易的msg, 则该属性为对应的变更交易的hash)
     * 如果是提案, 则该属性为提案交易hash
     */
    private String originalHash;

    /**
     * 交易类型
     */
    private int type;

    /**
     * 异构链hash列表
     */
    private List<HeterogeneousHash> heterogeneousHashList;

    public BroadcastHashSignMessage() {

    }

    public BroadcastHashSignMessage(Transaction tx, P2PHKSignature p2PHKSignature) {
        this.hash = tx.getHash();
        this.type = tx.getType();
        this.p2PHKSignature = p2PHKSignature;
    }

    public BroadcastHashSignMessage(Transaction tx, P2PHKSignature p2PHKSignature, String originalHash) {
        this.hash = tx.getHash();
        this.type = tx.getType();
        this.originalHash = originalHash;
        this.p2PHKSignature = p2PHKSignature;
    }

    public BroadcastHashSignMessage(Transaction tx, P2PHKSignature p2PHKSignature, List<HeterogeneousHash> heterogeneousHashList) {
        this.hash = tx.getHash();
        this.type = tx.getType();
        this.p2PHKSignature = p2PHKSignature;
        this.heterogeneousHashList = heterogeneousHashList;
    }

    public BroadcastHashSignMessage(Transaction tx, P2PHKSignature p2PHKSignature, String originalHash, List<HeterogeneousHash> heterogeneousHashList) {
        this.hash = tx.getHash();
        this.type = tx.getType();
        this.p2PHKSignature = p2PHKSignature;
        this.originalHash = originalHash;
        this.heterogeneousHashList = heterogeneousHashList;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(hash.getBytes());
        stream.writeNulsData(p2PHKSignature);
        stream.writeString(originalHash);
        stream.writeUint16(type);
        int count = heterogeneousHashList == null ? 0 : heterogeneousHashList.size();
        stream.writeUint16(count);
        if(null != heterogeneousHashList){
            for(HeterogeneousHash hash : heterogeneousHashList){
                stream.writeNulsData(hash);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.hash = byteBuffer.readHash();
        this.p2PHKSignature = byteBuffer.readNulsData(new P2PHKSignature());
        this.originalHash = byteBuffer.readString();
        this.type = byteBuffer.readUint16();
        int count = byteBuffer.readUint16();
        if(0 < count){
            List<HeterogeneousHash> list = new ArrayList<>();
            for(int i = 0; i< count; i++){
                list.add(byteBuffer.readNulsData(new HeterogeneousHash()));
            }
            this.heterogeneousHashList = list;
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfNulsData(p2PHKSignature);
        size += SerializeUtils.sizeOfString(originalHash);
        size += SerializeUtils.sizeOfUint16();
        size += SerializeUtils.sizeOfUint16();
        if (null != heterogeneousHashList) {
            for(HeterogeneousHash hash : heterogeneousHashList){
                size += SerializeUtils.sizeOfNulsData(hash);
            }
        }
        return size;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<HeterogeneousHash> getHeterogeneousHashList() {
        return heterogeneousHashList;
    }

    public void setHeterogeneousHashList(List<HeterogeneousHash> heterogeneousHashList) {
        this.heterogeneousHashList = heterogeneousHashList;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public P2PHKSignature getP2PHKSignature() {
        return p2PHKSignature;
    }

    public void setP2PHKSignature(P2PHKSignature p2PHKSignature) {
        this.p2PHKSignature = p2PHKSignature;
    }

}
