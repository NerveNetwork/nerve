/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package nerve.network.pocnetwork.model.message.sub;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.crypto.ECIESUtil;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.CryptoException;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.pocnetwork.model.ConsensusNet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description 用于广播身份消息
 */
public class ConsensusIdentitiesSub extends BaseNulsData {
    private List<NodeIdentity> identityList = new ArrayList<>();
    private boolean isBroadcast = false;
    private long messageTime;

    /**
     * 不作序列化,本地节点数据
     */
    private transient ConsensusNet consensusNet;

    public ConsensusIdentitiesSub(ConsensusNet consensusNet) {
        this.consensusNet = consensusNet;
        messageTime = NulsDateUtils.getCurrentTimeSeconds();
    }

    public ConsensusIdentitiesSub() {
        super();
    }

    public List<NodeIdentity> getIdentityList() {
        return identityList;
    }

    public void setIdentityList(List<NodeIdentity> identityList) {
        this.identityList = identityList;
    }


    public ConsensusNet getConsensusNet() {
        return consensusNet;
    }

    public void setConsensusNet(ConsensusNet consensusNet) {
        this.consensusNet = consensusNet;
    }

    public void addEncryptNodes(byte[] pubKey) throws IOException {
        NodeIdentity nodeIdentity = new NodeIdentity(ECIESUtil.encrypt(pubKey, consensusNet.serialize()), pubKey);
        identityList.add(nodeIdentity);

    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer buffer) throws IOException {
        buffer.writeUint16(identityList.size());
        for (NodeIdentity identity : identityList) {
            buffer.writeNulsData(identity);
        }
        buffer.writeBoolean(isBroadcast);
        buffer.writeUint48(messageTime);
    }

    @Override
    public void parse(NulsByteBuffer nulsByteBuffer) throws NulsException {
        int size = nulsByteBuffer.readUint16();
        for (int i = 0; i < size; i++) {
            NodeIdentity identity = new NodeIdentity();
            nulsByteBuffer.readNulsData(identity);
            this.identityList.add(identity);
        }
        this.isBroadcast = nulsByteBuffer.readBoolean();
        this.messageTime = nulsByteBuffer.readUint48();
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
        for (NodeIdentity identity : identityList) {
            size += SerializeUtils.sizeOfNulsData(identity);
        }
        size += SerializeUtils.sizeOfBoolean();
        size += SerializeUtils.sizeOfUint48();
        return size;
    }

    public boolean isBroadcast() {
        return isBroadcast;
    }

    public void setBroadcast(boolean broadcast) {
        isBroadcast = broadcast;
    }

    public ConsensusNet getDecryptConsensusNet(byte[] privKey, byte[] pubKey) {
        try {
            for (NodeIdentity identity : identityList) {
                if (ArraysTool.arrayEquals(pubKey, identity.getPubKey())) {
                    byte[] enData = ECIESUtil.decrypt(privKey, HexUtil.encode(identity.getIdentity()));
                    ConsensusNet consensusNet = new ConsensusNet();
                    consensusNet.parse(enData, 0);
                    return consensusNet;
                }
            }
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (NulsException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

}
