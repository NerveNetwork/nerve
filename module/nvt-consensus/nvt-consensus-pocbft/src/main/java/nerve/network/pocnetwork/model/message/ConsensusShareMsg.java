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
package nerve.network.pocnetwork.model.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsSignData;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.crypto.ECIESUtil;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.CryptoException;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.pocnetwork.model.message.sub.ConsensusShare;

import java.io.IOException;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description 用于广播身份消息
 */
public class ConsensusShareMsg extends BaseBusinessMessage {
    /**
     * 密文 包含 nodeId,pubKey信息的List
     */
    private byte[] identityList;
    private long messageTime;
    //对identityList的签名
    private P2PHKSignature sign = null;
    private transient ConsensusShare consensusShare;
    public ConsensusShareMsg(ConsensusShare consensusShare, byte[] peerPubKey, byte[] pubKey, byte[] privKey) throws IOException {
        this.identityList = ECIESUtil.encrypt(peerPubKey, consensusShare.serialize());
        this.messageTime = NulsDateUtils.getCurrentTimeMillis();
        ECKey  ecKey = ECKey.fromPrivate(privKey);
        NulsSignData  nulsSignData = SignatureUtil.signDigest(identityList,ecKey);
        sign = new P2PHKSignature(nulsSignData, ecKey.getPubKey());
    }

    public ConsensusShareMsg() {
    }

    public ConsensusShare getDecryptConsensusShare(byte[] privKey, byte[] pubKey) {
        try {
            byte[] enData = ECIESUtil.decrypt(privKey, HexUtil.encode(identityList));
            ConsensusShare consensusShare = new ConsensusShare();
            consensusShare.parse(enData, 0);
            return consensusShare;
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (NulsException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(identityList);
        stream.writeUint48(messageTime);
        stream.writeNulsData(sign);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.identityList = byteBuffer.readByLengthByte();
        this.messageTime = byteBuffer.readUint48();
        this.sign = byteBuffer.readNulsData(new P2PHKSignature());
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfBytes(identityList);
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfNulsData(sign);
        return size;
    }


    public byte[] getIdentityList() {
        return identityList;
    }

    public void setIdentityList(byte[] identityList) {
        this.identityList = identityList;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public ConsensusShare getConsensusShare() {
        return consensusShare;
    }

    public void setConsensusShare(ConsensusShare consensusShare) {
        this.consensusShare = consensusShare;
    }

    public P2PHKSignature getSign() {
        return sign;
    }

    public void setSign(P2PHKSignature sign) {
        this.sign = sign;
    }

    /*public static void main(String[] args) {

        ECKey ecKey = new ECKey();
        ConsensusShareMsg consensusShareMsg = new ConsensusShareMsg();
        ConsensusNet consensusNet = new ConsensusNet();
        consensusNet.setNodeId("192.168.1.155");
        try {
            consensusShareMsg.setIdentityList(consensusNet.serialize());
            NulsSignData nulsSignData = SignatureUtil.signDigest(consensusShareMsg.serialize(), ecKey);
            P2PHKSignature p2phks = new P2PHKSignature(nulsSignData, ecKey.getPubKey());
            System.out.println(SignatureUtil.validateSignture(consensusShareMsg.serialize(), p2phks));
            consensusNet.setNodeId("192.168.1.156");
            consensusShareMsg.setIdentityList(consensusNet.serialize());
            System.out.println(SignatureUtil.validateSignture(consensusShareMsg.serialize(), p2phks));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NulsException e) {
            e.printStackTrace();
        }
    }*/
}
